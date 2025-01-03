package com.amazon.apl.android.providers;

import com.amazon.apl.viewhost.TimeProvider;

/**
 * Provider that will call subscribed listeners with the new local time offset if it changes due changes
 * in the default timezone (this includes daylight saving changes).
 * @deprecated Use {@link TimeProvider} instead.
 */
@Deprecated
public interface ILocalTimeOffsetProvider extends TimeProvider {
}
