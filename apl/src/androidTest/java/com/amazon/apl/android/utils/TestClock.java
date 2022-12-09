package com.amazon.apl.android.utils;

import com.amazon.apl.android.IClock;

public class TestClock implements IClock {

    private IClockCallback mCallback;

    public TestClock(IClockCallback callback) {
        this.mCallback = callback;
    }

    @Override
    public void start() {
        //Do nothing
    }

    @Override
    public void stop() {
        //Do nothing
    }

    public void doFrameUpdate(long frameTime) {
        mCallback.onTick(frameTime);
    }
}
