package com.smartcalendar.ai.notification;

import android.content.Context;

import androidx.core.app.NotificationManagerCompat;

final class NotificationHelperCancel {

    private NotificationHelperCancel() {}

    static void cancel(Context context, long eventId) {
        NotificationManagerCompat.from(context).cancel((int) eventId);
    }
}
