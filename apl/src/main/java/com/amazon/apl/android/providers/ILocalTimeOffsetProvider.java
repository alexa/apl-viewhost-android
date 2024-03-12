package com.amazon.apl.android.providers;

import java.lang.ref.WeakReference;

/**
 * Provider that will call subscribed listeners with the new local time offset if it changes due changes
 * in the default timezone (this includes daylight saving changes).
 */
public interface ILocalTimeOffsetProvider {

    void addListener(WeakReference<LocalTimeOffsetChangedListener> listener);

    long getCurrentOffset();

    interface LocalTimeOffsetChangedListener {
        void localTimeOffsetChanged(long newOffset);
    }
}
