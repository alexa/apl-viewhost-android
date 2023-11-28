/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android;

import com.amazon.apl.android.bitmap.IBitmapCache;
import com.amazon.apl.android.bitmap.ShadowCache;
import com.amazon.apl.android.robolectric.ViewhostRobolectricTest;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.annotation.NonNull;

public class DocumentLifecycleTest extends ViewhostRobolectricTest {
    @Mock
    private RootContext mRootContext;
    @Mock
    RenderingContext mRenderingContext;
    @Mock
    private IBitmapCache mBitmapCache;
    @Mock
    private ShadowCache mShadowCache;

    private IAPLViewPresenter mPresenter;

    @Before
    public void setUp() {
        when(mRootContext.getOptions()).thenReturn(APLOptions.builder().build());
        when(mRootContext.getRenderingContext()).thenReturn(mRenderingContext);
        when(mRenderingContext.getBitmapCache()).thenReturn(mBitmapCache);
        when(mRenderingContext.getShadowCache()).thenReturn(mShadowCache);
        APLController.setRuntimeConfig(RuntimeConfig.builder().build());
        APLLayout aplLayout = new APLLayout(getApplication(), false);
        mPresenter = aplLayout.getPresenter();
    }

    class LegacyLifecycleListener implements IDocumentLifecycleListener {
        public boolean isDocumentRenderCalled = false;
        public boolean isDocumentDisplayedCalled = false;
        public boolean isDocumentPausedCalled = false;
        public boolean isDocumentResumedCalled = false;
        public boolean isDocumentFinishCalled = false;

        @Override
        public void onDocumentRender(@NonNull RootContext rootContext) {
            isDocumentRenderCalled = true;
        }

        @Override
        public void onDocumentDisplayed() {
            isDocumentDisplayedCalled = true;
        }

        @Override
        public void onDocumentPaused() {
            isDocumentPausedCalled = true;
        }

        @Override
        public void onDocumentResumed() {
            isDocumentResumedCalled = true;
        }

        @Override
        public void onDocumentFinish() {
            isDocumentFinishCalled = true;
        }
    }

    @Test
    public void testListenerCallbacks() {
        LegacyLifecycleListener listenerA = new LegacyLifecycleListener();
        LegacyLifecycleListener listenerB = new LegacyLifecycleListener();
        mPresenter.addDocumentLifecycleListener(listenerA);
        mPresenter.addDocumentLifecycleListener(listenerB);

        mPresenter.onDocumentRender(mRootContext);
        assertTrue(listenerA.isDocumentRenderCalled);
        assertTrue(listenerB.isDocumentRenderCalled);

        mPresenter.onDocumentDisplayed(System.currentTimeMillis());
        assertTrue(listenerA.isDocumentDisplayedCalled);
        assertTrue(listenerB.isDocumentDisplayedCalled);

        mPresenter.onDocumentPaused();
        assertTrue(listenerA.isDocumentPausedCalled);
        assertTrue(listenerB.isDocumentPausedCalled);

        mPresenter.onDocumentResumed();
        assertTrue(listenerA.isDocumentResumedCalled);
        assertTrue(listenerB.isDocumentResumedCalled);

        mPresenter.onDocumentFinish();
        assertTrue(listenerA.isDocumentFinishCalled);
        assertTrue(listenerB.isDocumentFinishCalled);
    }

    @Test
    public void testListenersRemovedOnFinish() {
        LegacyLifecycleListener listener = new LegacyLifecycleListener();
        mPresenter.addDocumentLifecycleListener(listener);

        mPresenter.onDocumentRender(mRootContext);
        mPresenter.onDocumentFinish();
        assertTrue(listener.isDocumentRenderCalled);
        assertTrue(listener.isDocumentFinishCalled);

        mPresenter.onDocumentRender(mRootContext);
        mPresenter.onDocumentDisplayed(System.currentTimeMillis());
        mPresenter.onDocumentPaused();
        mPresenter.onDocumentResumed();
        mPresenter.onDocumentFinish();

        assertFalse(listener.isDocumentDisplayedCalled);
        assertFalse(listener.isDocumentPausedCalled);
        assertFalse(listener.isDocumentResumedCalled);
    }

    class NewLifecycleListener implements IDocumentLifecycleListener {
        public boolean isLegacyMethodCalled = false;
        public boolean isNewMethodCalled = false;

        @Override
        public void onDocumentDisplayed() {
            isLegacyMethodCalled = true;
        }

        @Override
        public void onDocumentDisplayed(long utcTime) {
            isNewMethodCalled = true;
        }
    }

    @Test
    public void testNewLifecycleListenerMethodCalledIfImplemented() {
        NewLifecycleListener listener = new NewLifecycleListener();
        mPresenter.addDocumentLifecycleListener(listener);

        assertFalse(listener.isNewMethodCalled);
        assertFalse(listener.isLegacyMethodCalled);

        mPresenter.onDocumentRender(mRootContext);
        mPresenter.onDocumentDisplayed(System.currentTimeMillis());

        assertTrue(listener.isNewMethodCalled);
        assertFalse(listener.isLegacyMethodCalled);
    }
}


