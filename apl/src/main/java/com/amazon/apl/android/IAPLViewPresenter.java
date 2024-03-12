/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.amazon.apl.android.bitmap.IBitmapFactory;
import com.amazon.apl.android.configuration.ConfigurationChange;
import com.amazon.apl.android.functional.Consumer;
import com.amazon.apl.android.providers.AbstractMediaPlayerProvider;
import com.amazon.apl.android.providers.ITelemetryProvider;
import com.amazon.apl.android.scaling.Scaling;
import com.amazon.apl.android.scaling.ViewportMetrics;
import com.amazon.apl.android.shadow.ShadowBitmapRenderer;
import com.amazon.apl.android.utils.APLTrace;
import com.amazon.apl.enums.PropertyKey;
import com.amazon.apl.enums.UpdateType;

import java.util.List;

/**
 * The APL View Context.  Responsible for building Views and displaying.
 */
public interface IAPLViewPresenter extends View.OnClickListener, IDocumentLifecycleListener, ViewGroup.OnHierarchyChangeListener {

    // TODO not yet a true Presenter
    // TODO this class represents architectural migration from tightly coupled
    // TODO Components and Views.  Goals: 1) create a layer of abstraction between
    // TODO the logical models and views, 2) assume responsibility for view creation
    // TODO 3) extract the relationship between model and view

    /**
     * @return The Android Context.
     */
    Context getContext();

    /**
     * Notification that a change has been made to the components.
     *
     * @param component The Component that has changed.
     * @param dirtyProperties The list of properties that have changed (marked as dirty by Core)
     */
    void onComponentChange(Component component, List<PropertyKey> dirtyProperties);

    /**
     * Binds all properties of a given Component to it's associated view.
     *
     * @param component The Component to bind
     */
    void applyAllProperties(Component component, View view);

    /**
     * Request the view to be laid out.
     *
     * @param component the component
     */
    void requestLayout(Component component);

    /**
     * Find the View for a Component.
     *
     * @param component The APL Component.s
     * @return the View associated with the component, can be null;
     */
    @Nullable
    View findView(Component component);

    /**
     * Finds the Component represented by the View.
     *
     * @param view The view.
     * @return The Component.
     */
    @Nullable
    Component findComponent(View view);

    /**
     * Associates a given Component and given View, meaning that the View will rendered to represent
     * the Component it's associated with. This method does NOT inflate or bind views, only records
     * that the View visually represents the Component.
     * This method also disassociates any previous Component that View was already associated with.
     */
    void associate(Component component, View view);

    /**
     * Disassociates any View currently bound to given Component, meaning the given component is no
     * longer being rendered to the screen.
     * Calls to {@link #findView(Component)} with the same Component after calling this method will
     * return null until a new view is associated with it.
     *
     * See {@link #associate(Component, View)}
     *
     * @param component The component whose view will be disassociated
     */
    void disassociate(Component component);

    void disassociate(View view);

    /**
     * Create and return the viewport metrics.
     *
     * Note:
     * When reusing APLLayout across multiple documents, it is imperative to call this method,
     * otherwise the metrics will not be initialized.
     *
     */
    @NonNull
    ViewportMetrics getOrCreateViewportMetrics() throws IllegalStateException;


    /**
     * Sends an key update to the {@link RootContext}.
     *
     * @param event the motion event.
     * @return true if key was consumed, false otherwise.
     */
    boolean onKeyPress(@NonNull final KeyEvent event);

    /**
     * A context is being prepared, a call to onDocumentRender will follow when
     * the context is ready. Call this method to start performance timers prior to render.
     */
    void preDocumentRender();

    /**
     * Returns an instance of {@link ShadowBitmapRenderer} for drawing shadows under Components
     */
    ShadowBitmapRenderer getShadowRenderer();

