/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.component;

import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import android.view.View;

import com.amazon.apl.android.APLAccessibilityDelegate;
import com.amazon.apl.android.Component;
import com.amazon.apl.android.Pager;
import com.amazon.apl.android.RootContext;
import com.amazon.apl.android.views.APLAbsoluteLayout;
import com.amazon.apl.enums.ComponentType;
import com.amazon.apl.enums.Navigation;

import org.junit.Test;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PagerViewAdapterTest extends AbstractComponentViewAdapterTest<Pager, APLAbsoluteLayout> {
    @Mock
    private Pager mPager;

    @Override
    Pager component() {
        return mPager;
    }

    List<Component> getChildren(int count) {
        List<Component> children = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Component mockChild = mock(Component.class);
            children.add(mockChild);
        }
        return children;
    }


    @Override
    void componentSetup() {
        when(mPager.getComponentType()).thenReturn(ComponentType.kComponentTypePager);
        when(mPager.getCurrentPage()).thenReturn(0);
        when(mPager.getNavigation()).thenReturn(Navigation.kNavigationNormal);
        when(mPager.getDisplayedChildren()).thenReturn(getChildren(1));
    }

    @Test
    public void test_accessibility_navigationNormal_noScrolling() {
        when(mPager.getChildCount()).thenReturn(1);

        AccessibilityNodeInfoCompat nodeInfoCompat = initializeNodeInfo();

        assertEquals(0, nodeInfoCompat.getActionList().size());
    }

    @Test
    public void test_accessibility_navigationNormal_scrollForwards() {
        when(mPager.getChildCount()).thenReturn(2);

        AccessibilityNodeInfoCompat nodeInfoCompat = initializeNodeInfo();

        assertEquals(1, nodeInfoCompat.getActionList().size());
        checkNodeInfoHasAction(nodeInfoCompat, AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_SCROLL_FORWARD);
    }

    @Test
    public void test_accessibility_navigationNormal_onlyScrollsBackwards() {
        when(mPager.getChildCount()).thenReturn(2);
        when(mPager.getCurrentPage()).thenReturn(1);

        AccessibilityNodeInfoCompat nodeInfoCompat = initializeNodeInfo();

        assertEquals(1, nodeInfoCompat.getActionList().size());
        checkNodeInfoHasAction(nodeInfoCompat, AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_SCROLL_BACKWARD);
    }

    @Test
    public void test_accessibility_navigationNormal_bothDirections() {
        when(mPager.getChildCount()).thenReturn(3);
        when(mPager.getCurrentPage()).thenReturn(1);

        AccessibilityNodeInfoCompat nodeInfoCompat = initializeNodeInfo();

        assertEquals(2, nodeInfoCompat.getActionList().size());
        checkNodeInfoHasAction(nodeInfoCompat, AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_SCROLL_FORWARD);
        checkNodeInfoHasAction(nodeInfoCompat, AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_SCROLL_BACKWARD);
    }

    @Test
    public void test_accessibility_navigationNone_noScrolling() {
        when(mPager.getChildCount()).thenReturn(3);
        when(mPager.getCurrentPage()).thenReturn(1);
        when(mPager.getNavigation()).thenReturn(Navigation.kNavigationNone);

        AccessibilityNodeInfoCompat nodeInfoCompat = initializeNodeInfo();

        assertEquals(0, nodeInfoCompat.getActionList().size());
    }

    @Test
    public void test_accessibility_navigationForward_noScrolling() {
        when(mPager.getChildCount()).thenReturn(2);
        when(mPager.getCurrentPage()).thenReturn(1);
        when(mPager.getNavigation()).thenReturn(Navigation.kNavigationForwardOnly);

        AccessibilityNodeInfoCompat nodeInfoCompat = initializeNodeInfo();

        assertEquals(0, nodeInfoCompat.getActionList().size());
    }

    @Test
    public void test_accessibility_navigationForward_withScrolling() {
        when(mPager.getChildCount()).thenReturn(2);
        when(mPager.getCurrentPage()).thenReturn(0);
        when(mPager.getNavigation()).thenReturn(Navigation.kNavigationForwardOnly);

        AccessibilityNodeInfoCompat nodeInfoCompat = initializeNodeInfo();

        assertEquals(1, nodeInfoCompat.getActionList().size());
        checkNodeInfoHasAction(nodeInfoCompat, AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_SCROLL_FORWARD);
    }

    @Test
    public void test_accessibility_navigationWrap_withScrolling() {
        when(mPager.getChildCount()).thenReturn(2);
        when(mPager.getCurrentPage()).thenReturn(0);
        when(mPager.getNavigation()).thenReturn(Navigation.kNavigationWrap);

        AccessibilityNodeInfoCompat nodeInfoCompat = initializeNodeInfo();

        assertEquals(2, nodeInfoCompat.getActionList().size());
        checkNodeInfoHasAction(nodeInfoCompat, AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_SCROLL_FORWARD);
        checkNodeInfoHasAction(nodeInfoCompat, AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_SCROLL_BACKWARD);
    }

    @Test
    public void test_accessibility_navigationWrap_noScrolling() {
        when(mPager.getChildCount()).thenReturn(1);
        when(mPager.getCurrentPage()).thenReturn(0);
        when(mPager.getNavigation()).thenReturn(Navigation.kNavigationWrap);

        AccessibilityNodeInfoCompat nodeInfoCompat = initializeNodeInfo();

        assertEquals(0, nodeInfoCompat.getActionList().size());
    }

    // TODO remove these tests
    @Test
    public void test_accessibility_scrollForwardsAction() {
        RootContext rootContext = mock(RootContext.class);
        when(mPager.getRootContext()).thenReturn(rootContext);
        when(mPager.getChildCount()).thenReturn(2);
        when(mPager.getCurrentPage()).thenReturn(0);
        when(mPager.getComponentId()).thenReturn("pager");

        APLAccessibilityDelegate aplAccessibilityDelegate = APLAccessibilityDelegate.create(mPager, getApplication());
        aplAccessibilityDelegate.performAccessibilityAction(mock(View.class), AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD, null);

        verify(rootContext).executeCommands(eq("[{\"type\": \"SetPage\", \"componentId\": \"pager\", \"position\": \"relative\", \"value\": 1}]"));
    }

    // TODO remove these tests
    @Test
    public void test_accessibility_scrollBackwardsAction() {
        RootContext rootContext = mock(RootContext.class);
        when(mPager.getRootContext()).thenReturn(rootContext);
        when(mPager.getChildCount()).thenReturn(2);
        when(mPager.getCurrentPage()).thenReturn(1);
        when(mPager.getComponentId()).thenReturn("pager");

        APLAccessibilityDelegate aplAccessibilityDelegate = APLAccessibilityDelegate.create(mPager, getApplication());
        aplAccessibilityDelegate.performAccessibilityAction(mock(View.class), AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD, null);

        verify(rootContext).executeCommands(eq("[{\"type\": \"SetPage\", \"componentId\": \"pager\", \"position\": \"relative\", \"value\": -1}]"));
    }
}