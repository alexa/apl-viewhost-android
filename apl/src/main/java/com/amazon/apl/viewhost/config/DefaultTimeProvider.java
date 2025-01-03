package com.amazon.apl.viewhost.config;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.amazon.apl.viewhost.TimeProvider;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.ListIterator;

/**
 * To be used as a default impl from unified APIs pathway.
 * Default implementation for TimeProvider interface.
 * This DefaultTimeProvider works by determining timezone and DST changes via
 * monitoring changes in the local time when Intent.ACTION_TIME_TICK,
 * Intent.ACTION_TIMEZONE_CHANGED, or ACTION_TIME_CHANGED is broadcast from the OS.
 */
public class DefaultTimeProvider extends BroadcastReceiver implements TimeProvider {
    private final String TAG = DefaultTimeProvider.class.getSimpleName();

    private long mCurrentOffset;
    private final ArrayList<WeakReference<LocalTimeOffsetListener>> mListeners = new ArrayList<>();

    public DefaultTimeProvider(Context context) {
        mCurrentOffset = getLocalTimeOffset();

        IntentFilter intentFilter = new IntentFilter();

        intentFilter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        intentFilter.addAction(Intent.ACTION_TIME_CHANGED);
        intentFilter.addAction(Intent.ACTION_TIME_TICK);

        context.registerReceiver(this, intentFilter);
    }

    @Override
    public void addListener(WeakReference<LocalTimeOffsetListener> listener) {
        synchronized (mListeners) {
            mListeners.add(listener);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        long offset = getLocalTimeOffset();

        if (mCurrentOffset != offset) {
            Log.i(TAG, "Time offset change detected, notifying all listeners.");
            mCurrentOffset = offset;

            synchronized (mListeners) {
                ListIterator<WeakReference<LocalTimeOffsetListener>> iter = mListeners.listIterator();
                while (iter.hasNext()) {
                    WeakReference<LocalTimeOffsetListener> weakListener = iter.next();

                    LocalTimeOffsetListener listener = weakListener.get();

                    if (listener == null) {
                        iter.remove();
                    } else {
                        listener.onLocalTimeOffsetUpdated(mCurrentOffset);
                    }
                }
            }
        }
    }

    @Override
    public long getLocalTimeOffset() {
        // Using TimeZone is the best way to calculate this but the TimeZone object was only
        // added in API 24.
        //return TimeZone.getDefault().getOffset(System.currentTimeMillis());
        Calendar now = Calendar.getInstance();
        return now.get(Calendar.ZONE_OFFSET) + now.get(Calendar.DST_OFFSET);
    }

    @Override
    public long getUTCTime() {
        // Get current time in milliseconds since epoch
        return System.currentTimeMillis();
    }
}

