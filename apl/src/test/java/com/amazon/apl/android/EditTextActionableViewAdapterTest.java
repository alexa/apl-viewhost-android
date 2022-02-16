/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android;

import com.amazon.apl.android.font.TypefaceResolver;
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

        when(proxy().getTypefaceResolver()).thenReturn(mockTypefaceResolver);
        when(proxy().getSubmitKeyType()).thenReturn(kSubmitKeyTypeDone);
        when(proxy().getKeyboardType()).thenReturn(KeyboardType.kKeyboardTypeNormal);
        when(proxy().getText()).thenReturn("");
    }
}
