package com.example.crambusterapp;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.method.LinkMovementMethod;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NavUtils;

import com.example.crambusterapp.databinding.ActivitySettingsBinding; // Import the generated View Binding class

import static com.example.crambusterapp.Constants.RINGING_VOLUME_LEVEL_KEY;
import static com.example.crambusterapp.Constants.TICKING_VOLUME_LEVEL_KEY;
import static com.example.crambusterapp.VolumeSeekBarUtils.convertToFloat;
import static com.example.crambusterapp.VolumeSeekBarUtils.floatRingingVolumeLevel;
import static com.example.crambusterapp.VolumeSeekBarUtils.floatTickingVolumeLevel;
import static com.example.crambusterapp.VolumeSeekBarUtils.initializeSeekBar;
import static com.example.crambusterapp.VolumeSeekBarUtils.maxVolume;

public class SettingsActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener, SeekBar.OnSeekBarChangeListener {

    private ActivitySettingsBinding binding; // Declare the View Binding object
    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater()); // Inflate the binding
        setContentView(binding.getRoot());

        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        // Rule 1: Initialize the Spinner and SeekBar
        initSpinner();
        seekBarInitialization();
        binding.aboutUsText.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private void seekBarInitialization() {
        initializeSeekBar(this, binding.tickingSeekBar);
        initializeSeekBar(this, binding.ringingSeekBar);
        binding.tickingSeekBar.setOnSeekBarChangeListener(this);
        binding.ringingSeekBar.setOnSeekBarChangeListener(this);
    }

    private void initSpinner() {
        ArrayAdapter<CharSequence> workDurationAdapter = ArrayAdapter.createFromResource(this,
                R.array.work_duration_array, R.layout.spinner_item);
        ArrayAdapter<CharSequence> shortBreakDurationAdapter = ArrayAdapter.createFromResource(this,
                R.array.short_break_duration_array, R.layout.spinner_item);
        ArrayAdapter<CharSequence> longBreakDurationAdapter = ArrayAdapter.createFromResource(this,
                R.array.long_break_duration_array, R.layout.spinner_item);
        ArrayAdapter<CharSequence> startlongbreakafterAdapter = ArrayAdapter.createFromResource(this,
                R.array.start_long_break_after_array, R.layout.spinner_item);

        workDurationAdapter.setDropDownViewResource(R.layout.spinner_item);
        shortBreakDurationAdapter.setDropDownViewResource(R.layout.spinner_item);
        longBreakDurationAdapter.setDropDownViewResource(R.layout.spinner_item);
        startlongbreakafterAdapter.setDropDownViewResource(R.layout.spinner_item);

        binding.workDurationSpinner.setAdapter(workDurationAdapter);
        binding.shortBreakDurationSpinner.setAdapter(shortBreakDurationAdapter);
        binding.longBreakDurationSpinner.setAdapter(longBreakDurationAdapter);
        binding.startLongBreakAfterSpinner.setAdapter(startlongbreakafterAdapter);

        binding.workDurationSpinner.setSelection(preferences.getInt(getString(R.string.work_duration_key), 1));
        binding.shortBreakDurationSpinner.setSelection(preferences.getInt(getString(R.string.short_break_duration_key), 1));
        binding.longBreakDurationSpinner.setSelection(preferences.getInt(getString(R.string.long_break_duration_key), 1));
        binding.startLongBreakAfterSpinner.setSelection(preferences.getInt(getString(R.string.start_long_break_after_key), 2));

        binding.workDurationSpinner.setOnItemSelectedListener(this);
        binding.shortBreakDurationSpinner.setOnItemSelectedListener(this);
        binding.longBreakDurationSpinner.setOnItemSelectedListener(this);
        binding.startLongBreakAfterSpinner.setOnItemSelectedListener(this);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        SharedPreferences.Editor editor = preferences.edit();

        // Rule 6: Dispatch Spinner Selection Handling to Specific Methods
        handleWorkDurationSelection(parent, editor, position);
        handleShortBreakDurationSelection(parent, editor, position);
        handleLongBreakDurationSelection(parent, editor, position);
        handleStartLongBreakAfterSelection(parent, editor, position);

        editor.apply();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    // Rule 4: Handle Work Duration Spinner Selection
    private void handleWorkDurationSelection(AdapterView<?> parent, SharedPreferences.Editor editor, int position) {
        if (parent.getId() == R.id.work_duration_spinner) {
            editor.putInt(getString(R.string.work_duration_key), position);
        }

    }

    // Rule 6.1: Handle Short Break Duration Spinner Selection
    private void handleShortBreakDurationSelection(AdapterView<?> parent, SharedPreferences.Editor editor, int position) {
        if (parent.getId() == R.id.short_break_duration_spinner) {
            editor.putInt(getString(R.string.short_break_duration_key), position);
        }

    }

    // Rule 6.2: Handle Long Break Duration Spinner Selection
    private void handleLongBreakDurationSelection(AdapterView<?> parent, SharedPreferences.Editor editor, int position) {
        if (parent.getId() == R.id.long_break_duration_spinner) {
            editor.putInt(getString(R.string.long_break_duration_key), position);
        }

    }

    // Rule 6.3: Handle Start Long Break After Spinner Selection
    private void handleStartLongBreakAfterSelection(AdapterView<?> parent, SharedPreferences.Editor editor, int position) {
        if (parent.getId() == R.id.start_long_break_after_spinner) {
            editor.putInt(getString(R.string.start_long_break_after_key), position);
        }

    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (seekBar.getId() == R.id.ticking_seek_bar) {
            preferences.edit().putInt(TICKING_VOLUME_LEVEL_KEY, progress).apply();
            floatTickingVolumeLevel = convertToFloat(preferences.getInt(TICKING_VOLUME_LEVEL_KEY, maxVolume), maxVolume);
        } else if (seekBar.getId() == R.id.ringing_seek_bar) {
            preferences.edit().putInt(RINGING_VOLUME_LEVEL_KEY, progress).apply();
            floatRingingVolumeLevel = convertToFloat(preferences.getInt(RINGING_VOLUME_LEVEL_KEY, maxVolume), maxVolume);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (binding != null) {
        }
    }
}
