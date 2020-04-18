package com.example.nwipe_android;

import android.renderscript.ScriptGroup;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(JUnit4.class)
public class WipeAsyncTaskTest {
    @Test
    public void testSimpleWipe() throws FileNotFoundException, InterruptedException {
        WipeAsyncTask asyncTask = new MockWipeAsyncTask();

        WipeJob wipeJob = new WipeJob();
        wipeJob.number_passes = 1;
        wipeJob.verify = false;
        wipeJob.blank = false;

        // Sanity check.
        Assert.assertEquals(wipeJob.isCompleted(), false);

        asyncTask.wipeJob = wipeJob;

        asyncTask.wipe();

        Assert.assertEquals(wipeJob.failed(), false);
        Assert.assertEquals(wipeJob.errorMessage, "");
        Assert.assertEquals(wipeJob.isCompleted(), true);
    }

    @Test
    public void testOnlyBlankingPass() throws FileNotFoundException, InterruptedException {
        WipeAsyncTask asyncTask = new MockWipeAsyncTask();

        WipeJob wipeJob = new WipeJob();
        wipeJob.number_passes = 0;
        wipeJob.verify = false;
        wipeJob.blank = true;

        // Sanity check.
        Assert.assertEquals(wipeJob.isCompleted(), false);

        asyncTask.wipeJob = wipeJob;

        asyncTask.wipe();

        Assert.assertEquals(wipeJob.failed(), false);
        Assert.assertEquals(wipeJob.errorMessage, "");
        Assert.assertEquals(wipeJob.isCompleted(), true);
    }
}