package com.smartcalendar.ai.notification;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.TaskStackBuilder;
import androidx.core.content.ContextCompat;

import com.smartcalendar.ai.R;
import com.smartcalendar.ai.ui.event.EventFormActivity;
import com.smartcalendar.ai.util.TimeUtils;

public final class NotificationHelper {

    public static final String CHANNEL_ID = "event_reminders";

    private NotificationHelper() {}

    public static void createChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.notif_channel_name),
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription(context.getString(R.string.notif_channel_desc));
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    public static void show(Context context, long eventId, String title, long startTime, int reminderMinutes) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        createChannel(context);

        Intent formIntent = new Intent(context, EventFormActivity.class);
        formIntent.putExtra(EventFormActivity.EXTRA_EVENT_ID, eventId);
        PendingIntent contentIntent = TaskStackBuilder.create(context)
                .addNextIntentWithParentStack(formIntent)
                .getPendingIntent((int) eventId, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String amount = reminderMinutes % 60 == 0 && reminderMinutes > 0
                ? context.getString(R.string.reminder_fmt_hours, reminderMinutes / 60)
                : context.getString(R.string.reminder_fmt_minutes, reminderMinutes);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(context.getString(R.string.notif_text, TimeUtils.formatTime(startTime), amount))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_EVENT)
                .setAutoCancel(true)
                .setContentIntent(contentIntent);

        try {
            NotificationManagerCompat.from(context).notify((int) eventId, builder.build());
        } catch (SecurityException ignored) {
        }
    }
}
