/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.component;

import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;

import android.graphics.RectF;
import android.view.View;

import com.amazon.apl.android.APLAccessibilityDelegate;
import com.amazon.apl.android.APLVersionCodes;
import com.amazon.apl.android.Component;
import com.amazon.apl.android.MultiChildComponent;
import com.amazon.apl.android.PropertyMap;
import com.amazon.apl.android.primitive.Dimension;
import com.amazon.apl.android.primitive.Rect;
import com.amazon.apl.android.shadow.ShadowBitmapKey;
import com.amazon.apl.android.utils.AccessibilitySettingsUtil;
import com.amazon.apl.android.views.APLAbsoluteLayout;
import com.amazon.apl.enums.ComponentType;
import com.amazon.apl.enums.PropertyKey;
import com.amazon.apl.enums.ScrollDirection;
import com.amazon.apl.enums.UpdateType;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;


public class MultiChildViewAdapterTest extends AbstractComponentViewAdapterTest<MultiChildComponent, APLAbsoluteLayout> {
    @Mock
    MultiChildComponent mockMultiChildComponent;
    @Mock
    APLAbsoluteLayout mockLayout;
    @Mock
    MultiChildViewAdapter mockMultiChildViewAdapter;
    @Mock
    View mockChildView1;
    @Mock
    Component mockChildComponent1;
    @Mock
    View mockChildView2;
    @Mock
    Component mockChildComponent2;
    @Mock
    PropertyMap<Component, PropertyKey> mockPropertyMap;

    @Override
    MultiChildComponent component() {
        return mockMultiChildComponent;
    }

    @Override
    void componentSetup() {
        mockMultiChildViewAdapter = MultiChildViewAdapter.getInstance();
        when(component().getProperties()).thenReturn(mockPropertyMap);
        when(component().isFocusable()).thenReturn(true);
    }


    @Test
    public void test_updateScrollPosition() {
        //  Arrange
        Dimension mockDimension = mock(Dimension.class);
        when(mockDimension.intValue()).thenReturn(10);
        when(mockPropertyMap.getDimension(PropertyKey.kPropertyScrollPosition)).thenReturn(mockDimension);
        when(component().hasProperty(PropertyKey.kPropertyScrollPosition)).thenReturn(true);
        when(mockPropertyMap.getEnum(PropertyKey.kPropertyScrollDirection)).thenReturn(1);

        //Act
        mockMultiChildViewAdapter.applyAllProperties(component(), mockLayout);

        //Assert
        verify(mockLayout).updateScrollPosition(10, ScrollDirection.kScrollDirectionHorizontal);
    }

    @Test
    public void test_onChildrenChanged_WithSingleRemoveAction() {

        //  Arrange
        Map<String,String> change = new HashMap<>();
        change.put("action", "remove");
        change.put("uid", "205");
        Map<String, String>[] changes = new HashMap[1];
        changes[0] = change;
        when(mockPropertyMap.get(PropertyKey.kPropertyNotifyChildrenChanged)).thenReturn(changes);
        when(mockLayout.getChildCount()).thenReturn(1);
        when(mockLayout.getChildAt(0)).thenReturn(mockChildView1);
        when(component().getViewPresenter().findComponent(mockChildView1)).thenReturn(mockChildComponent1);
        when(mockChildComponent1.getComponentId()).thenReturn("205");

        //Act
        mockMultiChildViewAdapter.onChildrenChanged(component(), mockLayout);

        //Assert
        verify(mockLayout).removeViewInLayout(mockChildView1);
    }

