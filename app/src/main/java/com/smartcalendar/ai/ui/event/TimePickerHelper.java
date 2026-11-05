package com.smartcalendar.ai.ui.event;

import android.content.Context;
import android.text.InputFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.NumberPicker;

import androidx.appcompat.app.AlertDialog;

import com.smartcalendar.ai.R;

import java.util.Locale;

public final class TimePickerHelper {

    public interface OnTimeSetListener {
        void onTimeSet(int hour, int minute);
    }

    private TimePickerHelper() {}

    public static void show(Context context, String title, int hour, int minute, OnTimeSetListener listener) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_time_picker, null);
        NumberPicker npHour = view.findViewById(R.id.npHour);
        NumberPicker npMinute = view.findViewById(R.id.npMinute);
        setup(npHour, 23, hour);
        setup(npMinute, 59, minute);

        new AlertDialog.Builder(context)
                .setTitle(title)
                .setView(view)
                .setPositiveButton(R.string.action_ok, (dialog, which) -> {
                    npHour.clearFocus();
                    npMinute.clearFocus();
                    listener.onTimeSet(npHour.getValue(), npMinute.getValue());
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private static void setup(NumberPicker picker, int max, int value) {
        picker.setMinValue(0);
        picker.setMaxValue(max);
        picker.setFormatter(v -> String.format(Locale.US, "%02d", v));
        picker.setWrapSelectorWheel(true);
        picker.setValue(value);
        View child = picker.getChildAt(0);
        if (child instanceof EditText) {
            ((EditText) child).setFilters(new InputFilter[0]);
        }
    }
}
