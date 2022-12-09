/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.views;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;

import com.amazon.apl.android.IAPLViewPresenter;
import com.amazon.apl.android.robolectric.ViewhostRobolectricTest;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.shadows.ShadowView;

public class APLImageViewTest extends ViewhostRobolectricTest {
    private APLImageView mAplImageView;

    @Mock
    private IAPLViewPresenter mockPresenter;

    @Before
    public void setup() {
        mAplImageView = new APLImageView(getApplication(), mockPresenter);
        shadowOf(mAplImageView).setDidRequestLayout(false);
    }

    @Test
    public void testView_layoutRequestsEnabledByDefault() {
        mAplImageView.requestLayout();
        assertTrue(shadowOf(mAplImageView).didRequestLayout());
    }

    @Test
    public void testView_layoutRequestsDisabled() throws Throwable {
        mAplImageView.setLayoutRequestsEnabled(false);
        mAplImageView.requestLayout();
        assertFalse(shadowOf(mAplImageView).didRequestLayout());
    }
}
