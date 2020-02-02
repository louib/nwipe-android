package com.example.nwipe_android;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
        // TODO cleanup the wiping thread.
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

    public void startWipe(View v) {
        Button startWipeButton = (Button) findViewById(R.id.start_wipe_button);
        if (this.isWiping) {
            Log.i("MainActivity", "Cancelling startWipe process.");
            this.stopWipe();
        } else {
            Log.i("MainActivity", "Starting startWipe process.");
            startWipeButton.setText(R.string.cancel_wipe_button_label);
            this.isWiping = true;
            try {
                this.startWipe();
            } catch (IOException e) {
                Log.i("MainActivity", "Could not start wipe on device." + e.getMessage());
            }
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

    public void startWipe() throws IOException {
        Context context = getApplicationContext();

        long availableBytesCount = MainActivity.getAvailableBytesCount();
        int availableBytesCountCasted = (int)availableBytesCount;
        if (availableBytesCountCasted <= 0) {
            availableBytesCountCasted = Integer.MAX_VALUE;
        }
        Log.i("MainActivity", String.format("Got %d bytes available for writing.", availableBytesCount));

        ProgressBar wipeProgressBar = (ProgressBar) findViewById(R.id.wipe_progress_bar);
        wipeProgressBar.setVisibility(View.VISIBLE);


        wipeProgressBar.setMax(availableBytesCountCasted);


        // TODO handle the int/long cast.
        // TODO add real timestamp.
        String wipeFileName = String.format("nwipe-android-%d", System.currentTimeMillis());
        //File file = new File(context.getFilesDir(), wipeFileName);

        Random rnd = new Random();
        // TODO get an actual random seed.
        rnd.setSeed(1);
        byte[] bytesBuffer = new byte[WIPE_BUFFER_SIZE];

        try (FileOutputStream fos = context.openFileOutput(wipeFileName, Context.MODE_PRIVATE)) {
            int writtenBytesCount = 0;
            while (writtenBytesCount < availableBytesCount) {


                int bytesToWriteCount = Math.min(WIPE_BUFFER_SIZE, (availableBytesCountCasted - writtenBytesCount));
                rnd.nextBytes(bytesBuffer);
                fos.write(bytesBuffer, 0, bytesToWriteCount);

                writtenBytesCount += bytesToWriteCount;
                wipeProgressBar.setProgress(writtenBytesCount);

            }
        }

    }

    public void stopWipe() {
        Button startWipeButton = (Button) findViewById(R.id.start_wipe_button);
        ProgressBar wipeProgressBar = (ProgressBar) findViewById(R.id.wipe_progress_bar);

        wipeProgressBar.setVisibility(View.INVISIBLE);
        startWipeButton.setText(R.string.start_wipe_button_label);
        this.isWiping = false;
    }

    /*
     * Gets the total number of bytes available for writing in the
     * internal memory.
     */
    public static long getAvailableBytesCount() {
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSizeLong();
        long availableBlocks = stat.getAvailableBlocksLong();
        return availableBlocks * blockSize;
    }

    /*
     * Gets the total number of bytes of the internal memory.
     */
    public static long getTotalBytesCount() {
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSizeLong();
        long totalBlocks = stat.getBlockCountLong();
        return totalBlocks * blockSize;
    }

}
