/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.component;

import android.net.Uri;

import com.amazon.apl.android.VectorGraphic;
import com.amazon.apl.android.dependencies.IContentRetriever;
import com.amazon.apl.android.graphic.APLVectorGraphicView;
import com.amazon.apl.enums.VectorGraphicAlign;
import com.amazon.apl.enums.VectorGraphicScale;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class VectorGraphicViewAdapterTest extends AbstractComponentViewAdapterTest<VectorGraphic, APLVectorGraphicView> {

    @Mock
    VectorGraphic mComponent;
    @Mock
    IContentRetriever<Uri, String> mContentRetriever;

    @Override
    VectorGraphic component() {
        return mComponent;
    }

    @Override
    void componentSetup() {
        when(component().getAlign()).thenReturn(VectorGraphicAlign.kVectorGraphicAlignCenter);
        when(component().getScale()).thenReturn(VectorGraphicScale.kVectorGraphicScaleNone);
        when(component().hasGraphic()).thenReturn(false);
        when(component().getContentRetriever()).thenReturn(mContentRetriever);
    }

    @Test
    public void contentRetriever_fetchCalled_success() {
        final String avgJson = "{}";
        final String source = "https://www.dummy.json";
        doAnswer(invocation -> {
            IContentRetriever.SuccessCallback<Uri, String> callback = invocation.getArgument(1);
            callback.onSuccess(invocation.getArgument(0), avgJson);
            return null;
        }).when(mContentRetriever).fetch(eq(Uri.parse(source)), any(), any());

        when(component().getSource()).thenReturn(source);
        applyAllProperties();
        verify(mContentRetriever).fetch(eq(Uri.parse(source)), any(), any());

        verify(component()).updateGraphic(avgJson);
    }
}
