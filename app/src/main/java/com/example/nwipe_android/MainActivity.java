package com.example.nwipe_android;

import android.app.usage.StorageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.Environment;
import android.os.MemoryFile;
import android.os.StatFs;
import android.os.storage.StorageManager;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    public static int WIPE_BUFFER_SIZE = 2048;

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
        // mainTextView = (TextView) findViewById(R.id.main_text_view);
        // startWipeButton = (Button) findViewById(R.id.start_wipe_button);
        if (this.isWiping) {
            Log.i("MainActivity", "Cancelling wipe process.");
            // mainTextView.setText("temp");
            // startWipeButton.setText("Start Wipe");
            this.isWiping = false;
        } else {
            Log.i("MainActivity", "Starting wipe process.");
            // mainTextView.setText("temp");
            // startWipeButton.setText("Wiping...");
            this.isWiping = true;
        }
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

    public void wipe() throws IOException {
        long availableBytesCount = MainActivity.getAvailableBytesCount();

        // TODO handle the int/long cast.
        MemoryFile wipeFile = new MemoryFile("nwipe-android-timestamp", (int)availableBytesCount);
        try {
            // This method is deprecated in API level 29, so it might raise.
            wipeFile.allowPurging(false);
        } catch (IOException e) {
            Log.i("MainActivity", "Could not set wipe file to non purgeable.");
        }

        Random rnd = new Random();
        // TODO get an actual random seed.
        rnd.setSeed(1);
        byte[] bytesBuffer = new byte[WIPE_BUFFER_SIZE];

        OutputStream outputStream = wipeFile.getOutputStream();
        int writtenBytesCount = 0;
        while (writtenBytesCount < availableBytesCount) {
            int bytesToWriteCount = Math.min(WIPE_BUFFER_SIZE, ((int)availableBytesCount - writtenBytesCount));
            rnd.nextBytes(bytesBuffer);
            outputStream.write(bytesBuffer, writtenBytesCount, bytesToWriteCount);

            writtenBytesCount += bytesToWriteCount;
        }
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
