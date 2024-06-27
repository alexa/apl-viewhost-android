/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.os.Build;
import android.os.Looper;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.accessibility.AccessibilityManager;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.annotation.VisibleForTesting;

import com.amazon.apl.android.bitmap.IBitmapFactory;
import com.amazon.apl.android.bitmap.ShadowCache;
import com.amazon.apl.android.component.ComponentViewAdapter;
import com.amazon.apl.android.component.ComponentViewAdapterFactory;
import com.amazon.apl.android.component.ImageViewAdapter;
import com.amazon.apl.android.configuration.ConfigurationChange;
import com.amazon.apl.android.functional.Consumer;
import com.amazon.apl.android.graphic.GraphicContainerElement;
import com.amazon.apl.android.metrics.ICounter;
import com.amazon.apl.android.metrics.IMetricsRecorder;
import com.amazon.apl.android.metrics.MetricsMilestoneConstants;
import com.amazon.apl.android.primitive.Gradient;
import com.amazon.apl.android.primitive.Rect;
import com.amazon.apl.android.providers.AbstractMediaPlayerProvider;
import com.amazon.apl.android.providers.ITelemetryProvider;
import com.amazon.apl.android.providers.impl.NoOpTelemetryProvider;
import com.amazon.apl.android.scaling.IMetricsTransform;
import com.amazon.apl.android.scaling.Scaling;
import com.amazon.apl.android.scaling.ViewportMetrics;
import com.amazon.apl.android.scenegraph.APLLayer;
import com.amazon.apl.android.scenegraph.APLScenegraph;
import com.amazon.apl.android.shadow.ShadowBitmapRenderer;
import com.amazon.apl.android.touch.Pointer;
import com.amazon.apl.android.touch.PointerTracker;
import com.amazon.apl.android.utils.APLTrace;
import com.amazon.apl.android.utils.KeyUtils;
import com.amazon.apl.android.utils.LazyImageLoader;
import com.amazon.apl.android.utils.TracePoint;
import com.amazon.apl.android.utils.TransformUtils;
import com.amazon.apl.android.views.APLAbsoluteLayout;
import com.amazon.apl.android.views.APLImageView;
import com.amazon.apl.android.views.APLRootView;
import com.amazon.apl.android.views.APLView;
import com.amazon.apl.devtools.DevToolsProvider;
import com.amazon.apl.devtools.enums.DTError;
import com.amazon.apl.devtools.enums.ViewState;
import com.amazon.apl.devtools.models.ViewTypeTarget;
import com.amazon.apl.devtools.util.IDTCallback;
import com.amazon.apl.devtools.util.RequestStatus;
import com.amazon.apl.devtools.views.IAPLView;
import com.amazon.apl.enums.ComponentType;
import com.amazon.apl.enums.FocusDirection;
import com.amazon.apl.enums.PropertyKey;
import com.amazon.apl.enums.RootProperty;
import com.amazon.apl.enums.ScreenShape;
import com.amazon.apl.enums.UpdateType;
import com.amazon.apl.enums.ViewportMode;
import com.amazon.apl.viewhost.internal.DocumentState;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.amazon.apl.android.providers.ITelemetryProvider.APL_DOMAIN;
import static com.amazon.apl.android.providers.ITelemetryProvider.Type.COUNTER;
import static com.amazon.apl.android.providers.ITelemetryProvider.Type.TIMER;
import static com.amazon.apl.devtools.enums.CommandMethod.DOCUMENT_HIDE_HIGHLIGHT;
import static com.amazon.apl.devtools.enums.CommandMethod.DOCUMENT_HIGHLIGHT_COMPONENT;

/**
 * A FrameLayout that is specified by a set of APL documents for layout, style, and data.
 * See {@link <a https://developer.amazon.com/docs/alexa-presentation-language/apl-overview.html> APL Overview</a>}
 * APL will instantiate a json based layout into this view.  Child components are created,
 * styled, properties are set, and then added to this ViewGroup.
 */
@UiThread
public class APLLayout extends FrameLayout implements AccessibilityManager.AccessibilityStateChangeListener, IAPLView {
    private static final String TAG = "APLLayout";
    private static final boolean DEBUG = BuildConfig.DEBUG;

    private static final int DOCUMENT_BACKGROUND_LAYER_ID = 1;

    // the apl root context
    @Nullable
    private RootContext mRootContext;

    // The apl dev tools
    private ViewTypeTarget mDTView;
    private DevToolsProvider mDevToolsProvider;
    private Session mAPLSession;

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
    private ICounter mViewsCounter;
    private long cViewsBatchedIncrementCount;

    private static final String METRIC_INFLATE_BEFORE_FINISH_ERROR = TAG + ".inflateBeforeFinish" + ITelemetryProvider.FAIL_SUFFIX;

    // Layout dirty state
    private final AtomicBoolean mViewsNeedLayout = new AtomicBoolean(false);
    private final AtomicBoolean mViewsNeedDisplay = new AtomicBoolean(false);

    // Map of Component unique ID to View in this layout.
    // Views are stored in a lookup map rather than referenced by the Component because some
    // Views are recycled.
    private final Map<String, View> mViews = new HashMap<>();

    // Maps views to the components that own them
    private final Map<View, Component> mComponents = new HashMap<>();
    private boolean mSimulateMeasureAndLayout;

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
    private int mMinHeight;
    private int mMaxHeight;
    private int mMinWidth;
    private int mMaxWidth;

    /**
     * Viewport pixel width
     */
    private ViewportMetrics mMetrics;

    private Content.DocumentBackground mDocumentBackground;

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
    private boolean mHandleConfigurationChangeOnSizeChanged = false;

    /**
     * Trace points for a given agent name.
     */
    private final APLTrace mAplTrace = new APLTrace(APL_DOMAIN);

