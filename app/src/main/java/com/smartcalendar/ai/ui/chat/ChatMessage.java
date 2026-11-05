package com.smartcalendar.ai.ui.chat;

import com.smartcalendar.ai.data.local.Event;
import com.smartcalendar.ai.data.remote.AssistantResponse;

import java.util.List;

public class ChatMessage {

    public enum Type { USER, BOT, EVENT_PREVIEW, PICK, ACTION }

    public enum State { PENDING, SAVED, DISCARDED }

    public final long id;
    public final Type type;
    public final String text;
    public final State state;

    public final String title;
    public final String description;
    public final String category;
    public final long startMillis;
    public final Long endMillis;
    public final int reminderMinutes;

    public final String conflictText;

    public final List<Event> candidates;
    public final AssistantResponse.Changes changes;
    public final boolean delete;
    public final Event target;
    public final Event updated;

    private static class Builder {
        long id;
        Type type;
        String text;
        State state = State.PENDING;
        String title;
        String description;
        String category;
        long startMillis;
        Long endMillis;
        int reminderMinutes = -1;
        String conflictText;
        List<Event> candidates;
        AssistantResponse.Changes changes;
        boolean delete;
        Event target;
        Event updated;

        ChatMessage build() {
            return new ChatMessage(this);
        }
    }

    private ChatMessage(Builder b) {
        id = b.id;
        type = b.type;
        text = b.text;
        state = b.state;
        title = b.title;
        description = b.description;
        category = b.category;
        startMillis = b.startMillis;
        endMillis = b.endMillis;
        reminderMinutes = b.reminderMinutes;
        conflictText = b.conflictText;
        candidates = b.candidates;
        changes = b.changes;
        delete = b.delete;
        target = b.target;
        updated = b.updated;
    }

    private Builder toBuilder() {
        Builder b = new Builder();
        b.id = id;
        b.type = type;
        b.text = text;
        b.state = state;
        b.title = title;
        b.description = description;
        b.category = category;
        b.startMillis = startMillis;
        b.endMillis = endMillis;
        b.reminderMinutes = reminderMinutes;
        b.conflictText = conflictText;
        b.candidates = candidates;
        b.changes = changes;
        b.delete = delete;
        b.target = target;
        b.updated = updated;
        return b;
    }

    public static ChatMessage user(long id, String text) {
        Builder b = new Builder();
        b.id = id;
        b.type = Type.USER;
        b.text = text;
        return b.build();
    }

    public static ChatMessage bot(long id, String text) {
        Builder b = new Builder();
        b.id = id;
        b.type = Type.BOT;
        b.text = text;
        return b.build();
    }

    public static ChatMessage preview(long id, String text, String title, String description, String category,
                                      long startMillis, Long endMillis, int reminderMinutes,
                                      String conflictText) {
        Builder b = new Builder();
        b.id = id;
        b.type = Type.EVENT_PREVIEW;
        b.text = text;
        b.title = title;
        b.description = description;
        b.category = category;
        b.startMillis = startMillis;
        b.endMillis = endMillis;
        b.reminderMinutes = reminderMinutes;
        b.conflictText = conflictText;
        return b.build();
    }

    public static ChatMessage pick(long id, String text, List<Event> candidates,
                                   AssistantResponse.Changes changes, boolean delete) {
        Builder b = new Builder();
        b.id = id;
        b.type = Type.PICK;
        b.text = text;
        b.candidates = candidates;
        b.changes = changes;
        b.delete = delete;
        return b.build();
    }

    public static ChatMessage action(long id, Event target, Event updated, boolean delete, String conflictText) {
        Builder b = new Builder();
        b.id = id;
        b.type = Type.ACTION;
        b.target = target;
        b.updated = updated;
        b.delete = delete;
        b.conflictText = conflictText;
        return b.build();
    }

    public ChatMessage withState(State newState) {
        Builder b = toBuilder();
        b.state = newState;
        return b.build();
    }
}
