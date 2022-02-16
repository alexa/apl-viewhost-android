/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.component;

import android.net.Uri;

import com.amazon.apl.android.RenderingContext;
import com.amazon.apl.android.VectorGraphic;
import com.amazon.apl.android.dependencies.IContentRetriever;
import com.amazon.apl.android.graphic.APLVectorGraphicView;
import com.amazon.apl.android.graphic.AlexaVectorDrawable;
import com.amazon.apl.android.graphic.GraphicContainerElement;
import com.amazon.apl.android.primitive.UrlRequests;
import com.amazon.apl.enums.PropertyKey;
import com.amazon.apl.enums.VectorGraphicAlign;
import com.amazon.apl.enums.VectorGraphicScale;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class VectorGraphicViewAdapterTest extends AbstractComponentViewAdapterTest<VectorGraphic, APLVectorGraphicView> {

    @Mock
    private VectorGraphic mComponent;
    @Mock
    private IContentRetriever<Uri, String> mContentRetriever;
    @Mock
    private GraphicContainerElement mockGraphicContainerElement;
    @Mock
    private RenderingContext mockRenderingContext;
    private Set<Integer> mDirtyGraphicsSet = new HashSet<>();

    @Override
    VectorGraphic component() {
        return mComponent;
    }

    @Override
    void componentSetup() {
        when(mockGraphicContainerElement.getRenderingContext()).thenReturn(mockRenderingContext);
        when(component().getAlign()).thenReturn(VectorGraphicAlign.kVectorGraphicAlignCenter);
        when(component().getScale()).thenReturn(VectorGraphicScale.kVectorGraphicScaleNone);
        when(component().hasGraphic()).thenReturn(false);
        when(component().getContentRetriever()).thenReturn(mContentRetriever);
        when(component().getOrCreateGraphicContainerElement()).thenReturn(mockGraphicContainerElement);
        when(component().getRenderingContext()).thenReturn(mockRenderingContext);
        when(component().getDirtyGraphics()).thenReturn(mDirtyGraphicsSet);
        final UrlRequests.UrlRequest request = UrlRequests.UrlRequest.builder().url("").build();
        when(component().getSourceRequest()).thenReturn(request);

    }

    @Test
    public void contentRetriever_fetchV2Called_success() {
        final String avgJson = "{}";
        final String source = "https://www.dummy.json";
        final UrlRequests.UrlRequest request = UrlRequests.UrlRequest.builder().url(source).build();
        doAnswer(invocation -> {
            IContentRetriever.SuccessCallback<Uri, String> callback = invocation.getArgument(2);
            callback.onSuccess(invocation.getArgument(0), avgJson);
            return null;
        }).when(mContentRetriever).fetchV2(eq(Uri.parse(source)), anyMap(), any(), any());

        when(component().getSourceRequest()).thenReturn(request);
        applyAllProperties();
        verify(mContentRetriever).fetchV2(eq(Uri.parse(source)), anyMap(), any(), any());

        verify(component()).updateGraphic(avgJson);
    }

    @Test
    public void contentRetriever_fetchV2Called_failure() {
        final String avgJson = "{}";
        final String source = "https://www.dummy.json";
        final UrlRequests.UrlRequest request = UrlRequests.UrlRequest.builder().url(source).build();
        doAnswer(invocation -> {
            IContentRetriever.FailureCallbackV2<Uri> callback = invocation.getArgument(3);
            callback.onFailure(invocation.getArgument(0), "", 0);
            return null;
        }).when(mContentRetriever).fetchV2(eq(Uri.parse(source)), anyMap(), any(), any());

        when(component().getSourceRequest()).thenReturn(request);
        applyAllProperties();
        verify(mContentRetriever).fetchV2(eq(Uri.parse(source)), anyMap(), any(), any());

        verify(component(), never()).updateGraphic(avgJson);
    }

    @Test
    public void contentRetriever_fetchV2Called_withHeaders() {
        final String avgJson = "{}";
        final String source = "https://www.dummy.json";
        final String headerKey = "headerKey";
        final String headerValue = "headerValue";
        final Map<String, String> headers = Collections.singletonMap(headerKey, headerValue);
        final UrlRequests.UrlRequest request = UrlRequests.UrlRequest.builder()
                .url(source)
                .headers(headers)
                .build();

        doAnswer(invocation -> {
            IContentRetriever.SuccessCallback<Uri, String> callback = invocation.getArgument(2);
            callback.onSuccess(invocation.getArgument(0), avgJson);
            return null;
        }).when(mContentRetriever).fetchV2(eq(Uri.parse(source)), anyMap(), any(), any());

        when(component().getSourceRequest()).thenReturn(request);
        applyAllProperties();
        ArgumentCaptor<Map<String, String>> headerCaptor = ArgumentCaptor.forClass(Map.class);
        verify(mContentRetriever).fetchV2(eq(Uri.parse(source)), headerCaptor.capture(), any(), any());
        Map<String, String> headerResult = headerCaptor.getValue();
        assertTrue(headerResult.containsKey(headerKey));
        assertEquals(headerValue, headerResult.get(headerKey));
        verify(component()).updateGraphic(avgJson);
    }

    @Test
    public void testCreateVectorDrawable() {
        when(component().hasGraphic()).thenReturn(true);
        assertNull(getView().getDrawable());
        applyAllProperties();
        assertNotNull(getView().getDrawable());
    }

    @Test
    public void testRefreshProperties_null_drawable_creates_drawable() {
        assertNull(getView().getDrawable());
        refreshProperties(getView(), PropertyKey.kPropertyGraphic);
        assertNotNull(getView().getDrawable());
    }

    @Test
    public void testRefreshProperties_nonNull_drawable_updates_drawable() {
        APLVectorGraphicView spyView = spy(getView());
        AlexaVectorDrawable mockDrawable = mock(AlexaVectorDrawable.class);
        when(spyView.getDrawable()).thenReturn(mockDrawable);
        refreshProperties(spyView, PropertyKey.kPropertyGraphic);
        verify(mockDrawable).updateDirtyGraphics(mDirtyGraphicsSet);
    }
}
