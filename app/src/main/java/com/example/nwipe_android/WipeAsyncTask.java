package com.example.nwipe_android;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;

public class WipeAsyncTask extends AsyncTask <MainActivity, WipeStatus, WipeStatus> {

    public static int WIPE_BUFFER_SIZE = 4096;

    private MainActivity mainActivity;
    private WipeStatus wipeStatus;

    @Override
    protected WipeStatus doInBackground(MainActivity... mainActivity) {
        this.mainActivity = mainActivity[0];
        this.wipeStatus = new WipeStatus();

        Context context = this.mainActivity.getApplicationContext();
        TextView wipeTextView = this.mainActivity.findViewById(R.id.wipe_text_view);

        long availableBytesCount = WipeAsyncTask.getAvailableBytesCount();
        int availableBytesCountCasted = (int)availableBytesCount;
        if (availableBytesCountCasted <= 0) {
            availableBytesCountCasted = Integer.MAX_VALUE;
        }
        Log.i("MainActivity", String.format("Got %d bytes available for writing.", availableBytesCount));
        this.wipeStatus.totalBytes = availableBytesCountCasted;
        this.wipeStatus.wipedBytes = 0;

        // TODO handle the int/long cast.
        String wipeFileName = String.format("nwipe-android-%d", System.currentTimeMillis());

        Random rnd = new Random();
        // TODO get an actual random seed.
        rnd.setSeed(1);
        byte[] bytesBuffer = new byte[WIPE_BUFFER_SIZE];

        try (FileOutputStream fos = context.openFileOutput(wipeFileName, Context.MODE_PRIVATE)) {
            while (this.wipeStatus.wipedBytes < this.wipeStatus.totalBytes) {

                int bytesLeftToWrite = this.wipeStatus.totalBytes - this.wipeStatus.wipedBytes;
                int bytesToWriteCount = Math.min(WIPE_BUFFER_SIZE, bytesLeftToWrite);
                rnd.nextBytes(bytesBuffer);
                fos.write(bytesBuffer, 0, bytesToWriteCount);

                this.wipeStatus.wipedBytes += bytesToWriteCount;
                this.publishProgress(this.wipeStatus);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return this.wipeStatus;
        } catch (IOException e) {
            e.printStackTrace();
            return this.wipeStatus;
        }

        context.deleteFile(wipeFileName);

        this.wipeStatus.succeeded = true;
        return this.wipeStatus;
    }

    protected void onProgressUpdate(WipeStatus... status) {
        this.mainActivity.setWipeProgress(status[0]);
    }

    protected void onPostExecute(WipeStatus result) {
        Log.i("MainActivity", String.format("Wiped %d bytes available for writing.", wipeStatus.wipedBytes));
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
