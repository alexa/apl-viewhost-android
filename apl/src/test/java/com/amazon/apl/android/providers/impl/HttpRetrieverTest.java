/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.providers.impl;

import com.amazon.apl.android.providers.IDataRetriever.Callback;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.concurrent.ExecutorService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class HttpRetrieverTest {

    @Mock
    private Callback mockCallback;

    @Mock
    private ExecutorService mockExecutorService;

    /* tests if fetch correctly sets up a threadpool and runs the function */
    @Test
    public void test_fetchSuccessful () {
        HttpRetriever retriever = spy(new HttpRetriever(mockExecutorService));
        doNothing().when(retriever).getData(anyString(), any(Callback.class));

        retriever.fetch("http://test", mockCallback);
        ArgumentCaptor<Runnable> runnableArgumentCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(mockExecutorService).submit(runnableArgumentCaptor.capture());
        Runnable runnable = runnableArgumentCaptor.getValue();
        runnable.run();
        verify(retriever).getData("http://test", mockCallback);
    }
}
