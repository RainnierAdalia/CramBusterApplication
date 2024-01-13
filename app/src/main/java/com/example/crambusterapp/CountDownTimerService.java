package com.example.crambusterapp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.preference.PreferenceManager;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.crambusterapp.R;
import com.example.crambusterapp.Utils;
import com.example.crambusterapp.StopTimerActionReceiver;

import static com.example.crambusterapp.Constants.CHANNEL_ID;
import static com.example.crambusterapp.Constants.COUNTDOWN_BROADCAST;
import static com.example.crambusterapp.Constants.INTENT_NAME_ACTION;
import static com.example.crambusterapp.Constants.INTENT_VALUE_CANCEL;
import static com.example.crambusterapp.Constants.INTENT_VALUE_COMPLETE;
import static com.example.crambusterapp.Constants.TASK_INFORMATION_NOTIFICATION_ID;
import static com.example.crambusterapp.Constants.TASK_MESSAGE;
import static com.example.crambusterapp.Constants.STOP_ACTION_BROADCAST;
import static com.example.crambusterapp.Constants.TAMETU;
import static com.example.crambusterapp.Utils.ringID;
import static com.example.crambusterapp.Utils.soundPool;
import static com.example.crambusterapp.Utils.tickID;
import static com.example.crambusterapp.VolumeSeekBarUtils.floatRingingVolumeLevel;
import static com.example.crambusterapp.VolumeSeekBarUtils.floatTickingVolumeLevel;

public class CountDownTimerService extends Service {
    public static final int ID = 1;
    LocalBroadcastManager broadcaster;
    private CountDownTimer countDownTimer;
    private SharedPreferences preferences;
    private int newWorkSessionCount;
    private int currentlyRunningServiceType;

    public CountDownTimerService() {
    }

    @Override
    public void onCreate() {
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        broadcaster = LocalBroadcastManager.getInstance(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Your Channel Name",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }


    }

    @Override
    public void onDestroy() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        long TIME_PERIOD = intent.getLongExtra("time_period", 0);
        long TIME_INTERVAL = intent.getLongExtra("time_interval", 0);
        currentlyRunningServiceType = Utils.retrieveCurrentlyRunningServiceType(preferences, this);

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // For "Complete" button - Intent and PendingIntent
        Intent completeIntent = new Intent(this, StopTimerActionReceiver.class)
                .putExtra(INTENT_NAME_ACTION, INTENT_VALUE_COMPLETE);
        PendingIntent completeActionPendingIntent = PendingIntent.getBroadcast(this,
                1, completeIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // For "Cancel" button - Intent and PendingIntent
        Intent cancelIntent = new Intent(this, StopTimerActionReceiver.class)
                .putExtra(INTENT_NAME_ACTION, INTENT_VALUE_CANCEL);
        PendingIntent cancelActionPendingIntent = PendingIntent.getBroadcast(this,
                0, cancelIntent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(
                this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(pendingIntent)
                .setOngoing(false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            notificationBuilder
                    .setWhen(System.currentTimeMillis() + TIME_PERIOD)
                    .setUsesChronometer(true)
                    .setChronometerCountDown(true);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            notificationBuilder.setColor(getResources().getColor(R.color.colorPrimary));
        }

        // Adding separate cases for both service types to ensure the order of the action
        // buttons is preserved in the notification
        switch (currentlyRunningServiceType) {
            case 0:
                notificationBuilder
                        .addAction(R.drawable.complete, "Complete", completeActionPendingIntent)
                        .addAction(R.drawable.cancel, "Cancel", cancelActionPendingIntent)
                        .setContentTitle("Tametu Countdown Timer")
                        .setContentText(getContentText());
                break;
            case 1:
            case 2:
                notificationBuilder
                        .addAction(R.drawable.cancel, "Cancel", cancelActionPendingIntent)
                        .setContentTitle("On a break")
                        .setContentText("Break timer is running");
                break;
        }

        Notification notification = notificationBuilder.build();

        // Clearing any previous notifications.
        NotificationManagerCompat.from(this).cancel(TASK_INFORMATION_NOTIFICATION_ID);


        startForeground(ID, notification);
        countDownTimerBuilder(TIME_PERIOD, TIME_INTERVAL).start();
        return START_REDELIVER_INTENT;
    }

    private String getContentText() {
        String contentText;
        String taskMessage = preferences.getString(TASK_MESSAGE, "");
        if (!taskMessage.equals("")) {
            contentText = taskMessage + " is running";
        } else {
            contentText = "Countdown timer is running";
        }
        return contentText;
    }

    /**
     * @return a CountDownTimer which ticks every 1 second for the given Time period.
     */
    private CountDownTimer countDownTimerBuilder(long TIME_PERIOD, long TIME_INTERVAL) {
        currentlyRunningServiceType = Utils.retrieveCurrentlyRunningServiceType(preferences, getApplicationContext());
        countDownTimer = new CountDownTimer(TIME_PERIOD, TIME_INTERVAL) {
            @Override
            public void onTick(long timeInMilliSeconds) {
                soundPool.play(tickID, floatTickingVolumeLevel, floatTickingVolumeLevel, 1, 0, 1f);

                String countDown = Utils.getCurrentDurationPreferenceStringFor(timeInMilliSeconds);

                broadcaster.sendBroadcast(new Intent(COUNTDOWN_BROADCAST).putExtra("countDown", countDown));
            }

            @Override
            public void onFinish() {
                // Updates and Retrieves the new value of WorkSessionCount.
                if (currentlyRunningServiceType == TAMETU) {
                    newWorkSessionCount = Utils.updateWorkSessionCount(preferences, getApplicationContext());
                    // Getting the type of break the user should take, and updating the type of currently running service
                    currentlyRunningServiceType = Utils.getTypeOfBreak(preferences, getApplicationContext());
                } else {
                    // If the last value of currentlyRunningServiceType was SHORT_BREAK or LONG_BREAK, set it back to POMODORO
                    currentlyRunningServiceType = TAMETU;
                }

                newWorkSessionCount = preferences.getInt(getString(R.string.work_session_count_key), 0);
                // Updating the value of currentlyRunningServiceType in SharedPreferences.
                Utils.updateCurrentlyRunningServiceType(preferences, getApplicationContext(), currentlyRunningServiceType);
                // Ring once ticking ends.
                soundPool.play(ringID, floatRingingVolumeLevel, floatRingingVolumeLevel, 2, 0, 1f);
                stopSelf();
                stoppedBroadcastIntent();
            }
        };
        return countDownTimer;
    }

    // Broadcasts intent that the timer has stopped.
    protected void stoppedBroadcastIntent() {
        broadcaster.sendBroadcast(new Intent(STOP_ACTION_BROADCAST).putExtra("workSessionCount", newWorkSessionCount));
    }
}
