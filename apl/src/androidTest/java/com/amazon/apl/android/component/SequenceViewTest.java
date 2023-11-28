/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.component;

import android.graphics.Color;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.view.View;

import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.IdlingResource;
import androidx.test.espresso.ViewAction;
import androidx.test.platform.app.InstrumentationRegistry;

import com.amazon.apl.android.APLGradientDrawable;
import com.amazon.apl.android.Component;
import com.amazon.apl.android.MultiChildComponent;
import com.amazon.apl.android.Text;
import com.amazon.apl.android.espresso.APLViewIdlingResource;
import com.amazon.apl.android.views.APLAbsoluteLayout;
import com.amazon.apl.android.views.APLTextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.swipeDown;
import static androidx.test.espresso.action.ViewActions.swipeUp;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static com.amazon.apl.android.espresso.APLViewActions.executeCommands;
import static com.amazon.apl.android.espresso.APLViewActions.waitFor;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.List;

public class SequenceViewTest extends AbstractComponentViewTest<APLAbsoluteLayout, MultiChildComponent> {
    @Before
    public void doBefore() {
        REQUIRED_PROPERTIES = "\"items\": [" +
                "{\"type\": \"Text\",\"id\": \"text1\",\"text\": \"Text 1\"}," +
                "{\"type\": \"Text\",\"id\": \"text2\",\"text\": \"Text 2\"}]";
        OPTIONAL_PROPERTIES = "";
        CHILD_LAYOUT_PROPERTIES = "\"items\": [" +
                "{\"type\": \"Text\",\"id\": \"text1\",\"text\": \"Text 1\"}," +
                "{\"type\": \"Text\",\"id\": \"text2\",\"text\": \"Text 2\"}]";
    }

    @Override
    String getComponentType() {
        return "Sequence";
    }

    @Override
    Class<APLAbsoluteLayout> getViewClass() {
        return APLAbsoluteLayout.class;
    }

    @Override
    void testView_applyProperties(APLAbsoluteLayout view) {

    }

    private IdlingResource mIdlingResource;


    private ShapeDrawable getBorder(View view) {
        return (ShapeDrawable) ((LayerDrawable) view.getBackground()).getDrawable(0);
    }


    @After
    public void tearDown() {
        if (mIdlingResource != null) {
            IdlingRegistry.getInstance().unregister(mIdlingResource);
        }
    }

    @Test
    public void testView_VerticalLayout() {
        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(inflate(REQUIRED_PROPERTIES, ""))
                .check(hasRootContext());
        Component component = mTestContext.getTestComponent();
        for (int i = 0; i < component.getChildCount(); i++) {
            Component child = component.getChildAt(i);
            onView(withComponent(child))
                    .check(matches(isDisplayed()));

            // swipe to the next view after verifying previous
            onView(withComponent(child))
                    .perform(swipeUp());
        }
        APLAbsoluteLayout view = mTestContext.getTestView();
        assertEquals(2, view.getChildCount());
    }

    @Test
    public void testView_HorizontalLayout_scrollToEnd() {
        // children with different widths can scroll to end
        String componentProperties = "\"items\": [" +
                "  { \"type\": \"Frame\", \"height\": \"100vh\", \"width\": \"10vw\", \"backgroundColor\": \"red\" }," +
                "  { \"type\": \"Frame\", \"height\": \"100vh\", \"width\": \"50vw\", \"backgroundColor\": \"orange\" }," +
                "  { \"type\": \"Frame\", \"height\": \"100vh\", \"width\": \"50vw\", \"backgroundColor\": \"yellow\" }," +
                "  { \"type\": \"Frame\", \"height\": \"100vh\", \"width\": \"50vw\", \"backgroundColor\": \"green\" }," +
                "  { \"type\": \"Frame\", \"height\": \"100vh\", \"width\": \"10vw\", \"backgroundColor\": \"blue\" }" +
                "]";
        String optionalProperties = "\"height\": \"100%\", \"width\": \"100%\"," +
                "\"scrollDirection\": \"horizontal\"," +
                "\"onMount\": {" +
                "  \"type\": \"Sequential\", \"repeatCount\": 1, \"commands\": [" +
                "    { \"type\": \"Scroll\", \"componentId\": \"testcomp\", \"distance\": 0.5 }" +
                "  ]" +
                "}";
        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(inflate(componentProperties, optionalProperties))
                .check(hasRootContext());
        mIdlingResource = new APLViewIdlingResource(getTestView());
        IdlingRegistry.getInstance().register(mIdlingResource);
        onView(isRoot())
                .perform(waitFor(100));
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        MultiChildComponent component = mTestContext.getTestComponent();
        assertEquals(5, component.getChildCount());
        Component child = component.getChildAt(component.getChildCount()-1);
        onView(withComponent(child))
                .check(matches(isDisplayed()));
    }

