package com.example.crambusterapp;

public class Reminder {

    public void setDate(String date) {
        this.date = date;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public void setEvent(String event) {
        this.event = event;
    }
    String date;
    String time;
    String event;
    public String getDate() {
        return date;
    }

    public String getTime() {
        return time;
    }

    public String getEvent() {
        return event;
    }


}
