package com.example.nwipe_android;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.FileNotFoundException;

@RunWith(JUnit4.class)
public class WipeAsyncTaskTest {
    @Test
    public void testSimpleWipe() throws FileNotFoundException, InterruptedException {
        MockWipeAsyncTask asyncTask = new MockWipeAsyncTask();

        MemoryOutputStream out = new MemoryOutputStream();
        MemoryInputStream in = new MemoryInputStream();
        in.setBytes(out.getBytes());
        asyncTask.setStreams(in, out);

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
        Assert.assertEquals(out.getBytes().size(), asyncTask.DEFAULT_AVAILABLE_SPACE);
    }

    @Test
    public void testWithBlankingPass() throws FileNotFoundException, InterruptedException {
        MockWipeAsyncTask asyncTask = new MockWipeAsyncTask();

        MemoryOutputStream out = new MemoryOutputStream();
        MemoryInputStream in = new MemoryInputStream();
        in.setBytes(out.getBytes());
        asyncTask.setStreams(in, out);

        WipeJob wipeJob = new WipeJob();
        wipeJob.number_passes = 1;
        wipeJob.verify = false;
        wipeJob.blank = true;

        // Sanity check.
        Assert.assertEquals(wipeJob.isCompleted(), false);

        asyncTask.wipeJob = wipeJob;

        asyncTask.wipe();

        Assert.assertEquals(wipeJob.errorMessage, "");
        Assert.assertEquals(wipeJob.failed(), false);
        Assert.assertEquals(wipeJob.isCompleted(), true);
        Assert.assertEquals(out.getBytes().size(), asyncTask.DEFAULT_AVAILABLE_SPACE * 2);
    }

    @Test
    public void testOnlyBlankingPass() throws FileNotFoundException, InterruptedException {
        MockWipeAsyncTask asyncTask = new MockWipeAsyncTask();

        MemoryOutputStream out = new MemoryOutputStream();
        MemoryInputStream in = new MemoryInputStream();
        in.setBytes(out.getBytes());
        asyncTask.setStreams(in, out);

        WipeJob wipeJob = new WipeJob();
        wipeJob.number_passes = 0;
        wipeJob.verify = false;
        wipeJob.blank = true;

        // Sanity check.
        Assert.assertEquals(wipeJob.isCompleted(), false);

        asyncTask.wipeJob = wipeJob;

        asyncTask.wipe();

        Assert.assertEquals(wipeJob.errorMessage, "");
        Assert.assertEquals(wipeJob.failed(), false);
        Assert.assertEquals(wipeJob.isCompleted(), true);
        Assert.assertEquals(out.getBytes().size(), asyncTask.DEFAULT_AVAILABLE_SPACE);
    }

    @Test
    public void testVerify() throws FileNotFoundException, InterruptedException {
        MockWipeAsyncTask asyncTask = new MockWipeAsyncTask();

        MemoryOutputStream out = new MemoryOutputStream();
        MemoryInputStream in = new MemoryInputStream();
        in.setBytes(out.getBytes());
        asyncTask.setStreams(in, out);

        WipeJob wipeJob = new WipeJob();
        wipeJob.number_passes = 1;
        wipeJob.verify = true;
        wipeJob.blank = false;

        // Sanity check.
        Assert.assertEquals(wipeJob.isCompleted(), false);

        asyncTask.wipeJob = wipeJob;

        asyncTask.wipe();

        Assert.assertEquals(out.getBytes().size(), asyncTask.DEFAULT_AVAILABLE_SPACE);
        Assert.assertEquals(in.getBytes().size(), asyncTask.DEFAULT_AVAILABLE_SPACE);

        Assert.assertEquals(wipeJob.errorMessage, "");
        Assert.assertEquals(wipeJob.failed(), false);
        Assert.assertEquals(wipeJob.isCompleted(), true);
    }

    @Test
    public void testVerifyAndBlank() throws FileNotFoundException, InterruptedException {
        MockWipeAsyncTask asyncTask = new MockWipeAsyncTask();

        MemoryOutputStream out = new MemoryOutputStream();
        MemoryInputStream in = new MemoryInputStream();
        in.setBytes(out.getBytes());
        asyncTask.setStreams(in, out);

        WipeJob wipeJob = new WipeJob();
        wipeJob.number_passes = 1;
        wipeJob.verify = true;
        wipeJob.blank = false;

        // Sanity check.
        Assert.assertEquals(wipeJob.isCompleted(), false);

        asyncTask.wipeJob = wipeJob;

        asyncTask.wipe();

        Assert.assertEquals(out.getBytes().size(), asyncTask.DEFAULT_AVAILABLE_SPACE);
        Assert.assertEquals(in.getBytes().size(), asyncTask.DEFAULT_AVAILABLE_SPACE);

        Assert.assertEquals(wipeJob.errorMessage, "");
        Assert.assertEquals(wipeJob.failed(), false);
        Assert.assertEquals(wipeJob.isCompleted(), true);
    }

    @Test
    public void testMultiplePasses() throws FileNotFoundException, InterruptedException {
        MockWipeAsyncTask asyncTask = new MockWipeAsyncTask();

        MemoryOutputStream out = new MemoryOutputStream();
        MemoryInputStream in = new MemoryInputStream();
        in.setBytes(out.getBytes());
        asyncTask.setStreams(in, out);

        WipeJob wipeJob = new WipeJob();
        wipeJob.number_passes = 5;
        wipeJob.verify = true;
        wipeJob.blank = true;

        // Sanity check.
        Assert.assertEquals(wipeJob.isCompleted(), false);

        asyncTask.wipeJob = wipeJob;

        asyncTask.wipe();

        Assert.assertEquals(out.getBytes().size(), asyncTask.DEFAULT_AVAILABLE_SPACE * 6);
        Assert.assertEquals(in.getBytes().size(), asyncTask.DEFAULT_AVAILABLE_SPACE * 6);

        Assert.assertEquals(wipeJob.errorMessage, "");
        Assert.assertEquals(wipeJob.failed(), false);
        Assert.assertEquals(wipeJob.isCompleted(), true);

        // Testing that the passes are random.
        boolean equal = true;
        for (int i = 0; i < asyncTask.DEFAULT_AVAILABLE_SPACE; ++i) {
            equal &= (out.getBytes().get(i) == out.getBytes().get(i + asyncTask.DEFAULT_AVAILABLE_SPACE));
        }
        Assert.assertFalse("The first pass and the second pass should not be the same!", equal);
    }
}