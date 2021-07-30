/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.content;

import android.content.ContentResolver;
import android.net.Uri;

import com.amazon.apl.android.robolectric.ViewhostRobolectricTest;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

public class LocalContentRequestHandlerTest extends ViewhostRobolectricTest {
    @Mock
    private ContentResolver mContentResolver;

    private LocalContentRequestHandler mRequestHandler;

    @Before
    public void setup() {
        mRequestHandler = new LocalContentRequestHandler(mContentResolver);
    }

    @Test
    public void testFetch_success() throws IOException {
        Uri uri = Uri.parse("content://mycontent");
        String data = "my data";
        InputStream inputStream = new ByteArrayInputStream(data.getBytes());
        when(mContentResolver.openInputStream(uri)).thenReturn(inputStream);

        AtomicBoolean successCalled = new AtomicBoolean();
        AtomicBoolean failureCalled = new AtomicBoolean();
        mRequestHandler.fetch(uri,
                (source, result) -> {
                    successCalled.set(true);
                    assertEquals(data, result);
                },
                (source, failure) -> failureCalled.set(true)
        );

        assertTrue(successCalled.get());
        assertFalse(failureCalled.get());
    }

    @Test
    public void testFetch_failure_findingFile() throws IOException {
        Uri uri = Uri.parse("content://mycontent");
        when(mContentResolver.openInputStream(uri)).thenThrow(new FileNotFoundException("not found"));

        AtomicBoolean successCalled = new AtomicBoolean();
        AtomicBoolean failureCalled = new AtomicBoolean();
        mRequestHandler.fetch(uri,
                (source, result) -> {
                    successCalled.set(true);
                },
                (source, failure) -> {
                    failureCalled.set(true);
                    assertEquals("not found", failure);
                }
        );

        assertFalse(successCalled.get());
        assertTrue(failureCalled.get());
    }
}
