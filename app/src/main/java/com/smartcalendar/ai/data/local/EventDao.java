package com.smartcalendar.ai.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface EventDao {

    @Insert
    long insert(Event event);

    @Update
    void update(Event event);

    @Delete
    void delete(Event event);

    @Query("SELECT * FROM events WHERE id = :id")
    LiveData<Event> getById(long id);

    @Query("SELECT * FROM events WHERE "
            + "(endTime IS NULL AND startTime >= :dayStart AND startTime < :dayEnd) "
            + "OR (endTime IS NOT NULL AND startTime < :dayEnd AND endTime > :dayStart) "
            + "ORDER BY startTime ASC")
    LiveData<List<Event>> getEventsBetween(long dayStart, long dayEnd);

    @Query("SELECT * FROM events WHERE COALESCE(endTime, startTime) >= :now ORDER BY startTime ASC LIMIT :limit")
    LiveData<List<Event>> getUpcoming(long now, int limit);

    @Query("SELECT * FROM events WHERE title LIKE '%' || :keyword || '%' "
            + "OR description LIKE '%' || :keyword || '%' ORDER BY startTime DESC")
    LiveData<List<Event>> search(String keyword);

    @Query("SELECT * FROM events WHERE startTime < :rangeEnd "
            + "AND COALESCE(endTime, startTime + 1) > :rangeStart "
            + "AND (:color = 0 OR color = :color) "
            + "AND (:keyword = '' OR title LIKE '%' || :keyword || '%' "
            + "OR description LIKE '%' || :keyword || '%') "
            + "ORDER BY startTime ASC")
    LiveData<List<Event>> searchEvents(long rangeStart, long rangeEnd, int color, String keyword);

    @Query("SELECT * FROM events WHERE reminderMinutes >= 0 AND startTime > :now")
    List<Event> getUpcomingWithReminder(long now);

    @Query("SELECT * FROM events WHERE startTime < :to AND COALESCE(endTime, startTime + 1) > :from ORDER BY startTime ASC")
    List<Event> getEventsInRange(long from, long to);

    @Query("SELECT * FROM events WHERE startTime < :to AND COALESCE(endTime, startTime + 1) > :from "
            + "AND id != :excludeId ORDER BY startTime ASC")
    List<Event> getConflicts(long from, long to, long excludeId);
}
