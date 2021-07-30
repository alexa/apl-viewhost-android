/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.command;


import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.IdlingResource;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import com.amazon.apl.android.Component;
import com.amazon.apl.android.document.AbstractDocViewTest;
import com.amazon.apl.android.espresso.APLMatchers;
import com.amazon.apl.android.espresso.APLViewIdlingResource;
import com.amazon.apl.android.views.APLAbsoluteLayout;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static com.amazon.apl.android.espresso.APLViewActions.executeCommands;
import static com.amazon.apl.android.espresso.APLViewActions.waitFor;

@RunWith(AndroidJUnit4.class)
public class ScrollCommandsViewTest extends AbstractDocViewTest {
    private static final String[] COLORS = {"\"red\"", "\"blue\"", "\"yellow\"", "\"green\""};

    private static final String DOCUMENT_PROPS =
            "    \"layouts\": {\n" +
            "        \"square\": {\n" +
            "            \"parameters\": [\"color\", \"index\", \"compId\"],\n" +
            "            \"item\": {\n" +
            "                \"type\": \"Frame\",\n" +
            "                \"width\": 100,\n" +
            "                \"height\": 100,\n" +
            "                \"id\": \"${compId}\",\n" +
            "                \"backgroundColor\": \"${color}\",\n" +
            "                \"item\": {\n" +
            "                    \"type\": \"Text\",\n" +
            "                    \"text\": \"Item ${index + 1}\",\n" +
            "                    \"width\": 100,\n" +
            "                    \"height\": 100\n" +
            "                }\n" +
            "            }\n" +
            "        }\n" +
            "    }";


    private final String COMPONENT_PROPS =
            "            \"type\": \"Container\",\n" +
            "            \"direction\": \"row\",\n" +
            "            \"width\": \"100%\",\n" +
            "            \"height\": \"100%\",\n" +
            "            \"items\": [\n" +
            "                {\n" +
            "                    \"type\": \"ScrollView\",\n" +
            "                    \"id\": \"scrollView\",\n" +
            "                    \"height\": 300,\n" +
            "                    \"width\": 300,\n" +
            "                    \"item\": {\n" +
            "                        \"type\": \"Container\",\n" +
            "                        \"direction\": \"column\",\n" +
            "                        \"data\": " + createDataForPages(8) + ",\n" +
            "                        \"items\": {\n" +
            "                            \"type\": \"square\",\n" +
            "                            \"color\": \"${data.color}\",\n" +
            "                            \"index\": \"${index}\",\n" +
            "                            \"compId\": \"scrollView${index}\"\n" +
            "                        }\n" +
            "                    }\n" +
            "                },\n" +
            "                {\n" +
            "                    \"type\": \"Sequence\",\n" +
            "                    \"id\": \"sequence\",\n" +
            "                    \"height\": 300,\n" +
            "                    \"width\": 300,\n" +
            "                    \"data\": " + createDataForPages(8) + ",\n" +
            "                    \"items\": {\n" +
            "                        \"type\": \"square\",\n" +
            "                        \"color\": \"${data.color}\",\n" +
            "                        \"index\": \"${index}\",\n" +
            "                        \"compId\": \"sequence${index}\"\n" +
            "                    }\n" +
            "                }\n" +
            "            ]";

    private static final String SCROLL_COMMAND = "[\n" +
            "                {\n" +
            "                    \"type\": \"Scroll\",\n" +
            "                    \"componentId\": \"%s\",\n" +
            "                    \"distance\": %s\n" +
            "                }\n" +
            "            ]";

    private static final String SCROLL_TO_INDEX_COMMAND = "[\n" +
            "                {\n" +
            "                    \"type\": \"ScrollToIndex\",\n" +
            "                    \"componentId\": \"%s\",\n" +
            "                    \"align\": \"%s\",\n" +
            "                    \"index\": %d\n" +
            "                }\n" +
            "            ]";

    private static final String SCROLL_TO_COMPONENT_COMMAND = "[\n" +
            "                {\n" +
            "                    \"type\": \"ScrollToComponent\",\n" +
            "                    \"componentId\": \"%s\",\n" +
            "                    \"align\": \"%s\"\n" +
            "                }\n" +
            "            ]";

    private static final int COMPONENTS_PER_PAGE = 3;

    private Component mComponent;
    private APLAbsoluteLayout mView;
    private int mChildCount;
    private IdlingResource mIdlingResource;