    @Test
    public void test_onChildrenChanged_cleans_shadow_references() {
        //  Arrange
        Component mockChildWithShadow1 = mock(Component.class);
        when(mockChildWithShadow1.getShadowOffsetHorizontal()).thenReturn(1);
        when(mockChildWithShadow1.getShadowOffsetVertical()).thenReturn(2);
        when(mockChildWithShadow1.getShadowRadius()).thenReturn(0);
        when(mockChildWithShadow1.getShadowRect()).thenReturn(new RectF(1, 1, 4, 4));
        when(mockChildWithShadow1.getShadowCornerRadius()).thenReturn(new float[]{0, 1, 2, 3});
        when(mockChildWithShadow1.getParent()).thenReturn(mockChildComponent1);
        ShadowBitmapKey key = new ShadowBitmapKey(mockChildWithShadow1);
        // Insert one item into shadow cache
        component().getViewPresenter().getShadowRenderer().getCache().putShadow(key, mockChildWithShadow1);
        assertTrue(component().getViewPresenter().getShadowRenderer().getCache().getComponents().hasNext());
        Map<String,String> change1 = new HashMap<>();
        change1.put("action", "remove");
        change1.put("uid", "205");
        Map<String,String> change2 = new HashMap<>();
        change2.put("action", "remove");
        change2.put("uid", "206");
        Map<String, String>[] changes = new HashMap[2];
        changes[0] = change1;
        changes[1] = change2;
        when(mockPropertyMap.get(PropertyKey.kPropertyNotifyChildrenChanged)).thenReturn(changes);
        when(mockLayout.getChildCount()).thenReturn(2);
        when(mockLayout.getChildAt(0)).thenReturn(mockChildView1);
        when(mockLayout.getChildAt(1)).thenReturn(mockChildView2);
        when(component().getViewPresenter().findComponent(mockChildView1)).thenReturn(mockChildComponent1);
        when(component().getViewPresenter().findComponent(mockChildView2)).thenReturn(mockChildComponent2);
        when(mockChildComponent1.getComponentId()).thenReturn("205");
        when(mockChildComponent2.getComponentId()).thenReturn("206");

        //Act
        mockMultiChildViewAdapter.onChildrenChanged(component(), mockLayout);

        //Assert
        verify(mockLayout).removeViewInLayout(mockChildView1);
        verify(mockLayout).removeViewInLayout(mockChildView2);
        verify(mockChildWithShadow1).setShadowBitmap(null);
        //Shadow cache should not have any item
        assertFalse(component().getViewPresenter().getShadowRenderer().getCache().getComponents().hasNext());
    }


    @Test
    public void test_onChildrenChanged_WithMultipleRemoveActions() {

        //  Arrange
        Map<String,String> change1 = new HashMap<>();
        change1.put("action", "remove");
        change1.put("uid", "205");
        Map<String,String> change2 = new HashMap<>();
        change2.put("action", "remove");
        change2.put("uid", "206");
        Map<String, String>[] changes = new HashMap[2];
        changes[0] = change1;
        changes[1] = change2;
        when(mockPropertyMap.get(PropertyKey.kPropertyNotifyChildrenChanged)).thenReturn(changes);
        when(mockLayout.getChildCount()).thenReturn(2);
        when(mockLayout.getChildAt(0)).thenReturn(mockChildView1);
        when(mockLayout.getChildAt(1)).thenReturn(mockChildView2);
        when(component().getViewPresenter().findComponent(mockChildView1)).thenReturn(mockChildComponent1);
        when(component().getViewPresenter().findComponent(mockChildView2)).thenReturn(mockChildComponent2);
        when(mockChildComponent1.getComponentId()).thenReturn("205");
        when(mockChildComponent2.getComponentId()).thenReturn("206");

        //Act
        mockMultiChildViewAdapter.onChildrenChanged(component(), mockLayout);

        //Assert
        verify(mockLayout).removeViewInLayout(mockChildView1);
        verify(mockLayout).removeViewInLayout(mockChildView2);
    }

    @Override
    @Test
    public void test_refresh_accessibility() {
        String accessibilityString = "accessibility";
        when(component().getAccessibilityLabel()).thenReturn(accessibilityString);
        setScreenReaderEnabled(true);

        refreshProperties(PropertyKey.kPropertyAccessibilityLabel);

        verify(component(), atLeastOnce()).getAccessibilityLabel();
        verify(component(), atLeastOnce()).getRole();
        verify(component()).isFocusable();
        verify(component(), atLeastOnce()).isDisabled();
        verify(component()).isFocusableInTouchMode();
        verifyNoMoreInteractions(component());

        assertEquals(accessibilityString, getView().getContentDescription());
        assertTrue(getView().isFocusable());
        assertTrue(getView().isFocusableInTouchMode());
    }

