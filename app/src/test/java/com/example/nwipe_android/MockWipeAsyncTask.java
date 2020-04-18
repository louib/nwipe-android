package com.example.nwipe_android;

import java.io.InputStream;
import java.io.OutputStream;

public class MockWipeAsyncTask extends WipeAsyncTask {
    public int DEFAULT_AVAILABLE_SPACE = 100000;

    private OutputStream out;
    private InputStream in;

    public void setStreams(InputStream in, OutputStream out) {
        this.in = in;
        this.out = out;
    }

    @Override
    protected OutputStream getOutputStream(String filename) {
        return out;
    }

    @Override
    protected InputStream getInputStream(String filename) {
        return in;
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