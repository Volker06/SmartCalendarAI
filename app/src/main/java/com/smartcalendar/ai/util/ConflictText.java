package com.smartcalendar.ai.util;

import android.content.Context;

import com.smartcalendar.ai.R;
import com.smartcalendar.ai.data.local.Event;

import java.util.List;

public final class ConflictText {

    private ConflictText() {}

    private static String line(Event e, long referenceStart) {
        StringBuilder sb = new StringBuilder("• ");
        if (TimeUtils.startOfDay(e.startTime) != TimeUtils.startOfDay(referenceStart)) {
            sb.append(TimeUtils.formatDate(e.startTime)).append(" ");
        }
        sb.append(TimeUtils.formatTime(e.startTime));
        if (e.endTime != null) {
            sb.append(" – ").append(TimeUtils.formatTime(e.endTime));
        }
        sb.append("  ").append(e.title);
        return sb.toString();
    }

    public static String list(Context context, List<Event> conflicts, long referenceStart, int maxLines) {
        StringBuilder sb = new StringBuilder();
        int shown = Math.min(conflicts.size(), maxLines);
        for (int i = 0; i < shown; i++) {
            if (i > 0) {
                sb.append("\n");
            }
            sb.append(line(conflicts.get(i), referenceStart));
        }
        if (conflicts.size() > shown) {
            sb.append("\n").append(context.getString(R.string.chat_more_events, conflicts.size() - shown));
        }
        return sb.toString();
    }
}
