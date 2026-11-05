package com.smartcalendar.ai.util;

import android.content.Context;

import com.smartcalendar.ai.data.local.Event;

public final class EventText {

    private EventText() {}

    public static String describe(Context context, Event e) {
        StringBuilder sb = new StringBuilder(e.title);
        sb.append("\n").append(TimeUtils.formatFullDate(e.startTime)).append(" · ")
                .append(TimeUtils.formatTime(e.startTime));
        if (e.endTime != null) {
            boolean sameDay = TimeUtils.startOfDay(e.endTime) == TimeUtils.startOfDay(e.startTime);
            sb.append(" – ").append(sameDay
                    ? TimeUtils.formatTime(e.endTime)
                    : TimeUtils.formatDate(e.endTime) + " " + TimeUtils.formatTime(e.endTime));
        }
        String meta = e.category == null ? "" : e.category;
        if (e.reminderMinutes >= 0) {
            meta += (meta.isEmpty() ? "" : " · ") + ReminderText.format(context, e.reminderMinutes);
        }
        if (!meta.isEmpty()) {
            sb.append("\n").append(meta);
        }
        return sb.toString();
    }
}
