/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android;

import android.os.Looper;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.annotation.VisibleForTesting;

import com.amazon.apl.android.bitmap.PooledBitmapFactory;
import com.amazon.apl.android.configuration.ConfigurationChange;
import com.amazon.apl.android.dependencies.IExtensionEventCallback;
import com.amazon.apl.android.dependencies.IExtensionImageFilterCallback;
import com.amazon.apl.android.events.ControlMediaEvent;
import com.amazon.apl.android.events.DataSourceFetchEvent;
import com.amazon.apl.android.events.ExtensionEvent;
import com.amazon.apl.android.events.FinishEvent;
import com.amazon.apl.android.events.FocusEvent;
import com.amazon.apl.android.events.LineHighlightEvent;
import com.amazon.apl.android.events.LoadMediaRequestEvent;
import com.amazon.apl.android.events.OpenKeyboardEvent;
import com.amazon.apl.android.events.OpenURLEvent;
import com.amazon.apl.android.events.PlayMediaEvent;
import com.amazon.apl.android.events.PrerollEvent;
import com.amazon.apl.android.events.ReinflateEvent;
import com.amazon.apl.android.events.RequestFirstLineBounds;
import com.amazon.apl.android.events.RequestLineBoundsEvent;
import com.amazon.apl.android.events.SendEvent;
import com.amazon.apl.android.events.SpeakEvent;
import com.amazon.apl.android.primitive.Rect;
import com.amazon.apl.android.providers.AbstractMediaPlayerProvider;
import com.amazon.apl.android.providers.ITelemetryProvider;
import com.amazon.apl.android.providers.impl.NoOpMediaPlayerProvider;
import com.amazon.apl.android.scaling.MetricsTransform;
import com.amazon.apl.android.scaling.Scaling;
import com.amazon.apl.android.scaling.ViewportMetrics;
import com.amazon.apl.android.touch.Pointer;
import com.amazon.apl.android.utils.APLTrace;
import com.amazon.apl.android.utils.JNIUtils;
import com.amazon.apl.android.utils.TracePoint;
import com.amazon.apl.enums.ComponentType;
import com.amazon.apl.enums.DisplayState;
import com.amazon.apl.enums.EventScrollAlign;
import com.amazon.apl.enums.EventType;
import com.amazon.apl.enums.FocusDirection;
import com.amazon.apl.enums.PropertyKey;
import com.amazon.apl.enums.RootProperty;
import com.amazon.common.BoundObject;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.amazon.apl.android.providers.ITelemetryProvider.APL_DOMAIN;
import static com.amazon.apl.android.providers.ITelemetryProvider.Type.COUNTER;
import static com.amazon.apl.android.providers.ITelemetryProvider.Type.TIMER;
import static com.amazon.apl.enums.EventType.valueOf;


/**
 * APL RootContext is responsible for communication via JNI with APL core.  It marshals the tasks of
 * layout, an component construction.
 */
public class RootContext extends BoundObject implements IClock.IClockCallback {

    private static final String TAG = "RootContext";
    private static final boolean DEBUG = false;

    /**
     * Context configuration
     */
    @NonNull
    private final APLOptions mOptions;
    @NonNull
    private final ITelemetryProvider mTelemetryProvider;

    // DoFrame metrics
    private static final String METRIC_DO_FRAME_FAIL = TAG + ".doFrame.fail";
    private static final String METRIC_DROP_FRAME = TAG + ".dropFrame";
    private static final long TARGET_DO_FRAME_TIME = 16700000; //16.7ms
    private int cDoFrameFail;
    private int cDropFrame;

    // Inflate metrics
    private static final String METRIC_INFLATE = TAG + ".inflate";
    private int tInflate;

    // Reinflate metrics
    public static final String METRIC_REINFLATE = TAG + ".reinflate";
    private int tReinflate;

    private static final String METRIC_COMPONENT_COUNT = TAG + ".componentCount";
    private int cComponent;

    // Visual Context update timing
    private static final long VISUAL_CONTEXT_UPDATE_INTERVAL_MS = 500;
    private static final long DATA_SOURCE_CONTEXT_UPDATE_INTERVAL_MS = 500;
    private long mLastVisualContextUpdateTime = 0;
    private long mLastDataSourceUpdateTime = 0;

    private Map<ComponentType, Integer> cComponentType = new EnumMap<>(ComponentType.class);

    // Map of Component unique ID to Components in this layout
    // TODO consider only creating components as needed (i.e. ones that are laid-out by core).
    private static final int INITIAL_COMPONENT_MAP_CAPACITY = 128;
    private final Map<String, Component> mAplComponents = new ArrayMap<>(INITIAL_COMPONENT_MAP_CAPACITY);

    // The start time of the APL update loop.
    private long mStartLoopTime = 0;

    // The active state of the document
    private final AtomicBoolean mIsFinished = new AtomicBoolean(false);

    // Whether the document is in the resumed state (i.e. the frame loop has been started)
    private final AtomicBoolean mIsResumed = new AtomicBoolean(false);

    @NonNull
    private final Queue<Runnable> mWorkQueue = new ConcurrentLinkedQueue<>();

    // Used internally to communicate views back out to the APLLayout
    @NonNull
    private final IAPLViewPresenter mViewPresenter;

    // This callback is used by native code, maintain this instance
    @NonNull
    private final TextMeasureCallback mTextMeasureCallback;

    private boolean lastScreenLockStatus = false; // disabled

    private MetricsTransform mMetricsTransform;
    private final RootConfig mRootConfig;
    private final RenderingContext mRenderingContext;

    private final Set<BoundObject> mPending = new HashSet<>();

    private final Object mLock = new Object();

    private final String mAgentName;

    private final APLTrace mAplTrace;

    private final IClock mAplClock;

    /**
     * Construct a new RootContext object.
     *
     * @param metrics       the viewport metrics
     * @param content       the apl document
     * @param rootConfig    the environment configuration
     * @param options       the apl options
     * @param viewPresenter the view presenter
     */
    private RootContext(@NonNull ViewportMetrics metrics,
                        @NonNull Content content,
                        @NonNull RootConfig rootConfig,
                        @NonNull APLOptions options,
                        @NonNull IAPLViewPresenter viewPresenter) {
        mAplTrace = viewPresenter.getAPLTrace();
        try (APLTrace.AutoTrace autoTrace = mAplTrace.startAutoTrace(TracePoint.ROOT_CONTEXT_CREATE)) {
            mOptions = options;
            mAplClock = options.getAplClockProvider().create(this);
            mTelemetryProvider = mOptions.getTelemetryProvider();
            mViewPresenter = viewPresenter;
            mRootConfig = rootConfig;
            mMetricsTransform = MetricsTransform.create(metrics);
            mRenderingContext = buildRenderingContext(APLVersionCodes.getVersionCode(content.getAPLVersion()),
                    options, rootConfig, mMetricsTransform);
            mTextMeasureCallback = TextMeasureCallback.factory().create(mMetricsTransform, new TextMeasure(mRenderingContext));
            mAgentName = (String) rootConfig.getProperty(RootProperty.kAgentName);
            preBindInit();
            final long nativeHandle = createHandle(metrics, content, rootConfig, mTextMeasureCallback);
            mTextMeasureCallback.onRootContextCreated();
            bind(nativeHandle);
            inflate();
        }
    }