    /**
     * Test that APLMultiChildComponentAdapter.onBind works correctly (i.e. ViewHolders are bound to the
     * correct Components and Component properties are applied to the views).
     */
    @Test
    public void testAPLMultiChildComponentAdapter_onBind() {
        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(inflate(REQUIRED_PROPERTIES, ""))
                .check(hasRootContext());
        APLAbsoluteLayout view = mTestContext.getTestView();
        // 1st View in list is bound to 1st Component in items
        Assert.assertEquals("Text 1", ((APLTextView) view.getChildAt(0)).getLayout().getText().toString());
        // 2nd View in list is bound to 2nd Component in items
        assertEquals("Text 2", ((APLTextView) view.getChildAt(1)).getLayout().getText().toString());
    }

    /**
     * Verify that onBind works when we have items with different ViewTypes (hierarchy signatures)
     */
    @Test
    public void testAPLMultiChildComponentAdapter_OnBind_MultipleViewTypes() {
        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(inflate("\"items\": [" +
                                "{\"type\": \"Text\",\"id\": \"text1\",\"text\": \"Text 1\"}," +
                                "{ \"type\": \"Frame\", \"height\": \"100vh\", \"width\": \"10vw\", \"borderColor\": \"blue\" }" +
                                "]",
                        ""))
                .check(hasRootContext());
        APLAbsoluteLayout view = mTestContext.getTestView();
        // 1st View in list is bound to Text view holder
        assertEquals("Text 1", ((APLTextView) view.getChildAt(0)).getLayout().getText().toString());
        // 2nd View in list is bound to the Frame view holder
        assertEquals(Color.BLUE, getBorder(view.getChildAt(1)).getPaint().getColor());
    }

    /**
     * Verify that onBind works with children that have nested Components
     */
    @Test
    public void testAPLMultiChildComponentAdapter_OnBind_ChildHasMultipleViews() {
        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(inflate("\"items\": [" +
                                "{ \"type\": \"Frame\", " +
                                "\"height\": \"100vh\"," +
                                "\"width\": \"10vw\"," +
                                "\"borderColor\": \"red\"," +
                                "\"item\": {\"type\": \"Text\",\"id\": \"text1\",\"text\": \"nested text\"}" +
                                "}" +
                                "]",
                        ""))
                .check(hasRootContext());
        APLAbsoluteLayout view = mTestContext.getTestView();

        // verify frame is bound
        final APLAbsoluteLayout frameView = (APLAbsoluteLayout) view.getChildAt(0);
        assertEquals(Color.RED, getBorder(view.getChildAt(0)).getPaint().getColor());

        // verify child of frame is bound
        assertEquals("nested text", ((APLTextView ) frameView.getChildAt(0)).getLayout().getText().toString());
    }

    /**
     * verify ensureLayout gets called for Components on screen and not called for Components far away
     */
    @Test
    public void testEnsureLayoutCalledOnChildrenLazily() throws JSONException {
        // Create test list items
        final JSONArray itemsArray = new JSONArray();
        final int numChildren = 100;
        for(int i = 0; i < numChildren; i++) {
            JSONObject item = new JSONObject();
            item.put("type", "Frame");
            if (i % 2 == 0) {
                item.put("borderColor", "blue");
            } else {
                item.put("borderColor", "red");
            }
            item.put("borderWidth", 5);
            item.put("height", "100dp");
            item.put("width", "100vw");
            itemsArray.put(item);
        }
        final JSONObject requiredProperties = new JSONObject();
        requiredProperties.put("items", itemsArray);

        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(inflate("\"items\":" + itemsArray.toString()))
                .check(hasRootContext());
        mIdlingResource = new APLViewIdlingResource(getTestView());
        IdlingRegistry.getInstance().register(mIdlingResource);

        final MultiChildComponent sequence = getTestComponent();

        // check first item has had ensureLayout called, we verify this by checking that height is set
        Component firstItem = sequence.getChildAt(0);
        assertEquals(100, firstItem.getBounds().intHeight());

        // check last item has NOT had ensureLayout called on it, as a result the height will be zero
        Component lastItem = sequence.getChildAt(numChildren - 1);
        assertEquals(0, lastItem.getBounds().intHeight());

        // Swipe to the last item.
        for(int i = 0; i < 20; i++) {
            onView(withId(sequence.getComponentId().hashCode())).perform(swipeUp());
        }

        // Now that the last item is made visible, verify ensureLayout has been called on it
        assertEquals(100, lastItem.getBounds().intHeight());
    }

