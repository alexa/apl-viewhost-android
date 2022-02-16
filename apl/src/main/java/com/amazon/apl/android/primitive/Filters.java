/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.primitive;

import android.graphics.Color;
import androidx.annotation.Nullable;

import com.amazon.common.BoundObject;
import com.amazon.apl.android.image.filters.BlurFilterOperation;
import com.amazon.apl.android.scaling.IMetricsTransform;
import com.amazon.apl.android.utils.ColorUtils;
import com.amazon.apl.enums.APLEnum;
import com.amazon.apl.enums.BlendMode;
import com.amazon.apl.enums.FilterProperty;
import com.amazon.apl.enums.FilterType;
import com.amazon.apl.enums.GradientProperty;
import com.amazon.apl.enums.GradientType;
import com.amazon.apl.enums.NoiseFilterKind;
import com.google.auto.value.AutoValue;

import java.util.ArrayList;
import java.util.Map;

/**
 * APL Filters data type.
 * See {@link <a https://developer.amazon.com/en-US/docs/alexa/alexa-presentation-language/apl-filters.html>
 * APL Filters Specification </a>}
 */
@AutoValue
public abstract class Filters implements IterableProperty<Filters.Filter> {
    public static Filters create(BoundObject boundObject, APLEnum propertyKey, IMetricsTransform transform) {
        return IterableProperty.create(new FilterGetter(boundObject, propertyKey, transform));
    }

    public static Filters create() {
        return new AutoValue_Filters(new ArrayList<>());
    }

    @AutoValue
    public static abstract class Filter {
        public abstract FilterType filterType();

        /**
         * Blend properties
         **/
        @Nullable
        public abstract BlendMode blendMode();

        @Nullable
        public abstract Integer source();

        @Nullable
        public abstract Integer destination();

        /**
         * Blur properties
         **/
        public abstract float radius();

        /**
         * Color properties
         **/
        public abstract int color();

        /**
         * Noise properties
         **/
        @Nullable
        public abstract NoiseFilterKind noiseKind();
        public abstract boolean noiseUseColor();
        public abstract float noiseSigma();
        public abstract float amount();

        /** Gradient Properties **/
        @Nullable
        public abstract Gradient gradient();

        /**
         * Extension Properties
         **/
        @Nullable
        public abstract String name();

        @Nullable
        public abstract String extensionURI();

        @Nullable
        public abstract Map<String, Object> extensionParams();

        public static Builder builder() {
            return new AutoValue_Filters_Filter.Builder()
                    .radius(-1)
                    .color(-1)
                    .noiseUseColor(false)
                    .noiseSigma(-1)
                    .amount(-1);
        }

        @AutoValue.Builder
        public static abstract class Builder {
            public abstract Builder filterType(FilterType filterType);
            public abstract Builder noiseKind(NoiseFilterKind noiseFilterKind);
            public abstract Builder blendMode(BlendMode mode);
            public abstract Builder destination(Integer destination);
            public abstract Builder source(Integer source);
            public abstract Builder radius(float radius);
            public abstract Builder noiseUseColor(boolean noiseUseColor);
            public abstract Builder noiseSigma(float noiseSigma);
            public abstract Builder amount(float amount);
            public abstract Builder color(int color);
            public abstract Builder gradient(Gradient gradient);
            public abstract Builder name(String string);
            public abstract Builder extensionURI(String extensionURI);
            public abstract Builder extensionParams(Map<String, Object> params);
            public abstract Filter build();
        }
    }

    private static class FilterGetter extends ArrayGetter<Filters, Filter> {
        private final IMetricsTransform mTransform;

        private FilterGetter(BoundObject boundObject, APLEnum propertyKey, IMetricsTransform transform) {
            super(boundObject, propertyKey);
            mTransform = transform;
        }

        @Override
        Filters builder() {
            return new AutoValue_Filters(new ArrayList<>(size()));
        }

