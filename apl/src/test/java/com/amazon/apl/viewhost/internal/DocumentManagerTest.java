package com.amazon.apl.viewhost.internal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.amazon.apl.android.robolectric.ViewhostRobolectricTest;
import com.amazon.apl.viewhost.config.EmbeddedDocumentFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class DocumentManagerTest extends ViewhostRobolectricTest {
    @Mock
    EmbeddedDocumentFactory mEmbeddedDocumentFactory;
    @Mock
    Handler mHandler;
    @Mock
    EmbeddedDocumentRequestProxy mEmbeddedDocumentRequestProxy;

    DocumentManager documentManager;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        documentManager = new DocumentManager(mEmbeddedDocumentFactory, mHandler);
    }

    @Test
    public void testRequestEmbeddedDocumentNullSourceUrl() {
        when(mEmbeddedDocumentRequestProxy.getRequestUrl()).thenReturn(null);
        documentManager.requestEmbeddedDocument(mEmbeddedDocumentRequestProxy);
        verifyNoInteractions(mEmbeddedDocumentFactory);
    }

    @Test
    public void testRequestEmbeddedDocumentSuccess() {
        when(mEmbeddedDocumentRequestProxy.getRequestUrl()).thenReturn("url");
        documentManager.requestEmbeddedDocument(mEmbeddedDocumentRequestProxy);
        verify(mEmbeddedDocumentFactory).onDocumentRequested(any(EmbeddedDocumentRequestImpl.class));
    }
}
