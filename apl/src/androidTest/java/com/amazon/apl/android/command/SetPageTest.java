/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.command;

import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.IdlingResource;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.amazon.apl.android.document.AbstractDocViewTest;
import com.amazon.apl.android.espresso.APLViewIdlingResource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static com.amazon.apl.android.espresso.APLMatchers.withText;
import static com.amazon.apl.android.espresso.APLViewActions.executeCommands;
import static com.amazon.apl.android.espresso.APLViewActions.waitFor;

@RunWith(AndroidJUnit4.class)
public class SetPageTest extends AbstractDocViewTest {

    // Test content
    private final String COMPONENT_PROPS =
            "  \"type\": \"Pager\"," +
            "  \"height\": \"100%\"," +
            "  \"width\": \"100%\"," +
            "  \"navigation\": \"normal\"," +
            "  \"initialPage\": 1," +
            "  \"items\": [" +
            "    {" +
            "      \"type\": \"Text\"," +
            "      \"text\": \"1\"" +
            "    }, {" +
            "      \"type\": \"Text\"," +
            "      \"text\": \"2\"" +
            "    }, {" +
            "      \"type\": \"Text\"," +
            "      \"text\": \"3\"" +
            "    }, {" +
            "      \"type\": \"Text\"," +
            "      \"text\": \"4\"" +
            "    }" +
            "  ]";

    // Test commands
    private final String SET_PAGE_COMMAND = "[{" +
            "\"type\": \"SetPage\"," +
            "    \"componentId\": \"testcomp\"," +
            "    \"position\": \"%s\"," +
            "    \"value\": %s" +
            "}]";

    private final String AUTOPAGE_COMMAND = "[{" +
            "\"type\": \"AutoPage\"," +
            "    \"componentId\": \"testcomp\"," +
            "    \"duration\": %s" +
            "}]";

    private IdlingResource mIdlingResource;

    @Before
    public void setup() {
        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(inflate(COMPONENT_PROPS, ""))
                .check(hasRootContext());
        onView(withText("2")).check(matches(isDisplayed()));

        mIdlingResource = new APLViewIdlingResource(mTestContext.getTestView());
        IdlingRegistry.getInstance().register(mIdlingResource);
    }

    @After
    public void teardown() {
        if (mIdlingResource != null) {
            IdlingRegistry.getInstance().unregister(mIdlingResource);
        }
    }

    @Test
    public void testSetPageCommand_ForwardOnePageUsingRelativePositionCase() {
        onView(isRoot()).perform(executeCommands(mTestContext.getRootContext(), createSetPageDoc("relative", 1)));
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        onView(withText("3")).check(matches(isDisplayed()));
    }

    @Test
    public void testSetPageCommand_BackwardOnePageUsingRelativePositionCase() {
        onView(isRoot()).perform(executeCommands(mTestContext.getRootContext(), createSetPageDoc("relative", -1)));
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        onView(withText("1")).check(matches(isDisplayed()));
    }

    @Test
    public void testSetPageCommand_MovePageUsingAbsolutePositionCase() {
        onView(isRoot()).perform(executeCommands(mTestContext.getRootContext(), createSetPageDoc("absolute", 2)));
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        onView(withText("3")).check(matches(isDisplayed()));
    }

    @Test
    public void testSetPageCommand_MoveToLastPageUsingAbsolutePositionCase() {
        onView(isRoot()).perform(executeCommands(mTestContext.getRootContext(), createSetPageDoc("absolute", -1)));
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        onView(withText("4")).check(matches(isDisplayed()));
    }

    @Test
    public void testAutoPageCommand_NavigateToLastPageCase() {
        int waitTime = 500;
        onView(isRoot()).perform(executeCommands(mTestContext.getRootContext(), createAutoPage(waitTime)))
                .perform(waitFor(100));

        onView(withText("3")).check(matches(isDisplayed()));

        onView(isRoot()).perform(waitFor(waitTime));
        onView(withText("4")).check(matches(isDisplayed()));
    }

    private String createSetPageDoc(String position, int value) {
        return String.format(String.format(SET_PAGE_COMMAND, position, value));
    }

    private String createAutoPage(int duration) {
        return String.format(AUTOPAGE_COMMAND, duration);
    }
}

