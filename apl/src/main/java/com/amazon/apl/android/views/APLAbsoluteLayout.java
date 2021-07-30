/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.views;


import android.animation.LayoutTransition;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Path;
import android.graphics.RectF;
import androidx.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Transformation;

import com.amazon.apl.android.APLLayout;
import com.amazon.apl.android.APLVersionCodes;
import com.amazon.apl.android.Component;
import com.amazon.apl.android.IAPLViewPresenter;
import com.amazon.apl.android.primitive.Radii;
import com.amazon.apl.android.primitive.Rect;
import com.amazon.apl.android.shadow.ShadowBitmapRenderer;
import com.amazon.apl.enums.ComponentType;
import com.amazon.apl.enums.PropertyKey;
import com.amazon.apl.enums.ScrollDirection;

/**
 * Simple absolute layout.
 */
@SuppressLint("ViewConstructor")
public class APLAbsoluteLayout extends ViewGroup {
    private static final String TAG = "APLAbsoluteLayout";

    final IAPLViewPresenter mPresenter;

    private final ShadowBitmapRenderer mShadowRenderer;
    private Path mPath = new Path();

    /**
     * Construct an absolute layout for a Component.
     *
     * @param context   The Android Context.
     */
    public APLAbsoluteLayout(Context context, IAPLViewPresenter presenter) {
        super(context);
        setClipChildren(false);
        setStaticTransformationsEnabled(true);
        mPresenter = presenter;
        mShadowRenderer = presenter.getShadowRenderer();
    }

