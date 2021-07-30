/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.component;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;

import com.amazon.apl.android.APLOptions;
import com.amazon.apl.android.Image;
import com.amazon.apl.android.bitmap.IBitmapCache;
import com.amazon.apl.android.dependencies.IImageLoader;
import com.amazon.apl.android.providers.IImageLoaderProvider;
import com.amazon.apl.android.providers.ITelemetryProvider;
import com.amazon.apl.android.views.APLImageView;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static com.amazon.apl.android.espresso.APLViewActions.applyProperties;
import static com.amazon.apl.android.espresso.APLViewActions.executeCommands;
import static com.amazon.apl.android.espresso.APLViewActions.waitFor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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

    private static final Map<String, Integer> COLORS = new HashMap<>();
    static {
        COLORS.put("blue", Color.BLUE);
        COLORS.put("red", Color.RED);
        COLORS.put("black", Color.BLACK);
        COLORS.put("yellow", Color.YELLOW);
        COLORS.put("white", Color.WHITE);
    }

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
            IImageLoader.LoadImageCallback2 callback = invocation.getArgument(2);
            callback.onSuccess(dummyBitmap, invocation.getArgument(0));
            return null;
        }).when(mImageLoader).loadImage(anyString(), any(APLImageView.class), any(IImageLoader.LoadImageCallback2.class), anyBoolean());

        APLOptions options = APLOptions.builder()
                .imageProvider(provider)
                .bitmapCache(mock(IBitmapCache.class))
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

        APLImageView view = getTestView();
        for (String expectedSource : SOURCES) {
            onView(isRoot())
                    .perform(executeCommands(mTestContext.getRootContext(), setValueCommand("source", expectedSource)));
            verify(mImageLoader).loadImage(eq(expectedSource), eq(view), any(IImageLoader.LoadImageCallback2.class), anyBoolean());
        }
    }

    @Test
    public void testView_applyPropertiesCallsLoadImage() {
        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(inflateWithOptions(getOptions(), REQUIRED_PROPERTIES, OPTIONAL_PROPERTIES))
                .check(hasRootContext());

        // Inflation should trigger call to load image
        verify(mImageLoader).loadImage(eq("https://via.placeholder.com/300"), eq(getTestView()), any(IImageLoader.LoadImageCallback2.class), anyBoolean());

        // Reapply properties
        onView(withComponent(getTestComponent()))
                .perform(applyProperties(mTestContext.getPresenter(), getTestComponent()));

        // Apply properties should trigger another call to load image
        verify(mImageLoader, times(2)).loadImage(eq("https://via.placeholder.com/300"), eq(getTestView()), any(IImageLoader.LoadImageCallback2.class), anyBoolean());
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


    // TODO verify overlayColor changing.
    //  this is hard because we have two asynchronous methods that are being called when images are
    //  loaded:
    //      1) IImageLoader.loadImage()
    //      2) ImageProcessingAsyncTask
    //  To test overlayColor, we need to wait for both tasks to finish and then get the color from the
    //  ImageView drawable.
//    @Test
//    public void testView_dynamicOverlayColor() {
//        onView(withId(com.amazon.apl.android.test.R.id.apl))
//                .perform(inflate(REQUIRED_PROPERTIES, "\"source\": \"https://via.placeholder.com/100\""))
//                .check(hasRootContext());
//
//        onView(isRoot())
//                .perform(waitFor(1000));
//
//        for (String expectedColor : COLORS.keySet()) {
//            onView(withComponent(getTestComponent()))
//                    .perform(executeCommands(mTestContext.getRootContext(), setValueCommand("overlayColor", expectedColor)))
//                    .check((view, exception) -> {
//                            Bitmap bitmap = drawableToBitmap(((APLImageView)view).getDrawable());
//                            assertEquals((int)COLORS.get(expectedColor), bitmap.getPixel(0,0));
//                    });
//        }
//    }
}
