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

public class WipeAsyncTask extends AsyncTask <WipeJob, WipeJob, WipeJob> {
    public static int WIPE_BUFFER_SIZE = 4096;
    public static String WIPE_FILES_PREFIX = "nwipe-android-";

    private MainActivity mainActivity;
    private Context context;
    private WipeJob wipeJob;
    private int lastProgress = -1;

    public WipeAsyncTask(MainActivity mainActivity) {
        super();
        this.mainActivity = mainActivity;
    }

    @Override
    protected WipeJob doInBackground(WipeJob... wipeJobs) {
        this.wipeJob = wipeJobs[0];
        this.context = this.mainActivity.getApplicationContext();

        this.deleteWipeFiles();

        while (!wipeJob.isCompleted()) {
            try {
                this.executeWipePass();
            } catch (Exception e) {
                this.deleteWipeFiles();
                wipeJob.errorMessage = String.format("Unknown error while wiping: %s", e.toString());
                Log.e("WipeAsyncTask", wipeJob.errorMessage);
                this.publishProgress(this.wipeJob);
                return wipeJob;
            }
            this.deleteWipeFiles();
            this.publishProgress(this.wipeJob);

            if (this.wipeJob.failed() || this.isCancelled()) {
                return this.wipeJob;
            }
        }

        return this.wipeJob;
    }

    private void deleteWipeFiles() {
        File filesDir = this.context.getFilesDir();
        for (String fileName: filesDir.list()) {
            if (fileName.startsWith(WipeAsyncTask.WIPE_FILES_PREFIX)) {
                Log.i("WipeAsyncTask", String.format("Deleting old wipe file %s.", fileName));
                this.context.deleteFile(fileName);
            }
        }
    }

    private void executeWipePass() {
        long availableBytesCount = WipeAsyncTask.getAvailableBytesCount();
        Log.i("MainActivity", String.format("Got %d bytes available for writing.", availableBytesCount));
        this.wipeJob.totalBytes = availableBytesCount;
        this.wipeJob.wipedBytes = 0;
        this.publishProgress(this.wipeJob);

        String wipeFileName = String.format("%s%d", WIPE_FILES_PREFIX, System.currentTimeMillis());

        SecureRandom random = new SecureRandom();
        // TODO verify that this is a proper way of seeding.
        int randomSeed = random.nextInt();

        // FIXME where no doing the blanking yet!

        Log.i("WipeAsyncTask", "Starting wipe operation.");
        try (FileOutputStream fos = context.openFileOutput(wipeFileName, Context.MODE_PRIVATE)) {
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
                this.publishProgress(this.wipeJob);

                if (isCancelled()) {
                    break;
                }
            }
        } catch (FileNotFoundException e) {
            wipeJob.errorMessage = String.format("Error while wiping: %s", e.toString());
            Log.e("WipeAsyncTask", wipeJob.errorMessage);
            return;
        } catch (IOException e) {
            // Handling no space left errors at the end of the pass.
            if (e.toString().contains("ENOSP") && wipeJob.getCurrentPassPercentageCompletion() >= WipeJob.MIN_PERCENTAGE_COMPLETION) {
                this.wipeJob.totalBytes = this.wipeJob.wipedBytes;
            } else {
                wipeJob.errorMessage = String.format("Error while wiping: %s", e.toString());
                Log.e("WipeAsyncTask", wipeJob.errorMessage);
                return;
            }
        }

        this.wipeJob.wipedBytes = 0;
        this.publishProgress(this.wipeJob);

        if (!wipeJob.verify) {
            wipeJob.passes_completed++;
            return;
        }

        this.wipeJob.verifying = true;

        Log.i("WipeAsyncTask", "Starting verifying operation.");
        try (FileInputStream fis = context.openFileInput(wipeFileName)) {
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
                    Log.e("WipeAsyncTask", wipeJob.errorMessage);
                    return;
                }

                this.wipeJob.wipedBytes += bytesToReadCount;
                this.publishProgress(this.wipeJob);

                if (isCancelled()) {
                    break;
                }
            }
        } catch (FileNotFoundException e) {
            wipeJob.errorMessage = String.format("Error while verifying wipe file: %s", e.toString());
            Log.e("WipeAsyncTask", wipeJob.errorMessage);
            return;
        } catch (IOException e) {
            wipeJob.errorMessage = String.format("Error while verifying wipe file: %s", e.toString());
            Log.e("WipeAsyncTask", wipeJob.errorMessage);
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
