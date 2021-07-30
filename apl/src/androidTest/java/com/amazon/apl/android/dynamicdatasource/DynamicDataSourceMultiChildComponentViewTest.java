/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.dynamicdatasource;

import android.graphics.Color;
import android.view.View;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.amazon.apl.android.APLController;
import com.amazon.apl.android.APLGradientDrawable;
import com.amazon.apl.android.Component;
import com.amazon.apl.android.MultiChildComponent;
import com.amazon.apl.android.espresso.APLMatchers;
import com.amazon.apl.android.views.APLAbsoluteLayout;
import com.amazon.apl.android.views.APLTextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Map;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.actionWithAssertions;
import static androidx.test.espresso.action.ViewActions.longClick;
import static androidx.test.espresso.assertion.PositionAssertions.isCompletelyBelow;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static com.amazon.apl.android.espresso.APLViewActions.waitFor;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(AndroidJUnit4.class)
public class DynamicDataSourceMultiChildComponentViewTest extends AbstractDynamicDataSourceComponentViewTest {

    private static final String DOC = "{\n" +
            "  \"type\": \"APL\",\n" +
            "  \"version\": \"1.1\",\n" +
            "  \"theme\": \"light\",\n" +
            "  \"layouts\": {\n" +
            "    \"square\": {\n" +
            "      \"parameters\": [\"color\", \"text\"],\n" +
            "      \"item\": {\n" +
            "        \"type\": \"Frame\",\n" +
            "        \"width\": \"100vw\",\n" +
            "        \"height\": \"100dp\",\n" +
            "        \"id\": \"frame-${text}\",\n" +
            "        \"borderColor\": \"${color}\",\n" +
            "        \"borderWidth\": \"8dp\",\n" +
            "        \"item\": {\n" +
            "          \"type\": \"Text\",\n" +
            "          \"text\": \"${text}\",\n" +
            "          \"color\": \"black\",\n" +
            "          \"height\": 50\n" +
            "        }\n" +
            "      }\n" +
            "    }\n" +
            "  },\n" +
            "  \"mainTemplate\": {\n" +
            "    \"parameters\": [\n" +
            "      \"dynamicSource\"\n" +
            "    ],\n" +
            "    \"item\": {\n" +
            "      \"type\": \"Container\",\n" +
            "      \"id\": \"container\",\n" +
            "      \"width\":\"100vw\",\n" +
            "      \"height\":\"100vh\",\n" +
            "      \"data\": \"${dynamicSource}\",\n" +
            "      \"items\": [\n" +
            "        {\n" +
            "          \"type\": \"square\",\n" +
            "          \"color\": \"${data.color}\",\n" +
            "          \"text\": \"${data.text}\"\n" +
            "        }\n" +
            "      ]\n" +
            "    }\n" +
            "  }\n" +
            "}";

    private static final int START_INDEX = 10;

    private static final String DATA = String.format("{\n" +
            "  \"type\": \"dynamicIndexList\",\n" +
            "  \"listId\": \"vQdpOESlok\",\n" +
            "  \"startIndex\": %d,\n" +
            "    \"items\": [\n" +
            "      { \"color\": \"blue\", \"text\": \"10\" }\n" +
            "    ]\n" +
            "  }", START_INDEX);

    private static final String PAGE_TOKEN = "pageToken";

    private static final String FORWARD_PAGE_TOKEN = "nextPageToken";

    private static final String DATA_TOKEN = String.format("{\n" +
            "  \"type\": \"dynamicTokenList\",\n" +
            "  \"listId\": \"vQdpOESlok\",\n" +
            "  \"pageToken\": \"%s\",\n" +
            "  \"forwardPageToken\": \"%s\",\n" +
            "  \"items\": [\n" +
            "      { \"color\": \"blue\", \"text\": \"1\" }\n" +
            "  ]\n" +
            "}", PAGE_TOKEN, FORWARD_PAGE_TOKEN);

