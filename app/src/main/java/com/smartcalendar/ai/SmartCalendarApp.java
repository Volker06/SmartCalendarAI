package com.smartcalendar.ai;

import android.app.Application;

import com.smartcalendar.ai.notification.NotificationHelper;

public class SmartCalendarApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        NotificationHelper.createChannel(this);
    }
}