    /**
     * Construct a RootContext object from a previously inflated core RootContext.
     *
     * @param options       the apl options
     * @param viewPresenter the view presenter
     * @param nativeHandle  a pointer to the core RootContext
     */
    private RootContext(@NonNull APLOptions options,
                        @NonNull IAPLViewPresenter viewPresenter,
                        @NonNull MetricsTransform metricsTransform,
                        @NonNull RootConfig rootConfig,
                        long nativeHandle) {
        mAplTrace = viewPresenter.getAPLTrace();
        try (APLTrace.AutoTrace autoTrace = mAplTrace.startAutoTrace(TracePoint.ROOT_CONTEXT_CREATE)) {
            mOptions = options;
            mAplClock = options.getAplClockProvider().create(this);
            mTelemetryProvider = mOptions.getTelemetryProvider();
            mMetricsTransform = metricsTransform;
            mRootConfig = rootConfig;
            mViewPresenter = viewPresenter;
            bind(nativeHandle);
            mRenderingContext = buildRenderingContext(APLVersionCodes.getVersionCode(nGetVersionCode(nativeHandle)),
                    options, rootConfig, metricsTransform);
            // rebind to the previously configured text measure.
            mTextMeasureCallback = TextMeasureCallback.factory().create(mRootConfig, mMetricsTransform, new TextMeasure(mRenderingContext));
            mTextMeasureCallback.onRootContextCreated();
            mAgentName = (String) rootConfig.getProperty(RootProperty.kAgentName);
            inflate();
        }
    }

    private RenderingContext buildRenderingContext(int docVersion, APLOptions options, RootConfig config,MetricsTransform metricsTransform) {

        // TODO this is adapting legacy framework to new, deprecate the old
        ExtensionMediator extensionMediator = config.getExtensionMediator();
        IExtensionImageFilterCallback ifCB = (extensionMediator == null)
                ? options.getExtensionImageFilterCallback()
                : extensionMediator;
        IExtensionEventCallback eeCB = (extensionMediator == null)
                ? options.getExtensionEventCallback()
                : extensionMediator;

        RenderingContext.Builder ctxBuilder = RenderingContext.builder()
                .metricsTransform(metricsTransform)
                .docVersion(docVersion)
                .telemetryProvider(options.getTelemetryProvider())
                .avgContentRetriever(options.getAvgRetriever())
                .imageProcessor(options.getImageProcessor())
                .imageLoaderProvider(options.getImageProvider())
                .imageUriSchemeValidator(options.getImageUriSchemeValidator())
                .mediaPlayerProvider(getMediaPlayerProvider((Boolean) mRootConfig.getProperty(RootProperty.kDisallowVideo), options))
                .bitmapFactory(PooledBitmapFactory.create(options.getTelemetryProvider(), APLController.getRuntimeConfig().getBitmapPool()))
                .bitmapCache(APLController.getRuntimeConfig().getBitmapCache())
                .textLayoutFactory(TextLayoutFactory.create(metricsTransform))
                .extensionImageFilterCallback(ifCB)
                .extensionEventCallback(eeCB)
                .aplTrace(mAplTrace)
                .isMediaPlayerV2Enabled(config.isMediaPlayerV2Enabled());

        if (extensionMediator != null) {
            ctxBuilder.extensionResourceProvider(extensionMediator.extensionResourceProvider);
        }

        return ctxBuilder.build();
    }

    private AbstractMediaPlayerProvider getMediaPlayerProvider(boolean disallowVideo, APLOptions options) {
        if (mRootConfig.isMediaPlayerV2Enabled()) {
            return disallowVideo ? NoOpMediaPlayerProvider.getInstance() : mRootConfig.getMediaPlayerFactoryProxy().getMediaPlayerProvider();
        }
        return disallowVideo ? NoOpMediaPlayerProvider.getInstance() : options.getMediaPlayerProvider();
    }

    /**
     * Creates a RootContext from metrics, content and root config information.
     *
     * @param metrics    the viewport metrics
     * @param content    the content
     * @param rootConfig the root config
     * @param textMeasureCallback the callback for text measurement
     * @return a non-zero handle if created, 0 if failed.
     */
    private long createHandle(ViewportMetrics metrics, Content content,
                              RootConfig rootConfig, TextMeasureCallback textMeasureCallback) {
        final Scaling scaling = metrics.scaling();
        boolean retry;
        long rootContextHandle;
        do {
            final ViewportMetrics scaledMetrics = mMetricsTransform.getScaledMetrics();
            mTelemetryProvider.startTimer(tInflate);
            rootContextHandle = nCreate(
                    content.getNativeHandle(),
                    rootConfig.getNativeHandle(),
                    textMeasureCallback.getNativeHandle(),
                    scaledMetrics.width(),
                    scaledMetrics.height(),
                    scaledMetrics.dpi(),
                    scaledMetrics.shape().getIndex(),
                    scaledMetrics.theme(),
                    scaledMetrics.mode().getIndex());
            mTelemetryProvider.stopTimer(tInflate);
            // We continue to try different specifications if the RootContext fails to create
            // and we have specifications to try
            retry = rootContextHandle == 0 &&
                    scaling.removeChosenViewportSpecification(mMetricsTransform);
            if (retry) {
                // Update the metrics transform for this new specification
                mMetricsTransform = MetricsTransform.create(metrics);
                mRenderingContext.setMetricsTransform(mMetricsTransform);
            }
        } while(retry);
        return rootContextHandle;
    }

    /**
     * Initializes member variables and metrics.
     */
    private void preBindInit() {
        cDoFrameFail = mTelemetryProvider.createMetricId(APL_DOMAIN, METRIC_DO_FRAME_FAIL, COUNTER);
        cDropFrame = mTelemetryProvider.createMetricId(APL_DOMAIN, METRIC_DROP_FRAME, COUNTER);

        tInflate = mTelemetryProvider.createMetricId(APL_DOMAIN, METRIC_INFLATE, TIMER);
        tReinflate = mTelemetryProvider.createMetricId(APL_DOMAIN, METRIC_REINFLATE, TIMER);
        cComponent = mTelemetryProvider.createMetricId(APL_DOMAIN, METRIC_COMPONENT_COUNT, COUNTER);
    }

