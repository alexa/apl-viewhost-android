package com.amazon.apl.viewhost.config;

import static com.amazon.apl.viewhost.config.EmbeddedDocumentFactory.EmbeddedDocumentRequest;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.test.ext.junit.runners.AndroidJUnit4;


import com.amazon.apl.android.providers.IDataRetriever;
import com.amazon.apl.android.providers.IDataRetriever.Callback;
import com.amazon.apl.android.robolectric.ViewhostRobolectricTest;
import com.amazon.apl.viewhost.PreparedDocument;
import com.amazon.apl.viewhost.Viewhost;
import com.amazon.apl.viewhost.request.PrepareDocumentRequest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class EmbeddedDocumentFactoryTest  extends ViewhostRobolectricTest {
    @Mock
    private EmbeddedDocumentRequest mEmbeddedDocumentRequest;

    @Mock
    private IDataRetriever mDataRetriever;

    @Mock
    private Viewhost mViewhost;

    @Mock
    private PreparedDocument mPreparedDocument;

    private EmbeddedDocumentFactory mDefaultEmbeddedDocumentFactory;

    private static final String INVALID_URL = "url";
    private static final String RENDER_DOCUMENT_PAYLOAD = "{\n" +
            "  \"name\": \"RenderDocument\",\n" +
            "  \"payload\": {\n" +
            "    \"document\": {},\n" +
            "    \"datasources\": {}\n" +
            "  }\n" +
            "}";
    private static final String RENDER_DOCUMENT_WITHOUT_PAYLOAD = "{\n" +
            "  \"name\": \"RenderDocument\",\n" +
            "  \"namespace\": \"Alexa.Presentation.APL\",\n" +
            "  \"document\": {},\n" +
            "  \"datasources\": {}\n" +
            "}";
    private static final String PAYLOAD = "{}";
    private static final String NON_JSON_PAYLOAD = "non-json";
    private static final String VALID_URL = "http://something.json";

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        mDefaultEmbeddedDocumentFactory = new DefaultEmbeddedDocumentFactory(mViewhost, mDataRetriever);
    }

    @Test
    public void testOnDocumentRequestedInvalidUrl() {
        when(mEmbeddedDocumentRequest.getSource()).thenReturn(INVALID_URL);
        mDefaultEmbeddedDocumentFactory.onDocumentRequested(mEmbeddedDocumentRequest);
        verify(mEmbeddedDocumentRequest).fail(anyString());
    }

    @Test
    public void testOnDocumentRequestedSuccessRenderDocumentDirectivePayload() {
        when(mEmbeddedDocumentRequest.getSource()).thenReturn(VALID_URL);
        when(mViewhost.prepare(any(PrepareDocumentRequest.class))).thenReturn(mPreparedDocument);
        doAnswer(invocation -> {
            Callback callback = invocation.getArgument(1);
            callback.success(RENDER_DOCUMENT_PAYLOAD);
            return null;
        }).when(mDataRetriever).fetch(anyString(), any(Callback.class));

        mDefaultEmbeddedDocumentFactory.onDocumentRequested(mEmbeddedDocumentRequest);
        verify(mEmbeddedDocumentRequest).resolve(mPreparedDocument);
    }

    @Test
    public void testOnDocumentRequestedSuccessRenderDocumentDirectiveWithoutPayload() {
        when(mEmbeddedDocumentRequest.getSource()).thenReturn(VALID_URL);
        when(mViewhost.prepare(any(PrepareDocumentRequest.class))).thenReturn(mPreparedDocument);
        doAnswer(invocation -> {
            Callback callback = invocation.getArgument(1);
            callback.success(RENDER_DOCUMENT_WITHOUT_PAYLOAD);
            return null;
        }).when(mDataRetriever).fetch(anyString(), any(Callback.class));

        mDefaultEmbeddedDocumentFactory.onDocumentRequested(mEmbeddedDocumentRequest);
        verify(mEmbeddedDocumentRequest).resolve(mPreparedDocument);
    }

    @Test
    public void testOnDocumentRequestedSuccessPayload() {
        when(mEmbeddedDocumentRequest.getSource()).thenReturn(VALID_URL);
        when(mViewhost.prepare(any(PrepareDocumentRequest.class))).thenReturn(mPreparedDocument);
        doAnswer(invocation -> {
            Callback callback = invocation.getArgument(1);
            callback.success(PAYLOAD);
            return null;
        }).when(mDataRetriever).fetch(anyString(), any(Callback.class));

        mDefaultEmbeddedDocumentFactory.onDocumentRequested(mEmbeddedDocumentRequest);
        verify(mEmbeddedDocumentRequest).resolve(mPreparedDocument);
    }

    @Test
    public void testOnDocumentRequestedNonJSONPayload() {
        when(mEmbeddedDocumentRequest.getSource()).thenReturn(VALID_URL);
        when(mViewhost.prepare(any(PrepareDocumentRequest.class))).thenReturn(mPreparedDocument);
        doAnswer(invocation -> {
            Callback callback = invocation.getArgument(1);
            callback.success(NON_JSON_PAYLOAD);
            return null;
        }).when(mDataRetriever).fetch(anyString(), any(Callback.class));

        mDefaultEmbeddedDocumentFactory.onDocumentRequested(mEmbeddedDocumentRequest);
        verify(mEmbeddedDocumentRequest).fail(anyString());
    }

    @Test
    public void testOnDocumentRequestedFail() {
        when(mEmbeddedDocumentRequest.getSource()).thenReturn(VALID_URL);
        doAnswer(invocation -> {
            Callback callback = invocation.getArgument(1);
            callback.error();
            return null;
        }).when(mDataRetriever).fetch(anyString(), any(Callback.class));

        mDefaultEmbeddedDocumentFactory.onDocumentRequested(mEmbeddedDocumentRequest);
        verify(mEmbeddedDocumentRequest).fail(anyString());
    }

}
