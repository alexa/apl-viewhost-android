/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.android.graphic;

import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.Shader;
import android.util.Log;

import com.amazon.apl.android.APLVersionCodes;
import com.amazon.apl.android.PropertyMap;
import com.amazon.apl.android.RenderingContext;
import com.amazon.apl.android.primitive.Gradient;
import com.amazon.apl.enums.GraphicPropertyKey;

import static com.amazon.apl.enums.GraphicPropertyKey.kGraphicPropertyFill;
import static com.amazon.apl.enums.GraphicPropertyKey.kGraphicPropertyFillOpacity;
import static com.amazon.apl.enums.GraphicPropertyKey.kGraphicPropertyFillTransform;
import static com.amazon.apl.enums.GraphicPropertyKey.kGraphicPropertyStroke;
import static com.amazon.apl.enums.GraphicPropertyKey.kGraphicPropertyStrokeOpacity;
import static com.amazon.apl.enums.GraphicPropertyKey.kGraphicPropertyStrokeTransform;
import static com.amazon.apl.enums.GraphicPropertyKey.kGraphicPropertyStrokeWidth;

import androidx.annotation.NonNull;

import java.util.HashSet;

/**
 * Common interface for graphic elements which can be stroked/filled.
 */
abstract class FillStrokeGraphicElement extends GraphicElement {

    // As per official android guidelines, Paints should be reused.
    final protected Paint mStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    final protected Paint mFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private int mOriginalStrokePaintAlpha;
    private int mOriginalFillPaintAlpha;

    private boolean mFirstRender = true;

    protected FillStrokeGraphicElement(@NonNull GraphicElementMap map, long nativeHandle, RenderingContext renderingContext) {
        super(map, nativeHandle, renderingContext);
        applyProperties();

        mFirstRender = false;
    }

    /**
     * The properties for the RenderableGraphicElement.
     */
    public PropertyMap<GraphicElement, GraphicPropertyKey> getProperties() {
        return mProperties;
    }

    /**
     * The fill pattern. Only applicable when the fill type is Pattern.
     */
    public GraphicPattern getFillGraphicPattern() {
        return getGraphicPattern(kGraphicPropertyFill);
    }

    /**
     * The stroke pattern. Only applicable when the stroke type is Pattern.
     */
    public GraphicPattern getStrokeGraphicPattern() {
        return getGraphicPattern(kGraphicPropertyStroke);
    }

    /**
     * The opacity of the element's fill.
     */
    float getFillOpacity() {
        return getProperties().getFloat(kGraphicPropertyFillOpacity);
    }

    /**
     * Transformation applied against the fill. Only applicable if fill type is Gradient or Pattern.
     */
    Matrix getFillTransform() {
        return getProperties().getTransform(kGraphicPropertyFillTransform);
    }

    /**
     * The color of the element's fill. Only applicable when the fill type is Color.
     */
    int getFillColor() {
        return getProperties().getColor(kGraphicPropertyFill);
    }

    /**
     * The opacity of the element's stroke.
     */
    float getStrokeOpacity() {
        return getProperties().getFloat(kGraphicPropertyStrokeOpacity);
    }

    /**
     * Transformation applied against the stroke. Only applicable if stroke type is Gradient or
     * Pattern.
     */
    Matrix getStrokeTransform() {
        return getProperties().getTransform(kGraphicPropertyStrokeTransform);
    }

    /**
     * The color of the element's stroke. Only applicable when the stroke type is Color.
     */
    int getStrokeColor() {
        return getProperties().getColor(kGraphicPropertyStroke);
    }

    /**
     * The width of the stroke.
     */
    float getStrokeWidth() {
        return getProperties().getFloat(kGraphicPropertyStrokeWidth);
    }

    /**
     * @param applyOpacity The opacity value to apply.
     * @return The fill paint with the alpha applied to the original opacity value.
     */
    public Paint getFillPaint(final float applyOpacity) {
        mFillPaint.setAlpha((int)(applyOpacity * mOriginalFillPaintAlpha));
        return mFillPaint;
    }

    /**
     * @return The fill paint.
     */
    public Paint getFillPaint() {
        return mFillPaint;
    }

    /**
     * @param applyOpacity The opacity value to apply.
     * @return The stroke paint with the alpha applied to the original opacity value.
     */
    public Paint getStrokePaint(final float applyOpacity) {
        mStrokePaint.setAlpha((int)(applyOpacity * mOriginalStrokePaintAlpha));
        return mStrokePaint;
    }

    /**
     * @return The stroke paint.
     */
    public Paint getStrokePaint() {
        return mStrokePaint;
    }

    protected void applyFillPaintProperties(Rect bounds, Boolean boundsChanged, HashSet dirtyProperties) {
        Boolean alphaChanged = applyPaintProperties(mFillPaint, Paint.Style.FILL,
                bounds, boundsChanged,
                kGraphicPropertyFill, dirtyProperties.contains(kGraphicPropertyFill.getIndex()),
                kGraphicPropertyFillTransform, dirtyProperties.contains(kGraphicPropertyFillTransform.getIndex()),
                kGraphicPropertyFillOpacity, dirtyProperties.contains(kGraphicPropertyFillOpacity.getIndex()));

        if (alphaChanged) {
            mOriginalFillPaintAlpha = mFillPaint.getAlpha();
        }
    }

