package com.example.crambusterapp;

import static com.example.crambusterapp.Constants.CHANNEL_ID;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

import java.util.Objects;

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    public void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(
                    CHANNEL_ID,
                    getResources().getString(R.string.app_name),
                    NotificationManager.IMPORTANCE_LOW);

            NotificationManager notificationManager = Objects.requireNonNull(
                    getSystemService(NotificationManager.class));

            notificationManager.createNotificationChannel(notificationChannel);
        }
    }
}