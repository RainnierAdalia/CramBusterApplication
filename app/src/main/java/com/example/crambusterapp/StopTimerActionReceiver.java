package com.example.crambusterapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.example.crambusterapp.StopTimerUtils;

import static com.example.crambusterapp.Constants.INTENT_VALUE_CANCEL;
import static com.example.crambusterapp.Constants.INTENT_VALUE_COMPLETE;


/**
 * StopTimerActionReceiver is used when notification
 * <p>
 * COMPLETE and CANCEL
 * <p>
 * action buttons are click
 */
public class StopTimerActionReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String receivedIntent = intent.getStringExtra("action");
        switch (receivedIntent) {
            case INTENT_VALUE_COMPLETE:
                StopTimerUtils.sessionComplete(context);
                break;
            case INTENT_VALUE_CANCEL:
                StopTimerUtils.sessionCancel(context, preferences);
                break;
        }

    }
}