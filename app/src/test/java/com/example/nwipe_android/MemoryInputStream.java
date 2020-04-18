package com.example.nwipe_android;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class MemoryInputStream extends InputStream {
    private List<Byte> bytes = new ArrayList<Byte>();
    private int index = 0;

    @Override
    public int read() throws IOException {
        return this.bytes.get(index++);
    }

    public void setBytes(List<Byte> bytes) {
        this.bytes = bytes;
    }

    public List<Byte> getBytes() {
        return this.bytes;
    }
}
