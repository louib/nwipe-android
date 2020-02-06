package com.example.nwipe_android;

public class WipeStatus {
    public long totalBytes;
    public long wipedBytes = 0;
    public boolean succeeded = false;
    public WipeOperation currentOperation = WipeOperation.WIPING;
    public int passNumber = 1;

    public String toString() {
        return String.format("Pass %d: %s", this.passNumber, this.currentOperation);
    }

    public int getCurrentPassPercentageCompletion() {
        return (int)(((double)this.wipedBytes / (double)this.totalBytes) * 100);
    }
}
