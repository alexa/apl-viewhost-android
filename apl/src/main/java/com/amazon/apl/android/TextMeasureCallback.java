/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.android;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.amazon.apl.android.scaling.IMetricsTransform;
import com.amazon.apl.android.utils.JNIUtils;
import com.amazon.apl.enums.ComponentType;
import com.amazon.common.BoundObject;

/**
 * Text Measurement Callback.  This object is called from core via JNI.
 * <p>
 * The sizing of text based Components is dependent on view host resources, such as font.  For
 * this reason APL Core defers size calculation of text based components to the view host.  This callback
 * is configured by {@link RootContext} and used any time core needs to (re)calculate a text component
 * size.
 * <p>
 * The callback retrieves the component under measure from the native peer and delegates to a
 * {@link TextMeasure} instance.
 * <p>
 * The native peer maintains a reference to the target component under measure through the measurement
 * process and releases the reference upon completion.  As core iterates over the required
 * measurements, the target component is updated.  This object accesses the component via proxy
 * during the measurement effort. To maintain proper binding with the native peer, this callback
 * should not be used across document.
 */
public class TextMeasureCallback extends BoundObject {

    private static final String TAG = "TextMeasureCB";

    // delegate for measurement processing
    private TextMeasure mDelegate;
    private IMetricsTransform mMetricsTransform;


    /**
     * @return a factory for TextMeasureCallback creation.
     */
    public static Factory factory() {
        // return an injected instance of the factory
        // typically for testing
        if (Factory.sInstance != null)
            return Factory.sInstance;
        // otherwise treat this as a factory method without preserved instance
        return new Factory();
    }

    /**
     * Encapsulate factory methods.
     */
    public static class Factory {
        private static Factory sInstance;

        /**
         * @param factory Injected factory, typically for testing.
         */
        @VisibleForTesting
        public static void inject(Factory factory) {
            sInstance = factory;
        }

        /**
         * Create and Bind to the native callback. Because this is a callback, the instance
         * construction should be complete before binding;
         *
         * @param metricsTransform Used for px-dp conversions.
         * @param textMeasure      The Measurement delegate
         * @return A bound text measurement callback.
         */
        public TextMeasureCallback create(@NonNull IMetricsTransform metricsTransform,
                                          @NonNull TextMeasure textMeasure) {


            TextMeasureCallback callback = new TextMeasureCallback(textMeasure, metricsTransform);
            long handle = callback.nCreate();

            /*
            Exposing handle validity testing logic as a utility
            method for testability.
            */
            if (!JNIUtils.isHandleValid(handle))
                return null;
            callback.bind(handle);

            return callback;
        }

        /**
         * Create and Re-Bind to an existing native callback. Because this is a callback, the instance
         * construction should be complete before binding;
         *
         * @param rootConfig       Configuration used in re-binding.
         * @param metricsTransform Used for px-dp conversions.
         * @param textMeasure      The measurement delegate;
         * @return A bound text measurement callback.
         */
        public TextMeasureCallback create(@NonNull RootConfig rootConfig,
                                          @NonNull IMetricsTransform metricsTransform,
                                          @NonNull TextMeasure textMeasure) {

            TextMeasureCallback callback = new TextMeasureCallback(textMeasure, metricsTransform);
            long handle = callback.nCreateHandle(rootConfig.getNativeHandle());

            /*
            Exposing handle validity testing logic as a utility
            method for testability.
            */
            if (!JNIUtils.isHandleValid(handle))
                return null;
            callback.bind(handle);

            return callback;
        }
    }


    private TextMeasureCallback(TextMeasure measure, IMetricsTransform metricsTransform) {
        mMetricsTransform = metricsTransform;
        delegate(measure);
    }

    /**
     * Update the metrics transform for this measure callback.
     * @param transform the metrics transform.
     */
    public void setMetricsTransform(IMetricsTransform transform) {
        mMetricsTransform = transform;
    }

    public void onRootContextCreated() {
        mDelegate.onRootContextCreated();
    }

    /**
     * Called by the JNI layer when a component needs to be measured.
     */
    @SuppressWarnings("unused")
    @VisibleForTesting
    public float[] callbackMeasure(String textHash, int componentType,
                                   float widthDp, int widthMode,
                                   float heightDp, int heightMode) {
        return mDelegate.measure(textHash, ComponentType.valueOf(componentType),
                widthDp, TextMeasure.MEASURE_MODES[widthMode],
                heightDp, TextMeasure.MEASURE_MODES[heightMode]);
    }

    @VisibleForTesting
    public void delegate(TextMeasure textMeasure) {
        // allows for injection of spy/mock test object
        mDelegate = textMeasure;

        final TextProxy<TextMeasureCallback> textProxy = new TextProxy<TextMeasureCallback>() {
            @NonNull
            @Override
            public TextMeasureCallback getMapOwner() {
                return TextMeasureCallback.this;
            }

            @Nullable
            @Override
            protected IMetricsTransform getMetricsTransform() {
                return TextMeasureCallback.this.mMetricsTransform;
            }
        };

        final EditTextProxy<TextMeasureCallback> editTextProxy = new EditTextProxy<TextMeasureCallback>() {
            @NonNull
            @Override
            public TextMeasureCallback getMapOwner() {
                return TextMeasureCallback.this;
            }

            @Nullable
            @Override
            protected IMetricsTransform getMetricsTransform() {
                return TextMeasureCallback.this.mMetricsTransform;
            }
        };
        mDelegate.prepare(textProxy, editTextProxy);
    }

    @VisibleForTesting
    public TextMeasure getDelegate() {
        return mDelegate;
    }

    @VisibleForTesting
    public long getNativeAddress() {
        // returns the address of the native callback
        return nGetNativeAddress(getNativeHandle());
    }

    /* Native object create and binding */
    private native long nCreate();

    /* Native object binding to existing */
    private native long nCreateHandle(long rootConfigHandle);

    static private native long nGetNativeAddress(long nativeHandle);
}