    protected void applyStrokePaintProperties(Rect bounds, Boolean boundsChanged, HashSet dirtyProperties) {
        Boolean alphaChanged = applyPaintProperties(mStrokePaint, Paint.Style.STROKE,
                bounds, boundsChanged,
                kGraphicPropertyStroke, dirtyProperties.contains(kGraphicPropertyStroke.getIndex()),
                kGraphicPropertyStrokeTransform, dirtyProperties.contains(kGraphicPropertyStrokeTransform.getIndex()),
                kGraphicPropertyStrokeOpacity, dirtyProperties.contains(kGraphicPropertyStrokeOpacity.getIndex()));

        mStrokePaint.setStrokeWidth(getStrokeWidth());

        if (alphaChanged) {
            mOriginalStrokePaintAlpha = mStrokePaint.getAlpha();
        }
    }

    private Boolean applyPaintProperties(Paint paint, Paint.Style paintStyle,
                                      Rect bounds, Boolean boundsChanged,
                                      GraphicPropertyKey shaderKey, Boolean shaderDirty,
                                      GraphicPropertyKey transformKey, Boolean transformDirty,
                                      GraphicPropertyKey opacityKey, Boolean opacityDirty) {
        Boolean isPattern = getProperties().isGraphicPattern(shaderKey);
        Boolean isColor = !isPattern ? getProperties().isColor(shaderKey) : false;

        // The shader (or color) changing means we need to recreate.
        Boolean setShader = shaderDirty || boundsChanged || mFirstRender;

        //The opacity can change on it's own, but we also need to reset it when the shader changes.
        Boolean setOpacity = opacityDirty || setShader || mFirstRender;

        // This handles the case where *only* the transform was dirty since setting the shader
        // also sets the transform.
        Boolean setTransform = transformDirty && !setShader;

        paint.setStyle(paintStyle);

        Boolean alphaChanged  = false;

        //Shaders
        if (setShader) {
            if (isPattern) {
                paint.setShader(PathRenderer.createPattern(getProperties().getTransform(transformKey), getGraphicPattern(shaderKey)));
            } else if (isColor) {
                paint.setColor(applyAlpha(getProperties().getColor(shaderKey), getProperties().getFloat(opacityKey)));
            } else { // Gradient
                paint.setShader(createShader(
                        getGradient(shaderKey),
                        bounds,
                        getProperties().getTransform(transformKey),
                        getRenderingContext()
                ));
            }
        }

        //Opacity
        if (setOpacity) {
            if (isPattern) {
                paint.setAlpha((int) (255 * getProperties().getFloat(opacityKey)));
            } else if (isColor) {
                paint.setColor(applyAlpha(getProperties().getColor(shaderKey), getProperties().getFloat(opacityKey)));
            } else { // Gradient
                paint.setAlpha((int) (255 * getProperties().getFloat(opacityKey)));
            }

            alphaChanged = true;
        }

        //Transforms
        if (setTransform) {
            Shader shader = paint.getShader();
            if (shader != null) {
                shader.setLocalMatrix(getProperties().getTransform(transformKey));
            }
        }

        return alphaChanged;
    }

    // APL Spec for AVG Gradient: https://developer.amazon.com/en-US/docs/alexa/alexa-presentation-language/apl-avg-format.html#avg-gradients
    private Shader createShader(@NonNull final Gradient graphicGradient,
                            @NonNull final Rect graphicBounds,
                            @NonNull final Matrix transform,
                            @NonNull final RenderingContext renderingContext
    ) {
        return ShaderFactory.getInstance().getShader(graphicGradient, graphicBounds, transform, renderingContext);
    }

    static Rect getBounds(final Path path, final RenderingContext renderingContext) {
        if (renderingContext.getDocVersion() <= APLVersionCodes.APL_1_6) {
            // prior to 1.7 we were clipping the bounds to positive values, this lead to
            // incorrect rendering for gradient shaders on paths with bounds that should
            // actually be negative
            Region clip = new Region(0, 0, 10000, 10000);
            Region region = new Region();
            if(path != null) {
                region.setPath(path, clip);
            }
            return region.getBounds();
        } else {
            // These bounds include control points of bezier curves, so we can't use
            // them directly. Instead we use Region to get the actual shape bounds and
            // use these for the required clip region.
            RectF bounds = new RectF();
            path.computeBounds(bounds, true);

            Rect roundedBounds = new Rect();
            bounds.roundOut(roundedBounds); // round towards outside to be safe

            Region clip = new Region(roundedBounds);

            Region region = new Region();
            region.setPath(path, clip);

            return region.getBounds();
        }
    }

    static int applyAlpha(final int color, final float alpha) {
        int alphaBytes = Color.alpha(color);
        int colorWithAlphaApplied = color & 0x00FFFFFF;
        colorWithAlphaApplied |= ((int) (alphaBytes * alpha)) << 24;
        return colorWithAlphaApplied;
    }
}
