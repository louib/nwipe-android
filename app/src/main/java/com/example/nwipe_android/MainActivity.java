package com.example.nwipe_android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;


public class MainActivity extends AppCompatActivity {

    public WipeAsyncTask wipeAsyncTask = null;
    public BroadcastReceiver powerBroadcastReceiver = null;

    private boolean isWiping = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Switch verifySwitch = findViewById(R.id.verify_switch);
        verifySwitch.setChecked(WipeJob.DEFAULT_VERIFY);
        Switch blankSwitch = findViewById(R.id.blanking_switch);
        blankSwitch.setChecked(WipeJob.DEFAULT_BLANK);
        SeekBar numberPassesSeekBar = findViewById(R.id.number_passes_seek_bar);
        numberPassesSeekBar.setMax(WipeJob.MAX_NUMBER_PASSES - 1);

        numberPassesSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                TextView numberPassesTextView = findViewById(R.id.number_passes_text_view);
                String label = getString(R.string.number_passes_label);
                numberPassesTextView.setText(label + " " + (progress + 1));
            }
        });
        // The progress must be set after registering the listener to make sure the initial value of
        // the label is updated.
        numberPassesSeekBar.setProgress(WipeJob.DEFAULT_NUMBER_PASSES - 1);

        // This is not working in android studio :(
        // if (!deviceIsPluggedIn()) {
        //     this.showPowerDisconnectedMessage();
        // }

        TextView sizeTextView = findViewById(R.id.available_size_text_view);
        sizeTextView.setText(String.format(
                "%s/%s available for wiping.",
                WipeAsyncTask.getTextualAvailableMemory(),
                WipeAsyncTask.getTextualTotalMemory()
        ));

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_POWER_CONNECTED);
        intentFilter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        this.powerBroadcastReceiver = new PowerBroadcastReceiver();
        this.registerReceiver(this.powerBroadcastReceiver, intentFilter);
    }

    public void showPowerDisconnectedMessage() {
        TextView errorTextView = findViewById(R.id.error_text_view);
        errorTextView.setText("The device is not plugged in!");
        Button startWipeButton = findViewById(R.id.start_wipe_button);
        startWipeButton.setClickable(false);
    }

    public void clearPowerDisconnectedMessage() {
        TextView errorTextView = findViewById(R.id.error_text_view);
        errorTextView.setText("");
        Button startWipeButton = findViewById(R.id.start_wipe_button);
        startWipeButton.setClickable(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.teardown();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    public void onMainButtonClick(View v) {
        if (this.isWiping) {
            Log.i("MainActivity", "Cancelling wipe process.");
            this.stopWipe();
        } else {
            Log.i("MainActivity", "Starting wipe process.");
            this.startWipe();
        }
    }

    public void teardown() {
        if (this.wipeAsyncTask != null) {
            this.wipeAsyncTask.cancel(true);
            this.wipeAsyncTask = null;
        }
        unregisterReceiver(this.powerBroadcastReceiver);
    }

    public void onCloseButtonClick(MenuItem item) {
        this.teardown();
        this.finish();
    }

    public void onWipeFinished(WipeJob wipeJob) {
        Button startWipeButton = findViewById(R.id.start_wipe_button);
        TextView errorTextView = findViewById(R.id.error_text_view);

        this.isWiping = false;
        errorTextView.setText(wipeJob.errorMessage);
        startWipeButton.setText(R.string.start_wipe_button_label);

        SeekBar numberPassesSeekBar = findViewById(R.id.number_passes_seek_bar);
        Switch verifySwitch = findViewById(R.id.verify_switch);
        Switch blankingSwitch = findViewById(R.id.blanking_switch);
        numberPassesSeekBar.setEnabled(true);
        verifySwitch.setEnabled(true);
        blankingSwitch.setEnabled(true);
    }

    /*
     * See https://developer.android.com/training/monitoring-device-state/battery-monitoring#java
     * for details on monitoring battery status.
     */
    public boolean deviceIsPluggedIn() {
        Context context = getApplicationContext();
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, intentFilter);

        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        return status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL;
    }

    public void startWipe() {
        Button startWipeButton = findViewById(R.id.start_wipe_button);
        ProgressBar wipeProgressBar = findViewById(R.id.wipe_progress_bar);

        startWipeButton.setText(R.string.cancel_wipe_button_label);
        this.isWiping = true;

        SeekBar numberPassesSeekBar = findViewById(R.id.number_passes_seek_bar);
        Switch verifySwitch = findViewById(R.id.verify_switch);
        Switch blankingSwitch = findViewById(R.id.blanking_switch);

        WipeJob wipeJob = new WipeJob();
        wipeJob.number_passes = numberPassesSeekBar.getProgress() + 1;
        wipeJob.verify = verifySwitch.isChecked();
        wipeJob.blank = blankingSwitch.isChecked();

        this.wipeAsyncTask = new WipeAsyncTask(this);
        this.wipeAsyncTask.execute(wipeJob);

        numberPassesSeekBar.setEnabled(false);
        verifySwitch.setEnabled(false);
        blankingSwitch.setEnabled(false);
    }

    public void stopWipe() {
        if (this.wipeAsyncTask != null) {
            this.wipeAsyncTask.cancel(true);
            this.wipeAsyncTask = null;
        }
        this.isWiping = false;

        Button startWipeButton = findViewById(R.id.start_wipe_button)   ;
        TextView wipeTextView = findViewById(R.id.wipe_text_view);
        ProgressBar wipeProgressBar = findViewById(R.id.wipe_progress_bar);

        wipeProgressBar.setProgress(0);
        wipeTextView.setText("");
        startWipeButton.setText(R.string.start_wipe_button_label);

        SeekBar numberPassesSeekBar = findViewById(R.id.number_passes_seek_bar);
        Switch verifySwitch = findViewById(R.id.verify_switch);
        Switch blankingSwitch = findViewById(R.id.blanking_switch);
        numberPassesSeekBar.setEnabled(true);
        verifySwitch.setEnabled(true);
        blankingSwitch.setEnabled(true);
    }

    public void setWipeProgress(WipeJob wipeJob) {
        ProgressBar wipeProgressBar = findViewById(R.id.wipe_progress_bar);
        TextView wipeTextView = findViewById(R.id.wipe_text_view);
        TextView errorTextView = findViewById(R.id.error_text_view);

        errorTextView.setText(wipeJob.errorMessage);
        if (!this.isWiping) {
            return;
        }

        if (wipeJob.wipedBytes == 0) {
            wipeProgressBar.setVisibility(View.INVISIBLE);
            wipeProgressBar.setVisibility(View.VISIBLE);
        }

        int percentageCompletion = wipeJob.getCurrentPassPercentageCompletion();
        wipeTextView.setText(wipeJob.toString());
        wipeProgressBar.setMax(100);
        if (wipeJob.isCompleted()) {
            wipeProgressBar.setProgress(100);
        } else {
            wipeProgressBar.setProgress(percentageCompletion);
        }
    }
}