    /**
     * Inflates a hierarchy of Components from given root Component in their corresponding Android
     * Views by calling
     * {@link com.amazon.apl.android.component.ComponentViewAdapter#createView(Context, IAPLViewPresenter)}
     * for each Component.
     *
     * @param root The root Component in the given Component hierarchy.
     */
     View inflateComponentHierarchy(Component root);

    /**
     * Convenience method for visiting all Components in a given hierarchy via a Breadth-First Search.
     *
     * @param root The root of the hierarchy to traverse.
     * @param visitorOperation A Consumer function that will be called once for each Component reached
     *                         during traversal of the hierarchy. This function takes a single
     *                         Component as input
     */
    void traverseComponentHierarchy(Component root, Consumer<Component> visitorOperation);

    /**
     * Updates the layout parameters for given View based on Component bounds. Also adds the View
     * to the parent layout if necessary. This method should be called whenever Component bounds
     * have changed.
     *
     * @param component The Component associated with the given View.
     * @param view The View whose layout will be updated
     */
    void updateViewInLayout(Component component, View view);

    void loadBackground(Content.DocumentBackground bg);

    /**
     * The metrics provider;
     */
    ITelemetryProvider telemetry();

    /**
     * Handle a motion event.
     *
     * This event should be using window coordinates as APLLayout will translate the event
     * according to its location in window. See {@link View#getLocationInWindow(int[])}. Most Activities
     * should be able to just call this directly by overriding dispatchTouchEvent like so:
     *
     * {@code
     *  @Override
     *  public boolean dispatchTouchEvent(MotionEvent ev) {
     *      return mAplLayout.getPresenter().handleTouchEvent(ev) || super.dispatchTouchEvent(ev);
     *  }
     * }
     *
     * @param event the motion event.
     */
    boolean handleTouchEvent(@NonNull final MotionEvent event);

    /**
     * Send an explicit {@link com.amazon.apl.enums.PointerEventType#kPointerCancel} event to notify
     * that further touch events are being processed by Android.
     *
     * See https://developer.amazon.com/en-US/docs/alexa/alexa-presentation-language/apl-touchable-component.html#oncancel
     *
     * Typically this is called in {@link APLLayout#requestDisallowInterceptTouchEvent(boolean)}.
     */
    void cancelTouchEvent();

    IBitmapFactory getBitmapFactory();

    /**
     * Adds a listener for document lifecycle events.
     * @param documentLifecycleListener the listener.
     */
    void addDocumentLifecycleListener(@NonNull IDocumentLifecycleListener documentLifecycleListener);

    void updateComponent(View componentView, UpdateType updateType, boolean value);

    void updateComponent(View componentView, UpdateType updateType, int value);

    void updateComponent(View componentView, UpdateType updateType, String value);

    long getElapsedTime();

    ConfigurationChange getConfigurationChange();

    /**
     * Updates the Scaling reference.
     * @param scaling the scaling
     */
    void setScaling(Scaling scaling);

    /**
     * Clear display state and views from the layout.
     */
    void clearLayout();

    /**
     * Release the last received motion event to Android.
     */
    void releaseLastMotionEvent();

    /**
     * Trigger a pass to reinflate the view hierarchy for the current RootContext.
     * This should be done in response to a {@link com.amazon.apl.android.events.ReinflateEvent}.
     */
    void reinflate();

    AbstractMediaPlayerProvider<View> getMediaPlayerProvider();

    /**
     * Notifies core that the media related to the url has been loaded successfully.
     * @param url The url of the media object that has loaded
     */
    void mediaLoaded(String url);

    /**
     * Notifies core that there was an error loading the media object.
     * @param url The url of the media object that failed to load.
     * @param errorCode The error code (typically HttpStatusCode) that was received, if any.
     *                  This is defined by the runtimes, and is not defined in the spec.
     * @param errorMessage A generic error message
     */
    void mediaLoadFailed(String url, int errorCode, String errorMessage);

    @Nullable
    APLTrace getAPLTrace();

    boolean isHardwareAccelerationForVectorGraphicsEnabled();
}