    /**
     * Verify new child Views are rendered after a TokenListDataSource update which inserts new Components to the
     * Container
     */
    @Test
    @LargeTest
    public void testTokenDataSourceUpdate_TestRendered() {
        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(actionWithAssertions(inflate(DOC, DATA_TOKEN, new TestTokenBasedDataSourceFetchCallback() {
                    @Override
                    JSONArray getItems(String pageToken) {
                        // do nothing, no lazy loading
                        return null;
                    }
                    @Override
                    String getNextPageToken(String pageToken) {
                        // do nothing, no lazy loading
                        return null;
                    }
                    @Override
                    APLController getAplController() {
                        return null;
                    }
                })))
                .check(hasRootContext());

        final Component container =  mTestContext.getRootContext().findComponentById("container");
        final APLAbsoluteLayout absoluteLayout = (APLAbsoluteLayout) mTestContext.getPresenter().findView(container);

        // Verify initial item is loaded
        assertEquals(container.getChildCount(), 1);
        assertEquals(absoluteLayout.getChildCount(), 1);
        onView(APLMatchers.withText("1")).check(matches(isDisplayed())); //  Text 10 is the initial item

        // update payload
        String updatePayload = String.format("{\n" +
                "  \"listId\": \"vQdpOESlok\",\n" +
                "  \"pageToken\": \"%s\",\n" +
                "  \"nextPageToken\": \"nextPageToken2\",\n" +
                "  \"items\": [\n" +
                "    { \"color\": \"blue\", \"text\": \"2\" },\n" +
                "    { \"color\": \"red\", \"text\": \"3\" },\n" +
                "    { \"color\": \"green\", \"text\": \"4\" },\n" +
                "    { \"color\": \"yellow\", \"text\": \"5\" },\n" +
                "    { \"color\": \"white\", \"text\": \"6\" }\n" +
                "  ]\n" +
                "}", FORWARD_PAGE_TOKEN);
        assertTrue(mAplController.updateDataSource("dynamicTokenList", updatePayload));
        onView(withId(container.getComponentId().hashCode())).perform(longClick());

        // Verify new items 2-6 are loaded
        assertEquals(6, container.getChildCount()); // 1 initial + 5 new ones
        assertEquals(6, absoluteLayout.getChildCount());

        // Verify the new item Views are rendered
        onView(APLMatchers.withText("2")).check(matches(isDisplayed()));
        assertEquals(absoluteLayout.getChildAt(1).getWidth(), container.getChildAt(1).getBounds().intWidth());

        onView(APLMatchers.withText("3")).check(matches(isDisplayed()));
        assertEquals(absoluteLayout.getChildAt(2).getWidth(), container.getChildAt(2).getBounds().intWidth());

        onView(APLMatchers.withText("4")).check(matches(isDisplayed()));
        assertEquals(absoluteLayout.getChildAt(3).getWidth(), container.getChildAt(3).getBounds().intWidth());

        onView(APLMatchers.withText("5")).check(matches(isDisplayed()));
        assertEquals(absoluteLayout.getChildAt(4).getWidth(), container.getChildAt(4).getBounds().intWidth());

        onView(APLMatchers.withText("6")).check(matches(isDisplayed()));
        assertEquals(absoluteLayout.getChildAt(5).getWidth(), container.getChildAt(5).getBounds().intWidth());
    }

