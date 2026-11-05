package com.smartcalendar.ai.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import com.smartcalendar.ai.data.local.Event;
import com.smartcalendar.ai.data.repository.EventRepository;

import java.util.Collections;
import java.util.List;

public class SearchViewModel extends AndroidViewModel {

    private final EventRepository repository;
    private final MediatorLiveData<List<Event>> results = new MediatorLiveData<>();
    private LiveData<List<Event>> source;

    public SearchViewModel(@NonNull Application application) {
        super(application);
        repository = new EventRepository(application);
    }

    public void search(long rangeStart, long rangeEnd, int color, String keyword) {
        removeSource();
        source = repository.searchEvents(rangeStart, rangeEnd, color, keyword);
        results.addSource(source, results::setValue);
    }

    public void clear() {
        removeSource();
        results.setValue(Collections.emptyList());
    }

    private void removeSource() {
        if (source != null) {
            results.removeSource(source);
            source = null;
        }
    }

    public LiveData<List<Event>> getResults() {
        return results;
    }

    public void delete(Event event) {
        repository.delete(event);
    }
}