    @Before
    public void setup() {
        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(inflate(COMPONENT_PROPS, DOCUMENT_PROPS))
                .check(hasRootContext());
    }

    @After
    public void teardown() {
        if (mIdlingResource != null) {
            IdlingRegistry.getInstance().unregister(mIdlingResource);
        }
    }

    @Test
    @LargeTest
    public void testScrollView_scrollPages() {
        testSimpleScrollPage("scrollView");
    }

    @Test
    @LargeTest
    public void testScrollView_scrollToComponent() {
        testScrollToComponent("scrollView");
    }

    @Test
    @LargeTest
    public void testSequence_scrollPages() {
        testSimpleScrollPage("sequence");
    }

    @Test
    @LargeTest
    public void testSequence_scrollToIndex() {
        testScrollToIndex("sequence");
    }

    @Test
    @LargeTest
    public void testSequence_scrollToComponent() {
        testScrollToComponent("sequence");
    }

    private void init(final String componentId) {
        mComponent = mTestContext.getRootContext().findComponentById(componentId);
        // ScrollView doesn't directly hold list of children.
        if (componentId.equals("scrollView")) {
            mComponent = mComponent.getChildAt(0);
        }
        mView = (APLAbsoluteLayout) mTestContext.getPresenter().findView(mComponent);
        mChildCount = mComponent.getChildCount();
        mIdlingResource = new APLViewIdlingResource(mView);
        IdlingRegistry.getInstance().register(mIdlingResource);
    }

    public void testSimpleScrollPage(final String componentId) {
        init(componentId);

        // Scroll one page
        testCommand(String.format(SCROLL_COMMAND, componentId, 1), COMPONENTS_PER_PAGE);

        // Scroll two more pages
        testCommand(String.format(SCROLL_COMMAND, componentId, 2), 3 * COMPONENTS_PER_PAGE);

        // Scroll three more pages
        testCommand(String.format(SCROLL_COMMAND, componentId, 3), 6 * COMPONENTS_PER_PAGE);

        // Scroll back to the beginning
        testCommand(String.format(SCROLL_COMMAND, componentId, -6), 0);
    }

    public void testScrollToIndex(final String componentId) {
        init(componentId);

        // 0, 1, 2 -> 3, 4, 5
        testCommand(String.format(SCROLL_TO_INDEX_COMMAND, componentId, "first", 3), 3);

        // 3, 4, 5 -> 4, 5, 6
        testCommand(String.format(SCROLL_TO_INDEX_COMMAND, componentId, "visible", 6), 4);

        // 4, 5, 6 - > 0, 1, 2
        testCommand(String.format(SCROLL_TO_INDEX_COMMAND, componentId, "last", 2), 0);

        // 0, 1, 2 -> 21, 22, 23
        testCommand(String.format(SCROLL_TO_INDEX_COMMAND, componentId, "center", mChildCount - 2), 21);
    }

    public void testScrollToComponent(final String componentId) {
        init(componentId);

        // 0, 1, 2 -> 3, 4, 5
        testCommand(String.format(SCROLL_TO_COMPONENT_COMMAND, componentId + 3, "first"), 3);

        // 3, 4, 5 -> 4, 5, 6
        testCommand(String.format(SCROLL_TO_COMPONENT_COMMAND, componentId + 6, "visible"), 4);

        // 4, 5, 6 - > 0, 1, 2
        testCommand(String.format(SCROLL_TO_COMPONENT_COMMAND, componentId + 2, "last"), 0);

        // 0, 1, 2 -> 21, 22, 23
        testCommand(String.format(SCROLL_TO_COMPONENT_COMMAND, componentId + (mChildCount - 2), "center"), 21);
    }

    private void testCommand(String command, final int expectedPosition) {
        onView(isRoot())
                .perform(executeCommands(mTestContext.getRootContext(), command))
                .perform(waitFor(100));

        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        for (int i = 0; i < 3; i++) {
            Component topVisibleComponent = mComponent.getChildAt(expectedPosition + i);
            onView(APLMatchers.withComponent(topVisibleComponent))
                    .check(matches(isDisplayed()));
        }
    }

    private static String createDataForPages(int pages) {
        StringBuilder sb = new StringBuilder().append("[");
        for (int i = 0; i < 3 * pages; i++) {
            sb.append("{");
            sb.append("\"color\": ");
            sb.append(COLORS[i % COLORS.length]);
            sb.append("}");
            if (i != 3 * pages - 1) {
                sb.append(",");
            }
        }

        sb.append("]");
        return sb.toString();
    }
}
