package com.example.nwipe_android;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class MemoryOutputStream extends OutputStream {
    private List<Byte> bytes = new ArrayList<Byte>();

    @Override
    public void write(int b) throws IOException {
        bytes.add((byte) b);
    }

    public List<Byte> getBytes() {
        return this.bytes;
    }
}