    /**
     * Request to create a RootContext.
     *
     * @param metrics APL display metrics.
     * @param content APL document content.
     * @return a RootContext.
     */
    public static RootContext create(
            @NonNull final ViewportMetrics metrics, @NonNull final Content content, @NonNull final RootConfig rootConfig,
            @NonNull final APLOptions options, @NonNull IAPLViewPresenter presenter)
            throws IllegalArgumentException, IllegalStateException {
        if (metrics == null) {
            throw new IllegalArgumentException("Metrics must not be null");
        } else if (content == null) {
            throw new IllegalArgumentException("Content must not be null");
        } else if (rootConfig == null) {
            throw new IllegalArgumentException("RootConfig must not be null.");
        } else if (!content.isReady()) {
            throw new IllegalArgumentException("Content must be in the 'ready' state.");
        } else if (options == null) {
            throw new IllegalArgumentException("APLOptions must not be null");
        } else if (presenter == null) {
            throw new IllegalArgumentException("APLPresenter must not be null");
        }
        presenter.preDocumentRender();
        RootContext rootContext = new RootContext(metrics, content, rootConfig, options, presenter);
        if (!rootContext.isBound()) {
            throw new IllegalStateException("Could not create RootContext");
        }
        return rootContext;
    }

    /**
     * Inflate the layout.
     * This results in core calling back to {@link #buildComponent(String, long, int)}.
     */
    private void inflate() {
        try {
            nInflate(getNativeHandle());
        } catch (Exception e) {
            mTelemetryProvider.fail(tInflate);
            throw (e);
        }
    }

    /**
     * Reinflate the layout.
     * This results in core calling back to #buildComponent(String, long, int)}.
     */
    public void reinflate() {
        try (APLTrace.AutoTrace trace = mAplTrace.startAutoTrace(TracePoint.ROOT_CONTEXT_RE_INFLATE)) {
            mTelemetryProvider.startTimer(tReinflate);
            // Finish component and views resources
            clearComponentAndViewsResources();

            // Recreate a new Viewport metrics (required before any inflation).
            mViewPresenter.getOrCreateViewportMetrics();

            // Stop processing frame loops so that events are not processed until reinflation is completed.
            pauseDocument();
            mIsFinished.set(true);

            // Java components and views reinflation
            try {
                if (!nReinflate(getNativeHandle())) {
                    Log.w(TAG, "Reinflation failed because document cannot be rendered");
                    return;
                }
                notifyContext();
            } catch (Exception e) {
                mTelemetryProvider.fail(tReinflate);
                throw (e);
            }

            // Initialize presenter using existing rootContext.
            mViewPresenter.reinflate();
        }
    }

    private void clearComponentAndViewsResources() {
        mViewPresenter.clearLayout();
        mWorkQueue.clear();
        mPending.clear();
        mAplComponents.clear();
        mRenderingContext.getMediaPlayerProvider().releasePlayers();
    }

    /**
     * Request to create a RootContext from a handle to a Core RootContext object.
     *
     * @param documentState the cached document state.
     * @param viewPresenter target ViewPresenter
     * @return a new {@link RootContext} from {@link DocumentState} and {@link IAPLViewPresenter}
     */
    public static RootContext createFromCachedDocumentState(@NonNull final DocumentState documentState, @NonNull IAPLViewPresenter viewPresenter) {
        return new RootContext(documentState.getOptions(), viewPresenter, documentState.getMetricsTransform(), documentState.getRootConfig(), documentState.getNativeHandle());
    }

    public RenderingContext getRenderingContext() {
        return mRenderingContext;
    }

    MetricsTransform getMetricsTransform() {
        return mMetricsTransform;
    }

    RootConfig getRootConfig() {
        return mRootConfig;
    }

    /**
     * Increment the count of a metric using the global TelemetryProvider.
     *
     * @param metricName the metric name to be incremented
     */
    public void incrementMetricCount(final String metricName) {
        final int metricId = mTelemetryProvider.createMetricId(ITelemetryProvider.APL_DOMAIN, metricName, ITelemetryProvider.Type.COUNTER);
        mTelemetryProvider.incrementCount(metricId);
    }


    /**
     * Request that the document be finished.
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
    public void finishDocument() {
        Log.i(TAG, String.format("Document(%s) finishing.", mRootConfig.getSession().getLogId()));
        synchronized (mLock) {
            checkUiThread();
            // mark the context as finished to block any per frame loop.
            mIsFinished.set(true);
            mAplClock.stop();

            // End all active events
            cancelExecution();

            // notify providers that rendering is finished
            mViewPresenter.onDocumentFinish();

            // clear all work
            mWorkQueue.clear();

            // clean up any pending events
            mPending.clear();

            // clean up Components
            mAplComponents.clear();
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
    public void pauseDocument() {
        if (!mIsFinished.get() && mIsResumed.get()) {
            Log.i(TAG, String.format("Document(%s) pausing.", mRootConfig.getSession().getLogId()));
            mIsResumed.set(false);
            mAplClock.stop();
            cancelExecution();
            mStartLoopTime = 0;
            mViewPresenter.onDocumentPaused();
        } else {
            Log.i(TAG, String.format("Document(%s) already resumed.", mRootConfig.getSession().getLogId()));
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
    public void resumeDocument() {
        if (!mIsFinished.get() && !mIsResumed.get()) {
            Log.i(TAG, String.format("Document(%s) resuming.", mRootConfig.getSession().getLogId()));
            mIsResumed.set(true);
            mAplClock.start();
            mViewPresenter.onDocumentResumed();
        } else {
            Log.i(TAG, String.format("Document(%s) already paused.", mRootConfig.getSession().getLogId()));
        }
    }

    /**
     * @return The APL configuration options.
     */
    @NonNull
    APLOptions getOptions() {
        return mOptions;
    }

    private static native String nGetTopComponent(long nativeHandle);


    public void scrollToRectInComponent(@NonNull Component component, int x, int y, int w, int h, @NonNull EventScrollAlign align) {
        nScrollToRectInComponent(getNativeHandle(),
                component.getNativeHandle(),
                mMetricsTransform.toCore(x),
                mMetricsTransform.toCore(y),
                mMetricsTransform.toCore(w),
                mMetricsTransform.toCore(h),
                align.getIndex());
    }

    /**
     * Returns the native handle for a component if it exists
     *
     * @param componentId the component id
     * @return the native handle
     */
    @SuppressWarnings("unused")
    private long getComponentHandle(String componentId) {
        Component component = mAplComponents.get(componentId);
        return component != null ? component.getNativeHandle() : 0;
    }

    /**
     * callback for APLCore to use java string functions
     *
     * @param value        the string to be capitalized
     * @param localeString language tag
     * @return capitalized value parameter
     */
    @SuppressWarnings("unused")
    @VisibleForTesting
    public static String callbackToUpperCase(String value, String localeString) {
        Locale locale = (localeString == null || localeString.isEmpty()) ? Locale.getDefault()
                : Locale.forLanguageTag(localeString);
        return JNIUtils.safeStringValues(value).toUpperCase(locale);
    }