    @Test
    public void testAPLMultiChildComponentAdapter_OnBindWithRecycledViews() throws JSONException {
        // Create test list items
        final JSONArray itemsArray = new JSONArray();
        final int numChildren = 100;
        for (int i = 0; i < numChildren; i++) {
            JSONObject item = new JSONObject();
            item.put("type", "Frame");
            item.put("height", "100dp");
            item.put("width", "100vw");
            item.put("borderWidth", "10dp");
            item.put("borderColor", i == 0 ? "red" : "blue"); // make the first one red to check binding of a recycled view
            itemsArray.put(item);
        }
        final JSONObject requiredProperties = new JSONObject();
        requiredProperties.put("items", itemsArray);

        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(inflate("\"items\":" + itemsArray.toString()))
                .check(hasRootContext());
        mIdlingResource = new APLViewIdlingResource(getTestView());
        IdlingRegistry.getInstance().register(mIdlingResource);

        final APLAbsoluteLayout view = mTestContext.getTestView();

        // Verify initial onBind to first item
        assertEquals(Color.RED, getBorder(view.getChildAt(0)).getPaint().getColor());

        // Swipe to the last item.
        ViewAction[] swipeUps = new ViewAction[30];
        for (int i = 0; i < swipeUps.length; i++) {
            swipeUps[i] = swipeUp();
        }
        final MultiChildComponent sequence = getTestComponent();
        onView(withId(sequence.getComponentId().hashCode())).perform(swipeUps);
        // Now swipe back to the first item
        ViewAction[] swipeDowns = new ViewAction[60];
        for (int i = 0; i < swipeDowns.length; i++) {
            swipeDowns[i] = swipeDown();
        }
        onView(withId(sequence.getComponentId().hashCode())).perform(swipeDowns);

        // Verify the first item has had its view re-bound correctly:
        assertEquals(Color.RED, getBorder(view.getChildAt(0)).getPaint().getColor());
    }

    @Test
    public void testView_VerticalLayout_NoSpacing() {
        String componentProps =
                "\"width\": \"auto\"," +
                        "\"data\": [1,2,3,4]," +
                        "\"items\": [{" +
                        "\"type\": \"Frame\"," +
                        "\"backgroundColor\": \"red\"," +
                        "\"height\": \"100\"," +
                        "\"width\": \"100\"" +
                        "}]";

        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(inflate(componentProps))
                .check(hasRootContext());

        APLAbsoluteLayout view = mTestContext.getTestView();

        for (int i = 0; i < 4; i++) {
            View childView = view.getChildAt(i);

            onView(withId(is(childView.getId())))
                    .check(matches(isDisplayed()));

            assertEquals((100) * i, childView.getTop()); // Only height of the child. Zero as spacing.
            assertEquals(0, childView.getLeft());
        }
    }

    @Test
    public void testView_VerticalLayout_UsesSpacing() {
        String componentProps =
                "\"width\": \"auto\"," +
                        "\"scrollDirection\": \"vertical\"," +
                        "\"data\": [1,2,3,4]," +
                        "\"items\": [{" +
                        "\"type\": \"Frame\"," +
                        "\"backgroundColor\": \"red\"," +
                        "\"height\": \"100\"," +
                        "\"width\": \"100\"," +
                        "\"spacing\": \"20dp\"" +
                        "}]";

        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(inflate(componentProps))
                .check(hasRootContext());

        APLAbsoluteLayout view = mTestContext.getTestView();

        // First child should ignore spacing
        // Remaining items should contains corresponding spacing
        for (int i = 0; i < 4; i++) {
            View childView = view.getChildAt(i);

            onView(withId(is(childView.getId())))
                    .check(matches(isDisplayed()));

            assertEquals((20 + 100) * i, childView.getTop()); // spacing + height of the child. Expects: 0, 120, 240, 360
            assertEquals(0, childView.getLeft());
        }
    }

