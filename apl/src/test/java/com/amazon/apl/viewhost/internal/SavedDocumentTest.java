/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.viewhost.internal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.amazon.apl.viewhost.DocumentHandle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 32)
public class SavedDocumentTest {

    @Mock
    DocumentHandle mDocumentHandle;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testSavedDocumentFinishesMediatorOnFinish() {
        SavedDocument savedDoc = SavedDocument.builder()
                .documentHandle(mDocumentHandle)
                .build();
        savedDoc.finish();

        verify(mDocumentHandle).getToken();
        verify(mDocumentHandle).finish(any());

        verifyNoMoreInteractions(mDocumentHandle);
    }
}
