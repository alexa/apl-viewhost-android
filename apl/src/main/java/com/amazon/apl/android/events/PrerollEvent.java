/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.events;

import androidx.annotation.NonNull;
import android.util.Log;

import com.amazon.apl.android.Event;
import com.amazon.apl.android.RootContext;
import com.amazon.apl.android.dependencies.ITtsSourceProvider;
import com.amazon.apl.android.providers.ITtsPlayerProvider;
import com.amazon.apl.enums.EventProperty;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.URL;


/**
 * @deprecated
 * TODO [JIRA-28202] Delete this and automatically update consumers to new api
 * This class is deprecated in favor of {@link com.amazon.apl.android.audio.AudioPlayer} since
 * TTS events are now driven by APLCore and do not need to be handled using PrerollEvent.
 * This class will be removed in future versions.
 * APL Set Page Event
 * See @{link <a https://developer.amazon.com/docs/alexa-presentation-language/apl-commands-media.html#playmedia>
 * APL Command Specification</a>}
 */
@Deprecated
public class PrerollEvent extends Event {

    private static final String TAG = "PrerollEvent";
    private static final String METRIC_TTS_SOURCE_PREPARE_FAILED = TAG + ".ttsSourcePrepareFailed";

    @NonNull
    private final WeakReference<ITtsPlayerProvider> mTTSPlayerProvider;

    /**
     * Constructs the Event.
     *
     * @param nativeHandle      Handle to the native event.
     * @param rootContext       The root context for the event.
     * @param ttsPlayerProvider Provides tts player access.
     */
    private PrerollEvent(long nativeHandle, RootContext rootContext,
                         ITtsPlayerProvider ttsPlayerProvider) {
        super(nativeHandle, rootContext);
        mTTSPlayerProvider = new WeakReference<>(ttsPlayerProvider);
    }


    static public PrerollEvent create(long nativeHandle, RootContext rootContext,
                                      ITtsPlayerProvider ttsPlayerProvider) {
        return new PrerollEvent(nativeHandle, rootContext, ttsPlayerProvider);
    }

    /**
     * Execute the command.
     */
    @Override
    public void execute() {
        final ITtsPlayerProvider provider = mTTSPlayerProvider.get();

        if (provider == null) {
            // Cancel the execution if resources aren't available, this will result in terminate
            Log.w(TAG, "Failed Preroll execute");
            mRootContext.cancelExecution();
            return;
        }

        final String source = mProperties.getString(EventProperty.kEventPropertySource);
        try {
            provider.prepare(source, new ITtsSourceProvider() {
                @Override
                public void onSource(InputStream stream) {
                    provider.getPlayer().prepare(source, stream);
                }

                @Override
                public void onSource(URL url) {
                    provider.getPlayer().prepare(source, url);
                }
            });
        } catch (IOException|IllegalStateException e) {
            mRootContext.incrementMetricCount(METRIC_TTS_SOURCE_PREPARE_FAILED);
            Log.e(TAG, "Error while preparing source for TTS player", e);
        }
    }

    /**
     * Terminate the event process.
     */
    @Override
    public void terminate() {
        final ITtsPlayerProvider provider = mTTSPlayerProvider.get();
        if (provider != null) {
            provider.getPlayer().stop();
            mTTSPlayerProvider.clear();
        }
    }
}
