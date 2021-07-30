/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android;

import com.amazon.apl.android.robolectric.ViewhostRobolectricTest;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class DocumentLifecycleTest extends ViewhostRobolectricTest {
    @Mock
    private IDocumentLifecycleListener mListenerA;
    @Mock
    private IDocumentLifecycleListener mListenerB;
    @Mock
    private RootContext mRootContext;
    @Mock
    RenderingContext mRenderingContext;

    private IAPLViewPresenter mPresenter;

    @Before
    public void setUp() {
        when(mRootContext.getOptions()).thenReturn(APLOptions.builder().build());
        when(mRootContext.getRenderingContext()).thenReturn(mRenderingContext);
        APLLayout aplLayout = new APLLayout(getApplication(), false);
        mPresenter = aplLayout.getPresenter();
        mPresenter.addDocumentLifecycleListener(mListenerA);
        mPresenter.addDocumentLifecycleListener(mListenerB);
    }

    @Test
    public void testListenerCallbacks() {
        mPresenter.onDocumentRender(mRootContext);
        verify(mListenerA).onDocumentRender(mRootContext);
        verify(mListenerB).onDocumentRender(mRootContext);

        mPresenter.onDocumentDisplayed();
        verify(mListenerA).onDocumentDisplayed();
        verify(mListenerB).onDocumentDisplayed();

        mPresenter.onDocumentPaused();
        verify(mListenerA).onDocumentPaused();
        verify(mListenerB).onDocumentPaused();

        mPresenter.onDocumentResumed();
        verify(mListenerA).onDocumentResumed();
        verify(mListenerB).onDocumentResumed();

        mPresenter.onDocumentFinish();
        verify(mListenerA).onDocumentFinish();
        verify(mListenerB).onDocumentFinish();
    }

    @Test
    public void testListenersRemovedOnFinish() {
        mPresenter.onDocumentRender(mRootContext);
        verify(mListenerA).onDocumentRender(mRootContext);
        verify(mListenerB).onDocumentRender(mRootContext);

        mPresenter.onDocumentFinish();
        verify(mListenerA).onDocumentFinish();
        verify(mListenerB).onDocumentFinish();

        mPresenter.onDocumentRender(mRootContext);
        mPresenter.onDocumentDisplayed();
        mPresenter.onDocumentPaused();
        mPresenter.onDocumentResumed();
        mPresenter.onDocumentFinish();

        verifyNoMoreInteractions(mListenerA);
        verifyNoMoreInteractions(mListenerB);
    }
}


