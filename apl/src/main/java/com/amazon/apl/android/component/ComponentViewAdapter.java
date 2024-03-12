/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.component;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.core.view.ViewCompat;

import com.amazon.apl.android.APLAccessibilityDelegate;
import com.amazon.apl.android.BuildConfig;
import com.amazon.apl.android.Component;
import com.amazon.apl.android.IAPLViewPresenter;
import com.amazon.apl.android.functional.BiConsumer;
import com.amazon.apl.android.primitive.Rect;
import com.amazon.apl.android.utils.APLTrace;
import com.amazon.apl.android.utils.AccessibilitySettingsUtil;
import com.amazon.apl.android.utils.TracePoint;
import com.amazon.apl.enums.PropertyKey;
import com.amazon.apl.enums.Role;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Base class for ComponentViewAdapters.
 *
 * This class is responsible for creating Views, and applying component properties to them.
 *
 * @param <C> the Component class
 * @param <V> the View class
 */
public abstract class ComponentViewAdapter<C extends Component, V extends View> {
    private static final String TAG = "ComponentViewAdapter";
    private final Map<PropertyKey, BiConsumer<C,V>> mDynamicPropertyFunctionMap;
    private static AccessibilitySettingsUtil sAccessibilitySettingsUtil = AccessibilitySettingsUtil.getInstance();

    ComponentViewAdapter() {
        mDynamicPropertyFunctionMap = new HashMap<>();
        mDynamicPropertyFunctionMap.put(PropertyKey.kPropertyOpacity, this::applyAlpha);
        mDynamicPropertyFunctionMap.put(PropertyKey.kPropertyDisplay, this::applyDisplay);
        mDynamicPropertyFunctionMap.put(PropertyKey.kPropertyAccessibilityLabel, this::applyAccessibility);
        mDynamicPropertyFunctionMap.put(PropertyKey.kPropertyAccessibilityAdjustableValue, this::applyAccessibility);
        mDynamicPropertyFunctionMap.put(PropertyKey.kPropertyAccessibilityAdjustableRange, this::applyAccessibilityAdjustableRange);
        mDynamicPropertyFunctionMap.put(PropertyKey.kPropertyDisabled, this::applyDisabled);
        mDynamicPropertyFunctionMap.put(PropertyKey.kPropertyTransform, this::applyTransform);
        mDynamicPropertyFunctionMap.put(PropertyKey.kPropertyBounds, this::requestLayout);
        mDynamicPropertyFunctionMap.put(PropertyKey.kPropertyInnerBounds, this::requestLayout);
    }

    /**
     * Create a view for this Component.
     * @param context   the Android context
     * @param presenter the view presenter
     * @return          an Android view
     */
    public abstract V createView(Context context, IAPLViewPresenter presenter);

    /**
     * Apply all the component's properties to a view.
     *
     * Note: subclasses should override this method with their specific properties and call the super method.
     *
     * @param component the Component
     * @param view      the View
     */
    @CallSuper
    public void applyAllProperties(C component, V view) {
        applyAlpha(component, view);
        applyDisplay(component, view);
        applyAccessibility(component, view);
        applyDisabled(component, view);
        applyPadding(component, view);
    }

    /**
     * Update only this Component's dirty properties for a view.
     *
     * @param view the view to update
     * @param dirtyProperties a list of dirty properties to process
     */
    public void refreshProperties(C component, V view, List<PropertyKey> dirtyProperties) {
        Set<BiConsumer<C,V>> biConsumers = new HashSet<>();
        for (PropertyKey propertyKey : dirtyProperties) {
            BiConsumer<C,V> consumer = mDynamicPropertyFunctionMap.get(propertyKey);
            if (consumer != null) {
                biConsumers.add(consumer);
            } else if (BuildConfig.DEBUG) {
                Log.w(TAG, "Property function not implemented for: " + propertyKey + ".");
            }
        }

        // Use set to avoid duplicate calls
        for (BiConsumer<C,V> consumer : biConsumers) {
            consumer.accept(component,view);
        }
    }

    /**
     * Add a property-function mapping to the Adapter.
     * @param key       the property key
     * @param function  the function to call when the property is dirty
     */
    void putPropertyFunction(PropertyKey key, BiConsumer<C,V> function) {
        mDynamicPropertyFunctionMap.put(key, function);
    }

    private void applyTransform(C component, V view) {
        view.invalidate();
        ViewParent parent = view.getParent();
        if (parent instanceof View) {
            ((View)parent).invalidate();
        }
    }

    /**
     * Apply opacity to the view.
     * @param component the component
     * @param view      the view
     */
    private void applyAlpha(C component, V view) {
        if (component.hasProperty(PropertyKey.kPropertyOpacity)) {
            view.setAlpha(component.getOpacity());
        }
    }

    /**
     * Apply displayed to the view.
     * @param component the component
     * @param view      the view
     */
    private void applyDisplay(C component, V view) {
        int isDisplayed;
        switch (component.getDisplay()) {
            case kDisplayInvisible:
                isDisplayed = View.INVISIBLE;
                break;
            case kDisplayNone:
                isDisplayed = View.GONE;
                break;
            case kDisplayNormal:
            default:
                isDisplayed = View.VISIBLE;
                break;
        }

        if (component.isInvisibleOverride()) {
            isDisplayed = View.INVISIBLE;
        }

        if (view.getWindowVisibility() != isDisplayed || view.getVisibility() != isDisplayed) {
            view.setVisibility(isDisplayed);
        }
    }

