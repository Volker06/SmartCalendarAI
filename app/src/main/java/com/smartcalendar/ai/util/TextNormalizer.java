package com.smartcalendar.ai.util;

import java.text.Normalizer;
import java.util.Locale;

public final class TextNormalizer {

    private TextNormalizer() {}

    public static String normalize(String text) {
        if (text == null) {
            return "";
        }
        String s = text.replace('đ', 'd').replace('Đ', 'D');
        s = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return s.toLowerCase(Locale.ROOT).trim();
    }

    public static boolean matchesAllWords(String title, String keyword) {
        String k = normalize(keyword);
        if (k.isEmpty()) {
            return true;
        }
        String t = normalize(title);
        for (String word : k.split("\\s+")) {
            if (!t.contains(word)) {
                return false;
            }
        }
        return true;
    }
}
