/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.viewhost.legacy;

import android.os.Handler;
import android.os.HandlerThread;

import com.amazon.alexaext.ActivityDescriptor;
import com.amazon.alexaext.ExtensionRegistrar;
import com.amazon.alexaext.LocalExtensionProxy;
import com.amazon.alexaext.LocalExtensionV2;
import com.amazon.alexaext.ExtensionMessage;

import com.amazon.apl.android.APLController;
import com.amazon.apl.viewhost.AbstractLegacyViewhostTest;

import org.junit.Test;

public class LegacyRenderWithExtensionTest extends AbstractLegacyViewhostTest {
    private static final String EXTENSION_DOCUMENT = "{" +
            "  \"type\": \"APL\"," +
            "  \"version\": \"2024.2\"," +
            "  \"extensions\": [" +
            "    {" +
            "      \"uri\": \"example:foo:10\"," +
            "      \"name\": \"Foo\"" +
            "    }" +
            "  ]," +
            "  \"onMount\": [" +
            "    {" +
            "      \"type\": \"SetValue\"," +
            "      \"componentId\": \"MyFrame\"," +
            "      \"property\": \"background\"," +
            "      \"value\": \"red\"" +
            "    }" +
            "  ]," +
            "  \"Foo:onUpdate\": [" +
            "    {" +
            "      \"type\": \"SendEvent\"," +
            "      \"sequencer\": \"SEND_EVENT_SEQUENCER\"," +
            "      \"arguments\": [" +
            "        \"${event.count}\"" +
            "      ]" +
            "    }," +
            "    {" +
            "      \"type\": \"AnimateItem\"," +
            "      \"componentId\": \"MyFrame\"," +
            "      \"sequencer\": \"ANIMATE_SEQUENCER\"," +
            "      \"duration\": 500," +
            "      \"value\": {" +
            "        \"property\": \"transform\"," +
            "        \"from\": [" +
            "          {" +
            "            \"translateX\": 200" +
            "          }," +
            "          {" +
            "            \"rotate\": 90" +
            "          }" +
            "        ]," +
            "        \"to\": [" +
            "          {" +
            "            \"translateX\": 0" +
            "          }," +
            "          {" +
            "            \"rotate\": 0" +
            "          }" +
            "        ]" +
            "      }" +
            "    }" +
            "  ]," +
            "  \"mainTemplate\": {" +
            "    \"items\": {" +
            "      \"type\": \"Frame\"," +
            "      \"id\": \"MyFrame\"," +
            "      \"width\": 300," +
            "      \"height\": 300," +
            "      \"background\": \"white\"" +
            "    }" +
            "  }" +
            "}";

    @Test
    public void testExtensionEventHandling() {
        ExtensionRegistrar registrar = new ExtensionRegistrar();
        registrar.registerExtension(new LocalExtensionProxy(new NoisyExtension()));
        mRootConfig.extensionProvider(registrar);

        APLController.builder()
                .aplDocument(EXTENSION_DOCUMENT)
                .rootConfig(mRootConfig)
                .aplOptions(mAplOptionsBuilder.build())
                .aplLayout(mAplLayout)
                .disableAsyncInflate(true)
                .render();

        runUntil(() -> mUserEvents.size() == 50);
    }

    @Test
    public void testExtensionEventHandlingWithAsyncInflation() {
        ExtensionRegistrar registrar = new ExtensionRegistrar();
        registrar.registerExtension(new LocalExtensionProxy(new NoisyExtension()));
        mRootConfig.extensionProvider(registrar);

        APLController.builder()
                .aplDocument(EXTENSION_DOCUMENT)
                .rootConfig(mRootConfig)
                .aplOptions(mAplOptionsBuilder.build())
                .aplLayout(mAplLayout)
                .disableAsyncInflate(false)
                .render();

        runUntil(() -> mUserEvents.size() == 50);
    }

    /**
     * This extension's job is to send lots of extension events.
     */
    public static class NoisyExtension extends LocalExtensionV2 {
        private static final String URI = "example:foo:10";

        private final Handler mHandler;

        public NoisyExtension() {
            super(URI);
            HandlerThread handlerThread = new HandlerThread("ExtensionThread");
            handlerThread.start();
            mHandler = new Handler(handlerThread.getLooper());
        }
        @Override
        public String createRegistration(ActivityDescriptor activity, String registrationRequest) {
            ExtensionMessage.RegistrationSuccess registrationSuccess = new ExtensionMessage
                    .RegistrationSuccess()
                    .token("some-token")
                    .schema(schema -> schema.uri(URI).event("onUpdate"));

            return registrationSuccess.getMessage().toString();
        }

        @Override
        public String createRegistration(String uri, String registrationRequest) {
            return createRegistration((ActivityDescriptor) null, registrationRequest);
        }

        @Override
        public void onRegistered(ActivityDescriptor activity) {
            ExtensionMessage.Event event = new ExtensionMessage.Event(activity.getURI())
                    .name("onUpdate")
                    .property("count", 0);
            // Send one message on the calling thread
            invokeExtensionEventHandler(activity, event.getMessage().toString());

            // Send a bunch more from our own thread
            for (int i = 1; i < 50; i++) {
                event.property("count", i);
                String message = event.getMessage().toString();
                // Post with a little delay
                mHandler.postDelayed(() -> invokeExtensionEventHandler(activity, message), i);
            }
        }
    }
}
