/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.amazon.apl.android.utils.AccessibilitySettingsUtil;
import com.amazon.apl.enums.APLEnum;
import com.amazon.apl.enums.AnimationQuality;
import com.amazon.apl.enums.RootProperty;
import com.amazon.apl.enums.ScreenMode;

import java.util.Calendar;
import java.util.Map;

/**
 * Configuration settings used when rendering an APL Document.
 */
@SuppressWarnings("WeakerAccess")
public class RootConfig extends BoundObject {
    /**
     * Creates a default RootConfig.
     */
    private RootConfig() {
        long handle = nCreate();
        bind(handle);
    }

    /**
     * Creates a default RootConfig with time initialized to now.
     *
     * @return A {@link RootConfig} instance as {@link BoundObject}.
     */
    public static RootConfig create() {
        final long currentTime = System.currentTimeMillis();
        final Calendar now = Calendar.getInstance();
        final long offset = now.get(Calendar.ZONE_OFFSET) + now.get(Calendar.DST_OFFSET);

        return new RootConfig()
                .utcTime(currentTime)
                .localTimeAdjustment(offset);
    }

    /**
     * Creates a default RootConfig with time initialized to now and accessibility preferences by user.
     *
     * @return A {@link RootConfig} instance as {@link BoundObject}.
     */
    public static RootConfig create(Context context) {
        AccessibilitySettingsUtil util = AccessibilitySettingsUtil.getInstance();
        float fontScale = util.getFontScale(context);
        ScreenMode screenMode = util.isHighContrast(context) ? ScreenMode.kScreenModeHighContrast : ScreenMode.kScreenModeNormal;
        boolean screenReader = util.isScreenReaderEnabled(context);

        return create()
                .fontScale(fontScale)
                .screenMode(screenMode)
                .screenReader(screenReader);
    }

    /**
     * Creates a default RootConfig with an agent name and version.
     *
     * @param agentName    The name of the application client
     * @param agentVersion The version of the application client
     * @return A {@link RootConfig} instance as {@link BoundObject}.
     */
    static public RootConfig create(@NonNull String agentName, @NonNull String agentVersion) {
        return create()
                .agent(agentName, agentVersion);
    }


    /**
     * Set a configuration property.
     *
     * @param name  The property string identifier.
     * @param value The property value.
     * @return This object for chaining.
     */
    public RootConfig set(String name, Object value) {
        nSetByName(getNativeHandle(), name,
                value instanceof APLEnum ? ((APLEnum) value).getIndex() : value);
        return this;
    }

    /**
     * Set a configuration property.
     *
     * @param property The property identifier.
     * @param value    The property value.
     * @return This object for chaining.
     */
    public RootConfig set(RootProperty property, Object value) {
        nSetByProperty(getNativeHandle(), property.getIndex(),
                value instanceof APLEnum ? ((APLEnum) value).getIndex() : value);
        return this;
    }

    /**
     * Set multiple configuration properties.
     *
     * @param properties A collection of configuration properties.
     * @return This object for chaining.
     */
    public RootConfig set(Map<RootProperty, Object> properties) {
        // Loop over and set individually.  This eliminates the jni overhead
        // of converting a map keyed with RootProperty (which requires calls into the enum class)
        for (Map.Entry<RootProperty, Object> property : properties.entrySet()) {
            set(property.getKey(), property.getValue());
        }
        return this;
    }

    /**
     * Get a configuration property
     *
     * @param property The property identifier.
     * @return The property value.
     */
    public Object getProperty(RootProperty property) {
        return nGetProperty(getNativeHandle(), property.getIndex());
    }

    /**
     * Sets the agent name and version.
     *
     * @param agentName    The name of the application client
     * @param agentVersion The version of the application client
     * @return this for chaining
     * @deprecated use {@link #set(RootProperty, Object)}
     */
    @NonNull
    @Deprecated
    public RootConfig agent(@NonNull String agentName, @NonNull String agentVersion) {
        nAgent(getNativeHandle(), agentName, agentVersion);
        return this;
    }

