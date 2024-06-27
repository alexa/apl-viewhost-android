/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;

import android.graphics.Paint;
import android.os.Build;
import android.os.Trace;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Transformation;

import androidx.annotation.NonNull;

import com.amazon.apl.android.APLLayoutParams;
import com.amazon.apl.android.primitive.SGRadii;
import com.amazon.apl.android.primitive.SGRect;
import com.amazon.apl.android.scenegraph.APLLayer;
import com.amazon.apl.android.scenegraph.generic.Point;
import com.amazon.apl.android.scenegraph.rendering.APLRender;
import com.amazon.apl.android.sgcontent.Node;
import com.amazon.apl.android.sgcontent.Shadow;

public class APLView extends ViewGroup {

    public APLLayer mAplLayer;

    public APLView(final Context context, final APLLayer aplLayer) {
        super(context);
        mAplLayer = aplLayer;
        SGRect bounds = aplLayer.getBounds();
        int w = bounds.intWidth();
        int h = bounds.intHeight();
        /**
         * As per https://developer.android.com/topic/performance/hardware-accel#scaling ,
         * on older Android versions < API level 28, some drawing operations degrading quality
         * significantly at higher scale values. The provided table shows drawPath being an
         * impacted drawing operation. Hence disable hardware acceleration for Path operations
         * on old Android versions.
         */
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            for (Node node : aplLayer.getContent()) {
                if (containsDrawNode(node)) {
                    disableHardwareAcceleration();
                    break;
                }
            }
        }
        setAlpha(aplLayer.getOpacity());
        setLayoutParams(new APLLayoutParams(w, h, 0,0));
        setWillNotDraw(false);
        this.setStaticTransformationsEnabled(true);
    }

    @Override
    protected boolean getChildStaticTransformation(View child, Transformation t) {
        if (child instanceof APLView) {
            Matrix m = ((APLView) child).mAplLayer.getTransform();
            t.setTransformationType(Transformation.TYPE_MATRIX);
            t.getMatrix().set(m);
            return true;
        }
        return false;
    }

    @NonNull
    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        int w = mAplLayer.getBounds().intWidth();
        int h = mAplLayer.getBounds().intHeight();
        return new APLAbsoluteLayout.LayoutParams(w, h, 0, 0);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = mAplLayer.getBounds().intWidth();
        int h = mAplLayer.getBounds().intHeight();
        setMeasuredDimension(MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY));
        // measure children
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                ViewGroup.LayoutParams lp = child.getLayoutParams();
                child.measure(MeasureSpec.makeMeasureSpec(lp.width, MeasureSpec.EXACTLY),
                        MeasureSpec.makeMeasureSpec(lp.height, MeasureSpec.EXACTLY));
            }
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        SGRect bounds = mAplLayer.getBounds();
        int w = bounds.intWidth();
        int h = bounds.intHeight();
        for (int i = 0; i < getChildCount(); i++) {
            View viewChild = getChildAt(i);
            if (viewChild.getVisibility() != GONE) {
                if (viewChild instanceof APLView) {
                    SGRect childBounds = ((APLView) viewChild).mAplLayer.getBounds();
                    // Apply child offset which takes care of things like scrolling
                    Point<Float> offsetPoint = mAplLayer.getChildOffset();
                    int scrolledX = Math.round(childBounds.getLeft() - offsetPoint.getX());
                    int scrolledY = Math.round(childBounds.getTop() - offsetPoint.getY());
                    // currently laying out view based on its bounds. This prevents us from drawing
                    // shadows outside its padding but within the clipping bounds. Currently
                    // side-stepping this shadow issue by drawing the layer's shadow from the parent
                    // view, but this may have correctness/performance implications.
                    viewChild.layout(scrolledX, scrolledY, scrolledX + childBounds.intWidth(), scrolledY + childBounds.intHeight());
                } else {
                    viewChild.layout(0, 0, w, h);
                }
            }
        }
    }

    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        int saveCount = canvas.save();
        applyClippingBoundsForChildren(canvas);

        // see notes in onLayout
        if (child instanceof APLView) {
            ((APLView) child).drawShadow(canvas, mAplLayer.getChildOffset());
        }

        boolean result = super.drawChild(canvas, child, drawingTime);
        canvas.restoreToCount(saveCount);
        return result;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawContent(canvas, mAplLayer.getContent());
    }

    private void drawShadow(Canvas canvas, Point<Float> childOffsetPoint) {
        android.graphics.Path path = mAplLayer.getShadowPath();
        if (path != null) {
            canvas.save();
            SGRect bounds = mAplLayer.getBounds();
            int scrolledX = Math.round(bounds.getLeft() - childOffsetPoint.getX());
            int scrolledY = Math.round(bounds.getTop() - childOffsetPoint.getY());
            canvas.translate(scrolledX, scrolledY);
            canvas.drawPath(path, mAplLayer.getShadowPaint());
            canvas.restore();
        }
    }

    private void applyClippingBoundsForChildren(Canvas canvas) {
        android.graphics.Path outline = mAplLayer.getOutlinePath();
        if (outline != null) {
            canvas.clipPath(outline);
        } else {
            canvas.clipRect(mAplLayer.getBoundsRect());
        }

        android.graphics.Path childClip = mAplLayer.getChildClip();
        if (childClip != null) {
            canvas.clipPath(childClip);
        }
    }

    private void drawContent(Canvas canvas, Node[] content) {
        try {
            Trace.beginSection("APLView.drawContent");
            for (int i = 0; i < content.length; i++) {
                APLRender.drawNode(mAplLayer, mAplLayer.getRenderingContext(), content[i], 1.0f, canvas);
            }
        } finally {
            Trace.endSection();
        }
    }

    private void disableHardwareAcceleration() {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        this.setLayerType(LAYER_TYPE_SOFTWARE, p);
    }

    private boolean containsDrawNode(Node node) {
        if ("Draw".equals(node.getType()) && "General".equals(node.getPath().getType())) {
            return true;
        }
        for (Node child : node.getChildren()) {
            boolean containsDrawNode = containsDrawNode(child);
            if (containsDrawNode) {
                return true;
            }
        }
        return false;
        //return true;
    }
}
