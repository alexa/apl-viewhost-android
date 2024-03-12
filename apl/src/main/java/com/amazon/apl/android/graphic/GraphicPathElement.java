/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.android.graphic;

import android.graphics.DashPathEffect;
import android.graphics.Paint;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.Rect;
import android.util.Log;

import com.amazon.apl.android.PropertyMap;
import com.amazon.apl.android.RenderingContext;
import com.amazon.apl.enums.GraphicLineCap;
import com.amazon.apl.enums.GraphicLineJoin;
import com.amazon.apl.enums.GraphicPropertyKey;
import com.amazon.common.storage.WeakCache;

import static com.amazon.apl.enums.GraphicPropertyKey.kGraphicPropertyFill;
import static com.amazon.apl.enums.GraphicPropertyKey.kGraphicPropertyPathData;
import static com.amazon.apl.enums.GraphicPropertyKey.kGraphicPropertyPathLength;
import static com.amazon.apl.enums.GraphicPropertyKey.kGraphicPropertyStroke;
import static com.amazon.apl.enums.GraphicPropertyKey.kGraphicPropertyStrokeDashArray;
import static com.amazon.apl.enums.GraphicPropertyKey.kGraphicPropertyStrokeDashOffset;
import static com.amazon.apl.enums.GraphicPropertyKey.kGraphicPropertyStrokeLineCap;
import static com.amazon.apl.enums.GraphicPropertyKey.kGraphicPropertyStrokeLineJoin;
import static com.amazon.apl.enums.GraphicPropertyKey.kGraphicPropertyStrokeMiterLimit;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.HashSet;

/**
 * Represents path avg object.
 */
public class GraphicPathElement extends FillStrokeGraphicElement {
    private static final String TAG = "GraphicPathElement";

    private Path mPath;

    @NonNull
    private Paint.Join mPaintJoin = Paint.Join.BEVEL;

    @NonNull
    private Paint.Cap mPaintCap = Paint.Cap.BUTT;

    private GraphicPathElement(GraphicElementMap map, long nativeHandle, RenderingContext renderingContext) {
        super(map, nativeHandle, renderingContext);
        applyProperties();
    }

    static GraphicPathElement create(GraphicElementMap map, long graphicHandle,
                                     RenderingContext renderingContext) {
        return new GraphicPathElement(map, graphicHandle, renderingContext);
    }

    /**
     * The path data of the path element.
     * @return the value of the path data of the path element.
     */
    @Nullable
    String getPathData() {
        return mProperties.getString(kGraphicPropertyPathData);
    }

    /**
     * If defined, specifies the user-defined "length" of the path.
     * @return the length of the path
     */
    float getPathLength() {
        return mProperties.getFloat(kGraphicPropertyPathLength);
    }

    /**
     * The strokeDashArray is an array of numbers that defines the pattern of dashes and gaps used
     * to stroke the path. If the array is empty, the stroke is solid. If the array contains an
     * odd number of elements, it is implicitly doubled. The odd indices in the array are the dash
     * lengths (in viewport drawing units); the even indices in the array are the space lengths
     * (in viewport drawing units).
     * @return the strokeDashArray
     */
    @NonNull
    float[] getStrokeDashArray() {
        return mProperties.getFloatArray(kGraphicPropertyStrokeDashArray);
    }

    /**
     * The strokeDashOffset shifts the starting point of the strokeDashArray by the specified
     * number of viewport drawing units.
     * @return the strokeDashOffset in user-defined units
     */
    float getStrokeDashOffset() {
        return mProperties.getFloat(kGraphicPropertyStrokeDashOffset);
    }

    /**
     * The strokeLineCap property determines the shape to be used at the ends of open paths.
     * @return value corresponding to the strokeLineCap property of the Path
     */
    @NonNull
    GraphicLineCap getStrokeLineCap() {
        return GraphicLineCap.valueOf(mProperties.getEnum(kGraphicPropertyStrokeLineCap));
    }

    /**
     * The strokeLineJoin property determines how sharp corners in a path will be drawn.
     * @return value corresponding to the strokeLineJoin property of the Path
     */
    @NonNull
    GraphicLineJoin getStrokeLineJoin() {
        return GraphicLineJoin.valueOf(mProperties.getEnum(kGraphicPropertyStrokeLineJoin));
    }

