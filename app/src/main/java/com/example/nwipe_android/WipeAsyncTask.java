package com.example.nwipe_android;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Random;

public class WipeAsyncTask extends AsyncTask <MainActivity, WipeStatus, WipeStatus> {

    public static int WIPE_BUFFER_SIZE = 4096;
    public static String WIPE_FILES_PREFIX = "nwipe-android-";
    public static int NUMBER_OF_PASSES = 1;

    private MainActivity mainActivity;
    private WipeStatus wipeStatus;

    @Override
    protected WipeStatus doInBackground(MainActivity... mainActivity) {
        this.mainActivity = mainActivity[0];
        this.wipeStatus = new WipeStatus();

        Context context = this.mainActivity.getApplicationContext();
        File filesDir = context.getFilesDir();
        for (String fileName: filesDir.list()) {
            if (fileName.startsWith(WIPE_FILES_PREFIX)) {
                Log.i("WipeAsyncTask", String.format("Deleting old wipe file %s.", fileName));
                context.deleteFile(fileName);
            }
        }

        long availableBytesCount = WipeAsyncTask.getAvailableBytesCount();
        Log.i("MainActivity", String.format("Got %d bytes available for writing.", availableBytesCount));
        this.wipeStatus.totalBytes = availableBytesCount;
        this.wipeStatus.wipedBytes = 0;
        this.publishProgress(this.wipeStatus);

        String wipeFileName = String.format("%s%d", WIPE_FILES_PREFIX, System.currentTimeMillis());

        SecureRandom random = new SecureRandom();
        // TODO verify that this is a proper way of seeding.
        int randomSeed = random.nextInt();

        Log.i("WipeAsyncTask", "Starting wipe operation.");
        try (FileOutputStream fos = context.openFileOutput(wipeFileName, Context.MODE_PRIVATE)) {
            Random rnd = new Random();
            rnd.setSeed(randomSeed);
            byte[] bytesBuffer = new byte[WIPE_BUFFER_SIZE];

            while (this.wipeStatus.wipedBytes < this.wipeStatus.totalBytes) {

                long bytesLeftToWrite = this.wipeStatus.totalBytes - this.wipeStatus.wipedBytes;
                int bytesToWriteCount = WIPE_BUFFER_SIZE;
                if (bytesLeftToWrite < WIPE_BUFFER_SIZE) {
                    // No risk of overflow here since we just verified the size.
                    bytesToWriteCount = (int)bytesLeftToWrite;
                }

                rnd.nextBytes(bytesBuffer);
                fos.write(bytesBuffer, 0, bytesToWriteCount);

                this.wipeStatus.wipedBytes += bytesToWriteCount;
                this.publishProgress(this.wipeStatus);

                if (isCancelled()) {
                    break;
                }
            }
        } catch (FileNotFoundException e) {
            Log.e("WipeAsyncTask", String.format("Error while wiping: %s", e.toString()));
            return this.wipeStatus;
        } catch (IOException e) {
            Log.e("WipeAsyncTask", String.format("Error while wiping: %s", e.toString()));
            return this.wipeStatus;
        }

        this.wipeStatus.wipedBytes = 0;
        this.wipeStatus.currentOperation = WipeOperation.VERIFYING;
        this.publishProgress(this.wipeStatus);

        Log.i("WipeAsyncTask", "Starting verifying operation.");
        try (FileInputStream fis = context.openFileInput(wipeFileName)) {
            Random rnd = new Random();
            rnd.setSeed(randomSeed);
            byte[] bytesBuffer = new byte[WIPE_BUFFER_SIZE];
            byte[] bytesInputBuffer = new byte[WIPE_BUFFER_SIZE];

            while (this.wipeStatus.wipedBytes < this.wipeStatus.totalBytes) {

                long bytesLeftToRead = this.wipeStatus.totalBytes - this.wipeStatus.wipedBytes;
                int bytesToReadCount = WIPE_BUFFER_SIZE;
                if (bytesLeftToRead < WIPE_BUFFER_SIZE) {
                    bytesToReadCount = (int)bytesLeftToRead;
                }

                rnd.nextBytes(bytesBuffer);
                fis.read(bytesInputBuffer, 0, bytesToReadCount);


                if (!Arrays.equals(Arrays.copyOfRange(bytesBuffer, 0, bytesToReadCount), Arrays.copyOfRange(bytesInputBuffer, 0, bytesToReadCount))) {
                    Log.e("WipeAsyncTask", "Error while verifying wipe file: streams are not the same!");
                    return this.wipeStatus;
                }

                this.wipeStatus.wipedBytes += bytesToReadCount;
                this.publishProgress(this.wipeStatus);

                if (isCancelled()) {
                    break;
                }
            }
        } catch (FileNotFoundException e) {
            Log.e("WipeAsyncTask", String.format("Error while verifying wipe file: %s", e.toString()));
            return this.wipeStatus;
        } catch (IOException e) {
            Log.e("WipeAsyncTask", String.format("Error while verifying wipe file: %s", e.toString()));
            return this.wipeStatus;
        }

        Log.i("WipeAsyncTask", String.format("Deleting wipe file %s.", wipeFileName));
        context.deleteFile(wipeFileName);

        this.wipeStatus.succeeded = true;
        return this.wipeStatus;
    }

    protected void onProgressUpdate(WipeStatus... status) {
        this.mainActivity.setWipeProgress(status[0]);
    }

    protected void onPostExecute(WipeStatus result) {
        Log.e("WipeAsyncTask", String.format("Wiped %d bytes available for writing.", wipeStatus.wipedBytes));
        this.mainActivity.onWipeFinished();
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

    public static String getTextualAvailableMemory() {
        long totalBytes = WipeAsyncTask.getAvailableBytesCount();
        if (totalBytes < (1024 * 1024)) {
            return String.format("%d bytes", totalBytes);
        } else {
            return String.format("%d MB", totalBytes / (1024 * 1024));
        }
    }

    public static String getTextualTotalMemory() {
        long totalBytes = WipeAsyncTask.getTotalBytesCount();
        if (totalBytes < (1024 * 1024)) {
            return String.format("%d bytes", totalBytes);
        } else {
            return String.format("%d MB", totalBytes / (1024 * 1024));
        }
    }
}
