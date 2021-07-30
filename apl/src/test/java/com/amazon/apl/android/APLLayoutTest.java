/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android;

import android.view.KeyEvent;
import android.view.MotionEvent;

import com.amazon.apl.android.bitmap.IBitmapCache;
import com.amazon.apl.android.configuration.ConfigurationChange;
import com.amazon.apl.android.providers.impl.NoOpTelemetryProvider;
import com.amazon.apl.android.robolectric.ViewhostRobolectricTest;
import com.amazon.apl.android.scaling.ViewportMetrics;
import com.amazon.apl.enums.ScreenMode;
import com.amazon.apl.enums.ViewportMode;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.robolectric.RuntimeEnvironment;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class APLLayoutTest extends ViewhostRobolectricTest {

    private APLLayout mView;

    @Mock
    private IAPLViewPresenter mPresenter;
    @Mock
    private RootContext mRootContext;
    @Mock
    private RootConfig mockRootConfig;
    @Mock
    private APLOptions mockAPLOptions;
    @Mock
    private IBitmapCache mockBitmapCache;
    @Mock
    private ViewportMetrics mockViewportMetrics;
    @Mock
    private Component mockComponent;
    @Mock
    private ConfigurationChange.Builder mockConfigurationChangeBuilder;

    @Before
    public void setup() {
        when(mockConfigurationChangeBuilder.screenReaderEnabled(anyBoolean())).thenReturn(mockConfigurationChangeBuilder);
        when(mockConfigurationChangeBuilder.fontScale(anyFloat())).thenReturn(mockConfigurationChangeBuilder);
        when(mockConfigurationChangeBuilder.screenMode(any(ScreenMode.class))).thenReturn(mockConfigurationChangeBuilder);
        when(mPresenter.telemetry()).thenReturn(NoOpTelemetryProvider.getInstance());
        when(mockViewportMetrics.theme()).thenReturn("dark");
        when(mockViewportMetrics.mode()).thenReturn(ViewportMode.kViewportModeHub);
        when(mockRootConfig.getScreenModeEnumerated()).thenReturn(ScreenMode.kScreenModeNormal);
        when(mRootContext.getTopComponent()).thenReturn(mockComponent);
        when(mRootContext.getOptions()).thenReturn(mockAPLOptions);
        when(mRootContext.createConfigurationChange()).thenReturn(mockConfigurationChangeBuilder);
        when(mockAPLOptions.getBitmapCache()).thenReturn(mockBitmapCache);

        mView = new APLLayout(RuntimeEnvironment.systemContext, false, mPresenter);
    }

    @Test
    public void test_defaults() {
        assertEquals(false, mView.isFocusable());
        assertEquals(false, mView.isFocusableInTouchMode());
    }

    /**
     * APL layout should receive key events and pass them to presenter at any time.
     */
    @Test
    public void test_keyboardEvent() {
        KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_A);

        mView.dispatchKeyEvent(event);

        verify(mPresenter).onKeyPress(event);
    }

    @Test
    public void testCreateConfigurationChange_defaults() {
        ConfigurationChange configurationChange = ConfigurationChange.create(mockViewportMetrics, mockRootConfig).build();
        assertEquals(configurationChange.width(), mockViewportMetrics.width());
        assertEquals(configurationChange.height(), mockViewportMetrics.height());
        assertEquals(configurationChange.theme(), mockViewportMetrics.theme());
        assertEquals(configurationChange.mode(), mockViewportMetrics.mode());
        assertEquals(configurationChange.screenMode(), mockRootConfig.getScreenModeEnumerated());
        assertEquals(configurationChange.screenReaderEnabled(), mockRootConfig.getScreenReader());
        assertEquals(configurationChange.fontScale(), mockRootConfig.getFontScale(), 0.01f);
    }

    @Test
    public void testCreateConfigurationChange_nonDefaults() {
        ConfigurationChange configurationChange = ConfigurationChange.create(mockViewportMetrics, mockRootConfig).
                width(1280).
                height(800).
                theme("light").
                mode(ViewportMode.kViewportModeMobile).
                screenMode(ScreenMode.kScreenModeHighContrast).
                screenReaderEnabled(true).
                fontScale(2.0f).
                build();
        assertEquals(configurationChange.width(), 1280);
        assertEquals(configurationChange.height(), 800);
        assertEquals(configurationChange.theme(), "light");
        assertEquals(configurationChange.mode(), ViewportMode.kViewportModeMobile);
        assertEquals(configurationChange.screenMode(), ScreenMode.kScreenModeHighContrast);
        assertEquals(configurationChange.screenReaderEnabled(), true);
        assertEquals(configurationChange.fontScale(), 2.0f, 0.01f);
    }

    @Test
    public void testHandleConfigurationChange_when_topComponent_is_not_null() throws APLController.APLException {
        mView.onDocumentRender(mRootContext);
        ConfigurationChange configurationChange = ConfigurationChange.create(mockViewportMetrics, mockRootConfig).build();
        mView.handleConfigurationChange(configurationChange);
        verify(mRootContext).handleConfigurationChange(configurationChange);
        mView.onLayout(true, 0, 0, 1280, 800);
        verify(mRootContext).initTime();
    }

    @Test
    public void testHandleConfigurationChange_when_topComponent_is_null() {
        mView.onDocumentRender(mRootContext);
        when(mRootContext.getTopComponent()).thenReturn(null);
        ConfigurationChange configurationChange = ConfigurationChange.create(mockViewportMetrics, mockRootConfig).build();
        try {
            mView.handleConfigurationChange(configurationChange);
        } catch (APLController.APLException e) {
            // Do nothing, this is expected.
        }
    }

    @Test
    public void testHandleConfigurationChange_for_accessibility_state_change() throws APLController.APLException {
        mView.onDocumentRender(mRootContext);
        ConfigurationChange configurationChange = ConfigurationChange.create(mockViewportMetrics, mockRootConfig)
                .build();
        mView.handleConfigurationChange(configurationChange);
    }

    @Test
    public void testOnAccessibilityStateChanged_to_true() {
        mView.onDocumentRender(mRootContext);
        ConfigurationChange configurationChange = ConfigurationChange.create(mockViewportMetrics, mockRootConfig).build();
        when(mockConfigurationChangeBuilder.build()).thenReturn(configurationChange);
        mView.onAccessibilityStateChanged(true);
        verify(mRootContext, never()).createConfigurationChange();
        verify(mRootContext, never()).handleConfigurationChange(configurationChange);
    }

    @Test
    public void testOnAccessibilityStateChanged_to_false() {
        mView.onDocumentRender(mRootContext);
        ConfigurationChange configurationChange = ConfigurationChange.create(mockViewportMetrics, mockRootConfig).build();
        when(mockConfigurationChangeBuilder.build()).thenReturn(configurationChange);
        mView.onAccessibilityStateChanged(false);
        verify(mRootContext, never()).createConfigurationChange();
        verify(mRootContext, never()).handleConfigurationChange(configurationChange);
    }

    @Test
    public void testDispatchTouchEvent() {
        mView.onDocumentRender(mRootContext);
        MotionEvent mockEvent = mock(MotionEvent.class);
        mView.dispatchTouchEvent(mockEvent);
        verify(mPresenter).handleTouchEvent(mockEvent);
    }
}
