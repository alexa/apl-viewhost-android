/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.image.filters;

import android.graphics.Bitmap;
import android.renderscript.Element;
import android.renderscript.FieldPacker;
import android.renderscript.Matrix4f;
import android.renderscript.ScriptIntrinsicColorMatrix;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.amazon.apl.android.bitmap.BitmapCreationException;
import com.amazon.apl.android.bitmap.IBitmapFactory;
import com.amazon.apl.android.image.filters.bitmap.FilterResult;
import com.amazon.apl.android.image.filters.bitmap.Size;
import com.amazon.apl.android.primitive.Filters;
import com.amazon.apl.enums.FilterType;

import java.util.List;
import java.util.concurrent.Future;

/**
 * Convert the image bitmap to a grayscale image of specified amount.
 */
public class ColorMatrixFilterOperation extends RenderscriptFilterOperation<ScriptIntrinsicColorMatrix> {
    private static final String TAG = "ColorMatrixFilter";
    Size mTargetSize;

    public ColorMatrixFilterOperation(List<Future<FilterResult>> sourceBitmaps, Filters.Filter filter, IBitmapFactory bitmapFactory, RenderScriptWrapper renderScript) {
        this(sourceBitmaps, filter, bitmapFactory, renderScript, null);
    }

    public ColorMatrixFilterOperation(List<Future<FilterResult>> sourceBitmaps, Filters.Filter filter, IBitmapFactory bitmapFactory, RenderScriptWrapper renderScript, Size targetSize) {
        super(sourceBitmaps, filter, bitmapFactory, renderScript);
        mTargetSize = targetSize;
    }

    @Override
    FilterBitmaps createFilterBitmaps() throws BitmapCreationException {
        FilterResult source = getSource();
        if (source == null || !source.isBitmap()) {
            throw new IllegalArgumentException(TAG + ": Source bitmap must be an actual bitmap.");
        }

        Bitmap sourceBitmap = mTargetSize == null ? source.getBitmap() : source.getBitmap(mTargetSize);
        Bitmap destinationBitmap = getBitmapFactory().createBitmap(sourceBitmap.getWidth(), sourceBitmap.getHeight());
        return FilterBitmaps.create(sourceBitmap, destinationBitmap, destinationBitmap);
    }

    @Override
    ScriptIntrinsicColorMatrix getScript(Element element) {
        ScriptIntrinsicColorMatrix scriptIntrinsicColorMatrix = mRenderscriptWrapper.createScript(element, ScriptIntrinsicColorMatrix.class);
        FieldPacker fp = new FieldPacker(16*4);
        fp.addMatrix(getColorMatrix());
        scriptIntrinsicColorMatrix.setVar(0, fp);
        return scriptIntrinsicColorMatrix;
    }

    @Nullable
    @Override
    ScriptActor<ScriptIntrinsicColorMatrix> getScriptActor() {
        return ScriptIntrinsicColorMatrix::forEach;
    }

    @VisibleForTesting
    Matrix4f getColorMatrix() {
        if (getFilter().filterType() == FilterType.kFilterTypeGrayscale) {
            return getGrayscaleColorMatrix();
        }
        return getSaturateColorMatrix();
    }

    /**
     * Calculate the color matrix to be applied to the image to convert to Grayscale of specified amount.
     * The coefficients are used in the {@link ScriptIntrinsicColorMatrix#setGreyscale()} method.
     * As per spec: https://developer.amazon.com/en-US/docs/alexa/alexa-presentation-language/apl-filters.html#grayscale
     * An amount of 0% leaves the input unchanged. In other words, return an Identity matrix.
     * An amount of 100% results in a completely grayscale image.
     * @return - Color matrix
     */
    private Matrix4f getGrayscaleColorMatrix() {
        final float amount = getFilter().amount();
        Matrix4f matrix = new Matrix4f();
        matrix.set(0, 0, 1f - 0.701f * amount);
        matrix.set(1, 0, 0.587f * amount);
        matrix.set(2, 0, 0.114f * amount);
        matrix.set(0, 1, 0.299f * amount);
        matrix.set(1, 1, 1f - 0.413f * amount);
        matrix.set(2, 1, 0.114f * amount);
        matrix.set(0, 2, 0.299f * amount);
        matrix.set(1, 2, 0.587f * amount);
        matrix.set(2, 2, 1f - 0.886f * amount);
        return matrix;
    }

    /**
     * Calculate the color matrix to be applied to the image to saturate color components by pecified amount.
     * The coefficients are used in the {@link ScriptIntrinsicColorMatrix#setGreyscale()} method.
     * As per spec: https://developer.amazon.com/en-US/docs/alexa/alexa-presentation-language/apl-filters.html#saturate
     * An amount of 0% completed unsaturates the image. In other words, it converts the image to Grayscale.
     * An amount of 100% leaves the image the same. In other words, this returns an Identity matrix.
     * Values greater than 100% produce super-saturation.
     * @return - Color matrix
     */
    public Matrix4f getSaturateColorMatrix() {
        final float amount = getFilter().amount();
        Matrix4f matrix = new Matrix4f();
        float coeff = 1f - amount;
        matrix.set(0, 0, 0.701f * amount + 0.299f);
        matrix.set(1, 0, 0.587f * coeff);
        matrix.set(2, 0, 0.114f * coeff);
        matrix.set(0, 1, 0.299f * coeff);
        matrix.set(1, 1, 0.413f * amount + 0.587f);
        matrix.set(2, 1, 0.114f * coeff);
        matrix.set(0, 2, 0.299f * coeff);
        matrix.set(1, 2, 0.587f * coeff);
        matrix.set(2, 2, 0.886f * amount + 0.114f);
        return matrix;
    }
}
