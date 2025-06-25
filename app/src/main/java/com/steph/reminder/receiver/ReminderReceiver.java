package com.steph.reminder.receiver;

import android.app.AlarmManager;
import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.lifecycle.ViewModelProvider;

import com.steph.reminder.R;
import com.steph.reminder.data.model.ReminderViewModel;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class ReminderReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            Log.d("ReminderReceiver", "Notification received");

            NotificationChannel channel = new NotificationChannel(
                    "reminder_channel",
                    "Reminder Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT
            );

            NotificationManager notificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(channel);

            long id = intent.getLongExtra("id", 0L);
            String title = intent.getStringExtra("title");
            String description = intent.getStringExtra("description");
            String type = intent.getStringExtra("type");
            String repeat = intent.getStringExtra("repeat");

            long reminderTimeFrom = intent.getLongExtra("reminderTimeFrom", 0L);
            String formattedDate = getTimeFormatted(reminderTimeFrom);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "reminder_channel")
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(formattedDate + ": " + title)
                    .setContentText(type + ": " + description)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true);

            notificationManager.notify(0, builder.build());

            if (repeat != null && !repeat.equals("No Repeat")) {

                long nextReminderTimeFrom = getNextReminderTime(reminderTimeFrom, repeat);
                long reminderTimeTo = intent.getLongExtra("reminderTimeTo", 0L);
                long nextReminderTimeTo = getNextReminderTime(reminderTimeTo, repeat);
                updateReminderInDatabase(context, id, nextReminderTimeFrom, nextReminderTimeTo);

                long notificationTime = intent.getLongExtra("notificationTime", 0L);
                long nextNotificationTime = getNextReminderTime(notificationTime, repeat);
                scheduleNextReminder(context, id, title, description, type, repeat, nextNotificationTime, nextReminderTimeFrom, nextReminderTimeTo);

            }

        }

    }

    @NonNull
    private static String getTimeFormatted(long timeMiliseconds) {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM hh:mm a", Locale.getDefault());
        String formattedDate = sdf.format(new Date(timeMiliseconds));
        return formattedDate;
    }

    private long getNextReminderTime(long reminderTime, String repeatType) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(reminderTime);
        switch (repeatType) {
            case "Daily":
                calendar.add(Calendar.DAY_OF_YEAR, 1);
                break;
            case "Weekly":
                calendar.add(Calendar.WEEK_OF_YEAR, 1);
                break;
            case "Monthly":
                calendar.add(Calendar.MONTH, 1);
                break;
            case "Yearly":
                calendar.add(Calendar.YEAR, 1);
                break;
            default:
                break;
        }
        return calendar.getTimeInMillis();
    }

    private void updateReminderInDatabase(Context context, long reminderId, long from, long to) {
        ReminderViewModel reminderViewModel = new ViewModelProvider.AndroidViewModelFactory(
                (Application) context.getApplicationContext()
        ).create(ReminderViewModel.class);
        reminderViewModel.getReminderById(reminderId).observeForever(reminder -> {
            if (reminder != null) {
                reminder.setFromTime(from);
                reminder.setToTime(to);
                reminderViewModel.update(reminder);
            }
        });
    }

    private void scheduleNextReminder(Context context, long reminderId, String title, String description, String type, String repeat, long nextNotificationTime, long reminderTimeFrom, long reminderTimeTo) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager.canScheduleExactAlarms()) {
                setExactAlarm(context, nextNotificationTime, createPendingIntent(context, reminderId, title, description, type, repeat, nextNotificationTime, reminderTimeFrom, reminderTimeTo));
            } else {
                Log.e("ReminderReceiver", "Exact alarms cannot be scheduled");
            }
        } else {
            setExactAlarm(context, nextNotificationTime, createPendingIntent(context, reminderId, title, description, type, repeat, nextNotificationTime, reminderTimeFrom, reminderTimeTo));
        }
    }

    private void setExactAlarm(Context context, long nextNotificationTime, PendingIntent pendingIntent) {
        try {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, nextNotificationTime, pendingIntent);
        } catch (SecurityException e) {
            Log.e("ReminderReceiver", "SecurityException: " + e.getMessage());
        }
    }

    private PendingIntent createPendingIntent(Context context, long reminderId, String title, String description, String type, String repeat, long nextNotificationTime, long reminderTimeFrom, long reminderTimeTo) {

        Intent nextIntent = new Intent(context, ReminderReceiver.class);

        nextIntent.putExtra("id", reminderId);
        nextIntent.putExtra("title", title);
        nextIntent.putExtra("description", description);
        nextIntent.putExtra("type", type);
        nextIntent.putExtra("repeat", repeat);

        nextIntent.putExtra("reminderTimeFrom", reminderTimeFrom);
        nextIntent.putExtra("reminderTimeTo", reminderTimeTo);

        nextIntent.putExtra("notificationTime", nextNotificationTime);


        return PendingIntent.getBroadcast(
            context,
            (int) reminderId,
            nextIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

    }

}
