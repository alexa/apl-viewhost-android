/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.component;

import android.view.View;
import android.view.ViewGroup;

import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;

import com.amazon.apl.android.APLAccessibilityDelegate;
import com.amazon.apl.android.Component;
import com.amazon.apl.android.IAPLViewPresenter;
import com.amazon.apl.android.RenderingContext;
import com.amazon.apl.android.bitmap.ShadowCache;
import com.amazon.apl.android.primitive.AccessibilityActions;
import com.amazon.apl.android.primitive.Rect;
import com.amazon.apl.android.robolectric.ViewhostRobolectricTest;
import com.amazon.apl.android.shadow.ShadowBitmapRenderer;
import com.amazon.apl.android.utils.APLTrace;
import com.amazon.apl.android.utils.AccessibilitySettingsUtil;
import com.amazon.apl.enums.Display;
import com.amazon.apl.enums.PropertyKey;
import com.amazon.apl.enums.Role;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public abstract class AbstractComponentViewAdapterTest<C extends Component, V extends View> extends ViewhostRobolectricTest {
    private V mView;
    @Mock
    IAPLViewPresenter mMockPresenter;
    @Mock
    AccessibilitySettingsUtil mockAccessibilitySettingsUtil;
    @Mock
    RenderingContext mRenderingContext;
    @Mock
    private ShadowBitmapRenderer mockShadowRenderer;

    AccessibilityActions.AccessibilityAction mScrollBackwardAction;
    AccessibilityActions.AccessibilityAction mScrollForwardAction;
    AccessibilityActions mBothDirectionAccessbilityActions;
    AccessibilityActions mForwardOnlyAccessbilityActions;
    AccessibilityActions mBackwardOnlyAccessbilityActions;

    private ShadowCache mBitmapCache;
    
    V getView() {
        return mView;
    }

    private V createView() {
        return getAdapter().createView(RuntimeEnvironment.systemContext, mMockPresenter);
    }

    abstract C component();

    /**
     * Override this method to test Component specific defaults.vv
     */
    void assertDefaults() {}

    abstract void componentSetup() throws Exception;

    protected ComponentViewAdapter<C,V> getAdapter() {
        //noinspection unchecked
        return ComponentViewAdapterFactory.getAdapter(component());
    }

    @Before
    public void setup() throws Exception {
        mView = createView();
        mBitmapCache = new ShadowCache();
        when(mMockPresenter.getAPLTrace()).thenReturn(mock(APLTrace.class));
        when(mockShadowRenderer.getCache()).thenReturn(mBitmapCache);
        when(mMockPresenter.getShadowRenderer()).thenReturn(mockShadowRenderer);
        mScrollBackwardAction = AccessibilityActions.AccessibilityAction.create("scrollbackward", "scrollbackward");
        mScrollForwardAction = AccessibilityActions.AccessibilityAction.create("scrollforward", "scrollforward");
        mBothDirectionAccessbilityActions = new AccessibilityActions() {
            @Override
            public List<AccessibilityAction> list() {
                List<AccessibilityAction> accessibilityActionList = new ArrayList<>();
                accessibilityActionList.add(mScrollBackwardAction);
                accessibilityActionList.add(mScrollForwardAction);
                return accessibilityActionList;
            }
        };

        mForwardOnlyAccessbilityActions = new AccessibilityActions() {
            @Override
            public List<AccessibilityAction> list() {
                List<AccessibilityAction> accessibilityActionList = new ArrayList<>();
                accessibilityActionList.add(mScrollForwardAction);
                return accessibilityActionList;
            }
        };

        mBackwardOnlyAccessbilityActions = new AccessibilityActions() {
            @Override
            public List<AccessibilityAction> list() {
                List<AccessibilityAction> accessibilityActionList = new ArrayList<>();
                accessibilityActionList.add(mScrollBackwardAction);
                return accessibilityActionList;
            }
        };
        setupMockComponent(component());

        setScreenReaderEnabled(false);

        componentSetup();
    }

    public void setupMockComponent(Component component) {
        when(component.getRenderingContext()).thenReturn(mRenderingContext);
        when(component.getDisplay()).thenReturn(Display.kDisplayNormal);
        when(component.isInvisibleOverride()).thenReturn(false);
        when(component.isFocusable()).thenReturn(false);
        when(component.getBounds()).thenReturn(Rect.builder().left(0).top(0).width(200).height(50).build());
        when(component.getInnerBounds()).thenReturn(Rect.builder().left(0).top(0).width(198).height(48).build());
        when(component.getViewPresenter()).thenReturn(mMockPresenter);
        when(component.getRole()).thenReturn(Role.kRoleNone);
        when(component.getAccessibilityActions()).thenReturn(new AccessibilityActions() {
            @Override
            public List<AccessibilityAction> list() {
                return Collections.emptyList();
            }
        });
    }

    @Test
    public void test_defaults() {
        applyAllProperties();
        
        assertEquals(View.VISIBLE, getView().getVisibility());
        assertEquals(0f, getView().getZ(), 0.01);
        assertEquals(1f, getView().getAlpha(), 0.01);
        assertNull(getView().getContentDescription());
        assertTrue(getView().isEnabled());
        assertTrue(ViewCompat.hasAccessibilityDelegate(getView()));
        assertFalse(getView().hasOnClickListeners());

        assertDefaults();
    }

    @Test
    public void test_displayGone() {
        when(component().getDisplay()).thenReturn(Display.kDisplayNone);

        applyAllProperties();
        
        assertEquals(View.GONE, getView().getVisibility());
    }

    @Test
    public void test_displayInvisible() {
        when(component().getDisplay()).thenReturn(Display.kDisplayInvisible);

        applyAllProperties();

        assertEquals(View.INVISIBLE, getView().getVisibility());
    }

    @Test
    public void test_opacity() {
        when(component().hasProperty(PropertyKey.kPropertyOpacity)).thenReturn(true);
        when(component().getOpacity()).thenReturn(0.7f);

        applyAllProperties();

        assertEquals(0.7f, getView().getAlpha(), 0.01);
    }

    @Test
    public void test_accessibility() {
        String accessibilityString = "accessibility";
        when(component().getAccessibilityLabel()).thenReturn(accessibilityString);

        applyAllProperties();

        assertEquals(accessibilityString, getView().getContentDescription());
    }

    @Test
    public void test_screenReaderFocusability_noAccessibilityLabel() {
        String accessibilityString = "";
        when(component().getAccessibilityLabel()).thenReturn(accessibilityString);
        setScreenReaderEnabled(true);

        applyAllProperties();

        assertEquals(component().isFocusable(), getView().isFocusable());
        assertEquals(component().isFocusableInTouchMode(), getView().isFocusableInTouchMode());
    }

    @Test
    public void test_screenReaderFocusability_accessibilityLabel() {
        String accessibilityString = "accessibility";
        when(component().getAccessibilityLabel()).thenReturn(accessibilityString);
        setScreenReaderEnabled(true);

        applyAllProperties();

        assertTrue(getView().isFocusable());
        assertTrue(getView().isFocusableInTouchMode());
    }

    @Test
    public void test_refresh_display() {
        when(component().getDisplay()).thenReturn(Display.kDisplayNone);

        refreshProperties(PropertyKey.kPropertyDisplay);

        verify(component()).getDisplay();
        verify(component()).isInvisibleOverride();
        verifyNoMoreInteractions(component());

        assertEquals(View.GONE, getView().getVisibility());
    }

    @Test
    public void test_refresh_opacity() {
        when(component().hasProperty(PropertyKey.kPropertyOpacity)).thenReturn(true);
        when(component().getOpacity()).thenReturn(0.25f);

        refreshProperties(PropertyKey.kPropertyOpacity);

        verify(component()).getOpacity();
        verify(component()).hasProperty(PropertyKey.kPropertyOpacity);
        verifyNoMoreInteractions(component());

        assertEquals(0.25f, getView().getAlpha(), 0.01);
    }

    @Test
    public void test_refresh_accessibility() {
        String accessibilityString = "accessibility";
        when(component().getAccessibilityLabel()).thenReturn(accessibilityString);

        refreshProperties(PropertyKey.kPropertyAccessibilityLabel);

        verify(component(), atLeastOnce()).getAccessibilityLabel();
        verify(component()).isFocusable();
        verify(component()).isFocusableInTouchMode();
        verifyNoMoreInteractions(component());

        assertEquals(accessibilityString, getView().getContentDescription());
    }

    @Test
    public void test_refresh_disabled() {
        when(component().isDisabled()).thenReturn(true);

        refreshProperties(PropertyKey.kPropertyDisabled);

        verify(component(), atLeastOnce()).isDisabled();
        verify(component()).isClickable();
        verify(component()).isFocusable();
        verify(component()).isFocusableInTouchMode();
        verify(component()).getAccessibilityLabel();
        verifyNoMoreInteractions(component());

        assertFalse(getView().isEnabled());
    }

    @Test
    public void test_invisibleOverride() {
        when(component().isInvisibleOverride()).thenReturn(true);

        refreshProperties(PropertyKey.kPropertyDisplay);

        assertEquals(View.INVISIBLE, getView().getVisibility());
    }

    @Test
    public void test_clickable_components_setsClickListener() {
        when(component().isClickable()).thenReturn(true);

        refreshProperties(PropertyKey.kPropertyDisabled);

        assertTrue(getView().hasOnClickListeners());
        getView().performClick();

        verify(mMockPresenter).onClick(eq(getView()));
    }


    @Test
    public void test_transform() {
        ViewGroup parent = mock(ViewGroup.class);
        V spyView = spy(mView);
        when(spyView.getParent()).thenReturn(parent);
        getAdapter().refreshProperties(component(), spyView, Arrays.asList(PropertyKey.kPropertyTransform));

        verify(parent).invalidate();
    }

    // TODO tests for
    //  1. shadows
    //  2. padding/layout
    //  3. transforms

    void applyAllProperties() {
        getAdapter().applyAllProperties(component(), getView());
    }

    void applyAllProperties(V view) {
        getAdapter().applyAllProperties(component(), view);
    }

    void refreshProperties(PropertyKey... dirtyProperties) {
        getAdapter().refreshProperties(component(), getView(), Arrays.asList(dirtyProperties));
    }

    void refreshProperties(V view, PropertyKey... dirtyProperties) {
        getAdapter().refreshProperties(component(), view, Arrays.asList(dirtyProperties));
    }

    void setScreenReaderEnabled(boolean enabled) {
        when(mockAccessibilitySettingsUtil.isScreenReaderEnabled(any())).thenReturn(enabled);
        getAdapter().setsAccessibilitySettingsUtil(mockAccessibilitySettingsUtil);
    }

    AccessibilityNodeInfoCompat initializeNodeInfo() {
        View mockView = mock(View.class);
        APLAccessibilityDelegate aplAccessibilityDelegate = APLAccessibilityDelegate.create(component(), getApplication());

        AccessibilityNodeInfoCompat nodeInfoCompat = AccessibilityNodeInfoCompat.obtain();
        aplAccessibilityDelegate.onInitializeAccessibilityNodeInfo(mockView, nodeInfoCompat);
        return nodeInfoCompat;
    }

    static boolean checkNodeInfoHasAction(AccessibilityNodeInfoCompat nodeInfoCompat,
                                           AccessibilityNodeInfoCompat.AccessibilityActionCompat targetAction) {
        List<AccessibilityNodeInfoCompat.AccessibilityActionCompat> actionList = nodeInfoCompat.getActionList();
        for (AccessibilityNodeInfoCompat.AccessibilityActionCompat actionCompat : actionList) {
            if (targetAction.getId() == actionCompat.getId()) {
                return true;
            }
        }
        throw new AssertionError("Action: " + targetAction + ", not in list: " + actionList);
    }
}
