package com.steph.reminder.data.model;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.steph.reminder.data.db.Reminder;
import com.steph.reminder.data.db.ReminderDao;
import com.steph.reminder.data.db.ReminderDatabase;

import java.util.List;
import java.util.concurrent.Executors;

public class ReminderViewModel extends AndroidViewModel {

    private ReminderDao reminderDao;
    private LiveData<List<Reminder>> allReminders;

    public ReminderViewModel(@NonNull Application application) {
        super(application);
        ReminderDatabase database = ReminderDatabase.getInstance(application);
        reminderDao = database.reminderDao();
        allReminders = reminderDao.getAllReminders();
    }

    public LiveData<List<Reminder>> getAllReminders() {
        return allReminders;
    }

    public LiveData<Long> insert(Reminder reminder) {
        MutableLiveData<Long> insertedId = new MutableLiveData<>();
        Executors.newSingleThreadExecutor().execute(() -> {
            long id = reminderDao.insert(reminder);
            insertedId.postValue(id);
        });
        return insertedId;
    }

    public LiveData<Reminder> getReminderById(long id) {
        return reminderDao.getReminderById(id);
    }

    public LiveData<Integer> update(final Reminder reminder) {
        final MutableLiveData<Integer> result = new MutableLiveData<>();
        new Thread(() -> {
            int rowsAffected = reminderDao.update(reminder);
            result.postValue(rowsAffected);
        }).start();
        return result;
    }

    public void delete(Reminder reminder) {
        new Thread(() -> reminderDao.delete(reminder)).start();
    }

}