    /**
     * callback for APLCore to use java string functions
     *
     * @param value        the string to be uncapitalized
     * @param localeString language tag
     * @return uncapitalized value parameter
     */
    @SuppressWarnings("unused")
    @VisibleForTesting
    public static String callbackToLowerCase(String value, String localeString) {
        Locale locale = (localeString == null || localeString.isEmpty()) ? Locale.getDefault()
                : Locale.forLanguageTag(localeString);

        return JNIUtils.safeStringValues(value).toLowerCase(locale);
    }


    /**
     * Build a Component and pair it with the native Component.
     * <p>
     * This method is also called from the native layer during {@link #inflate()}.
     * <p>
     * TODO extract this into a ComponentFactory when moving Components to package
     *
     * @param componentId   the unique string id of the component
     * @param nativeHandle  the pointer to the native Component
     * @param typeId        the type of component to inflate
     */
    @SuppressWarnings("unused")
    private void buildComponent(String componentId, long nativeHandle, int typeId) {
        if (mAplComponents.containsKey(componentId)) {
            return;
        }

        Component component = null;
        ComponentType type = ComponentType.valueOf(typeId);
        switch (type) {
            case kComponentTypeSequence:
            case kComponentTypeGridSequence:
            case kComponentTypeContainer:
            case kComponentTypeScrollView:
            case kComponentTypeTouchWrapper:
                component = new MultiChildComponent(nativeHandle, componentId, getRenderingContext());
                break;
            case kComponentTypePager:
                component = new Pager(nativeHandle, componentId, getRenderingContext());
                break;
            case kComponentTypeFrame:
                component = new Frame(nativeHandle, componentId, getRenderingContext());
                break;
            case kComponentTypeExtension:
                component = new ExtensionComponent(nativeHandle, componentId, getRenderingContext());
                break;
            case kComponentTypeImage:
                component = new Image(nativeHandle, componentId, getRenderingContext());
                break;
            case kComponentTypeText:
                component = new Text(nativeHandle, componentId, getRenderingContext());
                break;
            case kComponentTypeVectorGraphic:
                component = new VectorGraphic(nativeHandle, componentId, getRenderingContext());
                break;
            case kComponentTypeVideo:
                component = new Video(nativeHandle, componentId, getRenderingContext());
                break;
            case kComponentTypeEditText:
                component = (Boolean) mRootConfig.getProperty(RootProperty.kDisallowEditText) ?
                    new NoOpComponent(nativeHandle, componentId, getRenderingContext()) :
                    new EditText(nativeHandle, componentId, getRenderingContext());
        }

        //TODO remove this, component should not have ref to RootContext
        // at this time it is using it as component cache, and visual context notifier
        //noinspection deprecation
        component.mRootContext = this;

        mAplComponents.put(componentId, component);
        mTelemetryProvider.incrementCount(cComponent);
        if (BuildConfig.DEBUG) {
            // metrics per component, not for production, used for debug only
            Integer cIdx = cComponentType.get(type);
            if (null == cIdx) {
                cIdx = mTelemetryProvider.createMetricId(APL_DOMAIN, TAG + "." + type.toString(), COUNTER);
                cComponentType.put(type, cIdx);
            }
            mTelemetryProvider.incrementCount(cIdx);
        }
    }

    /**
     * Set the local time adjustment. This is the number of milliseconds added to the UTC
     * time that gives the correct local time including DST.
     *
     * @param adjustment The adjustment time in milliseconds
     */
    public void setLocalTimeAdjustment(long adjustment) {
        nSetLocalTimeAdjustment(getNativeHandle(), adjustment);
    }

    @UiThread
    private static native void nSetLocalTimeAdjustment(long nativeHandle, long adjustment);

    /**
     * Updates the core with the current frame time.
     *
     * @param frameTime frame time in nanoseconds since loop start.
     * @param utcTime   Current UTC time in millis
     */
    @UiThread
    private static native void updateTime(long nativeHandle, long frameTime, long utcTime);

    /**
     * Executes an array of commands
     *
     * @param commands The commands to execute
     * @return An action to know when the commands are done or terminated.
     */
    @Nullable
    public Action executeCommands(@NonNull String commands) {
        long handle = nExecuteCommands(getNativeHandle(), commands);
        if (handle == 0) {
            return null;
        }
        Action action = new Action(handle, this);
        return action;
    }


    @Nullable
    public Action invokeExtensionEventHandler(String uri, String name, Map<String, Object> data,
                                              boolean fastmode) {

        long handle = nInvokeExtensionEventHandler(getNativeHandle(),
                uri, name, data, fastmode);

        if (handle == 0) {
            return null;
        } else {
            return new Action(handle, this);
        }
    }

    /**
     * Updates data source with a given data.
     *
     * @param type - the DataSource type to update
     * @param data - data to update data source
     * @return true if data source has been updated successfully, otherwise - false
     */
    public boolean updateDataSource(@NonNull final String type, @NonNull final String data) {
        return nUpdateDataSource(getNativeHandle(), type, data);
    }


    /**
     * Get a view associated with a component. Creates it if it does not exist. Updates properties.
     *
     * @param component    The parent of the Component;
     * @param ensureLayout The child should have it's layout calculated.
     */
    private void onComponentChange(@NonNull Component component, @SuppressWarnings("SameParameterValue") boolean ensureLayout, List<PropertyKey> dirtyProperties) {
        mAplTrace.startTrace(TracePoint.ROOT_CONTEXT_ON_COMPONENT_CHANGE);
        if (ensureLayout) {
            component.ensureLayout();
        }
        // Inflate a Java component if the change is insert.
        if (dirtyProperties.contains(PropertyKey.kPropertyNotifyChildrenChanged)) {
            Object[] changes = component.getChangedChildren();
            for (Object o : changes) {
                if (!(o instanceof Map)) {
                    Log.e(TAG, "Could not process kPropertyNotifyChildrenChanged, content is not a Map.");
                    return;
                }
                Map change = (Map) o;
                if (!change.containsKey("uid")) {
                    Log.e(TAG, "Missing uid key in kPropertyNotifyChildrenChanged map.");
                    return;
                }

                String id = (String) change.get("uid");
                if (TextUtils.equals((String) change.get("action"), "insert")) {
                    getOrInflateComponentWithUniqueId(id);
                } else if (TextUtils.equals((String) change.get("action"), "remove")) {
                    Component toRemove = mAplComponents.get(id);
                    if (toRemove == null) {
                        Log.w(TAG, "Invalid component to remove in kPropertyNotifyChildrenChanged, ignoring.");
                    } else {
                        APLLayout.traverseComponentHierarchy(toRemove, child -> mAplComponents.remove(child.getComponentId()));
                    }
                }
            }
        }

        mViewPresenter.onComponentChange(component, dirtyProperties);
        mAplTrace.endTrace();
    }

