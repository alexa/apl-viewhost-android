package com.amazon.apl.android.scenegraph;

import android.graphics.BlurMaskFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Trace;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;

import com.amazon.apl.android.APLAccessibilityDelegate;
import com.amazon.apl.android.Component;
import com.amazon.apl.android.IAccessibilityProvider;
import com.amazon.apl.android.RenderingContext;
import com.amazon.apl.android.primitive.SGRect;
import com.amazon.apl.android.scenegraph.accessibility.Accessibility;
import com.amazon.apl.android.scenegraph.edittext.APLEditLayer;
import com.amazon.apl.android.scenegraph.generic.Point;
import com.amazon.apl.android.scenegraph.media.APLVideoLayer;
import com.amazon.apl.android.scenegraph.rendering.APLRender;
import com.amazon.apl.android.sgcontent.VideoNode;
import com.amazon.apl.android.sgcontent.EditTextNode;
import com.amazon.apl.android.sgcontent.Node;
import com.amazon.apl.android.sgcontent.Path;
import com.amazon.apl.android.sgcontent.Shadow;
import com.amazon.apl.android.utils.AccessibilitySettingsUtil;

public class APLLayer {
    private long mCoreLayerHandle = 0;
    public ViewGroup mChildView;
    private final RenderingContext mRenderingContext;
    private final AccessibilitySettingsUtil sAccessibilitySettingsUtil = AccessibilitySettingsUtil.getInstance();
    private android.graphics.Path mShadowPath;
    private Paint mShadowPaint;
    private android.graphics.Path mOutlinePath;
    private android.graphics.Path mClipPath;
    private android.graphics.RectF mBounds;

    public APLLayer(RenderingContext renderingContext) {
        mRenderingContext = renderingContext;
    }

    public RenderingContext getRenderingContext() {
        return mRenderingContext;
    }

    /**
     * For a given
     * @param coreLayerHandle
     * @return
     */
    public static APLLayer ensure(long coreLayerHandle, RenderingContext renderingContext) {
        APLLayer aplLayer = nGetAplLayer(coreLayerHandle);
        if (aplLayer == null) {
            if (nGetContent(coreLayerHandle).length == 1) {
                Node contentNode = new Node(nGetContent(coreLayerHandle)[0]);
                if ("EditText".equals(contentNode.getType())) {
                    aplLayer = new APLEditLayer(renderingContext);
                } else if ("Video".equals(contentNode.getType())) {
                    aplLayer = new APLVideoLayer(renderingContext);
                } else {
                    aplLayer = new APLContentLayer(renderingContext);
                }
            } else {
                aplLayer = new APLContentLayer(renderingContext);
            }
            aplLayer.mCoreLayerHandle = coreLayerHandle;
            aplLayer.nSetUserData(coreLayerHandle);
        }
        return aplLayer;
    }

    public void forceUpdate() {
        mChildView.postInvalidate();
    }

    /**
     * Called from C++ to update dirty properties for a layer.
     */
    void updateDirtyProperties(int flags) {
        try {
            Trace.beginSection("APLLayer.updateDirtyProperties");
        /*
            kFlagOpacityChanged = 1u << 0,
            kFlagPositionChanged = 1u << 1,
            kFlagSizeChanged = 1u << 2,
            kFlagTransformChanged = 1u << 3,
            kFlagChildOffsetChanged = 1u << 4,
            kFlagOutlineChanged = 1u << 5,
            kFlagRedrawContent = 1u << 6,
            kFlagRedrawShadow = 1u << 7,
            kFlagChildrenChanged = 1u << 8,
            kFlagChildClipChanged = 1u << 9,
            kFlagAccessibilityChanged = 1u << 10,
            kFlagInteractionChanged = 1u << 11
        */
            if ((flags & 1) != 0) {
                fixOpacity();
            }

            if ((flags & 2) != 0) { // position
                fixBounds();
                mChildView.invalidate();
            }

            if ((flags & 4) != 0) { // size
                fixBounds();
                mChildView.requestLayout();
            }

            if ((flags & 8) != 0) { // transform
                fixTransform(); // invalidates parent
            }

            if ((flags & 16) != 0) { // child offset
                mChildView.requestLayout();
            }

            if ((flags & 36) != 0) { // outline (32), or size (4) changed
                fixOutlinePath();
                mChildView.invalidate();
            }

            if ((flags & 64) != 0) { // redraw content
                mChildView.invalidate();
            }

            if ((flags & 164) != 0) { // shadow (128), outline (32), or size (4) changed
                fixShadow();
                invalidateParentView(); // layer shadow are drawn in the parent view
            }

            if ((flags & 256) != 0) { // children changed
                // Handled in {@code APLContentLayer}
            }

            if ((flags & 546) != 0) { // child clip changed (512), outline (32), or size (4) changed
                fixClipPath();
                mChildView.invalidate();
            }

            if ((flags & 1024) != 0) { // accessibility changed
                // TODO: Refactor.
                //fixAccessibility(mChildView);
            }

            if ((flags & 2048) != 0) { // interaction changed
                // TODO: what properties is this for?
            }
        } finally {
            Trace.endSection();
        }
    }

