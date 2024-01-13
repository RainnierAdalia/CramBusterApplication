package com.example.crambusterapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.example.crambusterapp.Utils;

import static com.example.crambusterapp.Constants.INTENT_NAME_ACTION;
import static com.example.crambusterapp.Constants.INTENT_VALUE_LONG_BREAK;
import static com.example.crambusterapp.Constants.INTENT_VALUE_SHORT_BREAK;
import static com.example.crambusterapp.Constants.INTENT_VALUE_START;
import static com.example.crambusterapp.Constants.LONG_BREAK;
import static com.example.crambusterapp.Constants.SHORT_BREAK;
import static com.example.crambusterapp.Constants.TAMETU;
import static com.example.crambusterapp.StartTimerUtils.startTimer;

public class StartTimerActionReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String receivedIntent = intent.getStringExtra(INTENT_NAME_ACTION);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        // Rule 1: Extracting common code
        long duration = 0;
        String logMessage = "";

        switch (receivedIntent) {
            case INTENT_VALUE_START:
                duration = Utils.getCurrentDurationPreferenceOf(preferences, context, TAMETU);
                logMessage = "TIMER was started with";
                break;
            case INTENT_VALUE_SHORT_BREAK:
                duration = Utils.getCurrentDurationPreferenceOf(preferences, context, SHORT_BREAK);
                logMessage = "SHRT_BRK started with";
                break;
            case INTENT_VALUE_LONG_BREAK:
                duration = Utils.getCurrentDurationPreferenceOf(preferences, context, LONG_BREAK);
                logMessage = "LONG_BRK started with";
                break;
        }

        // Rule 2: Avoid code duplication by using a common method
        startTimer(duration, context);
        Log.d(logMessage, String.valueOf(duration));
    }
}
