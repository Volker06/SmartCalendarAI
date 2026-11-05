package com.smartcalendar.ai.notification;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;

import com.smartcalendar.ai.data.local.Event;

public final class ReminderScheduler {

    public static final String ACTION_REMINDER = "com.smartcalendar.ai.ACTION_REMINDER";
    public static final String EXTRA_EVENT_ID = "event_id";
    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_START = "start";
    public static final String EXTRA_REMINDER = "reminder";

    private ReminderScheduler() {}

    public static void schedule(Context context, Event event) {
        cancel(context, event.id);
        if (event.reminderMinutes < 0) {
            return;
        }
        long triggerAt = event.startTime - event.reminderMinutes * 60_000L;
        if (triggerAt <= System.currentTimeMillis()) {
            return;
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }

        Intent intent = baseIntent(context, event.id);
        intent.putExtra(EXTRA_EVENT_ID, event.id);
        intent.putExtra(EXTRA_TITLE, event.title);
        intent.putExtra(EXTRA_START, event.startTime);
        intent.putExtra(EXTRA_REMINDER, event.reminderMinutes);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, (int) event.id, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent);
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent);
            }
        } catch (SecurityException e) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent);
        }
    }

    public static void cancel(Context context, long eventId) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, (int) eventId, baseIntent(context, eventId),
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
        }
        NotificationHelperCancel.cancel(context, eventId);
    }

    private static Intent baseIntent(Context context, long eventId) {
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.setAction(ACTION_REMINDER);
        intent.setData(Uri.parse("smartcalendar://event/" + eventId));
        return intent;
    }
}
