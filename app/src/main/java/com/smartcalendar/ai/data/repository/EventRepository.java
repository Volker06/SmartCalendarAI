package com.smartcalendar.ai.data.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.smartcalendar.ai.data.local.AppDatabase;
import com.smartcalendar.ai.data.local.Event;
import com.smartcalendar.ai.data.local.EventDao;
import com.smartcalendar.ai.notification.ReminderScheduler;

import java.util.List;

public class EventRepository {

    public interface ResultCallback<T> {
        void onResult(T value);
    }

    private final Application application;
    private final EventDao dao;

    public EventRepository(Application application) {
        this.application = application;
        dao = AppDatabase.getInstance(application).eventDao();
    }

    public LiveData<List<Event>> getEventsForDay(long dayStart, long dayEnd) {
        return dao.getEventsBetween(dayStart, dayEnd);
    }

    public LiveData<Event> getEventById(long id) {
        return dao.getById(id);
    }

    public LiveData<List<Event>> getUpcoming(long now, int limit) {
        return dao.getUpcoming(now, limit);
    }

    public LiveData<List<Event>> search(String keyword) {
        return dao.search(keyword);
    }

    public LiveData<List<Event>> searchEvents(long rangeStart, long rangeEnd, int color, String keyword) {
        return dao.searchEvents(rangeStart, rangeEnd, color, keyword);
    }

    public void getEventsInRange(long from, long to, ResultCallback<List<Event>> callback) {
        AppDatabase.databaseExecutor.execute(() -> callback.onResult(dao.getEventsInRange(from, to)));
    }

    public void findConflicts(long start, Long end, long excludeId, ResultCallback<List<Event>> callback) {
        long to = (end != null && end > start) ? end : start + 1;
        AppDatabase.databaseExecutor.execute(() -> callback.onResult(dao.getConflicts(start, to, excludeId)));
    }

    public void insert(Event event) {
        AppDatabase.databaseExecutor.execute(() -> {
            event.id = dao.insert(event);
            ReminderScheduler.schedule(application, event);
        });
    }

    public void update(Event event) {
        AppDatabase.databaseExecutor.execute(() -> {
            dao.update(event);
            ReminderScheduler.schedule(application, event);
        });
    }

    public void delete(Event event) {
        AppDatabase.databaseExecutor.execute(() -> {
            dao.delete(event);
            ReminderScheduler.cancel(application, event.id);
        });
    }
}
