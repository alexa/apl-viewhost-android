/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android;

import android.graphics.Matrix;
import android.graphics.RectF;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import android.util.Log;

import com.amazon.apl.android.primitive.AccessibilityActions;
import com.amazon.apl.android.primitive.Dimension;
import com.amazon.apl.android.primitive.Rect;
import com.amazon.apl.android.providers.ITelemetryProvider;
import com.amazon.apl.android.scaling.IMetricsTransform;
import com.amazon.apl.android.utils.AccessibilitySettingsUtil;
import com.amazon.common.BoundObject;
import com.amazon.apl.enums.ComponentType;
import com.amazon.apl.enums.Display;
import com.amazon.apl.enums.LayoutDirection;
import com.amazon.apl.enums.PropertyKey;
import com.amazon.apl.enums.Role;
import com.amazon.apl.enums.UpdateType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.nio.charset.StandardCharsets;

import static com.amazon.apl.android.providers.ITelemetryProvider.APL_DOMAIN;
import static com.amazon.apl.android.providers.ITelemetryProvider.Type.COUNTER;
import static com.amazon.apl.android.providers.ITelemetryProvider.UNKNOWN_METRIC_ID;

/**
 * APL logical representation of a View.
 */
public abstract class Component extends BoundObject {

    private final static String TAG = "Component";

    private final static boolean DEBUG = false;

    private static AccessibilitySettingsUtil sAccessibilitySettingsUtil = AccessibilitySettingsUtil.getInstance();

    private final static String METRIC_COMPONENT_NULL = TAG + ".uninflated_component_in_core";

    /**
     * The root context that created this component.
     * TODO component should not have access to RootContext, it is currently
     * TODO being used as a Component cache and Visual Context notifier
     * TODO component cache child and parent cache should be in this class for recycling,
     * TODO visual context should be APLOptions callback
     */
    @Deprecated
    RootContext mRootContext;

    /**
     * The Unique id of this APL component.
     */
    @NonNull
    private final String mComponentId;

    /**
     * The properties describing this object.
     */
    @NonNull
    public final PropertyMap<Component, PropertyKey> mProperties;

    private final RenderingContext mRenderingContext;

    private boolean mInvisibleOverride = false;

    /**
     * Component constructor.
     * @param nativeHandle      the native handle to bind
     * @param renderingContext  the rendering context
     */
    protected Component(long nativeHandle, @NonNull String componentId, @NonNull RenderingContext renderingContext) {
        bind(nativeHandle);
        mComponentId = componentId;
        mRenderingContext = renderingContext;
        mProperties = createPropertyMap();
    }

    protected PropertyMap createPropertyMap() {
        return new PropertyMap<Component, PropertyKey>() {
            @NonNull
            @Override
            public Component getMapOwner() {
                return Component.this;
            }

            @NonNull
            @Override
            public IMetricsTransform getMetricsTransform() {
                return getRenderingContext().getMetricsTransform();
            }
        };
    }

    /**
     * @return The collection of properties describing this Component..
     */
    @NonNull
    public final PropertyMap<Component, PropertyKey> getProperties() {
        return mProperties;
    }

    public IAPLViewPresenter getViewPresenter() {
        return mRootContext.getViewPresenter();
    }

    public final RenderingContext getRenderingContext() { return mRenderingContext; }

    /**
     * Sets whether this component's view should be set to invisible and notifies the view.
     * @param invisibleOverride true if the view should be invisible.
     */
    void setInvisibleOverride(boolean invisibleOverride) {
        if (mInvisibleOverride != invisibleOverride) {
            mInvisibleOverride = invisibleOverride;
            getViewPresenter().onComponentChange(this, Collections.singletonList(PropertyKey.kPropertyDisplay));
        }
    }

    /**
     * @return true if the view for this component should be invisible.
     */
    public boolean isInvisibleOverride() {
        return mInvisibleOverride;
    }

    /**
     * @return if a Component is clickable for the accessibility framework.
     */
    public boolean isClickable() {
        final ComponentType componentType = getComponentType();
        return !isDisabled() && isFocusable()
                && (componentType == ComponentType.kComponentTypeTouchWrapper || componentType == ComponentType.kComponentTypeVectorGraphic);
    }

