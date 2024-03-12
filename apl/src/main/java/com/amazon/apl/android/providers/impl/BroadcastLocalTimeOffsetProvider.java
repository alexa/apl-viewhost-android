package com.amazon.apl.android.providers.impl;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.amazon.apl.android.providers.ILocalTimeOffsetProvider;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.ListIterator;
import java.util.TimeZone;

/**
 * This BroadcastLocalTimeOffsetProvider works by determining timezone and DST changes via
 * monitoring changes in the local time when Intent.ACTION_TIME_TICK,
 * Intent.ACTION_TIMEZONE_CHANGED, or ACTION_TIME_CHANGED is broadcast from the OS.
 */
public class BroadcastLocalTimeOffsetProvider extends BroadcastReceiver implements ILocalTimeOffsetProvider {
    private final String TAG = BroadcastLocalTimeOffsetProvider.class.getSimpleName();

    private long mCurrentOffset;
    private final ArrayList<WeakReference<LocalTimeOffsetChangedListener>> mListeners = new ArrayList<>();

    public BroadcastLocalTimeOffsetProvider(Context context) {
        mCurrentOffset = getCurrentOffset();

        IntentFilter intentFilter = new IntentFilter();

        intentFilter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        intentFilter.addAction(Intent.ACTION_TIME_CHANGED);
        intentFilter.addAction(Intent.ACTION_TIME_TICK);

        context.registerReceiver(this, intentFilter);
    }

    @Override
    public void addListener(WeakReference<LocalTimeOffsetChangedListener> listener) {
        synchronized (mListeners) {
            mListeners.add(listener);
        }
    }

    @Override
    public long getCurrentOffset() {
        // Using TimeZone is the best way to calculate this but the TimeZone object was only
        // added in API 24.
        //return TimeZone.getDefault().getOffset(System.currentTimeMillis());
        Calendar now = Calendar.getInstance();
        return now.get(Calendar.ZONE_OFFSET) + now.get(Calendar.DST_OFFSET);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        long offset = getCurrentOffset();

        if (mCurrentOffset != offset) {
            Log.i(TAG, "Time offset change detected, notifying all listeners.");
            mCurrentOffset = offset;

            synchronized (mListeners) {
                ListIterator<WeakReference<LocalTimeOffsetChangedListener>> iter = mListeners.listIterator();
                while (iter.hasNext()) {
                    WeakReference<LocalTimeOffsetChangedListener> weakListener = iter.next();

                    LocalTimeOffsetChangedListener listener = weakListener.get();

                    if (listener == null) {
                        iter.remove();
                    } else {
                        listener.localTimeOffsetChanged(mCurrentOffset);
                    }
                }
            }
        }
    }
}