    /**
     * Builds an event object;
     *
     * @param nativeHandle The native peer to the event.
     * @param typeId       The event type;
     * @return the created event
     */
    @VisibleForTesting
    public Event buildEvent(long nativeHandle, int typeId) {

        EventType type = valueOf(typeId);
        switch (type) {
            case kEventTypeControlMedia:
                return ControlMediaEvent.create(nativeHandle, this);
            case kEventTypeReinflate:
                return ReinflateEvent.create(nativeHandle, this);
            case kEventTypeFocus:
                return FocusEvent.create(nativeHandle, this);
            case kEventTypeOpenURL:
                return OpenURLEvent.create(nativeHandle, this,
                        mOptions.getOpenUrlCallback());
            case kEventTypePlayMedia:
                return PlayMediaEvent.create(nativeHandle, this);
            case kEventTypePreroll:
                return PrerollEvent.create(nativeHandle, this, mOptions.getTtsPlayerProvider());
            case kEventTypeSendEvent:
                return SendEvent.create(nativeHandle, this,
                        mOptions.getSendEventCallbackV2());
            case kEventTypeExtension:
                return ExtensionEvent.create(nativeHandle, this,
                        mOptions.getExtensionEventCallback());
            case kEventTypeSpeak:
                return SpeakEvent.create(nativeHandle, this,
                        mOptions.getTtsPlayerProvider());
            case kEventTypeRequestFirstLineBounds:
                return RequestFirstLineBounds.create(nativeHandle, this);
            case kEventTypeFinish:
                return FinishEvent.create(nativeHandle, this, mOptions.getOnAplFinishCallback());
            case kEventTypeDataSourceFetchRequest:
                return DataSourceFetchEvent.create(
                        nativeHandle,
                        this,
                        mOptions.getDataSourceFetchCallback());
            case kEventTypeOpenKeyboard:
                return OpenKeyboardEvent.create(nativeHandle, this);
            case kEventTypeMediaRequest:
                return LoadMediaRequestEvent.create(nativeHandle, this, mOptions.getImageProvider());
            case kEventTypeRequestLineBounds:
                return RequestLineBoundsEvent.create(nativeHandle, this);
            case kEventTypeLineHighlight:
                return LineHighlightEvent.create(nativeHandle, this);
        }

        return null;
    }


    /**
     * Handles the event sent by the core.
     *
     * @param eventHandle Handle to the native event.
     * @param eventType   The type of event
     */
    @SuppressWarnings("unused")
    private void callbackHandleEvent(long eventHandle, int eventType) {
        Event event = buildEvent(eventHandle, eventType);
        event.execute();
    }

    /**
     * Adds a BoundObject that is pending execution. This is used to ensure that Events and Actions are not GC'd before Core
     * needs to terminate them.
     * @param object an bound object that is pending resolution.
     */
    void addPending(BoundObject object) {
        mPending.add(object);
    }

    /**
     * Remove an BoundObject that was pending resolution. This enables Events and Actions to be cleaned up correctly
     * by GC.
     * @param object the bound object that was resolved or terminated.
     */
    void removePending(BoundObject object) {
        mPending.remove(object);
    }

    /**
     * Notify the runtime with visual and dataSourceContext
     */
    public void notifyContext() {
        notifyVisualContext();
        notifyDataSourceContext();
    }
    /**
     * Notify visual context.
     * For now, we rely only on core specifying the visual context is dirty and call this during the frame loop.
     * <p>
     * TODO consider moving this to a worker thread with the payload String if it is too expensive.
     */
    private void notifyVisualContext() {
        mLastVisualContextUpdateTime = System.currentTimeMillis();
        try {
            mOptions.getVisualContextListener()
                    .onVisualContextUpdate(
                            new JSONObject(serializeVisualContext())
                    );
        } catch (JSONException e) {
            Log.wtf(TAG, "Error serializing visual context object.", e);
        }
    }

    /**
     * Notify DataSource context.
     * For now, we rely only on core specifying the data source context is dirty and call this during the frame loop.
     * Works on same logic as visual context, so any update on visual context may also apply on data source context.
     *                            */
    private void notifyDataSourceContext() {
        mLastDataSourceUpdateTime = System.currentTimeMillis();
        try {
            mOptions.getDataSourceContextListener()
                    .onDataSourceContextUpdate(
                            new JSONArray(serializeDataSourceContext())
                    );
        } catch (JSONException e) {
            Log.wtf(TAG, "Error serializing dataSource context object.", e);
        }
    }

    /**
     * Finds a component by the common name assigned in the APL document.  This method traverses
     * the Component list and is therefore slower than {@link #getOrInflateComponentWithUniqueId(String)}.
     *
     * @param id The common name assigned to the Component in the APL document.
     * @return The Component, null if the name does not exist.
     */
    @Nullable
    public Component findComponentById(@NonNull String id) {
        for (Component component : mAplComponents.values()) {
            if (id.equals(component.getId()))
                return component;
        }
        return null;
    }

    /**
     * Create the document context. This method is called from  the APLView during onLayout().
     */
    @UiThread
    native long nCreate(long contentHandle, long rootConfigHandle, long textMeasureHandle,
                        int width, int height, int dpi, int shape, String theme, int mode);

    /**
     * @return The component at the root of the APL Component Hierarchy.
     */
    @Nullable
    @SuppressWarnings("WeakerAccess")
    public Component getTopComponent() {
        String top = nGetTopComponent(getNativeHandle());
        if (top != null) {
            return mAplComponents.get(top);
        }
        return null;
    }

    /**
     * @return the collection of Components in this RootContext.
     */
    @NonNull
    @VisibleForTesting
    public Map<String, Component> getComponents() {
        return mAplComponents;
    }


    /**
     * @return The number of components.
     */
    public int getComponentCount() {
        return mAplComponents.size();
    }


    /**
     * Start the frame choreographer.  This should be called when the views are ready.
     */
    public void initTime() {
        mStartLoopTime = 0;
        mIsFinished.set(false);
        resumeDocument();
    }

    /**
     * Updates an APL component.
     * This method is called from the native layer during {@link #nHandleDirtyProperties(long)} ()}.
     *
     * @param componentId     The handle to the native peer.
     * @param dirtyProperties The {@link com.amazon.apl.enums.PropertyKey properties} that have been updated.
     */
    @SuppressWarnings("unused")
    private void callbackUpdateComponent(String componentId, int[] dirtyProperties) {
        Component component = mAplComponents.get(componentId);
        if (component != null) {
            onComponentChange(component, false, createPropertyKeyListFromIntArray(dirtyProperties));
        }
    }

    private static List<PropertyKey> createPropertyKeyListFromIntArray(int[] dirtyProperties) {
        List<PropertyKey> list = new LinkedList<>();
        for (int property : dirtyProperties) {
            list.add(PropertyKey.valueOf(property));
        }
        return list;
    }


