package com.smartcalendar.ai.ui.calendar;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.smartcalendar.ai.R;
import com.smartcalendar.ai.data.local.Event;
import com.google.android.material.card.MaterialCardView;
import com.smartcalendar.ai.util.EventColors;
import com.smartcalendar.ai.util.TimeUtils;

import java.util.Objects;

public class EventAdapter extends ListAdapter<Event, EventAdapter.EventViewHolder> {

    public interface OnEventClickListener {
        void onEventClick(Event event);
    }

    public interface OnEventDeleteListener {
        void onEventDelete(Event event);
    }

    private static final DiffUtil.ItemCallback<Event> DIFF = new DiffUtil.ItemCallback<Event>() {
        @Override
        public boolean areItemsTheSame(@NonNull Event a, @NonNull Event b) {
            return a.id == b.id;
        }

        @Override
        public boolean areContentsTheSame(@NonNull Event a, @NonNull Event b) {
            return Objects.equals(a.title, b.title)
                    && Objects.equals(a.description, b.description)
                    && Objects.equals(a.category, b.category)
                    && a.color == b.color
                    && a.startTime == b.startTime
                    && Objects.equals(a.endTime, b.endTime)
                    && a.reminderMinutes == b.reminderMinutes;
        }
    };

    private boolean showDate = false;

    private final OnEventClickListener clickListener;
    private final OnEventDeleteListener deleteListener;

    public EventAdapter(OnEventClickListener clickListener, OnEventDeleteListener deleteListener) {
        super(DIFF);
        this.clickListener = clickListener;
        this.deleteListener = deleteListener;
    }

    public void setShowDate(boolean showDate) {
        this.showDate = showDate;
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_event, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = getItem(position);
        Context context = holder.itemView.getContext();

        String time = TimeUtils.formatTime(event.startTime);
        if (event.endTime != null) {
            time += " – " + TimeUtils.formatTime(event.endTime);
        }
        if (showDate) {
            time = TimeUtils.formatDate(event.startTime) + " · " + time;
        }
        holder.tvTime.setText(time);
        holder.tvTitle.setText(event.title);

        if (event.description != null && !event.description.isEmpty()) {
            holder.tvDescription.setText(event.description);
            holder.tvDescription.setVisibility(View.VISIBLE);
        } else {
            holder.tvDescription.setVisibility(View.GONE);
        }

        String meta = event.category == null ? "" : event.category;
        if (event.reminderMinutes >= 0) {
            meta += " · " + reminderText(context, event.reminderMinutes);
        }
        holder.tvMeta.setText(meta);
        holder.card.setCardBackgroundColor(EventColors.cardBackground(context, event.color));
        holder.viewColor.setBackgroundTintList(ColorStateList.valueOf(event.color));
        holder.tvTime.setTextColor(EventColors.timeText(event.color));

        holder.itemView.setOnClickListener(v -> clickListener.onEventClick(event));
        holder.btnDelete.setOnClickListener(v -> deleteListener.onEventDelete(event));
    }

    private static String reminderText(Context context, int minutes) {
        String amount = minutes % 60 == 0
                ? context.getString(R.string.reminder_fmt_hours, minutes / 60)
                : context.getString(R.string.reminder_fmt_minutes, minutes);
        return context.getString(R.string.reminder_before, amount);
    }

    static class EventViewHolder extends RecyclerView.ViewHolder {
        final MaterialCardView card;
        final View viewColor;
        final TextView tvTime;
        final TextView tvTitle;
        final TextView tvDescription;
        final TextView tvMeta;
        final ImageButton btnDelete;

        EventViewHolder(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.cardEvent);
            viewColor = itemView.findViewById(R.id.viewColor);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvMeta = itemView.findViewById(R.id.tvMeta);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