    @Test
    public void test_refresh_accessibility_disabled() {
        String accessibilityString = "accessibility";
        when(component().getAccessibilityLabel()).thenReturn(accessibilityString);
        when(component().isDisabled()).thenReturn(true);

        refreshProperties(PropertyKey.kPropertyAccessibilityLabel);

        verify(component(), atLeastOnce()).getAccessibilityLabel();
        verify(component(), atLeastOnce()).getRole();
        verify(component()).isFocusable();
        verify(component(), atLeastOnce()).isDisabled();
        verify(component()).isFocusableInTouchMode();
        verifyNoMoreInteractions(component());

        assertEquals(accessibilityString, getView().getContentDescription());
        assertFalse(getView().isFocusable());
        assertFalse(getView().isFocusableInTouchMode());
    }

    @Test
    public void test_accessibility_noScroll() {
        when(component().hasProperty(PropertyKey.kPropertyScrollPosition)).thenReturn(false);

        AccessibilityNodeInfoCompat nodeInfoCompat = initializeNodeInfo();

        assertEquals(0, nodeInfoCompat.getActionList().size());
    }

    @Test
    public void test_accessibility_vertical_scrollForwards() {
        setupChildWithBounds(Rect.builder().left(0).top(0).height(300).width(100).build());

        when(mockPropertyMap.hasProperty(PropertyKey.kPropertyScrollPosition)).thenReturn(true);
        when(mockPropertyMap.getEnum(PropertyKey.kPropertyScrollDirection)).thenReturn(ScrollDirection.kScrollDirectionVertical.getIndex());
        when(component().getAccessibilityActions()).thenReturn(mForwardOnlyAccessbilityActions);

        AccessibilityNodeInfoCompat nodeInfoCompat = initializeNodeInfo();

        assertEquals(1, nodeInfoCompat.getActionList().size());
        checkNodeInfoHasAction(nodeInfoCompat, AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_SCROLL_FORWARD);
    }

    @Test
    public void test_accessibility_vertical_scrollBoth() {
        setupChildWithBounds(Rect.builder().left(0).top(0).height(300).width(100).build());

        when(mockPropertyMap.getDimension(PropertyKey.kPropertyScrollPosition)).thenReturn(Dimension.create(50));
        when(mockPropertyMap.getEnum(PropertyKey.kPropertyScrollDirection)).thenReturn(ScrollDirection.kScrollDirectionVertical.getIndex());
        when(component().getAccessibilityActions()).thenReturn(mBothDirectionAccessbilityActions);

        AccessibilityNodeInfoCompat nodeInfoCompat = initializeNodeInfo();

        checkNodeInfoHasAction(nodeInfoCompat, AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_SCROLL_BACKWARD);
        checkNodeInfoHasAction(nodeInfoCompat, AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_SCROLL_FORWARD);
    }

    @Test
    public void test_accessibility_vertical_scrollBackwards() {
        setupChildWithBounds(Rect.builder().left(0).top(0).height(300).width(100).build());

        // height is 50
        when(mockPropertyMap.getDimension(PropertyKey.kPropertyScrollPosition)).thenReturn(Dimension.create(250));
        when(mockPropertyMap.getEnum(PropertyKey.kPropertyScrollDirection)).thenReturn(ScrollDirection.kScrollDirectionVertical.getIndex());
        when(component().getAccessibilityActions()).thenReturn(mBackwardOnlyAccessbilityActions);

        AccessibilityNodeInfoCompat nodeInfoCompat = initializeNodeInfo();

        assertEquals(1, nodeInfoCompat.getActionList().size());
        checkNodeInfoHasAction(nodeInfoCompat, AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_SCROLL_BACKWARD);
    }

    @Test
    public void test_accessibility_horizontal_scrollForwards() {
        setupChildWithBounds(Rect.builder().left(0).top(0).height(100).width(300).build());

        when(mockPropertyMap.getDimension(PropertyKey.kPropertyScrollPosition)).thenReturn(Dimension.create(0));
        when(mockPropertyMap.getEnum(PropertyKey.kPropertyScrollDirection)).thenReturn(ScrollDirection.kScrollDirectionHorizontal.getIndex());
        when(component().getAccessibilityActions()).thenReturn(mForwardOnlyAccessbilityActions);

        AccessibilityNodeInfoCompat nodeInfoCompat = initializeNodeInfo();

        assertEquals(1, nodeInfoCompat.getActionList().size());
        checkNodeInfoHasAction(nodeInfoCompat, AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_SCROLL_FORWARD);
    }