    /**
     * Cancels all currently executing commands
     */
    public void cancelExecution() {
        nCancelExecution(getNativeHandle());
    }


    /**
     * Post work to be done on the frame thread
     *
     * @param r The work.
     */
    public void post(Runnable r) {
        mWorkQueue.add(r);
    }

    /**
     * Identify if there is a status change for screen lock and post updates if required.
     *
     * @param screenLocked Core screen lock state.
     */
    private void processScreenLock(boolean screenLocked) {
        final boolean hasPlayingMedia = mRenderingContext.getMediaPlayerProvider().hasPlayingMediaPlayer();
        final boolean screenLockStatus = screenLocked || hasPlayingMedia;
        if (lastScreenLockStatus != screenLockStatus) {
            lastScreenLockStatus = screenLockStatus;
            mOptions.getScreenLockListener().onScreenLockChange(screenLockStatus);
        }
    }

    /**
     * APL Core relies on operations to be performed in particular way.
     * Order and set of operations in this method should be preserved.
     * Order is the following:
     * * Update time and adjust TimeZone if required.
     * * Call **clearPending** method on RootConfig to give Core possibility to execute all pending actions and updates.
     * * Process dirty properties. Side note: this can inflate Views which are required by an Event in the same frame.
     * * Process requested events.
     * * Send time update to AudioPlayers for speech
     * * Check and set screenlock if required.
     * * check for data source errors.
     *
     * @param time system time in milliseconds.
     */
    private void coreFrameUpdate(long time) {
        long nativeHandle = getNativeHandle();

        long now = System.currentTimeMillis();

        mAplTrace.startTrace(TracePoint.ROOT_CONTEXT_UPDATE_TIME);
        updateTime(nativeHandle, time, now);
        mAplTrace.endTrace();

        mAplTrace.startTrace(TracePoint.ROOT_CONTEXT_CLEAR_PENDING);
        nClearPending(nativeHandle);
        mAplTrace.endTrace();

        mAplTrace.startTrace(TracePoint.ROOT_CONTEXT_HANDLE_DIRTY_PROPERTIES);
        nHandleDirtyProperties(nativeHandle);
        mAplTrace.endTrace();

        mAplTrace.startTrace(TracePoint.ROOT_CONTEXT_HANDLE_EVENTS);
        nHandleEvents(nativeHandle);
        mAplTrace.endTrace();

        processScreenLock(nIsScreenLocked(nativeHandle));

        checkDataSourceErrors(nativeHandle);

        mAplTrace.startTrace(TracePoint.ROOT_CONTEXT_NOTIFY_VISUAL_CONTEXT);
        if (nIsVisualContextDirty(nativeHandle) && now - mLastVisualContextUpdateTime >= VISUAL_CONTEXT_UPDATE_INTERVAL_MS) {
            notifyVisualContext();
        }
        mAplTrace.endTrace();

        mAplTrace.startTrace(TracePoint.ROOT_CONTEXT_NOTIFY_DATA_SOURCE_CONTEXT);
        if (nIsDataSourceContextDirty(nativeHandle) && now - mLastDataSourceUpdateTime >= DATA_SOURCE_CONTEXT_UPDATE_INTERVAL_MS) {
            notifyDataSourceContext();
        }
        mAplTrace.endTrace();
    }

    private void checkDataSourceErrors(final long nativeHandle) {
        final Object errors = nGetDataSourceErrors(nativeHandle);
        if (errors != null) {
            mOptions.getDataSourceErrorCallback().onDataSourceError(errors);
        }
    }

    /**
     * Called when a new display frame is being rendered.
     * See {@link IClock.IClockCallback#onTick(long)}
     *
     * @param frameTimeNanos The time in nanoseconds when the frame started being rendered,
     *                       in the {@link System#nanoTime()} timebase.
     */
    @Override
    public void onTick(long frameTimeNanos) {
        try {
            mAplTrace.startTrace(TracePoint.ROOT_CONTEXT_DO_FRAME);
            if (mStartLoopTime == 0) {
                // Use the elapsed time from core if it was set
                mStartLoopTime = frameTimeNanos - getElapsedTime() * 1000000;
            } else if (mIsFinished.get()) {
                return;
            }


            //do any work that's pending
            while (!mWorkQueue.isEmpty()) {
                Runnable r = mWorkQueue.poll();
                r.run();
            }

            // convert to ms
            long time = (frameTimeNanos - mStartLoopTime) / 1000000;

            coreFrameUpdate(time);

            final long end = System.nanoTime();
            final long doFrameTime = end - frameTimeNanos;

            if (doFrameTime > TARGET_DO_FRAME_TIME && mTelemetryProvider != null) {
                if(!mIsFinished.get()){
                    mTelemetryProvider.incrementCount(cDropFrame);
                }
            }
        } catch (Exception e) {
            // mTelemetryProvider may be null if the document has been finished.
            if (mTelemetryProvider != null) {
                mTelemetryProvider.incrementCount(cDoFrameFail);
            }

            throw e;
        } finally {
            mAplTrace.endTrace();
        }
    }

    /**
     * Get's the elapsed time since the document was displayed.
     *
     * @return the elapsed time in milliseconds.
     */
    long getElapsedTime() {
        return nElapsedTime(getNativeHandle());
    }

    /**
     * Returns whether or not the setting was set in the document.
     *
     * @param propertyName the name of the setting.
     * @return true if the setting was set in the document,
     * false otherwise.
     */
    @Deprecated
    public boolean hasSetting(String propertyName) {
        return nSetting(getNativeHandle(), propertyName) != null;
    }

    /**
     * Gets the setting value stored in the APL Document or the fallback value if it wasn't set.
     *
     * @param propertyName the value in the apl document
     * @param defaultValue the fallback value if it is not set
     * @param <K>          the type of the expected setting value
     * @return the setting value
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    public <K> K optSetting(String propertyName, K defaultValue) {
        Object value = nSetting(getNativeHandle(), propertyName);
        // If the value isn't found or doesn't match the default value's class, then fallback
        if (value == null || (defaultValue != null && value.getClass() != defaultValue.getClass())) {
            return defaultValue;
        }
        return (K) value;
    }

    /**
     * @return The calculated pixel width after scaling
     */
    public int getPixelWidth() {
        return mMetricsTransform.getScaledViewhostWidth();
    }

    /**
     * @return The calculated pixel height after scaling
     */
    public int getPixelHeight() {
        return mMetricsTransform.getScaledViewhostHeight();
    }

    /**
     * Sends an update message to the focused component when a key is pressed.
     *
     * @param keyboard The keyboard message.
     * @return true if keyboard was processed, false otherwise.
     */
    boolean handleKeyboard(APLKeyboard keyboard) {
        boolean isAplConsumed =
                nHandleKeyboard(getNativeHandle(), keyboard.type().getIndex(),
                        keyboard.code(),
                        keyboard.key(),
                        keyboard.repeat(),
                        keyboard.shift(),
                        keyboard.alt(),
                        keyboard.ctrl(),
                        keyboard.meta());

        if (DEBUG) Log.d(TAG, "keyboard: " + keyboard + ", isAplConsumed: " + isAplConsumed);
        return isAplConsumed;
    }

