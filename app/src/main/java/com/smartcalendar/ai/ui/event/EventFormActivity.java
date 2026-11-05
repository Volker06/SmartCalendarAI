package com.smartcalendar.ai.ui.event;

import android.app.DatePickerDialog;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.smartcalendar.ai.R;
import com.smartcalendar.ai.data.local.Event;
import com.smartcalendar.ai.util.EventColors;
import com.smartcalendar.ai.util.SystemBars;
import com.smartcalendar.ai.util.TimeUtils;
import com.smartcalendar.ai.viewmodel.EventViewModel;

import java.util.Calendar;

public class EventFormActivity extends AppCompatActivity {

    public static final String EXTRA_EVENT_ID = "event_id";
    public static final String EXTRA_DAY = "day";
    public static final String EXTRA_TITLE = "title";

    private static final long ONE_HOUR = 60L * 60L * 1000L;

    private EventViewModel viewModel;

    private EditText etTitle;
    private EditText etDescription;
    private LinearLayout colorRow;
    private int selectedColor = EventColors.DEFAULT;
    private ChipGroup chipCategory;
    private ChipGroup chipReminder;
    private MaterialButton btnDate;
    private MaterialButton btnTime;
    private MaterialButton btnEndTime;
    private MaterialButton btnClearEnd;
    private MaterialButton btnDelete;
    private MaterialButton btnSave;

    private final Calendar start = Calendar.getInstance();

    private boolean hasEnd = false;
    private int endHour;
    private int endMinute;