    /**
     * Sets whether the video component is disabled.
     *
     * @param disallowVideo true if the video is disabled,
     *                      false otherwise
     * @return this for chaining
     * @deprecated use {@link #set(RootProperty, Object)}
     */
    @NonNull
    @Deprecated
    public RootConfig disallowVideo(boolean disallowVideo) {
        nDisallowVideo(getNativeHandle(), disallowVideo);
        return this;
    }

    /**
     * Sets whether the OpenUrlCommand is enabled.
     *
     * @param allowOpenUrl true if the OpenUrlCommand is enabled,
     *                     false otherwise
     * @return this for chaining
     * @deprecated use {@link #set(RootProperty, Object)}
     */
    @NonNull
    @Deprecated
    public RootConfig allowOpenUrl(boolean allowOpenUrl) {
        nAllowOpenUrl(getNativeHandle(), allowOpenUrl);
        return this;
    }

    /**
     * Sets the animation quality to enable/disable animations.
     *
     * @param animationQuality the animation quality
     * @return this for chaining
     * @deprecated use {@link #set(RootProperty, Object)}
     */
    @NonNull
    @Deprecated
    public RootConfig animationQuality(@NonNull AnimationQuality animationQuality) {
        nAnimationQuality(getNativeHandle(), animationQuality.getIndex());
        return this;
    }

    /**
     * Sets the current UTC time in milliseconds since the epoch.
     *
     * @param utcTime milliseconds
     * @return this for chaining
     * @deprecated use {@link #set(RootProperty, Object)}
     */
    @NonNull
    @Deprecated
    public RootConfig utcTime(long utcTime) {
        nUTCTime(getNativeHandle(), utcTime);
        return this;
    }

    /**
     * Sets the local time adjustment in milliseconds. When added to the current UTC time,
     * this will give the local time. THis includes any daylight saving time adjustment.
     *
     * @param adjustment milliseconds
     * @return this for chaining
     * @deprecated use {@link #set(RootProperty, Object)}
     */
    @NonNull
    @Deprecated
    public RootConfig localTimeAdjustment(long adjustment) {
        nLocalTimeAdjustment(getNativeHandle(), adjustment);
        return this;
    }

    /**
     * Sets a {@link LiveArray} to the top level context.
     *
     * @param name      the name of the LiveArray.
     * @param liveArray the data
     * @return this for chaining
     */
    @NonNull
    public RootConfig liveData(@NonNull String name, @NonNull LiveArray liveArray) {
        nLiveData(getNativeHandle(), name, liveArray.getNativeHandle());
        return this;
    }

    /**
     * Sets a {@link LiveMap} to the top level context.
     *
     * @param name    the name of the LiveMap.
     * @param liveMap the data
     * @return this for chaining
     */
    @NonNull
    public RootConfig liveData(@NonNull String name, @NonNull LiveMap liveMap) {
        nLiveData(getNativeHandle(), name, liveMap.getNativeHandle());
        return this;
    }

    /**
     * @return The agent name configured.
     * @deprecated use {@link #getProperty(RootProperty)}
     */
    @NonNull
    @Deprecated
    public String getAgentName() {
        return nGetAgentName(getNativeHandle());
    }

    /**
     * @return The agent version configured.
     * @deprecated use {@link #getProperty(RootProperty)}
     */
    @NonNull
    @Deprecated
    public String getAgentVersion() {
        return nGetAgentVersion(getNativeHandle());
    }

    /**
     * @return true if video is supported.
     * @deprecated use {@link #getProperty(RootProperty)}
     */
    @Deprecated
    public boolean getDisallowVideo() {
        return nGetDisallowVideo(getNativeHandle());
    }

    /**
     * @return true if the OpenURL command is supported.
     * @deprecated use {@link #getProperty(RootProperty)}
     */
    @Deprecated
    public boolean getAllowOpenUrl() {
        return nGetAllowOpenUrl(getNativeHandle());
    }

    /**
     * @return The expected quality of animation playback.
     * @deprecated use {@link #getProperty(RootProperty)}
     */
    @NonNull
    @Deprecated
    public AnimationQuality getAnimationQuality() {
        return AnimationQuality.valueOf(nGetAnimationQuality(getNativeHandle()));
    }