    @Test
    public void test_accessibility_horizontal_scrollBoth() {
        setupChildWithBounds(Rect.builder().left(0).top(0).height(100).width(300).build());

        when(mockPropertyMap.getDimension(PropertyKey.kPropertyScrollPosition)).thenReturn(Dimension.create(50));
        when(mockPropertyMap.getEnum(PropertyKey.kPropertyScrollDirection)).thenReturn(ScrollDirection.kScrollDirectionHorizontal.getIndex());
        when(component().getAccessibilityActions()).thenReturn(mBothDirectionAccessbilityActions);

        AccessibilityNodeInfoCompat nodeInfoCompat = initializeNodeInfo();

        checkNodeInfoHasAction(nodeInfoCompat, AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_SCROLL_FORWARD);
        checkNodeInfoHasAction(nodeInfoCompat, AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_SCROLL_BACKWARD);
    }

    @Test
    public void test_accessibility_horizontal_scrollBackwards() {
        setupChildWithBounds(Rect.builder().left(0).top(0).height(100).width(300).build());

        when(mockPropertyMap.getDimension(PropertyKey.kPropertyScrollPosition)).thenReturn(Dimension.create(100));
        when(mockPropertyMap.getEnum(PropertyKey.kPropertyScrollDirection)).thenReturn(ScrollDirection.kScrollDirectionHorizontal.getIndex());
        when(component().getAccessibilityActions()).thenReturn(mBackwardOnlyAccessbilityActions);

        AccessibilityNodeInfoCompat nodeInfoCompat = initializeNodeInfo();

        assertEquals(1, nodeInfoCompat.getActionList().size());
        checkNodeInfoHasAction(nodeInfoCompat, AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_SCROLL_BACKWARD);
    }

    @Test
    public void test_accessibility_actionScrollForwards() {
        setupChildWithBounds(Rect.builder().left(0).top(0).height(100).width(300).build());

        when(mockPropertyMap.getDimension(PropertyKey.kPropertyScrollPosition)).thenReturn(Dimension.create(0));
        when(mockPropertyMap.getEnum(PropertyKey.kPropertyScrollDirection)).thenReturn(ScrollDirection.kScrollDirectionHorizontal.getIndex());
        when(component().getAccessibilityActions()).thenReturn(mForwardOnlyAccessbilityActions);

        APLAccessibilityDelegate aplAccessibilityDelegate = APLAccessibilityDelegate.create(component(), getApplication());
        View view = mock(View.class);
        aplAccessibilityDelegate.onInitializeAccessibilityNodeInfo(view, AccessibilityNodeInfoCompat.obtain());
        aplAccessibilityDelegate.performAccessibilityAction(mock(View.class), AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD, null);

        verify(component()).update(eq(UpdateType.kUpdateAccessibilityAction), eq("scrollforward"));
    }

    @Test
    public void test_accessibility_actionScrollBackwards() {
        setupChildWithBounds(Rect.builder().left(0).top(0).height(100).width(400).build());

        when(mockPropertyMap.getDimension(PropertyKey.kPropertyScrollPosition)).thenReturn(Dimension.create(200));
        when(mockPropertyMap.getEnum(PropertyKey.kPropertyScrollDirection)).thenReturn(ScrollDirection.kScrollDirectionHorizontal.getIndex());
        when(component().getAccessibilityActions()).thenReturn(mBackwardOnlyAccessbilityActions);

        APLAccessibilityDelegate aplAccessibilityDelegate = APLAccessibilityDelegate.create(component(), getApplication());
        View view = mock(View.class);
        aplAccessibilityDelegate.onInitializeAccessibilityNodeInfo(view, AccessibilityNodeInfoCompat.obtain());
        aplAccessibilityDelegate.performAccessibilityAction(mock(View.class), AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD, null);

        verify(component()).update(eq(UpdateType.kUpdateAccessibilityAction), eq("scrollbackward"));
    }


