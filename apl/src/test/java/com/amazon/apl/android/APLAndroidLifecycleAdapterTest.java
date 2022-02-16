/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android;

import com.amazon.apl.android.robolectric.ViewhostRobolectricTest;
import com.amazon.apl.android.APLVersionCodes;
import com.amazon.apl.enums.DisplayState;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.InOrder;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class APLAndroidLifecycleAdapterTest extends ViewhostRobolectricTest {
    @Mock
    private APLController mController;

    @Test
    public void testLegacyBehavior() {
        when(mController.getDocVersion()).thenReturn(APLVersionCodes.APL_1_7);

        APLAndroidLifecycleAdapter adapter = new APLAndroidLifecycleAdapter(mController);
        verify(mController).getDocVersion();

        InOrder inOrder = inOrder(mController);
        adapter.onStart();
        inOrder.verify(mController).updateDisplayState(eq(DisplayState.kDisplayStateBackground));
        inOrder.verify(mController, never()).resumeDocument();

        adapter.onResume();
        inOrder.verify(mController).updateDisplayState(eq(DisplayState.kDisplayStateForeground));
        inOrder.verify(mController).resumeDocument();

        adapter.onPause();
        inOrder.verify(mController).updateDisplayState(eq(DisplayState.kDisplayStateBackground));
        inOrder.verify(mController).pauseDocument();

        adapter.onStop();
        inOrder.verify(mController).updateDisplayState(eq(DisplayState.kDisplayStateHidden));
        inOrder.verify(mController, never()).pauseDocument();
    }

    @Test
    public void testBehaviorWithBackgroundProcessing() {
        when(mController.getDocVersion()).thenReturn(APLVersionCodes.APL_1_8);

        APLAndroidLifecycleAdapter adapter = new APLAndroidLifecycleAdapter(mController);
        verify(mController).getDocVersion();

        InOrder inOrder = inOrder(mController);
        adapter.onStart();
        inOrder.verify(mController).updateDisplayState(eq(DisplayState.kDisplayStateBackground));
        inOrder.verify(mController).resumeDocument();

        adapter.onResume();
        inOrder.verify(mController).updateDisplayState(eq(DisplayState.kDisplayStateForeground));
        inOrder.verify(mController, never()).resumeDocument();

        adapter.onPause();
        inOrder.verify(mController).updateDisplayState(eq(DisplayState.kDisplayStateBackground));
        inOrder.verify(mController, never()).pauseDocument();

        adapter.onStop();
        inOrder.verify(mController).updateDisplayState(eq(DisplayState.kDisplayStateHidden));
        inOrder.verify(mController).pauseDocument();
    }
}
