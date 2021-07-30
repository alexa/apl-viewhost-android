/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.android.graphic;

import android.graphics.Paint;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Log;

import com.amazon.apl.android.PropertyMap;
import com.amazon.apl.android.RenderingContext;
import com.amazon.apl.enums.GraphicLineCap;
import com.amazon.apl.enums.GraphicLineJoin;

import static com.amazon.apl.enums.GraphicPropertyKey.kGraphicPropertyFill;
import static com.amazon.apl.enums.GraphicPropertyKey.kGraphicPropertyPathData;
import static com.amazon.apl.enums.GraphicPropertyKey.kGraphicPropertyPathLength;
import static com.amazon.apl.enums.GraphicPropertyKey.kGraphicPropertyStroke;
import static com.amazon.apl.enums.GraphicPropertyKey.kGraphicPropertyStrokeDashArray;
import static com.amazon.apl.enums.GraphicPropertyKey.kGraphicPropertyStrokeDashOffset;
import static com.amazon.apl.enums.GraphicPropertyKey.kGraphicPropertyStrokeLineCap;
import static com.amazon.apl.enums.GraphicPropertyKey.kGraphicPropertyStrokeLineJoin;
import static com.amazon.apl.enums.GraphicPropertyKey.kGraphicPropertyStrokeMiterLimit;

/**
 * Represents path avg object.
 */
public class GraphicPathElement extends GraphicElement implements RenderableGraphicElement {
    private static final String TAG = "GraphicPathElement";

    @NonNull
    private Paint mStrokePaint;

    @NonNull
    private Paint mFillPaint;

    @Nullable
    private PathParser.PathDataNode[] mNodes = null;

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

    @Override
    public PropertyMap getProperties() {
        return mProperties;
    }

    @Override
    public GraphicPattern getFillGraphicPattern() {
        return getGraphicPattern(kGraphicPropertyFill);
    }

    /**
     * @return a defensive copy of the fill paint for this path element.
     * The caller of this method can do anything they want with the
     * returned Paint object, without affecting the internals of this
     * class in any way.
     */
    @Override
    public Paint getFillPaint() {
        // copy of fill paint (defensive mechanism for repeated draw calls)
        return new Paint(mFillPaint);
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


    @Override
    public GraphicPattern getStrokeGraphicPattern() {
        return getGraphicPattern(kGraphicPropertyStroke);
    }

    /**
     * @return a defensive copy of the stroke paint for this path element.
     * The caller of this method can do anything they want with the
     * returned Paint object, without affecting the internals of this
     * class in any way.
     */
    @Override
    public Paint getStrokePaint() {
        // copy of stroke paint (defensive mechanism for repeated draw calls)
        return new Paint(mStrokePaint);
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
    PathParser.PathDataNode[] getPathNodes() {
        return mNodes;
    }

    /**
     * Update cached properties when Graphic is marked dirty.
     */
    @Override
    void applyProperties() {
        applyPath();

        applyPaintCap();

        applyPaintJoin();

        applyStrokePaint();

        applyFillPaint();
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

    private void applyStrokePaint() {
        mStrokePaint = RenderableGraphicElement.super.getStrokePaint();
        mStrokePaint.setStrokeJoin(getPaintJoin());
        mStrokePaint.setStrokeCap(getPaintCap());
        mStrokePaint.setStrokeMiter(getStrokeMiterLimit());
    }

    private void applyFillPaint() {
        mFillPaint = RenderableGraphicElement.super.getFillPaint();
    }

    private void applyPath() {
        mNodes = PathParser.createNodesFromPathData(getPathData(), getRenderingContext());
    }
}