    /**
     * Verify new child Views are rendered after a DataSource update which inserts new Components to the
     * Container
     */
    @Test
    @LargeTest
    public void testDataSourceUpdate_ViewsRendered() {
        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(actionWithAssertions(inflate(DOC, DATA, new TestIndexBasedDataSourceFetchCallback() {
                    @Override
                    Map<String, Object> getItem(int index) {
                        return null;
                    }

                    @Override
                    int getItemCount() {
                        return 0;
                    }

                    @Override
                    APLController getAplController() {
                        return null;
                    }
                })))
                .check(hasRootContext());

        final MultiChildComponent multiChildComponent = (MultiChildComponent) mTestContext.getRootContext().findComponentById("container");
        final APLAbsoluteLayout absoluteLayout = (APLAbsoluteLayout) mTestContext.getPresenter().findView(multiChildComponent);

        // Verify initial item is loaded
        assertEquals(multiChildComponent.getChildCount(), 1);
        assertEquals(absoluteLayout.getChildCount(), 1);
        onView(APLMatchers.withText("10")).check(matches(isDisplayed())); //  Text 10 is the initial item

        // Send an update for items 5-9, before the initial startIndex:
        String updatePayload = "{\n" +
                "  \"listId\": \"vQdpOESlok\",\n" +
                "  \"startIndex\": 5,\n" +
                "  \"items\": [\n" +
                "    { \"color\": \"blue\", \"text\": \"5\" },\n" +
                "    { \"color\": \"red\", \"text\": \"6\" },\n" +
                "    { \"color\": \"green\", \"text\": \"7\" },\n" +
                "    { \"color\": \"yellow\", \"text\": \"8\" },\n" +
                "    { \"color\": \"white\", \"text\": \"9\" }\n" +
                "  ]\n" +
                "}";
        assertTrue(mAplController.updateDataSource("dynamicIndexList", updatePayload));
        onView(withId(multiChildComponent.getComponentId().hashCode())).perform(longClick());

        // Verify new items 5-9 are loaded
        assertEquals(6, multiChildComponent.getChildCount()); // 1 initial + 5 new ones
        assertEquals(6, absoluteLayout.getChildCount());

        // Verify the new item Views are rendered
        onView(APLMatchers.withText("5")).check(matches(isDisplayed()));
        assertEquals(absoluteLayout.getChildAt(1).getWidth(), multiChildComponent.getChildAt(1).getBounds().intWidth());

        onView(APLMatchers.withText("6")).check(matches(isDisplayed()));
        assertEquals(absoluteLayout.getChildAt(2).getWidth(), multiChildComponent.getChildAt(2).getBounds().intWidth());

        onView(APLMatchers.withText("7")).check(matches(isDisplayed()));
        assertEquals(absoluteLayout.getChildAt(3).getWidth(), multiChildComponent.getChildAt(3).getBounds().intWidth());

        onView(APLMatchers.withText("8")).check(matches(isDisplayed()));
        assertEquals(absoluteLayout.getChildAt(4).getWidth(), multiChildComponent.getChildAt(4).getBounds().intWidth());

        onView(APLMatchers.withText("9")).check(matches(isDisplayed()));
        assertEquals(absoluteLayout.getChildAt(5).getWidth(), multiChildComponent.getChildAt(5).getBounds().intWidth());

        // Send an update for item 11, after the initial startIndex
        updatePayload = "{\n" +
                "  \"listId\": \"vQdpOESlok\",\n" +
                "  \"startIndex\": 11,\n" +
                "  \"items\": [\n" +
                "    { \"color\": \"purple\", \"text\": \"11\" }\n" +
                "  ]\n" +
                "}";
        assertTrue(mAplController.updateDataSource("dynamicIndexList", updatePayload));
        onView(withId(multiChildComponent.getComponentId().hashCode())).perform(longClick());

        // Verify item 11 loaded
        assertEquals(7, multiChildComponent.getChildCount()); // added 1 item to the previous 6
        assertEquals(7, absoluteLayout.getChildCount());

        // Verify the new item View is rendered
        onView(APLMatchers.withText("9")).check(matches(isDisplayed()));
        assertEquals(absoluteLayout.getChildAt(6).getWidth(), multiChildComponent.getChildAt(6).getBounds().intWidth());
    }

