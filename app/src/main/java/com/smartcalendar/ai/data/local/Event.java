package com.smartcalendar.ai.data.local;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.smartcalendar.ai.util.EventColors;

@Entity(tableName = "events")
public class Event {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public String title;
    public String description;

    public long startTime;

    public Long endTime;

    public String category;

    public int color = EventColors.DEFAULT;

    public int reminderMinutes = -1;

    public Event copy() {
        Event e = new Event();
        e.id = id;
        e.title = title;
        e.description = description;
        e.startTime = startTime;
        e.endTime = endTime;
        e.color = color;
        e.category = category;
        e.reminderMinutes = reminderMinutes;
        return e;
    }
}
