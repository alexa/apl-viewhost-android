/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android;

import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.accessibility.AccessibilityManager;

import com.amazon.apl.android.bitmap.IBitmapCache;
import com.amazon.apl.android.configuration.ConfigurationChange;
import com.amazon.apl.android.graphic.GraphicContainerElement;
import com.amazon.apl.android.helper.LinearGradientWrapper;
import com.amazon.apl.android.helper.RadialGradientWrapper;
import com.amazon.apl.android.primitive.Gradient;
import com.amazon.apl.android.primitive.Rect;
import com.amazon.apl.android.providers.impl.NoOpTelemetryProvider;
import com.amazon.apl.android.robolectric.ViewhostRobolectricTest;
import com.amazon.apl.android.scaling.ViewportMetrics;
import com.amazon.apl.android.utils.ColorUtils;
import com.amazon.apl.android.views.APLAbsoluteLayout;
import com.amazon.apl.android.views.APLImageView;
import com.amazon.apl.enums.ComponentType;
import com.amazon.apl.enums.GradientType;
import com.amazon.apl.enums.PropertyKey;
import com.amazon.apl.enums.RootProperty;
import com.amazon.apl.enums.ScreenMode;
import com.amazon.apl.enums.ViewportMode;
import com.google.common.collect.ImmutableMap;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.robolectric.RuntimeEnvironment;

