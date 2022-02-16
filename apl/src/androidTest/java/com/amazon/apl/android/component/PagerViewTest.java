/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.component;

import android.view.View;

import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.IdlingResource;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import com.amazon.apl.android.Component;
import com.amazon.apl.android.IAPLViewPresenter;
import com.amazon.apl.android.MultiChildComponent;
import com.amazon.apl.android.espresso.APLViewIdlingResource;
import com.amazon.apl.android.views.APLAbsoluteLayout;
import com.amazon.apl.android.views.APLTextView;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.swipeLeft;
import static androidx.test.espresso.action.ViewActions.swipeRight;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static com.amazon.apl.android.espresso.APLViewActions.waitFor;
import static org.hamcrest.Matchers.anything;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class PagerViewTest extends AbstractComponentViewTest<APLAbsoluteLayout, MultiChildComponent> {

    static String ANOTHER_CHILD_LAYOUT_PROPERTIES = "";

    private IdlingResource mIdlingResource = null;

    @Override
    Class<APLAbsoluteLayout> getViewClass() {
        return APLAbsoluteLayout.class;
    }

    @Before
    public void doBefore() {
        REQUIRED_PROPERTIES = ""; // no required properties in Pager Component.
        OPTIONAL_PROPERTIES =
                " \"navigation\": \"normal\", " +
                        " \"initialPage\": 2," +
                        " \"items\": [ {" +
                        "     \"type\": \"Frame\", " +
                        "     \"backgroundColor\": \"red\" " +
                        " }, { " +
                        "     \"type\": \"Frame\", " +
                        "     \"backgroundColor\": \"blue\" " +
                        " }, {" +
                        "     \"type\": \"Frame\", " +
                        "     \"backgroundColor\": \"yellow\" " +
                        " }]";
        CHILD_LAYOUT_PROPERTIES =
                " \"items\": [\n" +
                        "        {\n" +
                        "          \"type\": \"Text\",\n" +
                        "          \"text\": \"Text content shown on page #1\"\n" +
                        "        },\n" +
                        "        {\n" +
                        "          \"type\": \"Text\",\n" +
                        "          \"text\": \"Text content shown on page #2\"\n" +
                        "        },\n" +
                        "        {\n" +
                        "          \"type\": \"Text\",\n" +
                        "          \"text\": \"Text content shown on page #3\"\n" +
                        "        },\n" +
                        "        {\n" +
                        "          \"type\": \"Text\",\n" +
                        "          \"text\": \"Text content shown on page #4\"\n" +
                        "        }\n" +
                        "      ]";

        ANOTHER_CHILD_LAYOUT_PROPERTIES =
                " \"items\": [ {" +
                        "     \"type\": \"Frame\", " +
                        "     \"backgroundColor\": \"red\" " +
                        " }, { " +
                        "     \"type\": \"Frame\", " +
                        "     \"backgroundColor\": \"blue\" " +
                        " }, {" +
                        "     \"type\": \"Frame\", " +
                        "     \"backgroundColor\": \"yellow\" " +
                        " }]";
    }

    @After
    public void doAfter() {
        if (mIdlingResource != null) {
            IdlingRegistry.getInstance().unregister(mIdlingResource);
            mIdlingResource = null;
        }
    }

    @Test
    @LargeTest
    @Override
    public void testView_layout() {
        // this is overriding the super layout because not all pager children are on the screen
        // at the same time.
        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(inflate(REQUIRED_PROPERTIES, CHILD_LAYOUT_PROPERTIES))
                .check(hasRootContext());

        Component component = mTestContext.getTestComponent();
        mIdlingResource = new APLViewIdlingResource(mTestContext.getTestView());
        IdlingRegistry.getInstance().register(mIdlingResource);

        for (int i = 0; i < component.getChildCount(); i++) {
            Component child = component.getChildAt(i);
            onView(withComponent(child))
                    .check(matches(isDisplayed()));

            // swipe to the next view after verifying previous
            onView(withComponent(child))
                    .perform(swipeLeft());
        }

        // Validate that last item shown is the first page (wrap mode)
        Component firstChild = component.getChildAt(0);
        onView(withComponent(firstChild))
                .check(matches(isDisplayed()));
    }


    @Test
    @LargeTest
    public void testView_layout_withNormalNavigation() {
        // this is overriding the super layout because not all pager children are on the screen
        // at the same time.
        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(inflate(REQUIRED_PROPERTIES, " \"navigation\": \"normal\", " + CHILD_LAYOUT_PROPERTIES))
                .check(hasRootContext());

        Component component = mTestContext.getTestComponent();
        mIdlingResource = new APLViewIdlingResource(mTestContext.getTestView());
        IdlingRegistry.getInstance().register(mIdlingResource);

        for (int i = 0; i < component.getChildCount()-1; i++) {
            Component child = component.getChildAt(i);
            onView(withComponent(child))
                    .check(matches(isDisplayed()));

            // swipe to the next view after verifying previous
            onView(withComponent(child))
                    .perform(swipeLeft());
        }

        // Validate that last item shown is the last page
        Component lastChild = component.getChildAt(component.getChildCount() - 1);
        onView(withComponent(lastChild))
                .check(matches(isDisplayed()));
    }

    private String getItems(int n) {
        final String[] color = {"red", "orange", "yellow", "green", "blue", "purple"};

        String items = "\"items\": [ ";
        for (int i = 0; i < n; i++) {
            items += "{ " +
                    "\"type\": \"Frame\"," +
                    "\"backgroundColor\": \"" + color[i % color.length] + "\"" +
                    " }";
            if (i < n - 1) items += ", ";
        }
        items += " ]";
        return items;
    }

    private void wrapNavigation(int nItems, int nIterations) {
        // create a wrap around pager with 'nItems' children
        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(inflate(REQUIRED_PROPERTIES, " \"navigation\": \"wrap\", "+getItems(nItems)))
                .check(hasRootContext());

        Component component = mTestContext.getTestComponent();
        mIdlingResource = new APLViewIdlingResource(mTestContext.getTestView());
        IdlingRegistry.getInstance().register(mIdlingResource);

        int childCount = component.getChildCount();
        int swipeCount = childCount * nIterations;
        // walk forward thru all the children from left to right 'nIterations' times
        //     0 -> 1 -> 2 ... count-2 -> count-1 -> 0 -> 1 -> 2 ... count-2 -> count-1 -> 0
        for (int i = 0; i < swipeCount; i++) {
            Component child = component.getChildAt(i % childCount);
            onView(withComponent(child))
                    .check(matches(isDisplayed()));

            // swipe to the next view after verifying previous
            onView(withComponent(child))
                    .perform(swipeLeft());
        }
        // walk backward thru all the children from right to left 'nIterations' times
        //     0 -> count-1 -> count-2 ... 2 -> 1 -> 0 -> count-1 -> count-2 ... 2 -> 1 -> 0
        for (int i = swipeCount; i > 0; i--) {
            Component child = component.getChildAt(i % childCount);
            onView(withComponent(child))
                    .check(matches(isDisplayed()));

            // swipe to the next view after verifying previous
            onView(withComponent(child))
                    .perform(swipeRight());
        }

        // Validate that last item shown is the first page
        Component child = component.getChildAt(0);
        onView(withComponent(child))
                .check(matches(isDisplayed()));

        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }

    @Test
    public void testView_opacityZero_stillInflatesViews() {
        String componentProps =
                "\"onMount\": [\n" +
                "        {\n" +
                "          \"type\": \"Sequential\",\n" +
                "          \"commands\": [\n" +
                "            {\n" +
                "              \"type\": \"SetValue\",\n" +
                "              \"componentId\": \"slide-0\",\n" +
                "              \"property\": \"opacity\",\n" +
                "              \"value\": 0\n" +
                "            }\n" +
                "          ],\n" +
                "          \"finally\": [\n" +
                "            {\n" +
                "              \"type\": \"AnimateItem\",\n" +
                "              \"componentId\": \"slide-0\",\n" +
                "              \"value\": [\n" +
                "                {\n" +
                "                  \"property\": \"opacity\",\n" +
                "                  \"from\": 0,\n" +
                "                  \"to\": 1\n" +
                "                }\n" +
                "              ],\n" +
                "              \"duration\": 1000\n" +
                "            }\n" +
                "          ]\n" +
                "        }\n" +
                "      ],\n" +
                "      \"data\": \"[ 1, 2]\",\n" +
                "      \"items\": {\n" +
                "        \"type\": \"Container\",\n" +
                "        \"width\": \"100%\",\n" +
                "        \"id\": \"slide-${index}\"," +
                "        \"bind\": [\n" +
                "          {\n" +
                "            \"name\": \"layoutIndex\",\n" +
                "            \"value\": \"${index}\"\n" +
                "          }\n" +
                "        ],\n" +
                "        \"items\": [\n" +
                "          {\n" +
                "            \"id\": \"slide-${layoutIndex}-a\",\n" +
                "            \"type\": \"Text\",\n" +
                "            \"text\": \"${layoutIndex} Page ${layoutIndex}\",\n" +
                "            \"fontSize\": 200\n," +
                "            \"color\": \"black\"\n" +
                "          }\n" +
                "        ]\n" +
                "      }";


        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(inflate(REQUIRED_PROPERTIES, componentProps))
                .check(hasRootContext());
        APLAbsoluteLayout pager = mTestContext.getTestView();

        mIdlingResource = new APLViewIdlingResource(pager);
        IdlingRegistry.getInstance().register(mIdlingResource);

        // The animation length is 1000 ms, so we need to wait at least that long.
        onView(isRoot()).perform(waitFor(1000));

        View initialChild = pager.getChildAt(0);
        assertEquals(initialChild, pager.getChildAt(0));
        assertEquals(1f, initialChild.getAlpha(), 0.01f);
    }

    @Test
    public void testView_layout_withWrapNavigation_3items() {
        int nItems = 3;
        int nIterations = 2;
        // wrap around pager with 3 children (red, orange, yellow)
        // walk forward thru all 3 children from left to right 2 times
        //     0 -> 1 -> 2 -> 0 -> 1 -> 2 -> 0
        // then walk backward thru all 3 children from right to left 2 times
        //     0 -> 2 -> 1 -> 0 -> 2 -> 1 -> 0
        wrapNavigation(nItems, nIterations);
    }

    @Test
    public void testView_layout_withWrapNavigation_4items() {
        int nItems = 4;
        int nIterations = 2;
        // wrap around pager with 4 children:
        //     red, orange, yellow, green
        // walk forward thru all 4 children from left to right 2 times
        //     0 -> 1 -> 2 -> 3 -> 0 -> 1 -> 2 -> 3 -> 0
        // then walk backward thru all 4 children from right to left 2 times
        //     0 -> 3 -> 2 -> 1 -> 0 -> 3 -> 2 -> 1 -> 0
        wrapNavigation(nItems, nIterations);
    }

    @Test
    public void testView_layout_withWrapNavigation_5items() {
        int nItems = 5;
        int nIterations = 2;
        // wrap around pager with 5 children:
        //     red, orange, yellow, green, blue
        // walk forward thru all 5 children from left to right 2 times
        //     0 -> 1 -> 2 -> 3 -> 4 -> 0 -> 1 -> 2 -> 3 -> 4 -> 0
        // then backward thru all 5 children from right to left 2 times
        //     0 -> 4 -> 3 -> 2 -> 1 -> 0 -> 4 -> 3 -> 2 -> 1 -> 0
        wrapNavigation(nItems, nIterations);
    }

    @Test
    public void testView_layout_withWrapNavigation_12items() {
        int nItems = 12;
        int nIterations = 1;
        // wrap around pager with 12 children:
        //     red, orange, yellow, green, blue, purple, red, orange, yellow, green, blue, purple
        // walk backward thru all 12 children from left to right only 1 time
        //     0 -> 1 -> 2 -> ... -> 10 -> 11 -> 0
        // then backward thru all 12 children from right to left only 1 time
        //     0 -> 11 -> 10 -> ... -> 2 -> 1 -> 0
        wrapNavigation(nItems, nIterations);
    }

    @Test
    @LargeTest
    public void testView_layout_withNoneNavigation() {
        // this is overriding the super layout because not all pager children are on the screen
        // at the same time.
        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(inflate(REQUIRED_PROPERTIES, " \"navigation\": \"none\", " + CHILD_LAYOUT_PROPERTIES))
                .check(hasRootContext());

        Component component = mTestContext.getTestComponent();
        mIdlingResource = new APLViewIdlingResource(mTestContext.getTestView());
        IdlingRegistry.getInstance().register(mIdlingResource);

        Component child = component.getChildAt(0);
        onView(withComponent(child))
                .check(matches(isDisplayed()));

        // Right child is displayed on the view, but invisible
        Component rightChild = component.getChildAt(1);
        assertTrue(rightChild.isInvisibleOverride());
        onView(Matchers.allOf(withComponent(rightChild), withEffectiveVisibility(ViewMatchers.Visibility.INVISIBLE)))
                .check(matches(anything()));
    }

    // TODO: Missing tests for 'forward-only' nav modes

    // TODO: Missing tests for gestures boundaries in each nav modes. For example, in 'forward-only',
    //  make sure that it does not allow to move a previous page.
    // TODO: Missing tests for all nav modes using keys (specially for Fire TV) rather than finger gestures (swipes)

    /**
     * @return The string representation used in the APL document for this component.  For example
     * when testing the Text Component this method should return "Text".
     */
    @Override
    String getComponentType() {
        return "Pager";
    }

    /**
     * Test the view after properties have been assigned.
     *
     * @param view The Component View for testing.
     */
    @Override
    void testView_applyProperties(APLAbsoluteLayout view) {

    }

    // Verify page Views are only inflated after instantiateItem is called for them and that
    // ensureLayout is called on the page Components
    @Test
    public void testPager_PagesInflatedLazily() {
        final String items =
                " \"items\": [\n" +
                        "        {\n" +
                        "          \"type\": \"Text\",\n" +
                        "          \"text\": \"Text1\"\n" +
                        "        },\n" +
                        "        {\n" +
                        "          \"type\": \"Text\",\n" +
                        "          \"text\": \"Text2\"\n" +
                        "        },\n" +
                        "        {\n" +
                        "          \"type\": \"Text\",\n" +
                        "          \"text\": \"Text3\"\n" +
                        "        },\n" +
                        "        {\n" +
                        "          \"type\": \"Text\",\n" +
                        "          \"text\": \"Text4\"\n" +
                        "        },\n" +
                        "        {\n" +
                        "          \"type\": \"Text\",\n" +
                        "          \"text\": \"Text5\"\n" +
                        "        },\n" +
                        "        {\n" +
                        "          \"type\": \"Text\",\n" +
                        "          \"text\": \"Text6\"\n" +
                        "        },\n" +
                        "        {\n" +
                        "          \"type\": \"Text\",\n" +
                        "          \"text\": \"Text7\"\n" +
                        "        },\n" +
                        "        {\n" +
                        "          \"type\": \"Text\",\n" +
                        "          \"text\": \"Text8\"\n" +
                        "        }\n" +
                        "      ]";
        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(inflate(REQUIRED_PROPERTIES, items))
                .check(hasRootContext());

        Component component = mTestContext.getTestComponent();
        mIdlingResource = new APLViewIdlingResource(mTestContext.getTestView());
        IdlingRegistry.getInstance().register(mIdlingResource);
        final IAPLViewPresenter presenter = mTestContext.getPresenter();

        // Verify that Component for a page that hasn't been instantiated hasn't had ensureLayout called
        final Component pagerChild = component.getChildAt(5); // go to a page which wont be instantiated yet, last page might because its adjacent to first
        // assertEquals(0, offscreenPage.getBounds().intHeight()); TODO: once Core lazily lays out Pager children uncomment this assert

        // Verify that the View hierarchy for the Component hasn't been inflated yet
        assertNull(presenter.findView(pagerChild));

        // go the last page to trigger instantiateItem call
        // TODO: we should espresso contrib library to swipe to pager page
        for(int i = 0; i < 5; i++) {
            Component child = component.getChildAt(i);
            // swipe to the next view after verifying previous
            onView(withComponent(child))
                    .perform(swipeLeft());
        }

        // Now verify ensureLayout has been called on the Component for the page  we've instantiated by swiping to it
        assertEquals(component.getBounds().intHeight(), pagerChild.getBounds().intHeight()); // we can verify ensureLayout by checking the height, which should match the Pager height

        // Verify the View for the page was inflated and bound to the Component
        final View pagerChildView = getTestView().getChildAt(0); // pager maintains 1 offscreen page on each side of the current one, so the "in view" child is at child-index 1
        assertEquals(presenter.findView(pagerChild), pagerChildView);

        // Verify the Component properties were applied to the page View
        Assert.assertEquals("Text6", ((APLTextView) pagerChildView).getLayout().getText().toString());
    }
}
