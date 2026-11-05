package com.smartcalendar.ai.ui.chat;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.smartcalendar.ai.R;
import com.smartcalendar.ai.data.local.Event;
import com.smartcalendar.ai.util.EventText;
import com.smartcalendar.ai.util.ReminderText;
import com.smartcalendar.ai.util.TimeUtils;

import java.util.Objects;

public class ChatAdapter extends ListAdapter<ChatMessage, RecyclerView.ViewHolder> {

    public interface Listener {
        void onSave(ChatMessage message);

        void onDiscard(ChatMessage message);

        void onPick(ChatMessage message, int candidateIndex);
    }

    private static final DiffUtil.ItemCallback<ChatMessage> DIFF = new DiffUtil.ItemCallback<ChatMessage>() {
        @Override
        public boolean areItemsTheSame(@NonNull ChatMessage a, @NonNull ChatMessage b) {
            return a.id == b.id;
        }

        @Override
        public boolean areContentsTheSame(@NonNull ChatMessage a, @NonNull ChatMessage b) {
            return a.state == b.state && Objects.equals(a.text, b.text);
        }
    };

    private final Listener listener;

    public ChatAdapter(Listener listener) {
        super(DIFF);
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        return getItem(position).type.ordinal();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        switch (ChatMessage.Type.values()[viewType]) {
            case USER:
                return new TextHolder(inflater.inflate(R.layout.item_chat_user, parent, false));
            case EVENT_PREVIEW:
                return new PreviewHolder(inflater.inflate(R.layout.item_chat_event, parent, false));
            case PICK:
                return new PickHolder(inflater.inflate(R.layout.item_chat_pick, parent, false));
            case ACTION:
                return new ActionHolder(inflater.inflate(R.layout.item_chat_action, parent, false));
            default:
                return new TextHolder(inflater.inflate(R.layout.item_chat_bot, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = getItem(position);
        if (holder instanceof PreviewHolder) {
            bindPreview((PreviewHolder) holder, message);
        } else if (holder instanceof PickHolder) {
            bindPick((PickHolder) holder, message);
        } else if (holder instanceof ActionHolder) {
            bindAction((ActionHolder) holder, message);
        } else {
            ((TextHolder) holder).tvText.setText(message.text);
        }
    }

    private void bindPreview(PreviewHolder h, ChatMessage m) {
        Context context = h.itemView.getContext();

        if (m.text == null || m.text.isEmpty()) {
            h.tvReply.setVisibility(View.GONE);
        } else {
            h.tvReply.setText(m.text);
            h.tvReply.setVisibility(View.VISIBLE);
        }
        h.tvTitle.setText(m.title);

        String time = TimeUtils.formatFullDate(m.startMillis) + " · " + TimeUtils.formatTime(m.startMillis);
        if (m.endMillis != null) {
            boolean sameDay = TimeUtils.startOfDay(m.endMillis) == TimeUtils.startOfDay(m.startMillis);
            time += " – " + (sameDay
                    ? TimeUtils.formatTime(m.endMillis)
                    : TimeUtils.formatDate(m.endMillis) + " " + TimeUtils.formatTime(m.endMillis));
        }
        h.tvTime.setText(time);

        String meta = m.category == null ? "" : m.category;
        if (m.reminderMinutes >= 0) {
            meta += (meta.isEmpty() ? "" : " · ") + ReminderText.format(context, m.reminderMinutes);
        }
        h.tvMeta.setText(meta);
        h.tvMeta.setVisibility(meta.isEmpty() ? View.GONE : View.VISIBLE);

        if (m.conflictText != null && m.state == ChatMessage.State.PENDING) {
            h.tvConflict.setText(m.conflictText);
            h.tvConflict.setVisibility(View.VISIBLE);
        } else {
            h.tvConflict.setVisibility(View.GONE);
        }

        if (m.description != null && !m.description.isEmpty()) {
            h.tvDescription.setText(m.description);
            h.tvDescription.setVisibility(View.VISIBLE);
        } else {
            h.tvDescription.setVisibility(View.GONE);
        }

        boolean pending = m.state == ChatMessage.State.PENDING;
        h.actions.setVisibility(pending ? View.VISIBLE : View.GONE);
        h.tvStatus.setVisibility(pending ? View.GONE : View.VISIBLE);
        h.tvStatus.setText(m.state == ChatMessage.State.SAVED ? R.string.chat_saved : R.string.chat_discarded);

        h.btnSave.setOnClickListener(v -> listener.onSave(m));
        h.btnDiscard.setOnClickListener(v -> listener.onDiscard(m));
    }

    private void bindPick(PickHolder h, ChatMessage m) {
        Context context = h.itemView.getContext();
        h.tvText.setText(m.text);
        h.container.removeAllViews();

        boolean pending = m.state == ChatMessage.State.PENDING;
        if (pending) {
            LayoutInflater inflater = LayoutInflater.from(context);
            for (int i = 0; i < m.candidates.size(); i++) {
                Event e = m.candidates.get(i);
                TextView row = (TextView) inflater.inflate(R.layout.item_chat_candidate, h.container, false);
                row.setText(TimeUtils.formatDate(e.startTime) + " " + TimeUtils.formatTime(e.startTime)
                        + "  " + e.title);
                final int index = i;
                row.setOnClickListener(v -> listener.onPick(m, index));
                h.container.addView(row);
            }
        }
        h.container.setVisibility(pending ? View.VISIBLE : View.GONE);
        h.btnCancel.setVisibility(pending ? View.VISIBLE : View.GONE);
        h.btnCancel.setOnClickListener(v -> listener.onDiscard(m));
        h.tvStatus.setVisibility(pending ? View.GONE : View.VISIBLE);
        h.tvStatus.setText(m.state == ChatMessage.State.SAVED ? R.string.chat_pick_done : R.string.chat_action_cancelled);
    }

    private void bindAction(ActionHolder h, ChatMessage m) {
        Context context = h.itemView.getContext();
        h.tvKind.setText(m.delete ? R.string.chat_action_delete_title : R.string.chat_action_edit_title);
        h.tvCurrentLabel.setText(m.delete ? R.string.chat_action_will_delete : R.string.chat_action_current);
        h.tvCurrent.setText(EventText.describe(context, m.target));

        boolean showAfter = !m.delete && m.updated != null;
        h.tvAfterLabel.setVisibility(showAfter ? View.VISIBLE : View.GONE);
        h.tvAfter.setVisibility(showAfter ? View.VISIBLE : View.GONE);
        if (showAfter) {
            h.tvAfter.setText(EventText.describe(context, m.updated));
        }

        boolean pending = m.state == ChatMessage.State.PENDING;
        if (m.conflictText != null && pending) {
            h.tvConflict.setText(m.conflictText);
            h.tvConflict.setVisibility(View.VISIBLE);
        } else {
            h.tvConflict.setVisibility(View.GONE);
        }

        h.btnConfirm.setText(m.delete ? R.string.chat_action_confirm_delete : R.string.chat_action_confirm_edit);
        int tint = ContextCompat.getColor(context, m.delete ? R.color.danger : R.color.primary);
        h.btnConfirm.setBackgroundTintList(ColorStateList.valueOf(tint));
        h.btnConfirm.setOnClickListener(v -> listener.onSave(m));
        h.btnCancel.setOnClickListener(v -> listener.onDiscard(m));

        h.buttons.setVisibility(pending ? View.VISIBLE : View.GONE);
        h.tvStatus.setVisibility(pending ? View.GONE : View.VISIBLE);
        if (m.state == ChatMessage.State.SAVED) {
            h.tvStatus.setText(m.delete ? R.string.chat_action_done_delete : R.string.chat_action_done_edit);
        } else {
            h.tvStatus.setText(R.string.chat_action_cancelled);
        }
    }

    static class TextHolder extends RecyclerView.ViewHolder {
        final TextView tvText;

        TextHolder(@NonNull View itemView) {
            super(itemView);
            tvText = itemView.findViewById(R.id.tvChatText);
        }
    }

    static class PreviewHolder extends RecyclerView.ViewHolder {
        final TextView tvReply;
        final TextView tvTitle;
        final TextView tvTime;
        final TextView tvMeta;
        final TextView tvConflict;
        final TextView tvDescription;
        final TextView tvStatus;
        final View actions;
        final MaterialButton btnSave;
        final MaterialButton btnDiscard;

        PreviewHolder(@NonNull View itemView) {
            super(itemView);
            tvReply = itemView.findViewById(R.id.tvPreviewReply);
            tvTitle = itemView.findViewById(R.id.tvPreviewTitle);
            tvTime = itemView.findViewById(R.id.tvPreviewTime);
            tvMeta = itemView.findViewById(R.id.tvPreviewMeta);
            tvConflict = itemView.findViewById(R.id.tvPreviewConflict);
            tvDescription = itemView.findViewById(R.id.tvPreviewDescription);
            tvStatus = itemView.findViewById(R.id.tvPreviewStatus);
            actions = itemView.findViewById(R.id.previewActions);
            btnSave = itemView.findViewById(R.id.btnPreviewSave);
            btnDiscard = itemView.findViewById(R.id.btnPreviewDiscard);
        }
    }

    static class PickHolder extends RecyclerView.ViewHolder {
        final TextView tvText;
        final LinearLayout container;
        final MaterialButton btnCancel;
        final TextView tvStatus;

        PickHolder(@NonNull View itemView) {
            super(itemView);
            tvText = itemView.findViewById(R.id.tvPickText);
            container = itemView.findViewById(R.id.pickContainer);
            btnCancel = itemView.findViewById(R.id.btnPickCancel);
            tvStatus = itemView.findViewById(R.id.tvPickStatus);
        }
    }

    static class ActionHolder extends RecyclerView.ViewHolder {
        final TextView tvKind;
        final TextView tvCurrentLabel;
        final TextView tvCurrent;
        final TextView tvAfterLabel;
        final TextView tvAfter;
        final TextView tvConflict;
        final View buttons;
        final MaterialButton btnCancel;
        final MaterialButton btnConfirm;
        final TextView tvStatus;

        ActionHolder(@NonNull View itemView) {
            super(itemView);
            tvKind = itemView.findViewById(R.id.tvActionKind);
            tvCurrentLabel = itemView.findViewById(R.id.tvActionCurrentLabel);
            tvCurrent = itemView.findViewById(R.id.tvActionCurrent);
            tvAfterLabel = itemView.findViewById(R.id.tvActionAfterLabel);
            tvAfter = itemView.findViewById(R.id.tvActionAfter);
            tvConflict = itemView.findViewById(R.id.tvActionConflict);
            buttons = itemView.findViewById(R.id.actionButtons);
            btnCancel = itemView.findViewById(R.id.btnActionCancel);
            btnConfirm = itemView.findViewById(R.id.btnActionConfirm);
            tvStatus = itemView.findViewById(R.id.tvActionStatus);
        }
    }
}
