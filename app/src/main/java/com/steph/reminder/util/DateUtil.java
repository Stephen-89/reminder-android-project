package com.steph.reminder.util;

import android.widget.Switch;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DateUtil {

    public static long calculateNotificationTime(long reminderTime, String timeForNotification) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(reminderTime);
        switch (timeForNotification) {
            case "30 minutes before reminder":
                calendar.add(Calendar.MINUTE, -30);  // Subtract 30 minutes
                break;
            case "1 hour before reminder":
                calendar.add(Calendar.HOUR, -1);  // Subtract 1 hour
                break;
            case "Day of reminder at 09:00 AM":
                calendar.set(Calendar.HOUR_OF_DAY, 9);  // Set time to 09:00 AM
                calendar.set(Calendar.MINUTE, 0);  // Set minutes to 0
                calendar.set(Calendar.SECOND, 0);  // Set seconds to 0
                calendar.set(Calendar.MILLISECOND, 0);  // Set milliseconds to 0
                break;
            case "Day before reminder at 09:00 AM":
                calendar.add(Calendar.DAY_OF_YEAR, -1);  // Subtract 1 day
                calendar.set(Calendar.HOUR_OF_DAY, 9);  // Set time to 09:00 AM
                calendar.set(Calendar.MINUTE, 0);  // Set minutes to 0
                calendar.set(Calendar.SECOND, 0);  // Set seconds to 0
                calendar.set(Calendar.MILLISECOND, 0);  // Set milliseconds to 0
                break;
            default:
                // Default case is "Time of reminder", do nothing
                break;
        }
        return calendar.getTimeInMillis();
    }

    public static void setDefaultDateTime(TextView dateFrom, TextView dateTo, TextView timeFrom, TextView timeTo) {
        Calendar calendar = Calendar.getInstance();
        String defaultDate = String.format(Locale.getDefault(), "%02d/%02d/%04d",
                calendar.get(Calendar.DAY_OF_MONTH),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.YEAR));
        String defaultTime = String.format(Locale.getDefault(), "%02d:%02d",
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE));
        dateFrom.setText(defaultDate);
        dateTo.setText(defaultDate);
        timeFrom.setText(defaultTime);
        timeTo.setText(defaultTime);
    }

    public static long getReminderTime(Switch switchBoxAllDay, String date, String time) {
        boolean isAllDay = switchBoxAllDay.isChecked();
        String[] dateParts = date.split("/");
        int day = Integer.parseInt(dateParts[0]);
        int month = Integer.parseInt(dateParts[1]) - 1;
        int year = Integer.parseInt(dateParts[2]);

        int hour = 0;
        int minute = 0;

        if (!isAllDay) {
            String[] timeParts = time.split(":");
            hour = Integer.parseInt(timeParts[0]);
            minute = Integer.parseInt(timeParts[1]);
        }

        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month, day, hour, minute, 0);
        return calendar.getTimeInMillis();
    }

    public static long getStartOfDay(){
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    public static long getEndOfDay(){
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        return calendar.getTimeInMillis();
    }

    public static boolean isSameDay(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
                && cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    public static boolean isSameWeek(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
                && cal1.get(Calendar.WEEK_OF_YEAR) == cal2.get(Calendar.WEEK_OF_YEAR);
    }

    public static String formattedDate(Date date){
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM hh:mm a", Locale.getDefault());
        return sdf.format(date);
    }

}
