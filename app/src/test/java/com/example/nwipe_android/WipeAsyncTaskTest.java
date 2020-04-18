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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(JUnit4.class)
public class WipeAsyncTaskTest {
    @Test
    public void testSimpleWipe() throws FileNotFoundException {
        WipeAsyncTask asyncTask = mock(WipeAsyncTask.class);

        when(WipeAsyncTask.getAvailableBytesCount()).thenReturn((long) 100000);

        InputStream inputStream = new MemoryInputStream();
        OutputStream outputStream = new MemoryOutputStream();

        when(asyncTask.getOutputStream(anyString())).thenReturn(outputStream);
        when(asyncTask.getInputStream(anyString())).thenReturn(inputStream);

        WipeJob wipeJob = new WipeJob();
        wipeJob.number_passes 1;
        wipeJob.verify = false;
        wipeJob.blank = false;


        asyncTask.doInBackground();
    }
}