    @Test
    public void test_refresh_displayedChildren_container_legacy() {
        MultiChildComponent mockChild = mock(MultiChildComponent.class);
        View childView = new View(getApplication());
        when(mockChild.getComponentType()).thenReturn(ComponentType.kComponentTypeFrame);
        when(mockChild.getBounds()).thenReturn(Rect.builder().width(10).height(10).left(0).top(0).build());

        when(component().getChildCount()).thenReturn(1);
        when(component().getAllChildren()).thenReturn(Arrays.asList(mockChild));
        when(component().getProperties()).thenReturn(mockPropertyMap);
        when(component().getRenderingContext().getDocVersion()).thenReturn(APLVersionCodes.APL_1_5);
        when(component().getComponentType()).thenReturn(ComponentType.kComponentTypeContainer);
        when(mockPropertyMap.get(PropertyKey.kPropertyNotifyChildrenChanged)).thenReturn(new Object[]{});
        when(mMockPresenter.inflateComponentHierarchy(mockChild)).thenReturn(childView);

        // Check that view has no children
        assertEquals(0, getView().getChildCount());

        refreshProperties(PropertyKey.kPropertyNotifyChildrenChanged);

        assertEquals(1, getView().getChildCount());
        assertEquals(childView, getView().getChildAt(0));
    }

    @Test
    public void test_refresh_displayedChildren_touchWrapper_legacy() {
        MultiChildComponent mockChild = mock(MultiChildComponent.class);
        View childView = new View(getApplication());
        when(mockChild.getComponentType()).thenReturn(ComponentType.kComponentTypeFrame);
        when(mockChild.getBounds()).thenReturn(Rect.builder().width(10).height(10).left(0).top(0).build());

        when(component().getChildCount()).thenReturn(1);
        when(component().getAllChildren()).thenReturn(Arrays.asList(mockChild));
        when(component().getProperties()).thenReturn(mockPropertyMap);
        when(component().getRenderingContext().getDocVersion()).thenReturn(APLVersionCodes.APL_1_5);
        when(component().getComponentType()).thenReturn(ComponentType.kComponentTypeTouchWrapper);
        when(mockPropertyMap.get(PropertyKey.kPropertyNotifyChildrenChanged)).thenReturn(new Object[]{});
        when(mMockPresenter.inflateComponentHierarchy(mockChild)).thenReturn(childView);

        // Check that view has no children
        assertEquals(0, getView().getChildCount());

        refreshProperties(PropertyKey.kPropertyNotifyChildrenChanged);

        assertEquals(1, getView().getChildCount());
        assertEquals(childView, getView().getChildAt(0));
    }

    @Test
    public void test_refresh_displayedChildren_noChildrenChanged_doesntModifyViews() {
        MultiChildComponent mockChild = mock(MultiChildComponent.class);
        APLAbsoluteLayout mockView = mock(APLAbsoluteLayout.class);
        when(mockChild.getComponentType()).thenReturn(ComponentType.kComponentTypeFrame);
        when(mockChild.getBounds()).thenReturn(Rect.builder().width(10).height(10).left(0).top(0).build());

        when(component().getChildCount()).thenReturn(1);
        when(component().getAllChildren()).thenReturn(Arrays.asList(mockChild));
        when(component().getProperties()).thenReturn(mockPropertyMap);
        when(component().getComponentType()).thenReturn(ComponentType.kComponentTypeContainer);
        when(mockPropertyMap.get(PropertyKey.kPropertyNotifyChildrenChanged)).thenReturn(new Object[]{});
        when(mMockPresenter.inflateComponentHierarchy(mockChild)).thenReturn(mockView);
        when(mMockPresenter.findView(mockChild)).thenReturn(mockView);

        when(mockView.getChildCount()).thenReturn(0);

        refreshProperties(mockView, PropertyKey.kPropertyNotifyChildrenChanged);

        verify(mockView).detachAllViews();
        verify(mockView).attachView(mockView);

        Mockito.reset(mockView);
        when(mockView.getChildCount()).thenReturn(1);
        when(mockView.getChildAt(0)).thenReturn(mockView);

        refreshProperties(mockView, PropertyKey.kPropertyNotifyChildrenChanged);

        verify(mockView, never()).detachAllViews();
        verify(mockView, never()).attachView(any());
        verify(mockView, never()).requestLayout();
        verify(mockView, never()).invalidate();
    }

    private void setupChildWithBounds(Rect bounds) {
        when(component().hasProperty(PropertyKey.kPropertyScrollPosition)).thenReturn(true);
        when(component().getChildCount()).thenReturn(1);
        Component mockChild = mock(Component.class);
        when(component().getChildAt(0)).thenReturn(mockChild);
        when(mockChild.isLaidOut()).thenReturn(true);
        when(mockChild.getBounds()).thenReturn(bounds);
    }
}
