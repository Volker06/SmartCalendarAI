package com.smartcalendar.ai.notification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.smartcalendar.ai.data.local.AppDatabase;
import com.smartcalendar.ai.data.local.Event;

import java.util.List;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action) && !Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            return;
        }
        PendingResult result = goAsync();
        Context appContext = context.getApplicationContext();
        AppDatabase.databaseExecutor.execute(() -> {
            try {
                List<Event> events = AppDatabase.getInstance(appContext).eventDao()
                        .getUpcomingWithReminder(System.currentTimeMillis());
                for (Event event : events) {
                    ReminderScheduler.schedule(appContext, event);
                }
            } finally {
                result.finish();
            }
        });
    }
}