    /**
     * @return the current UTC time in milliseconds since the epoch.
     * @deprecated use {@link #getProperty(RootProperty)}
     */
    @Deprecated
    public long getUTCTime() {
        return nGetUTCTime(getNativeHandle());
    }

    /**
     * @return the local time adjustment in milliseconds.
     * @deprecated use {@link #getProperty(RootProperty)}
     */
    @Deprecated
    public long getLocalTimeAdjustment() {
        return nGetLocalTimeAdjustment(getNativeHandle());
    }

    /**
     * @return The requested scaling factor for fonts.
     * @deprecated use {@link #getProperty(RootProperty)}
     */
    @Deprecated
    public float getFontScale() {
        return nGetFontScale(getNativeHandle());
    }

    /**
     * @return The current screen mode (high-contrast or normal)
     * @deprecated use {@link #getProperty(RootProperty)}
     */
    @Deprecated
    public String getScreenMode() {
        return nGetScreenMode(getNativeHandle());
    }

    /**
     * @return The current screen mode enum.
     * @deprecated use {@link #getProperty(RootProperty)}
     */
    @Deprecated
    public ScreenMode getScreenModeEnumerated() {
        return ScreenMode.valueOf(nGetScreenModeEnumerated(getNativeHandle()));
    }

    /**
     * @return True if an accessibility screen reader is enabled
     * @deprecated use {@link #getProperty(RootProperty)}
     */
    @Deprecated
    public boolean getScreenReader() {
        return nGetScreenReader(getNativeHandle());
    }

    /**
     * @return Double press timeout in milliseconds.
     * @deprecated use {@link #getProperty(RootProperty)}
     */
    @Deprecated
    public int getDoublePressTimeout() {
        return nGetDoublePressTimeout(getNativeHandle());
    }

    /**
     * @return Long press timeout in milliseconds.
     * @deprecated use {@link #getProperty(RootProperty)}
     */
    @Deprecated
    public int getLongPressTimeout() {
        return nGetLongPressTimeout(getNativeHandle());
    }

    /**
     * @return Fling velocity threshold
     * @deprecated use {@link #getProperty(RootProperty)}
     */
    @Deprecated
    public int getMinimumFlingVelocity() {
        return nGetMinimumFlingVelocity(getNativeHandle());
    }

    /**
     * @return Duration to show the "pressed" state of a component when programmatically invoked
     * @deprecated use {@link #getProperty(RootProperty)}
     */
    @Deprecated
    public int getPressedDuration() {
        return nGetPressedDuration(getNativeHandle());
    }

    /**
     * @return Maximum time to wait before deciding that a touch event starts a scroll or paging gesture.
     * @deprecated use {@link #getProperty(RootProperty)}
     */
    @Deprecated
    public int getTapOrScrollTimeout() {
        return nGetTapOrScrollTimeout(getNativeHandle());
    }


    /**
     * Register a supported extension.
     *
     * @param uri The URI of the extension
     * @return This object for chaining
     */
    @NonNull
    public RootConfig registerExtension(@NonNull String uri) {
        nRegisterExtension(getNativeHandle(), uri);
        return this;
    }

    /**
     * Register an environment for an extension.  The document may access the extension environment by
     * the extension name in the “environment.extension” environment property.
     * Any previously registered environment is overwritten.
     * This method will also register the extension as a supported extension.
     *
     * @param uri         The URI of the extension
     * @param environment values
     * @return This object for chaining
     */
    @NonNull
    public RootConfig registerExtensionEnvironment(@NonNull String uri, @NonNull Object environment) {
        nRegisterExtensionEnvironment(getNativeHandle(), uri, environment);
        return this;
    }

    /**
     * Register an extension event handler.  The name should be something like 'onDomainAction'.
     * This method will also register the extension as a supported extension.
     *
     * @param handler The extension event handler.
     * @return This object for chaining.
     */
    @NonNull
    public RootConfig registerExtensionEventHandler(@NonNull ExtensionEventHandler handler) {
        nRegisterExtensionEventHandler(getNativeHandle(), handler.getNativeHandle());
        return this;
    }

