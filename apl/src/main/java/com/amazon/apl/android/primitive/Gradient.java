/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.android.primitive;

import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amazon.common.BoundObject;
import com.amazon.apl.android.utils.ColorUtils;
import com.amazon.apl.enums.APLEnum;
import com.amazon.apl.enums.GradientProperty;
import com.amazon.apl.enums.GradientSpreadMethod;
import com.amazon.apl.enums.GradientType;
import com.amazon.apl.enums.GradientUnits;
import com.google.auto.value.AutoValue;


/**
 * Gradient property.
 * This also contains getters for additional AVG Gradient properties
 * APL Spec for AVG Gradient: https://developer.amazon.com/en-US/docs/alexa/alexa-presentation-language/apl-avg-format.html#avg-gradients
 * Common properties between component gradient and AVG gradient: type, colorRange, inputRange
 * Specific properties for AVG Linear Gradient:
 *      1. spreadMethod: Gradient behavior outside of the defined range (x1, y1) to (x2, y2)
 *      2. x1/x2/y1/y2: Starting and ending point of the linear gradient
 * Specific properties for AVG Radial Gradient:
 *      1. centerX, centerY: Coordinates of the center of the radial gradient.
 *      2. radius: The radius of the gradient.
 */
@AutoValue
public abstract class Gradient {
    /**
     * @return The type of the gradient linear | radial.
     */
    @NonNull
    public abstract GradientType getType();

    /**
     * @return Angle of a linear gradient, in degrees. 0 is up, 90 is to the right.
     * Defaults to 0.
     */
    public abstract float getAngle();

    /**
     * @return The input stops of the gradient. Must be in ascending order with values
     * between 0 and 1.
     */
    @NonNull
    @SuppressWarnings("mutable")
    public abstract float[] getInputRange();

    /**
     * @return The color to assign at each gradient stop.
     */
    @NonNull
    @SuppressWarnings("mutable")
    public abstract int[] getColorRange();

    public abstract float getX1();

    public abstract float getY1();

    public abstract float getX2();

    public abstract float getY2();

    public abstract float getCenterX();

    public abstract float getCenterY();

    public abstract float getRadius();

    @Nullable
    public abstract GradientSpreadMethod getSpreadMethod();

    @Nullable
    public abstract GradientUnits getUnits();

    public static Builder builder() {
        return new AutoValue_Gradient.Builder()
                .x1(-1)
                .y1(-1)
                .x2(-1)
                .y2(-1)
                .centerX(-1)
                .centerY(-1)
                .radius(-1);
    }

    public static Gradient create(BoundObject boundObject, APLEnum propertyKey) {
        GradientType type = GradientType.valueOf(getInt(boundObject, propertyKey, GradientProperty.kGradientPropertyType));
        Builder builder = builder()
                .type(type)
                .angle(getFloat(boundObject, propertyKey, GradientProperty.kGradientPropertyAngle))
                .spreadMethod(GradientSpreadMethod.valueOf(getInt(boundObject, propertyKey, GradientProperty.kGradientPropertySpreadMethod)))
                .inputRange(getFloatArray(boundObject, propertyKey, GradientProperty.kGradientPropertyInputRange))
                .colorRange(convertColorRange(getColorArray(boundObject, propertyKey, GradientProperty.kGradientPropertyColorRange)))
                .units(GradientUnits.valueOf(getInt(boundObject, propertyKey, GradientProperty.kGradientPropertyUnits)));
        switch (type) {
            case LINEAR:
                builder.x1(getFloat(boundObject, propertyKey, GradientProperty.kGradientPropertyX1))
                        .y1(getFloat(boundObject, propertyKey, GradientProperty.kGradientPropertyY1))
                        .x2(getFloat(boundObject, propertyKey, GradientProperty.kGradientPropertyX2))
                        .y2(getFloat(boundObject, propertyKey, GradientProperty.kGradientPropertyY2));
                break;
            case RADIAL:
                builder.centerX(getFloat(boundObject, propertyKey, GradientProperty.kGradientPropertyCenterX))
                        .centerY(getFloat(boundObject, propertyKey, GradientProperty.kGradientPropertyCenterY))
                        .radius(getFloat(boundObject, propertyKey, GradientProperty.kGradientPropertyRadius));
                break;
        }
        return builder.build();
    }

