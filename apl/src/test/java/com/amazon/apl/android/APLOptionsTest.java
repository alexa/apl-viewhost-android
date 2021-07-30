/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android;

import com.amazon.apl.android.dependencies.impl.DefaultUriSchemeValidator;
import com.amazon.apl.android.providers.impl.GlideImageLoaderProvider;
import com.amazon.apl.android.providers.impl.MediaPlayerProvider;
import com.amazon.apl.android.providers.impl.NoOpTelemetryProvider;
import com.amazon.apl.android.providers.impl.NoOpTtsPlayerProvider;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class APLOptionsTest {

    @Test
    public void test_defaults() {
        APLOptions options = APLOptions.builder().build();
        checkDefaults(options);
    }

    @SuppressWarnings("deprecated")
    @Test
    public void test_deprecatedBuilder() {
        APLOptions deprecatedOptions = APLOptionsBuilder.create().build();
        checkDefaults(deprecatedOptions);
    }

    private void checkDefaults(APLOptions options) {
        assertTrue(options.getTelemetryProvider() instanceof NoOpTelemetryProvider);
        assertTrue(options.getImageProvider() instanceof GlideImageLoaderProvider);
        assertTrue(options.getMediaPlayerProvider() instanceof MediaPlayerProvider);
        assertTrue(options.getImageUriSchemeValidator() instanceof DefaultUriSchemeValidator);
        assertTrue(options.getTtsPlayerProvider() instanceof NoOpTtsPlayerProvider);
    }
}
