/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.component;

import android.view.KeyEvent;

import androidx.test.espresso.IdlingRegistry;

import com.amazon.apl.android.APLOptions;
import com.amazon.apl.android.Component;
import com.amazon.apl.android.dependencies.ISendEventCallbackV2;
import com.amazon.apl.android.document.AbstractDocViewTest;
import com.amazon.apl.android.espresso.APLViewActions;
import com.amazon.apl.android.espresso.APLViewIdlingResource;

import org.junit.Test;
import org.mockito.Mock;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.swipeLeft;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static com.amazon.apl.android.espresso.APLViewActions.waitFor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * This class tests the interaction between nested pagers and touch wrappers.
 *
 * The test layout is:
 *
 *  TouchWrapper
 *      Pager
 *          TouchWrapper
 *              Text
 *          Text
 *
 * Two interactions are currently being tested:
 *
 * 1) If the inner TouchWrapper is being displayed, then it will receive the click event, and the
 * outer TouchWrapper is not triggered.
 * 2) If the inner TouchWrapper is not being displayed, then it will not receive a click event,
 * and the outer TouchWrapper will be triggered.
 *
 */
public class PagerTouchWrapperTest extends AbstractDocViewTest {

    static String DOC =
            "      \"type\": \"Frame\",\n" +
            "      \"backgroundColor\": \"black\",\n" +
            "      \"width\": \"100vw\",\n" +
            "      \"height\": \"100vh\",\n" +
            "      \"item\": {\n" +
            "        \"type\": \"TouchWrapper\",\n" +
            "        \"onPress\": {\n" +
            "          \"type\": \"SendEvent\",\n" +
            "          \"arguments\": \"outer\"\n" +
            "        },\n" +
            "        \"item\": {\n" +
            "          \"type\": \"Pager\",\n" +
            "          \"id\": \"pager\",\n" +
            "          \"navigation\": \"%s\",\n" +
            "          \"width\": \"100vw\",\n" +
            "          \"height\": \"100vh\",\n" +
            "          \"items\": [\n" +
            "            {\n" +
            "              \"type\": \"TouchWrapper\",\n" +
            "              \"id\": \"inner\",\n" +
            "              \"onPress\": {\n" +
            "                \"type\": \"SendEvent\",\n" +
            "                \"arguments\": \"inner\"\n" +
            "              },\n" +
            "              \"item\": {\n" +
            "                \"type\": \"Text\",\n" +
            "                \"text\": \"Text on page #1\"\n" +
            "              }\n" +
            "            },\n" +
            "            {\n" +
            "              \"type\": \"Text\",\n" +
            "              \"text\": \"Text on page #2\"\n" +
            "            }\n" +
            "          ]\n" +
            "        }\n" +
            "      }\n" +
            "    }";

    @Mock
    private ISendEventCallbackV2 mSendEventCallback;

    private void loadDocumentWithNavigation(String navigation) {
        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(inflateWithOptions(String.format(DOC, navigation), "", APLOptions.builder().sendEventCallbackV2(mSendEventCallback).build()))
                .check(hasRootContext());
        mIdlingResource = new APLViewIdlingResource(mTestContext.getTestView());
        IdlingRegistry.getInstance().register(mIdlingResource);
    }

    @Test
    public void testView_innerTouchWrapperReceivesClickBeforeSwipingPager() {
        loadDocumentWithNavigation("normal");
        Component frame = mTestContext.getTestComponent();

        onView(withComponent(frame))
                .perform(click());

        verify(mSendEventCallback).onSendEvent(eq(new String[] { "inner" }), any(), any(), any());
    }

    @Test
    public void testView_outerTouchWrapperReceivesClickAfterSwipingPager() {
        loadDocumentWithNavigation("normal");
        onView(isRoot()).perform(waitFor(100));
        Component frame = mTestContext.getTestComponent();

        onView(withComponent(frame))
                .perform(swipeLeft());

        verifyZeroInteractions(mSendEventCallback);

        onView(withComponent(frame))
                .perform(click());

        verify(mSendEventCallback).onSendEvent(eq(new String[] { "outer" }), any(), any(), any());
    }

    @Test
    public void testView_innerTouchWrapperReceivesKeyEventWithNoneNavigation() throws InterruptedException {
        loadDocumentWithNavigation("none");

        CountDownLatch latch = new CountDownLatch(2);
        doAnswer(invocation -> {
            latch.countDown();
            return null;
        }).when(mSendEventCallback).onSendEvent(eq(new String[] { "outer" }), any(), any(), any());

        // Espresso is intermittantly not calling ACTION_DOWN key events, so we'll do it explicitly here.
        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(APLViewActions.pressKey(KeyEvent.KEYCODE_DPAD_DOWN))
                .perform(APLViewActions.pressKey(KeyEvent.KEYCODE_ENTER))
                .perform(APLViewActions.pressKey(KeyEvent.KEYCODE_DPAD_CENTER));

        latch.await(4, TimeUnit.SECONDS);

        // the expectation is that core's focus doesn't recursively search in TW
        verify(mSendEventCallback, times(2)).onSendEvent(eq(new String[] { "outer" }), any(), any(), any());
    }
}
