/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.document;

import android.view.KeyEvent;

import com.amazon.apl.android.Component;
import com.amazon.apl.android.DocumentState;
import com.amazon.apl.android.espresso.APLViewIdlingResource;
import com.amazon.apl.android.views.APLTextView;

import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.IdlingResource;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.action.EspressoKey;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.pressKey;
import static androidx.test.espresso.action.ViewActions.swipeLeft;
import static androidx.test.espresso.action.ViewActions.swipeUp;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasFocus;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static com.amazon.apl.android.espresso.APLViewActions.finish;
import static com.amazon.apl.android.espresso.APLViewActions.restoreAndExecuteCommands;
import static com.amazon.apl.android.espresso.APLViewActions.waitFor;
import static com.amazon.apl.android.espresso.APLViewAssertions.isFinished;
import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class RestoreDocumentTest extends AbstractDocViewTest {
    private static final String DOC_SCROLLVIEW =
            "\"type\": \"ScrollView\",\n" +
                    "      \"height\": \"100vh\",\n" +
                    "      \"width\": \"100vw\",\n" +
                    "      \"item\": {\n" +
                    "        \"type\": \"Container\",\n" +
                    "        \"items\": [\n" +
                    "          {\n" +
                    "            \"type\": \"Frame\",\n" +
                    "            \"height\": \"100vh\",\n" +
                    "            \"width\": \"100vw\",\n" +
                    "            \"id\": \"frame1\",\n" +
                    "            \"backgroundColor\": \"red\"\n" +
                    "          },\n" +
                    "          {\n" +
                    "            \"type\": \"Frame\",\n" +
                    "            \"height\": \"100vh\",\n" +
                    "            \"width\": \"100vw\",\n" +
                    "            \"id\": \"frame2\",\n" +
                    "            \"backgroundColor\": \"blue\"\n" +
                    "          },\n" +
                    "          {\n" +
                    "            \"type\": \"Frame\",\n" +
                    "            \"height\": \"100vh\",\n" +
                    "            \"width\": \"100vw\",\n" +
                    "            \"id\": \"frame3\",\n" +
                    "            \"backgroundColor\": \"green\"\n" +
                    "          },\n" +
                    "          {\n" +
                    "            \"type\": \"Text\",\n" +
                    "            \"text\": \"Hello\",\n" +
                    "            \"id\": \"textComponent\"" +
                    "          }\n" +
                    "        ]\n" +
                    "      }";

    private static final String DOC_SEQUENCE =
            "\"type\": \"Sequence\",\n" +
                    "\"scrollDirection\": \"%s\",\n" +
                    "    \"height\": \"100vh\",\n" +
                    "    \"width\": \"100vw\",\n" +
                    "    \"items\": [\n" +
                    "        {\n" +
                    "          \"type\": \"Frame\",\n" +
                    "          \"height\": \"100vh\",\n" +
                    "          \"width\": \"100vw\",\n" +
                    "          \"id\": \"frame1\",\n" +
                    "          \"backgroundColor\": \"red\"\n" +
                    "        },\n" +
                    "        {\n" +
                    "          \"type\": \"Frame\",\n" +
                    "          \"height\": \"100vh\",\n" +
                    "          \"width\": \"100vw\",\n" +
                    "          \"id\": \"frame2\",\n" +
                    "          \"backgroundColor\": \"blue\"\n" +
                    "        },\n" +
                    "        {\n" +
                    "          \"type\": \"Frame\",\n" +
                    "          \"height\": \"100vh\",\n" +
                    "          \"width\": \"100vw\",\n" +
                    "          \"id\": \"frame3\",\n" +
                    "          \"backgroundColor\": \"green\"\n" +
                    "        },\n" +
                    "        {\n" +
                    "          \"type\": \"Text\",\n" +
                    "          \"text\": \"Hello\",\n" +
                    "          \"id\": \"textComponent\"" +
                    "        }\n" +
                    "      ]";

    private static final String DOC_PAGER =
            "\"type\": \"Pager\",\n" +
                    "    \"height\": \"100vh\",\n" +
                    "    \"width\": \"100vw\",\n" +
                    "    \"items\": [\n" +
                    "        {\n" +
                    "          \"type\": \"Frame\",\n" +
                    "          \"height\": \"100vh\",\n" +
                    "          \"width\": \"100vw\",\n" +
                    "          \"id\": \"frame1\",\n" +
                    "          \"backgroundColor\": \"red\"\n" +
                    "        },\n" +
                    "        {\n" +
                    "          \"type\": \"Frame\",\n" +
                    "          \"height\": \"100vh\",\n" +
                    "          \"width\": \"100vw\",\n" +
                    "          \"id\": \"frame2\",\n" +
                    "          \"backgroundColor\": \"blue\"\n" +
                    "        },\n" +
                    "        {\n" +
                    "          \"type\": \"Frame\",\n" +
                    "          \"height\": \"100vh\",\n" +
                    "          \"width\": \"100vw\",\n" +
                    "          \"id\": \"frame3\",\n" +
                    "          \"backgroundColor\": \"green\"\n" +
                    "        },\n" +
                    "        {\n" +
                    "          \"type\": \"Text\",\n" +
                    "          \"text\": \"Hello\",\n" +
                    "          \"id\": \"textComponent\"" +
                    "        }\n" +
                    "      ]";

    private static final String DOC_SEQUENCE_TOUCH_WRAPPER =
            "\"type\": \"Sequence\",\n" +
                    "    \"height\": \"100vh\",\n" +
                    "    \"width\": \"100vw\",\n" +
                    "    \"items\": [\n" +
                    "        {\n" +
                    "          \"type\": \"TouchWrapper\",\n" +
                    "          \"height\": \"100vh\",\n" +
                    "          \"width\": \"100vw\",\n" +
                    "          \"id\": \"frame1\",\n" +
                    "          \"item\": \n" +
                    "        {\n" +
                    "          \"type\": \"Frame\",\n" +
                    "          \"height\": \"100vh\",\n" +
                    "          \"width\": \"100vw\",\n" +
                    "          \"backgroundColor\": \"red\"\n" +
                    "        }\n" +
                    "        },\n" +
                    "        {\n" +
                    "          \"type\": \"TouchWrapper\",\n" +
                    "          \"height\": \"100vh\",\n" +
                    "          \"width\": \"100vw\",\n" +
                    "          \"id\": \"frame2\",\n" +
                    "          \"item\": \n" +
                    "        {\n" +
                    "          \"type\": \"Frame\",\n" +
                    "          \"height\": \"100vh\",\n" +
                    "          \"width\": \"100vw\",\n" +
                    "          \"backgroundColor\": \"blue\"\n" +
                    "        }\n" +
                    "        },\n" +
                    "        {\n" +
                    "          \"type\": \"TouchWrapper\",\n" +
                    "          \"height\": \"100vh\",\n" +
                    "          \"width\": \"100vw\",\n" +
                    "          \"id\": \"frame3\",\n" +
                    "          \"item\": \n" +
                    "        {\n" +
                    "          \"type\": \"Frame\",\n" +
                    "          \"height\": \"100vh\",\n" +
                    "          \"width\": \"100vw\",\n" +
                    "          \"backgroundColor\": \"green\"\n" +
                    "        }\n" +
                    "        }\n" +
                    "      ]";

    private IdlingResource mIdlingResource;

    @After
    public void teardown() {
        if (mIdlingResource != null) {
            IdlingRegistry.getInstance().unregister(mIdlingResource);
        }
    }

    @Test
    public void testRestoreDocument_scrollViewRestoresState() {
        testFramesAreDisplayed(DOC_SCROLLVIEW, swipeUp(), swipeUp());
    }

    @Test
    public void testRestoreDocument_sequence_vertical_RestoresState() {
        testFramesAreDisplayed(String.format(DOC_SEQUENCE, "vertical"), swipeUp(), swipeUp());
    }

    @Test
    public void testRestoreDocument_sequence_horizontal_RestoresState() {
        testFramesAreDisplayed(String.format(DOC_SEQUENCE, "horizontal"), swipeLeft(), swipeLeft());
    }

    @Test
    public void testRestoreDocument_pagerRestoresState() {
        testFramesAreDisplayed(DOC_PAGER, swipeLeft(), swipeLeft());
    }

    // TODO determine better way to swipe until view is displayed.
    private void testFramesAreDisplayed(String DOC, ViewAction... swipeActions) {
        // Inflate first document
        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(inflate(DOC, ""))
                .check(hasRootContext());


        mIdlingResource = new APLViewIdlingResource(mTestContext.getTestView());
        IdlingRegistry.getInstance().register(mIdlingResource);

        // Swipe to last frame
        for (ViewAction swipeAction : swipeActions) {
            onView(withId(com.amazon.apl.android.test.R.id.apl))
                    .perform(swipeAction);
        }

        // Verify last frame is displayed
        verifyFrameIsDisplayed("frame3");

        DocumentState cacheDocumentState = mAplController.getDocumentState();

        IdlingRegistry.getInstance().unregister(mIdlingResource);

        // Finish and inflate another document
        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(finish(mAplController))
                .check(isFinished());

        activityRule.getScenario().recreate();

        // Finish and inflate another document
        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(inflate(DOC, ""))
                .check(hasRootContext());

        // Verify that initial frame is displayed
        verifyFrameIsDisplayed("frame1");

        // Finish and restore first document
        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(finish(mAplController))
                .check(isFinished());

        String commands = "[\n" +
                "    {\n" +
                "        \"type\": \"SetValue\",\n" +
                "        \"property\": \"text\",\n" +
                "        \"value\": \"World\",\n" +
                "        \"componentId\": \"textComponent\"\n" +
                "    }\n" +
                "]";

        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(restoreAndExecuteCommands(mTestContext.getPresenter(), cacheDocumentState, commands))
                .check(hasRootContext());

        mIdlingResource = new APLViewIdlingResource(mTestContext.getTestView());
        IdlingRegistry.getInstance().register(mIdlingResource);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        // Verify that last frame is displayed
        verifyFrameIsDisplayed("frame3");
        // Verify that the SetValue command is executed after document is restored
        verifyCommandsExecuted();
    }

    private void verifyFrameIsDisplayed(String displayedFrame) {
        Component displayed = mTestContext.getRootContext().findComponentById(displayedFrame);
        onView(withComponent(displayed))
                .check(matches(isDisplayed()));
    }

    private void verifyCommandsExecuted() {
        onView(isRoot()).perform(waitFor(100));
        Component textComponent = mTestContext.getRootContext().findComponentById("textComponent");
        APLTextView aplTextView = (APLTextView) mTestContext.getPresenter().findView(textComponent);
        assertEquals("World", aplTextView.getLayout().getText().toString());
    }
}
