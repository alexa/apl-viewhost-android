/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android;

import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.os.SystemClock;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.annotation.VisibleForTesting;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityManager;
import android.view.animation.Transformation;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.amazon.apl.android.bitmap.BitmapCreationException;
import com.amazon.apl.android.bitmap.BitmapFactory;
import com.amazon.apl.android.bitmap.GlideCachingBitmapPool;
import com.amazon.apl.android.bitmap.IBitmapFactory;
import com.amazon.apl.android.bitmap.IBitmapPool;
import com.amazon.apl.android.bitmap.PooledBitmapFactory;
import com.amazon.apl.android.component.ComponentViewAdapter;
import com.amazon.apl.android.component.ComponentViewAdapterFactory;
import com.amazon.apl.android.component.ImageViewAdapter;
import com.amazon.apl.android.configuration.ConfigurationChange;
import com.amazon.apl.android.functional.Consumer;
import com.amazon.apl.android.primitive.Gradient;
import com.amazon.apl.android.primitive.Rect;
import com.amazon.apl.android.providers.AbstractMediaPlayerProvider;
import com.amazon.apl.android.providers.ITelemetryProvider;
import com.amazon.apl.android.providers.impl.NoOpMediaPlayerProvider;
import com.amazon.apl.android.providers.impl.NoOpTelemetryProvider;
import com.amazon.apl.android.scaling.IMetricsTransform;
import com.amazon.apl.android.scaling.Scaling;
import com.amazon.apl.android.scaling.ViewportMetrics;
import com.amazon.apl.android.shadow.ShadowBitmapRenderer;
import com.amazon.apl.android.touch.Pointer;
import com.amazon.apl.android.touch.PointerTracker;
import com.amazon.apl.android.utils.KeyUtils;
import com.amazon.apl.android.utils.LazyImageLoader;
import com.amazon.apl.android.utils.SystraceUtils;
import com.amazon.apl.android.views.APLAbsoluteLayout;
import com.amazon.apl.android.views.APLImageView;
import com.amazon.apl.enums.FocusDirection;
import com.amazon.apl.enums.PropertyKey;
import com.amazon.apl.enums.ScreenShape;
import com.amazon.apl.enums.UpdateType;
import com.amazon.apl.enums.ViewportMode;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.engine.bitmap_recycle.LruBitmapPool;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.amazon.apl.android.providers.ITelemetryProvider.APL_DOMAIN;
import static com.amazon.apl.android.providers.ITelemetryProvider.Type.COUNTER;
import static com.amazon.apl.android.providers.ITelemetryProvider.Type.TIMER;

/**
 * A FrameLayout that is specified by a set of APL documents for layout, style, and data.
 * See {@link <a https://developer.amazon.com/docs/alexa-presentation-language/apl-overview.html> APL Overview</a>}
 * APL will instantiate a json based layout into this view.  Child components are created,
 * styled, properties are set, and then added to this ViewGroup.
 */
@UiThread
public class APLLayout extends FrameLayout implements AccessibilityManager.AccessibilityStateChangeListener {


    private static final String TAG = "APLLayout";
    private static final boolean DEBUG = BuildConfig.DEBUG;

    private static final int DOCUMENT_BACKGROUND_LAYER_ID = 1;

    // the apl root context
    @Nullable
    private RootContext mRootContext;

    // Latest Configuration Change
    @Nullable
    private ConfigurationChange mLatestConfigChange;

    // Layout metrics
    private static final String METRIC_LAYOUT = TAG + ".layout";
    private int tLayout;

    // Inflate metrics
    private static final String METRIC_VIEW_INFLATE = TAG + ".viewInflate";
    private int tViewInflate;

    // View metrics
    private static final String METRIC_VIEW_COUNT = TAG + ".viewCount";
    private int cViews;

    // Layout dirty state
    private final AtomicBoolean mViewsNeedLayout = new AtomicBoolean(false);
    private final AtomicBoolean mViewsNeedDisplay = new AtomicBoolean(false);

    // Map of Component unique ID to View in this layout.
    // Views are stored in a lookup map rather than referenced by the Component because some
    // Views are recycled.
    private final Map<String, View> mViews = new HashMap<>();

    // Maps views to the components that own them
    private final Map<View, Component> mComponents = new HashMap<>();

    // Theme.name() is used to sent String representation to core. Do not rename.
    enum Theme {
        light,
        dark
    }

