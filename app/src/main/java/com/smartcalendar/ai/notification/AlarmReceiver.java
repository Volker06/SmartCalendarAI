package com.smartcalendar.ai.notification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        long eventId = intent.getLongExtra(ReminderScheduler.EXTRA_EVENT_ID, 0);
        String title = intent.getStringExtra(ReminderScheduler.EXTRA_TITLE);
        long startTime = intent.getLongExtra(ReminderScheduler.EXTRA_START, 0);
        int reminder = intent.getIntExtra(ReminderScheduler.EXTRA_REMINDER, 0);
        if (eventId == 0 || title == null) {
            return;
        }
        NotificationHelper.show(context, eventId, title, startTime, reminder);
    }
}
