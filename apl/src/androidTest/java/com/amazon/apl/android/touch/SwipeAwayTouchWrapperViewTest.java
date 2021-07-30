/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.touch;


import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.amazon.apl.android.views.APLAbsoluteLayout;
import com.amazon.apl.android.document.AbstractDocViewTest;
import com.amazon.apl.android.Component;
import com.amazon.apl.android.espresso.APLMatchers;

import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.swipeLeft;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;


/**
 * Tests SwipeAway gesture functionality on swipe-able components:
 *
 * https://developer.amazon.com/en-US/docs/alexa/alexa-presentation-language/apl-swipeaway.html
 */
@RunWith(AndroidJUnit4.class)
public class SwipeAwayTouchWrapperViewTest extends AbstractDocViewTest {

    private static final String SWIPE_AWAY_TOUCH_WRAPPER =
            "        \"type\": \"TouchWrapper\",\n" +
            "        \"width\": \"90vw\",\n" +
            "        \"componentId\": \"testcomp\",\n" +
            "        \"item\": {\n" +
            "          \"type\": \"Frame\",\n" +
            "          \"backgroundColor\": \"blue\",\n" +
            "          \"paddingLeft\": \"20dp\",\n" +
            "          \"items\": {\n" +
            "            \"type\": \"Text\",\n" +
            "            \"text\": \"You have not swiped\",\n" +
            "            \"fontSize\": 60\n" +
            "          }\n" +
            "        },\n" +
            "        \"gestures\": [\n" +
            "          {\n" +
            "            \"type\": \"SwipeAway\",\n" +
            "            \"direction\": \"left\",\n" +
            "            \"action\":\"%s\",\n" +
            "            \"items\": {\n" +
            "              \"type\": \"Frame\",\n" +
            "              \"backgroundColor\": \"purple\",\n" +
            "              \"paddingLeft\": \"20dp\",\n" +
            "              \"width\": \"90vw\",\n" +
            "              \"items\": {\n" +
            "                \"type\": \"Text\",\n" +
            "                \"text\": \"You have swiped\",\n" +
            "                \"fontSize\": 60,\n" +
            "                \"color\": \"white\"\n" +
            "              }\n" +
            "            }\n" +
            "          }\n" +
            "        ]\n";

    @Test
    public void testSwipeAway_Slide() {
        testSwipeAwayWithAction("slide");
    }

    @Test
    public void testSwipeAway_Cover() {
        testSwipeAwayWithAction("cover");
    }

    @Test
    public void testSwipeAway_Reveal() {
        testSwipeAwayWithAction("reveal");
    }
    private void testSwipeAwayWithAction(String action) {
        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(inflate(String.format(SWIPE_AWAY_TOUCH_WRAPPER, action), ""))
                .check(hasRootContext());

        final Component touchWrapper = mTestContext.getTestComponent();
        final APLAbsoluteLayout absoluteLayout = mTestContext.getTestView();

        // Perform a swipe gesture
        onView(withComponent(touchWrapper)).perform(swipeLeft(), swipeLeft());

        // Verify TouchWrapper has changed its child views
        assertEquals(absoluteLayout.getChildCount(), 1);

        // Check views are displayed properly
        onView(APLMatchers.withText("You have not swiped")).check(doesNotExist());
        onView(APLMatchers.withText("You have swiped")).check(matches(isDisplayed()));
    }
}