    private boolean mIsReinflating = false;

    /**
     * Used for highlighting a document component.
     */
    private final Path mPath = new Path();

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
     * To be used only for testing.
     * @param presenter
     */
    public void setAplViewPresenterForTesting(IAPLViewPresenter presenter) {
        mAplViewPresenter = presenter;
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

        setClipChildren(true);
        setClickable(true);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        setOnHierarchyChangeListener(mAplViewPresenter);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setDefaultFocusHighlightEnabled(false);
            setFocusable(FOCUSABLE);
        }

        setAccessibilityManager((AccessibilityManager) getContext().getSystemService(Context.ACCESSIBILITY_SERVICE));

        // Set default density and shape to match the screen
        mDpi = context.getResources().getDisplayMetrics().densityDpi;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mShape = context.getResources().getConfiguration().isScreenRound()
                    ? ScreenShape.ROUND
                    : ScreenShape.RECTANGLE;
        }

        // Styled properties.
        if (attrs != null) {
            TypedArray a = context.getTheme().obtainStyledAttributes(
                    attrs, R.styleable.APLLayout, 0, 0);

            try {
                int attr = R.styleable.APLLayout_aplTheme;
                if (a.hasValue(attr)) {
                    int theme = a.getInteger(attr, 0);
                    mTheme = Theme.values()[theme].toString();
                }

                attr = R.styleable.APLLayout_isRound;
                if (a.hasValue(attr)) {
                    mShape = a.getBoolean(attr, mShape == ScreenShape.ROUND)
                            ? ScreenShape.ROUND
                            : ScreenShape.RECTANGLE;
                }

                attr = R.styleable.APLLayout_mode;
                if (a.hasValue(attr)) {
                    int mode = a.getInteger(attr, 0);
                    mMode = ViewportMode.values()[mode];
                }

                attr = R.styleable.APLLayout_dpi;
                if (a.hasValue(attr)) {
                    mDpi = a.getInteger(attr, mDpi);
                }

                attr = R.styleable.APLLayout_defaultBackground;
                if (a.hasValue(attr)) {
                    mDeviceBackgroundColor = a.getColor(attr, mDeviceBackgroundColor);
                }
            } finally {
                a.recycle();
            }
        }

        mBackgroundDrawable = new LayerDrawable(new Drawable[]{
                new ColorDrawable(mDeviceBackgroundColor),
                new ColorDrawable(Color.TRANSPARENT)
        });
        mBackgroundDrawable.setId(1, DOCUMENT_BACKGROUND_LAYER_ID);
        mSimulateMeasureAndLayout = APLController.getRuntimeConfig().isSimulateMeasureAndLayoutEnabled();
        initializeDeveloperTools();
    }

    private void initializeDeveloperTools() {
        mDevToolsProvider = new DevToolsProvider();
        mDTView = mDevToolsProvider.getDTView();
        mDTView.setView(this);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        registerAccessibilityListener();
        mDTView.registerToCatalog();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mAccessibilityManager.removeAccessibilityStateChangeListener(this);
        mDTView.unregisterToCatalog();
    }

    @VisibleForTesting
    void setAccessibilityManager(AccessibilityManager accessibilityManager) {
        mAccessibilityManager = accessibilityManager;
    }

    private void registerAccessibilityListener() {
        onAccessibilityStateChanged(mAccessibilityManager.isEnabled());
        mAccessibilityManager.addAccessibilityStateChangeListener(this);
    }

    @Override
    public void startFrameMetricsRecording(int id, IDTCallback<String> callback) {
        if (mRootContext == null) {
            DTError error = DTError.NO_DOCUMENT_RENDERED;
            mDTView.post(() -> callback.execute(RequestStatus.failed(id, error)));
            return;
        }

        mRootContext.startFrameMetricsRecording();
        mDTView.post(() -> callback.execute(RequestStatus.successful()));
    }

    @Override
    public void stopFrameMetricsRecording(int id, IDTCallback<List<JSONObject>> callback) {
        if (mRootContext == null) {
            Log.w(TAG, "No root context: ");
            callback.execute(RequestStatus.failed(id, DTError.NO_DOCUMENT_RENDERED));
            return;
        }

        try {
            List<JSONObject> frameMetrics = mRootContext.stopFrameMetricsRecording();
            mDTView.post(() -> callback.execute(frameMetrics, RequestStatus.successful()));
        } catch (JSONException e) {
            mDTView.post(() -> callback.execute(RequestStatus.failed(id, DTError.METHOD_FAILURE)));
        }
    }

    @Override
    public void documentCommandRequest(int id, String method, JSONObject params, IDTCallback<String> callback) {
        if (mRootContext == null) {
            Log.w(TAG, "No root context: " + method);
            mDTView.post(() -> callback.execute(RequestStatus.failed(id, DTError.NO_DOCUMENT_RENDERED)));
            return;
        }

        if (method.equals(DOCUMENT_HIGHLIGHT_COMPONENT.toString())) {
            try {
                String componentId = params.getString("componentId");
                if (!componentId.isEmpty()) {
                    Component component = mRootContext.getOrInflateComponentWithUniqueId(componentId);
                    if (component != null) {
                        drawPathFromPoints(component.getPathCoordinates());
                    }
                } else {
                    clearDrawing();
                }
                // Invalidates the current view to update the highlight drawings.
                invalidate();
                mDTView.post(() -> callback.execute(RequestStatus.successful()));
            } catch (JSONException e) {
                Log.e(TAG, "Could not parse json for params: " + params);
                mDTView.post(() -> callback.execute(RequestStatus.failed(id, DTError.METHOD_FAILURE)));
            }
        } else if (method.equals(DOCUMENT_HIDE_HIGHLIGHT.toString())) {
            clearDrawing();
            invalidate();
            mDTView.post(() -> callback.execute(RequestStatus.successful()));
        } else {
            String result = mRootContext.documentCommandRequest(method, params.toString());
            mDTView.post(() -> callback.execute(result, RequestStatus.successful()));
        }
    }

    private void drawPathFromPoints(ArrayList<float[]> componentPoints) {
        IMetricsTransform metricsTransform = mRootContext.getMetricsTransform();

        float[] p1 = componentPoints.get(0);
        float[] p2 = componentPoints.get(1);
        float[] p3 = componentPoints.get(2);
        float[] p4 = componentPoints.get(3);

        // Create a path from 4 points.
        mPath.moveTo(metricsTransform.toViewhost(p1[0]),
                metricsTransform.toViewhost(p1[1]));
        mPath.lineTo(metricsTransform.toViewhost((p2[0])),
                metricsTransform.toViewhost(p2[1]));
        mPath.lineTo(metricsTransform.toViewhost(p3[0]),
                metricsTransform.toViewhost(p3[1]));
        mPath.lineTo(metricsTransform.toViewhost(p4[0]),
                metricsTransform.toViewhost(p4[1]));
        mPath.lineTo(metricsTransform.toViewhost(p1[0]),
                metricsTransform.toViewhost(p1[1]));
        mPath.close();
    }

    private void clearDrawing() {
        mPath.reset();
    }

    /**
     * @return the {@link DevToolsProvider} instance corresponding to the view.
     */
    public DevToolsProvider getDevToolsProvider() {
        return mDevToolsProvider;
    }

    /**
     * Sets the APLSession.
     * @param aplSession The {@link Session} associated with the document being rendered.
     */
    public void setAPLSession(Session aplSession) {
        mAPLSession = aplSession;
    }

    private void writeAPLSessionInfoLog(String message) {
        if (mAPLSession != null) {
            mAPLSession.write(Session.LogEntryLevel.INFO, Session.LogEntrySource.VIEW, message);
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
     * Sets Min Max Range
     * @param minWidth min width
     * @param maxWidth max width
     */
    public void setMinMaxWidth(int minWidth, int maxWidth) {
        mMinWidth = minWidth;
        mMaxWidth = maxWidth;
    }

    /**
     * Sets min max Range
     * @param minHeight min height
     * @param maxHeight max height
     */
    public void setMinMaxHeight(int minHeight, int maxHeight) {
        mMinHeight = minHeight;
        mMaxHeight = maxHeight;
    }

    /**
     * Sets the scaling for this APLLayout based on viewport specifications supplied.
     * @param biasConstant
     * @param viewportSpecifications
     * @param viewportModes
     */
    public void setScaling(final float biasConstant, final List<Scaling.ViewportSpecification> viewportSpecifications, final List<ViewportMode> viewportModes) {
        mScaling = viewportSpecifications.isEmpty() ? new Scaling() : new Scaling(biasConstant, viewportSpecifications, viewportModes);
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
        ITelemetryProvider telemetryProvider = mAplViewPresenter.telemetry();
        tLayout = telemetryProvider.createMetricId(
                ITelemetryProvider.APL_DOMAIN, METRIC_LAYOUT, TIMER);
        tViewInflate = telemetryProvider.createMetricId(
                ITelemetryProvider.APL_DOMAIN, METRIC_VIEW_INFLATE, TIMER);
        cViews = telemetryProvider.createMetricId(APL_DOMAIN, METRIC_VIEW_COUNT, COUNTER);
        mViewsCounter = mAplViewPresenter.metricsRecorder().createCounter(APL_DOMAIN + "." + METRIC_VIEW_COUNT);

        // Update the view
        inflateViews();
        mDTView.setAPLOptions(mRootContext.getOptions());
    }

    private final Runnable measureAndLayout = new Runnable() {
        @Override
        public void run() {
            measure(
                    MeasureSpec.makeMeasureSpec(getWidth(), MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(getHeight(), MeasureSpec.EXACTLY));
            layout(getLeft(), getTop(), getRight(), getBottom());
        }
    };

    @Override
    public void requestLayout() {
        super.requestLayout();

        // The APLLayout relies on a measure + layout pass happening after it calls requestLayout().
        // Without this, the widget never actually changes the selection and doesn't call the
        // appropriate listeners. Certain frameworks overrides onLayout in its own ViewGroups, a layout pass never
        // happens after a call to requestLayout, so we simulate one here.
        if(mSimulateMeasureAndLayout)
            post(measureAndLayout);
    }

    void inflateViews() {
        mViewsNeedLayout.set(true);
        if (mRootContext.isAutoSizeLayoutPending()) {
            resize();
        }
        if (Looper.myLooper() == Looper.getMainLooper() && !mRootContext.isAutoSizeLayoutPending()) {
            invalidate();
            requestLayout();
        } else {
            postInvalidate();
            post(this::requestLayout);
        }
    }

    /**
     * When autosize enabled, we resize the aplLayout
     */
    private void resize() {
        Log.i(TAG, "View resized");
        ViewGroup.LayoutParams params = getLayoutParams();
        params.height = mRootContext.getAutoSizedHeight();
        params.width = mRootContext.getAutoSizedWidth();
        this.setLayoutParams(params);
        writeAPLSessionInfoLog("Resizing to size= " + params.width + "x" + params.height);
    }

    /**
     * The RootContext is no longer valid.
     */
    private void onFinish() {
        if(mRootContext != null) {
            mRootContext = null;
        }
        mMetrics = null;
        setScaling(new Scaling());
        if (APLController.getRuntimeConfig().isClearViewsOnFinish()) {
            clear();
        }
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

    private Queue<Consumer<ViewportMetrics>> mMetricsConsumers = new LinkedList<>();

    /**
     * Add a callback for when metrics are ready.
     * @param metricsConsumer   function to execute when the view is measured
     */
    public void addMetricsReadyListener(Consumer<ViewportMetrics> metricsConsumer) {
        if (mIsMeasureValid && !isLayoutRequested()) {
            if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
                metricsConsumer.accept(mAplViewPresenter.getOrCreateViewportMetrics());
            } else {
                post(() -> metricsConsumer.accept(mAplViewPresenter.getOrCreateViewportMetrics()));
            }
        } else {
            mMetricsConsumers.add(metricsConsumer);
        }
    }

    private ViewportMetrics createMetrics() {
        int width = getMeasuredWidth();
        int height = getMeasuredHeight();
        ViewportMetrics.Builder  builder = ViewportMetrics.builder()
                .width(width)
                .height(height)
                .dpi(mDpi)
                .shape(mShape)
                .theme(mTheme)
                .mode(mMode)
                .scaling(mScaling);
        if (mMinWidth <= width && mMaxWidth >= width) {
            builder.minWidth(mMinWidth);
            builder.maxWidth(mMaxWidth);
        }

        if (mMinHeight <= height && mMaxHeight >= height) {
            builder.minHeight(mMinHeight);
            builder.maxHeight(mMaxHeight);
        }

        return builder.build();
    }

    /**
     * Tracks whether we are waiting for the first layout pass
     */
    boolean mIsMeasureValid = false;

    /**
     * Method to update the DTView with the latest {@link ViewState} change.
     *
     * @param viewState The new {@link ViewState}.
     */
    public void changeViewState(ViewState viewState) {
        double currentTimeStamp = System.currentTimeMillis() / 1000D;
        mDTView.post(() -> mDTView.onViewStateChange(viewState, currentTimeStamp));
    }

    /**
     * Listens to changes in APL RootContext and updates the view hierarchy.
     */
    @NonNull
    IAPLViewPresenter mAplViewPresenter = new IAPLViewPresenter() {

        // TODO extract this impl

        private final PointerTracker mPointerTracker = new PointerTracker();
        @NonNull
        private ITelemetryProvider mTelemetryProvider = NoOpTelemetryProvider.getInstance();
        @NonNull
        private IMetricsRecorder mMetricsRecorder;

        // RenderDocument in this context means time after Content and Extensions have been prepared and
        // we intend to inflate the RootContext
        private int tRenderDocument = ITelemetryProvider.UNKNOWN_METRIC_ID;
        // start time of the Document Render "time to glass"
        private long mRenderStartTime;
        private boolean mIsRenderStartTimeSet = false;

        private ShadowBitmapRenderer mShadowRenderer;
        @NonNull
        private IBitmapFactory mBitmapFactory;
        private MotionEvent mLastMotionEvent;

        public AbstractMediaPlayerProvider getMediaPlayerProvider() {
            return mRootContext.getRenderingContext().getMediaPlayerProvider();
        }

        /*
         * {@inheritDoc}
         */
        @Override
        public void mediaLoaded(String url) {
            post(() -> {
                if (mRootContext != null) {
                    mRootContext.mediaLoaded(url);
                }
            });
        }

        /*
         * {@inheritDoc}
         */
        @Override
        public void mediaLoadFailed(String url, int errorCode, String errorMessage) {
            post(() -> {
                if (mRootContext != null) {
                    mRootContext.mediaLoadFailed(url, errorCode, errorMessage);
                }
            });
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
                if (adapter == null) {
                    Log.e(TAG, "adapter is null");
                    return;
                }
                adapter.refreshProperties(component, view, dirtyProperties);
            } else if (component.getComponentType() == ComponentType.kComponentTypeVectorGraphic) {
                // The vector graphic is cached, so even if the view isn't available (not inflated), we need to notify the
                // vector graphic that it is dirty.
                final VectorGraphic vcComponent = (VectorGraphic)component;
                final GraphicContainerElement graphicContainerElement = vcComponent.getOrCreateGraphicContainerElement();
                if (graphicContainerElement != null) {
                    graphicContainerElement.applyDirtyProperties(vcComponent.getDirtyGraphics());
                }
            }
        }

        @Override
        public void applyAllProperties(Component component, View view) {
            if (view != null) {
                associate(component, view);
                updateViewInLayout(component, view);
                ComponentViewAdapter adapter = ComponentViewAdapterFactory.getAdapter(component);
                if (adapter == null) {
                    Log.e(TAG, "adapter is null");
                    return;
                }

                // Since the ImageViewAdapter is a singleton. And we can create multiple APLLayout,
                // then we need to update the DTNetworkRequestHandler before loading the image to
                // know from which View is coming from.
                if (adapter instanceof ImageViewAdapter) {
                    ((ImageViewAdapter) adapter).setDTNetworkRequestHandler(mDevToolsProvider.getNetworkRequestHandler());
                }
                adapter.applyAllProperties(component, view);
                view.invalidate();
            }
        }

        @Override
        public void requestLayout(Component component) {
            final View view = mViews.get(component.getComponentId());
            if (view != null) {
                ComponentViewAdapter adapter = ComponentViewAdapterFactory.getAdapter(component);
                if (adapter == null) {
                    Log.e(TAG, "adapter is null");
                    return;
                }
                // Since the ImageViewAdapter is a singleton. And we can create multiple APLLayout,
                // then we need to update the DTNetworkRequestHandler before loading the image to
                // know from which View is coming from.
                if (adapter instanceof ImageViewAdapter) {
                    ((ImageViewAdapter) adapter).setDTNetworkRequestHandler(mDevToolsProvider.getNetworkRequestHandler());
                }
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
        @NonNull
        public ViewportMetrics getOrCreateViewportMetrics() throws IllegalStateException {
            if (!mIsMeasureValid) {
                throw new IllegalStateException("The view must be measured");
            }

            if (mMetrics == null) {
                mMetrics = createMetrics();
            }
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
        public void preDocumentRender() {
            mIsRenderStartTimeSet = true;
            mRenderStartTime = SystemClock.elapsedRealtime();
            changeViewState(ViewState.LOADED);
            writeAPLSessionInfoLog("Document Loaded");
        }

        @Override
        public void onDocumentRender(RootContext rootContext) {
            // TODO We should consider an api that handles this for our clients. For now this is an
            //  error scenario.
            if (mRootContext != null) {
                String message = "Trying to render a document prior to finishing old document!";
                if (BuildConfig.DEBUG) {
                    throw new AssertionError(message);
                }
                Log.wtf(TAG, message);
                int id = mTelemetryProvider.createMetricId(ITelemetryProvider.APL_DOMAIN, METRIC_INFLATE_BEFORE_FINISH_ERROR, COUNTER);
                mTelemetryProvider.incrementCount(id);
            }

            // Init telemetry
            mTelemetryProvider = rootContext.getOptions().getTelemetryProvider();
            mMetricsRecorder = rootContext.getMetricsRecorder();
            mBitmapFactory = rootContext.getRenderingContext().getBitmapFactory();
            ShadowCache shadowCache = rootContext.getRenderingContext().getShadowCache();
            mShadowRenderer = new ShadowBitmapRenderer(shadowCache, mBitmapFactory);

            tRenderDocument = mTelemetryProvider.createMetricId(ITelemetryProvider.APL_DOMAIN,
                    ITelemetryProvider.RENDER_DOCUMENT, TIMER);

            long seedTime = (mIsRenderStartTimeSet ? SystemClock.elapsedRealtime() - mRenderStartTime : 0);
            mIsRenderStartTimeSet = false;
            mTelemetryProvider.startTimer(tRenderDocument, TimeUnit.MILLISECONDS, seedTime);

            //seedTime needs to be calculated and subtracted by runtime in metrics sink implementation
            mMetricsRecorder.recordMilestone(MetricsMilestoneConstants.APLLAYOUT_RENDER_DOCUMENT_START_MILESTONE);

            APLLayout.this.onDocumentRender(rootContext);
            for (IDocumentLifecycleListener documentLifecycleListener: mDocumentLifecycleListeners) {
                documentLifecycleListener.onDocumentRender(rootContext);
            }

            rootContext.notifyContext();
            changeViewState(ViewState.READY);
            writeAPLSessionInfoLog("Document Ready");
        }

        public void reinflate() {
            mIsReinflating = true;
            APLLayout.this.inflateViews();
            writeAPLSessionInfoLog("Document Reinflated");
        }

        @Override
        public void addDocumentLifecycleListener(@NonNull IDocumentLifecycleListener documentLifecycleListener) {
            Objects.requireNonNull(documentLifecycleListener, "Parameter must not be null");
            mDocumentLifecycleListeners.add(documentLifecycleListener);
        }

        @Override
        public void onDocumentPaused() {
            for (IDocumentLifecycleListener documentLifecycleListener : mDocumentLifecycleListeners) {
                documentLifecycleListener.onDocumentPaused();
            }
            writeAPLSessionInfoLog("Document Paused");
        }

        @Override
        public void onDocumentResumed() {
            for (IDocumentLifecycleListener documentLifecycleListener : mDocumentLifecycleListeners) {
                documentLifecycleListener.onDocumentResumed();
            }
            writeAPLSessionInfoLog("Document Resumed");
        }

        @Override
        public void onDocumentDisplayed() {
            final long documentDisplayedTime = SystemClock.elapsedRealtime();

            if (tRenderDocument != ITelemetryProvider.UNKNOWN_METRIC_ID) {
                mTelemetryProvider.stopTimer(tRenderDocument);
                tRenderDocument = ITelemetryProvider.UNKNOWN_METRIC_ID;
            }
            mMetricsRecorder.recordMilestone(MetricsMilestoneConstants.APLLAYOUT_RENDER_DOCUMENT_END_MILESTONE);
            // Check for Reinflation
            if (mIsReinflating) {
                mIsReinflating = false;
                int tReinflate = mTelemetryProvider.getMetricId(APL_DOMAIN, RootContext.METRIC_REINFLATE);
                mTelemetryProvider.stopTimer(tReinflate);
                mMetricsRecorder.recordMilestone(MetricsMilestoneConstants.ROOTCONTEXT_REINFLATE_END_MILESTONE);
            }

            for (IDocumentLifecycleListener documentLifecycleListener : mDocumentLifecycleListeners) {
                documentLifecycleListener.onDocumentDisplayed(documentDisplayedTime);
            }
            if (mRootContext != null && mRootContext.getDocumentHandle() != null) {
                mRootContext.getDocumentHandle().setDocumentState(DocumentState.DISPLAYED);
            }

            changeViewState(ViewState.INFLATED);
            writeAPLSessionInfoLog("Document Inflated");
        }

        @Override
        public void associate(Component component, View view) {
            String componentId = component.getComponentId();
            final Component oldComponent = mComponents.get(view);
            if (oldComponent != null) {
                disassociate(oldComponent);
            }
            view.setId(componentId.hashCode());
            mViews.put(componentId, view);
            mComponents.put(view, component);
        }

        @Override
        public void disassociate(Component component) {
            disassociate(component, findView(component));
        }

        @Override
        public void disassociate(View view) {
            disassociate(findComponent(view), view);
        }

        private void disassociate(Component component, View view) {
            if (view == null || component == null) {
                final String message = "Trying to disassociate component/view but map is out of sync! Component: " + component + ", View: " + view;
                Log.e(TAG, message);
            }

            if (component != null) {
                final String componentId = component.getComponentId();
                mViews.remove(componentId);
            }

            if (view != null) {
                view.setId(0);
                mComponents.remove(view);
            }

            // TODO we could consider Image component manages its own memory
            if (component instanceof Image && view instanceof APLImageView) {
                LazyImageLoader.clearImageResources(ImageViewAdapter.getInstance(), (Image) component, (APLImageView) view);
            }
        }

        @Override
        public void onDocumentFinish() {
            APLLayout.this.onFinish();
            tRenderDocument = ITelemetryProvider.UNKNOWN_METRIC_ID;
            mTelemetryProvider = NoOpTelemetryProvider.getInstance();
            if (mShadowRenderer != null) {
                mShadowRenderer.cleanUp();
                mShadowRenderer = null;
            }

            for (IDocumentLifecycleListener documentLifecycleListener : mDocumentLifecycleListeners) {
                documentLifecycleListener.onDocumentFinish();
            }
            mDocumentLifecycleListeners.clear();

            changeViewState(ViewState.EMPTY);
            mDTView.cleanup();
        }

        @Override
        public ITelemetryProvider telemetry() {
            return mTelemetryProvider;
        }

        @Override
        public IMetricsRecorder metricsRecorder() {
            return mMetricsRecorder;
        }

        @Override
        public void setMetricsRecorder(IMetricsRecorder metricsRecorder) {
            mMetricsRecorder = metricsRecorder;
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
            mAplTrace.startTrace(TracePoint.APL_LAYOUT_INFLATE_COMPONENT_HIERARCHY);
            cViewsBatchedIncrementCount = 0;
            traverseComponentHierarchy(
                    root,
                    component -> {
                        ComponentViewAdapter viewAdapter = ComponentViewAdapterFactory.getAdapter(component);
                        if (viewAdapter == null) {
                            Log.e(TAG, "adapter is null");
                            return;
                        }
                        final View view = viewAdapter.createView(getContext(), mAplViewPresenter);
                        cViewsBatchedIncrementCount += 1;
                        // TODO: ideally these two methods would be called by the caller of inflateComponentHierarchy
                        // TODO: but we'll keep them here for now to avoid have to iterate over every Component twice from APLLayout.onLayout
                        applyAllProperties(component, view);
                    });
            mViewsCounter.increment(cViewsBatchedIncrementCount);
            mTelemetryProvider.incrementCount(cViews, (int) cViewsBatchedIncrementCount);
            mAplTrace.endTrace();
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
            if (bg == null) {
                mBackgroundDrawable.setDrawableByLayerId(DOCUMENT_BACKGROUND_LAYER_ID, new ColorDrawable(Color.TRANSPARENT));
                return;
            }
            if (bg.isGradient()) {
                final ViewportMetrics viewportMetrics = getOrCreateViewportMetrics();
                ShapeDrawable drawable = new ShapeDrawable(new RectShape());
                Shader shader = Gradient.createGradientShader(bg.getColorRange(), bg.getInputRange(),
                        bg.getType(), bg.getAngle(), viewportMetrics.height(), viewportMetrics.width());
                drawable.getPaint().setShader(shader);
                mBackgroundDrawable.setDrawableByLayerId(DOCUMENT_BACKGROUND_LAYER_ID, drawable);
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
                mAplTrace.startTrace(TracePoint.APL_LAYOUT_HANDLE_TOUCH);
                IMetricsTransform metricsTransform = mRootContext.getMetricsTransform();
                // apply negative offsets
                int xOffset = -metricsTransform.getViewportOffsetX();
                int yOffset = -metricsTransform.getViewportOffsetY();
                pointer.translate(xOffset, yOffset);
                boolean handled = mRootContext.handlePointer(pointer);
                mAplTrace.endTrace();
                return handled;
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
        public boolean isHardwareAccelerationForVectorGraphicsEnabled() {
            return mRootContext != null ? mRootContext.getRenderingContext().isHardwareAccelerationForVectorGraphicsEnabled() : false;
        }

        @Override
        public void onChildViewAdded(View parent, View child) {
            // no-op
        }

        /**
         * Whenever a view is removed, we remove the view <-> component mapping for the whole View hierarchy.
         * @param parent    the view parent
         * @param child     the child being removed
         */
        @Override
        public void onChildViewRemoved(View parent, View child) {
            // With scenegraph, the view hierarchy clean up is taken care through user data release callback
            if (mRootContext != null && !mRootContext.getOptions().isScenegraphEnabled()) {
                traverseViewHierarchy(child, this::disassociate);
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

        @Override
        public APLTrace getAPLTrace() {
            return mAplTrace;
        }

        @Override
        public void inflateScenegraph() {
            new APLScenegraph(mRootContext).applyUpdates();
        }

        @Override
        public boolean enqueueMotionEvents(List<MotionEvent> motionEvents) {
            if (mRootContext != null) {
                List<Pair<Pointer, Long>> pointers = new ArrayList<>();
                for (MotionEvent motionEvent : motionEvents) {
                    final Pointer pointer = mPointerTracker.trackAndGetPointer(motionEvent);
                    if (pointer != null) {
                        IMetricsTransform metricsTransform = mRootContext.getMetricsTransform();
                        // apply negative offsets
                        int xOffset = -metricsTransform.getViewportOffsetX();
                        int yOffset = -metricsTransform.getViewportOffsetY();
                        pointer.translate(xOffset, yOffset);
                        // Create a copy of the pointer, otherwise all pointers end up being the same
                        Pair<Pointer, Long> pointerPair = new Pair(new Pointer(pointer), motionEvent.getEventTime());
                        pointers.add(pointerPair);
                    }
                }
                mRootContext.addPointersToQueue(pointers);
                return true;
            }
            Log.w(TAG, "Trying to drive motion events when there is no document rendered");
            return false;
        }

        @Override
        public void clearMotionEvents() {
            if (mRootContext != null) {
                mRootContext.addPointersToQueue(null);
                return;
            }
            Log.w(TAG, "Trying to cancel motion events when there is no document rendered");
        }
    };

    /**
     * Sets the agent name for documents rendered in this layout.
     * This agent name will show up in SysTrace.
     * @param rootConfig the root config
     */
    public void setAgentName(@NonNull RootConfig rootConfig) {
        mAplTrace.setAgentName((String) rootConfig.getProperty(RootProperty.kAgentName));
    }

    public String getSerializedScenegraph() {
        if (mRootContext != null) {
            return new APLScenegraph(mRootContext).getSerializedScenegraph();
        }
        return "{}";
    }

    public String getVisualContext() {
        if (mRootContext != null) {
            return mRootContext.serializeVisualContext();
        }
        return "{}";
    }

    public String getDOM() {
        if (mRootContext != null) {
            return new APLScenegraph(mRootContext).getDOM();
        }
        return "{}";
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        mIsMeasureValid = true;
        Consumer<ViewportMetrics> metricsConsumer;
        while ((metricsConsumer = mMetricsConsumers.poll()) != null) {
            metricsConsumer.accept(mAplViewPresenter.getOrCreateViewportMetrics());
        }

        ITelemetryProvider telemetry = mAplViewPresenter.telemetry();
        IMetricsRecorder metricsRecorder = mAplViewPresenter.metricsRecorder();
        final boolean isNewLayout = mViewsNeedLayout.getAndSet(false);
        try (APLTrace.AutoTrace trace = mAplTrace.startAutoTrace(TracePoint.APL_LAYOUT_ON_LAYOUT)) {
            if (isNewLayout && mRootContext != null) {
                if (metricsRecorder != null) {
                    mAplViewPresenter.metricsRecorder().recordMilestone(MetricsMilestoneConstants.APLLAYOUT_LAYOUT_START_MILESTONE);
                    mAplViewPresenter.metricsRecorder().recordMilestone(MetricsMilestoneConstants.APLLAYOUT_VIEW_INFLATE_START_MILESTONE);
                }
                if (telemetry != null) {
                    telemetry.startTimer(tLayout);
                    telemetry.startTimer(tViewInflate);
                }
                // Remove previous layout
                clear();

                if (mRootContext.getOptions().isScenegraphEnabled()) {
                    Log.d(TAG, "Using scenegraph to inflate views");
                    long topCoreLayer = new APLScenegraph(mRootContext).getTop();
                    APLLayer aplLayer = APLLayer.ensure(topCoreLayer, mRootContext.getRenderingContext());
                    APLView aplView = new APLView(getContext(), aplLayer);
                    float scalingForDpi = mMetrics.dpi()/ 160f;
                    APLRootView aplRootView = new APLRootView(getContext(), mMetrics.width(), mMetrics.height(), scalingForDpi);
                    aplRootView.addView(aplView);
                    aplLayer.attachView(aplView);
                    this.addView(aplRootView);
                } else {
                    Component topComponent = mRootContext.getTopComponent();
                    mAplViewPresenter.inflateComponentHierarchy(topComponent);
                }

                if (telemetry != null) {
                    telemetry.stopTimer(tViewInflate);
                    if (metricsRecorder != null) {
                        metricsRecorder.recordMilestone(MetricsMilestoneConstants.APLLAYOUT_VIEW_INFLATE_END_MILESTONE);
                    }
                }

                // kick off the timer
                mRootContext.initTime();

                // signal that next dispatchDraw is for new Views
                mViewsNeedDisplay.set(true);
            }

            // Layout the components
            final int count = getChildCount();

            try (APLTrace.AutoTrace forTrace = mAplTrace.startAutoTrace(TracePoint.APL_LAYOUT_ON_LAYOUT_FOR)) {
                for (int i = 0; i < count; i++) {
                    final View child = getChildAt(i);
                    if (child.getVisibility() != GONE) {

                        // Place the child.

                        final FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) child.getLayoutParams();

                        int left = lp.leftMargin;
                        int top = lp.rightMargin;

                        // When APLController.getRuntimeConfig().isClearViewsOnFinish() == false, onFinish()
                        // will not clear() views. So control may reach here while mRootContext is null
                        if (mRootContext != null) {
                            IMetricsTransform transform = mRootContext.getMetricsTransform();
                            left += transform.getViewportOffsetX();
                            top += transform.getViewportOffsetY();
                        } else {
                            Log.w(TAG, "Got onLayout() when mRootContext was null, skipping applying transforms");
                        }
                        // Adjust the child width and height.
                        child.measure(MeasureSpec.makeMeasureSpec(lp.width, MeasureSpec.EXACTLY),
                                MeasureSpec.makeMeasureSpec(lp.height, MeasureSpec.EXACTLY));
                        if (mRootContext != null && !mRootContext.getOptions().isScenegraphEnabled()) {
                            child.layout(left, top, left + lp.width, top + lp.height);
                        } else {
                            final int width = r - l;
                            final int height = b - t;
                            child.layout(left, top, left + width, top + height);
                        }
                    }
                }
            }

            if (telemetry != null && isNewLayout && mRootContext != null) {
                telemetry.stopTimer(tLayout);
                if (metricsRecorder != null) {
                    metricsRecorder.recordMilestone(MetricsMilestoneConstants.APLLAYOUT_LAYOUT_END_MILESTONE);
                }
            }
        } catch (Exception ex) {
            if (telemetry != null && isNewLayout && mRootContext != null) {
                telemetry.fail(tLayout);
                if (metricsRecorder != null) {
                    metricsRecorder.recordMilestone(MetricsMilestoneConstants.APLLAYOUT_LAYOUT_FAILED_MILESTONE);
                }
            }
            throw (ex);
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        try {
            super.dispatchDraw(canvas);
            if (!mPath.isEmpty()){
                drawPathOnCanvas(canvas);
            }
        } finally {
            if (mViewsNeedDisplay.getAndSet(false)) {
                mAplViewPresenter.onDocumentDisplayed();
            }
        }
    }

    private void drawPathOnCanvas(Canvas canvas) {
        Paint paint = new Paint();
        paint.setColor(Color.YELLOW);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(20);
        canvas.drawPath(mPath, paint);

        paint.setColor(Color.GRAY);
        paint.setStyle(Paint.Style.FILL);
        paint.setAlpha(150);
        canvas.drawPath(mPath, paint);
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
        int w = bounds.intWidth();
        int h = bounds.intHeight();
        int l = bounds.intLeft();
        int t = bounds.intTop();

        // Get the parent to determine correct layout params
        String parentId = component.getParentId();
        View parentView = null;
        if (parentId != null && !parentId.isEmpty()) {
            @SuppressWarnings("ConstantConditions") Component pComp = mRootContext.getOrInflateComponentWithUniqueId(parentId);
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
            // check if the event gets consumed by any component (e.g. navigational keys in EditText)
            // if not, send to core.
            // if that remains false, and the key is directional, focus the first element and return.
            boolean consumed = super.dispatchKeyEvent(event) || mAplViewPresenter.onKeyPress(event);
            if (!consumed && KeyUtils.isKeyEventDirectional(event) &&
                    mRootContext.getFocusedComponentId().isEmpty()) {
                consumed = mRootContext.nextFocus(FocusDirection.kFocusDirectionRight);
            }
            return consumed;
        }
        return mAplViewPresenter.onKeyPress(event) || super.dispatchKeyEvent(event);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        boolean coreHandled = mAplViewPresenter.handleTouchEvent(event);
        if (coreHandled) {
            final ViewParent parent = getParent();
            if (parent != null) {
                // Coordinate with parent view (e.g. scroll view), which may be considering intercepting touch events
                parent.requestDisallowInterceptTouchEvent(true);
            }

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
        try (APLTrace.AutoTrace trace = mAplTrace.startAutoTrace(TracePoint.APL_LAYOUT_HANDLE_CONFIGURATION_CHANGE)) {
            if (configurationChange.screenReaderEnabled() != mAccessibilityManager.isEnabled()) {
                handleAccessibilityStateChange(mAccessibilityManager.isEnabled());
            }
            if (mRootContext != null) {
                mRootContext.handleConfigurationChange(configurationChange);
            }
            // store latest configuration change to be used for restoring backstack document
            mLatestConfigChange = configurationChange;

            // With scenegraph, there is no need to check for the top component
            if (!mRootContext.getOptions().isScenegraphEnabled()) {
                final Component topComponent = mRootContext.getTopComponent();
                if (topComponent == null) {
                    throw new APLController.APLException("Document cannot be rendered in this configuration", new IllegalStateException());
                }
            }
        }
    }

    public void setHandleConfigurationChangeOnSizeChanged(boolean handleConfigurationChangeOnSizeChanged) {
        mHandleConfigurationChangeOnSizeChanged = handleConfigurationChangeOnSizeChanged;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (mHandleConfigurationChangeOnSizeChanged && mRootContext != null && !mRootContext.isAutoSizeLayoutPending()) {
            Log.i(TAG, "Handle configuration change onSizeChanged");
            try {
                handleConfigurationChange(
                        createConfigurationChange()
                                .width(w)
                                .height(h).build());
            } catch (APLController.APLException e) {
                Log.e(TAG, "Unable to handle configuration change.", e);
            }
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
        if (enabled != mIsAccessibilityActive) {
            mIsAccessibilityActive = enabled;

            for (View view : mViews.values()) {
                Component component = mAplViewPresenter.findComponent(view);
                ComponentViewAdapter componentViewAdapter = ComponentViewAdapterFactory.getAdapter(component);
                if (componentViewAdapter == null) {
                    Log.e(TAG, "adapter is null");
                    return;
                }
                componentViewAdapter.applyFocusability(component, view);
            }
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

    /**
     * Convenience method for visiting all Views in a given hierarchy via a Breadth-First Search.
     * This includes views that have been detached.
     *
     * Similar to {@link #traverseComponentHierarchy(Component, Consumer)} except that this uses the
     * view hierarchy whereas the other uses displayed children hierarchy.
     *
     * @param root              the root view
     * @param visitorOperation  the operation
     */
    public static void traverseViewHierarchy(
            final View root,
            final Consumer<View> visitorOperation) {
        final Queue<View> queue = new LinkedList<>();
        queue.add(root);
        while (!queue.isEmpty()) {
            View current = queue.poll();
            visitorOperation.accept(current);
            // Add the children to the queue
            if (current instanceof APLAbsoluteLayout) {
                APLAbsoluteLayout layout = (APLAbsoluteLayout) current;
                for (View child : layout.getAttachedAndDetachedChildren()) {
                    queue.add(child);
                }
            }
        }
    }

    @VisibleForTesting
    public List<View> getBfsOrderedViews() {
        if (getChildCount() == 0) {
            return Collections.emptyList();
        }

        List<View> bfsQueue = new ArrayList<>(getViews().size());
        traverseViewHierarchy(getChildAt(0), bfsQueue::add);
        return bfsQueue;
    }

    @VisibleForTesting
    public Map<View, Component> getComponents() {
        return mComponents;
    }

    @VisibleForTesting
    public Map<String, View> getViews() {
        return mViews;
    }

    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        final Component childComponent = mAplViewPresenter.findComponent(child);
        int saveCount = canvas.save();
        if (childComponent != null && childComponent.hasTransform()) {
            TransformUtils.applyChildTransformToParentCanvas(childComponent.getTransform(), child, canvas);
        }
        boolean result;
        result = super.drawChild(canvas, child, drawingTime);
        canvas.restoreToCount(saveCount);
        return result;
    }
}