import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class APLLayoutTest extends ViewhostRobolectricTest {

    private APLLayout mView;

    @Mock
    private IAPLViewPresenter mPresenter;
    @Mock
    private RootContext mockRootContext;
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
    @Mock
    private RenderingContext mockRenderingContext;

    @Before
    public void setup() {
        when(mockConfigurationChangeBuilder.screenReaderEnabled(anyBoolean())).thenReturn(mockConfigurationChangeBuilder);
        when(mockConfigurationChangeBuilder.fontScale(anyFloat())).thenReturn(mockConfigurationChangeBuilder);
        when(mockConfigurationChangeBuilder.screenMode(any(ScreenMode.class))).thenReturn(mockConfigurationChangeBuilder);
        when(mockConfigurationChangeBuilder.disallowVideo(anyBoolean())).thenReturn(mockConfigurationChangeBuilder);
        when(mockConfigurationChangeBuilder.environmentValue(anyString(), any())).thenReturn(mockConfigurationChangeBuilder);
        when(mPresenter.telemetry()).thenReturn(NoOpTelemetryProvider.getInstance());
        when(mockViewportMetrics.theme()).thenReturn("dark");
        when(mockViewportMetrics.mode()).thenReturn(ViewportMode.kViewportModeHub);
        when(mockRootConfig.getScreenModeEnumerated()).thenReturn(ScreenMode.kScreenModeNormal);
        when(mockRootConfig.getProperty(RootProperty.kDisallowVideo)).thenReturn(false);
        when(mockRootContext.getTopComponent()).thenReturn(mockComponent);
        when(mockRootContext.getOptions()).thenReturn(mockAPLOptions);
        when(mockRootContext.createConfigurationChange()).thenReturn(mockConfigurationChangeBuilder);
        when(mockRootContext.getRenderingContext()).thenReturn(mockRenderingContext);
        when(mockRenderingContext.getBitmapCache()).thenReturn(mockBitmapCache);
        APLController.setRuntimeConfig(RuntimeConfig.builder().build());

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
                disallowVideo(true).
                environmentValue("key1", "val1").
                environmentValue("key2", true).
                environmentValue("key3", 5).
                build();
        assertEquals(configurationChange.width(), 1280);
        assertEquals(configurationChange.height(), 800);
        assertEquals(configurationChange.theme(), "light");
        assertEquals(configurationChange.mode(), ViewportMode.kViewportModeMobile);
        assertEquals(configurationChange.screenMode(), ScreenMode.kScreenModeHighContrast);
        assertEquals(configurationChange.screenReaderEnabled(), true);
        assertEquals(configurationChange.fontScale(), 2.0f, 0.01f);
        assertEquals(configurationChange.disallowVideo(), true);
        assertEquals(configurationChange.environmentValues(), ImmutableMap.of("key1", "val1", "key2", true, "key3", 5));
    }

    @Test
    public void testHandleConfigurationChange_when_topComponent_is_not_null() throws APLController.APLException {
        mView.onDocumentRender(mockRootContext);
        ConfigurationChange configurationChange = ConfigurationChange.create(mockViewportMetrics, mockRootConfig).build();
        mView.handleConfigurationChange(configurationChange);
        verify(mockRootContext).handleConfigurationChange(configurationChange);
        mView.onLayout(true, 0, 0, 1280, 800);
        verify(mockRootContext).initTime();
    }

    @Test
    public void testHandleConfigurationChange_when_topComponent_is_null() {
        mView.onDocumentRender(mockRootContext);
        when(mockRootContext.getTopComponent()).thenReturn(null);
        ConfigurationChange configurationChange = ConfigurationChange.create(mockViewportMetrics, mockRootConfig).build();
        try {
            mView.handleConfigurationChange(configurationChange);
        } catch (APLController.APLException e) {
            // Do nothing, this is expected.
        }
    }

    @Test
    public void testHandleConfigurationChange_for_accessibility_state_change() throws APLController.APLException {
        mView.onDocumentRender(mockRootContext);
        ConfigurationChange configurationChange = ConfigurationChange.create(mockViewportMetrics, mockRootConfig)
                .build();
        mView.handleConfigurationChange(configurationChange);
    }

    @Test
    public void testOnAccessibilityStateChanged_to_true() {
        mView.onDocumentRender(mockRootContext);
        ConfigurationChange configurationChange = ConfigurationChange.create(mockViewportMetrics, mockRootConfig).build();
        when(mockConfigurationChangeBuilder.build()).thenReturn(configurationChange);
        mView.onAccessibilityStateChanged(true);
        verify(mockRootContext, never()).createConfigurationChange();
        verify(mockRootContext, never()).handleConfigurationChange(configurationChange);
    }

    @Test
    public void testOnAccessibilityStateChanged_to_false() {
        mView.onDocumentRender(mockRootContext);
        ConfigurationChange configurationChange = ConfigurationChange.create(mockViewportMetrics, mockRootConfig).build();
        when(mockConfigurationChangeBuilder.build()).thenReturn(configurationChange);
        mView.onAccessibilityStateChanged(false);
        verify(mockRootContext, never()).createConfigurationChange();
        verify(mockRootContext, never()).handleConfigurationChange(configurationChange);
    }

    @Test
    public void testDispatchTouchEvent() {
        mView.onDocumentRender(mockRootContext);
        MotionEvent mockEvent = mock(MotionEvent.class);
        mView.dispatchTouchEvent(mockEvent);
        verify(mPresenter).handleTouchEvent(mockEvent);
    }

    @Test
    public void testDispatchTouchEvent_requestsDisallowInterceptOnParentWhenCoreHandlesEvent() {
        ViewGroup parentView = mock(ViewGroup.class);
        APLLayout view = spy(new APLLayout(RuntimeEnvironment.systemContext, false, mPresenter));
        doReturn(parentView).when(view).getParent();

        MotionEvent mockEvent = mock(MotionEvent.class);
        when(mPresenter.handleTouchEvent(mockEvent)).thenReturn(true).thenReturn(false);

        InOrder inOrder = inOrder(parentView);

        view.dispatchTouchEvent(mockEvent);
        inOrder.verify(parentView).requestDisallowInterceptTouchEvent(true);

        view.dispatchTouchEvent(mockEvent);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testFinishWithNullContext() {
        APLLayout view = new APLLayout(RuntimeEnvironment.systemContext, false);
        view.getPresenter().onDocumentFinish(); // calls onFinish
    }

    @Test
    public void test_onComponentChange_with_VectorGraphic_no_View() {
        VectorGraphic graphic = mock(VectorGraphic.class);
        GraphicContainerElement gce = mock(GraphicContainerElement.class);

        final Set<Integer> dirtyGraphics = Set.of(1);

        when(graphic.getComponentId()).thenReturn("1001");
        when(graphic.getComponentType()).thenReturn(ComponentType.kComponentTypeVectorGraphic);
        when(graphic.getDirtyGraphics()).thenReturn(dirtyGraphics);
        when(graphic.getOrCreateGraphicContainerElement()).thenReturn(gce);

        APLLayout view = new APLLayout(RuntimeEnvironment.systemContext, false);
        view.getPresenter().onComponentChange(graphic, Arrays.asList(new PropertyKey[]{ }));
        verify(gce).applyDirtyProperties(any(Set.class));
    }

    @Test
    public void testRoundingOfLayoutParams() {
        APLLayout view = new APLLayout(RuntimeEnvironment.systemContext, false);
        Image imageComponent = mock(Image.class);
        APLImageView imageView = mock(APLImageView.class);
        ArgumentCaptor<APLLayoutParams> layoutParamsCapture = ArgumentCaptor.forClass(APLLayoutParams.class);

        when(imageComponent.getBounds()).thenReturn(Rect.builder().height(0.9f).width(1.2f).left(1).top(1).build());
        when(imageComponent.getParentId()).thenReturn(null);

        // Just needs to be non-null.
        when(imageView.getParent()).thenReturn(mock(ViewParent.class));

        view.getPresenter().updateViewInLayout(imageComponent, imageView);

        verify(imageView).setLayoutParams(layoutParamsCapture.capture());

        APLLayoutParams params = layoutParamsCapture.getValue();

        assertEquals(1, params.width);
        assertEquals(1, params.height);
    }

    @Test
    public void testFinish_withFlagNotSet_clearsViews() {
        // Arrange
        APLController.setRuntimeConfig(RuntimeConfig.builder()
                .clearViewsOnFinish(true)
                .build());
        APLLayout view = new APLLayout(RuntimeEnvironment.systemContext, false);
        View child = new View(RuntimeEnvironment.systemContext);
        child.setLayoutParams(new APLLayoutParams(100, 100, 0, 0));
        view.addView(child);
        assertEquals(child, view.getChildAt(0));
        Component mockComponent = mock(Component.class);
        when(mockComponent.getComponentId()).thenReturn(":1001");
        view.getPresenter().associate(mockComponent, child);

        // Act
        view.getPresenter().onDocumentFinish();

        // Assert
        assertNull(view.getChildAt(0));
    }

    @Test
    public void testFinish_withFlagSet_doesNotClearViews() {
        // Arrange
        APLController.setRuntimeConfig(RuntimeConfig.builder()
                .clearViewsOnFinish(false)
                .build());
        APLLayout view = new APLLayout(RuntimeEnvironment.systemContext, false);
        View child = new APLAbsoluteLayout(RuntimeEnvironment.systemContext, view.getPresenter());
        child.setLayoutParams(new APLLayoutParams(100, 100, 0, 0));
        view.addView(child);
        assertEquals(child, view.getChildAt(0));
        Component mockComponent = mock(Component.class);
        when(mockComponent.getComponentId()).thenReturn(":1001");
        view.getPresenter().associate(mockComponent, child);

        // Act
        view.getPresenter().onDocumentFinish();

        // Assert
        assertEquals(child, view.getChildAt(0));
    }

    @Test
    public void testViewPresenterMediaLoaded_nullRootContextDoesNotThrow() {
        // Given
        String testUrl = "url";
        APLLayout layout = new APLLayout(RuntimeEnvironment.systemContext, false);
        layout.getPresenter().onDocumentFinish();

        // When
        layout.getPresenter().mediaLoaded(testUrl);

        // Then: does not throw NPE
    }

    @Test
    public void testViewPresenterMediaLoadFailed_nullRootContextDoesNotThrow() {
        // Given
        String testUrl = "url";
        String testError = "error";
        int testErrorCode = 0;
        APLLayout layout = new APLLayout(RuntimeEnvironment.systemContext, false);
        layout.getPresenter().onDocumentFinish();

        // When
        layout.getPresenter().mediaLoadFailed(testUrl, testErrorCode, testError);

        // Then: does not throw NPE
    }

    /**
     * This test verifies that layout will notify the metrics are ready, instead of measure since
     * measure may occur more than once prior to the layout pass.
     *
     * I/APLLayout: onMeasure. w: MeasureSpec: EXACTLY 960, h: MeasureSpec: EXACTLY 408
     * I/APLLayout: onMeasure. w: MeasureSpec: EXACTLY 960, h: MeasureSpec: EXACTLY 432
     * I/APLLayout: onLayout. l: 0, t: 48, r: 960, b: 480
     */
    @Test
    public void testLayoutNotifiesMetricsAreReady() {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        APLLayout layout = new APLLayout(RuntimeEnvironment.systemContext, false);
        layout.addMetricsReadyListener((viewportMetrics -> {
            assertEquals(960, viewportMetrics.width());
            assertEquals(432, viewportMetrics.height());
            countDownLatch.countDown();
        }));


        layout.measure(View.MeasureSpec.makeMeasureSpec(960, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(408, View.MeasureSpec.EXACTLY));
        layout.measure(View.MeasureSpec.makeMeasureSpec(960, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(432, View.MeasureSpec.EXACTLY));
        assertEquals("Layout should not be fully measured yet.", 1, countDownLatch.getCount());

        layout.layout( 0, 48, 960, 480);
        try {
            assertTrue("Layout wasn't measured", countDownLatch.await(1, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testLayoutRemeasuredOnNextDocumentWhenLayoutRequested() {
        CountDownLatch countDownLatch = new CountDownLatch(2);
        APLLayout layout = new APLLayout(RuntimeEnvironment.systemContext, false);
        layout.addMetricsReadyListener((viewportMetrics -> {
            assertEquals(640, viewportMetrics.width());
            assertEquals(480, viewportMetrics.height());
            countDownLatch.countDown();
        }));

        layout.measure(View.MeasureSpec.makeMeasureSpec(640, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(480, View.MeasureSpec.EXACTLY));
        layout.layout(0, 0, 640, 480);

        // Any layout request and finish will cause metrics to no longer be "ready"
        layout.requestLayout();
        layout.getPresenter().onDocumentFinish();

        layout.addMetricsReadyListener((viewportMetrics -> {
            assertEquals(480, viewportMetrics.width());
            assertEquals(640, viewportMetrics.height());
            countDownLatch.countDown();
        }));

        layout.measure(View.MeasureSpec.makeMeasureSpec(480, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(640, View.MeasureSpec.EXACTLY));
        layout.layout(0, 0, 480, 640);

        try {
            assertTrue(countDownLatch.await(1, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void test_contentBackground_linearGradient() {
        APLLayout aplLayout = new APLLayout(getApplication(), false);
        IAPLViewPresenter presenter = aplLayout.getPresenter();
        aplLayout.measure(
                View.MeasureSpec.makeMeasureSpec(1200, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(800, View.MeasureSpec.EXACTLY));
        aplLayout.layout( 0,0,1200,800);
        presenter.getOrCreateViewportMetrics();
        presenter.loadBackground(new Content.DocumentBackground(true,
                GradientType.LINEAR.getIndex(), Color.TRANSPARENT, 90f,
                new long[] {Color.RED,Color.BLACK}, new float[] {0.25f, 0.75f}));

        LayerDrawable layerDrawable = (LayerDrawable) aplLayout.getBackground();
        ShapeDrawable shapeDrawable = (ShapeDrawable) layerDrawable.getDrawable(1);

        Shader expectedShader = Gradient.createGradientShader(
                new int[] {ColorUtils.toARGB(Color.RED), ColorUtils.toARGB(Color.BLACK)},
                new float[] {0.25f, 0.75f}, GradientType.LINEAR, 90f, 800, 1200);
        Shader actualShader = shapeDrawable.getPaint().getShader();

        assertTrue(expectedShader instanceof LinearGradient);
        assertTrue(actualShader instanceof LinearGradient);

        assertEquals(new LinearGradientWrapper((LinearGradient) expectedShader),
                new LinearGradientWrapper((LinearGradient) actualShader));

        Matrix expectedMatrix = new Matrix();
        Matrix actualMatrix = new Matrix();
        expectedShader.getLocalMatrix(expectedMatrix);
        actualShader.getLocalMatrix(actualMatrix);

        assertEquals(expectedMatrix, actualMatrix);
    }

    @Test
    public void test_contentBackground_radialGradient() {
        APLLayout aplLayout = new APLLayout(getApplication(), false);
        IAPLViewPresenter presenter = aplLayout.getPresenter();
        aplLayout.measure(
                View.MeasureSpec.makeMeasureSpec(1200, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(800, View.MeasureSpec.EXACTLY));
        aplLayout.layout( 0,0,1200,800);
        presenter.getOrCreateViewportMetrics();
        presenter.loadBackground(new Content.DocumentBackground(true,
                GradientType.RADIAL.getIndex(), Color.TRANSPARENT, 90f,
                new long[] {Color.RED,Color.BLACK}, new float[] {0.25f, 0.75f}));

        LayerDrawable layerDrawable = (LayerDrawable) aplLayout.getBackground();
        ShapeDrawable shapeDrawable = (ShapeDrawable) layerDrawable.getDrawable(1);

        Shader expectedShader = Gradient.createGradientShader(
                new int[] {ColorUtils.toARGB(Color.RED), ColorUtils.toARGB(Color.BLACK)},
                new float[] {0.25f, 0.75f}, GradientType.RADIAL, 90f, 800, 1200);
        Shader actualShader = shapeDrawable.getPaint().getShader();

        assertTrue(expectedShader instanceof RadialGradient);
        assertTrue(actualShader instanceof RadialGradient);

        assertEquals(new RadialGradientWrapper((RadialGradient) expectedShader),
                new RadialGradientWrapper((RadialGradient) actualShader));

        Matrix expectedMatrix = new Matrix();
        Matrix actualMatrix = new Matrix();
        expectedShader.getLocalMatrix(expectedMatrix);
        actualShader.getLocalMatrix(actualMatrix);

        assertEquals(expectedMatrix, actualMatrix);
    }

    @Test
    public void test_loadBackground_afterFinish_loadsBackground() {
        APLLayout aplLayout = new APLLayout(getApplication(), false);
        aplLayout.measure(
                View.MeasureSpec.makeMeasureSpec(1200, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(800, View.MeasureSpec.EXACTLY));
        aplLayout.layout( 0,0,1200,800);
        IAPLViewPresenter presenter = aplLayout.getPresenter();
        presenter.getOrCreateViewportMetrics();
        presenter.onDocumentFinish();

        presenter.loadBackground(new Content.DocumentBackground(true,
                GradientType.LINEAR.getIndex(), Color.TRANSPARENT, 90f,
                new long[] {Color.RED,Color.BLACK}, new float[] {0.25f, 0.75f}));

        LayerDrawable layerDrawable = (LayerDrawable) aplLayout.getBackground();
        ShapeDrawable shapeDrawable = (ShapeDrawable) layerDrawable.getDrawable(1);

        Shader expectedShader = Gradient.createGradientShader(
                new int[] {ColorUtils.toARGB(Color.RED), ColorUtils.toARGB(Color.BLACK)},
                new float[] {0.25f, 0.75f}, GradientType.LINEAR, 90f, 800, 1200);
        Shader actualShader = shapeDrawable.getPaint().getShader();

        assertTrue(expectedShader instanceof LinearGradient);
        assertTrue(actualShader instanceof LinearGradient);

        assertEquals(new LinearGradientWrapper((LinearGradient) expectedShader),
                new LinearGradientWrapper((LinearGradient) actualShader));

        Matrix expectedMatrix = new Matrix();
        Matrix actualMatrix = new Matrix();
        expectedShader.getLocalMatrix(expectedMatrix);
        actualShader.getLocalMatrix(actualMatrix);

        assertEquals(expectedMatrix, actualMatrix);
    }

    @Test
    public void test_accessibilityStateChangeListener_registeredWhenAttached() {
        AccessibilityManager accessibilityManager = mock(AccessibilityManager.class);
        APLLayout aplLayout = new APLLayout(getApplication(), false);
        aplLayout.setAccessibilityManager(accessibilityManager);
        aplLayout.onAttachedToWindow();
        verify(accessibilityManager).addAccessibilityStateChangeListener(aplLayout);
        aplLayout.onDetachedFromWindow();
        verify(accessibilityManager).removeAccessibilityStateChangeListener(aplLayout);
    }

    @Test(expected = Test.None.class)
    public void test_onDetachedFromWindow_doesntThrowExceptionWhenTimeZoneNotRegistered() {
        APLLayout aplLayout = new APLLayout(getApplication(), false);
        aplLayout.onDetachedFromWindow();
    }
}
