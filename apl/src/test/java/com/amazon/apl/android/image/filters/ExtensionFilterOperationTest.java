/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.image.filters;

import android.graphics.Bitmap;
import android.graphics.Color;

import com.amazon.apl.android.bitmap.BitmapCreationException;
import com.amazon.apl.android.bitmap.IBitmapFactory;
import com.amazon.apl.android.dependencies.ExtensionFilterParameters;
import com.amazon.apl.android.image.filters.bitmap.BitmapFilterResult;
import com.amazon.apl.android.image.filters.bitmap.ColorFilterResult;
import com.amazon.apl.android.image.filters.bitmap.FilterResult;
import com.amazon.apl.android.primitive.Filters;
import com.amazon.apl.enums.FilterType;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ExtensionFilterOperationTest extends FilterOperationTest<ExtensionFilterOperation> {
    private final String mUri = "aplext:imageprocessor:10";
    private final String mName = "MyFilter";
    private final Map<String, Object> mParams = new HashMap<>();

    @Test
    public void testExtensionFilter_source() {
        init(Collections.singletonList(createDummyBitmap()),
                Filters.Filter.builder()
                .filterType(FilterType.kFilterTypeExtension)
                        .source(0)
                        .destination(null)
                        .name(mName)
                        .extensionParams(mParams)
                        .extensionURI(mUri)
                        .build());

        FilterResult result = getFilterOperation().call();
        assertNotNull(result);
        verify(mExtensionImageFilterCallback).processImage(any(), isNull(), eq(ExtensionFilterParameters.create(mUri, mName, 0, null, mParams)));
    }

    @Test
    public void testExtensionFilter_sourceAndDestination() {
        FilterResult source = createDummyBitmap();
        FilterResult dest = createDummyBitmap();
        init(Arrays.asList(source, dest),
                Filters.Filter.builder()
                        .filterType(FilterType.kFilterTypeExtension)
                        .source(0)
                        .destination(1)
                        .name(mName)
                        .extensionParams(mParams)
                        .extensionURI(mUri)
                        .build());

        FilterResult result = getFilterOperation().call();
        assertNotNull(result);
        verify(mExtensionImageFilterCallback).processImage(eq(source.getBitmap()), eq(dest.getBitmap()), eq(ExtensionFilterParameters.create(mUri, mName, 0, 1, mParams)));
    }

    @Test
    public void testExtensionFilter_null() {
        init(Collections.emptyList(), Filters.Filter.builder()
                .filterType(FilterType.kFilterTypeExtension)
                .name(mName)
                .extensionParams(mParams)
                .extensionURI(mUri)
                .build());

        FilterResult result = getFilterOperation().call();
        assertNotNull(result);
        verify(mExtensionImageFilterCallback).processImage(isNull(), isNull(), eq(ExtensionFilterParameters.create(mUri, mName, null, null, mParams)));
    }

    @Test
    public void testExtensionFilter_scalesNonBitmap() throws BitmapCreationException {
        Bitmap blue = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        Bitmap img = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888);
        IBitmapFactory mockFactory = mock(IBitmapFactory.class);
        when(mockFactory.createBitmap(10, 10)).thenReturn(blue);
        FilterResult color = new ColorFilterResult(Color.BLUE, mockFactory);
        FilterResult source = new BitmapFilterResult(img, mBitmapFactory);

        init(Arrays.asList(color, source),
                Filters.Filter.builder()
                        .filterType(FilterType.kFilterTypeExtension)
                        .source(0)
                        .destination(1)
                        .name(mName)
                        .extensionParams(mParams)
                        .extensionURI(mUri)
                        .build());

        FilterResult result = getFilterOperation().call();
        assertNotNull(result);
        verify(mExtensionImageFilterCallback).processImage(eq(blue), eq(img),  eq(ExtensionFilterParameters.create(mUri, mName, 0, 1, mParams)));
        verify(mockFactory).createBitmap(10, 10);
    }

    @Test
    public void testExtensionFilter_sourceAndDestNotBitmaps_throws() {
        FilterResult blue = new ColorFilterResult(Color.BLUE, mBitmapFactory);
        FilterResult black = new ColorFilterResult(Color.BLACK, mBitmapFactory);

        init(Arrays.asList(blue, black),
                Filters.Filter.builder()
                        .filterType(FilterType.kFilterTypeExtension)
                        .source(0)
                        .destination(1)
                        .name(mName)
                        .extensionParams(mParams)
                        .extensionURI(mUri)
                        .build());

        try {
            getFilterOperation().call();
            fail("Exception expected");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }
}
