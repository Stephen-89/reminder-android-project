package com.steph.reminder.data.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface ReminderDao {

    @Insert
    long insert(Reminder reminder);

    @Query("SELECT * FROM reminders WHERE id = :id LIMIT 1")
    LiveData<Reminder> getReminderById(long id);

    @Update
    int update(Reminder reminder);

    @Delete
    void delete(Reminder reminder);

    @Query("SELECT * FROM reminders")
    LiveData<List<Reminder>> getAllReminders();

}