    public boolean isFocusableInTouchMode() {
        return false;
    }

    /**
     * Get the child Component at a specific index.
     *
     * @param index The child index.
     * @return The child Component.
     */
    @Nullable
    public Component getChildAt(int index) {
        if (BuildConfig.DEBUG && (index >= getChildCount())) throw new AssertionError();

        String childId = getChildId(index);
        return getChildById(childId);
    }


    /**
     * Get the child Component by Component ID.  Note the ComponentID is a locally
     * cached version of the Component UniqueId
     *
     * @param componentId The Component ID.
     * @return The child component, may be null.
     */
    public Component getChildById(String componentId) {
        //  TODO cache children in this class
        Component child = mRootContext.getOrInflateComponentWithUniqueId(componentId);
        if (child != null) {
            return child;
        }
        Log.e(TAG, this + ". getChildById returned null for id: " + componentId);
        return null;
    }

    /**
     * Get the parent Component of this Component.
     *
     * @return The parent component, may be null.
     */
    @Nullable
    public Component getParent() {
        return mRootContext.getOrInflateComponentWithUniqueId(getParentId());
    }


    /**
     * @return The type of the component.
     */
    @NonNull
    final public ComponentType getComponentType() {
        return ComponentType.valueOf(nGetType(getNativeHandle()));
    }

    /**
     * The Component identifier.  This is a cached value of the Component uniqueID from core.
     *
     * @return The Component unique identifier;
     */
    @NonNull
    public String getComponentId() {
        return mComponentId;
    }

    /**
     * This value is cached in the {@link #mComponentId} value. Use {@link #getComponentId()}.
     *
     * @return the unique ID assigned to this component by the system.
     */
    @NonNull
    final public String getUniqueId() {
        return nGetUniqueId(getNativeHandle());
    }

    /**
     * @return the uniqueID of the parent();
     */
    public final String getParentId() {
        return nGetParentId(getNativeHandle());
    }

    /**
     * @return the uniqueID of the parent();
     */
    public final ComponentType getParentType() {
        return ComponentType.valueOf(nGetParentType(getNativeHandle()));
    }

    /**
     * @return The ID assigned to this component by the APL author, if not assigned the empty string.
     */
    @NonNull
    final public String getId() {
        return nGetId(getNativeHandle());
    }

    // TODO: Remove the following update methods once all components start using the same methods in IAPLViewPresenter
    /**
     * Change the state of the component.  This may trigger a style change in
     * this component or a descendant.
     *
     * @param updateType The type of update
     * @param value      The new value of the state property.
     */
    public void update(@NonNull UpdateType updateType, int value) {
        if (updateType == UpdateType.kUpdateScrollPosition) {
            value = Math.round(getRenderingContext().getMetricsTransform().toCore(value));
        }
        if (DEBUG) Log.d(TAG, "updateType: " + updateType + ", " + "value: " + value);
        nUpdate(getNativeHandle(), updateType.getIndex(), value);
    }

    public void update(@NonNull UpdateType updateType, String value) {
        nUpdate(getNativeHandle(), updateType.getIndex(), (value + "\0").getBytes(StandardCharsets.UTF_8));
    }

    public void update(@NonNull UpdateType updateType, boolean value) {
        update(updateType, value ? 1 : 0);
    }

    /**
     * The component gained or lose Focus.
     */
    public void focus(boolean hasFocus) {
        update(UpdateType.kUpdateTakeFocus, hasFocus);
    }

    /**
     * @return the layout direction
     */
    public LayoutDirection getLayoutDirection() {
        return LayoutDirection.valueOf(mProperties.getEnum(PropertyKey.kPropertyLayoutDirection));
    }

    /**
     * @return an array of Maps containing changed children.
     */
    public Object[] getChangedChildren() {
        return mProperties.get(PropertyKey.kPropertyNotifyChildrenChanged);
    }

    /**
     * @return Voice-over will read this string when the user selects this component
     */
    @Nullable
    public final String getAccessibilityLabel() {
        return mProperties.getString(PropertyKey.kPropertyAccessibilityLabel);
    }

    /**
     * @return Programmatic equivalents for complex touch interactions
     */
    @NonNull
    public final AccessibilityActions getAccessibilityActions() {
        return mProperties.getAccessibilityActions(PropertyKey.kPropertyAccessibilityActions);
    }