    /**
     * Register an extension command that can be executed in the document.  The name should be something like 'DomainEvent'.
     * This method will also register the extension as a supported extension.
     * Once registered, changes to the extension command have no impact.
     *
     * @param commandDef The definition of the custom command (includes the name, URI, etc).
     * @return This object for chaining
     */
    @NonNull
    public RootConfig registerExtensionCommand(@NonNull ExtensionCommandDefinition commandDef) {
        nRegisterExtensionCommand(getNativeHandle(), commandDef.getNativeHandle());
        return this;
    }

    @NonNull
    public RootConfig registerExtensionFilter(@NonNull ExtensionFilterDefinition filterDef) {
        nRegisterExtensionFilter(getNativeHandle(), filterDef.getNativeHandle());
        return this;
    }

    /**
     * Register a dynamic DataSource.
     *
     * @param type The Data Source provider type, currently only "dynamicIndexList" is supported.
     * @return this
     */
    @NonNull
    public RootConfig registerDataSource(final String type) {
        nRegisterDataSource(getNativeHandle(), type);
        return this;
    }

    /**
     * Set sequence layout cache in both directions. 1 is default and results in 1 page ensured before and one after
     * current one.
     *
     * @param cacheSize Number of pages to ensure before and after current one.
     * @return This object for chaining.
     * @deprecated use {@link #set(RootProperty, Object)}
     */
    @Deprecated
    public RootConfig sequenceChildCache(final int cacheSize) {
        nSequenceChildCache(getNativeHandle(), cacheSize);
        return this;
    }

    /**
     * Set pager layout cache in both directions. 1 is default and results in 1 page ensured before and one after
     * current one.
     *
     * @param cacheSize Number of pages to ensure before and after current one.
     * @return This object for chaining.
     * @deprecated use {@link #set(RootProperty, Object)}
     */
    @Deprecated
    public RootConfig pagerChildCache(final int cacheSize) {
        nPagerChildCache(getNativeHandle(), cacheSize);
        return this;
    }

    /**
     * Set the requested font scaling factor for the document.
     *
     * @param scale The scaling factor. Default is 1.0
     * @return This object for chaining
     * @deprecated use {@link #set(RootProperty, Object)}
     */
    @Deprecated
    public RootConfig fontScale(final float scale) {
        nFontScale(getNativeHandle(), scale);
        return this;
    }

    /**
     * Set the screen display mode for accessibility (normal or high-contrast)
     *
     * @param screenMode The screen display mode
     * @return This object for chaining
     * @deprecated use {@link #set(RootProperty, Object)}
     */
    @Deprecated
    public RootConfig screenMode(final ScreenMode screenMode) {
        nScreenMode(getNativeHandle(), screenMode.getIndex());
        return this;
    }

    /**
     * Set double press timeout.
     *
     * @param timeout new double press timeout. Default is 500 ms.
     * @return This object for chaining
     * @deprecated use {@link #set(RootProperty, Object)}
     */
    @Deprecated
    public RootConfig doublePressTimeout(final int timeout) {
        nDoublePressTimeout(getNativeHandle(), timeout);
        return this;
    }

    /**
     * Set long press timeout.
     *
     * @param timeout new long press timeout. Default is 1000 ms.
     * @return This object for chaining
     * @deprecated use {@link #set(RootProperty, Object)}
     */
    @Deprecated
    public RootConfig longPressTimeout(final int timeout) {
        nLongPressTimeout(getNativeHandle(), timeout);
        return this;
    }

    /**
     * Set the fling velocity threshold.  The user must fling at least this fast to start a fling action.
     *
     * @param velocity Fling velocity in dp per second.
     * @return This object for chaining
     * @deprecated use {@link #set(RootProperty, Object)}
     */
    @Deprecated
    public RootConfig minimumFlingVelocity(final int velocity) {
        nMinimumFlingVelocity(getNativeHandle(), velocity);
        return this;
    }

    /**
     * Set pressed duration timeout.  This is the duration to show the "pressed" state of a component
     * when programmatically invoked.
     *
     * @param timeout Duration in milliseconds.  Default is 64 ms.
     * @return This object for chaining
     * @deprecated use {@link #set(RootProperty, Object)}
     */
    @Deprecated
    public RootConfig pressedDuration(final int timeout) {
        nPressedDuration(getNativeHandle(), timeout);
        return this;
    }

