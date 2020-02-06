package com.example.nwipe_android;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class WipeStatusTest {
    private WipeStatus status;

    @Before
    public void setUp() {
        this.status = new WipeStatus();
        this.status.totalBytes = 1000;
    }

    @Test
    public void testNotStarted() {
        this.status.wipedBytes = 0;
        Assert.assertEquals(this.status.getCurrentPassPercentageCompletion(), 0);
    }

    @Test
    public void testStarted() {
        this.status.wipedBytes = 500;
        Assert.assertEquals(this.status.getCurrentPassPercentageCompletion(), 50);
    }

    @Test
    public void testFinished() {
        this.status.wipedBytes = this.status.totalBytes;
        Assert.assertEquals(this.status.getCurrentPassPercentageCompletion(), 100);
    }

}
