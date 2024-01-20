package com.example.crambusterapp;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;

import com.example.crambusterapp.R;
import com.example.crambusterapp.StartTimerActionReceiver;

import static com.example.crambusterapp.Constants.CRAMBUSTER;
import static com.example.crambusterapp.Constants.INTENT_NAME_ACTION;
import static com.example.crambusterapp.Constants.INTENT_VALUE_LONG_BREAK;
import static com.example.crambusterapp.Constants.INTENT_VALUE_SHORT_BREAK;
import static com.example.crambusterapp.Constants.INTENT_VALUE_START;
import static com.example.crambusterapp.Constants.LONG_BREAK;
import static com.example.crambusterapp.Constants.SHORT_BREAK;
import static com.example.crambusterapp.Constants.CRAMBUSTER;

public class NotificationActionUtils {
    /**
     * @param currentlyRunningServiceType The next service that shall be run
     * @return Returns Action Buttons and assigns pendingIntents to Actions
     */
    public static NotificationCompat.Action getIntervalAction(int currentlyRunningServiceType,
                                                              Context context) {
        // Rule 1: Reduced redundancy by using a common method
        int iconResource = 0;
        String actionText = "";

        switch (currentlyRunningServiceType) {
            case CRAMBUSTER:
                iconResource = R.drawable.play;
                actionText = context.getString(R.string.start_tametu);
                break;
            case SHORT_BREAK:
                iconResource = R.drawable.short_break;
                actionText = context.getString(R.string.start_short_break);
                break;
            case LONG_BREAK:
                iconResource = R.drawable.long_break;
                actionText = context.getString(R.string.start_long_break);
                break;
            default:
                return null;
        }

        return new NotificationCompat.Action(
                iconResource,
                actionText,
                // Rule 2: Reduced code duplication by using a common method
                getPendingIntent(currentlyRunningServiceType, context));
    }

    // Rule 3: Common method for creating PendingIntent
    private static PendingIntent getPendingIntent(int requestCode, Context context) {
        String intentValue = "";
        switch (requestCode) {
            case CRAMBUSTER:
                intentValue = INTENT_VALUE_START;
                break;
            case SHORT_BREAK:
                intentValue = INTENT_VALUE_SHORT_BREAK;
                break;
            case LONG_BREAK:
                intentValue = INTENT_VALUE_LONG_BREAK;
                break;
        }

        Intent startIntent = new Intent(context, StartTimerActionReceiver.class)
                .putExtra(INTENT_NAME_ACTION, intentValue);

        return PendingIntent.getBroadcast(
                context,
                requestCode,
                startIntent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);
    }
}
