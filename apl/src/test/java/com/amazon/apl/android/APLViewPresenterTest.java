/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android;

import com.amazon.apl.android.providers.impl.NoOpTelemetryProvider;
import com.amazon.apl.android.robolectric.ViewhostRobolectricTest;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.robolectric.RuntimeEnvironment;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class APLViewPresenterTest extends ViewhostRobolectricTest {

    @Mock
    private RootContext mockRootContext;
    @Mock
    private APLOptions mockAPLOptions;
    @Mock
    private RenderingContext mockRenderingContext;

    @Before
    public void setup() {
        APLController.setRuntimeConfig(RuntimeConfig.builder().build());
    }

    @Test
    public void testPresenterUpdatesVisualContext() {
        when(mockAPLOptions.getTelemetryProvider()).thenReturn(NoOpTelemetryProvider.getInstance());
        when(mockRootContext.getOptions()).thenReturn(mockAPLOptions);
        when(mockRootContext.getRenderingContext()).thenReturn(mockRenderingContext);

        IDocumentLifecycleListener lifeCycleListener = mock(IDocumentLifecycleListener.class);

        APLLayout view = new APLLayout(RuntimeEnvironment.systemContext, false);
        view.getPresenter().addDocumentLifecycleListener(lifeCycleListener);

        view.getPresenter().onDocumentRender(mockRootContext);
        
        InOrder inOrder = inOrder(lifeCycleListener, mockRootContext);
        inOrder.verify(lifeCycleListener).onDocumentRender(mockRootContext);
        inOrder.verify(mockRootContext).notifyVisualContext();
    }
}
