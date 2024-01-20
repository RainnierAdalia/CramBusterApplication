package com.example.crambusterapp;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.ToggleButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.crambusterapp.databinding.ActivityMainBinding;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import static com.example.crambusterapp.CheckMarkUtils.updateCheckMarkCount;
import static com.example.crambusterapp.Constants.CHANNEL_ID;
import static com.example.crambusterapp.Constants.COMPLETE_ACTION_BROADCAST;
import static com.example.crambusterapp.Constants.COUNTDOWN_BROADCAST;
import static com.example.crambusterapp.Constants.LONG_BREAK;
import static com.example.crambusterapp.Constants.LONG_BREAK_DURATION_KEY;
import static com.example.crambusterapp.Constants.SHORT_BREAK;
import static com.example.crambusterapp.Constants.SHORT_BREAK_DURATION_KEY;
import static com.example.crambusterapp.Constants.START_ACTION_BROADCAST;
import static com.example.crambusterapp.Constants.START_LONG_BREAK_AFTER_KEY;
import static com.example.crambusterapp.Constants.STOP_ACTION_BROADCAST;
import static com.example.crambusterapp.Constants.CRAMBUSTER;
import static com.example.crambusterapp.Constants.TASK_INFORMATION_NOTIFICATION_ID;
import static com.example.crambusterapp.Constants.TASK_MESSAGE;
import static com.example.crambusterapp.Constants.TASK_ON_HAND_COUNT_KEY;
import static com.example.crambusterapp.Constants.WORK_DURATION_KEY;
import static com.example.crambusterapp.NotificationActionUtils.getIntervalAction;
import static com.example.crambusterapp.StartTimerUtils.startTimer;
import static com.example.crambusterapp.StopTimerUtils.sessionCancel;
import static com.example.crambusterapp.StopTimerUtils.sessionComplete;


