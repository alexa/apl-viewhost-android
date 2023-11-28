package com.amazon.apl.viewhost.internal;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.os.Handler;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.amazon.apl.android.Content;
import com.amazon.apl.android.robolectric.ViewhostRobolectricTest;
import com.amazon.apl.viewhost.config.EmbeddedDocumentFactory.EmbeddedDocumentRequest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;

@RunWith(AndroidJUnit4.class)
public class EmbeddedDocumentRequestTest extends ViewhostRobolectricTest {
    EmbeddedDocumentRequest embeddedDocumentRequest;
    @Mock
    EmbeddedDocumentRequestProxy mEmbeddedDocumentRequestProxy;
    @Mock
    Handler mHandler;
    @Mock
    PreparedDocumentImpl mPreparedDocument;
    @Mock
    DocumentHandleImpl mDocumentHandle;
    @Mock
    Content mContent;

    @Before
    public void setUp() {
        embeddedDocumentRequest = new EmbeddedDocumentRequestImpl(mEmbeddedDocumentRequestProxy, mHandler);
        when(mHandler.post(any(Runnable.class))).thenAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            task.run();
            return null;
        });
    }

    @Test
    public void testResolve() {
        when(mPreparedDocument.getHandle()).thenReturn(mDocumentHandle);
        embeddedDocumentRequest.resolve(mPreparedDocument);
        verify(mDocumentHandle).registerStateChangeListener(any());
    }

    @Test
    public void testFail() {
        embeddedDocumentRequest.fail("reason");
        verify(mEmbeddedDocumentRequestProxy).failure(any());
    }

    @Test
    public void testOnDocumentStateChangedToError() {
        ((EmbeddedDocumentRequestImpl)embeddedDocumentRequest).onDocumentStateChanged(DocumentState.ERROR);
        verify(mEmbeddedDocumentRequestProxy).failure(any());
    }

    @Test
    public void testOnDocumentStateChangedToPrepared() {
        ((EmbeddedDocumentRequestImpl)embeddedDocumentRequest).setDocumentHandle(mDocumentHandle);
        when(mDocumentHandle.getContent()).thenReturn(mContent);
        ((EmbeddedDocumentRequestImpl)embeddedDocumentRequest).onDocumentStateChanged(DocumentState.PREPARED);
        verify(mEmbeddedDocumentRequestProxy).success(anyLong(), anyBoolean(), anyLong());
    }

    @Test
    public void testSuccessCallbackReturnsNullDocumentContext_setsTerminalDocumentState() {
        // given that a null DocumentContext is returned
        ((EmbeddedDocumentRequestImpl)embeddedDocumentRequest).setDocumentHandle(mDocumentHandle);
        when(mDocumentHandle.getContent()).thenReturn(mContent);
        when(mEmbeddedDocumentRequestProxy.success(anyLong(), anyBoolean(), anyLong())).thenReturn(null);
        // when
        ((EmbeddedDocumentRequestImpl)embeddedDocumentRequest).onDocumentStateChanged(DocumentState.PREPARED);
        // then the DocumentHandle is in a terminal state
        assertEquals(mDocumentHandle.isValid(), false);
    }
}
