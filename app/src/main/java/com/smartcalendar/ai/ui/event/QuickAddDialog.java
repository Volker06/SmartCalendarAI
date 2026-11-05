package com.smartcalendar.ai.ui.event;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.smartcalendar.ai.R;
import com.smartcalendar.ai.util.EventColors;
import com.smartcalendar.ai.util.TimeUtils;
import com.smartcalendar.ai.viewmodel.EventViewModel;

import java.util.Calendar;

public final class QuickAddDialog {

    public interface OnSaveListener {
        void onSave(String title, long startMillis, int color, String category, int reminderMinutes);
    }

    private QuickAddDialog() {}

    public static void show(Activity activity, long dayStart, EventViewModel viewModel, OnSaveListener listener) {
        BottomSheetDialog dialog = new BottomSheetDialog(activity);
        View view = LayoutInflater.from(activity).inflate(R.layout.dialog_quick_add, null);
        dialog.setContentView(view);

        TextView tvDay = view.findViewById(R.id.tvQuickDay);
        EditText etTitle = view.findViewById(R.id.etQuickTitle);
        MaterialButton btnTime = view.findViewById(R.id.btnQuickTime);
        LinearLayout colorRow = view.findViewById(R.id.colorQuickRow);
        ChipGroup chipCategory = view.findViewById(R.id.chipQuickCategory);
        ChipGroup chipReminder = view.findViewById(R.id.chipQuickReminder);
        MaterialButton btnSave = view.findViewById(R.id.btnQuickSave);

        int defaultHour = 9;
        if (dayStart == TimeUtils.startOfDay(System.currentTimeMillis())) {
            defaultHour = Math.min(23, Calendar.getInstance().get(Calendar.HOUR_OF_DAY) + 1);
        }
        Calendar start = Calendar.getInstance();
        start.setTimeInMillis(dayStart);
        start.set(Calendar.HOUR_OF_DAY, defaultHour);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        start.set(Calendar.MILLISECOND, 0);

        tvDay.setText(TimeUtils.formatFullDate(dayStart));
        btnTime.setText(TimeUtils.formatTime(start.getTimeInMillis()));
        btnTime.setOnClickListener(v -> {
            etTitle.clearFocus();
            TimePickerHelper.show(activity, activity.getString(R.string.title_start_time),
                    start.get(Calendar.HOUR_OF_DAY), start.get(Calendar.MINUTE),
                    (hour, minute) -> {
                        start.set(Calendar.HOUR_OF_DAY, hour);
                        start.set(Calendar.MINUTE, minute);
                        btnTime.setText(TimeUtils.formatTime(start.getTimeInMillis()));
                    });
        });

        final int[] selectedColor = {EventColors.DEFAULT};
        for (int color : EventColors.PALETTE) {
            View swatch = LayoutInflater.from(activity).inflate(R.layout.item_color_swatch, colorRow, false);
            swatch.findViewById(R.id.swatchCircle).setBackgroundTintList(ColorStateList.valueOf(color));
            swatch.setOnClickListener(v -> {
                selectedColor[0] = color;
                updateColorSelection(colorRow, selectedColor[0]);
            });
            colorRow.addView(swatch);
        }
        updateColorSelection(colorRow, selectedColor[0]);

        String[] categories = activity.getResources().getStringArray(R.array.categories);
        String[] reminderLabels = activity.getResources().getStringArray(R.array.reminder_labels);
        int[] reminderValues = activity.getResources().getIntArray(R.array.reminder_values);
        setupChips(activity, chipCategory, categories);
        setupChips(activity, chipReminder, reminderLabels);

        btnSave.setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            if (title.isEmpty()) {
                etTitle.setError(activity.getString(R.string.error_title_required));
                return;
            }
            String category = categories[selectedIndex(chipCategory)];
            int reminder = reminderValues[selectedIndex(chipReminder)];
            long startMillis = start.getTimeInMillis();
            int color = selectedColor[0];
            Runnable commit = () -> {
                listener.onSave(title, startMillis, color, category, reminder);
                dialog.dismiss();
            };

            btnSave.setEnabled(false);
            viewModel.findConflicts(startMillis, null, 0, conflicts -> activity.runOnUiThread(() -> {
                btnSave.setEnabled(true);
                if (conflicts.isEmpty()) {
                    commit.run();
                } else {
                    ConflictDialog.show(activity, conflicts, startMillis, commit);
                }
            }));
        });
        etTitle.setOnEditorActionListener((v, actionId, keyEvent) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                btnSave.performClick();
                return true;
            }
            return false;
        });

        dialog.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
        dialog.getBehavior().setSkipCollapsed(true);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
                            | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
        dialog.show();
        etTitle.requestFocus();
    }

    private static void updateColorSelection(LinearLayout colorRow, int selectedColor) {
        for (int i = 0; i < colorRow.getChildCount(); i++) {
            boolean selected = EventColors.PALETTE[i] == selectedColor;
            colorRow.getChildAt(i).findViewById(R.id.swatchCheck)
                    .setVisibility(selected ? View.VISIBLE : View.INVISIBLE);
        }
    }

    private static void setupChips(Activity activity, ChipGroup group, String[] labels) {
        for (int i = 0; i < labels.length; i++) {
            Chip chip = (Chip) LayoutInflater.from(activity).inflate(R.layout.item_choice_chip, group, false);
            chip.setId(View.generateViewId());
            chip.setText(labels[i]);
            chip.setTag(i);
            group.addView(chip);
        }
        ((Chip) group.getChildAt(0)).setChecked(true);
    }

    private static int selectedIndex(ChipGroup group) {
        int id = group.getCheckedChipId();
        if (id == View.NO_ID) {
            return 0;
        }
        Chip chip = group.findViewById(id);
        return (Integer) chip.getTag();
    }
}
