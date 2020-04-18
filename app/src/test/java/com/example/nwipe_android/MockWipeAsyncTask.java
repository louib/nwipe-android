package com.example.nwipe_android;

import java.io.InputStream;
import java.io.OutputStream;

public class MockWipeAsyncTask extends WipeAsyncTask {
    public int DEFAULT_AVAILABLE_SPACE = 100000;

    @Override
    protected OutputStream getOutputStream(String filename) {
        return new MemoryOutputStream();
    }

    @Override
    protected InputStream getInputStream(String filename) {
        return new MemoryInputStream();
    }

    @Override
    protected void deleteWipeFiles() {

    }

    @Override
    protected void updateJobStatus() {

    }

    @Override
    protected boolean cancelled() {
        return false;
    }

    @Override
    protected long getAvailableBytesCountInternal() {
        return this.DEFAULT_AVAILABLE_SPACE;
    }
}