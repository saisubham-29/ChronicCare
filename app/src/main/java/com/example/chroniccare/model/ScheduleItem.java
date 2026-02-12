package com.example.chroniccare.model;

import com.google.firebase.Timestamp;

public class ScheduleItem {
    private String time;
    private String title;
    private boolean completed;
    private Timestamp timestamp;

    public ScheduleItem() {}

    public ScheduleItem(String time, String title, boolean completed) {
        this.time = time;
        this.title = title;
        this.completed = completed;
        this.timestamp = Timestamp.now();
    }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }

    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
}
