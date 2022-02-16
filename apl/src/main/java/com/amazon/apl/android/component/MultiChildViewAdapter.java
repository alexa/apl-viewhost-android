/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.component;

import android.content.Context;
import android.view.View;

import com.amazon.apl.android.APLVersionCodes;
import com.amazon.apl.android.Component;
import com.amazon.apl.android.IAPLViewPresenter;
import com.amazon.apl.android.MultiChildComponent;
import com.amazon.apl.android.utils.APLTrace;
import com.amazon.apl.android.utils.TracePoint;
import com.amazon.apl.android.views.APLAbsoluteLayout;
import com.amazon.apl.enums.ComponentType;
import com.amazon.apl.enums.PropertyKey;
import com.amazon.apl.enums.ScrollDirection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MultiChildViewAdapter<C extends MultiChildComponent> extends ComponentViewAdapter<C, APLAbsoluteLayout> {
    private static final String TAG = "MultiChildAdapter";
    private static MultiChildViewAdapter INSTANCE;

    public static MultiChildViewAdapter getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new MultiChildViewAdapter();
        }

        return INSTANCE;
    }

    MultiChildViewAdapter() {
        super();
        putPropertyFunction(PropertyKey.kPropertyNotifyChildrenChanged, this::onChildrenChanged);
        putPropertyFunction(PropertyKey.kPropertyScrollPosition, this::updateScrollPosition);
    }


    @Override
    public APLAbsoluteLayout createView(Context context, IAPLViewPresenter presenter) {
        return new APLAbsoluteLayout(context, presenter);
    }

    @Override
    public void applyAllProperties(C component, APLAbsoluteLayout layout) {
        super.applyAllProperties(component, layout);
        if (component.hasProperty(PropertyKey.kPropertyScrollPosition)) {
            int scrollPosition = component.getProperties().getDimension(PropertyKey.kPropertyScrollPosition).intValue();
            ScrollDirection scrollDirection = ScrollDirection.valueOf(component.getProperties().getEnum(PropertyKey.kPropertyScrollDirection));
            layout.updateScrollPosition(scrollPosition, scrollDirection);
        }
    }

    @Override
    void applyPadding(MultiChildComponent component, APLAbsoluteLayout layout) {
        // No need to calculate padding as it is calculated by layout positioning.
        // Furthermore, padding causes clipping that is incompatible with Flexbox layouts.
    }

    public void onChildrenChanged(MultiChildComponent component, APLAbsoluteLayout layout) {
        APLTrace trace = component.getViewPresenter().getAPLTrace();
        trace.startTrace(TracePoint.COMPONENT_ON_CHILDREN_CHANGED);
        final List<View> viewsToRemove = getViewsToRemove(component, layout);
        for (View viewToRemove : viewsToRemove) {
            layout.removeViewInLayout(viewToRemove);
        }
        
        onDisplayedChildrenChanged(component, layout);
        trace.endTrace();
    }

    private List<View> getViewsToRemove(MultiChildComponent component, APLAbsoluteLayout layout) {
        List<View> viewsToRemove = new ArrayList<>();
        Object[] changes = component.getProperties().get(PropertyKey.kPropertyNotifyChildrenChanged);
        Map<String, View> childrenComponentMap = null;
        for (Object change : changes) {
            if (!(change instanceof Map)) {
                continue;
            }
            Map changeMap = (Map) change;
            String action = (String) changeMap.get("action");
            if ("remove".equals(action)) {
                if (childrenComponentMap == null) {
                    //lazy initialization to create children map only when remove action is involved
                    childrenComponentMap = getChildrenComponentMapping(component, layout);
                }
                String componentId = (String) changeMap.get("uid");
                if (childrenComponentMap.containsKey(componentId)) {
                    viewsToRemove.add(childrenComponentMap.get(componentId));
                }
            }
        }

        return viewsToRemove;
    }

    private Map<String, View> getChildrenComponentMapping(MultiChildComponent component, APLAbsoluteLayout layout) {
        IAPLViewPresenter presenter = component.getViewPresenter();
        Map<String, View> childrenComponentMap = new HashMap<>(layout.getChildCount());
        for (int i = 0; i < layout.getChildCount(); i++) {
            View childView = layout.getChildAt(i);
            Component child = presenter.findComponent(childView);
            if (child != null) {
                childrenComponentMap.put(child.getComponentId(), childView);
            }
        }
        return childrenComponentMap;
    }

    /**
     * Updates the children in a MultiChildComponent.
     * This inflates any new views and attaches them to their parent {@link APLAbsoluteLayout}.
     *
     * @param component the component with children.
     * @param layout    the layout for that component.
     */
    public void onDisplayedChildrenChanged(MultiChildComponent component, APLAbsoluteLayout layout) {
        final IAPLViewPresenter presenter = component.getViewPresenter();
        boolean isAtMostAPL15 = component.getRenderingContext().getDocVersion() <= APLVersionCodes.APL_1_5;
        List<Component> children;
        if (isAtMostAPL15 &&
                (component.getComponentType() == ComponentType.kComponentTypeContainer
                 || component.getComponentType() == ComponentType.kComponentTypeTouchWrapper)) {
            // Preserve a corner case where some documents only rendered correctly due to a bug with
            // clipping regions being selectively applied prior to APL 1.6.
            children = component.getAllChildren();
        } else {
            children = component.getDisplayedChildren();
        }
        List<View> childViews = new ArrayList<>(layout.getChildCount());
        // If the counts are different, then we changed
        boolean changed = layout.getChildCount() != children.size();
        for (int i = 0; i < layout.getChildCount(); i++) {
            View child = layout.getChildAt(i);
            childViews.add(child);
            // If at least one child is different then we've changed.
            if (!changed) {
                changed |= presenter.findView(children.get(i)) != child;
            }
        }

        if (!changed) {
            /**
             * on newer devices there is tearing when an object is transformed/ jittering when animated.
             * This invalidate is a performance hit vs. just doing the early return.
             * It will be revisited in quieter times for a potential performance gain.
             */
            layout.invalidate();
            return;
        }

        layout.detachAllViews();

        // Avoiding this call will keep children in memory for a Sequence/GridSequence component but leave them
        // unattached to the ViewHierarchy
        ComponentType componentType = component.getComponentType();
        if (componentType != ComponentType.kComponentTypeGridSequence &&
                componentType != ComponentType.kComponentTypeSequence) {
            for (View childView : childViews) {
                Component childComponent = presenter.findComponent(childView);
                if (!children.contains(childComponent)) {
                    layout.removeDetachedView(childView);
                }
            }
        }

        for (Component child : children) {
            View childView = presenter.findView(child);
            if (childView == null) {
                childView = presenter.inflateComponentHierarchy(child);
            }

            if (childView.getParent() == null) {
                layout.attachView(childView);
            }
        }

        layout.requestLayout();
        layout.invalidate();
    }

    private void updateScrollPosition(MultiChildComponent component, APLAbsoluteLayout layout) {
        onDisplayedChildrenChanged(component, layout);
        int scrollPosition = component.getProperties().getDimension(PropertyKey.kPropertyScrollPosition).intValue();
        ScrollDirection scrollDirection = ScrollDirection.valueOf(component.getProperties().getEnum(PropertyKey.kPropertyScrollDirection));
        layout.updateScrollPosition(scrollPosition, scrollDirection);
    }

}
