package com.example.nwipe_android;

import android.content.Context;
import android.view.View;
import android.widget.SeekBar;

import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.ViewAssertion;
import androidx.test.espresso.action.ViewActions;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.ActivityTestRule;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.*;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class MainActivityTest {
    @Rule
    public ActivityTestRule<MainActivity> mActivityRule = new ActivityTestRule<>(MainActivity.class);

    @Test
    public void useAppContext() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();

        assertEquals("com.example.nwipe_android", appContext.getPackageName());
    }

    @Test
    public void testNumberPassesSeekBar() {
        onView(withId(R.id.number_passes_seek_bar))
                .check(new ViewAssertion() {
                    @Override
                    public void check(View view, NoMatchingViewException noViewFoundException) {
                        SeekBar numberPassesSeekBar = (SeekBar) view;
                        Assert.assertEquals(numberPassesSeekBar.getProgress(), 2);
                    }
                });

        onView(withId(R.id.number_passes_seek_bar))
                .perform(ViewActions.click())
                .check(new ViewAssertion() {
                    @Override
                    public void check(View view, NoMatchingViewException noViewFoundException) {
                        SeekBar numberPassesSeekBar = (SeekBar) view;
                        Assert.assertEquals(numberPassesSeekBar.getProgress(), 5);
                    }
                });

    }
}