    /**
     * ask Core to switch focus to the next element in a direction. Core will most likely send a FocusEvent.
     *
     * @param focusDirection direction of movement
     * @return boolean if a next element has been found
     */
    boolean nextFocus(FocusDirection focusDirection) {
        return nNextFocus(getNativeHandle(), focusDirection.getIndex());
    }

    /**
     * Force APL to release focus. Always succeeds
     */
    void clearFocus() {
        nClearFocus(getNativeHandle());
    }

    /**
     * Get a list of all focusable Areas from APLCore
     *
     * @return Hash were the keys are the identifiers from corecomponents, and the value is a float array of [x,y,width,height]
     */
    LinkedHashMap<String, float[]> getFocusableAreas() {
        return nGetFocusableAreas(getNativeHandle());
    }

    /**
     * Sets focus to an element in core
     *
     * @param direction focus movement direction
     * @param x         coordinate of origin
     * @param y         coordinate of origin
     * @param width     width of origin
     * @param height    height of origin
     * @param target_id targetId ID of area selected by runtime from list provided by getFocusableAreas()
     * @return true if focus was accepted, false otherwise
     */
    boolean setFocus(FocusDirection direction, float x, float y, float width, float height, String target_id) {
        return nSetFocus(getNativeHandle(),
                direction.getIndex(),
                x,
                y,
                width,
                height,
                target_id);
    }

    /**
     * set focus to a component
     *
     * @param component component to focus
     * @return true if focus was accepted, false otherwise
     */
    boolean setFocus(Component component) {
        return setFocus(component,
                FocusDirection.kFocusDirectionNone);
    }

    /**
     * set focus to a component in a certain direction of origin
     *
     * @param component origin component
     * @param direction direction of movement
     * @return true if focus was accepted, false otherwise
     */
    boolean setFocus(Component component, FocusDirection direction) {
        Rect bounds = component.getBounds();
        return setFocus(direction,
                bounds.getTop(),
                bounds.getLeft(),
                bounds.getWidth(),
                bounds.getHeight(),
                component.getComponentId()
        );
    }

    /**
     * Handle a pointer event.
     *
     * @param pointer the pointer.
     * @return true if pointer was processed, false otherwise.
     */
    @VisibleForTesting
    public boolean handlePointer(@NonNull final Pointer pointer) {
        return nHandlePointerEvent(getNativeHandle(),
                pointer.getId(),
                pointer.getPointerType().getIndex(),
                pointer.getPointerEventType().getIndex(),
                mMetricsTransform.toCore(pointer.getX()),
                mMetricsTransform.toCore(pointer.getY()));
    }

    /**
     * Creates a {@link ConfigurationChange} object with values filled with current ViewportMetrics and RootConfig values.
     * This needs to be used by Runtime to fill new values, which are available with Runtime.
     *
     * @return {@link ConfigurationChange}
     */
    public ConfigurationChange.Builder createConfigurationChange() {
        return ConfigurationChange.create(mMetricsTransform.getUnscaledMetrics(), getRootConfig());
    }

    /**
     * Handle a configuration change.
     *
     * @param configurationChange the {@link ConfigurationChange}
     */
    public void handleConfigurationChange(@NonNull final ConfigurationChange configurationChange) {
        if (DEBUG) Log.d(TAG, "On Configuration Change: " + configurationChange);

        // create new viewport profile
        ViewportMetrics oldMetrics = mViewPresenter.getOrCreateViewportMetrics();
        ViewportMetrics newMetrics = ViewportMetrics.builder()
                .width(configurationChange.width())
                .height(configurationChange.height())
                .dpi(oldMetrics.dpi())
                .shape(oldMetrics.shape())
                .theme(configurationChange.theme())
                .mode(oldMetrics.mode())
                .scaling(oldMetrics.scaling())
                .build();

        // Run scaling algo and update its references with this new MetricsTransform object.
        mMetricsTransform = MetricsTransform.create(newMetrics);
        mTextMeasureCallback.setMetricsTransform(mMetricsTransform);
        mRenderingContext.setMetricsTransform(mMetricsTransform);

        AbstractMediaPlayerProvider oldMediaPlayerProvider = mRenderingContext.getMediaPlayerProvider();
        AbstractMediaPlayerProvider newMediaPlayerProvider = getMediaPlayerProvider(configurationChange.disallowVideo(), mOptions);
        if (oldMediaPlayerProvider != newMediaPlayerProvider) {
            oldMediaPlayerProvider.releasePlayers();
            mRenderingContext.setMediaPlayerProvider(newMediaPlayerProvider);
        }

        // Notify Core about new config changes.
        // If there is auto-scaling, use scaled metrics.
        ViewportMetrics scaledMetrics = mMetricsTransform.getScaledMetrics();
        Log.d(TAG, "handleConfigurationChange. metrics: " + scaledMetrics);

        nHandleConfigurationChange(getNativeHandle(),
                scaledMetrics.width(),
                scaledMetrics.height(),
                scaledMetrics.theme(),
                scaledMetrics.mode().getIndex(),
                configurationChange.fontScale(),
                configurationChange.screenMode().getIndex(),
                configurationChange.screenReaderEnabled(),
                configurationChange.disallowVideo(),
                configurationChange.environmentValues());

        // If we have a scaled viewport and we're undergoing a configuration change,
        // then our metrics are changing and the existing layouts need to be cleared and re-laid out.
        // We may get a further optimization here by checking if the scaled metrics are the same as
        // before (i.e. oldMetrics.equals(scaledMetrics)).
        if (scaledMetrics.scaling().isScalingRequested()) {
            Log.d(TAG, "Handling a configuration change due to scaling.");
            mRenderingContext.getTextLayoutFactory().clear();
            APLLayout.traverseComponentHierarchy(getTopComponent(), mViewPresenter::requestLayout);
        }
    }

    /**
     * Inform document about its display state.
     *
     * @param displayState the {@link DisplayState}
     */
    public void updateDisplayState(final DisplayState displayState) {
        if (DEBUG) Log.d(TAG, "Update Display State: " + displayState.name());

        nUpdateDisplayState(getNativeHandle(), displayState.getIndex());

        // When the display state changes, we want to ensure that the document is notified promptly
        // and that any resulting events are processed immediately, rather than waiting for the next
        // tick of the frame loop. We do this because another tick of the frame loop is not
        // guaranteed, particularly not in the "background" or "hidden" states, in which case the
        // runtime may reduce the frequency of the ticks or stop ticking entirely.
        nHandleEvents(getNativeHandle());
    }