    /**
     * @return If true, this component has the checked state set.
     */
    public final boolean isChecked() {
        return mProperties.getBoolean(PropertyKey.kPropertyChecked);
    }

    /**
     * @return If true, this component does not respond to touch or focus.
     */
    public final boolean isDisabled() {
        return mProperties.getBoolean(PropertyKey.kPropertyDisabled);
    }

    /**
     * @return Control if the component is displayed on the screen.
     */
    public final Display getDisplay() {
        return Display.valueOf(mProperties.getEnum(PropertyKey.kPropertyDisplay));
    }

    /**
     * @return Opacity of this component and children.
     */
    public float getOpacity() {
        return mProperties.getFloat(PropertyKey.kPropertyOpacity);
    }

    /**
     * Returns a list of all children, whether visible or not.
     *
     * @return a list of all children of this component
     */
    public List<Component> getAllChildren() {
        int count = getChildCount();
        List<Component> children = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            Component component = getChildAt(i);
            if (component != null) {
                children.add(component);
            }
        }
        return children;
    }

    /**
     * Gets the count of children for an APL component.
     *
     * @return number of direct children.
     */
    final public int getChildCount() {
        return nGetChildCount(getNativeHandle());
    }


    /**
     * Gets the id of a child component based on the child index.
     *
     * @param index the index of the child.
     * @return The identifier of the component child.
     */
    @NonNull
    final public String getChildId(int index) {
        return nGetChildId(getNativeHandle(), index);
    }

    /**
     * Returns a list of all displayed Children.
     * @return
     */
    public List<Component> getDisplayedChildren() {
        int count = getDisplayedChildCount();
        List<Component> children = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            Component component = getDisplayedChildAt(i);
            if (component != null) {
                children.add(component);
            }
        }
        return children;
    }

    public int getDisplayedChildCount() {
        return nGetDisplayedChildCount(getNativeHandle());
    }

    public String getDisplayedChildId(int index) {
        return nGetDisplayedChildId(getNativeHandle(), index);
    }

    public Component getDisplayedChildAt(int index) {
        return getChildById(getDisplayedChildId(index));
    }

    /**
     * Ensure that a component has a layout.  This is used for children of a
     * Sequence to allow lazy creation.
     */
    final public void ensureLayout() {
        nEnsureLayout(getNativeHandle());
    }

    /**
     * The component hierarchy signature is a unique text string that represents the type
     * of this component and all of the components below it in the hierarchy.  This signature
     * in mainly intended for use in recycling views where native layouts are re-used for
     * new component hierarchies.
     *
     * @return A unique signature representing the layout of components in a hierarchy.
     */
    @NonNull
    public final String getHierarchySignature() {
        return nGetHierarchySignature(getNativeHandle());
    }

    /**
     * @return The component bounds in DP.
     */
    public final Rect getBounds() {
        return mProperties.getRect(PropertyKey.kPropertyBounds);
    }

    /**
     * @return true if the component has a transform applied, that is not an identity transform.
     */
    public final boolean hasTransform() {
        return mProperties.hasTransform();
    }

    /**
     * @return The component's 2D transform.
     */
    public final Matrix getTransform() {
        return mProperties.getScaledTransform(PropertyKey.kPropertyTransform);
    }

    /**
     * @return The component inner bounds in DP. This is the bounds minus padding.
     */
    public final Rect getInnerBounds() {
        return mProperties.getRect(PropertyKey.kPropertyInnerBounds);
    }

    /**
     * @return Indicates if component can directly receive input from touch, cursor, or keyboard events.
     *         NOTE: A Component may have Android Focus (for purpose of remote or keyboard navigation)
     *               but not be APLFocusable. An example of this is a Sequence whose children are not
     *               one of the Actionable Components, Android still puts focus on these children
     *               when scrolling through the list via Remote, but the Sequence will still have
     *               Focus from Core/APL perspective not the children.
     */
    public boolean isFocusable() {
        return mProperties.getBoolean(PropertyKey.kPropertyFocusable);
    }

    /**
     * @return The horizontal offset of the Component shadow in pixels.
     */
    public int getShadowOffsetHorizontal() {
        Dimension top = mProperties.getDimension(PropertyKey.kPropertyShadowHorizontalOffset);
        return top == null ? 0 : top.intValue();
    }

    /**
     * @return The vertical offset of the Component shadow in pixels.
     */
    public int getShadowOffsetVertical() {
        Dimension offset = mProperties.getDimension(PropertyKey.kPropertyShadowVerticalOffset);
        return offset == null ? 0 : offset.intValue();
    }

    /**
     * @return The blur radius of the Component shadow in pixels.
     */
    public int getShadowRadius() {
        Dimension radius = mProperties.getDimension(PropertyKey.kPropertyShadowRadius);
        return radius == null ? 0 : radius.intValue();
    }

    /**
     * @return The Component shadow color. Defaults to transparent.
     */
    public int getShadowColor() {
        return mProperties.getColor(PropertyKey.kPropertyShadowColor);
    }

    /**
     * Returns shadow corner radius as array: [topLeft, topRight, bottomRight, bottomLeft]
     */
    public float[] getShadowCornerRadius() {
        return new float[] {0f, 0f, 0f, 0f};
    }

    /**
     * Determines whether a box shadow is drawn around the bounds of this Component.
     * Subclasses should override this and return false if they want to render their own custom
     * shadow. E.g. Text draws shadows around each character instead of around the bounding box
     * of the entire Component
     *
     * @return true if the standard box shadow should be drawn around Component, else false
     */
    public boolean shouldDrawBoxShadow() {
        return getShadowOffsetHorizontal() != 0
                || getShadowOffsetVertical() != 0
                || getShadowRadius() != 0;
    }

    /**
     * The bounding Rect of the Shadow, excluding the blur radius, relative to the parent
     */
    public RectF getShadowRect() {
        final Rect bounds = getBounds();

        final int offsetX = getShadowOffsetHorizontal();
        final int offsetY = getShadowOffsetVertical();

        return new RectF(bounds.getLeft() + offsetX, bounds.getTop() + offsetY, bounds.getRight() + offsetX, bounds.getBottom() + offsetY);
    }

    public boolean isLaidOut() {
        return mProperties.getBoolean(PropertyKey.kPropertyLaidOut);
    }

    /**
     * Testing use only.
     *
     * @param property The Component property.
     * @return True if the Component property is dirty
     */
    @VisibleForTesting
    public boolean checkDirtyProperty(PropertyKey property) {
        return nCheckDirtyProperty(getNativeHandle(), property.getIndex());
    }

    /**
     * Testing use only.
     *
     * @return True if this Component has dirty properties.
     */
    @VisibleForTesting
    public boolean checkDirty() {
        return nCheckDirty(getNativeHandle());
    }

    /**
     * @return Role or purpose of the component
     */
    public Role getRole() {
        return Role.valueOf(mProperties.getEnum(PropertyKey.kPropertyRole));
    }

    public boolean hasProperty(PropertyKey propertyKey) {
        return mProperties.hasProperty(propertyKey);
    }

    @Deprecated
    @VisibleForTesting
    public RootContext getRootContext() {
        return mRootContext;
    }

    @NonNull
    private static native String nGetUniqueId(long nativeHandle);

    @NonNull
    private static native String nGetId(long nativeHandle);

    private static native int nGetChildCount(long nativeHandle);

    private static native int nGetDisplayedChildCount(long nativeHandle);

    private static native String nGetDisplayedChildId(long nativeHandle, int index);

    private static native String nGetParentId(long nativeHandle);

    private static native int nGetParentType(long nativeHandle);

    @NonNull
    private static native String nGetChildId(long nativeHandle, int index);

    private static native int nGetType(long nativeHandle);

    private static native void nEnsureLayout(long nativeHandle);

    @NonNull
    private static native String nGetHierarchySignature(long nativeHandle);

    private static native void nUpdate(long nativeHandle, int updateId, int value);

    private static native void nUpdate(long nativeHandle, int updateId, byte[] value);

    private static native boolean nCheckDirtyProperty(long nativeHandle, int propIndex);

    private static native boolean nCheckDirty(long nativeHandle);

    public String toString() {
        return "{type: " + getComponentType() + ", uid: " + getComponentId() + ", id: " + getId() + ", parent: " + getParentId() + "}";
    }
}
