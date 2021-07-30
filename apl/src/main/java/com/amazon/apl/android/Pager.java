/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import android.view.View;

import com.amazon.apl.enums.Navigation;
import com.amazon.apl.enums.PropertyKey;

import java.util.List;

public class Pager extends MultiChildComponent {

    Pager(long nativeHandle, String componentId, @NonNull RenderingContext renderingContext) {
        super(nativeHandle, componentId, renderingContext);
    }

    @Override
    public List<Component> getDisplayedChildren() {
        List<Component> displayedChildren = super.getDisplayedChildren();
        // edge case for if Pager has no items in it
        if (displayedChildren.size() == 0) {
            if (getChildCount() == 0) {
                return displayedChildren;
            } else {
                // Some documents use opacity of zero to indicate a page is not displayed
                // Core interprets this item as not in the list of displayedChildren
                // so we add it in here.
                displayedChildren.add(getChildAt(getCurrentPage()));
            }
        }

        // This ensures that each child in the pager that should be displayed has the correct visibility
        // set on the view.
        for (Component child : displayedChildren) {
            child.setInvisibleOverride(false);
        }

        final Navigation navigation = getNavigation();
        if (navigation == Navigation.kNavigationNone) {
            return displayedChildren;
        }

        final int currentPage = getCurrentPage();
        if (navigation == Navigation.kNavigationForwardOnly && currentPage < getChildCount() - 1) {
            // Add the child to the right but make it invisible.
            Component rightChild = getChildAt(currentPage + 1);
            if (rightChild != null && !displayedChildren.contains(rightChild)) {
                rightChild.setInvisibleOverride(true);
                displayedChildren.add(rightChild);
            }
            return displayedChildren;
        }

        final boolean isWrap = navigation == Navigation.kNavigationWrap;
        int leftIdx = currentPage - 1;
        if (leftIdx < 0) {
            leftIdx = isWrap ? getChildCount() - 1 : 0;
        }

        int rightIdx = currentPage + 1;
        if (rightIdx >= getChildCount()) {
            rightIdx = isWrap ? 0 : getChildCount() - 1;
        }

        // Add the child to the left but make it invisible.
        Component leftChild = getChildAt(leftIdx);
        if (leftChild != null && !displayedChildren.contains(leftChild)) {
            leftChild.setInvisibleOverride(true);
            displayedChildren.add(leftChild);
        }

        // Add the child to the right but make it invisible.
        Component rightChild = getChildAt(rightIdx);
        if (rightChild != null && !displayedChildren.contains(rightChild)) {
            rightChild.setInvisibleOverride(true);
            displayedChildren.add(rightChild);
        }

        return displayedChildren;
    }

    public int getCurrentPage() {
        return mProperties.getInt(PropertyKey.kPropertyCurrentPage);
    }

    public Navigation getNavigation() {
        return Navigation.valueOf(mProperties.getEnum(PropertyKey.kPropertyNavigation));
    }

    static class PagerAccessibilityDelegate extends APLAccessibilityDelegate<Pager> {
        public PagerAccessibilityDelegate(Pager c, Context context) {
            super(c, context);
        }

        @Override
        public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfoCompat info) {
            super.onInitializeAccessibilityNodeInfo(host, info);

            int currentPage = mComponent.getCurrentPage();
            Navigation navigation = mComponent.getNavigation();
            int totalPages = mComponent.getChildCount();
            if (totalPages > 1) {
                switch (navigation) {
                    case kNavigationWrap:
                        info.addAction(AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_SCROLL_FORWARD);
                        info.addAction(AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_SCROLL_BACKWARD);
                        break;
                    case kNavigationNormal:
                        if (currentPage < totalPages - 1) {
                            info.addAction(AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_SCROLL_FORWARD);
                        }
                        if (currentPage > 0) {
                            info.addAction(AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_SCROLL_BACKWARD);
                        }
                        break;
                    case kNavigationForwardOnly:
                        if (currentPage < totalPages - 1) {
                            info.addAction(AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_SCROLL_FORWARD);
                        }
                }
            }
        }

        @Override
        public boolean performAccessibilityAction(View host, int action, Bundle args) {
            if (action == AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD || action == AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD) {
                int value = action == AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD ? 1 : -1;
                String setPageCommand = "[{\"type\": \"SetPage\", \"componentId\": \"" + mComponent.getComponentId() + "\", \"position\": \"relative\", \"value\": " + value + "}]";
                mComponent.getRootContext().executeCommands(setPageCommand);
                return true;
            }

            return super.performAccessibilityAction(host, action, args);
        }
    }
}