        @Override
        public Filter get(int index) {
            final FilterType filterType = FilterType.valueOf(nGetFilterTypeAt(getNativeHandle(), getIndex(), index));
            final Filter.Builder filterBuilder = Filter.builder().filterType(filterType);
            switch (filterType) {
                case kFilterTypeBlend:
                    filterBuilder
                            .source(getIntAt(FilterProperty.kFilterPropertySource, index))
                            .destination(getIntAt(FilterProperty.kFilterPropertyDestination, index))
                            .blendMode(BlendMode.valueOf(getIntAt(FilterProperty.kFilterPropertyMode, index)));
                    break;
                case kFilterTypeBlur:
                    final float radius = mTransform.toViewhost(getFloatAt(FilterProperty.kFilterPropertyRadius, index));
                    final float clampedRadius = Math.min(BlurFilterOperation.MAX_RADIUS, Math.max(BlurFilterOperation.MIN_RADIUS, radius));
                    filterBuilder
                            .source(getIntAt(FilterProperty.kFilterPropertySource, index))
                            .radius(clampedRadius);
                    break;
                case kFilterTypeColor:
                    filterBuilder
                            .color(ColorUtils.toARGB(getColorAt(index)));
                    break;
                case kFilterTypeNoise:
                    filterBuilder
                            .source(getIntAt(FilterProperty.kFilterPropertySource, index))
                            .noiseKind(NoiseFilterKind.valueOf(getIntAt(FilterProperty.kFilterPropertyKind, index)))
                            .noiseUseColor(getBooleanAt(FilterProperty.kFilterPropertyUseColor, index))
                            .noiseSigma(getFloatAt(FilterProperty.kFilterPropertySigma, index));
                    break;
                case kFilterTypeGradient:
                    final Gradient gradient;
                    if (nHasPropertyAt(getNativeHandle(), getIndex(), FilterProperty.kFilterPropertyGradient.getIndex(), index)) {
                        gradient = Gradient.builder()
                                .type(GradientType.valueOf(nGetGradientTypeAt(getNativeHandle(), getIndex(), index)))
                                .angle(nGetGradientFloatAt(getNativeHandle(), getIndex(), GradientProperty.kGradientPropertyAngle.getIndex(), index))
                                .inputRange(nGetGradientInputRangeAt(getNativeHandle(), getIndex(), index))
                                .colorRange(Gradient.convertColorRange(nGetGradientColorRangeAt(getNativeHandle(), getIndex(), index)))
                                .build();
                    } else {
                        // As per spec, if a gradient is not specified, the gradient is replaced with a transparent black color.
                        gradient = Gradient.builder()
                                .type(GradientType.LINEAR)
                                .angle(90f)
                                .inputRange(new float[]{0f, 1f})
                                .colorRange(new int[]{Color.TRANSPARENT, Color.TRANSPARENT})
                                .build();
                    }
                    filterBuilder.gradient(gradient);
                    break;
                case kFilterTypeSaturate:
                case kFilterTypeGrayscale:
                    filterBuilder
                            .source(getIntAt(FilterProperty.kFilterPropertySource, index))
                            .amount(getFloatAt(FilterProperty.kFilterPropertyAmount, index));
                    break;
                case kFilterTypeExtension:
                    filterBuilder
                            .extensionURI(getStringAt(FilterProperty.kFilterPropertyExtensionURI, index))
                            .name(getStringAt(FilterProperty.kFilterPropertyName, index))
                            // source and destination may be null for extension
                            .source(getObjectAt(FilterProperty.kFilterPropertySource, index))
                            .destination(getObjectAt(FilterProperty.kFilterPropertyDestination, index))
                            .extensionParams(getMapAt(FilterProperty.kFilterPropertyExtension, index));
                    break;
            }
            return filterBuilder.build();
        }

        // TODO it would be nice to pull these into a reusable interface for complex properties.
        //  In order to do this we'd need to have some type checking and recursive calls since
        //  APLObjects can be recursively defined:
        //     Filter -> Map<kFilterProperty, APLObject>
        private float getFloatAt(FilterProperty property, int index) {
            return nGetFloatAt(getNativeHandle(), getIndex(), property.getIndex(), index);
        }

        private int getIntAt(FilterProperty property, int index) {
            return nGetIntAt(getNativeHandle(), getIndex(), property.getIndex(), index);
        }

        private long getColorAt(int index) {
            return nGetColorAt(getNativeHandle(), getIndex(), index);
        }

        private boolean getBooleanAt(FilterProperty property, int index) {
            return nGetBooleanAt(getNativeHandle(), getIndex(), property.getIndex(), index);
        }

        private String getStringAt(FilterProperty property, int index) {
            return nGetStringAt(getNativeHandle(), getIndex(), property.getIndex(), index);
        }

        private Map<String, Object> getMapAt(FilterProperty property, int index) {
            return nGetMapAt(getNativeHandle(), getIndex(), property.getIndex(), index);
        }

        private <T> T getObjectAt(FilterProperty property, int index) {
            //noinspection unchecked
            return (T) nGetObjectAt(getNativeHandle(), getIndex(), property.getIndex(), index);
        }
    }

    private static native int nGetFilterTypeAt(long componentHandle, int propertyKey, int index);
    private static native boolean nGetBooleanAt(long componentHandle, int propertyKey, int filterPropertyKey, int index);
    private static native float nGetFloatAt(long componentHandle, int propertyKey, int filterPropertyKey, int index);
    private static native int nGetIntAt(long componentHandle, int propertyKey, int filterPropertyKey, int index);
    private static native long nGetColorAt(long componentHandle, int propertyKey, int index);
    private static native String nGetStringAt(long componentHandle, int propertyKey, int filterPropertyKey, int index);
    private static native Map<String, Object> nGetMapAt(long componentHandle, int propertyKey, int filterPropertyKey, int index);
    private static native Object nGetObjectAt(long componentHandle, int propertyKey, int filterPropertyKey, int index);

    /**
     * Gradient Properties for kFilterTypeGradient
     **/
    private static native boolean nHasPropertyAt(long componentHandle, int propertyKey, int filterPropertyKey, int index);
    private static native int nGetGradientTypeAt(long componentHandle, int propertyKey, int index);
    private static native float nGetGradientFloatAt(long componentHandle, int propertyKey, int gradientPropertyKey, int index);
    private static native float[] nGetGradientInputRangeAt(long componentHandle, int propertyKey, int index);
    private static native long[] nGetGradientColorRangeAt(long componentHandle, int propertyKey, int index);

}