    /**
     * Set the tap or scroll timeout.  This is the maximum amount of time that can pass before the
     * system has to commit to this being a touch event instead of a scroll event.
     *
     * @param timeout Duration in milliseconds.  Default is 100 ms.
     * @return This object for chaining
     * @deprecated use {@link #set(RootProperty, Object)}
     */
    @Deprecated
    public RootConfig tapOrScrollTimeout(final int timeout) {
        nTapOrScrollTimeout(getNativeHandle(), timeout);
        return this;
    }

    /**
     * Inform that a screen reader is turned on.
     *
     * @param enabled True if the screen reader is enabled
     * @return This object for chaining
     * @deprecated use {@link #set(RootProperty, Object)}
     */
    @Deprecated
    public RootConfig screenReader(final boolean enabled) {
        nScreenReader(getNativeHandle(), enabled);
        return this;
    }

    private static native long nCreate();

    private static native void nSetByName(long nativeHandle, String name, Object value);

    private static native void nSetByProperty(long nativeHandle, int property, Object value);

    private static native Object nGetProperty(long nativeHandle, int property);

    private static native long nAgent(long nativeHandle, String agentName, String agentVersion);

    @NonNull
    private static native String nGetAgentName(long nativeHandle);

    @NonNull
    private static native String nGetAgentVersion(long nativeHandle);

    private static native boolean nGetDisallowVideo(long nativeHandle);

    private static native boolean nGetAllowOpenUrl(long nativeHandle);

    private static native int nGetAnimationQuality(long nativeHandle);

    private static native long nGetUTCTime(long nativeHandle);

    private static native long nGetLocalTimeAdjustment(long nativeHandle);

    private static native float nGetFontScale(long nativeHandle);

    private static native String nGetScreenMode(long nativeHandle);

    private static native int nGetScreenModeEnumerated(long nativeHandle);

    private static native boolean nGetScreenReader(long nativeHandle);

    private static native int nGetDoublePressTimeout(long nativeHandle);

    private static native int nGetLongPressTimeout(long nativeHandle);

    private static native int nGetMinimumFlingVelocity(long nativeHandle);

    private static native int nGetPressedDuration(long nativeHandle);

    private static native int nGetTapOrScrollTimeout(long nativeHandle);

    private static native void nAllowOpenUrl(long nativeHandle, boolean allowOpenUrl);

    private static native void nDisallowVideo(long nativeHandle, boolean disallowVideo);

    private static native void nAnimationQuality(long nativeHandle, int animationQuality);

    private static native void nUTCTime(long nativeHandle, long utcTime);

    private static native void nLocalTimeAdjustment(long nativeHandle, long adjustment);

    private static native void nLiveData(long nativeHandle, String name, long liveDataHandle);

    private static native void nRegisterExtension(long nativeHandle, String uri);

    private static native void nRegisterExtensionEnvironment(long nativeHandle, String uri, Object environment);

    private static native void nRegisterExtensionEventHandler(long nativeHandle, long handlerNativeHandle);

    private static native void nRegisterExtensionCommand(long nativeHandle, long commandNativeHandle);

    private static native void nRegisterExtensionFilter(long nativeHandle, long commandNativeHandle);

    private static native void nRegisterDataSource(long nativeHandle, String type);

    private static native void nSequenceChildCache(long nativeHandle, int cacheSize);

    private static native void nPagerChildCache(long nativehandle, int cacheSize);

    private static native void nFontScale(long nativehandle, float scale);

    private static native void nScreenMode(long nativehandle, int screenMode);

    private static native void nScreenReader(long nativehandle, boolean enabled);

    private static native void nDoublePressTimeout(long nativehandle, int timeout);

    private static native void nLongPressTimeout(long nativehandle, int timeout);

    private static native void nMinimumFlingVelocity(long nativehandle, int velocity);

    private static native void nPressedDuration(long nativehandle, int timeout);

    private static native void nTapOrScrollTimeout(long nativehandle, int timeout);
}
