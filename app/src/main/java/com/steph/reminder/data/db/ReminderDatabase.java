package com.steph.reminder.data.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {Reminder.class}, version = 1, exportSchema = false)
public abstract class ReminderDatabase extends RoomDatabase {

    private static volatile ReminderDatabase INSTANCE;

    public abstract ReminderDao reminderDao();

    public static ReminderDatabase getInstance(final Context context) {
        if (INSTANCE == null) {
            synchronized (ReminderDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                ReminderDatabase.class, "reminder_database")
                        .build();
                }
            }
        }
        return INSTANCE;
    }

}
