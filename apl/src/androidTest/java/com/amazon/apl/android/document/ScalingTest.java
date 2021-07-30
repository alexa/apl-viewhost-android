/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.document;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.amazon.apl.android.APLOptions;
import com.amazon.apl.android.scaling.Scaling;
import com.amazon.apl.android.scaling.ViewportMetrics;
import com.amazon.apl.enums.ScreenShape;
import com.amazon.apl.enums.ViewportMode;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class ScalingTest extends AbstractDocUnitTest {


    @Before
    public void setup() {

    }

    private final String CONDITIONAL = "{\n" +
            "  \"type\": \"APL\",\n" +
            "  \"version\": \"1.0\",\n" +
            "  \"mainTemplate\": {\n" +
            "    \"items\": [\n" +
            "      {\n" +
            "        \"when\": \"${viewport.width == 800}\",\n" +
            "        \"id\": \"frame1\",\n" +
            "        \"type\": \"Frame\"\n" +
            "      }\n" +
            "    ]\n" +
            "  }\n" +
            "}";

    private final String VIEWPORT_HUB = "{" +
            "  \"type\": \"APL\"," +
            "  \"version\": \"1.0\"," +
            "  \"mainTemplate\": {" +
            "    \"items\": [" +
            "      {" +
            "        \"when\": \"${viewport.mode == 'hub' && viewport.width == 800}\"," +
            "        \"id\": \"frame1\"," +
            "        \"type\": \"Frame\"" +
            "      }" +
            "    ]" +
            "  }" +
            "}";

    private final String VIEWPORT_MOBILE = "{" +
            "  \"type\": \"APL\"," +
            "  \"version\": \"1.0\"," +
            "  \"mainTemplate\": {" +
            "    \"items\": [" +
            "      {" +
            "        \"when\": \"${viewport.mode == 'mobile' && viewport.width == 800}\"," +
            "        \"id\": \"frame1\"," +
            "        \"type\": \"Frame\"" +
            "      }" +
            "    ]" +
            "  }" +
            "}";

    @Test
    public void testFallback() {
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
        Scaling scaling = new Scaling(1.0, specs);
        APLOptions options = APLOptions.builder().build();

        ViewportMetrics metrics = ViewportMetrics.builder()
                .width(1600)
                .height(800)
                .dpi(160)
                .shape(ScreenShape.RECTANGLE)
                .theme("dark")
                .mode(ViewportMode.kViewportModeHub)
                .scaling(scaling)
                .build();

        loadDocument(CONDITIONAL, options, metrics);
        Assert.assertEquals(800, mRootContext.getPixelWidth());
        Assert.assertEquals(800, mRootContext.getPixelHeight());
    }

    @Test
    public void testMultipleFallbacks() {
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
                        .minWidth(1500)
                        .maxWidth(1500)
                        .minHeight(800)
                        .maxHeight(800)
                        .round(false)
                        .mode(ViewportMode.kViewportModeHub)
                        .build(),
                Scaling.ViewportSpecification.builder()
                        .minWidth(900)
                        .maxWidth(900)
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
        Scaling scaling = new Scaling(1.0, specs);
        APLOptions options = APLOptions.builder().build();

        ViewportMetrics metrics = ViewportMetrics.builder()
                .width(1600)
                .height(800)
                .dpi(160)
                .shape(ScreenShape.RECTANGLE)
                .theme("dark")
                .mode(ViewportMode.kViewportModeHub)
                .scaling(scaling)
                .build();

        loadDocument(CONDITIONAL, options, metrics);
        Assert.assertEquals(800, mRootContext.getPixelWidth());
        Assert.assertEquals(800, mRootContext.getPixelHeight());
    }

    @Test(expected = IllegalStateException.class)
    public void testThrows() {
        loadDocument(CONDITIONAL);
    }

    @Test
    public void testAllowModes_allowMode_viewportSupported() {
        Scaling.ViewportSpecification[] specsArray = {
                Scaling.ViewportSpecification.builder()
                        .minWidth(800)
                        .maxWidth(800)
                        .minHeight(1280)
                        .maxHeight(1280)
                        .round(false)
                        .mode(ViewportMode.kViewportModeHub)
                        .build(),
                Scaling.ViewportSpecification.builder()
                        .minWidth(800)
                        .maxWidth(800)
                        .minHeight(800)
                        .maxHeight(800)
                        .round(false)
                        .mode(ViewportMode.kViewportModeMobile)
                        .build(),
                Scaling.ViewportSpecification.builder()
                        .minWidth(600)
                        .maxWidth(600)
                        .minHeight(1024)
                        .maxHeight(1024)
                        .round(false)
                        .mode(ViewportMode.kViewportModeTV)
                        .build()
        };

        List<Scaling.ViewportSpecification> specs = Arrays.asList(specsArray);
        List<ViewportMode> allowModes = Arrays.asList(ViewportMode.kViewportModeHub);
        Scaling scaling = new Scaling(1.0, specs, allowModes);
        APLOptions options = APLOptions.builder().build();

        ViewportMetrics metrics = ViewportMetrics.builder()
                .width(600)
                .height(1024)
                .dpi(160)
                .shape(ScreenShape.RECTANGLE)
                .theme("dark")
                .mode(ViewportMode.kViewportModeMobile)
                .scaling(scaling)
                .build();

        loadDocument(VIEWPORT_MOBILE, options, metrics);

        Assert.assertEquals(600, mRootContext.getPixelWidth());
        Assert.assertEquals(600, mRootContext.getPixelHeight());
    }

    @Test
    public void testAllowModes_allowMode_viewportNotSupported() {
        Scaling.ViewportSpecification[] specsArray = {
                Scaling.ViewportSpecification.builder()
                        .minWidth(800)
                        .maxWidth(800)
                        .minHeight(800)
                        .maxHeight(800)
                        .round(false)
                        .mode(ViewportMode.kViewportModeHub)
                        .build(),
                Scaling.ViewportSpecification.builder()
                        .minWidth(800)
                        .maxWidth(800)
                        .minHeight(1280)
                        .maxHeight(1280)
                        .round(false)
                        .mode(ViewportMode.kViewportModeTV)
                        .build()
        };

        List<Scaling.ViewportSpecification> specs = Arrays.asList(specsArray);
        List<ViewportMode> allowModes = Arrays.asList(ViewportMode.kViewportModeHub);
        Scaling scaling = new Scaling(1.0, specs, allowModes);
        APLOptions options = APLOptions.builder().build();

        ViewportMetrics metrics = ViewportMetrics.builder()
                .width(600)
                .height(1024)
                .dpi(160)
                .shape(ScreenShape.RECTANGLE)
                .theme("dark")
                .mode(ViewportMode.kViewportModeMobile)
                .scaling(scaling)
                .build();

        loadDocument(VIEWPORT_HUB, options, metrics);

        Assert.assertEquals(600, mRootContext.getPixelWidth());
        Assert.assertEquals(600, mRootContext.getPixelHeight());
    }
}