public class MainActivity extends AppCompatActivity implements View.OnClickListener,
        SharedPreferences.OnSharedPreferenceChangeListener {


    public static int currentlyRunningServiceType; // Type of Service can be TAMETU, SHORT_BREAK or LONG_BREAK
    BroadcastReceiver stoppedBroadcastReceiver;
    BroadcastReceiver countDownReceiver;
    BroadcastReceiver completedBroadcastReceiver;
    BroadcastReceiver startBroadcastReceiver;
    private long workDuration; // Time Period for Pomodoro (Work-Session)
    private String workDurationString; // Time Period for Pomodoro in String
    private long shortBreakDuration; // Time Period for Short-Break
    private String shortBreakDurationString; // Time Period for Short-Break in String
    private long longBreakDuration; // Time Period for Long-Break
    private String longBreakDurationString; // Time Period for Long-Break in String
    private SharedPreferences preferences;
    private AlertDialog alertDialog;
    private boolean isAppVisible = true;
    private String currentCountDown; // Current duration for Work-Session, Short-Break or Long-Break
    private ActivityMainBinding binding;
    private EditText message; // Add this line
    private ToggleButton timerButton; // Add this line
    private ImageView settingsImageViewMain; // Add this line
    private TextView countDownTextView; // Add this line

    private Button calendar,menu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        isAppVisible = true;
        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        // Initialize your views using findViewById
        message = findViewById(R.id.current_task_name_textview_main);
        timerButton = findViewById(R.id.timer_button_main);
        settingsImageViewMain = findViewById(R.id.settings_imageview_main);
        countDownTextView = findViewById(R.id.countdown_textview_main);
        menu = findViewById(R.id.menu);

        setOnClickListeners();

        determineViewState(isServiceRunning(CountDownTimerService.class));
        setupCalendarButton();

        menu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent save = new Intent(MainActivity.this, event.class);
                startActivity(save);
            }
        });

        // Receives broadcast that the timer has stopped.
        stoppedBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                sessionCompleteAVFeedback(context);
            }
        };

        // Receives broadcast for countDown at every tick.
        countDownReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getExtras() != null) {
                    currentCountDown = intent.getExtras().getString("countDown");
                    setTextCountDownTextView(currentCountDown);
                }
            }
        };

        //Receives broadcast when timer completes its time
        completedBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                sessionCompleteAVFeedback(context);
            }
        };

        //Receives broadcast when timer starts
        startBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                sessionStartAVFeedback();
            }
        };

        retrieveDurationValues(); //Duration values for Session and Short and Long Breaks
        setInitialValuesOnScreen(); //Button Text and Worksession Count

        alertDialog = createTametuCompletionAlertDialog();
        displayTametuCompletionAlertDialog();

        final SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(this);

        message.setText(prefs.getString("autoSave", ""));

        if (message.getText().toString().trim().length() == 0)
            message.setText("Task 1", TextView.BufferType.EDITABLE);


        message.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before,
                                      int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                preferences.edit().putInt(getString(R.string.task_on_hand_count_key), 0).apply();
                prefs.edit().putString("autoSave", s.toString()).apply();

            }
        });

        preferences.registerOnSharedPreferenceChangeListener(this);


    }

    private void determineViewState(boolean serviceRunning) {
        // Set button as checked if the service is already running.
        timerButton.setChecked(serviceRunning);
        //Set task message editable-ity.
        message.setFocusableInTouchMode(!serviceRunning);
        message.setClickable(!serviceRunning);
        message.setFocusable(!serviceRunning);
    }

    private void sessionStartAVFeedback() {
        ToggleButton toggleButton = findViewById(R.id.timer_button_main);
        toggleButton.setChecked(true);
        //Disable editing.
        message.setClickable(false);
        message.setFocusable(false);
        try {
            if (alertDialog.isShowing())
                alertDialog.dismiss();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setInitialValuesOnScreen() {
        // Changing textOn & textOff according to value of currentlyRunningServiceType.
        currentlyRunningServiceType = Utils.retrieveCurrentlyRunningServiceType(preferences, this);
        changeToggleButtonStateText(currentlyRunningServiceType);

        // Retrieving value of workSessionCount (Current value of workSessionCount) from SharedPreference.
        updateCheckMarkCount(this);
    }

    private void retrieveDurationValues() {
        // Retrieving current value of Duration for POMODORO, SHORT_BREAK and
        // LONG_BREAK from SharedPreferences.
        workDuration = Utils.getCurrentDurationPreferenceOf(preferences, this, CRAMBUSTER);
        shortBreakDuration = Utils.getCurrentDurationPreferenceOf(preferences, this, SHORT_BREAK);
        longBreakDuration = Utils.getCurrentDurationPreferenceOf(preferences, this, LONG_BREAK);

        // Retrieving duration in mm:ss format from duration value in milliSeconds.
        workDurationString = Utils.getCurrentDurationPreferenceStringFor(workDuration);
        shortBreakDurationString = Utils.getCurrentDurationPreferenceStringFor(shortBreakDuration);
        longBreakDurationString = Utils.getCurrentDurationPreferenceStringFor(longBreakDuration);
    }

    private void sessionCompleteAVFeedback(Context context) {
        //Enable editing the task message
        message.setClickable(true);
        message.setFocusable(true);
        message.setFocusableInTouchMode(true);
        // Retrieving value of currentlyRunningServiceType from SharedPreferences.
        currentlyRunningServiceType = Utils.retrieveCurrentlyRunningServiceType(preferences,
                getApplicationContext());
        changeToggleButtonStateText(currentlyRunningServiceType);
        alertDialog = createTametuCompletionAlertDialog();
        displayTametuCompletionAlertDialog();
        displayTaskInformationNotification();
        //Reset Timer TextView
        String duration = Utils.getCurrentDurationPreferenceStringFor(Utils.
                getCurrentDurationPreferenceOf(preferences, context, currentlyRunningServiceType));
        setTextCountDownTextView(duration);
    }


    private void setOnClickListeners() {
        settingsImageViewMain.setOnClickListener(this);
        binding.timerButtonMain.setOnClickListener(this);
        binding.finishImageviewMain.setOnClickListener(this);
    }

    @Override
    protected void onStart() {
        isAppVisible = true;
        currentlyRunningServiceType = Utils.retrieveCurrentlyRunningServiceType(preferences, this);
        super.onStart();
    }

    @Override
    protected void onResume() {
        isAppVisible = true;
        registerLocalBroadcastReceivers();
        // Creates new Alert Dialog.
        alertDialog = createTametuCompletionAlertDialog();
        displayTametuCompletionAlertDialog();
        super.onResume();
    }

    @Override
    protected void onPause() {
        isAppVisible = false;
        super.onPause();
    }

    @Override
    protected void onStop() {
        isAppVisible = false;
        if (!isServiceRunning(CountDownTimerService.class)) {
            unregisterLocalBroadcastReceivers();
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        isAppVisible = false;
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        currentCountDown = countDownTextView.getText().toString();
        outState.putString("currentCountDown", currentCountDown);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        currentCountDown = savedInstanceState.getString("currentCountDown");
        setTextCountDownTextView(currentCountDown);
    }

    @Override
    public void onClick(View v) {
        // Rule 1: Registering Broadcast Receivers
        registerLocalBroadcastReceivers();

        // Rule 2: Retrieving the currently running service type
        currentlyRunningServiceType = Utils.retrieveCurrentlyRunningServiceType(preferences, this);

        // Handle different button clicks using if-else statements
        if (v.getId() == R.id.settings_imageview_main) {
            // Rule 3: Navigating to Settings
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        } else if (v.getId() == R.id.timer_button_main) {
            // Rule 4: Determining if a Pomodoro or Break is clicked
            Date date = new Date(System.currentTimeMillis());
            long millis = date.getTime();
            int resume = (int) millis / 1000;
            int pause = preferences.getInt("pause", 0);
            if ((resume - pause) >= 14400)
                preferences.edit().putInt(getString(R.string.work_session_count_key), 0).apply();

            if (currentlyRunningServiceType == CRAMBUSTER) {
                // Rule 5: Starting or Cancelling a Pomodoro
                if (timerButton.isChecked()) {
                    startTimer(workDuration, this);
                } else {
                    sessionCancel(this, preferences);
                }
            } else if (currentlyRunningServiceType == SHORT_BREAK) {
                // Rule 6: Starting or Skipping a Short Break
                if (timerButton.isChecked()) {
                    startTimer(shortBreakDuration, this);
                } else {
                    sessionCancel(this, preferences);
                }
            } else if (currentlyRunningServiceType == LONG_BREAK) {
                // Rule 7: Starting or Skipping a Long Break
                if (timerButton.isChecked()) {
                    startTimer(longBreakDuration, this);
                } else {
                    sessionCancel(this, preferences);
                }
            }
            preferences.edit().putString(TASK_MESSAGE, message.getText().toString()).apply();
        } else if (v.getId() == R.id.finish_imageview_main) {
            // Rule 8: Completing a Task
            if (timerButton.isChecked()) {
                sessionComplete(this);
            }
        }

        // Rule 9: Displaying Notifications
        displayTaskInformationNotification();

        // Rule 10: Managing App Visibility
        determineViewState(isServiceRunning(CountDownTimerService.class));

    }

    /**
     * Changes textOn, textOff for Toggle Button & Resets CountDownTimer to initial value,
     * according to value of currentlyRunningServiceType.
     *
     * @param currentlyRunningServiceType can be POMODORO, SHORT_BREAK or LONG_BREAK.
     */
    private void changeToggleButtonStateText(int currentlyRunningServiceType) {
        timerButton.setChecked(isServiceRunning(CountDownTimerService.class));
        if (currentlyRunningServiceType == CRAMBUSTER) {
            countDownTextView.setText(workDurationString);
        } else if (currentlyRunningServiceType == SHORT_BREAK) {
            countDownTextView.setText(shortBreakDurationString);
        } else if (currentlyRunningServiceType == LONG_BREAK) {
            countDownTextView.setText(longBreakDurationString);
        }

        /*
         https://stackoverflow.com/a/3792554/4593315
         While changing textOn, textOff programmatically, button doesn't redraw so I used this hack.
          */
        timerButton.setChecked(timerButton.isChecked());
    }

    /**
     * Registers LocalBroadcastReceivers.
     */
    private void registerLocalBroadcastReceivers() {
        LocalBroadcastManager.getInstance(this).registerReceiver((stoppedBroadcastReceiver),
                new IntentFilter(STOP_ACTION_BROADCAST));
        LocalBroadcastManager.getInstance(this).registerReceiver((countDownReceiver),
                new IntentFilter(COUNTDOWN_BROADCAST));
        LocalBroadcastManager.getInstance(this).registerReceiver(completedBroadcastReceiver,
                new IntentFilter(COMPLETE_ACTION_BROADCAST));
        LocalBroadcastManager.getInstance(this).registerReceiver(startBroadcastReceiver,
                new IntentFilter(START_ACTION_BROADCAST));
    }

    /**
     * Unregisters LocalBroadcastReceivers.
     */
    private void unregisterLocalBroadcastReceivers() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(stoppedBroadcastReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(countDownReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(completedBroadcastReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(startBroadcastReceiver);
    }

    private void setTextCountDownTextView(String duration) {
        countDownTextView.setText(duration);
    }

    /**
     * Checks if a service is running or not.
     *
     * @param serviceClass name of the Service class.
     * @return true if service is running, otherwise false.
     */
    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (manager != null) {
            for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                if (serviceClass.getName().equals(service.service.getClassName())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Creates layout for alert-dialog, which is shown when Pomodoro (Work-Session) is completed.
     *
     * @return alert-dialog
     */
    private AlertDialog createTametuCompletionAlertDialog() {
        if (alertDialog != null)
            alertDialog.cancel();

        View alertDialogLayout = View.inflate(getApplicationContext(), R.layout.layout_alert_dialog, null);
        final Button startBreakLargeButton = alertDialogLayout.findViewById(R.id.start_break);
        final Button startOtherBreakMediumButton = alertDialogLayout.findViewById(R.id.start_other_break);
        Button skipBreakSmallButton = alertDialogLayout.findViewById(R.id.skip_break);

        if (currentlyRunningServiceType == SHORT_BREAK) {
            startBreakLargeButton.setText(R.string.start_short_break);
            startOtherBreakMediumButton.setText(R.string.start_long_break);
        } else if (currentlyRunningServiceType == LONG_BREAK) {
            startBreakLargeButton.setText(R.string.start_long_break);
            startOtherBreakMediumButton.setText(R.string.start_short_break);
        }

        startBreakLargeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String currentButtonText = startBreakLargeButton.getText().toString();
                startBreakFromAlertDialog(currentButtonText);
            }
        });

        startOtherBreakMediumButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String currentButtonText = startOtherBreakMediumButton.getText().toString();
                startBreakFromAlertDialog(currentButtonText);
            }
        });

        skipBreakSmallButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sessionCancel(MainActivity.this, preferences);
            }
        });

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setView(alertDialogLayout);
        alertDialogBuilder.setCancelable(false);
        return alertDialogBuilder.create();
    }

    /**
     * Displays alert dialog when a Pomodoro (Work-Session) is finished.
     */
    private void displayTametuCompletionAlertDialog() {
        if (currentlyRunningServiceType != CRAMBUSTER && isAppVisible && !alertDialog.isShowing() && !isServiceRunning(CountDownTimerService.class)) {
            alertDialog.show();
        }
    }

    /**
     * Sets appropriate values for medium and large button, and starts service; either SHORT_BREAK or LONG_BREAK.
     *
     * @param currentButtonText button text of either medium button or large button.
     */
    private void startBreakFromAlertDialog(String currentButtonText) {
        long breakDuration = 0;
        if (currentButtonText.equals(getString(R.string.start_long_break))) {
            Utils.updateCurrentlyRunningServiceType(preferences, getApplicationContext(), LONG_BREAK);
            breakDuration = longBreakDuration;
        } else if (currentButtonText.equals(getString(R.string.start_short_break))) {
            Utils.updateCurrentlyRunningServiceType(preferences, getApplicationContext(), SHORT_BREAK);
            breakDuration = shortBreakDuration;
        }

        currentlyRunningServiceType = Utils.retrieveCurrentlyRunningServiceType(preferences, getApplicationContext());
        if (alertDialog != null)
            alertDialog.cancel();
        registerLocalBroadcastReceivers();
        changeToggleButtonStateText(currentlyRunningServiceType);
        startTimer(breakDuration, this);
        timerButton.setChecked(isServiceRunning(CountDownTimerService.class));
    }

    /**
     * Creates structure for a notification which is shown when a task is Completed.
     * Task can be POMODORO, SHORT_BREAK, LONG_BREAK
     *
     * @return notification.
     */
    private NotificationCompat.Builder createTaskInformationNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setColor(getResources().getColor(R.color.colorPrimary))
                .setUsesChronometer(true); //timer that counts-up. Displays time in-between two sessions

        switch (currentlyRunningServiceType) {
            case CRAMBUSTER:
                notificationBuilder
                        .addAction(getIntervalAction(CRAMBUSTER, MainActivity.this))
                        .setContentTitle(getString(R.string.break_over_notification_title))
                        .setContentText(getString(R.string.break_over_notification_content_text));
                break;
            case SHORT_BREAK:
                notificationBuilder
                        .addAction(getIntervalAction(SHORT_BREAK, MainActivity.this))
                        .addAction(getIntervalAction(LONG_BREAK, MainActivity.this))
                        .setContentTitle(getString(R.string.tametu_completion_notification_message))
                        .setContentText(getString(R.string.session_over_notification_content_text));
                break;
            case LONG_BREAK:
                notificationBuilder
                        .addAction(getIntervalAction(LONG_BREAK, MainActivity.this))
                        .addAction(getIntervalAction(SHORT_BREAK, MainActivity.this))
                        .setContentTitle(getString(R.string.tametu_completion_alert_message))
                        .setContentText(getString(R.string.session_over_notification_content_text));
                break;
            default:
        }

        return notificationBuilder;
    }

    /**
     * Displays a notification when foreground service is finished.
     */
    private void displayTaskInformationNotification() {
        Notification notification = createTaskInformationNotification().build();
        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat
                .from(this);

        // Clearing any previous notifications.
        notificationManagerCompat
                .cancel(TASK_INFORMATION_NOTIFICATION_ID);

        // Displays a notification.
        if (!isServiceRunning(CountDownTimerService.class)) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            notificationManagerCompat
                    .notify(TASK_INFORMATION_NOTIFICATION_ID, notification);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch (key) {
            case WORK_DURATION_KEY:
            case SHORT_BREAK_DURATION_KEY:
            case LONG_BREAK_DURATION_KEY:
            case START_LONG_BREAK_AFTER_KEY:
                retrieveDurationValues();
                setInitialValuesOnScreen();
                break;
            case TASK_ON_HAND_COUNT_KEY:
                updateCheckMarkCount(this);
        }
    }
    private void setupCalendarButton() {
        Button calendarButton = findViewById(R.id.calendar);
        calendarButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDateTimePicker();
            }
        });
    }

    // Method to show date and time picker
    private void showDateTimePicker() {
        final Calendar currentDate = Calendar.getInstance();
        final Calendar date = Calendar.getInstance();
        new DatePickerDialog(MainActivity.this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                date.set(year, monthOfYear, dayOfMonth);
                new TimePickerDialog(MainActivity.this, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        date.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        date.set(Calendar.MINUTE, minute);

                        // Prompt for Event Name
                        final EditText eventNameInput = new EditText(MainActivity.this);
                        AlertDialog.Builder eventDialogBuilder = new AlertDialog.Builder(MainActivity.this);
                        eventDialogBuilder.setTitle("Enter Event Name");
                        eventDialogBuilder.setView(eventNameInput);
                        eventDialogBuilder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String eventName = eventNameInput.getText().toString();

                                // Format the date and time
                                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                                String formattedDate = dateFormat.format(date.getTime());
                                SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                                String formattedTime = timeFormat.format(date.getTime());

                                // Create Reminder instance
                                Reminder reminder = new Reminder();
                                reminder.setDate(formattedDate);
                                reminder.setTime(formattedTime);
                                reminder.setEvent(eventName);

                                // Save to Firebase
                                DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Reminders");
                                String reminderId = databaseReference.push().getKey();
                                if (reminderId != null) {
                                    databaseReference.child(reminderId).setValue(reminder);
                                }
                            }
                        });
                        eventDialogBuilder.setNegativeButton("Cancel", null);
                        AlertDialog eventDialog = eventDialogBuilder.create();
                        eventDialog.show();
                    }
                }, currentDate.get(Calendar.HOUR_OF_DAY), currentDate.get(Calendar.MINUTE), false).show();
            }
        }, currentDate.get(Calendar.YEAR), currentDate.get(Calendar.MONTH), currentDate.get(Calendar.DATE)).show();
    }



}