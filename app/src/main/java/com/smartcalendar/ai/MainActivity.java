package com.smartcalendar.ai;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.smartcalendar.ai.data.local.Event;
import com.smartcalendar.ai.ui.calendar.CalendarDayAdapter;
import com.smartcalendar.ai.ui.calendar.CalendarDayAdapter.DayCell;
import com.smartcalendar.ai.ui.calendar.EventAdapter;
import com.smartcalendar.ai.ui.chat.ChatActivity;
import com.smartcalendar.ai.ui.event.DeleteConfirmDialog;
import com.smartcalendar.ai.ui.event.EventFormActivity;
import com.smartcalendar.ai.ui.event.QuickAddDialog;
import com.smartcalendar.ai.ui.search.SearchActivity;
import com.smartcalendar.ai.util.SystemBars;
import com.smartcalendar.ai.util.TimeUtils;
import com.smartcalendar.ai.viewmodel.EventViewModel;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private EventViewModel viewModel;
    private CalendarDayAdapter dayAdapter;
    private TextView tvMonthTitle;
    private TextView tvSelectedDate;
    private TextView tvQuickAdd;

    private long monthStart;
    private long selectedDay;
    private int eventCount;
    private Map<Long, List<Integer>> dayColors = new HashMap<>();

    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (!granted) {
                    Toast.makeText(this, R.string.notification_permission_denied, Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SystemBars.apply(this, findViewById(R.id.appBar), findViewById(R.id.content));

        requestNotificationPermissionIfNeeded();

        viewModel = new ViewModelProvider(this).get(EventViewModel.class);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.inflateMenu(R.menu.menu_main);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_ai) {
                startActivity(new Intent(this, ChatActivity.class));
                return true;
            }
            if (item.getItemId() == R.id.action_search) {
                startActivity(new Intent(this, SearchActivity.class));
                return true;
            }
            if (item.getItemId() == R.id.action_today) {
                long today = TimeUtils.startOfDay(System.currentTimeMillis());
                viewModel.selectDay(today);
                viewModel.showMonth(today);
                return true;
            }
            return false;
        });

        tvMonthTitle = findViewById(R.id.tvMonthTitle);
        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        tvQuickAdd = findViewById(R.id.tvQuickAdd);
        TextView tvEmpty = findViewById(R.id.tvEmpty);
        RecyclerView rvCalendar = findViewById(R.id.rvCalendar);
        RecyclerView rvEvents = findViewById(R.id.recyclerEvents);
        FloatingActionButton fabAdd = findViewById(R.id.fabAdd);

        findViewById(R.id.btnPrevMonth).setOnClickListener(v -> viewModel.showPreviousMonth());
        findViewById(R.id.btnNextMonth).setOnClickListener(v -> viewModel.showNextMonth());

        dayAdapter = new CalendarDayAdapter(day -> viewModel.selectDay(day));
        rvCalendar.setLayoutManager(new GridLayoutManager(this, 7));
        rvCalendar.setAdapter(dayAdapter);

        EventAdapter eventAdapter = new EventAdapter(
                event -> {
                    Intent intent = new Intent(this, EventFormActivity.class);
                    intent.putExtra(EventFormActivity.EXTRA_EVENT_ID, event.id);
                    startActivity(intent);
                },
                event -> DeleteConfirmDialog.show(this, event, () -> {
                    viewModel.delete(event);
                    Toast.makeText(this, R.string.toast_deleted, Toast.LENGTH_SHORT).show();
                }));
        rvEvents.setLayoutManager(new LinearLayoutManager(this));
        rvEvents.setAdapter(eventAdapter);

        viewModel.getVisibleMonth().observe(this, month -> {
            monthStart = month;
            tvMonthTitle.setText(TimeUtils.formatMonthTitle(month));
            refreshCalendar();
        });

        viewModel.getDayColors().observe(this, colors -> {
            dayColors = colors;
            refreshCalendar();
        });

        viewModel.getSelectedDay().observe(this, day -> {
            selectedDay = day;
            updateSelectedDateHeader();
            refreshCalendar();
        });

        viewModel.getEventsForSelectedDay().observe(this, events -> {
            eventAdapter.submitList(events);
            eventCount = events == null ? 0 : events.size();
            tvEmpty.setVisibility(eventCount == 0 ? View.VISIBLE : View.GONE);
            updateSelectedDateHeader();
        });

        tvQuickAdd.setOnClickListener(v -> QuickAddDialog.show(this, selectedDay, viewModel, this::addQuickEvent));

        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(this, EventFormActivity.class);
            intent.putExtra(EventFormActivity.EXTRA_DAY, selectedDay);
            startActivity(intent);
        });
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    private void addQuickEvent(String title, long startMillis, int color, String category, int reminderMinutes) {
        Event event = new Event();
        event.title = title;
        event.description = "";
        event.startTime = startMillis;
        event.endTime = null;
        event.color = color;
        event.category = category;
        event.reminderMinutes = reminderMinutes;
        viewModel.insert(event);
        Toast.makeText(this, getString(R.string.quick_add_done, TimeUtils.formatTime(startMillis)),
                Toast.LENGTH_SHORT).show();
    }

    private void updateSelectedDateHeader() {
        if (selectedDay == 0) {
            return;
        }
        String text = TimeUtils.formatFullDate(selectedDay);
        if (eventCount > 0) {
            text += " · " + getString(R.string.events_count, eventCount);
        }
        tvSelectedDate.setText(text);
        tvQuickAdd.setText(getString(R.string.quick_add_hint, TimeUtils.formatDayMonth(selectedDay)));
    }

    private void refreshCalendar() {
        if (monthStart == 0 || dayAdapter == null) {
            return;
        }
        long today = TimeUtils.startOfDay(System.currentTimeMillis());
        dayAdapter.submit(buildCells(monthStart), dayColors, selectedDay, today);
    }

    private List<DayCell> buildCells(long monthStartMillis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(monthStartMillis);
        int offset = (c.get(Calendar.DAY_OF_WEEK) + 5) % 7;
        int daysInMonth = c.getActualMaximum(Calendar.DAY_OF_MONTH);
        int rows = (offset + daysInMonth + 6) / 7;

        List<DayCell> cells = new ArrayList<>();
        for (int i = 0; i < rows * 7; i++) {
            int day = i - offset + 1;
            if (day < 1 || day > daysInMonth) {
                cells.add(new DayCell(0, 0));
            } else {
                c.setTimeInMillis(monthStartMillis);
                c.set(Calendar.DAY_OF_MONTH, day);
                cells.add(new DayCell(c.getTimeInMillis(), day));
            }
        }
        return cells;
    }
}
