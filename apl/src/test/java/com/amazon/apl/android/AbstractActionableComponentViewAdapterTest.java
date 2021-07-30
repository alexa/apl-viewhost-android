/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android;

import android.view.View;
import android.widget.LinearLayout;

import com.amazon.apl.android.component.ComponentViewAdapter;
import com.amazon.apl.android.component.ComponentViewAdapterFactory;
import com.amazon.apl.android.primitive.Rect;
import com.amazon.apl.android.robolectric.ViewhostRobolectricTest;
import com.amazon.apl.enums.Display;
import com.amazon.apl.enums.LayoutDirection;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.robolectric.RuntimeEnvironment;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

public abstract class AbstractActionableComponentViewAdapterTest<C extends Component, V extends View> extends ViewhostRobolectricTest {
    private V mView;
    @Mock
    IAPLViewPresenter mMockPresenter;
    @Mock
    private Rect mockInnerBounds;
    @Mock
    private Rect mockBounds;

    V getView() {
        return mView;
    }

    private V createView() {
        return getAdapter().createView(RuntimeEnvironment.systemContext, mMockPresenter);
    }

    abstract C component();

    abstract void componentSetup();

    private ComponentViewAdapter<C,V> getAdapter() {
        //noinspection unchecked
        return ComponentViewAdapterFactory.getAdapter(component());
    }

    @Before
    public void setup() {
        mView = createView();
        when(mockBounds.intLeft()).thenReturn(0);
        when(mockBounds.intTop()).thenReturn(0);
        when(mockBounds.intRight()).thenReturn(200);
        when(mockBounds.intBottom()).thenReturn(50);
        when(mockBounds.intWidth()).thenReturn(200);
        when(mockBounds.intHeight()).thenReturn(50);

        when(mockInnerBounds.intLeft()).thenReturn(0);
        when(mockInnerBounds.intTop()).thenReturn(0);
        when(mockInnerBounds.intRight()).thenReturn(198);
        when(mockInnerBounds.intBottom()).thenReturn(48);
        when(mockInnerBounds.intWidth()).thenReturn(198);
        when(mockInnerBounds.intHeight()).thenReturn(48);

        when(component().getInnerBounds()).thenReturn(mockInnerBounds);
        when(component().getBounds()).thenReturn(mockBounds);

        when(component().getDisplay()).thenReturn(Display.kDisplayNormal);
        when(component().isInvisibleOverride()).thenReturn(false);
        when(component().isFocusable()).thenReturn(true);
        when(component().getLayoutDirection()).thenReturn(LayoutDirection.kLayoutDirectionLTR);

        componentSetup();
    }

    @Test
    public void test_focus() {
        // we can never lose focus unless a parent can take it
        shadowOf(getView())
                .setMyParent(new LinearLayout(RuntimeEnvironment.systemContext));

        assertEquals(false, getView().hasFocus());

        applyAllProperties();

        // gain focus
        getView().requestFocus();
        assertEquals(true, getView().hasFocus());

        // lose focus
        getView().clearFocus();
        assertEquals(false, getView().hasFocus());
    }

    void applyAllProperties() {
        getAdapter().applyAllProperties(component(), getView());
    }
}
