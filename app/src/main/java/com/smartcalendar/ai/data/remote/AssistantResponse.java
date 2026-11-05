package com.smartcalendar.ai.data.remote;

import com.google.gson.annotations.SerializedName;

public class AssistantResponse {

    public String intent;
    public String reply;
    public EventDraft event;
    public DateRange range;
    public Target target;
    public Changes changes;

    public static class EventDraft {
        public String title;
        public String start;
        public String end;
        public String description;
        public String category;

        @SerializedName("reminder_minutes")
        public Integer reminderMinutes;
    }

    public static class DateRange {
        public String start;
        public String end;
    }

    public static class Target {
        public String keyword;
        public String start;
        public String end;
    }

    public static class Changes {
        public String title;
        public String date;
        @SerializedName("start_time")
        public String startTime;
        @SerializedName("end_time")
        public String endTime;
        @SerializedName("remove_end")
        public Boolean removeEnd;
        public String description;
        public String category;
        @SerializedName("reminder_minutes")
        public Integer reminderMinutes;
        @SerializedName("remove_reminder")
        public Boolean removeReminder;
    }
}
