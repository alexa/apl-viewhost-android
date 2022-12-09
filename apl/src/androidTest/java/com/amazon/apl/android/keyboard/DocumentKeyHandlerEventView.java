/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.keyboard;

import android.graphics.Color;
import android.view.KeyEvent;

import com.amazon.apl.android.document.AbstractDocViewTest;
import com.amazon.apl.android.Component;
import com.amazon.apl.android.espresso.APLViewActions;

import org.junit.Test;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static com.amazon.apl.android.espresso.APLMatchers.withBackgroundColor;
import static org.hamcrest.Matchers.not;

public class DocumentKeyHandlerEventView extends AbstractDocViewTest {
    private static final String COMPONENT_PROPS =
            "      \"type\": \"Frame\"," +
            "      \"backgroundColor\": \"black\"," +
            "      \"width\": \"100%\"," +
            "      \"height\": \"100%\"," +
            "      \"display\": \"none\"";

    /**
     * Verifies 'handleKeyDown' document event is triggered.
     */
    @Test
    public void testView_executeKeyDownHandler() {
        String documentProps =
                "  \"handleKeyDown\": [{ " +
                "    \"when\": \"${event.keyboard.code == 'KeyW'}\"," +
                "    \"commands\": [{ " +
                "      \"type\": \"SetValue\"," +
                "      \"property\": \"display\"," +
                "      \"value\": \"normal\"," +
                "      \"componentId\": \"testcomp\"" +
                "    }]" +
                "  }]";

        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(inflate(COMPONENT_PROPS, documentProps))
                .check(hasRootContext());

        runTestIfFrameDisplayed();
    }

    /**
     * Verifies 'handleKeyUp' document event is triggered.
     */
    @Test
    public void testView_executeKeyUpHandler() {
        String documentProps =
                "\"handleKeyUp\": [{ " +
                "    \"when\": \"${event.keyboard.code == 'KeyW'}\"," +
                "    \"commands\": [{ " +
                "      \"type\": \"SetValue\"," +
                "      \"property\": \"display\"," +
                "      \"value\": \"normal\"," +
                "      \"componentId\": \"testcomp\"" +
                "    }]" +
                "  }]";

        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(inflate(COMPONENT_PROPS, documentProps))
                .check(hasRootContext());

        runTestIfFrameDisplayed();
    }

    /**
     * Verifies 'handleKeyDown' document event is triggered on multiple and different
     * key events.
     */
    @Test
    public void testView_executeKeyDownHandler_withMultipleKeyConditions() {
        String documentProps =
                "  \"handleKeyDown\": [{ " +
                "    \"when\": \"${event.keyboard.code == 'KeyW'}\"," +
                "    \"commands\": [{ " +
                "      \"type\": \"SetValue\"," +
                "      \"property\": \"display\"," +
                "      \"value\": \"normal\"," +
                "      \"componentId\": \"testcomp\"" +
                "    }]" +
                "  }, {" +
                "    \"when\": \"${event.keyboard.code == 'KeyS'}\"," +
                "    \"commands\": [{ " +
                "      \"type\": \"SetValue\"," +
                "      \"property\": \"backgroundColor\"," +
                "      \"value\": \"blue\"," +
                "      \"componentId\": \"testcomp\"" +
                "    }]" +
                "  }]";

        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(inflate(COMPONENT_PROPS, documentProps))
                .check(hasRootContext());

        Component c1 = mTestContext.getTestComponent();

        // verify frame is not displayed and have default background color
        onView(withComponent(c1))
                .check(matches(not(isDisplayed())));

        onView(withComponent(c1))
                .check(matches(withBackgroundColor(Color.BLACK)));

        // send two key events
        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(APLViewActions.pressKey(KeyEvent.KEYCODE_W));
        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(APLViewActions.pressKey(KeyEvent.KEYCODE_S));

        // verify frame is displayed and background color has changed
        onView(withComponent(c1))
                .check(matches(isDisplayed()));

        onView(withComponent(c1))
                .check(matches(withBackgroundColor(Color.BLUE)));
    }

    /**
     * Verifies 'handleKeyDown' document event is triggered on multiple and different
     * key events.
     */
    @Test
    public void testView_executeBothKeyHandlers() {
        String documentProps =
                "  \"handleKeyDown\": [{ " +
                "    \"when\": \"${event.keyboard.code == 'KeyW'}\"," +
                "    \"commands\": [{ " +
                "      \"type\": \"SetValue\"," +
                "      \"property\": \"display\"," +
                "      \"value\": \"normal\"," +
                "      \"componentId\": \"testcomp\"" +
                "    }]" +
                "  }]," +
                "  \"handleKeyUp\": [{ " +
                "    \"when\": \"${event.keyboard.code == 'KeyW'}\"," +
                "    \"commands\": [{ " +
                "      \"type\": \"SetValue\"," +
                "      \"property\": \"backgroundColor\"," +
                "      \"value\": \"blue\"," +
                "      \"componentId\": \"testcomp\"" +
                "    }]" +
                "  }]";

        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(inflate(COMPONENT_PROPS, documentProps))
                .check(hasRootContext());

        Component c1 = mTestContext.getTestComponent();

        // verify frame is not displayed and have default background color
        onView(withComponent(c1))
                .check(matches(not(isDisplayed())));

        onView(withComponent(c1))
                .check(matches(withBackgroundColor(Color.BLACK)));

        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(APLViewActions.pressKey(KeyEvent.KEYCODE_W));

        // verify frame is displayed and background color has changed
        onView(withComponent(c1))
                .check(matches(isDisplayed()));

        onView(withComponent(c1))
                .check(matches(withBackgroundColor(Color.BLUE)));
    }

    // TODO: Add integ. test when there is a focusable item in the document.

    private void runTestIfFrameDisplayed() {
        Component c1 = mTestContext.getTestComponent();

        // verify frame is not displayed
        onView(withComponent(c1))
                .check(matches(not(isDisplayed())));

        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(APLViewActions.pressKey(KeyEvent.KEYCODE_W));

        // verify frame is displayed
        onView(withComponent(c1))
                .check(matches(isDisplayed()));
    }
}
