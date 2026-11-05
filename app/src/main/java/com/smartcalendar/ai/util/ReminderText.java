package com.smartcalendar.ai.util;

import android.content.Context;

import com.smartcalendar.ai.R;

public final class ReminderText {

    private ReminderText() {}

    public static String format(Context context, int minutes) {
        String amount = minutes > 0 && minutes % 60 == 0
                ? context.getString(R.string.reminder_fmt_hours, minutes / 60)
                : context.getString(R.string.reminder_fmt_minutes, minutes);
        return context.getString(R.string.reminder_before, amount);
    }
}
