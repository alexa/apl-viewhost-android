/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.graphic;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.Point;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.Shader;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import android.util.Log;

import com.amazon.apl.android.APLVersionCodes;
import com.amazon.apl.android.BuildConfig;
import com.amazon.apl.android.RenderingContext;
import com.amazon.apl.android.bitmap.BitmapCreationException;
import com.amazon.apl.android.bitmap.IBitmapCache;
import com.amazon.apl.android.bitmap.IBitmapFactory;
import com.amazon.apl.android.primitive.Gradient;
import com.amazon.apl.enums.GradientType;
import com.amazon.apl.enums.GradientUnits;
import com.amazon.apl.enums.GraphicPropertyKey;

/**
 * Renderer for an AlexaVectorGraphic. Adapted from {@link android.graphics.drawable.VectorDrawable}.
 */
final class PathRenderer {
    private static final String TAG = "PathRenderer";
    /* Right now the internal data structure is organized as a tree.
     * Each node can be a group node, or a path.
     * A group node can have groups or paths as children, but a path node has
     * no children.
     * One example can be:
     *                 Root Group
     *                /    |     \
     *           Group    Path    Group
     *          /     \             |
     *         Path   Path         Path
     *
     */

    private static final Matrix IDENTITY_MATRIX = new Matrix();
    private static final float SCALE_X_100_PCT = 1f;
    private static final float SCALE_Y_100_PCT = 1f;

    // Variables that only used temporarily inside the draw() call, so there
    // is no need for deep copying.
    @NonNull
    private final Path mPath;
    @NonNull
    private final Path mRenderPath;
    private float mScaledWidth;
    private float mScaledHeight;

    /////////////////////////////////////////////////////
    // Variables below need to be copied (deep copy if applicable) for mutation.
    @NonNull
    private final GraphicContainerElement mRootGroup;
    private float mViewportWidth = 0;
    private float mViewportHeight = 0;
    private int mRootAlpha = 255;

    float mBaseWidth = 0;
    float mBaseHeight = 0;

    PathRenderer(@NonNull GraphicContainerElement element) {
        mRootGroup = element;
        mPath = new Path();
        mRenderPath = new Path();
    }

    void setRootAlpha(int alpha) {
        mRootAlpha = alpha;
    }

    int getRootAlpha() {
        return mRootAlpha;
    }

    PathRenderer(@NonNull PathRenderer copy) {
        mRootGroup = copy.mRootGroup;
        mPath = new Path(copy.mPath);
        mRenderPath = new Path(copy.mRenderPath);
        mBaseWidth = copy.mBaseWidth;
        mBaseHeight = copy.mBaseHeight;
        mViewportWidth = copy.mViewportWidth;
        mViewportHeight = copy.mViewportHeight;
        mRootAlpha = copy.mRootAlpha;
    }

    void applyBaseAndViewportDimensions() {
        applyBaseDimensions();
        applyViewportDimensions();
    }

    private void applyBaseDimensions() {
        mBaseWidth = mRootGroup.getWidthActual();
        mBaseHeight = mRootGroup.getHeightActual();
    }

    private void applyViewportDimensions() {
        mViewportWidth = mRootGroup.getViewportWidthActual();
        mViewportHeight = mRootGroup.getViewportHeightActual();
    }

    @NonNull
    GraphicContainerElement getRootGroup() {
        return mRootGroup;
    }

    /**
     * Draws the AVG to the canvas.
     *
     * @param canvas    the canvas to draw the avg to
     * @param w         the width of the avg
     * @param h         the height of the avg
     */
    void draw(@NonNull Canvas canvas, int w, int h, @NonNull IBitmapFactory bitmapFactory, boolean useHardwareAcceleration) {
        mScaledWidth = w / mViewportWidth;
        mScaledHeight = h / mViewportHeight;
        // Traverse the tree in pre-order to draw.
        drawGraphicElement(mScaledWidth, mScaledHeight, IDENTITY_MATRIX, mRootGroup, canvas, (float)mRootAlpha / 255, bitmapFactory, useHardwareAcceleration);
    }

