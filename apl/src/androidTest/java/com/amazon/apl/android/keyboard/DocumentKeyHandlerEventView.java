/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.keyboard;

import android.graphics.Color;
import android.view.KeyEvent;
import android.view.View;

import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;

import com.amazon.apl.android.APLLayout;
import com.amazon.apl.android.document.AbstractDocViewTest;
import com.amazon.apl.android.Component;

import org.hamcrest.Matcher;
import org.junit.Test;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.actionWithAssertions;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayingAtLeast;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
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
        onView(isRoot())
                .perform(keyEvent(KeyEvent.KEYCODE_W));
        onView(isRoot())
                .perform(keyEvent(KeyEvent.KEYCODE_S));

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

        onView(isRoot())
                .perform(keyEvent(KeyEvent.KEYCODE_W));

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

        onView(isRoot())
                .perform(keyEvent(KeyEvent.KEYCODE_W));

        // verify frame is displayed
        onView(withComponent(c1))
                .check(matches(isDisplayed()));
    }

    public ViewAction keyEvent(int keyCode) {
        return actionWithAssertions(new KeyEventAction(keyCode));
    }

    private class KeyEventAction implements ViewAction {
        private final int mKeyCode;

        public KeyEventAction(int keyCode) {
            this.mKeyCode = keyCode;
        }

        @Override
        public Matcher<View> getConstraints() {
            Matcher<View> standardConstraint = isDisplayingAtLeast(90);
            return standardConstraint;
        }

        @Override
        public String getDescription() {
            return "Performs a key press.";
        }

        @Override
        public void perform(UiController uiController, View view) {
            APLLayout aplLayout = activityRule.getActivity().findViewById(com.amazon.apl.android.test.R.id.apl); // APLLayout
            aplLayout.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, mKeyCode));
            aplLayout.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, mKeyCode));

            // Dispatching the key events will make the root context dirty until it's handled.
            while (mTestContext.getRootContext().isDirty()) {
                uiController.loopMainThreadForAtLeast(50);
            }
        }
    }
}
