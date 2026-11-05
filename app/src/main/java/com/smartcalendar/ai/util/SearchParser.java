package com.smartcalendar.ai.util;

import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SearchParser {

    public static final class Result {
        public final String keyword;
        public final boolean hasDate;
        public final boolean hasMonth;
        public final boolean invalidDate;
        public final int day;
        public final int month;
        public final int year;

        Result(String keyword, boolean hasDate, boolean hasMonth, boolean invalidDate, int day, int month, int year) {
            this.keyword = keyword;
            this.hasDate = hasDate;
            this.hasMonth = hasMonth;
            this.invalidDate = invalidDate;
            this.day = day;
            this.month = month;
            this.year = year;
        }
    }

    private static final Pattern DATE_DMY = Pattern.compile(
            "(?<!\\p{L})(?:ng[àa]y)?\\s*(?<!\\d)(\\d{1,2})\\s*[/\\-.]\\s*(\\d{1,2})\\s*[/\\-.]\\s*(\\d{4})(?!\\d)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private static final Pattern DATE_ISO = Pattern.compile("(?<!\\d)(\\d{4})-(\\d{1,2})-(\\d{1,2})(?!\\d)");

    private static final Pattern MONTH_PATTERN = Pattern.compile(
            "(?<!\\p{L})(?:th[áa]ng|th|t)?\\s*(?<!\\d)(\\d{1,2})\\s*[/\\-.\\s]\\s*(\\d{4})(?!\\d)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private SearchParser() {}

    public static Result parse(String input) {
        String text = input == null ? "" : input.trim();

        Matcher dmy = DATE_DMY.matcher(text);
        if (dmy.find()) {
            return dateResult(text, dmy,
                    Integer.parseInt(dmy.group(1)), Integer.parseInt(dmy.group(2)), Integer.parseInt(dmy.group(3)));
        }

        Matcher iso = DATE_ISO.matcher(text);
        if (iso.find()) {
            return dateResult(text, iso,
                    Integer.parseInt(iso.group(3)), Integer.parseInt(iso.group(2)), Integer.parseInt(iso.group(1)));
        }

        Matcher m = MONTH_PATTERN.matcher(text);
        while (m.find()) {
            int month = Integer.parseInt(m.group(1));
            int year = Integer.parseInt(m.group(2));
            if (month >= 1 && month <= 12) {
                return new Result(remove(text, m), false, true, false, 0, month, year);
            }
        }
        return new Result(text, false, false, false, 0, 0, 0);
    }

    private static Result dateResult(String text, Matcher m, int day, int month, int year) {
        if (!isValidDate(day, month, year)) {
            return new Result(text, false, false, true, 0, 0, 0);
        }
        return new Result(remove(text, m), true, false, false, day, month, year);
    }

    private static String remove(String text, Matcher m) {
        return (text.substring(0, m.start()) + " " + text.substring(m.end()))
                .replaceAll("\\s+", " ").trim();
    }

    private static boolean isValidDate(int day, int month, int year) {
        Calendar c = Calendar.getInstance();
        c.setLenient(false);
        c.clear();
        try {
            c.set(year, month - 1, day);
            c.getTimeInMillis();
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
