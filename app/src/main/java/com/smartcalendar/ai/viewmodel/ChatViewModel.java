package com.smartcalendar.ai.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.smartcalendar.ai.R;
import com.smartcalendar.ai.data.local.Event;
import com.smartcalendar.ai.data.remote.AiAssistant;
import com.smartcalendar.ai.data.remote.AssistantResponse;
import com.smartcalendar.ai.data.repository.EventRepository;
import com.smartcalendar.ai.ui.chat.ChatMessage;
import com.smartcalendar.ai.util.ConflictText;
import com.smartcalendar.ai.util.EventChanges;
import com.smartcalendar.ai.util.EventColors;
import com.smartcalendar.ai.util.TextNormalizer;
import com.smartcalendar.ai.util.TimeUtils;

import java.util.ArrayList;
import java.util.List;

public class ChatViewModel extends AndroidViewModel {

    private static final int MAX_LISTED_EVENTS = 20;
    private static final int MAX_CANDIDATES = 5;
    private static final long DAY_MS = 24L * 60 * 60 * 1000;
    private static final int MAX_HISTORY_TURNS = 4;

    private final EventRepository repository;
    private final List<ChatMessage> items = new ArrayList<>();
    private final MutableLiveData<List<ChatMessage>> messages = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final List<AiAssistant.Turn> history = new ArrayList<>();
    private long nextId = 1;

    public ChatViewModel(@NonNull Application application) {
        super(application);
        repository = new EventRepository(application);
        addBot(application.getString(R.string.chat_welcome));
    }

