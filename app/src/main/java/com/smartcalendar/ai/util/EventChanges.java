package com.smartcalendar.ai.util;

import com.smartcalendar.ai.data.local.Event;
import com.smartcalendar.ai.data.remote.AssistantResponse;

import java.util.Calendar;
import java.util.Objects;

public final class EventChanges {

    private EventChanges() {}

    public static Event apply(String[] categories, Event old, AssistantResponse.Changes c) {
        Event e = old.copy();

        if (c.title != null && !c.title.trim().isEmpty()) {
            e.title = c.title.trim();
        }
        if (c.description != null) {
            e.description = c.description.trim();
        }
        if (c.category != null) {
            for (String category : categories) {
                if (category.equalsIgnoreCase(c.category.trim())) {
                    e.category = category;
                }
            }
        }

        Calendar start = Calendar.getInstance();
        start.setTimeInMillis(old.startTime);
        int[] date = TimeUtils.parseLocalDate(c.date);
        if (date != null) {
            start.set(date[0], date[1] - 1, date[2]);
        }
        int[] startTime = TimeUtils.parseHourMinute(c.startTime);
        if (startTime != null) {
            start.set(Calendar.HOUR_OF_DAY, startTime[0]);
            start.set(Calendar.MINUTE, startTime[1]);
            start.set(Calendar.SECOND, 0);
            start.set(Calendar.MILLISECOND, 0);
        }
        e.startTime = start.getTimeInMillis();

        int[] endTime = TimeUtils.parseHourMinute(c.endTime);
        if (Boolean.TRUE.equals(c.removeEnd)) {
            e.endTime = null;
        } else if (endTime != null) {
            Calendar end = (Calendar) start.clone();
            end.set(Calendar.HOUR_OF_DAY, endTime[0]);
            end.set(Calendar.MINUTE, endTime[1]);
            end.set(Calendar.SECOND, 0);
            end.set(Calendar.MILLISECOND, 0);
            if (!end.after(start)) {
                end.add(Calendar.DAY_OF_MONTH, 1);
            }
            e.endTime = end.getTimeInMillis();
        } else if (old.endTime != null) {
            e.endTime = e.startTime + (old.endTime - old.startTime);
        }

        if (Boolean.TRUE.equals(c.removeReminder)) {
            e.reminderMinutes = -1;
        } else if (c.reminderMinutes != null && c.reminderMinutes > 0) {
            e.reminderMinutes = c.reminderMinutes;
        }
        return e;
    }

    public static boolean isSame(Event a, Event b) {
        return Objects.equals(a.title, b.title)
                && Objects.equals(a.description, b.description)
                && Objects.equals(a.category, b.category)
                && a.startTime == b.startTime
                && Objects.equals(a.endTime, b.endTime)
                && a.reminderMinutes == b.reminderMinutes;
    }
}