    /**
     * Test updating a Container with InsertItem operation
     */
    @Test
    @LargeTest
    public void testUpdate_InsertItem() {
        final String data = "{\n" +
                "  \"type\": \"dynamicIndexList\",\n" +
                "  \"listId\": \"vQdpOESlok\",\n" +
                "  \"startIndex\": 0,\n" +
                "  \"listVersion\": 0,\n" +
                "  \"items\": [\n" +
                "      { \"color\": \"blue\", \"text\": \"100\" },\n" +
                "      { \"color\": \"blue\", \"text\": \"102\" }\n" +
                "    ]\n" +
                "  }";

        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(actionWithAssertions(inflate(DOC, data, (type, payload) -> {
                    // do nothing, not lazy loading
                })))
                .check(hasRootContext());

        final MultiChildComponent multiChildComponent = (MultiChildComponent) mTestContext.getRootContext().findComponentById("container");
        final APLAbsoluteLayout absoluteLayout = (APLAbsoluteLayout) mTestContext.getPresenter().findView(multiChildComponent);

        // Verify initial items are present
        assertEquals(2, absoluteLayout.getChildCount());

        // Send InsertItem update
        final String updatePayload = "{\n" +
                "    \"listId\": \"vQdpOESlok\",\n" +
                "    \"listVersion\": 1,\n" +
                "    \"operations\": [\n" +
                "      {\n" +
                "        \"type\": \"InsertItem\",\n" +
                "        \"index\": 1,\n" +
                "        \"item\": {\n" +
                "           \"color\": \"red\", " +
                "           \"text\": \"101\"\n" +
                "        }\n" +
                "      }\n" +
                "    ]\n" +
                "  }";
        assertTrue(mAplController.updateDataSource("dynamicIndexList", updatePayload));
        onView(withId(multiChildComponent.getComponentId().hashCode())).perform(longClick());

        // Verify new item has been added to Container
        assertEquals(3, absoluteLayout.getChildCount());

        // Verify new item Views have been rendered
        onView(APLMatchers.withText("101")).check(matches(isDisplayed()));
        onView(withBorderColor(Color.RED)).check(matches(isDisplayed()));

        // Verify the new item is in the expected position on the screen
        onView(APLMatchers.withText("101")).check(isCompletelyBelow(APLMatchers.withText("100")));
        // Verify the new item is in the expected position in the ViewGroup
        assertSquare(absoluteLayout.getChildAt(1), Color.RED, "101");

        // Verify the item that was previously in the position we inserted at is now shifted down
        onView(APLMatchers.withText("102")).check(isCompletelyBelow(APLMatchers.withText("101")));
        // Verify the item that was previously in the position we inserted is in expected position in the ViewGroup
        assertSquare(absoluteLayout.getChildAt(2), Color.BLUE, "102");
    }