    /**
     * Do not use, inject this dependency directly.
     * break dependency on root context.
     *
     * @return the View context responsible for display.
     */
    @SuppressWarnings("DeprecatedIsStillUsed")
    @Deprecated
    @VisibleForTesting
    public IAPLViewPresenter getViewPresenter() {
        //TODO remove this method and inject dependency, otherwise callers maintain
        //TODO an unwanted dependency on rootContext and the presenter
        //TODO only in use by Events
        return mViewPresenter;
    }

    /**
     * Testing use only.
     * This method advances the core time without processing changes from core.  It allows
     * us to pre-evaluate pending changes in tests. After calling this method a test can
     * call doFrame(time) to perform regular frame processing of pending changes.
     */
    @VisibleForTesting
    public void testUtil_updateFrameTime(long time) {
        nUpdateFrameTime(getNativeHandle(), time);
    }


    private static native void nUpdateFrameTime(long nativeHandle, long time);

    /**
     * Check to see if the context has pending changes.
     * This method is called inside the JNI layer during doFrame updates,
     * it is exposed here for completeness and testing.
     *
     * @return true if the RootContext is dirty.
     */
    public boolean isDirty() {
        return nIsDirty(getNativeHandle());
    }

    /**
     * Returns a Java Component with the specified Component id, inflating one if necessary.
     *
     * Inflates a Component hierarchy rooted at the Component with given {@link Component#getComponentId()}.
     * Inflation in this context means creating and binding Java Component objects to their
     * corresponding Core Component objects. This method does not inflate any android Views.
     *
     * @param componentId The unique id of the Component to inflate
     */
    public Component getOrInflateComponentWithUniqueId(final String componentId) {
        Component component = mAplComponents.get(componentId);
        if (component != null) {
            return component;
        }
        nInflateComponentWithUniqueId(getNativeHandle(), componentId);
        return mAplComponents.get(componentId);
    }

    /**
     * Checks if this method is currently being called by the Ui Thread. Logs an error message if
     * being called from other than the Ui Thread. We should switch to throwing an exception or
     * pushing calls to the Ui Thread in the future.
     */
    private void checkUiThread() {
        if (Looper.getMainLooper() != Looper.myLooper()) {
            if (BuildConfig.DEBUG) throw new NotOnUiThreadError("Not on the main thread");
            Log.wtf(TAG, "Not on the main thread");
        }
    }

    /**
     * @return the visual context
     */
    private String serializeVisualContext() {
        return JNIUtils.safeStringValues(nSerializeVisualContext(getNativeHandle()));
    }

    /**
     * @return the data source context
     */
    private String serializeDataSourceContext() {
        return nSerializeDataSourceContext(getNativeHandle());
    }

    /**
     * @return the id of the currently focused component or the empty string if no component is focused.
     */
    public String getFocusedComponentId() {
        return nGetFocusedComponent(getNativeHandle());
    }

    /**
     * Notifies core that the media has loaded, for the given source url.
     * @param source The url of the media that was requested.
     */
    public void mediaLoaded(final String source) {
        nMediaLoaded(getNativeHandle(), source);
    }

    /**
     * Notifies core that the media has failed to load, for the given source url.
     * @param source The url that was requested by core
     * @param errorCode An error code defined by the runtimes, that is passed to the onFail callback
     * @param failureReason An error message that is passed to the onFail callback
     */
    public void mediaLoadFailed(final String source, int errorCode, String failureReason) {
        nMediaLoadFailed(getNativeHandle(), source, errorCode, failureReason);
    }

    /**
     * The agent name for this document as specified in {@link RootConfig#create(String, String)}.
     * @return the agent name
     */
    public String getAgentName() {
        return mAgentName;
    }

    // Native methods

    private static native boolean nIsDirty(long nativeHandle);

    private static native Object nSetting(long nativeHandle, String settingName);

    private static native boolean nIsScreenLocked(long nativeHandle);

    private native void nInflate(long nativeHandle);

    private native boolean nReinflate(long nativeHandle);

    private native void nInflateComponentWithUniqueId(long nativeHandle, String componentId);

    private native void nClearPending(long nativeHandle);

    /**
     * Requests outstanding events be sent to the rendering layer.  This results in a call
     * to updateEvent()
     */
    @UiThread
    private native void nHandleEvents(long nativeHandle);

    private static native void nCancelExecution(long nativeHandle);

    private static native long nExecuteCommands(long nativeHandle, String commands);

    private static native long nInvokeExtensionEventHandler(long nativeHandle,
                                                            String uri, String name, Map<String, Object> data, boolean fastmode);

    private static native void nScrollToRectInComponent(long nativeHandle, long componentHandle, int x, int y, int w, int h, int align);

    private static native String nGetVersionCode(long nativeHandle);

    /**
     * Update dirty Component properties.
     * This results in core calling back to {@link #callbackUpdateComponent(String, int[])}.
     * NOTE
     * The update loop could move to this view host and the v.h. call into jni.
     * However multiple values are needed to create a component, this would require
     * a jni call per value, wrapping the collection of params in a complex object,
     * or instantiating the view host Component via JNI calls to constructor in
     * these cases it's less performance overhead to have the v.h. initiate callbacks
     */
    @UiThread
    private native void nHandleDirtyProperties(long nativeHandle);

    private static native boolean nHandleKeyboard(long nativeHandle, int keyHandlerType, String code, String key, boolean repeat, boolean shiftKey, boolean altKey, boolean ctrlKey, boolean metaKey);

    private static native boolean nHandlePointerEvent(long nativeHandle, int pointerId, int pointerType, int pointerEventType, float x, float y);

    private static native void nHandleConfigurationChange(long nativeHandle, int width, int height, String theme, int viewportMode, float fontScale, int screenMode, boolean screenReaderEnabled, boolean disallowVideo, Map<String, Object> environmentValues);

    private static native void nUpdateDisplayState(long nativeHandle, int displayState);

    private static native boolean nUpdateDataSource(long nativeHandle, String type, String payload);

    private static native Object nGetDataSourceErrors(long nativeHandle);

    private static native long nElapsedTime(long nativeHandle);

    private static native boolean nIsVisualContextDirty(long nativeHandle);

    private static native boolean nIsDataSourceContextDirty(long nativeHandle);

    private static native String nSerializeVisualContext(long nativeHandle);

    private static native String nSerializeDataSourceContext(long nativeHandle);

    private static native boolean nNextFocus(long nativeHandle, int focusDirection);

    private static native void nClearFocus(long nativeHandle);

    private static native LinkedHashMap<String, float[]> nGetFocusableAreas(long nativeHandle);

    private static native boolean nSetFocus(long nativeHandle, int focus_direction, float origin_x, float origin_y, float origin_width, float origin_height, String targetId);

    private static native String nGetFocusedComponent(long nativeHandle);

    private static native void nMediaLoaded(long nativeHandle, String url);

    private static native void nMediaLoadFailed(long nativeHandle, String url, int errorCode, String error);
}