    /**
     * Apply the accessibility label and delegate to the view.
     * @param component the component
     * @param view      the view
     */
    private void applyAccessibility(C component, V view) {
        if (!ViewCompat.hasAccessibilityDelegate(view)) {
            ViewCompat.setAccessibilityDelegate(view, APLAccessibilityDelegate.create(component, view.getContext()));
        }
        StringBuilder accessibility = new StringBuilder();
        String accessibilityLabel = component.getAccessibilityLabel();
        if (accessibilityLabel != null)
            accessibility.append(accessibilityLabel);

        if (checkAccessibilityAdjustableValue(component)) {
            String accessibilityAdjustableValue = component.getAccessibilityAdjustableValue();
            accessibility.append(" ").append(accessibilityAdjustableValue);
        }

        view.setContentDescription(accessibility.toString());

        applyFocusability(component, view);
    }

    /**
     * Send Accessibility event to AccessibilityDelegate to update properties.
     * @param component the component
     * @param view      the view
     */
    private void applyAccessibilityAdjustableRange(C component, V view) {
         if (checkAccessibilityAdjustableValue(component))
                return;
        if (!ViewCompat.hasAccessibilityDelegate(view)) {
            ViewCompat.setAccessibilityDelegate(view, APLAccessibilityDelegate.create(component, view.getContext()));
        } else {
            ViewCompat.getAccessibilityDelegate(view).sendAccessibilityEvent(view, AccessibilityEvent.CONTENT_CHANGE_TYPE_CONTENT_DESCRIPTION);
        }
    }

    /**
     * Apply the accessibility focus to the view.
     * @param component the component
     * @param view      the view
     */
    public void applyFocusability(C component, V view) {
        boolean focusable = component.isFocusable() && !component.isDisabled();
        boolean focusableInTouchMode = component.isFocusableInTouchMode();
        // TODO this setting should likely be put in APLAccessibilityDelegate
        boolean accessibilityAdjustableValue = checkAccessibilityAdjustableValue(component);
        boolean accessibilityContentDescription = !TextUtils.isEmpty(component.getAccessibilityLabel()) || accessibilityAdjustableValue;
        boolean focusableForAccessibility = (accessibilityContentDescription && sAccessibilitySettingsUtil.isScreenReaderEnabled(view.getContext()));

        view.setFocusable(focusable || focusableForAccessibility);
        view.setFocusableInTouchMode(focusableInTouchMode || focusableForAccessibility);
    }

     /**
     * check for accessibilityAdjustableValue property.
     * @param component the component
     * @return true if accessibilityAdjustableValue is present and not empty, else false
     */
    private boolean checkAccessibilityAdjustableValue(C component) {
        return component.getRole() == Role.kRoleAdjustable && component.hasProperty(PropertyKey.kPropertyAccessibilityAdjustableValue) &&
            component.getAccessibilityAdjustableValue() != null && !component.getAccessibilityAdjustableValue().isEmpty();
    }

    /**
     * Apply disabled state to the view.
     * @param component the component
     * @param view      the view
     */
    private void applyDisabled(C component, V view) {
        view.setEnabled(!component.isDisabled());
        final boolean isClickable = component.isClickable();
        // Order matters as setting the onClickListener always causes the component to be clickable
        view.setOnClickListener(isClickable ? component.getViewPresenter() : null);
        view.setClickable(isClickable);
        applyFocusability(component, view);
    }

    /**
     * Request the view to be laid out.
     * @param component the component
     * @param view      the view
     */
    @CallSuper
    public void requestLayout(C component, V view) {
        APLTrace trace = component.getViewPresenter().getAPLTrace();
        trace.startTrace(TracePoint.COMPONENT_REQUEST_LAYOUT);
        component.getViewPresenter().updateViewInLayout(component, view);
        applyPadding(component, view);
        trace.endTrace();
    }

    /**
     * Apply the padding for a component.  Components clipping and child positioning may require
     * padding implementation other than Android behavior. This method forces each component to
     * make a decision.
     * <p>
     * ViewGroups using absolute positioning are recommended to not use Android padding.
     * Android layout and Flexbox treat padding differently when the child
     * is larger than the parent. Flexbox allows the child to spill out the
     * bottom where Android clips the child by default. Since APL has already calculated bounds,
     * absolute layouts can ignore padding to prevent improper clipping.
     * <p>
     * ViewGroups not using absolute layout should call
     * {@link #setPaddingFromBounds(Component, View, boolean)} and give careful attention
     * to the clipping requirements. By default Android will clip the child.
     *
     * @param component the component
     * @param view      the view
     */
    abstract void applyPadding(C component, V view);

    /**
     * Apply padding to a View.  Values are calculated by subtracting the Component InnerBounds from
     * the Bounds.See {@link Component#getBounds()} and {@link Component#getInnerBounds()}.  ViewGroups
     * may clip child drawing to the the bounds.  Flexbox layouts typically do not.
     *
     * @param component             the component
     * @param view                  the view
     * @param clipChildrenToPadding whether to clip children
     */
    void setPaddingFromBounds(C component, @NonNull View view, boolean clipChildrenToPadding) {
        setPadding(component, view);
        if (view instanceof ViewGroup) {
            ((ViewGroup) view).setClipToPadding(clipChildrenToPadding);
        }
    }

    private void setPadding(C component, @NonNull View view) {
        Rect innerBounds = component.getInnerBounds();
        Rect bounds = component.getBounds();
        int right = bounds.intWidth() - innerBounds.intWidth() - innerBounds.intLeft();
        int bottom = bounds.intHeight() - innerBounds.intHeight() - innerBounds.intTop();
        // Padding is an absolute value defaulting to 0
        view.setPadding(innerBounds.intLeft(), innerBounds.intTop(), right, bottom);
    }

    @VisibleForTesting
    public void setsAccessibilitySettingsUtil(AccessibilitySettingsUtil accessibilitySettingsUtil) {
        sAccessibilitySettingsUtil = accessibilitySettingsUtil;
    }
}