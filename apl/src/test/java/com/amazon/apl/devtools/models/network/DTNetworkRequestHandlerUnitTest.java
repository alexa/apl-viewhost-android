/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.models.network;

import com.amazon.apl.devtools.enums.DTNetworkRequestType;
import com.amazon.apl.devtools.models.ViewTypeTarget;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import org.mockito.MockitoAnnotations;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class DTNetworkRequestHandlerUnitTest {
    private static final String SAMPLE_URL = "https://sample.com";
    @Mock
    private ViewTypeTarget mViewTypeTarget;

    private ArgumentCaptor<Runnable> mTargetArgumentCaptor;
    private DTNetworkRequestHandler mDTNetworkRequestHandler;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        mTargetArgumentCaptor = ArgumentCaptor.forClass(Runnable.class);
        mDTNetworkRequestHandler = new DTNetworkRequestHandler(mViewTypeTarget);
    }

    @Test
    public void requestWillBeSent_onJobSchedulesAndExecutes() {
        // Given
        int requestId = IDTNetworkRequestHandler.IdGenerator.generateId();

        // When
        mDTNetworkRequestHandler.requestWillBeSent(requestId, 0, SAMPLE_URL, DTNetworkRequestType.IMAGE);

        // Then
        verify(mViewTypeTarget).post(mTargetArgumentCaptor.capture());
        Runnable job = mTargetArgumentCaptor.getValue();
        job.run();
        verify(mViewTypeTarget,times(1))
                .onNetworkRequestWillBeSent(eq(requestId), anyDouble(), eq(SAMPLE_URL), eq(DTNetworkRequestType.IMAGE.toString()));
    }

    @Test
    public void loadingFailed_onJobSchedulesAndExecutes() {
        // Given
        int requestId = IDTNetworkRequestHandler.IdGenerator.generateId();

        // When
        mDTNetworkRequestHandler.loadingFailed(requestId, 0);

        // Then
        verify(mViewTypeTarget).post(mTargetArgumentCaptor.capture());
        Runnable job = mTargetArgumentCaptor.getValue();
        job.run();
        verify(mViewTypeTarget,times(1)).onNetworkLoadingFailed(eq(requestId), anyDouble());
    }

    @Test
    public void loadingFinished_onJobSchedulesAndExecutes() {
        // Given
        int requestId = IDTNetworkRequestHandler.IdGenerator.generateId();

        // When
        mDTNetworkRequestHandler.loadingFinished(requestId, 0, 1000);

        // Then
        verify(mViewTypeTarget).post(mTargetArgumentCaptor.capture());
        Runnable job = mTargetArgumentCaptor.getValue();
        job.run();
        verify(mViewTypeTarget,times(1)).onNetworkLoadingFinished(eq(requestId), anyDouble(), anyInt());
    }
}