    private void fixProps() {
        if (mCoreLayerHandle == 0) return;
        fixBounds();
        fixOpacity();
        fixTransform();
        fixOutlinePath();
        fixClipPath();
        // Shadow uses Bounds/Outline so must be updated after those are updated
        fixShadow();
    }

    public android.graphics.Path getShadowPath() {
        return mShadowPath;
    }

    public Paint getShadowPaint() {
        return mShadowPaint;
    }

    private void fixBounds() {
        SGRect bounds = SGRect.create(nGetBounds(mCoreLayerHandle));
        mBounds = new android.graphics.RectF(0, 0, bounds.getWidth(), bounds.getHeight());
    }

    private void fixOutlinePath() {
        long pathHandle = nGetOutlinePath(mCoreLayerHandle);
        if (pathHandle != 0) {
            mOutlinePath = APLRender.convertPath(new Path(pathHandle));
        } else {
            mOutlinePath = null;
        }
    }

    private void fixClipPath() {
        long childClip = nGetChildClipPath(mCoreLayerHandle);
        if (childClip != 0) {
            mClipPath = APLRender.convertPath(new Path(childClip));
        } else {
            mClipPath = null;
        }
    }

    private void fixShadow() {
        Shadow shadow = getShadow();
        if (shadow != null) {
            mShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mShadowPaint.setColor(shadow.getColor());
            if (shadow.getRadius() > 0) {
                mShadowPaint.setMaskFilter(new BlurMaskFilter(shadow.getRadius(), BlurMaskFilter.Blur.NORMAL));
            }
            if (mOutlinePath != null) {
                mShadowPath = mOutlinePath;
            } else {
                // bounds offset is already applied to the canvas, so draw the shadow relative to 0,0
                mShadowPath = new android.graphics.Path();
                mShadowPath.addRect(mBounds, android.graphics.Path.Direction.CW);
            }
            float[] offset = shadow.getOffset();
            mShadowPath.offset(offset[0], offset[1]);
        } else {
            mShadowPath = null;
            mShadowPaint = null;
        }
    }

    public void attachView(ViewGroup view) {
        mChildView = view;
        // fixAccessibility(mChildView);
        fixProps();
    }

    long[] getChildren() {
        return nGetChildren(mCoreLayerHandle);
    }

    public Point getChildOffset() {
        float[] offset = nGetChildOffset(mCoreLayerHandle);
        Point<Float> offsetPoint = new Point<>(offset[0], offset[1]);
        return offsetPoint;
    }

    public SGRect getBounds() {
        return SGRect.create(nGetBounds(mCoreLayerHandle));
    }

    public RectF getBoundsRect() {
        return mBounds;
    }

    public android.graphics.Path getChildClip() {
        return mClipPath;
    }

    public android.graphics.Path getOutlinePath() {
        return mOutlinePath;
    }


    public Matrix getTransform() {
        Matrix m = new Matrix();
        float[] toMatrix = toMatrix(nGetLayerTransform(mCoreLayerHandle));
        m.setValues(toMatrix);
        return m;
    }

    public String getName() {
        return nGetLayerName(mCoreLayerHandle);
    }

    public float getOpacity() {
        return nGetLayerOpacity(mCoreLayerHandle);
    }

    public Shadow getShadow() {
        long shadowNativeHandle = nGetShadow(mCoreLayerHandle);
        return shadowNativeHandle != 0 ? new Shadow(shadowNativeHandle) : null;
    }

    private float[] toMatrix(float[] transform2D) {
        return new float[] {
                transform2D[0], transform2D[2], transform2D[4],
                transform2D[1], transform2D[3], transform2D[5],
                0,              0,              1
        };
    }

    public Node[] getContent() {
        long[] contentNodeAddresses = nGetContent(mCoreLayerHandle);
        Node[] nodes = new Node[contentNodeAddresses.length];
        int i = 0;
        for (long address : contentNodeAddresses) {
            Node node = Node.ensure(address);
            if ("EditText".equals(node.getType())) {
                nodes[i++] = new EditTextNode(address);
            } else if ("Video".equals(node.getType())) {
                nodes[i++] = new VideoNode(address);
            } else {
                nodes[i++] = node;
            }
        }
        return nodes;
    }

