/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.component;

import android.content.Context;
import android.graphics.Bitmap;

import com.amazon.apl.android.APLOptions;
import com.amazon.apl.android.Image;
import com.amazon.apl.android.dependencies.IImageLoader;
import com.amazon.apl.android.providers.IImageLoaderProvider;
import com.amazon.apl.android.providers.ITelemetryProvider;
import com.amazon.apl.android.views.APLImageView;

import org.junit.Before;
import org.junit.Test;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static com.amazon.apl.android.espresso.APLViewActions.applyProperties;
import static com.amazon.apl.android.espresso.APLViewActions.executeCommands;
import static com.amazon.apl.android.espresso.APLViewActions.waitFor;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

public class ImageViewTest extends AbstractComponentViewTest<APLImageView, Image> {
    private static final String[] SOURCES = {
            "https://via.placeholder.com/100",
            "https://via.placeholder.com/200",
            "https://via.placeholder.com/300",
            "https://via.placeholder.com/400",
            "https://via.placeholder.com/500",
    };

    @Before
    public void doBefore() {
        REQUIRED_PROPERTIES = "\"width\": 100, \"height\": 100";
        OPTIONAL_PROPERTIES =
                " \"source\": \"https://via.placeholder.com/300\"," +
                        " \"opacity\": \".75\"," +
                        " \"scale\": \"best-fit-down\"," +
                        " \"align\": \"bottom-right\"," +
                        " \"overlayColor\": \"red\"," +
                        " \"borderRadius\": \"100dp\"";
    }

    private IImageLoader mImageLoader;
    private static Bitmap dummyBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);

    private APLOptions getOptions() {
        IImageLoaderProvider provider = mock(IImageLoaderProvider.class);
        mImageLoader = mock(IImageLoader.class);
        when(mImageLoader.withTelemetry(any(ITelemetryProvider.class))).thenReturn(mImageLoader);
        when(provider.get(any(Context.class))).thenReturn(mImageLoader);
        doAnswer(invocation -> {
            IImageLoader.LoadImageParams load = invocation.getArgument(0);
            load.callback().onSuccess(dummyBitmap, load.path());
            return null;
        }).when(mImageLoader).loadImage(any(IImageLoader.LoadImageParams.class));

        APLOptions options = APLOptions.builder()
                .imageProvider(provider)
                .build();
        return options;
    }

    @Override
    String getComponentType() {
        return "Image";
    }

    @Override
    Class<APLImageView> getViewClass() {
        return APLImageView.class;
    }

    @Override
    void testView_applyProperties(APLImageView view) {
        // TODO test all the properties
    }

    @Test
    public void testView_dynamicSource() {
        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(inflateWithOptions(getOptions(), REQUIRED_PROPERTIES))
                .check(hasRootContext());

        for (String expectedSource : SOURCES) {
            onView(isRoot())
                    .perform(executeCommands(mTestContext.getRootContext(), setValueCommand("source", expectedSource)));
            verify(mImageLoader).loadImage(argThat(load -> expectedSource.equals(load.path())));
        }
    }

    @Test
    public void testView_applyPropertiesCallsLoadImage() {
        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(inflateWithOptions(getOptions(), REQUIRED_PROPERTIES, OPTIONAL_PROPERTIES))
                .check(hasRootContext());

        // Inflation should trigger call to load image
        verify(mImageLoader).loadImage(argThat(load -> "https://via.placeholder.com/300".equals(load.path())));

        // Reapply properties
        onView(withComponent(getTestComponent()))
                .perform(applyProperties(mTestContext.getPresenter(), getTestComponent()));

        // Apply properties should trigger another call to load image
        verify(mImageLoader, times(2)).loadImage(argThat(load -> "https://via.placeholder.com/300".equals(load.path())));
    }


    @Test
    public void testView_removeResourceOnReinflate() {
        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(inflateWithOptions(getOptions(), REQUIRED_PROPERTIES, OPTIONAL_PROPERTIES))
                .check(hasRootContext());

        APLImageView testView = getTestView();

        onView(isRoot()).perform(executeCommands(mTestContext.getRootContext(), "[{\"type\": \"Reinflate\"}]"));
        onView(isRoot()).perform(waitFor(100));

        verify(mImageLoader, times(1)).clear(testView);
    }

    @Test
    public void testView_layoutRequestsEnabledByDefault() throws Throwable {
        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(inflateWithOptions(getOptions(), REQUIRED_PROPERTIES, OPTIONAL_PROPERTIES))
                .check(hasRootContext());

        APLImageView testView = getTestView();

        activityRule.runOnUiThread(() -> {
            testView.requestLayout();
            assertTrue(testView.isLayoutRequested());
        });
    }

    @Test
    public void testView_layoutRequestsDisabled() throws Throwable {
        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(inflateWithOptions(getOptions(), REQUIRED_PROPERTIES, OPTIONAL_PROPERTIES))
                .check(hasRootContext());

        APLImageView testView = getTestView();

        activityRule.runOnUiThread(() -> {
            testView.setLayoutRequestsEnabled(false);
            testView.requestLayout();

            assertFalse(testView.isLayoutRequested());
        });
    }
}