    /**
     * Test updating a Container with InsertMultipleItems operation
     */
    @Test
    @LargeTest
    public void testUpdate_InsertMultipleItems() {
        final String data = "{\n" +
                "  \"type\": \"dynamicIndexList\",\n" +
                "  \"listId\": \"vQdpOESlok\",\n" +
                "  \"startIndex\": 0,\n" +
                "  \"listVersion\": 0,\n" +
                "  \"items\": [\n" +
                "      { \"color\": \"blue\", \"text\": \"100\" },\n" +
                "      { \"color\": \"blue\", \"text\": \"103\" }\n" +
                "    ]\n" +
                "  }";

        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(actionWithAssertions(inflate(DOC, data, (type, payload) -> {
                    // do nothing, not lazy loading
                })))
                .check(hasRootContext());

        final MultiChildComponent multiChildComponent = (MultiChildComponent) mTestContext.getRootContext().findComponentById("container");
        final APLAbsoluteLayout absoluteLayout = (APLAbsoluteLayout) mTestContext.getPresenter().findView(multiChildComponent);

        // Verify initial items are present
        assertEquals(2, absoluteLayout.getChildCount());

        // Send InsertMultipleItems update
        final String updatePayload = "{\n" +
                "    \"listId\": \"vQdpOESlok\",\n" +
                "    \"listVersion\": 1,\n" +
                "    \"operations\": [\n" +
                "      {\n" +
                "        \"type\": \"InsertMultipleItems\",\n" +
                "        \"index\": 1,\n" +
                "        \"items\": [\n" +
                "           { \"color\": \"red\", \"text\": \"101\" },\n" +
                "           { \"color\": \"rgb(0,255,0)\", \"text\": \"102\" }\n" +
                "         ]\n" +
                "      }\n" +
                "    ]\n" +
                "  }";
        assertTrue(mAplController.updateDataSource("dynamicIndexList", updatePayload));
        onView(withId(multiChildComponent.getComponentId().hashCode())).perform(longClick());

        // Verify new items have been added to Container
        assertEquals(4, absoluteLayout.getChildCount());

        // Verify new item Views have been rendered
        onView(APLMatchers.withText("101")).check(matches(isDisplayed()));
        onView(withBorderColor(Color.RED)).check(matches(isDisplayed()));
        onView(APLMatchers.withText("102")).check(matches(isDisplayed()));
        onView(withBorderColor(Color.rgb(0x00, 0xFF, 0x00))).check(matches(isDisplayed()));

        // Verify the new items are in the expected positions on the screen
        onView(APLMatchers.withText("101")).check(isCompletelyBelow(APLMatchers.withText("100")));
        onView(APLMatchers.withText("102")).check(isCompletelyBelow(APLMatchers.withText("101")));
        // Verify the new items are in the expected positions in the ViewGroup
        assertSquare(absoluteLayout.getChildAt(1), Color.RED, "101");
        assertSquare(absoluteLayout.getChildAt(2), Color.rgb(0x00, 0xFF, 0x00), "102");

        // Verify the item that was previously in the position we inserted at is now shifted down
        onView(APLMatchers.withText("103")).check(isCompletelyBelow(APLMatchers.withText("102")));
        // Verify the item that was previously in the position we inserted is in expected position in the ViewGroup
        assertSquare(absoluteLayout.getChildAt(3), Color.BLUE, "103");
    }

    /**
     * Test updating a Container with DeleteItem operation
     */
    @Test
    @LargeTest
    public void testUpdate_DeleteItem() {
        final String data = "{\n" +
                "  \"type\": \"dynamicIndexList\",\n" +
                "  \"listId\": \"vQdpOESlok\",\n" +
                "  \"startIndex\": 0,\n" +
                "  \"listVersion\": 0,\n" +
                "  \"items\": [\n" +
                "      { \"color\": \"blue\", \"text\": \"100\" },\n" +
                "      { \"color\": \"red\", \"text\": \"101\" },\n" +
                "      { \"color\": \"blue\", \"text\": \"102\" }\n" +
                "    ]\n" +
                "  }";

        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(actionWithAssertions(inflate(DOC, data, (type, payload) -> {
                    // do nothing, not lazy loading
                })))
                .check(hasRootContext());

        final MultiChildComponent multiChildComponent = (MultiChildComponent) mTestContext.getRootContext().findComponentById("container");
        final APLAbsoluteLayout absoluteLayout = (APLAbsoluteLayout) mTestContext.getPresenter().findView(multiChildComponent);

        // Verify initial items are present
        assertEquals(3, absoluteLayout.getChildCount());

        // Send DeleteItem update
        final String updatePayload = "{\n" +
                "    \"listId\": \"vQdpOESlok\",\n" +
                "    \"listVersion\": 1,\n" +
                "    \"operations\": [\n" +
                "      {\n" +
                "        \"type\": \"DeleteItem\",\n" +
                "        \"index\": 1\n" +
                "      }\n" +
                "    ]\n" +
                "  }";
        assertTrue(mAplController.updateDataSource("dynamicIndexList", updatePayload));
        onView(withId(multiChildComponent.getComponentId().hashCode())).perform(longClick());

        // Verify Container has one less item
        assertEquals(2, absoluteLayout.getChildCount());

        // Verify deleted item no longer exists
        onView(APLMatchers.withText("101")).check(doesNotExist());
        onView(withBorderColor(Color.RED)).check(doesNotExist());

        // Verify the item that was after the deleted item is now in the deleted items position
        assertSquare(absoluteLayout.getChildAt(1), Color.BLUE, "102");
    }