    public static int[] convertColorRange(long[] colorRange) {
        int[] mColorRange = new int[colorRange.length];
        for (int i = 0; i < colorRange.length; i++) {
            mColorRange[i] = ColorUtils.toARGB(colorRange[i]);
        }
        return mColorRange;
    }

    public Shader getShader(int width, int height) {
        return createGradientShader(getColorRange(), getInputRange(), getType(), getAngle(), height, width);
    }

    public static Shader createGradientShader(int[] colors, float[] positions, GradientType type, float angle, int height, int width) {
        Shader shader = null;
        switch (type) {
            case LINEAR:
                // rootContextCreate a gradient shader where up is zero degrees.
                // In order to make the gradient area cover height x width space, we need to get a
                // larger or equal size of it based on angle.
                final double radAngle = Math.toRadians(angle);
                final float projectionHeight = (float) (width * Math.abs(Math.sin(radAngle)) + height * Math.abs(Math.cos(radAngle)));
                shader = new LinearGradient(0, height + (projectionHeight - height) / 2,
                        0, 0 - (projectionHeight - height) / 2, colors, positions, Shader.TileMode.CLAMP);

                Matrix matrix = new Matrix();
                matrix.setRotate(angle, width/2, height/2);
                shader.setLocalMatrix(matrix);
                break;
            case RADIAL:
                // Radius is determined by the distance from the center to corner.
                // It follows the default ending shape implementation from CSS (farthest-corner)
                // https://developer.mozilla.org/en-US/docs/Web/CSS/radial-gradient
                float radius = (float) Math.sqrt((width * width) + (height * height)) / 2f;
                shader = new RadialGradient(width / 2f, height / 2f, radius, colors, positions, Shader.TileMode.CLAMP);
                break;
        }
        return shader;
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder type(GradientType type);
        public abstract Builder units(GradientUnits units);
        public abstract Builder spreadMethod(GradientSpreadMethod spreadMethod);
        public abstract Builder angle(float angle);
        public abstract Builder inputRange(float[] inputRange);
        public abstract Builder colorRange(int[] colorRange);
        public abstract Builder x1(float x1);
        public abstract Builder y1(float y1);
        public abstract Builder x2(float x2);
        public abstract Builder y2(float y2);
        public abstract Builder centerX(float centerX);
        public abstract Builder centerY(float centerY);
        public abstract Builder radius(float radius);
        public abstract Gradient build();
    }

    private static float[] getFloatArray(BoundObject boundObject, APLEnum propertyKey, GradientProperty property) {
        return nGetFloatArray(boundObject.getNativeHandle(), propertyKey.getIndex(), property.getIndex());
    }

    private static long[] getColorArray(BoundObject boundObject, APLEnum propertyKey, GradientProperty property) {
        return nGetColorArray(boundObject.getNativeHandle(), propertyKey.getIndex(), property.getIndex());
    }

    private static int getInt(BoundObject boundObject, APLEnum propertyKey, GradientProperty property) {
        return nGetInt(boundObject.getNativeHandle(), propertyKey.getIndex(), property.getIndex());
    }

    private static float getFloat(BoundObject boundObject, APLEnum propertyKey, GradientProperty property) {
        return nGetFloat(boundObject.getNativeHandle(), propertyKey.getIndex(), property.getIndex());
    }

    private static native float[] nGetFloatArray(long componentHandle, int propertyKey, int gradientPropertyKey);
    private static native long[] nGetColorArray(long componentHandle, int propertyKey, int gradientPropertyKey);
    private static native float nGetFloat(long componentHandle, int propertyKey, int gradientPropertyKey);
    private static native int nGetInt(long componentHandle, int propertyKey, int gradientPropertyKey);
}