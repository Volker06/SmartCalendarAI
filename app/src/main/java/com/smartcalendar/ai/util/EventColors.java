package com.smartcalendar.ai.util;

import android.content.Context;
import android.graphics.Color;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;

import com.smartcalendar.ai.R;

public final class EventColors {

    public static final int[] PALETTE = {
            0xFF4285F4,
            0xFF34A853,
            0xFFEA4335,
            0xFFFBBC04
    };

    public static final int DEFAULT = PALETTE[0];

    private EventColors() {}

    public static int cardBackground(Context context, int color) {
        return ColorUtils.blendARGB(ContextCompat.getColor(context, R.color.surface), color, 0.28f);
    }

    public static int timeText(int color) {
        return ColorUtils.blendARGB(color, Color.WHITE, 0.45f);
    }
}