    private long eventId = 0;
    private Event loadedEvent;
    private boolean loaded = false;
    private int[] reminderValues;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_form);
        SystemBars.apply(this, findViewById(R.id.appBar), findViewById(R.id.content));

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        viewModel = new ViewModelProvider(this).get(EventViewModel.class);

        etTitle = findViewById(R.id.etTitle);
        etDescription = findViewById(R.id.etDescription);
        chipCategory = findViewById(R.id.chipCategory);
        chipReminder = findViewById(R.id.chipReminder);
        btnDate = findViewById(R.id.btnDate);
        btnTime = findViewById(R.id.btnTime);
        btnEndTime = findViewById(R.id.btnEndTime);
        btnClearEnd = findViewById(R.id.btnClearEnd);
        btnDelete = findViewById(R.id.btnDelete);
        btnSave = findViewById(R.id.btnSave);

        colorRow = findViewById(R.id.colorRow);
        setupColorPicker();

        reminderValues = getResources().getIntArray(R.array.reminder_values);
        setupChips(chipCategory, getResources().getStringArray(R.array.categories));
        setupChips(chipReminder, getResources().getStringArray(R.array.reminder_labels));

        eventId = getIntent().getLongExtra(EXTRA_EVENT_ID, 0);

        if (eventId == 0) {
            toolbar.setTitle(R.string.title_add_event);
            long day = getIntent().getLongExtra(EXTRA_DAY, System.currentTimeMillis());
            start.setTimeInMillis(TimeUtils.startOfDay(day));
            start.set(Calendar.HOUR_OF_DAY, 9);
            String presetTitle = getIntent().getStringExtra(EXTRA_TITLE);
            if (presetTitle != null) {
                etTitle.setText(presetTitle);
            }
            btnDelete.setVisibility(View.GONE);
            updateButtons();
        } else {
            toolbar.setTitle(R.string.title_edit_event);
            viewModel.getEventById(eventId).observe(this, event -> {
                if (event != null && !loaded) {
                    loaded = true;
                    loadedEvent = event;
                    populate(event);
                }
            });
        }

        btnDate.setOnClickListener(v -> pickDate());
        btnTime.setOnClickListener(v -> pickStartTime());
        btnEndTime.setOnClickListener(v -> pickEndTime());
        btnClearEnd.setOnClickListener(v -> {
            hasEnd = false;
            updateButtons();
        });
        btnSave.setOnClickListener(v -> save());
        btnDelete.setOnClickListener(v -> confirmDelete());
    }

    private void setupChips(ChipGroup group, String[] labels) {
        for (int i = 0; i < labels.length; i++) {
            Chip chip = (Chip) getLayoutInflater().inflate(R.layout.item_choice_chip, group, false);
            chip.setId(View.generateViewId());
            chip.setText(labels[i]);
            chip.setTag(i);
            group.addView(chip);
        }
        selectIndex(group, 0);
    }

    private int selectedIndex(ChipGroup group) {
        int id = group.getCheckedChipId();
        if (id == View.NO_ID) {
            return 0;
        }
        Chip chip = group.findViewById(id);
        return (Integer) chip.getTag();
    }

    private void selectIndex(ChipGroup group, int index) {
        View child = group.getChildAt(index);
        if (child instanceof Chip) {
            ((Chip) child).setChecked(true);
        }
    }

    private void setupColorPicker() {
        for (int color : EventColors.PALETTE) {
            View swatch = getLayoutInflater().inflate(R.layout.item_color_swatch, colorRow, false);
            swatch.findViewById(R.id.swatchCircle).setBackgroundTintList(ColorStateList.valueOf(color));
            swatch.setOnClickListener(v -> {
                selectedColor = color;
                updateColorSelection();
            });
            colorRow.addView(swatch);
        }
        updateColorSelection();
    }

    private void updateColorSelection() {
        for (int i = 0; i < colorRow.getChildCount(); i++) {
            boolean selected = EventColors.PALETTE[i] == selectedColor;
            colorRow.getChildAt(i).findViewById(R.id.swatchCheck)
                    .setVisibility(selected ? View.VISIBLE : View.INVISIBLE);
        }
    }

    private void populate(Event event) {
        selectedColor = event.color;
        updateColorSelection();
        etTitle.setText(event.title);
        etDescription.setText(event.description);
        start.setTimeInMillis(event.startTime);

        if (event.endTime != null) {
            Calendar end = Calendar.getInstance();
            end.setTimeInMillis(event.endTime);
            hasEnd = true;
            endHour = end.get(Calendar.HOUR_OF_DAY);
            endMinute = end.get(Calendar.MINUTE);
        }

        String[] categories = getResources().getStringArray(R.array.categories);
        for (int i = 0; i < categories.length; i++) {
            if (categories[i].equals(event.category)) {
                selectIndex(chipCategory, i);
                break;
            }
        }
        for (int i = 0; i < reminderValues.length; i++) {
            if (reminderValues[i] == event.reminderMinutes) {
                selectIndex(chipReminder, i);
                break;
            }
        }
        updateButtons();
    }

    private long computeEndMillis() {
        Calendar end = (Calendar) start.clone();
        end.set(Calendar.HOUR_OF_DAY, endHour);
        end.set(Calendar.MINUTE, endMinute);
        end.set(Calendar.SECOND, 0);
        end.set(Calendar.MILLISECOND, 0);
        if (!end.after(start)) {
            end.add(Calendar.DAY_OF_MONTH, 1);
        }
        return end.getTimeInMillis();
    }

    private void updateButtons() {
        btnDate.setText(TimeUtils.formatFullDate(start.getTimeInMillis()));
        btnTime.setText(TimeUtils.formatTime(start.getTimeInMillis()));

        if (hasEnd) {
            long endMillis = computeEndMillis();
            boolean nextDay = TimeUtils.startOfDay(endMillis) != TimeUtils.startOfDay(start.getTimeInMillis());
            btnEndTime.setText(getString(nextDay ? R.string.end_time_next_day : R.string.end_time_set,
                    TimeUtils.formatTime(endMillis)));
            btnClearEnd.setVisibility(View.VISIBLE);
        } else {
            btnEndTime.setText(R.string.end_time_add);
            btnClearEnd.setVisibility(View.GONE);
        }
    }

    private void pickDate() {
        new DatePickerDialog(this, (view, year, month, day) -> {
            start.set(year, month, day);
            updateButtons();
        }, start.get(Calendar.YEAR), start.get(Calendar.MONTH), start.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void pickStartTime() {
        TimePickerHelper.show(this, getString(R.string.title_start_time),
                start.get(Calendar.HOUR_OF_DAY), start.get(Calendar.MINUTE),
                (hour, minute) -> {
                    long duration = hasEnd ? computeEndMillis() - start.getTimeInMillis() : 0;
                    start.set(Calendar.HOUR_OF_DAY, hour);
                    start.set(Calendar.MINUTE, minute);
                    start.set(Calendar.SECOND, 0);
                    start.set(Calendar.MILLISECOND, 0);
                    if (hasEnd) {
                        Calendar end = Calendar.getInstance();
                        end.setTimeInMillis(start.getTimeInMillis() + duration);
                        endHour = end.get(Calendar.HOUR_OF_DAY);
                        endMinute = end.get(Calendar.MINUTE);
                    }
                    updateButtons();
                });
    }

    private void pickEndTime() {
        int hour = endHour;
        int minute = endMinute;
        if (!hasEnd) {
            Calendar suggestion = Calendar.getInstance();
            suggestion.setTimeInMillis(start.getTimeInMillis() + ONE_HOUR);
            hour = suggestion.get(Calendar.HOUR_OF_DAY);
            minute = suggestion.get(Calendar.MINUTE);
        }
        TimePickerHelper.show(this, getString(R.string.title_end_time), hour, minute,
                (h, m) -> {
                    hasEnd = true;
                    endHour = h;
                    endMinute = m;
                    updateButtons();
                });
    }

    private void save() {
        String title = etTitle.getText().toString().trim();
        if (title.isEmpty()) {
            etTitle.setError(getString(R.string.error_title_required));
            return;
        }

        Event event = new Event();
        event.id = eventId;
        event.title = title;
        event.description = etDescription.getText().toString().trim();
        event.startTime = start.getTimeInMillis();
        event.endTime = hasEnd ? computeEndMillis() : null;
        event.color = selectedColor;
        event.category = getResources().getStringArray(R.array.categories)[selectedIndex(chipCategory)];
        event.reminderMinutes = reminderValues[selectedIndex(chipReminder)];

        btnSave.setEnabled(false);
        viewModel.findConflicts(event.startTime, event.endTime, eventId, conflicts -> runOnUiThread(() -> {
            btnSave.setEnabled(true);
            if (conflicts.isEmpty()) {
                commit(event);
            } else {
                ConflictDialog.show(this, conflicts, event.startTime, () -> commit(event));
            }
        }));
    }

    private void commit(Event event) {
        if (eventId == 0) {
            viewModel.insert(event);
        } else {
            viewModel.update(event);
        }
        finish();
    }

    private void confirmDelete() {
        if (loadedEvent == null) {
            return;
        }
        DeleteConfirmDialog.show(this, loadedEvent, () -> {
            viewModel.delete(loadedEvent);
            finish();
        });
    }
}
