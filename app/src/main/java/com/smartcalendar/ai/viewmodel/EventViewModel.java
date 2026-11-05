package com.smartcalendar.ai.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.smartcalendar.ai.data.local.Event;
import com.smartcalendar.ai.data.repository.EventRepository;
import com.smartcalendar.ai.util.TimeUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventViewModel extends AndroidViewModel {

    private final EventRepository repository;

    private final MutableLiveData<Long> selectedDay = new MutableLiveData<>();
    private final MediatorLiveData<List<Event>> eventsForSelectedDay = new MediatorLiveData<>();
    private LiveData<List<Event>> daySource;

    private final MutableLiveData<Long> visibleMonth = new MutableLiveData<>();
    private final MediatorLiveData<Map<Long, List<Integer>>> dayColors = new MediatorLiveData<>();
    private LiveData<List<Event>> monthSource;

    public EventViewModel(@NonNull Application application) {
        super(application);
        repository = new EventRepository(application);
        long today = TimeUtils.startOfDay(System.currentTimeMillis());
        selectDay(today);
        showMonth(today);
    }

    public void selectDay(long dayStartMillis) {
        selectedDay.setValue(dayStartMillis);
        if (daySource != null) {
            eventsForSelectedDay.removeSource(daySource);
        }
        daySource = repository.getEventsForDay(dayStartMillis, TimeUtils.nextDayStart(dayStartMillis));
        eventsForSelectedDay.addSource(daySource, eventsForSelectedDay::setValue);
    }

    public void showMonth(long anyMillisInMonth) {
        long monthStart = TimeUtils.startOfMonth(anyMillisInMonth);
        long monthEnd = TimeUtils.nextMonthStart(monthStart);
        visibleMonth.setValue(monthStart);
        if (monthSource != null) {
            dayColors.removeSource(monthSource);
        }
        monthSource = repository.getEventsForDay(monthStart, monthEnd);
        dayColors.addSource(monthSource,
                events -> dayColors.setValue(computeDayColors(events, monthStart, monthEnd)));
    }

    public void showPreviousMonth() {
        Long month = visibleMonth.getValue();
        if (month != null) {
            showMonth(month - 1);
        }
    }

    public void showNextMonth() {
        Long month = visibleMonth.getValue();
        if (month != null) {
            showMonth(TimeUtils.nextMonthStart(month));
        }
    }

    private static Map<Long, List<Integer>> computeDayColors(List<Event> events, long monthStart, long monthEnd) {
        Map<Long, List<Integer>> map = new HashMap<>();
        if (events == null) {
            return map;
        }
        for (Event e : events) {
            long firstDay = TimeUtils.startOfDay(e.startTime);
            long lastDay = (e.endTime != null && e.endTime > e.startTime)
                    ? TimeUtils.startOfDay(e.endTime - 1)
                    : firstDay;
            long day = firstDay;
            int guard = 0;
            while (day <= lastDay && guard++ < 400) {
                if (day >= monthStart && day < monthEnd) {
                    map.computeIfAbsent(day, k -> new ArrayList<>()).add(e.color);
                }
                day = TimeUtils.nextDayStart(day);
            }
        }
        return map;
    }

    public LiveData<Long> getSelectedDay() {
        return selectedDay;
    }

    public LiveData<List<Event>> getEventsForSelectedDay() {
        return eventsForSelectedDay;
    }

    public LiveData<Long> getVisibleMonth() {
        return visibleMonth;
    }

    public LiveData<Map<Long, List<Integer>>> getDayColors() {
        return dayColors;
    }

    public LiveData<Event> getEventById(long id) {
        return repository.getEventById(id);
    }

    public void findConflicts(long start, Long end, long excludeId, EventRepository.ResultCallback<List<Event>> callback) {
        repository.findConflicts(start, end, excludeId, callback);
    }

    public void insert(Event event) {
        repository.insert(event);
    }

    public void update(Event event) {
        repository.update(event);
    }

    public void delete(Event event) {
        repository.delete(event);
    }
}
