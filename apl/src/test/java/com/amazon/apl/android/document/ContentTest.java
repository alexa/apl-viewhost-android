/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.document;

import android.graphics.Color;

import androidx.test.platform.app.InstrumentationRegistry;

import com.amazon.apl.android.APLJSONData;
import com.amazon.apl.android.APLOptions;
import com.amazon.apl.android.Content;
import com.amazon.apl.android.Content.ImportRequest;
import com.amazon.apl.android.RootConfig;
import com.amazon.apl.android.Session;
import com.amazon.apl.android.dependencies.IContentDataRetriever;
import com.amazon.apl.android.dependencies.IContentRetriever;
import com.amazon.apl.android.dependencies.IPackageLoader;
import com.amazon.apl.android.providers.ITelemetryProvider;
import com.amazon.apl.android.robolectric.ViewhostRobolectricTest;
import com.amazon.apl.android.scaling.ViewportMetrics;
import com.amazon.apl.enums.GradientType;
import com.amazon.apl.enums.ScreenShape;
import com.amazon.apl.enums.ViewportMode;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.amazon.apl.android.Content.METRIC_CONTENT_CREATE;
import static com.amazon.apl.android.Content.METRIC_CONTENT_ERROR;
import static com.amazon.apl.android.Content.METRIC_CONTENT_IMPORT_REQUESTS;
import static com.amazon.apl.android.providers.ITelemetryProvider.APL_DOMAIN;
import static com.amazon.apl.android.providers.ITelemetryProvider.Type.COUNTER;
import static com.amazon.apl.android.providers.ITelemetryProvider.Type.TIMER;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ContentTest extends ViewhostRobolectricTest {
    // Test content
    private final String mTestDoc = "{" +
            "  \"type\": \"APL\"," +
            "  \"version\": \"1.0\"," +
            "  \"import\": [" +
            "    {\n" +
            "      \"name\": \"test-package\"," +
            "      \"version\": \"1.0\"" +
            "    }" +
            "  ]," +
            "  \"mainTemplate\": {" +
            "    \"parameters\": [" +
            "      \"payload\"" +
            "    ]," +
            "    \"item\": {" +
            "      \"type\": \"Text\"" +
            "    }" +
            "  }" +
            "}";

    private final String mTestDocMultipleParameters = "{" +
            "  \"type\": \"APL\"," +
            "  \"version\": \"1.0\"," +
            "  \"mainTemplate\": {" +
            "    \"parameters\": [" +
            "      \"dataSourceA\"," +
            "      \"dataSourceB\"" +
            "    ]," +
            "    \"item\": {" +
            "      \"type\": \"Text\"" +
            "    }" +
            "  }" +
            "}";

    private final String mTestData = "{ " +
            "  \"data\": {" +
            "    \"text\": \"Hello APL World!\"" +
            "  }" +
            "}";

    private final String mTestDataB = "{ " +
            "  \"data\": {" +
            "    \"text\": \"Hello APL World!\"" +
            "  }" +
            "}";

    private final String mTestPackage = "{" +
            "  \"type\": \"APL\"," +
            "  \"version\": \"1.0\"" +
            "}";

    private final String mTestImportDoc = "{" +
            "  \"type\": \"APL\"," +
            "  \"version\": \"1.0\"," +
            "  \"import\": [" +
            "    {\n" +
            "      \"name\": \"test-package2\"," +
            "      \"version\": \"1.0\"" +
            "    }" +
            "  ]," +
            "  \"mainTemplate\": {" +
            "    \"item\": {" +
            "      \"type\": \"Text\"" +
            "    }" +
            "  }" +
            "}";

    private final String mTestPackage2 = "{" +
            "  \"type\": \"APL\"," +
            "  \"version\": \"1.0\"," +
            "  \"import\": [" +
            "    {" +
            "      \"name\": \"test-package\"," +
            "      \"version\": \"1.0\"" +
            "    }" +
            "  ]" +
            "}";
    private final String mTestBackgroundColor = "{" +
            "  \"type\": \"APL\"," +
            "  \"version\": \"1.0\"," +
            "  \"background\": \"blue\"," +
            "  \"mainTemplate\": {" +
            "    \"parameters\": [" +
            "      \"payload\"" +
            "    ]," +
            "    \"item\": {" +
            "      \"type\": \"Text\"" +
            "    }" +
            "  }" +
            "}";

    private final String mTestBackgroundGradientLinear = "{" +
            "  \"type\": \"APL\"," +
            "  \"version\": \"1.0\"," +
            "  \"background\": {" +
            "    \"type\": \"linear\"," +
            "    \"colorRange\": [ \"white\", \"blue\" ]," +
            "    \"inputRange\": [ 0, 0.25 ],\n" +
            "    \"angle\": 90\n" +
            "   }," +
            "  \"mainTemplate\": {" +
            "    \"parameters\": [" +
            "      \"payload\"" +
            "    ]," +
            "    \"item\": {" +
            "      \"type\": \"Text\"" +
            "    }" +
            "  }" +
            "}";

    private final String mTestBackgroundGradientRadial = "{" +
            "  \"type\": \"APL\"," +
            "  \"version\": \"1.0\"," +
            "  \"background\": {\n" +
            "    \"type\": \"radial\"," +
            "    \"colorRange\": [ \"yellow\", \"rgba(red,0.2)\", \"red\" ]," +
            "    \"inputRange\": [ 0.4, 0.8, 1 ]" +
            "   }," +
            "  \"mainTemplate\": {" +
            "    \"parameters\": [" +
            "      \"payload\"" +
            "    ]," +
            "    \"item\": {" +
            "      \"type\": \"Text\"" +
            "    }" +
            "  }" +
            "}";

    private static String DOC_SETTINGS = "{" +
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
            "    \"-propertyB\": 60000," +
            "    \"-propertyC\": \"abc\"," +
            "    \"subSetting\": {" +
            "      \"propertyD\": 12.34" +
            "    }" +
            "  }" +
            "}";

    private final String mDuplicateImportDoc = "{" +
            "  \"type\": \"APL\"," +
            "  \"version\": \"1.0\"," +
            "  \"import\": [" +
            "    {\n" +
            "      \"name\": \"test-package\"," +
            "      \"version\": \"1.0\"" +
            "    }," +
            "    {\n" +
            "      \"name\": \"test-package\"," +
            "      \"version\": \"1.0\"," +
            "      \"source\": \"dummy-source-1\"" +
            "    }," +
            "    {\n" +
            "      \"name\": \"test-package\"," +
            "      \"version\": \"1.0\"," +
            "      \"source\": \"dummy-source-2\"" +
            "    }" +
            "  ]," +
            "  \"mainTemplate\": {" +
            "    \"item\": {" +
            "      \"type\": \"Text\"" +
            "    }" +
            "  }" +
            "}";

    private ViewportMetrics metrics = ViewportMetrics.builder()
            .width(640)
            .height(480)
            .dpi(160)
            .shape(ScreenShape.RECTANGLE)
            .theme("black")
            .mode(ViewportMode.kViewportModeHub)
            .build();

    private final int CONTENT_CREATE_METRIC_ID = 0;
    private final int CONTENT_ERROR_METRIC_ID = 1;
    private final int CONTENT_IMPORTS_METRIC_ID = 2;
    @Mock
    private ITelemetryProvider mMockTelemetryProvider;
    private APLOptions mAplOptions;
    @Mock
    IPackageLoader mPackageLoader;
    @Mock
    IContentDataRetriever mDataRetriever;
    @Mock
    private Session mSession;


    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        resetMocks();

        mAplOptions = APLOptions.builder()
                .telemetryProvider(mMockTelemetryProvider)
                .packageLoader(mPackageLoader)
                .contentDataRetriever(mDataRetriever)
                .build();
    }

    private void resetMocks() {
        reset(mMockTelemetryProvider);

        when(mMockTelemetryProvider.createMetricId(APL_DOMAIN, METRIC_CONTENT_CREATE, TIMER))
                .thenReturn(CONTENT_CREATE_METRIC_ID);
        when(mMockTelemetryProvider.createMetricId(APL_DOMAIN, METRIC_CONTENT_ERROR, COUNTER))
                .thenReturn(CONTENT_ERROR_METRIC_ID);
        when(mMockTelemetryProvider.createMetricId(APL_DOMAIN, METRIC_CONTENT_IMPORT_REQUESTS, COUNTER))
                .thenReturn(CONTENT_IMPORTS_METRIC_ID);
    }



    /**
     * The document version.
     */
    @Test
    public void testDoc_version() {
        Content content = null;
        try {
            content = Content.create(mTestDoc, mAplOptions);
        } catch (Content.ContentException e) {
            Assert.fail(e.getMessage());
        }
        assertEquals("1.0", content.getAPLVersion());
    }

    /**
     * Test synchronous document create, package import, data set.
     */
    @Test
    public void testRequest_asSyncCallback() {
        final boolean[] doc = {false, false, false};
        try {
            Content.create(mTestDoc, mAplOptions, new Content.Callback() {
                @Override
                public void onPackageRequest(Content content, ImportRequest request) {
                    doc[0] = true;
                    try {
                        content.addPackage(request, mTestPackage);
                    } catch (Content.ContentException e) {
                        fail(e.getMessage());
                    }
                }

                @Override
                public void onDataRequest(Content content, String dataId) {
                    doc[1] = true;
                    content.addData(dataId, mTestData);
                }

                @Override
                public void onComplete(Content content) {
                    doc[2] = true;
                }

                @Override
                public void onError(Content content) {
                    fail("Unexpected error on document load");
                }
            });
        } catch (Content.ContentException e) {
            fail(e.getMessage());
        }

        assertTrue("Expected package request", doc[0]);
        assertTrue("Expected data request", doc[1]);
        assertTrue("Expected document complete", doc[2]);

        verifySuccessTelemetry();
    }


    /**
     * Test asynchronous document create, package import, data set.  In this case adding the
     * package and data is done out-of-band from the callback.
     */
    @Test
    public void testRequest_asAsyncCallback() {

        final Object[] doc = new Object[2];


        Content c = null;
        try {
            c = Content.create(mTestDoc, mAplOptions, new Content.Callback() {
                @Override
                public void onPackageRequest(Content content, ImportRequest request) {
                    doc[0] = request;
                }

                @Override
                public void onDataRequest(Content content, String dataId) {
                    doc[1] = dataId;
                }

                @Override
                public void onComplete(Content content) {
                }

                @Override
                public void onError(Content content) {
                    fail("Unexpected error on document load");
                }
            });
        } catch (Content.ContentException e) {
            fail(e.getMessage());
        }
        verifyNoTelemetry();

        try {
            c.addPackage((ImportRequest) doc[0], mTestPackage);
        } catch (Content.ContentException e) {
            fail(e.getMessage());
        }
        c.addData((String) doc[1], mTestData);
        assertTrue("Expected document ready", c.isReady());
        assertFalse("Expected document not waiting.", c.isWaiting());
        assertFalse("Expected document not error.", c.isError());

        verifySuccessTelemetry();
        verifyImportsTelemetry(1);
    }


    /**
     * Test access to Content requests through collection interface.
     */
    @Test
    public void testRequest_asCollections() {

        Content c = null;
        try {
            c = Content.create(mTestDoc, mAplOptions);
        } catch (Content.ContentException e) {
            fail(e.getMessage());
        }

        // Get collections representing the needed content
        Set<ImportRequest> importRequests = c.getRequestedPackages();

        // Test that the collections re not empty and the document is waiting.
        assertFalse("Expected non-zero package length", importRequests.isEmpty());
        assertFalse("Expected document not ready", c.isReady());
        assertTrue("Expected document  waiting.", c.isWaiting());
        assertFalse("Expected document not error.", c.isError());
        verifyNoTelemetry();

        // Respond to import requests and data parameters
        for (ImportRequest ir : importRequests) {
            try {
                c.addPackage(ir, mTestPackage);
            } catch (Content.ContentException e) {
                fail(e.getMessage());
            }
        }
        verifyNoTelemetry();

        Set<String> parameters = c.getParameters();
        assertFalse("Expected non-zero parameter length", parameters.isEmpty());
        for (String param : parameters) {
            c.addData(param, mTestDoc);
        }

        // That the document is ready.
        assertTrue("Expected document ready", c.isReady());
        assertFalse("Expected document not waiting.", c.isWaiting());
        assertFalse("Expected document not error.", c.isError());

        // Test that no package requests are outstanding
        importRequests = c.getRequestedPackages();
        assertTrue("Expected zero length requests", importRequests.isEmpty());

        verifySuccessTelemetry();
        verifyImportsTelemetry(1);
    }

    /**
     * Test the Content status methods for waiting, ready, and error.
     * NOTE: this does not test the logic of the status, only that the status is correctly retrieved
     * via JNI. Business logic is tested in core layer.
     */
    @Test
    public void testDocument_Status() {

        try {
            Content.create(mTestDoc, mAplOptions, new Content.Callback() {
                @Override
                public void onPackageRequest(Content content, ImportRequest request) {
                    assertFalse("Expected document not ready.", content.isReady());
                    assertTrue("Expected document waiting.", content.isWaiting());
                    assertFalse("Expected document not in error.", content.isError());
                    try {
                        content.addPackage(request, mTestPackage);
                    } catch (Content.ContentException e) {
                        fail(e.getMessage());
                    }
                }

                @Override
                public void onDataRequest(Content content, String dataId) {
                    assertFalse("Expected document not ready.", content.isReady());
                    assertFalse("Expected document not waiting.", content.isWaiting());
                    assertFalse("Expected document not in error.", content.isError());
                    content.addData("payload", mTestData);
                }

                @Override
                public void onComplete(Content content) {
                    assertTrue("Expected document ready", content.isReady());
                    assertFalse("Expected document not waiting.", content.isWaiting());
                    assertFalse("Expected document not error.", content.isError());
                }

                @Override
                public void onError(Content content) {
                    fail("Unexpected error on document load");
                }
            });
        } catch (Content.ContentException e) {
            fail(e.getMessage());
        }

        verifySuccessTelemetry();
        verifyImportsTelemetry(1);
    }


    /**
     * Test ImportRequests that result in follow-on ImportRequests via Callback interface.
     */
    @Test
    public void testDocument_multiImportAsCallback() {

        Content content = null;
        try {
            content = Content.create(mTestImportDoc, mAplOptions, new Content.Callback() {

                @Override
                public void onPackageRequest(Content content, ImportRequest request) {

                    if ("test-package".equals(request.getPackageName())) {
                        try {
                            content.addPackage(request, mTestPackage);
                        } catch (Content.ContentException e) {
                            fail(e.getMessage());
                        }
                    } else if ("test-package2".equals(request.getPackageName())) {
                        try {
                            content.addPackage(request, mTestPackage2);
                        } catch (Content.ContentException e) {
                            fail(e.getMessage());
                        }
                    }
                }

                @Override
                public void onError(Content content) {
                    fail("Unexpected error on document load");
                }
            });
        } catch (Content.ContentException e) {
            fail(e.getMessage());
        }

        assertTrue("Expected document ready", content.isReady());
        assertFalse("Expected document not waiting.", content.isWaiting());
        assertFalse("Expected document not error.", content.isError());

        verifySuccessTelemetry();
        verifyImportsTelemetry(2);
    }

    /**
     * Test ImportRequests that result in follow-on ImportRequests via Callback interface.
     */
    @Test
    public void testDocument_multiImportCallback_callbackV2() {
        doAnswer(invocation -> {
            ImportRequest request = invocation.getArgument(0);
            IContentRetriever.SuccessCallback<ImportRequest, APLJSONData> successCallback = invocation.getArgument(1);
            if ("test-package".equals(request.getPackageName())) {
                successCallback.onSuccess(request, APLJSONData.create(mTestPackage));
            } else if ("test-package2".equals(request.getPackageName())) {
                successCallback.onSuccess(request, APLJSONData.create(mTestPackage2));
            }
            return null;
        }).when(mPackageLoader).fetch(any(), any(), any());

        CountDownLatch latch = new CountDownLatch(1);

        Content content = Content.create(mTestImportDoc, mAplOptions, new Content.CallbackV2() {
            @Override
            public void onComplete(Content content) {
                latch.countDown();
                super.onComplete(content);
            }

            @Override
            public void onError(Exception e) {
                Assert.fail(e.getCause().getMessage());
            }
        }, mSession);
        assertNotNull("Content should not be null.", content);

        try {
            // Give Content a second to complete.
            latch.await(1, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Assert.fail();
        }

        assertTrue("Expected document ready", content.isReady());
        assertFalse("Expected document not waiting.", content.isWaiting());
        assertFalse("Expected document not error.", content.isError());

        verifySuccessTelemetry();
        verifyImportsTelemetry(2);
        verify(mPackageLoader, times(2)).fetch(any(), any(), any());
    }

    @Test
    public void testDocument_dataSources_callbackV2() {
        doAnswer(invocation -> {
            String param = invocation.getArgument(0);
            IContentRetriever.SuccessCallback<String, String> successCallback = invocation.getArgument(1);
            if ("dataSourceA".equals(param)) {
                successCallback.onSuccess(param, mTestData);
            } else if ("dataSourceB".equals(param)) {
                successCallback.onSuccess(param, mTestDataB);
            }
            return null;
        }).when(mDataRetriever).fetch(any(), any(), any());

        AtomicBoolean completeCalled = new AtomicBoolean();
        Content content = Content.create(mTestDocMultipleParameters, mAplOptions, new Content.CallbackV2() {
            @Override
            public void onComplete(Content content) {
                completeCalled.set(true);
                super.onComplete(content);
            }
        }, mSession);
        assertNotNull("Content should not be null.", content);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        assertTrue("Expected complete called", completeCalled.get());
        assertTrue("Expected document ready", content.isReady());
        assertFalse("Expected document not waiting.", content.isWaiting());
        assertFalse("Expected document not error.", content.isError());
        verifySuccessTelemetry();
        verify(mDataRetriever, times(2)).fetch(any(), any(), any());
    }


    /**
     * Test ImportRequests that result in follow-on ImportRequests via collection interface.
     */
    @Test
    public void testDocument_multiImportAsCollection() {

        Content content = null;
        try {
            content = Content.create(mTestImportDoc, mAplOptions);
        } catch (Content.ContentException e) {
            fail(e.getMessage());
        }

        // Test that the doc is waiting for one import
        Set<ImportRequest> importRequests = content.getRequestedPackages();
        assertEquals("Expected 1 import request", 1, importRequests.size());
        ImportRequest importRequest = importRequests.iterator().next();
        assertEquals("Expected a different import request", importRequest.getPackageName(), "test-package2");
        assertTrue("Expected document waiting.", content.isWaiting());

        // Satisfy the request and verify the next import is requested
        try {
            content.addPackage(importRequest, mTestPackage2);
        } catch (Content.ContentException e) {
            fail(e.getMessage());
        }
        importRequests = content.getRequestedPackages();
        assertEquals("Expected 1 import request", 1, importRequests.size());
        importRequest = importRequests.iterator().next();
        assertEquals("Expected a different import request", importRequest.getPackageName(), "test-package");

        // add the follow on import and verify ready
        try {
            content.addPackage(importRequest, mTestPackage);
        } catch (Content.ContentException e) {
            fail(e.getMessage());
        }
        importRequests = content.getRequestedPackages();
        assertEquals("Expected zero package requests", 0, importRequests.size());
        assertTrue("Expected content ready.", content.isReady());

        verifySuccessTelemetry();
        verifyImportsTelemetry(2);
    }


    /**
     * Test Create attempt with an invalid mainTemplate document.
     */
    @Test
    public void testDocument_invalidCreate() {

        // Test creating an invalid Content object from a malformed document
        Content content = null;
        try {
            content = Content.create("malformed doc", mAplOptions);
            fail("Expected exception to be thrown");
        } catch (Exception e) {
            // do nothing
        }
        assertNull("Content create should not return an invalid Content object.", content);

        verifyErrorTelemetry();
    }


    /**
     * Test invalid documents for ImportRequests and data parameters via the Callback api.
     */
    @Test
    public void testDocument_invalidRequestsAsCallback() {

        final boolean[] error = {false};
        // Test invalid package
        try {
            Content.create(mTestDoc, mAplOptions, new Content.Callback() {
                @Override
                public void onPackageRequest(Content content, ImportRequest request) {
                    try {
                        content.addPackage(request, "malformed package");
                    } catch (Content.ContentException e) {
                        fail(e.getMessage());
                    }
                }

                @Override
                public void onComplete(Content content) {
                    fail("Expected error rather than complete");
                }

                @Override
                public void onError(Content content) {
                    error[0] = true;
                }
            });
        } catch (Content.ContentException e) {
            fail(e.getMessage());
        }
        assertTrue("Expected error callback but didn't receive it.", error[0]);
        verifyErrorTelemetry();
        resetMocks();

        // Test invalid data
        error[0] = false;
        // Test invalid package
        try {
            Content.create(mTestDoc, mAplOptions, new Content.Callback() {
                @Override
                public void onPackageRequest(Content content, ImportRequest request) {
                    try {
                        content.addPackage(request, mTestPackage);
                    } catch (Content.ContentException e) {
                        fail(e.getMessage());
                    }
                }

                @Override
                public void onDataRequest(Content content, String dataId) {
                    content.addData(dataId, "malformed data");
                }

                @Override
                public void onComplete(Content content) {
                    fail("Expected error rather than complete");
                }

                @Override
                public void onError(Content content) {
                    error[0] = true;
                }
            });
        } catch (Content.ContentException e) {
            fail(e.getMessage());
        }
        assertTrue("Expected error callback but didn't receive it.", error[0]);
        verifyErrorTelemetry();
    }

    /**
     * Test invalid documents for ImportRequests and data parameters via the collection api.
     */
    @Test
    public void testDocument_invalidRequestsAsCollection() {

        // Test an invalid package
        Content content = null;
        try {
            content = Content.create(mTestDoc, mAplOptions);
        } catch (Content.ContentException e) {
            fail(e.getMessage());
        }
        ImportRequest ir = content.getRequestedPackages().iterator().next();
        try {
            content.addPackage(ir, "malformed package");
        } catch (Content.ContentException e) {
            fail(e.getMessage());
        }
        assertTrue("Expected content error.", content.isError());
        verifyErrorTelemetry();
        resetMocks();

        // Test malformed data
        try {
            content = Content.create(mTestDoc, mAplOptions);
        } catch (Content.ContentException e) {
            fail(e.getMessage());
        }
        ir = content.getRequestedPackages().iterator().next();
        try {
            content.addPackage(ir, mTestPackage);
        } catch (Content.ContentException e) {
            fail(e.getMessage());
        }
        String param = content.getParameters().iterator().next();
        content.addData(param, "malformed data");
        assertTrue("Expected content error.", content.isError());
        verifyErrorTelemetry();
    }


    @Test
    public void test_invalidContent() {
        boolean error = false;
        try {
            Content.create("{}", mAplOptions);
        } catch (Content.ContentException e) {
            error = true;
        }
        assertTrue("Expected document import error.", error);
        verifyErrorTelemetry();
    }

    /**
     * Test empty document for ImportRequests and data parameters via the Callback api.
     */
    @Test
    public void testDocument_invalidPackageContent() {
        boolean error = false;
        Content c = null;
        try {
            c = Content.create(mTestDoc, mAplOptions);
        } catch (Content.ContentException e) {
            fail(e.getMessage());
        }

        // Get collections representing the needed content
        Set<ImportRequest> importRequests = c.getRequestedPackages();
        ImportRequest importRequest = importRequests.iterator().next();

        try {
            c.addPackage(importRequest, "");
        } catch (Content.ContentException e) {
            error = true;
        }

        assertTrue("Expected package import error.", error);
        verifyErrorTelemetry();
    }


    private final String myDocument = "{" +
            "  \"type\": \"APL\"," +
            "  \"version\": \"1.3\"," +
            "  \"import\": [" +
            "    {\n" +
            "      \"name\": \"test-package\"," +
            "      \"version\": \"1.0\"" +
            "    }" +
            "  ]," +
            "  \"mainTemplate\": {" +
            "    \"parameters\": [" +
            "      \"payload\"" +
            "    ]," +
            "    \"item\": {" +
            "      \"type\": \"Text\"" +
            "    }" +
            "  }" +
            "}";

    @Test
    public void test_userDocCollectionSample() {
        // this example is used in the User Guide. Should it fail, the User Guide must be updated
        // with any code changes
        try {

            // start User Guide example

            Content content = Content.create(myDocument, mAplOptions);

            while (content.isWaiting()) {

                for (ImportRequest request : content.getRequestedPackages()) {
                    String myImportDoc = fetchMyImportDocument(request);
                    content.addPackage(request, myImportDoc);
                }

                if (content.isReady()) {
                    break;
                }

                for (String dataId : content.getParameters()) {
                    String myData = fetchMyData(dataId);
                    content.addData(dataId, myData);
                }

            }

            if (content.isError()) {
                fail("Content import failed.");
            } else {
                // ready for display
            }

            // end user guide example

        } catch (Exception e) {
            fail("User Guide must be updated if failure fix requires code change.");
        }
    }


    @Test
    public void testRequest_userDocCallbackExample() {
        // this example is used in the User Guide. Should it fail, the User Guide must be updated
        // with any code changes

        try {

            // start User Guide example

            Content.create(mTestDoc, mAplOptions, new Content.Callback() {

                @Override
                public void onPackageRequest(Content content, ImportRequest request) {
                    String myImportDoc = fetchMyImportDocument(request);
                    try {
                        content.addPackage(request, myImportDoc);
                    } catch (Content.ContentException e) {
                        // throw new Exception("Content import failed.");
                    }
                }

                @Override
                public void onDataRequest(Content content, String dataId) {
                    String myData = fetchMyData(dataId);
                    content.addData(dataId, myData);
                }

                @Override
                public void onComplete(Content content) {
                    // ready for display
                }

                @Override
                public void onError(Content content) {
                    //throw new Exception("Content import failed.");
                }
            });

            // end user guide example

        } catch (Content.ContentException e) {
            fail("User Guide must be updated if failure fix requires code change.");
        }

    }


    @Test
    public void test_settings() {

        Content c = null;
        try {
            c = Content.create(DOC_SETTINGS);
        } catch (Content.ContentException e) {
            fail(e.getMessage());
        }


        boolean a = c.optSetting("propertyA", false);
        int b = c.optSetting("-propertyB", 30000);
        String s = c.optSetting("-propertyC", "foo");
        Map<String, Double> subSetting = c.optSetting("subSetting", new HashMap<>());
        double d = subSetting.get("propertyD");
        Object e = c.optSetting("-notExistingProperty", null);

        Assert.assertTrue(c.hasSetting("propertyA"));
        Assert.assertTrue(c.hasSetting("-propertyB"));
        Assert.assertTrue(c.hasSetting("-propertyC"));
        Assert.assertTrue(c.hasSetting("subSetting"));
        assertFalse(c.hasSetting("-notExistingProperty"));

        Assert.assertTrue(a);
        assertEquals(60000, b);
        assertEquals("abc", s);
        assertEquals(12.34, d, 0.001);
        assertNull(e);
    }


    @Test
    public void testDocumentBackground_noBackground() {
        Content c = null;
        try {
            c = Content.create(mTestDoc, mAplOptions);
        } catch (Content.ContentException e) {
            fail(e.getMessage());
        }

        RootConfig rootConfig = RootConfig.create();
        c.createDocumentBackground(metrics, rootConfig);
        Content.DocumentBackground bg = c.getDocumentBackground();
        assertNotNull(bg);
        assertEquals(Color.TRANSPARENT, bg.getColor());
        assertEquals(0.0f, bg.getAngle());
        assertEquals(GradientType.LINEAR, bg.getType());
        assertNull(bg.getInputRange());
        assertNull(bg.getColorRange());
    }

    @Test
    public void testDocumentBackground_color() {
        Content c = null;
        try {
            c = Content.create(mTestBackgroundColor, mAplOptions);
        } catch (Content.ContentException e) {
            fail(e.getMessage());
        }

        RootConfig rootConfig = RootConfig.create();
        c.createDocumentBackground(metrics, rootConfig);
        Content.DocumentBackground bg = c.getDocumentBackground();
        assertNotNull(bg);
        assertEquals(Color.BLUE, bg.getColor());
    }

    @Test
    public void testDocumentBackground_gradientLinear() {
        Content c = null;
        try {
            c = Content.create(mTestBackgroundGradientLinear, mAplOptions);
        } catch (Content.ContentException e) {
            fail(e.getMessage());
        }

        RootConfig rootConfig = RootConfig.create();
        c.createDocumentBackground(metrics, rootConfig);
        Content.DocumentBackground bg = c.getDocumentBackground();
        assertNotNull(bg);
        assertEquals(GradientType.LINEAR, bg.getType());
        assertEquals(90.0f, bg.getAngle());

        final int[] colorRange = bg.getColorRange();
        assertEquals(2, colorRange.length);
        assertEquals(Color.WHITE, colorRange[0]); // TODO green doesn't work here
        assertEquals(Color.BLUE, colorRange[1]);

        final float[] inputRange = bg.getInputRange();
        assertEquals(2, inputRange.length);
        assertEquals(0.0f, inputRange[0]);
        assertEquals(0.25f, inputRange[1]);
    }

    @Test
    public void testDocumentBackground_gradientRadial() {
        Content c = null;
        try {
            c = Content.create(mTestBackgroundGradientRadial, mAplOptions);
        } catch (Content.ContentException e) {
            fail(e.getMessage());
        }

        RootConfig rootConfig = RootConfig.create();
        c.createDocumentBackground(metrics, rootConfig);
        Content.DocumentBackground bg = c.getDocumentBackground();
        assertNotNull(bg);
        assertEquals(GradientType.RADIAL, bg.getType());

        final int[] colorRange = bg.getColorRange();
        assertEquals(3, colorRange.length);
        assertEquals(Color.YELLOW, colorRange[0]);
        assertEquals(Color.argb(51, 255,0,0), colorRange[1]);
        assertEquals(Color.RED, colorRange[2]);

        final float[] inputRange = bg.getInputRange();
        assertEquals(3, inputRange.length);
        assertEquals(0.4f, inputRange[0]);
        assertEquals(0.8f, inputRange[1]);
        assertEquals(1.0f, inputRange[2]);

    }

    /**
     * Test that the deprecated {@link Content#create(String, Content.Callback)} calls
     * are not broken.
     */
    @Test
    public void testDeprecatedContentCreate_asCallback() {
        final boolean[] doc = {false, false, false};
        try {
            Content.create(mTestDoc, new Content.Callback() { // deprecated factory
                @Override
                public void onPackageRequest(Content content, ImportRequest request) {
                    doc[0] = true;
                    try {
                        content.addPackage(request, mTestPackage);
                    } catch (Content.ContentException e) {
                        fail(e.getMessage());
                    }
                }

                @Override
                public void onDataRequest(Content content, String dataId) {
                    doc[1] = true;
                    content.addData(dataId, mTestData);
                }

                @Override
                public void onComplete(Content content) {
                    doc[2] = true;
                }

                @Override
                public void onError(Content content) {
                    fail("Unexpected error on document load");
                }
            });
        } catch (Content.ContentException e) {
            fail(e.getMessage());
        }

        assertTrue("Expected package request", doc[0]);
        assertTrue("Expected data request", doc[1]);
        assertTrue("Expected document complete", doc[2]);
    }

    /**
     * Test that the deprecated {@link Content#create(String)} calls
     * are not broken.
     */
    @Test
    public void testDeprecatedContentCreate_asCollections() {
        Content c = null;
        try {
            c = Content.create(mTestDoc); // deprecated factory
        } catch (Content.ContentException e) {
            fail(e.getMessage());
        }

        // Get collections representing the needed content
        Set<ImportRequest> importRequests = c.getRequestedPackages();

        // Test that the collections re not empty and the document is waiting.
        assertFalse("Expected non-zero package length", importRequests.isEmpty());
        assertFalse("Expected document not ready", c.isReady());
        assertTrue("Expected document  waiting.", c.isWaiting());
        assertFalse("Expected document not error.", c.isError());

        // Respond to import requests and data parameters
        for (ImportRequest ir : importRequests) {
            try {
                c.addPackage(ir, mTestPackage);
            } catch (Content.ContentException e) {
                fail(e.getMessage());
            }
        }

        Set<String> parameters = c.getParameters();
        assertFalse("Expected non-zero parameter length", parameters.isEmpty());
        for (String param : parameters) {
            c.addData(param, mTestDoc);
        }

        // That the document is ready.
        assertTrue("Expected document ready", c.isReady());
        assertFalse("Expected document not waiting.", c.isWaiting());
        assertFalse("Expected document not error.", c.isError());

        // Test that no package requests are outstanding
        importRequests = c.getRequestedPackages();
        assertTrue("Expected zero length requests", importRequests.isEmpty());
    }

    /**
     * Test multiple ImportRequest for same package name and version but different source,
     * for such duplicate requests package should be added only once to the Content.
     */
    @Test
    public void testDocument_duplicateImports() {

        CountDownLatch packageRequestedCountdown = new CountDownLatch(3);
        CountDownLatch packageLoadedCallbackCountdown = new CountDownLatch(1);

        doAnswer(invocation -> {
            ImportRequest request = invocation.getArgument(0);
            IContentRetriever.SuccessCallback<ImportRequest, APLJSONData> successCallback = invocation.getArgument(1);
            if ("test-package".equals(request.getPackageName())) {
                packageRequestedCountdown.countDown();
                successCallback.onSuccess(request, APLJSONData.create(mTestPackage));
            }
            return null;
        }).when(mPackageLoader).fetch(any(), any(), any());

        Content content = Content.create(mDuplicateImportDoc, mAplOptions, new Content.CallbackV2() {
            @Override
            public void onPackageLoaded(Content content) {
                packageLoadedCallbackCountdown.countDown();
                super.onComplete(content);
            }

            @Override
            public void onError(Exception e) {
                Assert.fail(e.getCause().getMessage());
            }
        }, mSession);
        assertNotNull("Content should not be null.", content);

        try {
            packageRequestedCountdown.await(1, TimeUnit.SECONDS);
            packageLoadedCallbackCountdown.await(1, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Assert.fail();
        }

        assertTrue("Expected document ready", content.isReady());
        assertFalse("Expected document not waiting.", content.isWaiting());
        assertFalse("Expected document not error.", content.isError());

        verifySuccessTelemetry();
        verifyImportsTelemetry(1);
    }

    // used as abstraction for user doc sample
    public String fetchMyImportDocument(ImportRequest request) {
        return mTestPackage;
    }

    // used as abstraction for user doc sample
    public String fetchMyData(String dataId) {
        return mTestData;
    }

    private void verifySuccessTelemetry() {
        InOrder ordered = Mockito.inOrder(mMockTelemetryProvider);
        ordered.verify(mMockTelemetryProvider).startTimer(eq(CONTENT_CREATE_METRIC_ID), eq(TimeUnit.NANOSECONDS), anyLong());
        ordered.verify(mMockTelemetryProvider).stopTimer(eq(CONTENT_CREATE_METRIC_ID));
    }

    private void verifyErrorTelemetry() {
        InOrder ordered = Mockito.inOrder(mMockTelemetryProvider);
        ordered.verify(mMockTelemetryProvider).startTimer(eq(CONTENT_CREATE_METRIC_ID), eq(TimeUnit.NANOSECONDS), anyLong());
        ordered.verify(mMockTelemetryProvider).fail(eq(CONTENT_CREATE_METRIC_ID));

        verify(mMockTelemetryProvider).incrementCount(eq(CONTENT_ERROR_METRIC_ID));
    }

    private void verifyNoTelemetry() {
        verify(mMockTelemetryProvider, never()).stopTimer(eq(CONTENT_CREATE_METRIC_ID));
        verify(mMockTelemetryProvider, never()).fail(eq(CONTENT_CREATE_METRIC_ID));
        verify(mMockTelemetryProvider, never()).incrementCount(eq(CONTENT_ERROR_METRIC_ID));
    }

    private void verifyImportsTelemetry(int numberOfImports) {
        verify(mMockTelemetryProvider, times(numberOfImports)).incrementCount(eq(CONTENT_IMPORTS_METRIC_ID));
    }
}