    public LiveData<List<ChatMessage>> getMessages() {
        return messages;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    private synchronized long newId() {
        return nextId++;
    }

    private synchronized void add(ChatMessage message) {
        items.add(message);
        messages.postValue(new ArrayList<>(items));
    }

    private void addBot(String text) {
        add(ChatMessage.bot(newId(), text));
    }

    private String str(int resId, Object... args) {
        return getApplication().getString(resId, args);
    }

    public void send(String text) {
        String trimmed = text == null ? "" : text.trim();
        if (trimmed.isEmpty() || Boolean.TRUE.equals(loading.getValue())) {
            return;
        }
        add(ChatMessage.user(newId(), trimmed));
        loading.setValue(true);

        AiAssistant.ask(trimmed, new ArrayList<>(history), System.currentTimeMillis(), new AiAssistant.Listener() {
            @Override
            public void onSuccess(AssistantResponse response) {
                loading.setValue(false);
                history.add(new AiAssistant.Turn(trimmed, AiAssistant.toJson(response)));
                while (history.size() > MAX_HISTORY_TURNS) {
                    history.remove(0);
                }
                handle(response);
            }

            @Override
            public void onError(AiAssistant.ErrorType type, int httpCode, String detail) {
                loading.setValue(false);
                String message = errorMessage(type, httpCode);
                if (detail != null && !detail.isEmpty()
                        && type != AiAssistant.ErrorType.NO_KEY) {
                    message += "\n\n" + str(R.string.chat_error_detail, detail);
                }
                addBot(message);
            }
        });
    }

    private String errorMessage(AiAssistant.ErrorType type, int httpCode) {
        switch (type) {
            case NO_KEY:
                return str(R.string.chat_error_no_key);
            case INVALID_KEY:
                return str(R.string.chat_error_invalid_key);
            case MODEL_NOT_FOUND:
                return str(R.string.chat_error_model);
            case RATE_LIMIT:
                return str(R.string.chat_error_rate_limit);
            case OVERLOADED:
                return str(R.string.chat_error_overloaded);
            case NETWORK:
                return str(R.string.chat_error_network);
            case BAD_RESPONSE:
                return str(R.string.chat_parse_failed);
            default:
                return str(R.string.chat_error_server, httpCode);
        }
    }

    public synchronized void newConversation() {
        if (Boolean.TRUE.equals(loading.getValue())) {
            return;
        }
        history.clear();
        items.clear();
        add(ChatMessage.bot(newId(), getApplication().getString(R.string.chat_welcome)));
    }

    private void handle(AssistantResponse response) {
        String reply = response.reply == null ? "" : response.reply.trim();

        if ("create_event".equals(response.intent) && response.event != null) {
            AssistantResponse.EventDraft draft = response.event;
            long start = TimeUtils.parseLocalIso(draft.start);
            if (start < 0 || draft.title == null || draft.title.trim().isEmpty()) {
                addBot(str(R.string.chat_parse_failed));
                return;
            }
            long end = TimeUtils.parseLocalIso(draft.end);
            Long endMillis = end > start ? end : null;
            int reminder = draft.reminderMinutes == null ? -1 : draft.reminderMinutes;
            String title = draft.title.trim();
            String description = draft.description == null ? "" : draft.description.trim();
            repository.findConflicts(start, endMillis, 0, conflicts -> {
                String conflictText = conflicts.isEmpty() ? null
                        : str(R.string.chat_conflict, ConflictText.list(getApplication(), conflicts, start, 3));
                add(ChatMessage.preview(newId(), reply, title, description,
                        draft.category, start, endMillis, reminder, conflictText));
            });

        } else if ("query_events".equals(response.intent) && response.range != null) {
            long from = TimeUtils.parseLocalIso(response.range.start);
            long to = TimeUtils.parseLocalIso(response.range.end);
            if (from < 0 || to <= from) {
                addBot(str(R.string.chat_parse_failed));
                return;
            }
            repository.getEventsInRange(from, to, events -> addBot(formatEventList(reply, events)));

        } else if (("update_event".equals(response.intent) || "delete_event".equals(response.intent))
                && response.target != null) {
            boolean delete = "delete_event".equals(response.intent);
            if (!delete && response.changes == null) {
                addBot(str(R.string.chat_parse_failed));
                return;
            }
            findTargetEvents(response.target, response.changes, delete);

        } else {
            addBot(reply.isEmpty() ? str(R.string.chat_parse_failed) : reply);
        }
    }

    private void findTargetEvents(AssistantResponse.Target target, AssistantResponse.Changes changes, boolean delete) {
        long from = TimeUtils.parseLocalIso(target.start);
        long to = TimeUtils.parseLocalIso(target.end);
        if (from < 0 || to <= from) {
            long now = System.currentTimeMillis();
            from = now - 30 * DAY_MS;
            to = now + 365 * DAY_MS;
        }
        String keyword = target.keyword;

        repository.getEventsInRange(from, to, events -> {
            List<Event> matches = new ArrayList<>();
            for (Event e : events) {
                if (TextNormalizer.matchesAllWords(e.title, keyword)) {
                    matches.add(e);
                }
            }
            if (matches.isEmpty()) {
                addBot(str(R.string.chat_target_not_found));
            } else if (matches.size() == 1) {
                showAction(matches.get(0), changes, delete);
            } else {
                List<Event> shown = matches.subList(0, Math.min(matches.size(), MAX_CANDIDATES));
                String prompt = str(delete ? R.string.chat_pick_delete : R.string.chat_pick_edit, matches.size());
                add(ChatMessage.pick(newId(), prompt, new ArrayList<>(shown), changes, delete));
            }
        });
    }

    private void showAction(Event target, AssistantResponse.Changes changes, boolean delete) {
        if (delete) {
            add(ChatMessage.action(newId(), target, null, true, null));
            return;
        }
        String[] categories = getApplication().getResources().getStringArray(R.array.categories);
        Event updated = EventChanges.apply(categories, target, changes);
        if (EventChanges.isSame(target, updated)) {
            addBot(str(R.string.chat_no_change));
            return;
        }
        repository.findConflicts(updated.startTime, updated.endTime, target.id, conflicts -> {
            String conflictText = conflicts.isEmpty() ? null
                    : str(R.string.chat_conflict, ConflictText.list(getApplication(), conflicts, updated.startTime, 3));
            add(ChatMessage.action(newId(), target, updated, false, conflictText));
        });
    }

    public void selectCandidate(long messageId, int candidateIndex) {
        ChatMessage picked;
        synchronized (this) {
            int index = indexOf(messageId);
            if (index < 0) {
                return;
            }
            ChatMessage m = items.get(index);
            if (m.type != ChatMessage.Type.PICK || m.state != ChatMessage.State.PENDING
                    || candidateIndex < 0 || candidateIndex >= m.candidates.size()) {
                return;
            }
            items.set(index, m.withState(ChatMessage.State.SAVED));
            messages.postValue(new ArrayList<>(items));
            picked = m;
        }
        showAction(picked.candidates.get(candidateIndex), picked.changes, picked.delete);
    }

    private void confirmAction(int index, ChatMessage m) {
        if (m.delete) {
            repository.delete(m.target);
        } else {
            repository.update(m.updated);
        }
        items.set(index, m.withState(ChatMessage.State.SAVED));
        messages.postValue(new ArrayList<>(items));
    }

    private String formatEventList(String reply, List<Event> events) {
        StringBuilder sb = new StringBuilder(reply.isEmpty() ? str(R.string.chat_query_default) : reply);
        if (events == null || events.isEmpty()) {
            sb.append("\n\n").append(str(R.string.chat_no_events));
            return sb.toString();
        }
        int shown = Math.min(events.size(), MAX_LISTED_EVENTS);
        for (int i = 0; i < shown; i++) {
            Event e = events.get(i);
            sb.append("\n• ").append(TimeUtils.formatDate(e.startTime))
                    .append(" ").append(TimeUtils.formatTime(e.startTime))
                    .append(" – ").append(e.title);
        }
        if (events.size() > shown) {
            sb.append("\n").append(str(R.string.chat_more_events, events.size() - shown));
        }
        return sb.toString();
    }

    public synchronized void confirmEvent(long messageId) {
        int index = indexOf(messageId);
        if (index < 0) {
            return;
        }
        ChatMessage m = items.get(index);
        if (m.type == ChatMessage.Type.ACTION && m.state == ChatMessage.State.PENDING) {
            confirmAction(index, m);
            return;
        }
        if (m.type != ChatMessage.Type.EVENT_PREVIEW || m.state != ChatMessage.State.PENDING) {
            return;
        }

        String[] categories = getApplication().getResources().getStringArray(R.array.categories);
        String category = categories[categories.length - 1];
        for (String c : categories) {
            if (c.equalsIgnoreCase(m.category)) {
                category = c;
            }
        }

        Event event = new Event();
        event.title = m.title;
        event.description = m.description;
        event.startTime = m.startMillis;
        event.endTime = m.endMillis;
        event.category = category;
        event.color = EventColors.DEFAULT;
        event.reminderMinutes = m.reminderMinutes;
        repository.insert(event);

        items.set(index, m.withState(ChatMessage.State.SAVED));
        messages.postValue(new ArrayList<>(items));
    }

    public synchronized void discardEvent(long messageId) {
        int index = indexOf(messageId);
        if (index < 0) {
            return;
        }
        ChatMessage m = items.get(index);
        if (m.state == ChatMessage.State.PENDING) {
            items.set(index, m.withState(ChatMessage.State.DISCARDED));
            messages.postValue(new ArrayList<>(items));
        }
    }

    private int indexOf(long messageId) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).id == messageId) {
                return i;
            }
        }
        return -1;
    }
}
