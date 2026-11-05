package com.smartcalendar.ai.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TimeUtils {

    private static final Locale VI = new Locale("vi", "VN");

    private TimeUtils() {}

    public static long startOfDay(long millis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(millis);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    public static long nextDayStart(long millis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(startOfDay(millis));
        c.add(Calendar.DAY_OF_MONTH, 1);
        return c.getTimeInMillis();
    }

    public static long startOfMonth(long millis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(startOfDay(millis));
        c.set(Calendar.DAY_OF_MONTH, 1);
        return c.getTimeInMillis();
    }

    public static long nextMonthStart(long millis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(startOfMonth(millis));
        c.add(Calendar.MONTH, 1);
        return c.getTimeInMillis();
    }

    public static long addMonths(long millis, int months) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(millis);
        c.add(Calendar.MONTH, months);
        return c.getTimeInMillis();
    }

    public static String formatMonthYear(long millis) {
        return new SimpleDateFormat("MM/yyyy", VI).format(new Date(millis));
    }

    public static String formatIsoWithOffset(long millis) {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).format(new Date(millis));
    }

    public static long parseLocalIso(String text) {
        if (text == null) {
            return -1;
        }
        String cleaned = text.trim().replaceAll("(Z|[+-]\\d{2}:?\\d{2})$", "").replaceAll("\\.\\d+$", "");
        String[] patterns = {"yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd'T'HH:mm"};
        for (String pattern : patterns) {
            try {
                SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.US);
                format.setLenient(false);
                Date date = format.parse(cleaned);
                if (date != null) {
                    return date.getTime();
                }
            } catch (ParseException ignored) {
            }
        }
        return -1;
    }

    public static int[] parseLocalDate(String text) {
        if (text == null) {
            return null;
        }
        Matcher m = Pattern.compile("^(\\d{4})-(\\d{1,2})-(\\d{1,2})").matcher(text.trim());
        if (!m.find()) {
            return null;
        }
        int year = Integer.parseInt(m.group(1));
        int month = Integer.parseInt(m.group(2));
        int day = Integer.parseInt(m.group(3));
        Calendar c = Calendar.getInstance();
        c.setLenient(false);
        try {
            c.clear();
            c.set(year, month - 1, day);
            c.getTimeInMillis();
        } catch (IllegalArgumentException e) {
            return null;
        }
        return new int[]{year, month, day};
    }

    public static int[] parseHourMinute(String text) {
        if (text == null) {
            return null;
        }
        Matcher m = Pattern.compile("^(\\d{1,2}):(\\d{2})").matcher(text.trim());
        if (!m.find()) {
            return null;
        }
        int hour = Integer.parseInt(m.group(1));
        int minute = Integer.parseInt(m.group(2));
        return (hour <= 23 && minute <= 59) ? new int[]{hour, minute} : null;
    }

    public static String formatDayMonth(long millis) {
        return new SimpleDateFormat("d 'Th'M", VI).format(new Date(millis));
    }

    public static String formatMonthTitle(long millis) {
        return new SimpleDateFormat("'Tháng' M, yyyy", VI).format(new Date(millis));
    }

    public static String formatDate(long millis) {
        return new SimpleDateFormat("dd/MM/yyyy", VI).format(new Date(millis));
    }

    public static String formatTime(long millis) {
        return new SimpleDateFormat("HH:mm", VI).format(new Date(millis));
    }

    public static String formatFullDate(long millis) {
        return new SimpleDateFormat("EEEE, dd/MM/yyyy", VI).format(new Date(millis));
    }
}
