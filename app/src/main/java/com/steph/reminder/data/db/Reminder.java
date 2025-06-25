package com.steph.reminder.data.db;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.io.Serializable;

@Entity(tableName = "reminders")
public class Reminder implements Serializable {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private int color;
    private String title;
    private String description;
    private String type;
    private String repeat;
    private String priority;
    private long fromTime;
    private long toTime;
    private boolean allDay;
    private String timeForNotification;

    @Ignore
    public Reminder(long id, int color, String title, String description, String type, String repeat, String priority, long fromTime, long toTime, boolean allDay, String timeForNotification) {
        this.id = id;
        this.color = color;
        this.title = title;
        this.description = description;
        this.type = type;
        this.repeat = repeat;
        this.priority = priority;
        this.fromTime = fromTime;
        this.toTime = toTime;
        this.allDay = allDay;
        this.timeForNotification = timeForNotification;
    }

    public Reminder(int color, String title, String description, String type, String repeat, String priority, long fromTime, long toTime, boolean allDay, String timeForNotification) {
        this.color = color;
        this.title = title;
        this.description = description;
        this.type = type;
        this.repeat = repeat;
        this.priority = priority;
        this.fromTime = fromTime;
        this.toTime = toTime;
        this.allDay = allDay;
        this.timeForNotification = timeForNotification;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public int getColor() {
        return color;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getRepeat() {
        return repeat;
    }

    public void setRepeat(String repeat) {
        this.repeat = repeat;
    }

    public long getFromTime() {
        return fromTime;
    }

    public void setFromTime(long fromTime) {
        this.fromTime = fromTime;
    }

    public long getToTime() {
        return toTime;
    }

    public void setToTime(long toTime) {
        this.toTime = toTime;
    }

    public boolean isAllDay() {
        return allDay;
    }

    public void setAllDay(boolean allDay) {
        this.allDay = allDay;
    }

    public String getTimeForNotification(){
        return timeForNotification;
    }

    public void setTimeForNotification(String timeForNotification){
        this.timeForNotification = timeForNotification;
    }

}
