/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android;

import com.amazon.apl.android.font.TypefaceResolver;
import com.amazon.apl.android.primitive.Dimension;
import com.amazon.apl.android.scaling.MetricsTransform;
import com.amazon.apl.android.views.APLEditText;
import com.amazon.apl.enums.KeyboardType;

import org.mockito.Mock;

import static com.amazon.apl.enums.SubmitKeyType.kSubmitKeyTypeDone;
import static org.mockito.Mockito.when;

public class EditTextActionableViewAdapterTest extends AbstractActionableComponentViewAdapterTest<EditText, APLEditText> {

    @Mock
    private EditText mEditText;
    @Mock
    private EditTextProxy mEditTextProxy;
    @Mock
    private TypefaceResolver mockTypefaceResolver;
    @Mock
    private RenderingContext mRenderingContext;
    @Mock
    private MetricsTransform mMetricsTransform;

    @Override
    EditText component() {
        return mEditText;
    }

    EditTextProxy proxy() {
        return mEditTextProxy;
    }

    void componentSetup() {
        when(component().getViewPresenter()).thenReturn(mMockPresenter);
        when(component().getProxy()).thenReturn(proxy());
        when(mEditText.getRenderingContext()).thenReturn(mRenderingContext);
        when(mRenderingContext.getMetricsTransform()).thenReturn(mMetricsTransform);
        when(mMetricsTransform.toCore(mEditTextProxy.getFontSize())).thenReturn(40.0f);
        when(proxy().getFontSize()).thenReturn(40.0f);
        when(proxy().getTypefaceResolver()).thenReturn(mockTypefaceResolver);
        when(proxy().getSubmitKeyType()).thenReturn(kSubmitKeyTypeDone);
        when(proxy().getKeyboardType()).thenReturn(KeyboardType.kKeyboardTypeNormal);
        when(proxy().getText()).thenReturn("");
    }
}
