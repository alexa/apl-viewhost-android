package com.amazon.apl.android.providers.impl;

import android.content.Context;

import com.amazon.apl.android.providers.ILocalTimeOffsetProvider;
import com.amazon.apl.viewhost.config.DefaultTimeProvider;

/**
 * To be used only from Legacy Pathway.
 * This BroadcastLocalTimeOffsetProvider works by determining timezone and DST changes via
 * monitoring changes in the local time when Intent.ACTION_TIME_TICK,
 * Intent.ACTION_TIMEZONE_CHANGED, or ACTION_TIME_CHANGED is broadcast from the OS.
 * To be used as default LocalTimeOffsetProvider only from Legacy pathway.
 * Can be removed once all the clients have moved to Unified APIs.
 * @deprecated Use {@link DefaultTimeProvider} instead.
 */
@Deprecated
public class BroadcastLocalTimeOffsetProvider extends DefaultTimeProvider implements ILocalTimeOffsetProvider {
    public BroadcastLocalTimeOffsetProvider(Context context) {
        super(context);
    }
}
