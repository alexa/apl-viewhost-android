/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.accessibility;

import android.content.Intent;

import androidx.test.espresso.ViewAssertion;
import androidx.test.platform.app.InstrumentationRegistry;

import com.amazon.apl.android.APLOptions;
import com.amazon.apl.android.Component;
import com.amazon.apl.android.dependencies.ISendEventCallback;
import com.amazon.apl.android.dependencies.ISendEventCallbackV2;
import com.amazon.apl.android.document.AbstractDocViewTest;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.doubleClick;
import static androidx.test.espresso.action.ViewActions.swipeRight;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static com.amazon.apl.android.espresso.APLViewActions.waitFor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


// TODO uncomment and make pass
public class SequenceAccessibilityTest extends AbstractDocViewTest {
    static final String DOC = "\n" +
            "          \"type\": \"Sequence\",\n" +
            "          \"position\": \"absolute\",\n" +
            "          \"scrollDirection\": \"vertical\",\n" +
            "          \"data\": \"${payload.listData}\",\n" +
            "          \"width\": \"100vw\",\n" +
            "          \"height\": \"100vh\",\n" +
            "          \"item\":\n" +
            "          {\n" +
            "            \"type\": \"TouchWrapper\",\n" +
            "            \"accessibilityLabel\": \"${index % 2 == 0 ? data.item : \\\"\\\"}\",\n" +
            "            \"disabled\": \"${index % 3 == 0 ? true : false}\",\n" +
            "            \"onPress\": {\n" +
            "              \"type\": \"SendEvent\",\n" +
            "              \"arguments\": [\n" +
            "                \"${data.item}\"\n" +
            "              ]\n" +
            "            },\n" +
            "            \"item\": {\n" +
            "              \"type\": \"Container\",\n" +
            "              \"direction\": \"row\",\n" +
            "              \"height\": 120,\n" +
            "              \"alignItems\": \"center\",\n" +
            "              \"items\": [\n" +
            "                {\n" +
            "                  \"type\": \"Frame\",\n" +
            "                  \"borderRadius\": 45,\n" +
            "                  \"width\": 60,\n" +
            "                  \"height\": 60,\n" +
            "                  \"backgroundColor\":\"${data.backgroundColor}\",\n" +
            "                  \"item\": {\n" +
            "                    \"type\": \"Text\",\n" +
            "                    \"text\": \"${data.number}\",\n" +
            "                    \"fontSize\": \"20\",\n" +
            "                    \"width\": 60,\n" +
            "                    \"height\": 60,\n" +
            "                    \"textAlign\": \"center\",\n" +
            "                    \"textAlignVertical\": \"center\"\n" +
            "                  }\n" +
            "                },\n" +
            "                {\n" +
            "                  \"type\": \"Image\",\n" +
            "                  \"source\": \"${data.imageSource}\",\n" +
            "                  \"spacing\": 24,\n" +
            "                  \"width\": 100,\n" +
            "                  \"height\": 100,\n" +
            "                  \"borderRadius\": 10\n" +
            "                },\n" +
            "                {\n" +
            "                  \"type\": \"Text\",\n" +
            "                  \"id\": \"text1\",\n" +
            "                  \"text\": \"${data.item}\",\n" +
            "                  \"color\": \"black\",\n" +
            "                  \"grow\": 1,\n" +
            "                  \"shrink\": 1,\n" +
            "                  \"spacing\": 24\n" +
            "                }\n" +
            "              ]\n" +
            "            }\n" +
            "          }\n" +
            "        ";

    static final String DATA = "{\n" +
            "  \"listData\": [\n" +
            "    {\n" +
            "      \"number\": \"1\",\n" +
            "      \"item\": \"Pick up Stacy from soccer practice\",\n" +
            "      \"backgroundColor\": \"red\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"number\": \"2\",\n" +
            "      \"item\": \"Walk the dog\",\n" +
            "      \"backgroundColor\": \"blue\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"number\": \"3\",\n" +
            "      \"item\": \"Buy the kids costumes\",\n" +
            "      \"backgroundColor\": \"green\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"number\": \"4\",\n" +
            "      \"item\": \"Book flight to Paris\",\n" +
            "      \"backgroundColor\": \"purple\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"number\": \"5\",\n" +
            "      \"item\": \"Buy groceries\",\n" +
            "      \"backgroundColor\": \"red\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"number\": \"6\",\n" +
            "      \"item\": \"Wash car\",\n" +
            "      \"backgroundColor\": \"blue\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"number\": \"7\",\n" +
            "      \"item\": \"Pay bills\",\n" +
            "      \"backgroundColor\": \"green\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"number\": \"8\",\n" +
            "      \"item\": \"Buy shampoo and conditioner for the kids\",\n" +
            "      \"backgroundColor\": \"purple\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"number\": \"9\",\n" +
            "      \"item\": \"Pick up dry cleaning\",\n" +
            "      \"backgroundColor\": \"red\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"number\": \"10\",\n" +
            "      \"item\": \"Watch the Oscars\",\n" +
            "      \"backgroundColor\": \"blue\"\n" +
            "    }"+
            "  ]\n" +
            "}";

    // TODO setup for the package suite instead of class as these run
    //  Not sure if we need https://developer.android.com/training/testing/espresso/accessibility-checking
    @BeforeClass
    public static void setUpAccessibility() {
        InstrumentationRegistry.getInstrumentation().getContext().sendBroadcast(new Intent("com.amazon.logan.settings.TOGGLE_ON_OFF").setPackage("com.amazon.logan"));
    }

    @AfterClass
    public static void teardownAccessibility() {
        InstrumentationRegistry.getInstrumentation().getContext().sendBroadcast(new Intent("com.amazon.logan.settings.TOGGLE_ON_OFF").setPackage("com.amazon.logan"));
    }

    private ISendEventCallback mSendEventCallback = mock(ISendEventCallback.class);
    private Component mSequence;

    @Before
    public void setup() {
        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(inflate(DOC, "", "payload", DATA, APLOptions.builder().sendEventCallback(mSendEventCallback).build()))
                .check(hasRootContext());
        mSequence = mTestContext.getTestComponent();
        // TODO poll APLLayout for accessibility to be active.
        onView(isRoot()).perform(waitFor(5000));
    }

    @Ignore
    @Test
    public void testInitialAccessibilityFocus() {
        Component firstChild = mSequence.getChildAt(0);
        onView(withComponent(firstChild))
                .check(isAccessibilityFocused());
    }

    @Ignore
    @Test
    public void testAccessibilityClick() {
        // Double tap to activate
        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(doubleClick());

        verify(mSendEventCallback, times(1)).onSendEvent(eq(new String[] {"Pick up Stacy from soccer practice"}), any(), any());
    }

    @Ignore
    @Test
    public void testMoveAccessibilityFocus() {
        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(swipeRight());

        onView(withComponent(mSequence.getChildAt(1)))
                .check(isAccessibilityFocused());
    }

    static ViewAssertion isAccessibilityFocused() {
        return (view, noViewFoundException) -> {
            if (!view.isAccessibilityFocused()) {
                throw new AssertionError("View: " + view + " not focused by accessibility");
            }
        };
    }
}