    public int getColor(long coreNodeHandle) {
        return nGetColor(coreNodeHandle);
    }

    /**
     * Apply the accessibility label and delegate to the view.
     * @param {@link IAccessibilityProvider} the component
     * @param view      the view
     */
    // TODO: APLAccessibilityDelegate takes a Component. The Accessibility type needs to either be
    // Merged together with Component or replaced by it.
    /*
    private void fixAccessibility(ViewGroup view) {
        Accessibility accessibilityProvider = getAccessibility();
        if (accessibilityProvider != null) {
            if (!ViewCompat.hasAccessibilityDelegate(view)) {
                ViewCompat.setAccessibilityDelegate(view, APLAccessibilityDelegate.create(accessibilityProvider, view.getContext()));
            }

            String accessibility = accessibilityProvider.getAccessibilityLabel();
            if (accessibility != null) {
                view.setContentDescription(accessibility);
            }

            applyFocusability(accessibilityProvider, view);
        }
    }
    */

    /**
     * Apply the accessibility focus to the view.
     * @param view      the view
     */
    public void applyFocusability(IAccessibilityProvider accessibilityProvider, ViewGroup view) {
        boolean pressable = nIsAccessibilityPressable(mCoreLayerHandle);
        boolean focusable = pressable && !accessibilityProvider.isDisabled();
        boolean focusableInTouchMode = pressable;
        // TODO this setting should likely be put in APLAccessibilityDelegate
        boolean focusableForAccessibility = (!TextUtils.isEmpty(accessibilityProvider.getAccessibilityLabel()) && sAccessibilitySettingsUtil.isScreenReaderEnabled(view.getContext()));

        view.setFocusable(focusable || focusableForAccessibility);
        view.setFocusableInTouchMode(focusableInTouchMode || focusableForAccessibility);
    }

    @Nullable
    public Accessibility getAccessibility() {
        if (mCoreLayerHandle == 0) return null;
        return new Accessibility(nGetAccessibility(mCoreLayerHandle),
                                getName(),
                                isScrollable(),
                                nIsAccessibilityChecked(mCoreLayerHandle),
                                nIsAccessibilityDisabled(mCoreLayerHandle));
    }

    // TODO: May need to break this further.
    public boolean isScrollable() {
        return nIsHorizontallyScrollable(mCoreLayerHandle) || nIsVerticallyScrollable(mCoreLayerHandle);
    }

    private void fixTransform() {
        if (mChildView != null) {
            invalidateParentView();

            // needed on FOS5 for static transform to be applied on hw layer
            mChildView.invalidate();
        }
    }

    private void invalidateParentView() {
        if (mChildView != null) {
            ViewParent parent = mChildView.getParent();
            if (parent instanceof View) {
                ((View) parent).invalidate();
            }
        }
    }

    private void fixOpacity() {
        mChildView.setAlpha(getOpacity());
    }

    // Native utility methods to retrieve layer attributes
    private static native APLLayer nGetAplLayer(long coreLayerHandle);
    private native void nSetUserData(long coreLayerHandle);
    private native long[] nGetChildren(long coreLayerHandle);
    private static native float[] nGetBounds(long nativeHandle);
    private static native float[] nGetChildOffset(long nativeHandle);
    private static native long nGetOutlinePath(long nativeHandle);
    private static native float[] nGetOutlineRect(long nativeHandle);
    private static native long nGetShadow(long nativeHandle);
    private static native long nGetAccessibility(long nativeHandle);
    private static native boolean nIsAccessibilityChecked(long nativeHandle);
    private static native boolean nIsAccessibilityDisabled(long nativeHandle);
    private static native boolean nIsAccessibilityPressable(long nativeHandle);
    private static native boolean nIsHorizontallyScrollable(long nativeHandle);
    private static native boolean nIsVerticallyScrollable(long nativeHandle);
    // Don't use
    private static native float[] nGetLayerClipRect(long nativeHandle);
    private static native long nGetChildClipPath(long nativeHandle);
    private static native float[] nGetLayerTransform(long nativeHandle);
    private static native float[] nGetOutlineRadii(long nativeHandle);
    private static native float[] nGetLayerClipRadii(long nativeHandle);
    private static native float nGetLayerOpacity(long nativeHandle);
    private static native long[] nGetContent(long coreLayerHandle);
    private static native String nGetLayerName(long coreLayerHandle);
    private static native int nGetColor(long coreContentHandle);
}
