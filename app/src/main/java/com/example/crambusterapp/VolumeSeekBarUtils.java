package com.example.crambusterapp;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.preference.PreferenceManager;
import android.widget.SeekBar;

import com.example.crambusterapp.R;

import static com.example.crambusterapp.Constants.RINGING_VOLUME_LEVEL_KEY;
import static com.example.crambusterapp.Constants.TICKING_VOLUME_LEVEL_KEY;

public class VolumeSeekBarUtils {
    public static int maxVolume;
    public static float floatTickingVolumeLevel;
    public static float floatRingingVolumeLevel;

    public static SeekBar initializeSeekBar(Activity activity, SeekBar seekBar) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activity);
        final AudioManager audioManager = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
        maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) - 1;
        seekBar.setMax(maxVolume - 1);

        int seekBarId = seekBar.getId();  // Store the SeekBar ID in a variable

        if (seekBarId == R.id.ticking_seek_bar) {
            seekBar.setProgress(preferences.getInt(TICKING_VOLUME_LEVEL_KEY, maxVolume));
        } else if (seekBarId == R.id.ringing_seek_bar) {
            seekBar.setProgress(preferences.getInt(RINGING_VOLUME_LEVEL_KEY, maxVolume));
        }
        return seekBar;

    }

    public static float convertToFloat(int currentVolume, int maxVolume) {
        float value = (float) (1 - (Math.log(maxVolume - currentVolume) / Math.log(maxVolume)));
        return value;
    }
}