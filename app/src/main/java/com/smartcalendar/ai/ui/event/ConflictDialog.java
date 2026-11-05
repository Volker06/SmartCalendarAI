package com.smartcalendar.ai.ui.event;

import android.content.Context;

import androidx.appcompat.app.AlertDialog;

import com.smartcalendar.ai.R;
import com.smartcalendar.ai.data.local.Event;
import com.smartcalendar.ai.util.ConflictText;

import java.util.List;

public final class ConflictDialog {

    private ConflictDialog() {}

    public static void show(Context context, List<Event> conflicts, long referenceStart, Runnable onSaveAnyway) {
        new AlertDialog.Builder(context)
                .setTitle(R.string.conflict_title)
                .setMessage(context.getString(R.string.conflict_message,
                        ConflictText.list(context, conflicts, referenceStart, 4)))
                .setPositiveButton(R.string.conflict_save_anyway, (dialog, which) -> onSaveAnyway.run())
                .setNegativeButton(R.string.conflict_change, null)
                .show();
    }
}
