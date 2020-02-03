package com.example.nwipe_android;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.Environment;
import android.os.MemoryFile;
import android.os.StatFs;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    public static int WIPE_BUFFER_SIZE = 4096;

    public WipeAsyncTask wipeAsyncTask = null;

    private boolean isWiping = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // TODO also monitor that the devices stays plugged during the wiping process!
        if (!deviceIsPlugged()) {
            TextView errorTextView = (TextView) findViewById(R.id.error_text_view);
            errorTextView.setText("The device is not plugged!");
            Button startWipeButton = (Button) findViewById(R.id.start_wipe_button);
            startWipeButton.setClickable(false);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (this.wipeAsyncTask != null) {
            this.wipeAsyncTask.cancel(true);
            this.wipeAsyncTask = null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void onMainButtonClick(View v) {
        Button startWipeButton = (Button) findViewById(R.id.start_wipe_button);
        if (this.isWiping) {
            Log.i("MainActivity", "Cancelling wipe process.");
            this.stopWipe();
        } else {
            Log.i("MainActivity", "Starting wipe process.");
            this.startWipe();
        }

        // TODO when the wipe is finished, change the behaviour of the wipe button to close
        // the app, and call this.finish().
    }

    /*
     * See https://developer.android.com/training/monitoring-device-state/battery-monitoring#java
     * for details on monitoring battery status.
     */
    public boolean deviceIsPlugged() {
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
        // wipeProgressBar.setVisibility(View.VISIBLE);
        this.wipeAsyncTask = new WipeAsyncTask();
        this.wipeAsyncTask.execute(this);
    }

    public void stopWipe() {
        Button startWipeButton = findViewById(R.id.start_wipe_button);
        ProgressBar wipeProgressBar = findViewById(R.id.wipe_progress_bar);

        // wipeProgressBar.setVisibility(View.INVISIBLE);
        startWipeButton.setText(R.string.start_wipe_button_label);

        if (this.wipeAsyncTask != null) {
            this.wipeAsyncTask.cancel(true);
            this.wipeAsyncTask = null;
        }
        this.isWiping = false;
    }

    public void setWipeProgress(WipeStatus status) {
        ProgressBar wipeProgressBar = findViewById(R.id.wipe_progress_bar);
        TextView wipeTextView = (TextView) findViewById(R.id.wipe_text_view);

        // This is the initial progress call we receive.
        if (status.wipedBytes == 0) {
            // wipeProgressBar.setVisibility(View.VISIBLE);
            wipeProgressBar.setMax(status.totalBytes);
            wipeTextView.setText(String.format("Got %d bytes available for writing.", status.totalBytes));
        }
        wipeProgressBar.setProgress(status.wipedBytes);
    }

}