    /**
     * The strokeMiterLimit property determines when miter joints in paths should be turned into
     * bevel joints. When the angle exceeds the miter limit, the angle is beveled instead of
     * mitered.
     * @return the miterLimit value in user-defined units
     */
    float getStrokeMiterLimit() {
        return mProperties.getFloat(kGraphicPropertyStrokeMiterLimit);
    }

    @NonNull
    Paint.Cap getPaintCap() {
        return mPaintCap;
    }

    @NonNull
    Paint.Join getPaintJoin() {
        return mPaintJoin;
    }

    @Nullable
    Path getPath() {
        return mPath;
    }

    /**
     * Update cached properties when Graphic is marked dirty.
     */
    @Override
    void applyProperties() {
        applyPath();

        applyPaintCap();

        applyPaintJoin();

        // Convert the set of dirty properties to a set for faster lookups.
        HashSet dirtyProperties = nGetDirtyProperties(this.getNativeHandle());

        Rect graphicBounds = FillStrokeGraphicElement.getBounds(getPath(), getRenderingContext());
        applyStrokePaint(graphicBounds, dirtyProperties.contains(kGraphicPropertyPathData.getIndex()), dirtyProperties);
        applyFillPaint(graphicBounds, dirtyProperties.contains(kGraphicPropertyPathData.getIndex()), dirtyProperties);
    }

    private void applyPaintCap() {
        switch(getStrokeLineCap()) {
            case kGraphicLineCapButt: mPaintCap = Paint.Cap.BUTT; break;
            case kGraphicLineCapSquare: mPaintCap = Paint.Cap.SQUARE; break;
            case kGraphicLineCapRound: mPaintCap = Paint.Cap.ROUND; break;
            default: {
                Log.w(TAG, "Unknown StrokeLineCap value " + getStrokeLineCap());
                mPaintCap = Paint.Cap.BUTT;
            }
        }
    }

    private void applyPaintJoin() {
        switch (getStrokeLineJoin()) {
            case kGraphicLineJoinBevel: mPaintJoin = Paint.Join.BEVEL; break;
            case kGraphicLineJoinMiter: mPaintJoin = Paint.Join.MITER; break;
            case kGraphicLineJoinRound: mPaintJoin = Paint.Join.ROUND; break;
            default: {
                Log.w(TAG, "Unknown StrokeLineJoin value " + getStrokeLineJoin());
                mPaintJoin = Paint.Join.BEVEL;
            }
        }
    }

    private void applyStrokePaint(Rect graphicBounds, Boolean boundsChanged, HashSet dirtyProperties) {
        applyStrokePaintProperties(graphicBounds, boundsChanged, dirtyProperties);
        mStrokePaint.setStrokeJoin(getPaintJoin());
        mStrokePaint.setStrokeCap(getPaintCap());
        mStrokePaint.setStrokeMiter(getStrokeMiterLimit());
        mStrokePaint.setStrokeWidth(getStrokeWidth());

        float[] strokeDashArray = getStrokeDashArray();
        if (strokeDashArray.length > 0) {
            DashPathEffect dashPathEffect = PathRenderer.createDashPathEffect(getPath(), this, 1.0f);
            mStrokePaint.setPathEffect(dashPathEffect);
        } else {
            mStrokePaint.setPathEffect(null);
        }
    }

    private void applyFillPaint(Rect graphicBounds, Boolean boundsChanged, HashSet dirtyProperties) {
        applyFillPaintProperties(graphicBounds, boundsChanged, dirtyProperties);
    }



    private void applyPath() {
        String pathData = getPathData();
        mPath = getRenderingContext().getPathCache().get(pathData);

        if (mPath == null) {
            mPath = new Path();
            PathParser.PathDataNode[] nodes = PathParser.createNodesFromPathData(pathData, getRenderingContext());
            PathParser.toPath(nodes, mPath);

            getRenderingContext().getPathCache().put(pathData, new WeakReference<>(mPath));
        }
    }
}