    /**
     * Measure the Component bounds and set the measured dimensions.
     *
     * @param widthMeasureSpec  overridden by bounds
     * @param heightMeasureSpec overridden by bounds
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // set size to absolute Component bounds
        Component component = mPresenter.findComponent(this);
        if (component == null) {
            Log.e(TAG, "onMeasure. Component is null. Cannot retrieve component bounds." + this);
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }
        Rect bounds = component.getBounds();
        int w = (int)bounds.getWidth();
        int h = (int)bounds.getHeight();
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
    protected boolean getChildStaticTransformation(View child, @NonNull Transformation t) {
        Component component = mPresenter.findComponent(child);
        if(component == null) {
            return false;
        }
        Matrix m = component.getTransform();
        t.getMatrix().set(m);

        return true;
    }

    /**
     * Returns a set of layout parameters that match the component bounds.
     */
    @NonNull
    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        Component component = mPresenter.findComponent(this);
        if (component == null) {
            Log.e(TAG, "generateDefaultLayoutParams. Component is null");
            return new LayoutParams(0, 0, 0, 0);
        }
        Rect bounds = component.getBounds();
        int w = (int)bounds.getWidth();
        int h = (int)bounds.getHeight();
        return new LayoutParams(w, h, 0, 0);
    }


    /**
     * Layout children in absolute dimensions.
     */
    @Override
    protected void onLayout(boolean changed, int l, int t,
                            int r, int b) {
        Component component = mPresenter.findComponent(this);
        if (component == null) {
            Log.e(TAG, "onLayout. Component is null");
            return;
        }
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View viewChild = getChildAt(i);
            if (viewChild.getVisibility() != GONE) {
                Component childComponent = mPresenter.findComponent(viewChild);
                LayoutParams lp = (LayoutParams) viewChild.getLayoutParams();
                viewChild.layout(lp.x, lp.y, lp.x + lp.width, lp.y + lp.height);
                // There are situations when Core's component children count can
                // be different than Viewhost's viewgroup children count for a
                // moment. This is possible in the following cases:
                // 1. SwipeAway (it adds new component on gesture start and removes one based on gesture been fulfilled or not)
                // 2. When number of children is manipulated from core in lazy loading.

                if(childComponent.shouldDrawBoxShadow()) {
                    mShadowRenderer.prepareShadow(childComponent);
                }
            }
        }
        if (component.hasProperty(PropertyKey.kPropertyScrollPosition)) {
            updateScrollPosition(mScrollPosition, mScrollDirection);
        }
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);

        Component component = mPresenter.findComponent(this);
        if (component == null) {
            return;
        }

        boolean isAtLeastAPL16 = component.getRenderingContext().getDocVersion() >= APLVersionCodes.APL_1_6;
        ComponentType componentType = component.getComponentType();

        // We previously clipped the children for only the following containers.
        // In order to have backwards compatibility with clipping rules, we clip
        // the following components and otherwise do a doc version check to avoid
        // clipping the children of components that previously didn't clip their children
        // to the parent bounds.
        if (componentType == ComponentType.kComponentTypeFrame ||
                componentType == ComponentType.kComponentTypeSequence ||
                componentType == ComponentType.kComponentTypeGridSequence ||
                componentType == ComponentType.kComponentTypePager ||
                componentType == ComponentType.kComponentTypeScrollView) {
            this.setClipChildren(true);

            resetPath();
            Radii radii = component.getProperties().getRadii(PropertyKey.kPropertyBorderRadii);
            if (radii != null) {
                mPath.addRoundRect(new RectF(0, 0, width, height), radii.toFloatArray(), Path.Direction.CW);
            } else {
                mPath.addRect(new RectF(0, 0, width, height), Path.Direction.CW);
            }
        } else if (isAtLeastAPL16) {
            this.setClipChildren(true);
            resetPath();
            mPath.addRect(new RectF(0, 0, width, height), Path.Direction.CW);
        } else {
            mPath = null;
        }
    }

    private void resetPath() {
        if (mPath == null) {
            mPath = new Path();
        } else {
            mPath.rewind();
        }
    }

    private int mScrollPosition = 0;
    private ScrollDirection mScrollDirection;

    public void updateScrollPosition(int scrollPosition, ScrollDirection scrollDirection) {
        // TODO consider a different approach for handling scroll position.
        //  This positions the views in the correct position post layout.
        //  We cannot simply move the canvas in draw because Accessibility and Video rely on the
        //  view's location on screen.

        mScrollPosition = scrollPosition;
        mScrollDirection = scrollDirection;
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (mScrollDirection == ScrollDirection.kScrollDirectionHorizontal) {
                child.setTranslationX(-mScrollPosition);
            } else {
                child.setTranslationY(-mScrollPosition);
            }
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        if (mPath != null) {
            canvas.clipPath(mPath);
        }
        super.dispatchDraw(canvas);
    }

    @NonNull
    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        throw new UnsupportedOperationException("Layout Params can only be derived from an APL document");
    }


    // Override to allow type-checking of LayoutParams.
    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }


    @NonNull
    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        throw new UnsupportedOperationException("Layout Params can only be derived from an APL document");
    }

    @Override
    public boolean shouldDelayChildPressedState() {
        return false;
    }

    // TODO remove when Core starts defining onChildAdded and onChildRemoved animations.
    private static LayoutTransition sLayoutTransition = new LayoutTransition();

    @Override
    public void removeViewInLayout(View view) {
        if (getLayoutTransition() == null) {
            setLayoutTransition(sLayoutTransition);
        }
        super.removeViewInLayout(view);
        setLayoutTransition(null);
    }

    public void addViewInLayout(View view, LayoutParams params) {
        // Cancel current layout transition. Setting to null doesn't cancel the animation, but resetting
        // to the default animation will.
        if (sLayoutTransition.isRunning()) {
            setLayoutTransition(sLayoutTransition);
            setLayoutTransition(null);
        }

        this.addViewInLayout(view, -1, params, true);
    }

    public void detachAllViews() {
        detachAllViewsFromParent();
    }

    public void removeDetachedView(View child) {
        removeDetachedView(child, false);
    }

    public void attachView(View child) {
        attachViewToParent(child, -1, child.getLayoutParams());
    }

    @Override
    public void onViewRemoved(View child) {
        super.onViewRemoved(child);
        Component childComponent = mPresenter.findComponent(child);
        if (childComponent != null) {
            APLLayout.traverseComponentHierarchy(childComponent, mPresenter::disassociateView);
        }
    }

    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        final Component childComponent = mPresenter.findComponent(child);
        if(childComponent != null && childComponent.shouldDrawBoxShadow()) {
            mShadowRenderer.drawShadow(canvas, childComponent, this);
        }
        return super.drawChild(canvas, child, drawingTime);
    }

    /**
     * Per-child layout information associated with APLAbsoluteLayout.
     */
    public static class LayoutParams extends ViewGroup.LayoutParams {
        /**
         * The horizontal, or X, location of the child within the view group.
         */
        public int x;
        /**
         * The vertical, or Y, location of the child within the view group.
         */
        public int y;

        /**
         * Creates a new set of layout parameters with the specified width,
         * height and location.
         *
         * @param width  the width
         * @param height the height
         * @param x      the X location of the child
         * @param y      the Y location of the child
         */
        public LayoutParams(int width, int height, int x, int y) {
            super(width, height);
            this.x = x;
            this.y = y;
        }

        public String toString() {
            return String.format("APLAbsoluteLayoutParams[%dx%d, (%d, %d)]", width, height, x, y);
        }
    }

}


