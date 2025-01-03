/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.amazon.common.NativeBinding;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 32)
public class DocumentStateTest {

    private static final long ROOT_CONTEXT_NATIVE_HANDLE = 1L;

    @Mock
    Content mContent;

    @Mock
    RootContext mRootContext;

    @Mock
    RootConfig mRootConfig;

    @Mock
    ExtensionMediator mExtensionMediator;

    @Captor
    private ArgumentCaptor<DocumentState> documentStateArgumentCaptor;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mRootContext.getNativeHandle()).thenReturn(ROOT_CONTEXT_NATIVE_HANDLE);
        when(mRootConfig.getExtensionMediator()).thenReturn(mExtensionMediator);
        when(mRootContext.getRootConfig()).thenReturn(mRootConfig);
    }

    @Test
    public void testDocumentStateFinishesMediatorOnFinish() {
        try(MockedStatic mockedStatic = Mockito.mockStatic(NativeBinding.class, Mockito.CALLS_REAL_METHODS)) {
            mockedStatic.when(() -> NativeBinding.register(any())).thenAnswer((Answer<Void>) invocation -> null);
            DocumentState state = new DocumentState(mRootContext, mContent, 1);
            state.finish();

            verify(mExtensionMediator).finish();

            verifyNoMoreInteractions(mExtensionMediator);

            mockedStatic.verify(() -> NativeBinding.register(documentStateArgumentCaptor.capture()));
            DocumentState actual = documentStateArgumentCaptor.getValue();
            assertEquals(ROOT_CONTEXT_NATIVE_HANDLE, actual.getNativeHandle());
        }
    }
}
