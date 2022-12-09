/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.command;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static com.amazon.apl.android.espresso.APLMatchers.withText;
import static com.amazon.apl.android.espresso.APLViewActions.executeCommands;
import static com.amazon.apl.android.espresso.APLViewActions.waitFor;

import com.amazon.apl.android.Component;
import com.amazon.apl.android.configuration.ConfigurationChange;
import com.amazon.apl.android.document.AbstractDocViewTest;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class PagerPreserveTest extends AbstractDocViewTest {

    // Test component - Pager containing 4 pages with initial page index 1 and preserves pageIndex on re-inflation
    private final String COMPONENT_PROPS_WITH_PRESERVE =
            "  \"type\": \"Pager\"," +
                    "  \"height\": \"100%\"," +
                    "  \"width\": \"100%\"," +
                    "  \"navigation\": \"normal\"," +
                    "  \"initialPage\": 1," +
                    "  \"preserve\": [\"pageIndex\"]," +
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

    private final String AUTOPAGE_COMMAND = "[{\n" +
            "    \"sequencer\": \"MAGIC\",\n" +
            "    \"type\": \"AutoPage\",\n" +
            "    \"componentId\": \"testcomp\",\n" +
            "    \"duration\": 1000\n" +
            "}]";

    private final String SETPAGE_COMMAND = "[{\n" +
            "    \"sequencer\": \"MAGIC\",\n" +
            "    \"delay\": 1000," +
            "    \"type\": \"SetPage\",\n" +
            "    \"componentId\": \"testcomp\",\n" +
            "    \"value\": 1,\n" +
            "    \"position\": \"relative\"\n" +
            "}]";

    @Before
    public void setup() {
        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(inflate(COMPONENT_PROPS_WITH_PRESERVE, DOCUMENT_PROPERTIES))
                .check(hasRootContext());
        onView(withText("2")).check(matches(isDisplayed()));
    }

    /**
     * Spec: https://developer.amazon.com/en-US/docs/alexa/alexa-presentation-language/apl-pager.html#preserve
     *
     * The BASE_DOC contains onConfigChange handler with Reinflate command preserving
     * the named sequencer "MAGIC"
     */
    @Test
    @Ignore // Need to rewrite as a test that pumps APLCore deterministically.
    public void testAutoPage_preserve_with_reinflation() {
        // AutoPage command with duration of 1000 ms starts with initial page index 1
        onView(isRoot())
                .perform(executeCommands(mTestContext.getRootContext(), AUTOPAGE_COMMAND))
                .perform(waitFor(500));

        // Page at index 2 is displayed
        Component component = mTestContext.getTestComponent();
        Component child = component.getChildAt(2);
        onView(withComponent(child))
                .check(matches(isDisplayed()));

        // Reinflate
        // Dummy config
        ConfigurationChange configChange = mTestContext.getRootContext().createConfigurationChange()
                .build();

        mTestContext.getRootContext().handleConfigurationChange(configChange);

        // After re-inflation AutoPage starts from the preserved pageIndex and finishes the remaining duration
        // check at 900 ms, page at index 2 is displayed, after 1000ms, auto page set to page at index 3
        onView(isRoot()).perform(waitFor(400));
        component = mTestContext.getTestComponent();
        child = component.getChildAt(2);
        onView(withComponent(child))
                .check(matches(isDisplayed()));

        onView(isRoot()).perform(waitFor(800));
        component = mTestContext.getTestComponent();
        child = component.getChildAt(3);
        onView(withComponent(child))
                .check(matches(isDisplayed()));
    }

    /**
     * Test to verify that SetPage command resumes after re-inflation with remaining delay
     *
     * Spec: If a command is executing on a sequencer that is preserved by a Reinflate Command,
     * then when the reinflate command is executed, the command’s remaining delay will be saved.
     */
    @Test
    @Ignore
    public void testSetPage_preserve_with_reinflation() {
        // AutoPage command with duration of 1000 ms starts with initial page index 1
        onView(isRoot())
                .perform(executeCommands(mTestContext.getRootContext(), SETPAGE_COMMAND))
                .perform(waitFor(300));

        // Page at index 2 is displayed
        Component component = mTestContext.getTestComponent();
        Component child = component.getChildAt(1);
        onView(withComponent(child))
                .check(matches(isDisplayed()));

        // Reinflate
        // Dummy config
        ConfigurationChange configChange = mTestContext.getRootContext().createConfigurationChange()
                .build();

        mTestContext.getRootContext().handleConfigurationChange(configChange);

        // After re-inflation SetPage starts from the preserved delay and executes
        onView(isRoot()).perform(waitFor(300));
        component = mTestContext.getTestComponent();
        child = component.getChildAt(1);
        onView(withComponent(child))
                .check(matches(isDisplayed()));

        onView(isRoot()).perform(waitFor(500));
        component = mTestContext.getTestComponent();
        child = component.getChildAt(2);
        onView(withComponent(child))
                .check(matches(isDisplayed()));
    }
}
