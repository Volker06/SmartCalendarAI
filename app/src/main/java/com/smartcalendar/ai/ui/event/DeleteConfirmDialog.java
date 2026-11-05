package com.smartcalendar.ai.ui.event;

import android.content.Context;

import androidx.appcompat.app.AlertDialog;

import com.smartcalendar.ai.R;
import com.smartcalendar.ai.data.local.Event;

public final class DeleteConfirmDialog {

    private DeleteConfirmDialog() {}

    public static void show(Context context, Event event, Runnable onConfirm) {
        new AlertDialog.Builder(context)
                .setTitle(R.string.dialog_delete_title)
                .setMessage(context.getString(R.string.dialog_delete_message_named, event.title))
                .setPositiveButton(R.string.action_delete, (dialog, which) -> onConfirm.run())
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }
}
