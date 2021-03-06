package com.example.nwipe_android;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Random;

public class WipeAsyncTask extends AsyncTask <WipeJob, WipeJob, WipeJob> {
    private static final int WIPE_BUFFER_SIZE = 4096;
    private static final String WIPE_FILES_PREFIX = "nwipe-android-";

    @SuppressLint("StaticFieldLeak")
    private MainActivity mainActivity = null;
    public WipeJob wipeJob;
    private int lastProgress = -1;

    public WipeAsyncTask() {

    }

    public WipeAsyncTask(MainActivity mainActivity) {
        super();
        this.mainActivity = mainActivity;
    }

    @Override
    protected WipeJob doInBackground(WipeJob... wipeJobs) {
        this.wipeJob = wipeJobs[0];

        this.wipe();

        return this.wipeJob;
    }

    public void wipe() {
        this.deleteWipeFiles();

        while (!wipeJob.isCompleted()) {
            try {
                this.executeWipePass();
            } catch (Exception e) {
                this.deleteWipeFiles();
                wipeJob.errorMessage = String.format("Unknown error while wiping: %s", e.toString());
                this.updateJobStatus();
                return;
            }
            this.deleteWipeFiles();
            this.updateJobStatus();

            if (this.wipeJob.failed() || this.cancelled()) {
                return;
            }
        }
    }

    protected void deleteWipeFiles() {
        Context context = this.mainActivity.getApplicationContext();
        File filesDir = context.getFilesDir();
        for (String fileName: filesDir.list()) {
            if (fileName.startsWith(WipeAsyncTask.WIPE_FILES_PREFIX)) {
                // Log.i("WipeAsyncTask", String.format("Deleting old wipe file %s.", fileName));
                context.deleteFile(fileName);
            }
        }
    }

    protected OutputStream getOutputStream(String fileName) throws FileNotFoundException {
        Context context = this.mainActivity.getApplicationContext();
        return context.openFileOutput(fileName, Context.MODE_PRIVATE);
    }

    protected InputStream getInputStream(String fileName) throws FileNotFoundException {
        Context context = this.mainActivity.getApplicationContext();
        return context.openFileInput(fileName);
    }

    protected void updateJobStatus() {
        this.publishProgress(this.wipeJob);
    }

    protected boolean cancelled() {
        return this.isCancelled();
    }

    public void executeWipePass() {
        long availableBytesCount = this.getAvailableBytesCountInternal();
        // Log.i("MainActivity", String.format("Got %d bytes available for writing.", availableBytesCount));
        this.wipeJob.totalBytes = availableBytesCount;
        this.wipeJob.wipedBytes = 0;
        this.updateJobStatus();

        String wipeFileName = String.format("%s%d", WIPE_FILES_PREFIX, System.currentTimeMillis());

        SecureRandom random = new SecureRandom();
        // TODO verify that this is a proper way of seeding.
        int randomSeed = random.nextInt();

        // Log.i("WipeAsyncTask", "Starting wipe operation.");
        try (OutputStream fos = this.getOutputStream(wipeFileName)) {
            Random rnd = new Random();
            rnd.setSeed(randomSeed);
            byte[] bytesBuffer = new byte[WIPE_BUFFER_SIZE];

            while (this.wipeJob.wipedBytes < this.wipeJob.totalBytes) {

                long bytesLeftToWrite = this.wipeJob.totalBytes - this.wipeJob.wipedBytes;
                int bytesToWriteCount = WIPE_BUFFER_SIZE;
                if (bytesLeftToWrite < WIPE_BUFFER_SIZE) {
                    // No risk of overflow here since we just verified the size.
                    bytesToWriteCount = (int)bytesLeftToWrite;
                }

                if (!wipeJob.isBlankingPass()) {
                    rnd.nextBytes(bytesBuffer);
                }

                fos.write(bytesBuffer, 0, bytesToWriteCount);

                this.wipeJob.wipedBytes += bytesToWriteCount;
                this.updateJobStatus();

                if (cancelled()) {
                    break;
                }
            }
        } catch (IOException e) {
            // Handling no space left errors at the end of the pass.
            if (e.toString().contains("ENOSP") && wipeJob.getCurrentPassPercentageCompletion() >= WipeJob.MIN_PERCENTAGE_COMPLETION) {
                this.wipeJob.totalBytes = this.wipeJob.wipedBytes;
            } else {
                wipeJob.errorMessage = String.format("Error while wiping: %s", e.toString());
                // Log.e("WipeAsyncTask", wipeJob.errorMessage);
                return;
            }
        }

        this.wipeJob.wipedBytes = 0;
        this.updateJobStatus();

        if (!wipeJob.verify) {
            wipeJob.passes_completed++;
            return;
        }

        this.wipeJob.verifying = true;

        // Log.i("WipeAsyncTask", "Starting verifying operation.");
        try (InputStream fis = this.getInputStream(wipeFileName)) {
            Random rnd = new Random();
            rnd.setSeed(randomSeed);
            byte[] bytesBuffer = new byte[WIPE_BUFFER_SIZE];
            byte[] bytesInputBuffer = new byte[WIPE_BUFFER_SIZE];

            while (this.wipeJob.wipedBytes < this.wipeJob.totalBytes) {

                long bytesLeftToRead = this.wipeJob.totalBytes - this.wipeJob.wipedBytes;
                int bytesToReadCount = WIPE_BUFFER_SIZE;
                if (bytesLeftToRead < WIPE_BUFFER_SIZE) {
                    bytesToReadCount = (int)bytesLeftToRead;
                }

                if (!wipeJob.isBlankingPass()) {
                    rnd.nextBytes(bytesBuffer);
                }

                fis.read(bytesInputBuffer, 0, bytesToReadCount);


                if (!Arrays.equals(Arrays.copyOfRange(bytesBuffer, 0, bytesToReadCount), Arrays.copyOfRange(bytesInputBuffer, 0, bytesToReadCount))) {
                    wipeJob.errorMessage = "Error while verifying wipe file: streams are not the same!";
                    // Log.e("WipeAsyncTask", wipeJob.errorMessage);
                    return;
                }

                this.wipeJob.wipedBytes += bytesToReadCount;
                this.updateJobStatus();

                if (cancelled()) {
                    break;
                }
            }
        } catch (IOException e) {
            wipeJob.errorMessage = String.format("Error while verifying wipe file: %s", e.toString());
            // Log.e("WipeAsyncTask", wipeJob.errorMessage);
            return;
        }

        this.wipeJob.verifying = false;
        wipeJob.passes_completed++;
        this.deleteWipeFiles();
    }

    protected void onProgressUpdate(WipeJob... wipeJobs) {
        if (lastProgress != -1 && lastProgress == wipeJobs[0].getCurrentPassPercentageCompletion() && !wipeJobs[0].failed()) {
            return;
        }
        lastProgress = wipeJobs[0].getCurrentPassPercentageCompletion();
        this.mainActivity.setWipeProgress(wipeJobs[0]);
    }

    protected void onPostExecute(WipeJob result) {
        this.mainActivity.onWipeFinished(result);
    }

    /**
     * This function is required when mocking the class during
     * unit testing.
     */
    protected long getAvailableBytesCountInternal() {
        return WipeAsyncTask.getAvailableBytesCount();
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