    // Viewport settings read from XML Attributes.
    @NonNull
    private String mTheme = Theme.dark.toString();
    private int mDpi = 0;
    @NonNull
    private ViewportMode mMode = ViewportMode.kViewportModeHub;
    @NonNull
    private ScreenShape mShape = ScreenShape.RECTANGLE;
    @NonNull
    private Scaling mScaling = new Scaling();

    /**
     * Viewport pixel width
     */
    private ViewportMetrics mMetrics;

    private Content.DocumentBackground mDocumentBackground;

    // Gradient background bitmap
    @Nullable
    private Bitmap mGradientBitmap = null;

    /**
     * LayerDrawable to hold a the device specified background with the {@link com.amazon.apl.android.Content.DocumentBackground}
     * layered on top.
     */
    private LayerDrawable mBackgroundDrawable;
    /**
     * Resource defined device background color.
     *
     * This color should be opaque to prevent a fully transparent document from being rendered.
     */
    private int mDeviceBackgroundColor = Color.BLACK;

    private AccessibilityManager mAccessibilityManager;
    private boolean mIsAccessibilityActive = false;

    public APLLayout(@NonNull Context context) {
        this(context, true);
    }

    public APLLayout(@NonNull Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, true);
    }

    public APLLayout(@NonNull Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs, true);
    }

    @VisibleForTesting
    APLLayout(@NonNull Context context, boolean checkInitialization) {
        super(context);
        init(context, null, checkInitialization);
    }

    @VisibleForTesting
    APLLayout(@NonNull Context context, boolean checkInitialization, IAPLViewPresenter presenter) {
        super(context);
        mAplViewPresenter = presenter;
        init(context, null, checkInitialization);
    }

    /**
     * Initialize the layout.
     *
     * @param context The Android Context.
     * @param attrs   Style attributes, may be null.
     */
    private void init(@NonNull final Context context, @Nullable final AttributeSet attrs, boolean checkAPL) {
        // APLController isn't a dependency and we're checking this static method which makes testing this class very difficult.
        if (checkAPL && !APLController.isInitialized(mAplViewPresenter.telemetry())) {
            throw new IllegalStateException("The APLController must be initialized.");
        }

        setStaticTransformationsEnabled(true);
        setClipChildren(true);
        setClickable(true);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setDefaultFocusHighlightEnabled(false);
            setFocusable(FOCUSABLE);
        }

        mAccessibilityManager = (AccessibilityManager) getContext().getSystemService(Context.ACCESSIBILITY_SERVICE);
        setAccessibilityActive(mAccessibilityManager.isEnabled());

        // Styled properties.
        if (attrs == null)
            return; // no styles, exit method

        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs, R.styleable.APLLayout, 0, 0);

        try {
            int attr = R.styleable.APLLayout_aplTheme;
            if (a.hasValue(attr)) {
                int theme = a.getInteger(attr, 0);
                mTheme = Theme.values()[theme].toString();
            }

            // If shape is not specified, we can fallback into Android Resources configuration
            attr = R.styleable.APLLayout_isRound;
            if (a.hasValue(attr)) {
                boolean isRound = a.getBoolean(attr, false);
                mShape = isRound ? ScreenShape.ROUND : ScreenShape.RECTANGLE;
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mShape = context.getResources().getConfiguration().isScreenRound()
                        ? ScreenShape.ROUND
                        : ScreenShape.RECTANGLE;
            }

            attr = R.styleable.APLLayout_mode;
            if (a.hasValue(attr)) {
                int mode = a.getInteger(attr, 0);
                mMode = ViewportMode.values()[mode];
            }

            // If dpi is not specified, we can fallback into Display metrics
            attr = R.styleable.APLLayout_dpi;
            if (a.hasValue(attr)) {
                mDpi = a.getInteger(attr, 0);
            } else {
                mDpi = context.getResources().getDisplayMetrics().densityDpi;
            }

            attr = R.styleable.APLLayout_defaultBackground;
            if (a.hasValue(attr)) {
                mDeviceBackgroundColor = a.getColor(attr, mDeviceBackgroundColor);
            }

            mBackgroundDrawable = new LayerDrawable(new Drawable[]{
                    new ColorDrawable(mDeviceBackgroundColor),
                    new ColorDrawable(Color.TRANSPARENT)
            });
            mBackgroundDrawable.setId(1, DOCUMENT_BACKGROUND_LAYER_ID);
        } finally {
            a.recycle();
        }
    }

    /**
     * Overrides the dpi.
     * @param dpi dpi to override
     */
    public void overrideDpi(int dpi) {
        mDpi = dpi;
    }

    /**
     * Sets the scaling for this APLLayout.
     * @param scaling the scaling
     */
    public void setScaling(@NonNull Scaling scaling) {
        mScaling = scaling;
    }

    /**
     * Clear display state and views from the layout.
     */
    private void clear() {
        removeAllViews();
        mViews.clear();
        mComponents.clear();
    }


    /**
     * Request to render a document.
     *
     * @param rootContext The document context.
     */
    void onDocumentRender(@NonNull RootContext rootContext) {
        mRootContext = rootContext;
        if (shouldHandleAccessibility()) {
            mAccessibilityManager.addAccessibilityStateChangeListener(this);
        }

        if(mRootContext.getOptions().getBitmapCache() instanceof ComponentCallbacks2) {
            getContext().registerComponentCallbacks((ComponentCallbacks2) mRootContext.getOptions().getBitmapCache());
        }

        ITelemetryProvider telemetryProvider = mAplViewPresenter.telemetry();
        tLayout = telemetryProvider.createMetricId(
                ITelemetryProvider.APL_DOMAIN, METRIC_LAYOUT, TIMER);
        tViewInflate = telemetryProvider.createMetricId(
                ITelemetryProvider.APL_DOMAIN, METRIC_VIEW_INFLATE, TIMER);
        cViews = telemetryProvider.createMetricId(APL_DOMAIN, METRIC_VIEW_COUNT, COUNTER);

        // Update the view
        mViewsNeedLayout.set(true);
        postInvalidate();
        requestLayout();
    }

    /**
     * The RootContext is no longer valid.
     */
    private void onFinish() {
        if (shouldHandleAccessibility()) {
            mAccessibilityManager.removeAccessibilityStateChangeListener(this);
        }
        mRootContext.getOptions().getBitmapCache().clear();
        if (mRootContext.getOptions().getBitmapCache() instanceof ComponentCallbacks2) {
            getContext().unregisterComponentCallbacks((ComponentCallbacks2) mRootContext.getOptions().getBitmapCache());
        }
        mRootContext = null;
        mMetrics = null;
        setScaling(new Scaling());
        clear();
    }

    private boolean shouldHandleAccessibility() {
        return !(APLController.getRuntimeConfig() != null &&
                APLController.getRuntimeConfig().getAccessibilityHandledByRuntime());
    }

    @Override
    protected boolean getChildStaticTransformation(View child, @NonNull Transformation t) {
        Component component = mAplViewPresenter.findComponent(child);
        if (component != null) {
            t.getMatrix().set(component.getTransform());
            return true;
        }
        return false;
    }


    @Nullable
    @VisibleForTesting
    public RootContext getAPLContext() {
        return mRootContext;
    }


    @NonNull
    public IAPLViewPresenter getPresenter() {
        return mAplViewPresenter;
    }


    private ViewportMetrics createMetrics() {
        return ViewportMetrics.builder()
            .width(getMeasuredWidth())
            .height(getMeasuredHeight())
            .dpi(mDpi)
            .shape(mShape)
            .theme(mTheme)
            .mode(mMode)
            .scaling(mScaling)
            .build();
    }

    boolean validateMeasure = false;

    /**
     * Assign the default size of the Android view as the bounding box for the APL context.
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        validateMeasure = true;
    }


    /**
     * Listens to changes in APL RootContext and updates the view hierarchy.
     */
    @NonNull
    IAPLViewPresenter mAplViewPresenter = new IAPLViewPresenter() {

        // TODO extract this impl

        private PointerTracker mPointerTracker = new PointerTracker();
        @NonNull
        private ITelemetryProvider mTelemetryProvider = NoOpTelemetryProvider.getInstance();

        private int tRenderDocument = ITelemetryProvider.UNKNOWN_METRIC_ID;
        // start time of the Document Render "time to glass"
        private long mRenderStartTime;
        // Metric identifying the style of render (restore or new)
        private String mRenderMetric = ITelemetryProvider.RENDER_DOCUMENT;
        private ShadowBitmapRenderer mShadowRenderer;
        @NonNull
        private IBitmapFactory mBitmapFactory = BitmapFactory.create(NoOpTelemetryProvider.getInstance());
        private IBitmapPool mBitmapPool;
        private MotionEvent mLastMotionEvent;

        public AbstractMediaPlayerProvider getMediaPlayerProvider() {
            return mRootContext.getRenderingContext().getMediaPlayerProvider();
        }

        private final Set<IDocumentLifecycleListener> mDocumentLifecycleListeners = new HashSet<>();

        /*
         * {@inheritDoc}
         */
        @Override
        public Context getContext() {
            return APLLayout.this.getContext();
        }

        /*
         * {@inheritDoc}
         */
        @UiThread
        @Override
        public void onComponentChange(@NonNull final Component component, List<PropertyKey> dirtyProperties) {
            final View view = mViews.get(component.getComponentId());
            if (view != null) {
                ComponentViewAdapter adapter = ComponentViewAdapterFactory.getAdapter(component);
                adapter.refreshProperties(component, view, dirtyProperties);
            }
        }

        @Override
        public void applyAllProperties(Component component, View view) {
            if (view != null) {
                associateView(component, view);
                updateViewInLayout(component, view);
                ComponentViewAdapter adapter = ComponentViewAdapterFactory.getAdapter(component);
                adapter.applyAllProperties(component, view);
                view.invalidate();
            }
        }

        @Override
        public void requestLayout(Component component) {
            final View view = mViews.get(component.getComponentId());
            if (view != null) {
                ComponentViewAdapter adapter = ComponentViewAdapterFactory.getAdapter(component);
                adapter.requestLayout(component, view);
                view.invalidate();
            }
        }

        /*
         * {@inheritDoc}
         */
        @Nullable
        @UiThread
        @Override
        public View findView(Component component) {
            return mViews.get(component.getComponentId());
        }

        @Nullable
        @Override
        public Component findComponent(View view) {
            return mComponents.get(view);
        }


        @Override
        public float getDensity() {
            if (mMetrics == null) {
                if (DEBUG) {
                    throw new AssertionError("Requesting Density before the viewport is initialized");
                }
                return mDpi / 160f;
            }
            return mMetrics.density();
        }

        @Override
        @NonNull
        public ViewportMetrics createViewportMetrics() throws IllegalStateException {
            if (!validateMeasure) {
                throw new IllegalStateException("The view must be measured");
            }

            if (mMetrics == null) {
                mMetrics = createMetrics();
            }
            return mMetrics;
        }

        @Override
        @Nullable
        public ViewportMetrics getViewportMetrics() {
            return mMetrics;
        }

        @Override
        public boolean onKeyPress(@NonNull final KeyEvent event) {
            // convert Android KeyEvent to APL Keyboard object and notify the root context.
            APLKeyboard keyboard = KeyboardTranslator.getInstance().translate(event);

            if (mRootContext != null && keyboard != null) {
                return mRootContext.handleKeyboard(keyboard);
            }
            return false;
        }

        @Override
        public void preDocumentRender(boolean restore) {
            mRenderMetric = restore ? ITelemetryProvider.RESTORE_DOCUMENT : ITelemetryProvider.RENDER_DOCUMENT;
            mRenderStartTime = SystemClock.elapsedRealtime();
        }

        @Override
        public void onDocumentRender(RootContext rootContext) {
            // Init telemetry
            mTelemetryProvider = rootContext.getOptions().getTelemetryProvider();
            mBitmapFactory = rootContext.getRenderingContext().getBitmapFactory();
            mShadowRenderer = new ShadowBitmapRenderer(getBitmapFactory());

            int componentCount = rootContext.getComponentCount();
            int complexity = componentCount > 100 ? componentCount - (componentCount % 100) + 100 :
                    componentCount - (componentCount % 10) + 10;
            tRenderDocument = mTelemetryProvider.createMetricId(ITelemetryProvider.APL_DOMAIN,
                    mRenderMetric + "." + complexity, TIMER);

            long seedTime = SystemClock.elapsedRealtime() - mRenderStartTime;
            mTelemetryProvider.startTimer(tRenderDocument, TimeUnit.MILLISECONDS, seedTime);

            APLLayout.this.onDocumentRender(rootContext);
            for (IDocumentLifecycleListener documentLifecycleListener: mDocumentLifecycleListeners) {
                documentLifecycleListener.onDocumentRender(rootContext);
            }
        }

        @Override
        public void addDocumentLifecycleListener(IDocumentLifecycleListener documentLifecycleListener) {
            mDocumentLifecycleListeners.add(documentLifecycleListener);
        }

        @Override
        public void onDocumentPaused() {
            for (IDocumentLifecycleListener documentLifecycleListener : mDocumentLifecycleListeners) {
                documentLifecycleListener.onDocumentPaused();
            }
        }

        @Override
        public void onDocumentResumed() {
            for (IDocumentLifecycleListener documentLifecycleListener : mDocumentLifecycleListeners) {
                documentLifecycleListener.onDocumentResumed();
            }
        }

        @Override
        public void onDocumentDisplayed() {
            if (tRenderDocument != ITelemetryProvider.UNKNOWN_METRIC_ID) {
                mTelemetryProvider.stopTimer(tRenderDocument);
            }

            for (IDocumentLifecycleListener documentLifecycleListener : mDocumentLifecycleListeners) {
                documentLifecycleListener.onDocumentDisplayed();
            }
        }

        @Override
        public void associateView(Component component, View view) {
            String componentId = component.getComponentId();
            final Component oldComponent = mComponents.get(view);
            if (oldComponent != null) {
                disassociateView(oldComponent);
            }
            view.setId(componentId.hashCode());
            mViews.put(componentId, view);
            mComponents.put(view, component);
        }

        @Override
        public void disassociateView(Component component) {
            final String componentId = component.getComponentId();
            View view = mViews.get(componentId);
            if (view != null) {
                view.setId(0);
            }
            mViews.remove(componentId);
            mComponents.remove(view);
            // TODO we could consider Image component manages its own memory
            if (component instanceof Image && view instanceof APLImageView) {
                LazyImageLoader.clearImageResources(ImageViewAdapter.getInstance(), (Image) component, (APLImageView) view);
            }
        }

        @Override
        public void onDocumentFinish() {
            APLLayout.this.onFinish();
            tRenderDocument = ITelemetryProvider.UNKNOWN_METRIC_ID;
            mRenderMetric = null;
            mTelemetryProvider = NoOpTelemetryProvider.getInstance();
            if (mShadowRenderer != null) {
                mShadowRenderer.cleanUp();
                mShadowRenderer = null;
            }
            mBitmapPool = new GlideCachingBitmapPool(Runtime.getRuntime().maxMemory() / 32);
            mBitmapFactory = PooledBitmapFactory.create(mTelemetryProvider, mBitmapPool);

            for (IDocumentLifecycleListener documentLifecycleListener : mDocumentLifecycleListeners) {
                documentLifecycleListener.onDocumentFinish();
            }
            mDocumentLifecycleListeners.clear();
        }

        @Override
        public ITelemetryProvider telemetry() {
            return mTelemetryProvider;
        }

        @Override
        public ShadowBitmapRenderer getShadowRenderer() {
            return mShadowRenderer;
        }

        @Override
        public IBitmapFactory getBitmapFactory() {
            return mBitmapFactory;
        }

        @Override
        public View inflateComponentHierarchy(final Component root) {
            traverseComponentHierarchy(
                    root,
                    component -> {
                        ComponentViewAdapter viewAdapter = ComponentViewAdapterFactory.getAdapter(component);
                        final View view = viewAdapter.createView(getContext(), mAplViewPresenter);
                        mTelemetryProvider.incrementCount(cViews);
                        // TODO: ideally these two methods would be called by the caller of inflateComponentHierarchy
                        // TODO: but we'll keep them here for now to avoid have to iterate over every Component twice from APLLayout.onLayout
                        applyAllProperties(component, view);
                    });
            return mViews.get(root.getComponentId());
        }

        @Override
        public void traverseComponentHierarchy(
                final Component root,
                final Consumer<Component> visitorOperation) {
            APLLayout.traverseComponentHierarchy(root, visitorOperation);
        }

        /**
         * {@inheritDoc}
         * @param component the component
         * @param view the view
         */
        @Override
        public void updateViewInLayout(Component component, View view) {
            APLLayout.this.updateViewInLayout(component, view);
        }

        /**
         * Load background color or gradient to APLLayout
         */
        @UiThread
        @Override
        public void loadBackground(final Content.DocumentBackground bg) {
            mDocumentBackground = bg;
            if ((bg == null || !bg.isGradient()) && mGradientBitmap != null) {
                mBitmapFactory.disposeBitmap(mGradientBitmap);
            }
            if (bg == null) {
                mBackgroundDrawable.setDrawableByLayerId(DOCUMENT_BACKGROUND_LAYER_ID, new ColorDrawable(Color.TRANSPARENT));
                return;
            }
            if (bg.isGradient()) {
                try {
                    final int height = mMetrics.height();
                    final int width = mMetrics.width();
                    if (mGradientBitmap == null || mGradientBitmap.isRecycled()) {
                        mGradientBitmap = mBitmapFactory.createBitmap(width, height);
                    } else {
                        final int gbHeight = mGradientBitmap.getHeight();
                        final int gbWidth = mGradientBitmap.getWidth();
                        if (gbWidth == width && gbHeight == height) {
                            // erase existing
                            mGradientBitmap.eraseColor(Color.TRANSPARENT);
                        } else if (gbWidth > width && gbHeight > height) {
                            // reuse
                            mGradientBitmap = mBitmapFactory.createScaledBitmap(mGradientBitmap, width, height, true);
                            mGradientBitmap.eraseColor(Color.TRANSPARENT);
                        } else {
                            mGradientBitmap = mBitmapFactory.createBitmap(width, height);
                        }
                    }

                    BitmapDrawable bitmapDrawable = new BitmapDrawable(null, mGradientBitmap);
                    Canvas canvas = new Canvas(mGradientBitmap);
                    Shader shader = Gradient.createGradientShader(bg.getColorRange(), bg.getInputRange(),
                            bg.getType(), bg.getAngle(), height, width);

                    Paint paint = new Paint();
                    paint.setShader(shader);
                    RectF rec = new RectF(0, 0, width, height);
                    canvas.drawRect(rec, paint);
                    mBackgroundDrawable.setDrawableByLayerId(DOCUMENT_BACKGROUND_LAYER_ID, bitmapDrawable);
                } catch (BitmapCreationException e) {
                    if (DEBUG) {
                        Log.e(TAG, "Unable to create document background.", e);
                    }
                }
            } else if (bg.isColor()) {
                mBackgroundDrawable.setDrawableByLayerId(DOCUMENT_BACKGROUND_LAYER_ID, new ColorDrawable(bg.getColor()));
            } else {
                Log.e(TAG, "document background is not color or gradient");
            }
            setBackground(mBackgroundDrawable);
        }

        @Override
        public boolean handleTouchEvent(@NonNull final MotionEvent event) {
            mLastMotionEvent = event;
            final Pointer pointer = mPointerTracker.trackAndGetPointer(event);
            if (mRootContext != null && pointer != null) {
                IMetricsTransform metricsTransform = mRootContext.getMetricsTransform();
                // apply negative offsets
                int xOffset = - metricsTransform.getViewportOffsetX();
                int yOffset = - metricsTransform.getViewportOffsetY();
                pointer.translate(xOffset, yOffset);
                return mRootContext.handlePointer(pointer);
            }
            return false;
        }

        @Override
        public void onClick(View view) {
            if (mIsAccessibilityActive) {
                updateComponent(view, UpdateType.kUpdatePressState, true);
                updateComponent(view, UpdateType.kUpdatePressed, true);
            }
        }

        @Override
        public void cancelTouchEvent() {
            final Pointer cancelPointer = mPointerTracker.cancelPointer();
            if (mRootContext != null && cancelPointer != null) {
                mRootContext.handlePointer(cancelPointer);
            }
        }

        @Override
        public void updateComponent(View componentView, UpdateType updateType, int value) {
            Component component = findComponent(componentView);
            if(component != null) {
                component.update(updateType, value);
            }
        }

        @Override
        public void updateComponent(View componentView, UpdateType updateType, boolean value) {
            Component component = findComponent(componentView);
            if(component != null) {
                component.update(updateType, value);
            }
        }

        @Override
        public void updateComponent(View componentView, UpdateType updateType, String value) {
            Component component = findComponent(componentView);
            if(component != null) {
                component.update(updateType, value);
            }
        }

        @Override
        public long getElapsedTime() {
            return mRootContext != null ? mRootContext.getElapsedTime() : 0;
        }

        @Override
        public ConfigurationChange getConfigurationChange() {
            return mLatestConfigChange;
        }

        @Override
        public void setScaling(Scaling scaling) {
            APLLayout.this.setScaling(scaling);
        }

        @Override
        public void clearLayout() {
            mMetrics = null;
            APLLayout.this.clear();
        }

        @Override
        public void releaseLastMotionEvent() {
            if (mLastMotionEvent != null) {
                // Call super and bypass Core
                APLLayout.super.dispatchTouchEvent(mLastMotionEvent);
                mLastMotionEvent = null;
            }
        }
    };


    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        ITelemetryProvider telemetry = mAplViewPresenter.telemetry();
        try {

            if (telemetry != null) {
                telemetry.startTimer(tLayout);
            }
            if (mViewsNeedLayout.getAndSet(false) && mRootContext != null) {

                if (DEBUG) {
                    SystraceUtils.startTrace(TAG, "onLayout");
                }
                if (telemetry != null) {
                    telemetry.startTimer(tViewInflate);
                }

                // Remove previous layout
                clear();

                Component topComponent = mRootContext.getTopComponent();
                mAplViewPresenter.inflateComponentHierarchy(topComponent);

                if (telemetry != null) {
                    telemetry.stopTimer(tViewInflate);
                }

                // kick off the timer
                mRootContext.initTime();

                if (DEBUG) {
                    SystraceUtils.endTrace();
                }

                // signal that next dispatchDraw is for new Views
                mViewsNeedDisplay.set(true);
            }

            // Layout the components
            final int count = getChildCount();
            if (DEBUG) {
                SystraceUtils.startTrace(TAG, "onLayout.for");
            }
            for (int i = 0; i < count; i++) {
                final View child = getChildAt(i);
                if (child.getVisibility() != GONE) {
                    // Place the child.
                    IMetricsTransform transform = mRootContext.getMetricsTransform();
                    final FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) child.getLayoutParams();
                    int left = lp.leftMargin + transform.getViewportOffsetX();
                    int top = lp.rightMargin + transform.getViewportOffsetY();

                    child.measure(MeasureSpec.makeMeasureSpec(lp.width, MeasureSpec.EXACTLY),
                            MeasureSpec.makeMeasureSpec(lp.height, MeasureSpec.EXACTLY));
                    child.layout(left, top, left + lp.width, top + lp.height);
                }
            }

            if (telemetry != null) {
                telemetry.stopTimer(tLayout);
            }
        } catch (Exception ex) {
            if (telemetry != null) {
                telemetry.fail(tLayout);
            }
            throw (ex);
        } finally {
            if (DEBUG) {
                SystraceUtils.endTrace();
            }

        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        try {
            super.dispatchDraw(canvas);
        } finally {
            if (mViewsNeedDisplay.getAndSet(false)) {
                mAplViewPresenter.onDocumentDisplayed();
            }
        }
    }

    @NonNull
    @Override
    protected FrameLayout.LayoutParams generateDefaultLayoutParams() {
        throw new UnsupportedOperationException("Layout Params can only be derived from an APL document");
    }

    @NonNull
    @Override
    public FrameLayout.LayoutParams generateLayoutParams(AttributeSet attrs) {
        throw new UnsupportedOperationException("Layout Params can only be derived from an APL document");
    }

    // Override to allow type-checking of LayoutParams.
    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof APLLayoutParams;
    }

    @NonNull
    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        throw new UnsupportedOperationException("Layout Params can only be derived from an APL document");
    }

    @SuppressWarnings("SameReturnValue")
    @Override
    public boolean shouldDelayChildPressedState() {
        return false;
    }


    /**
     * Create layout parameters for an APL component, and adds the view to the parent/layout.
     *
     * @param component the apl component.
     */
    @UiThread
    private void updateViewInLayout(@NonNull Component component, @NonNull View view) {
        Rect bounds = component.getBounds();
        int w = (int) bounds.getWidth();
        int h = (int) bounds.getHeight();
        int l = bounds.intLeft();
        int t = bounds.intTop();

        // Get the parent to determine correct layout params
        String parentId = component.getParentId();
        View parentView = null;
        if (parentId != null && !parentId.isEmpty()) {
            @SuppressWarnings("ConstantConditions") Component pComp = mRootContext.getComponent(parentId);
            if (pComp == null) {
                // parent isn't available yet defer
                return;
            }
            parentView = mViews.get(parentId);
        }

        if (parentView == null) { // this is likely a child of APLLayout
            // add to the root view
            APLLayoutParams params = new APLLayoutParams(w, h, l, t);
            if (view.getParent() == null) {
                // use addViewInLayout() rather than addView because this call is triggered by aplDriver.nCreate
                // which was called in OnLayout()
                addViewInLayout(view, -1, params, true);
            } else {
                view.setLayoutParams(params);
            }
        } else if (parentView instanceof APLAbsoluteLayout) {
            APLAbsoluteLayout.LayoutParams params = new APLAbsoluteLayout.LayoutParams(w, h, l, t);
            view.setLayoutParams(params);
            if (view.getParent() == null) {
                // add to view parent if it is not already assigned
                ((APLAbsoluteLayout) parentView).addViewInLayout(view, params);
            }
        } else {
            // update the layout or parent
            ViewGroup.MarginLayoutParams params = null;
            if (parentView instanceof FrameLayout) {
                params = new FrameLayout.LayoutParams(w, h);
            } else if (parentView instanceof RelativeLayout) {
                params = new RelativeLayout.LayoutParams(w, h);
            }

            if (params == null) {
                if (DEBUG)
                    throw new AssertionError("Failed to create layout parameters");

                Log.w(TAG, "Failed to create layout parameters. Ignore setting them to the view.");
            } else {
                params.leftMargin = l;
                params.topMargin = t;
                view.setLayoutParams(params);
            }

            if (view.getParent() == null) {
                // add to view parent if it is not assigned
                ((ViewGroup) parentView).addView(view);
            }
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (mRootContext != null && event.getAction() == KeyEvent.ACTION_DOWN) {
            if (KeyUtils.isKeyEventDirectional(event) &&
                    mRootContext.getFocusedComponentId().isEmpty()) {
                mRootContext.nextFocus(FocusDirection.kFocusDirectionRight);
            }
            return super.dispatchKeyEvent(event) || mAplViewPresenter.onKeyPress(event);
        }
        return mAplViewPresenter.onKeyPress(event) || super.dispatchKeyEvent(event);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        boolean coreHandled = mAplViewPresenter.handleTouchEvent(event);
        if (coreHandled) {
            if (event.getAction() != MotionEvent.ACTION_UP) {
                cancelPendingInputEvents();
            }
            return true;
        } else {
            return super.dispatchTouchEvent(event);
        }
    }

    /**
     * Create a {@link ConfigurationChange} object. This queries the current viewport and rootconfig parameters from Core.
     * @return - {@link ConfigurationChange}
     */
    @NonNull
    @UiThread
    public ConfigurationChange.Builder createConfigurationChange() {
        if (mRootContext == null) {
            throw new IllegalStateException("Cannot create configurationChange since document is not rendered yet");
        }
        return mRootContext.createConfigurationChange();
    }

    /**
     * Notify Viewhost of a Configuration change detected in Runtime.
     * @param configurationChange
     * @throws APLController.APLException Thrown when the document cannot be rendered.
     */
    @UiThread
    public void handleConfigurationChange(ConfigurationChange configurationChange) throws APLController.APLException {
        if (configurationChange.screenReaderEnabled() != mAccessibilityManager.isEnabled()) {
            handleAccessibilityStateChange(mAccessibilityManager.isEnabled());
        }
        if (mRootContext != null) {
            mRootContext.handleConfigurationChange(configurationChange);
        }
        // store latest configuration change to be used for restoring backstack document
        mLatestConfigChange = configurationChange;

        final Component topComponent = mRootContext.getTopComponent();
        if (topComponent == null) {
            throw new APLController.APLException("Document cannot be rendered in this configuration", new IllegalStateException());
        } else {
            mAplViewPresenter.traverseComponentHierarchy(topComponent, mAplViewPresenter::requestLayout);
        }
    }


    /**
     * Get mDocumentBackground, which could either indicate current background or previous
     * background, so that the runtime can take care of between-documents background behavior.
     *
     * @return mDocumentBackground
     */
    public Content.DocumentBackground getDocumentBackground() {
        return mDocumentBackground;
    }

    @Override
    public void onAccessibilityStateChanged(boolean enabled) {
        handleAccessibilityStateChange(enabled);
    }

    private void handleAccessibilityStateChange(boolean enabled) {
        setAccessibilityActive(enabled);

        for (View view : mViews.values()) {
            Component component = mAplViewPresenter.findComponent(view);
            ComponentViewAdapter componentViewAdapter = ComponentViewAdapterFactory.getAdapter(component);
            componentViewAdapter.applyFocusability(component, view);
        }
    }

    /**
     * Convenience method for visiting all Components in a given hierarchy via a Breadth-First Search.
     *
     * @param root The root of the hierarchy to traverse.
     * @param visitorOperation A Consumer function that will be called once for each Component reached
     *                         during traversal of the hierarchy. This function takes a single
     *                         Component as input
     */
    public static void traverseComponentHierarchy(
            final Component root,
            final Consumer<Component> visitorOperation) {
        // We'll use a Breadth-First Search Queue to traverse all the Components
        final Queue<Component> queue = new LinkedList<>();
        Component component = root;
        queue.add(component);
        while (!queue.isEmpty()) {
            // Create the view
            component = queue.poll();
            visitorOperation.accept(component);
            // Add the children to the queue
            queue.addAll(component.getDisplayedChildren());
        }
    }

    @VisibleForTesting
    public void setAccessibilityActive(boolean enabled) {
        mIsAccessibilityActive = enabled;
    }
}


