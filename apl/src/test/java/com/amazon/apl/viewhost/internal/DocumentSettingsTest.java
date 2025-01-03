package com.amazon.apl.viewhost.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import android.os.Handler;
import android.os.Looper;

import com.amazon.apl.android.Content;
import com.amazon.apl.android.DocumentSession;
import com.amazon.apl.android.metrics.MetricsOptions;
import com.amazon.apl.viewhost.AbstractUnifiedViewhostTest;
import com.amazon.apl.viewhost.DocumentHandle;
import com.amazon.apl.viewhost.Viewhost;
import com.amazon.apl.viewhost.primitives.Decodable;
import com.amazon.apl.viewhost.primitives.JsonDecodable;
import com.amazon.apl.viewhost.primitives.JsonStringDecodable;
import com.amazon.apl.viewhost.primitives.JsonTranscoder;
import com.amazon.apl.viewhost.request.FinishDocumentRequest;
import com.amazon.apl.viewhost.request.RenderDocumentRequest;
import com.amazon.apl.viewhost.utils.ManualExecutor;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class DocumentSettingsTest extends AbstractUnifiedViewhostTest  {

    @Mock
    private Handler mCoreWorker;
    @Mock
    private MetricsOptions mMetricsOptions;
    private ManualExecutor mRuntimeInteractionWorker;
    private Viewhost mViewhost;
    private static String DOC_WITH_SETTINGS = "{" +
            "  \"type\": \"APL\"," +
            "  \"version\": \"1.0\"," +
            "  \"mainTemplate\": {" +
            "    \"item\": {" +
            "      \"type\": \"Frame\"," +
            "      \"backgroundColor\": \"orange\"" +
            "    }" +
            "  }," +
            "  \"settings\": {" +
            "    \"propertyA\": true," +
            "    \"propertyB\": 60000," +
            "    \"propertyC\": \"abc\"" +
            "  }" +
            "}";
    private static final String DOC_WITHOUT_SETTINGS = "{" +
            "  \"type\": \"APL\"," +
            "  \"version\": \"2023.3\"," +
            "  \"mainTemplate\": {" +
            "    \"item\":" +
            "    {" +
            "      \"type\": \"Frame\"" +
            "    }" +
            "  }" +
            "}";
    @Before
    public void setup() {
        when(mCoreWorker.getLooper()).thenReturn(Looper.getMainLooper());
        when(mCoreWorker.post(any(Runnable.class))).thenAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            task.run();
            return null;
        });
        mRuntimeInteractionWorker = new ManualExecutor();
        mViewhost = new ViewhostImpl(mViewhostConfigBuilder.build(), mRuntimeInteractionWorker, mCoreWorker);
    }

    @Test
    public void testRequestContentSetting_DocumentPrepared_content_returnsExpectedValue() throws InterruptedException {
        RenderDocumentRequest renderDocumentRequest = RenderDocumentRequest.builder()
                .document(new JsonStringDecodable(DOC_WITH_SETTINGS))
                .documentSession(DocumentSession.create())
                .build();

        DocumentHandle handle = mViewhost.render(renderDocumentRequest);

        CountDownLatch latch = new CountDownLatch(1);
        assertDocumentSettings(handle, latch);
        assertTrue(mRuntimeInteractionWorker.size() > 0);
        mRuntimeInteractionWorker.flush();
        assertTrue(latch.await(100, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testRequestContentSettings_DocumentPrepapred_NullSettings_returnsCoreDefaults() throws InterruptedException {
        RenderDocumentRequest renderDocumentRequest = RenderDocumentRequest.builder()
                .document(new JsonStringDecodable(DOC_WITHOUT_SETTINGS))
                .documentSession(DocumentSession.create())
                .build();

        DocumentHandle handle = mViewhost.render(renderDocumentRequest);

        CountDownLatch latch = new CountDownLatch(1);
        boolean handleRequest = handle.requestDocumentSettings(new DocumentHandle.DocumentSettingsCallback() {
            @Override
            public void onSuccess(Decodable settings) {
                latch.countDown();
                assertTrue(settings instanceof JsonDecodable);
                JsonTranscoder transcoder = new JsonTranscoder();
                assertTrue(settings.transcode(transcoder));
                JSONObject jsonObject = transcoder.getJsonObject();
                assertNotNull(jsonObject);
                try {
                    //default defined in APL document
                    assertEquals(false, jsonObject.get("supportsResizing"));
                    //default value defined in APLCoreEngine
                    assertEquals(30000, jsonObject.get("idleTimeout"));
                } catch (JSONException e) {
                    fail(e.getMessage());
                }
            }

            @Override
            public void onFailure(String reason) {
            }
        });
        assertTrue(handleRequest);
        mRuntimeInteractionWorker.flush();
        assertTrue(latch.await(100, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testRequestDocumentSettings_invalidDocument_returnsFailureCallback() throws InterruptedException {
        RenderDocumentRequest renderDocumentRequest = RenderDocumentRequest.builder()
                .document(new JsonStringDecodable(DOC_WITH_SETTINGS))
                .documentSession(DocumentSession.create())
                .build();

        DocumentHandle handle = mViewhost.render(renderDocumentRequest);
        handle.finish(FinishDocumentRequest.builder().build());

        assertEquals(DocumentState.FINISHED, ((DocumentHandleImpl)handle).getDocumentState());

        CountDownLatch latch = new CountDownLatch(1);
        boolean handleRequest = handle.requestDocumentSettings(new DocumentHandle.DocumentSettingsCallback() {
            @Override
            public void onSuccess(Decodable settings) {
            }
            @Override
            public void onFailure(String reason) {
                latch.countDown();
            }
        });
        assertFalse(handleRequest);
        mRuntimeInteractionWorker.flush();
        assertTrue(latch.await(100, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testRequestContentSetting_DocumentPendingState_DifferentRuntimeThreads_returnsExpectedValue() throws InterruptedException {
        DocumentHandleImpl handle = new DocumentHandleImpl((ViewhostImpl) mViewhost, mCoreWorker, mMetricsOptions);
        try {
            final Content content = Content.create(DOC_WITH_SETTINGS);
            List<Thread> threadList = new ArrayList<>();
            CountDownLatch latch = new CountDownLatch(4);
            //Add 4 threads to the list
            for (int i=0; i<4; i++) {
                threadList.add(new Thread(() -> {
                    try {
                        assertDocumentSettings(handle, latch);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }));
            }

            ExecutorService service = Executors.newFixedThreadPool(5);
            threadList.forEach(service::submit);
            service.awaitTermination(100, TimeUnit.MILLISECONDS);

            service.submit(() -> {
                handle.setContent(content);
                assertTrue(mRuntimeInteractionWorker.size() > 0);
                mRuntimeInteractionWorker.flush();
                try {
                    assertTrue(latch.await(100, TimeUnit.MILLISECONDS));
                } catch (InterruptedException e) {
                    fail(e.getMessage());
                }
            });

        } catch (Content.ContentException e) {
            fail(e.getMessage());
        }
    }

    private void assertDocumentSettings(DocumentHandle handle, CountDownLatch latch) throws InterruptedException {
        boolean handleRequest = handle.requestDocumentSettings(new DocumentHandle.DocumentSettingsCallback() {
            @Override
            public void onSuccess(Decodable settings) {
                latch.countDown();
                assertTrue(settings instanceof JsonDecodable);
                JsonTranscoder transcoder = new JsonTranscoder();
                assertTrue(settings.transcode(transcoder));
                JSONObject jsonObject = transcoder.getJsonObject();
                assertNotNull(jsonObject);
                try {
                    // Properties existing in Content return expected values
                    assertEquals(true, jsonObject.get("propertyA"));
                    assertEquals(60000, jsonObject.get("propertyB"));
                    assertEquals("abc", jsonObject.get("propertyC"));
                    //default defined in APL document
                    assertEquals(false, jsonObject.get("supportsResizing"));
                    //default value defined in APLCoreEngine
                    assertEquals(30000, jsonObject.get("idleTimeout"));
                } catch (JSONException e) {
                    e.printStackTrace();
                    fail();
                }
            }
            @Override
            public void onFailure(String reason) {
            }
        });
        assertTrue(handleRequest);
    }
}
