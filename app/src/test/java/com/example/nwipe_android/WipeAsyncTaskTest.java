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
        WipeAsyncTask asyncTask = mock(WipeAsyncTask.class);

        doReturn((long) 10000).when(asyncTask).getAvailableBytesCountInternal();

        //when(asyncTask.getAvailableBytesCount()).thenReturn((long) 100000);
        //when(WipeAsyncTask.getTotalBytesCount()).thenReturn((long) 100000);


        MemoryInputStream inputStream = new MemoryInputStream();
        MemoryOutputStream outputStream = new MemoryOutputStream();

        doReturn(outputStream).when(asyncTask).getOutputStream(anyString());
        when(asyncTask.getInputStream(anyString())).thenReturn(inputStream);

        WipeJob wipeJob = new WipeJob();
        wipeJob.number_passes = 1;
        wipeJob.verify = false;
        wipeJob.blank = false;

        asyncTask.executeWipePass();

        Assert.assertEquals(outputStream.getBytes().size(), 5);



    }
}