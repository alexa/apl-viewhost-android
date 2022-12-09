/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.document;

import com.amazon.apl.android.scaling.MetricsTransform;
import com.amazon.apl.android.scaling.Scaling;
import com.amazon.apl.android.scaling.ViewportMetrics;
import com.amazon.apl.enums.ScreenShape;
import com.amazon.apl.enums.ViewportMode;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class MetricsTransformTest extends AbstractDocUnitTest {

    @Test
    public void test_metricsTransform_withoutScaling() {
        ViewportMetrics metrics = ViewportMetrics.builder()
                .width(1600)
                .height(800)
                .dpi(160)
                .shape(ScreenShape.RECTANGLE)
                .theme("dark")
                .mode(ViewportMode.kViewportModeHub)
                .build();

        MetricsTransform transform = MetricsTransform.create(metrics);

        assertEquals(metrics, transform.getScaledMetrics());
        assertEquals(1, transform.toViewhost(1));
        assertEquals(1, transform.toCore(1));
        assertEquals(1600, transform.getScaledViewhostWidth());
        assertEquals(800, transform.getScaledViewhostHeight());
    }

    @Test
    public void test_metricsTransform_withoutScaling_withDpi() {
        ViewportMetrics metrics = ViewportMetrics.builder()
                .width(1600)
                .height(800)
                .dpi(320)
                .shape(ScreenShape.RECTANGLE)
                .theme("dark")
                .mode(ViewportMode.kViewportModeHub)
                .build();

        MetricsTransform transform = MetricsTransform.create(metrics);

        assertEquals(metrics, transform.getScaledMetrics());
        assertEquals(2.0f, transform.toViewhost(1.0f), 0.01);
        assertEquals(0.5f, transform.toCore(1.0f), 0.01);
        assertEquals(1600, transform.getScaledViewhostWidth());
        assertEquals(800, transform.getScaledViewhostHeight());
    }

    @Test
    public void test_metricsTransform_withScaling() {
        Scaling.ViewportSpecification[] specsArray = {
                Scaling.ViewportSpecification.builder()
                    .minWidth(1600)
                    .maxWidth(1600)
                    .minHeight(800)
                    .maxHeight(800)
                    .round(false)
                    .mode(ViewportMode.kViewportModeHub)
                    .build(),
                Scaling.ViewportSpecification.builder()
                    .minWidth(800)
                    .maxWidth(800)
                    .minHeight(800)
                    .maxHeight(800)
                    .round(false)
                    .mode(ViewportMode.kViewportModeHub)
                    .build()
        };

        Vector<Scaling.ViewportSpecification> specs = new Vector<>(Arrays.asList(specsArray));
        Scaling scaling = new Scaling(1.0, specs);

        ViewportMetrics metrics = ViewportMetrics.builder()
                .width(1200)
                .height(600)
                .dpi(160)
                .shape(ScreenShape.RECTANGLE)
                .theme("dark")
                .mode(ViewportMode.kViewportModeHub)
                .scaling(scaling)
                .build();

        MetricsTransform transform = MetricsTransform.create(metrics);

        ViewportMetrics scaledMetrics = ViewportMetrics.builder()
                .width(1600)
                .height(800)
                .dpi(160)
                .shape(ScreenShape.RECTANGLE)
                .theme("dark")
                .mode(ViewportMode.kViewportModeHub)
                .scaling(scaling)
                .build();

        assertEquals(scaledMetrics, transform.getScaledMetrics());

        // Scale factor is 0.75 since 1200 / 1600 * 160 / 160
        assertEquals(0.75, transform.toViewhost(1.0f), 0.01);
        assertEquals(1.33, transform.toCore(1.0f), 0.01);
        // Occupy full screen since aspect ratio matches
        assertEquals(1200, transform.getScaledViewhostWidth());
        assertEquals(600, transform.getScaledViewhostHeight());
    }

    @Test
    public void test_metricsTransform_withScaling_withDpi() {
        Scaling.ViewportSpecification[] specsArray = {
                Scaling.ViewportSpecification.builder()
                        .minWidth(1600)
                        .maxWidth(1600)
                        .minHeight(800)
                        .maxHeight(800)
                        .round(false)
                        .mode(ViewportMode.kViewportModeHub)
                        .build(),
                Scaling.ViewportSpecification.builder()
                        .minWidth(800)
                        .maxWidth(800)
                        .minHeight(800)
                        .maxHeight(800)
                        .round(false)
                        .mode(ViewportMode.kViewportModeHub)
                        .build()
        };

        Vector<Scaling.ViewportSpecification> specs = new Vector<>(Arrays.asList(specsArray));
        Scaling scaling = new Scaling(1.0, specs);

        ViewportMetrics metrics = ViewportMetrics.builder()
                .width(2400)
                .height(1200)
                .dpi(320)
                .shape(ScreenShape.RECTANGLE)
                .theme("dark")
                .mode(ViewportMode.kViewportModeHub)
                .scaling(scaling)
                .build();

        MetricsTransform transform = MetricsTransform.create(metrics);

        // Choose 1600x800 dp viewport and convert to px
        ViewportMetrics scaledMetrics = ViewportMetrics.builder()
                .width(3200)
                .height(1600)
                .dpi(320)
                .shape(ScreenShape.RECTANGLE)
                .theme("dark")
                .mode(ViewportMode.kViewportModeHub)
                .scaling(scaling)
                .build();

        assertEquals(scaledMetrics, transform.getScaledMetrics());

        // Scale factor is 1.5 = 2400 / 3200 * 320 / 160
        assertEquals(1.5, transform.toViewhost(1.0f), 0.01);
        assertEquals(0.67, transform.toCore(1.0f), 0.01);
        // Occupy the full screen since aspect ratio matches
        assertEquals(2400, transform.getScaledViewhostWidth());
        assertEquals(1200, transform.getScaledViewhostHeight());
    }

    @Test
    public void test_metricsTransform_withScaling_withDpi_scaleFactor() {
        Scaling.ViewportSpecification[] specsArray = {
                Scaling.ViewportSpecification.builder()
                        .minWidth(1600)
                        .maxWidth(1600)
                        .minHeight(800)
                        .maxHeight(800)
                        .round(false)
                        .mode(ViewportMode.kViewportModeHub)
                        .build(),
                Scaling.ViewportSpecification.builder()
                        .minWidth(800)
                        .maxWidth(800)
                        .minHeight(800)
                        .maxHeight(800)
                        .round(false)
                        .mode(ViewportMode.kViewportModeHub)
                        .build()
        };

        Vector<Scaling.ViewportSpecification> specs = new Vector<>(Arrays.asList(specsArray));
        Scaling scaling = new Scaling(1.0, specs);

        ViewportMetrics metrics = ViewportMetrics.builder()
                .width(1920)
                .height(1080)
                .dpi(240)
                .shape(ScreenShape.RECTANGLE)
                .theme("dark")
                .mode(ViewportMode.kViewportModeHub)
                .scaling(scaling)
                .build();

        MetricsTransform transform = MetricsTransform.create(metrics);

        // Choose 1600x800 dp viewport and convert to px
        ViewportMetrics scaledMetrics = ViewportMetrics.builder()
                .width(2400)
                .height(1200)
                .dpi(240)
                .shape(ScreenShape.RECTANGLE)
                .theme("dark")
                .mode(ViewportMode.kViewportModeHub)
                .scaling(scaling)
                .build();

        assertEquals(scaledMetrics, transform.getScaledMetrics());

        // Scale factor is 1.2 since we take 1920 / 2400 * 240 / 160
        assertEquals(1.2, transform.toViewhost(1.0f), 0.01);
        assertEquals(0.833, transform.toCore(1.0f), 0.01);

        // Occupy 1920x960
        assertEquals(1920, transform.getScaledViewhostWidth());
        assertEquals(960, transform.getScaledViewhostHeight());
    }

    @Test
    public void test_metricsTransform_withScaling_allowModes_viewportNotSupported() {
        Scaling.ViewportSpecification[] specsArray = {
                Scaling.ViewportSpecification.builder()
                        .minWidth(1600)
                        .maxWidth(1600)
                        .minHeight(800)
                        .maxHeight(800)
                        .round(false)
                        .mode(ViewportMode.kViewportModeHub)
                        .build(),
                Scaling.ViewportSpecification.builder()
                        .minWidth(800)
                        .maxWidth(800)
                        .minHeight(800)
                        .maxHeight(800)
                        .round(false)
                        .mode(ViewportMode.kViewportModeHub)
                        .build()
        };

        List<Scaling.ViewportSpecification> specs = Arrays.asList(specsArray);
        List<ViewportMode> allowModes = Arrays.asList(ViewportMode.kViewportModeHub);
        Scaling scaling = new Scaling(1.0, specs, allowModes);

        ViewportMetrics metrics = ViewportMetrics.builder()
                .width(1920)
                .height(1080)
                .dpi(240)
                .shape(ScreenShape.RECTANGLE)
                .theme("dark")
                .mode(ViewportMode.kViewportModeMobile)
                .scaling(scaling)
                .build();

        MetricsTransform transform = MetricsTransform.create(metrics);

        // Choose 1600x800 dp viewport and convert to px and
        // override ViewportMode to Hub
        ViewportMetrics scaledMetrics = ViewportMetrics.builder()
                .width(2400)
                .height(1200)
                .dpi(240)
                .shape(ScreenShape.RECTANGLE)
                .theme("dark")
                .mode(ViewportMode.kViewportModeHub)
                .scaling(scaling)
                .build();

        assertEquals(scaledMetrics, transform.getScaledMetrics());

        // Scale factor is 1.2 since we take 1920 / 2400 * 240 / 160
        assertEquals(1.2, transform.toViewhost(1.0f), 0.01);
        assertEquals(0.833, transform.toCore(1.0f), 0.01);

        // Occupy 1920x960
        assertEquals(1920, transform.getScaledViewhostWidth());
        assertEquals(960, transform.getScaledViewhostHeight());
    }

    @Test
    public void test_metricsTransform_withScaling_allowModes_viewportSupported() {
        Scaling.ViewportSpecification[] specsArray = {
                Scaling.ViewportSpecification.builder()
                        .minWidth(1600)
                        .maxWidth(1600)
                        .minHeight(800)
                        .maxHeight(800)
                        .round(false)
                        .mode(ViewportMode.kViewportModeMobile)
                        .build(),
                Scaling.ViewportSpecification.builder()
                        .minWidth(800)
                        .maxWidth(800)
                        .minHeight(800)
                        .maxHeight(800)
                        .round(false)
                        .mode(ViewportMode.kViewportModeHub)
                        .build()
        };

        List<Scaling.ViewportSpecification> specs = Arrays.asList(specsArray);
        List<ViewportMode> allowModes = Arrays.asList(ViewportMode.kViewportModeHub);
        Scaling scaling = new Scaling(1.0, specs, allowModes);

        ViewportMetrics metrics = ViewportMetrics.builder()
                .width(1920)
                .height(1080)
                .dpi(240)
                .shape(ScreenShape.RECTANGLE)
                .theme("dark")
                .mode(ViewportMode.kViewportModeMobile)
                .scaling(scaling)
                .build();

        MetricsTransform transform = MetricsTransform.create(metrics);

        // Choose 1600x800 dp viewport and convert to px and
        // does not override ViewportMode
        ViewportMetrics scaledMetrics = ViewportMetrics.builder()
                .width(2400)
                .height(1200)
                .dpi(240)
                .shape(ScreenShape.RECTANGLE)
                .theme("dark")
                .mode(ViewportMode.kViewportModeMobile)
                .scaling(scaling)
                .build();

        assertEquals(scaledMetrics, transform.getScaledMetrics());

        // Scale factor is 1.2 since we take 1920 / 2400 * 240 / 160
        assertEquals(1.2, transform.toViewhost(1.0f), 0.01);
        assertEquals(0.833, transform.toCore(1.0f), 0.01);

        // Occupy 1920x960
        assertEquals(1920, transform.getScaledViewhostWidth());
        assertEquals(960, transform.getScaledViewhostHeight());
    }

    @Test
    public void test_metricsTransform_withScaling_withOffset() {
        Scaling.ViewportSpecification[] specsArray = {
                Scaling.ViewportSpecification.builder()
                        .minWidth(1024)
                        .maxWidth(1024)
                        .minHeight(600)
                        .maxHeight(600)
                        .round(false)
                        .mode(ViewportMode.kViewportModeHub)
                        .build(),
                Scaling.ViewportSpecification.builder()
                        .minWidth(1280)
                        .maxWidth(1280)
                        .minHeight(800)
                        .maxHeight(800)
                        .round(false)
                        .mode(ViewportMode.kViewportModeHub)
                        .build()
        };

        List<Scaling.ViewportSpecification> specs = Arrays.asList(specsArray);
        Scaling scaling = new Scaling(1.0, specs);

        // Dimensions in portrait orientation
        ViewportMetrics metrics = ViewportMetrics.builder()
                .width(1200)
                .height(1716)
                .dpi(240)
                .shape(ScreenShape.RECTANGLE)
                .theme("dark")
                .mode(ViewportMode.kViewportModeHub)
                .scaling(scaling)
                .build();

        MetricsTransform transform = MetricsTransform.create(metrics);

        // Choose 1024x600 dp viewport and dimensions are in landscape orientation
        ViewportMetrics scaledMetrics = ViewportMetrics.builder()
                .width(1536)
                .height(900)
                .dpi(240)
                .shape(ScreenShape.RECTANGLE)
                .theme("dark")
                .mode(ViewportMode.kViewportModeHub)
                .scaling(scaling)
                .build();

        assertEquals(scaledMetrics, transform.getScaledMetrics());

        // Width remains the same, height is smaller than the scaled viewport.
        assertEquals(1200, transform.getScaledViewhostWidth());
        assertEquals(703, transform.getScaledViewhostHeight());

        // As width remains the same, offsetX is 0. Only offsetY is affected to 506
        assertEquals(0, transform.getViewportOffsetX());
        assertEquals(506, transform.getViewportOffsetY());
    }

    @Test
    public void test_metricsTransform_withScaling_withoutOffset() {
        Scaling.ViewportSpecification[] specsArray = {
                Scaling.ViewportSpecification.builder()
                        .minWidth(1024)
                        .maxWidth(1024)
                        .minHeight(600)
                        .maxHeight(600)
                        .round(false)
                        .mode(ViewportMode.kViewportModeHub)
                        .build(),
                Scaling.ViewportSpecification.builder()
                        .minWidth(1280)
                        .maxWidth(1280)
                        .minHeight(800)
                        .maxHeight(800)
                        .round(false)
                        .mode(ViewportMode.kViewportModeHub)
                        .build()
        };

        List<Scaling.ViewportSpecification> specs = Arrays.asList(specsArray);
        Scaling scaling = new Scaling(1.0, specs);

        // Dimensions in landscape orientation
        ViewportMetrics metrics = ViewportMetrics.builder()
                .width(1920)
                .height(1200)
                .dpi(240)
                .shape(ScreenShape.RECTANGLE)
                .theme("dark")
                .mode(ViewportMode.kViewportModeHub)
                .scaling(scaling)
                .build();

        MetricsTransform transform = MetricsTransform.create(metrics);

        // Scaled viewport are the same as original viewport (proportional)
        assertEquals(metrics, transform.getScaledMetrics());

        // No scaling applied
        assertEquals(1920, transform.getScaledViewhostWidth());
        assertEquals(1200, transform.getScaledViewhostHeight());

        // As same coordinates should be, no offset should be applied.
        assertEquals(0, transform.getViewportOffsetX());
        assertEquals(0, transform.getViewportOffsetY());
    }
}