    @Test
    @LargeTest
    public void testUpdate_DeleteManyItems() {
        final String data = createManyItems(1000).toString();

        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(actionWithAssertions(inflate(DOC, data, (type, payload) -> {})))
                .check(hasRootContext());

        // Send DeleteMultipleItems update
        final String updatePayload = "{\n" +
                "    \"listId\": \"vQdpOESlok\",\n" +
                "    \"listVersion\": 1,\n" +
                "    \"operations\": [\n" +
                "      {\n" +
                "        \"type\": \"DeleteMultipleItems\",\n" +
                "        \"index\": 0,\n" +
                "        \"count\": 999\n" +
                "      }\n" +
                "    ]\n" +
                "  }";
        assertTrue(mAplController.updateDataSource("dynamicIndexList", updatePayload));
        onView(isRoot()).perform(waitFor(100));

        final MultiChildComponent multiChildComponent = (MultiChildComponent) mTestContext.getRootContext().findComponentById("container");
        final APLAbsoluteLayout absoluteLayout = (APLAbsoluteLayout) mTestContext.getPresenter().findView(multiChildComponent);

        // Verify Container has less items
        assertEquals(1, absoluteLayout.getChildCount());

        // Verify the item that was after the deleted items is now shifted to the deleted items position
        assertSquare(absoluteLayout.getChildAt(0), Color.BLUE, "999");
        assertEquals(3, mTestContext.getRootContext().getComponents().size());
    }

    private static JSONObject createManyItems(int size) {
        try {
            JSONArray items = new JSONArray();
            for (int i = 0; i < size; i++) {
                items.put(new JSONObject()
                        .put("color", "blue")
                        .put("text", Integer.toString(i)));
            }
            JSONObject result = new JSONObject()
                    .put("type", "dynamicIndexList")
                    .put("listId", "vQdpOESlok")
                    .put("startIndex", 0)
                    .put("listVersion", 0)
                    .put("items", items);
            return result;
        } catch (JSONException e) {
            fail(e.getMessage());
        }
        return null;
    }

    /**
     * Test updating a Container with DeleteMultipleItems operation
     */
    @Test
    @LargeTest
    public void testUpdate_DeleteMultipleItems() {
        final String data = "{\n" +
                "  \"type\": \"dynamicIndexList\",\n" +
                "  \"listId\": \"vQdpOESlok\",\n" +
                "  \"startIndex\": 0,\n" +
                "  \"listVersion\": 0,\n" +
                "  \"items\": [\n" +
                "      { \"color\": \"blue\", \"text\": \"100\" },\n" +
                "      { \"color\": \"red\", \"text\": \"101\" },\n" +
                "      { \"color\": \"rgb(0,255,0)\", \"text\": \"102\" },\n" +
                "      { \"color\": \"blue\", \"text\": \"103\" }\n" +
                "    ]\n" +
                "  }";

        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(actionWithAssertions(inflate(DOC, data, (type, payload) -> {
                    // do nothing, not lazy loading
                })))
                .check(hasRootContext());

        final MultiChildComponent multiChildComponent = (MultiChildComponent) mTestContext.getRootContext().findComponentById("container");
        final APLAbsoluteLayout absoluteLayout = (APLAbsoluteLayout) mTestContext.getPresenter().findView(multiChildComponent);

        // Verify initial items are present
        assertEquals(4, absoluteLayout.getChildCount());

        // Send DeleteMultipleItems update
        final String updatePayload = "{\n" +
                "    \"listId\": \"vQdpOESlok\",\n" +
                "    \"listVersion\": 1,\n" +
                "    \"operations\": [\n" +
                "      {\n" +
                "        \"type\": \"DeleteMultipleItems\",\n" +
                "        \"index\": 1,\n" +
                "        \"count\": 2\n" +
                "      }\n" +
                "    ]\n" +
                "  }";
        assertTrue(mAplController.updateDataSource("dynamicIndexList", updatePayload));
        onView(withId(multiChildComponent.getComponentId().hashCode())).perform(longClick());

        // Verify Container has less items
        assertEquals(2, absoluteLayout.getChildCount());

        // Verify deleted items no longer exists
        onView(APLMatchers.withText("101")).check(doesNotExist());
        onView(withBorderColor(Color.RED)).check(doesNotExist());
        onView(APLMatchers.withText("102")).check(doesNotExist());
        onView(withBorderColor(Color.rgb(0x00, 0xFF, 0x000))).check(doesNotExist());

        // Verify the item that was after the deleted items is now shifted to the deleted items position
        assertSquare(absoluteLayout.getChildAt(1), Color.BLUE, "103");
    }