    @Test
    public void testView_HorizontalLayout_UsesSpacing() {
        String componentProps =
                "\"width\": \"auto\"," +
                        "\"scrollDirection\": \"horizontal\"," +
                        "\"data\": [1,2,3,4]," +
                        "\"items\": [{" +
                        "\"type\": \"Frame\"," +
                        "\"backgroundColor\": \"red\"," +
                        "\"height\": \"100\"," +
                        "\"width\": \"100\"," +
                        "\"spacing\": \"20dp\"" +
                        "}]";

        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(inflate(componentProps))
                .check(hasRootContext());

        APLAbsoluteLayout view = mTestContext.getTestView();

        // First child should ignore spacing
        // Remaining items should contains corresponding spacing
        for (int i = 0; i < 4; i++) {
            View childView = view.getChildAt(i);

            onView(withId(is(childView.getId())))
                    .check(matches(isDisplayed()));

            assertEquals(0, childView.getTop());
            assertEquals((20 + 100) * i, childView.getLeft()); // spacing + height. Expects: 0, 120, 240, 360
        }
    }

    /**
     * Verify that an update sent to a MultiChildComponent is properly reflected in the corresponding Android View
     */
    @Test
    public void testAPLMultiChildComponentAdapter_UpdateChild() throws JSONException {
        // Create test list items
        final JSONArray itemsArray = new JSONArray();
        final int numChildren = 100;
        for (int i = 0; i < numChildren; i++) {
            JSONObject item = new JSONObject();
            item.put("id", "frame" + i);
            item.put("type", "Frame");
            item.put("height", "100dp");
            item.put("width", "100vw");
            item.put("borderWidth", "10dp");
            item.put("borderColor", "blue");
            itemsArray.put(item);
        }
        final JSONObject requiredProperties = new JSONObject();
        requiredProperties.put("items", itemsArray);

        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(inflate("\"items\":" + itemsArray.toString()))
                .check(hasRootContext());

        final APLAbsoluteLayout view = mTestContext.getTestView();

        // Verify initial onBind to first item
        assertEquals(10, getBorder(view.getChildAt(0)).getPaint().getStrokeWidth(), 0);

        //  Update a dynamic property  of the first item
        final String setValueCommand = "[{\n" +
                "  \"type\": \"SetValue\",\n" +
                "  \"componentId\": \"frame0\",\n" +
                "  \"property\": \"borderColor\",\n" +
                "  \"value\": \"red\"\n" +
                "}]\n";
        onView(isRoot()).perform(executeCommands(mTestContext.getRootContext(), setValueCommand));

        // Verify update has been applied
        assertEquals(Color.RED, getBorder(view.getChildAt(0)).getPaint().getColor());

        // Verify no update applied to child that had the same view type
        assertEquals(Color.BLUE, getBorder(view.getChildAt(1)).getPaint().getColor());
    }