    @Nullable
    static Shader createPattern(@NonNull final Matrix transform,
                                @NonNull final GraphicPattern graphicPattern) {
        int patternWidth = (int)graphicPattern.getWidth();
        int patternHeight = (int)graphicPattern.getHeight();
        if (patternWidth < 1 || patternHeight < 1) {
            Log.w(TAG, "Provided pattern dimensions are invalid");
            return null;
        }
        RenderingContext renderingContext = graphicPattern.getRenderingContext();
        IBitmapFactory bitmapFactory = renderingContext.getBitmapFactory();
        Bitmap patternBitmap;
        try {
            patternBitmap = bitmapFactory.createBitmap(patternWidth, patternHeight);
        } catch (BitmapCreationException e) {
            Log.e(TAG, "Error creating AVG pattern bitmap.", e);
            return null;
        }
        Canvas canvas = new Canvas(patternBitmap);

        for (GraphicElement item : graphicPattern.getItems()) {
            // use hardware acceleration as creating a shader has no scaling factor
            drawGraphicElement(SCALE_X_100_PCT, SCALE_Y_100_PCT, IDENTITY_MATRIX, item, canvas, 1.0f, bitmapFactory, true);
        }

        BitmapShader bitmapShader =
                new BitmapShader(patternBitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
        bitmapShader.setLocalMatrix(transform);
        return bitmapShader;
    }

    private static void drawGroupTree(final float xScale, final float yScale,
                                      @NonNull final Matrix currentTransform,
                                      @NonNull final GraphicGroupElement currentGroup,
                                      @NonNull final Canvas canvas,
                                      final float currentOpacity,
                                      @NonNull final IBitmapFactory bitmapFactory,
                                      boolean useHardwareAcceleration) {

        // Calculate current group's matrix by preConcat the parent's and
        // and the current one on the top of the stack.
        // Basically the Mfinal = Mviewport * M0 * M1 * M2;
        // Mi the local matrix at level i of the group tree.
        currentGroup.getStackedMatrix().set(currentTransform);
        currentGroup.getStackedMatrix().preConcat(currentGroup.getLocalMatrix());

        // Save the current clip information, which is local to this group.
        canvas.save();

        // If VClipPath is set on this VGroup, then it needs to be processed before
        // any children.
        if (currentGroup.getClipPathNodes() != null) {
            drawClipPath(xScale, yScale, currentGroup, canvas);
        }

        // Calculate new stacked opacity.
        float stackedOpacity = currentOpacity * currentGroup.getOpacity();

        // Draw the group tree in the same order as the AVG tree.
        for (GraphicElement child : currentGroup.getChildren()) {
            drawGraphicElement(xScale, yScale, currentGroup.getStackedMatrix(), child, canvas, stackedOpacity, bitmapFactory, useHardwareAcceleration);
        }

        canvas.restore();
    }

    private static void drawGraphicElement(final float xScale, final float yScale,
                                           @NonNull final Matrix currentTransform,
                                           @NonNull final GraphicElement graphicElement,
                                           @NonNull final Canvas parentCanvas,
                                           final float currentOpacity,
                                           @NonNull final IBitmapFactory bitmapFactory,
                                           boolean useHardwareAcceleration) {
        Bitmap bitmap = getFilterBitmap(parentCanvas.getWidth(), parentCanvas.getHeight(), graphicElement, bitmapFactory);
        Canvas canvas = parentCanvas;

        // Drop shadow bitmap computation uses the bitmap of the AVG object for which the filter is
        // applied. And so the original AVG object (group tree) needs to be drawn on the bitmap backed canvas.
        if(bitmap != null) {
            canvas = new Canvas(bitmap);
        }

        if (graphicElement instanceof GraphicGroupElement) {
            GraphicGroupElement graphicGroupElement = (GraphicGroupElement) graphicElement;
            drawGroupTree(xScale, yScale, currentTransform,
                    graphicGroupElement, canvas, currentOpacity, bitmapFactory, useHardwareAcceleration);
        } else if (graphicElement instanceof GraphicPathElement) {
            GraphicPathElement graphicPathElement = (GraphicPathElement) graphicElement;
            drawPath(xScale, yScale, currentTransform, graphicPathElement, canvas, currentOpacity, useHardwareAcceleration);
        } else if (graphicElement instanceof GraphicTextElement) {
            GraphicTextElement graphicTextElement = (GraphicTextElement) graphicElement;
            drawText(xScale, yScale, currentTransform, graphicTextElement, canvas, currentOpacity);
        }

        if(bitmap != null) {
            graphicElement.applyFilters(bitmap, xScale, yScale);
            parentCanvas.drawBitmap(bitmap, new Matrix(), new Paint());
        }
    }

    private static void drawText(final float xScale, final float yScale,
                                 @NonNull final Matrix currentTransform,
                                 @NonNull final GraphicTextElement textElement,
                                 @NonNull final Canvas canvas,
                                 final float stackedOpacity) {
        Matrix textTransform = new Matrix(currentTransform);
        textTransform.postScale(xScale, yScale);

        canvas.save();
        canvas.concat(textTransform);

        Paint fillPaint = textElement.getFillPaint(stackedOpacity);
        canvas.drawText(textElement.getText(), textElement.getX(), textElement.getY(), fillPaint);

        Paint strokePaint = textElement.getStrokePaint(stackedOpacity);
        canvas.drawText(textElement.getText(), textElement.getX(), textElement.getY(), strokePaint);

        canvas.restore();
    }

    private static void drawClipPath(final float xScale, final float yScale,
                                     @NonNull final GraphicGroupElement groupElement,
                                     @NonNull final Canvas canvas) {
        Matrix scaledMatrix = scaleMatrix(groupElement.getStackedMatrix(), xScale, yScale);

        final float matrixScale = getMatrixScale(groupElement.getStackedMatrix());
        if (matrixScale == 0) {
            // When either x or y is scaled to 0, we don't need to draw anything.
            return;
        }

        Path clipPath = new Path();
        PathParser.toPath(groupElement.getClipPathNodes(), clipPath);
        clipPath.transform(scaledMatrix);

        canvas.clipPath(clipPath);
    }

    private static void drawPath(final float xScale, final float yScale,
                                 @NonNull final Matrix currentTransform,
                                 @NonNull final GraphicPathElement pathElement,
                                 @NonNull final Canvas canvas,
                                 final float stackedOpacity,
                                 boolean useHardwareAcceleration) {
        Matrix scaledTransform = scaleMatrix(currentTransform, xScale, yScale);
        final float matrixScale = getMatrixScale(currentTransform);

        if (matrixScale == 0) {
            // When either x or y is scaled to 0, we don't need to draw anything.
            return;
        }

        Path currentPath;
        // Scale the paths for hardware rendering
        if (useHardwareAcceleration) {
            // create a new Path because we still want to reference the original unscaled Path
            currentPath = new Path(pathElement.getPath());
            currentPath.transform(scaledTransform);
            canvas.save();
        } else {
            currentPath = pathElement.getPath();
            canvas.save();
            canvas.concat(scaledTransform);
        }

        Paint fillPaint = pathElement.getFillPaint(stackedOpacity);
        Shader fillShader = fillPaint.getShader();

        if (useHardwareAcceleration && fillShader != null) {
            if (pathElement.getProperties().isGradient(GraphicPropertyKey.kGraphicPropertyFill)) {
                if (pathElement.getGradient(GraphicPropertyKey.kGraphicPropertyFill).getType() == GradientType.RADIAL) {
                    // Gradient is radial so we obtain a new Shader, same is not needed for Linear gradient
                    fillPaint.setShader(transformRadialGradientShader(pathElement, GraphicPropertyKey.kGraphicPropertyFill, scaledTransform, pathElement.getFillTransform()));
                }
            }
            Matrix fillTransform = pathElement.getFillTransform();
            fillTransform.postConcat(scaledTransform);
            fillShader.setLocalMatrix(fillTransform);
        }

        currentPath.setFillType(Path.FillType.WINDING);
        canvas.drawPath(currentPath, fillPaint);

        Paint strokePaint = pathElement.getStrokePaint(stackedOpacity);
        Shader strokeShader = strokePaint.getShader();

        if (useHardwareAcceleration && strokeShader != null) {
            if (pathElement.getProperties().isGradient(GraphicPropertyKey.kGraphicPropertyStroke)) {
                if (pathElement.getGradient(GraphicPropertyKey.kGraphicPropertyStroke).getType() == GradientType.RADIAL) {
                    // Gradient is radial so we obtain a new Shader, same is not needed for Linear gradient
                    strokePaint.setShader(transformRadialGradientShader(pathElement, GraphicPropertyKey.kGraphicPropertyStroke, scaledTransform, pathElement.getStrokeTransform()));
                }
            }
            Matrix strokeTransform = pathElement.getStrokeTransform();
            strokeTransform.postConcat(scaledTransform);
            strokeShader.setLocalMatrix(strokeTransform);
        }

        // Adjust stroke properties for hardware acceleration
        if (useHardwareAcceleration) {
            float scaleFactor = getMatrixScale(scaledTransform);
            // Adjust stroke width
            float scaledStrokeWidth = pathElement.getStrokeWidth() * scaleFactor;
            strokePaint.setStrokeWidth(scaledStrokeWidth);
            // Adjust dash path effect
            if (pathElement.getStrokeDashArray().length > 0) {
                // Original path needs to be used to create the dash path effect
                // And the scale factor needs to be multiplied to the dash array and dash offset.
                DashPathEffect dashPathEffect = createDashPathEffect(pathElement.getPath(), pathElement, scaleFactor);
                strokePaint.setPathEffect(dashPathEffect);
            }
        }

        canvas.drawPath(currentPath, strokePaint);
        canvas.restore();
    }

    static DashPathEffect createDashPathEffect(@NonNull final Path path, final GraphicPathElement pathElement, final float scaleFactor) {
        float[] strokeDashArray = pathElement.getStrokeDashArray();
        float strokeDashOffset = pathElement.getStrokeDashOffset();

        // Multiply the canvas scale factor to dash array and offset manually,
        // since the canvas will not be scaled when hardware acceleration is used.
        for (int i = 0; i < strokeDashArray.length; i++) {
            strokeDashArray[i] = strokeDashArray[i] * scaleFactor;
        }
        strokeDashOffset *= scaleFactor;

        // If pathLength is defined in the APL document, then the user-defined scale
        // needs to be multiplied to the dash array and offset
        if (pathElement.getPathLength() > 0) {
            PathMeasure pathMeasure = new PathMeasure();
            pathMeasure.setPath(path, false);

            float truePathLength = pathMeasure.getLength();
            float userDefinedScale = truePathLength / pathElement.getPathLength();

            float[] userDefinedScaleStrokeDashArray = new float[strokeDashArray.length];
            for (int i = 0; i < strokeDashArray.length; i++) {
                userDefinedScaleStrokeDashArray[i] = strokeDashArray[i] * userDefinedScale;
            }
            strokeDashArray = userDefinedScaleStrokeDashArray;
            strokeDashOffset *= userDefinedScale;
        }

        return new DashPathEffect(strokeDashArray, strokeDashOffset);
    }

    /**
     * Returns a new bitmap or clears an existing one to be used temporarily to apply filters.
     * A group cannot reuse a bitmap from another group in case of nested groups since we cannot
     * clear a bitmap before we finished drawing.
     *
     * @param width the width of the bitmap
     * @param height the height of the bitmap
     * @param graphicElement the graphic element to get the bitmap for
     * @param bitmapFactory the factory to use to create bitmaps
     * @return a new or cleared bitmap
     */
    private static Bitmap getFilterBitmap(int width, int height, GraphicElement graphicElement, @NonNull IBitmapFactory bitmapFactory) {
        if(!graphicElement.containsFilters()) {
            return null;
        }
        GraphicGroupElement groupElement = null;
        if(graphicElement instanceof GraphicGroupElement) {
            groupElement = (GraphicGroupElement) graphicElement;
        }

        FilterBitmapKey key = FilterBitmapKey.create(width, height, groupElement == null ? 0 : groupElement.hashCode());
        IBitmapCache bitmapCache = graphicElement.getRenderingContext().getBitmapCache();
        Bitmap bitmap = bitmapCache.getBitmap(key);

        if(bitmap != null) {
            bitmap.eraseColor(Color.TRANSPARENT);
            return bitmap;
        }
        try {
            bitmap = bitmapFactory.createBitmap(width, height);
        } catch (BitmapCreationException e) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "Error creating filter bitmap.", e);
            }
            return null;
        }

        bitmapCache.putBitmap(key, bitmap);
        return bitmap;
    }

    @NonNull
    private static Matrix scaleMatrix(@NonNull final Matrix matrixToScale, final float scaleWidth,
                                      final float scaleHeight) {
        Matrix scaledMatrix = new Matrix(matrixToScale);
        scaledMatrix.postScale(scaleWidth, scaleHeight);
        return scaledMatrix;
    }

    private static float cross(final float v1x, final float v1y, final float v2x, final float v2y) {
        return v1x * v2y - v1y * v2x;
    }

    private static float getMatrixScale(@NonNull final Matrix groupStackedMatrix) {
        // Given unit vectors A = (0, 1) and B = (1, 0).
        // After matrix mapping, we got A' and B'. Let theta = the angel b/t A' and B'.
        // Therefore, the final scale we want is min(|A'| * sin(theta), |B'| * sin(theta)),
        // which is (|A'| * |B'| * sin(theta)) / max (|A'|, |B'|);
        // If  max (|A'|, |B'|) = 0, that means either x or y has a scale of 0.
        //
        // For non-skew case, which is most of the cases, matrix scale is computing exactly the
        // scale on x and y axis, and take the minimal of these two.
        // For skew case, an unit square will mapped to a parallelogram. And this function will
        // return the minimal height of the 2 bases.
        float[] unitVectors = new float[]{0, 1, 1, 0};
        groupStackedMatrix.mapVectors(unitVectors);
        float scaleX = (float) Math.hypot(unitVectors[0], unitVectors[1]);
        float scaleY = (float) Math.hypot(unitVectors[2], unitVectors[3]);
        float crossProduct = cross(unitVectors[0], unitVectors[1], unitVectors[2],
                unitVectors[3]);
        float maxScale = Math.max(scaleX, scaleY);

        float matrixScale = 0;
        if (maxScale > 0) {
            matrixScale = Math.abs(crossProduct) / maxScale;
        }
        return matrixScale;
    }

    private static Shader transformRadialGradientShader(GraphicPathElement pathElement, GraphicPropertyKey key, Matrix scaledTransform, Matrix originalTransform){
        // For radial gradients, since we are using the bounds we want the bounds of the original path to prevent double scaling
        Rect bounds = FillStrokeGraphicElement.getBounds(pathElement.getPath(), pathElement.getRenderingContext());
        // Create a new scaling matrix by multiplying the original transformation Matrix and current scaleTransform
        originalTransform.postConcat(scaledTransform);
        Shader updatedShader = ShaderFactory.getInstance().getShader(pathElement.getGradient(key), bounds, originalTransform, pathElement.getRenderingContext());
        return updatedShader;
    }

}