    /**
     * Test updating a Container with SetItem operation
     */
    @Test
    @LargeTest
    public void testUpdate_SetItem() {
        final String data = "{\n" +
                "  \"type\": \"dynamicIndexList\",\n" +
                "  \"listId\": \"vQdpOESlok\",\n" +
                "  \"startIndex\": 0,\n" +
                "  \"listVersion\": 0,\n" +
                "  \"items\": [\n" +
                "      { \"color\": \"blue\", \"text\": \"100\" },\n" +
                "      { \"color\": \"red\", \"text\": \"101\" }\n" +
                "    ]\n" +
                "  }";

        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(actionWithAssertions(inflate(DOC, data, (type, payload) -> {
                    // do nothing, not lazy loading
                })))
                .check(hasRootContext());

        final MultiChildComponent multiChildComponent = (MultiChildComponent) mTestContext.getRootContext().findComponentById("container");
        final APLAbsoluteLayout absoluteLayout = (APLAbsoluteLayout) mTestContext.getPresenter().findView(multiChildComponent);

        // Verify initial items are present
        assertEquals(2, absoluteLayout.getChildCount());

        // Send SetItem update
        final String updatePayload = "{\n" +
                "    \"listId\": \"vQdpOESlok\",\n" +
                "    \"listVersion\": 1,\n" +
                "    \"operations\": [\n" +
                "      {\n" +
                "        \"type\": \"SetItem\",\n" +
                "        \"index\": 1,\n" +
                "        \"item\": {\n" +
                "           \"color\": \"rgb(0,255,0)\", " +
                "           \"text\": \"101 updated\"\n" +
                "        }\n" +
                "      }\n" +
                "    ]\n" +
                "  }";
        assertTrue(mAplController.updateDataSource("dynamicIndexList", updatePayload));
        onView(withId(multiChildComponent.getComponentId().hashCode())).perform(longClick());

        // Verify old item properties on updated item are gone
        onView(APLMatchers.withText("101")).check(doesNotExist());
        onView(withBorderColor(Color.RED)).check(doesNotExist());

        // Verify new item properties are rendered
        onView(APLMatchers.withText("101 updated")).check(matches(isDisplayed()));
        onView(withBorderColor(Color.rgb(0x00, 0xFF, 0x000))).check(matches(isDisplayed()));

        // Verify the new item properties are in the expected position by grabbing the Views at the updated index
        assertSquare(absoluteLayout.getChildAt(1), Color.rgb(0x00, 0xFF, 0x000), "101 updated");
    }

    // Asserts that an Item View representing a square (from DOC layout at the top) has been rendered
    private static void assertSquare(View itemView, int expectedColor, String expectedText) {
        APLAbsoluteLayout frameView = (APLAbsoluteLayout) itemView;
        APLGradientDrawable drawable = (APLGradientDrawable) frameView.getBackground();
        assertEquals(expectedColor, drawable.getBorderColor());
        assertEquals(expectedText, getText(frameView.getChildAt(0)));
    }

    private static CharSequence getText(final View view) {
        return ((APLTextView) view).getLayout().getText().toString();
    }
}