    @Test
    public void testAPLSequence_ClearsChildren() throws JSONException, InterruptedException {
        // Create sequence item.
        final JSONArray itemsArray = new JSONArray();
        JSONObject item = new JSONObject();
        item.put("id", "text1");
        item.put("type", "Text");
        item.put("text", "hello");
        item.put("height", "50");
        item.put("backgroundColor", "red");
        itemsArray.put(item);

        item = new JSONObject();
        item.put("id", "text2");
        item.put("type", "Text");
        item.put("text", "hello");
        item.put("height", "50");
        item.put("backgroundColor", "red");
        itemsArray.put(item);

        final JSONObject sequenceObject = new JSONObject();
        sequenceObject.put("id", "seq");
        sequenceObject.put("type", "Sequence");
        sequenceObject.put("scrollDirection", "horizontal");
        sequenceObject.put("items", itemsArray);

        item = new JSONObject();
        item.put("id", "text3");
        item.put("type", "Text");
        item.put("text", "hello");
        item.put("height", "50");
        item.put("backgroundColor", "red");

        final JSONArray containerArray = new JSONArray();
        containerArray.put(sequenceObject);
        containerArray.put(item);

        final JSONObject containerObject = new JSONObject();
        containerObject.put("id", "container");
        containerObject.put("type", "Container");
        containerObject.put("height", "30");
        containerObject.put("items", containerArray);

        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(inflateWithOptions("1.9", "Container", null,
                        "\"item\": " + containerObject.toString()))
                .check(hasRootContext());

        // The above was the minimal document needed to reproduce the behaviour this test is for.

        mIdlingResource = new APLViewIdlingResource(getTestView());
        IdlingRegistry.getInstance().register(mIdlingResource);

        final Component component = mTestContext.getTestComponent();
        final Component sequenceComponent = component.getChildAt(0).getChildAt(0);
        final List<Component> children = sequenceComponent.getAllChildren();

        onView(withComponent(component))
                .check(matches(isDisplayed()));

        onView(withComponent(children.get(0)))
                .check(matches(isDisplayed()));

        assertNotNull(mTestContext.getPresenter().findView(children.get(0)));

        final String setValueCommand = "[{\n" +
                "  \"type\": \"SetValue\",\n" +
                "  \"componentId\": \"%s\",\n" +
                "  \"property\": \"height\",\n" +
                "  \"value\": \"0\"\n" +
                "}]\n";

        // We first hide one of the sequence children. This causes the sequence to call onDisplayedChildrenChanged.
        // We need onDisplayedChildrenChanged to be called for the sequence ONLY and not the parent container, so we still need a visible child.
        // The shrunken view will become detached which is the trigger for the bug.
        onView(isRoot()).perform(executeCommands(mTestContext.getRootContext(), String.format(setValueCommand, "text1")));

        onView(withComponent(children.get(1)))
                .check(matches(isDisplayed()));

        // Verify that the detached view still exists.
        assertNotNull(mTestContext.getPresenter().findView(children.get(0)));


        // This should shrink the container and also remove any other views *except* the detached view, this is the bug.
        onView(isRoot()).perform(executeCommands(mTestContext.getRootContext(), String.format(setValueCommand, "container")));

        // There shouldn't be any sequence views.
        onView(withComponent(sequenceComponent)).check(doesNotExist());
        assertNull(mTestContext.getPresenter().findView(sequenceComponent));

        for (int i = 0; i < children.size(); i++) {
            // None of the children should have views either including the detached one.
            assertNull(mTestContext.getPresenter().findView(children.get(i)));
        }
    }

    /**
     * Verify that an update sent to a MultiChildComponent child Component is properly reflected in the corresponding Android View
     * when the child view hasn't yet been created or bound by the RecyclerView
     */
    @Test
    public void testAPLMultiChildComponentAdapter_UpdateUnboundChild() throws JSONException {
        // Create test list items
        final JSONArray itemsArray = new JSONArray();
        final int numChildren = 50;
        for (int i = 0; i < numChildren; i++) {
            JSONObject item = new JSONObject();
            item.put("id", "frame" + i);
            item.put("type", "Frame");
            item.put("height", "100dp");
            item.put("width", "100vw");
            item.put("borderWidth", "10dp");
            item.put("borderColor", "blue");
            itemsArray.put(item);
        }
        final JSONObject requiredProperties = new JSONObject();
        requiredProperties.put("items", itemsArray);

        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(inflate("\"items\":" + itemsArray.toString()))
                .check(hasRootContext());

        final APLAbsoluteLayout view = mTestContext.getTestView();

        //  Update a dynamic property of last item, whose View has not yet been created or bound
        final String setValueCommand = "[{\n" +
                "  \"type\": \"SetValue\",\n" +
                "  \"componentId\": \"frame49\",\n" +
                "  \"property\": \"borderColor\",\n" +
                "  \"value\": \"red\"\n" +
                "}]\n";
        onView(isRoot()).perform(executeCommands(mTestContext.getRootContext(), setValueCommand));

        // Swipe to the last item.
        ViewAction[] swipeUps = new ViewAction[20];
        for (int i = 0; i < swipeUps.length; i++) {
            swipeUps[i] = swipeUp();
        }
        final MultiChildComponent sequence = getTestComponent();
        onView(withId(sequence.getComponentId().hashCode())).perform(swipeUps);

        // Verify the update has been applied to last item
        assertEquals(Color.RED, getBorder(view.getChildAt(view.getChildCount() - 1)).getPaint().getColor());

        // Swipe back to first item
        ViewAction[] swipeDowns = new ViewAction[25];
        for (int i = 0; i < swipeDowns.length; i++) {
            swipeDowns[i] = swipeDown();
        }
        onView(withId(sequence.getComponentId().hashCode())).perform(swipeDowns);

        // Verify no update applied to first child, which has same View Type / template Component
        assertEquals(Color.BLUE, getBorder(view.getChildAt(0)).getPaint().getColor());
    }
}
