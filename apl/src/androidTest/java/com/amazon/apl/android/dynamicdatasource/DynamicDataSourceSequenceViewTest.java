/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.dynamicdatasource;

import android.graphics.Color;
import android.util.Log;
import android.view.View;

import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.IdlingResource;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.amazon.apl.android.APLController;
import com.amazon.apl.android.APLGradientDrawable;
import com.amazon.apl.android.Component;
import com.amazon.apl.android.MultiChildComponent;
import com.amazon.apl.android.Text;
import com.amazon.apl.android.espresso.APLMatchers;
import com.amazon.apl.android.espresso.APLViewIdlingResource;
import com.amazon.apl.android.views.APLAbsoluteLayout;
import com.amazon.apl.android.views.APLTextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.IntStream;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.actionWithAssertions;
import static androidx.test.espresso.action.ViewActions.longClick;
import static androidx.test.espresso.action.ViewActions.swipeDown;
import static androidx.test.espresso.action.ViewActions.swipeUp;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class DynamicDataSourceSequenceViewTest extends AbstractDynamicDataSourceComponentViewTest {

    private static final String DOC = "{\n" +
            "  \"type\": \"APL\",\n" +
            "  \"version\": \"1.1\",\n" +
            "  \"theme\": \"light\",\n" +
            "  \"layouts\": {\n" +
            "    \"square\": {\n" +
            "      \"parameters\": [\"color\", \"text\"],\n" +
            "      \"item\": {\n" +
            "        \"type\": \"Frame\",\n" +
            "        \"width\": \"100%\",\n" +
            "        \"height\": \"100dp\",\n" +
            "        \"id\": \"frame-${text}\",\n" +
            "        \"borderColor\": \"${color}\",\n" +
            "        \"borderWidth\": \"8dp\",\n" +
            "        \"item\": {\n" +
            "          \"type\": \"Text\",\n" +
            "          \"id\": \"text-${text}\",\n" +
            "          \"text\": \"${text}\",\n" +
            "          \"color\": \"white\",\n" +
            "          \"width\": 200,\n" +
            "          \"height\": 200\n" +
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
            "      \"items\": [\n" +
            "        {\n" +
            "          \"type\": \"Sequence\",\n" +
            "          \"id\": \"sequence\",\n" +
            "          \"data\": \"${dynamicSource}\",\n" +
            "          \"width\": \"100%\",\n" +
            "          \"height\": \"100%\",\n" +
            "          \"items\": {\n" +
            "            \"type\": \"square\",\n" +
            "            \"index\": \"${index}\",\n" +
            "            \"color\": \"${data.color}\",\n" +
            "            \"text\": \"${data.text}\"\n" +
            "          }\n" +
            "        }\n" +
            "      ]\n" +
            "    }\n" +
            "  }\n" +
            "}";

    private static final int ITEM_COUNT = 200;
    private static final int START_INDEX = 100;

    private static final String DATA = String.format("{\n" +
            "  \"type\": \"dynamicIndexList\",\n" +
            "  \"listId\": \"vQdpOESlok\",\n" +
            "  \"startIndex\": %d,\n" +
            "  \"minimumInclusiveIndex\": 0,\n" +
            "  \"maximumExclusiveIndex\": %d,\n" +
            "    \"items\": [\n" +
            "      { \"color\": \"blue\", \"text\": \"100\" },\n" +
            "      { \"color\": \"red\", \"text\": \"101\" },\n" +
            "      { \"color\": \"green\", \"text\": \"102\" },\n" +
            "      { \"color\": \"yellow\", \"text\": \"103\" },\n" +
            "      { \"color\": \"white\", \"text\": \"104\" },\n" +
            "      { \"color\": \"blue\", \"text\": \"105\" },\n" +
            "      { \"color\": \"red\", \"text\": \"106\" },\n" +
            "      { \"color\": \"green\", \"text\": \"107\" },\n" +
            "      { \"color\": \"yellow\", \"text\": \"108\" },\n" +
            "      { \"color\": \"white\", \"text\": \"109\" }\n" +
            "    ]\n" +
            "  }", START_INDEX, ITEM_COUNT);

    private static final String[] COLOURS = {"blue", "red", "green", "yellow", "white"};

    private static final int INITIAL_TOKEN_BASED_DATA_ITEM_COUNT = 100;
    public static final String BACKWARD_PAGE_TOKEN = "backwardPageToken";
    public static final String FORWARD_PAGE_TOKEN = "forwardPageToken";
    public static final String END_FORWARD_PAGE_TOKEN = "endForwardPageToken";
    public static final String END_BACKWARD_PAGE_TOKEN = "endBackwardPageToken";

    private IdlingResource mIdlingResource;

    @After
    public void teardown() {
        if (mIdlingResource != null) {
            IdlingRegistry.getInstance().unregister(mIdlingResource);
        }
    }

    /**
     * Verify all items in are rendered correctly when most are lazily loaded from the data
     * source
     */
    @Test
    @LargeTest
    public void testViewsRendered_indexList() {
        final TestDataSourceFetchCallback dataSourceFetchCallback = new TestDataSourceFetchCallback();
        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(actionWithAssertions(inflate(DOC, DATA, dataSourceFetchCallback)))
                .check(hasRootContext());

        final MultiChildComponent sequence = (MultiChildComponent) mTestContext.getRootContext().findComponentById("sequence");
        final APLAbsoluteLayout container
                = (APLAbsoluteLayout) mTestContext.getPresenter().findView(sequence);

        mIdlingResource = new APLViewIdlingResource(container);
        IdlingRegistry.getInstance().register(mIdlingResource);

        // Verify initial item rendered checking that the expected Text is displayed:
        onView(APLMatchers.withText(Integer.toString(START_INDEX))).check(matches(isDisplayed()));

        for (int i = 0; i < 10; i++) {
            onView(withId(sequence.getComponentId().hashCode())).perform(swipeUp());
        }

        // Verify last item is rendered:
        onView(APLMatchers.withText(Integer.toString(ITEM_COUNT-1))).check(matches(isDisplayed()));

        // Now swipe back to the first item
        for(int i = 0; i < 20; i++) {
            onView(withId(sequence.getComponentId().hashCode())).perform(swipeDown());
        }

        // Verify first item rendered
        onView(APLMatchers.withText(Integer.toString(0))).check(matches(isDisplayed()));
    }

    private class TestDataSourceFetchCallback extends TestIndexBasedDataSourceFetchCallback {
        @Override
        Map<String, Object> getItem(int index) {
            final Map<String, Object> item = new HashMap<>();
            item.put("color", COLOURS[index % COLOURS.length]);
            item.put("text", Integer.toString(index));
            return item;
        }

        @Override
        int getItemCount() {
            return ITEM_COUNT;
        }

        @Override
        APLController getAplController() {
            return mAplController;
        }
    }

    /**
     * Verify all Pages in are rendered correctly when most are fetched dynamically from the data
     * source
     * page tokens order: endBackwardPageToken - backwardPageToken - pageToken - forwardPageToken - endForwardPageToken
     */
    @Test
    @LargeTest
    public void testViewsRendered_tokenList() {
        final int eachPageTokenCount = COLOURS.length;
        final int totalItemCount = INITIAL_TOKEN_BASED_DATA_ITEM_COUNT + eachPageTokenCount * 4;
        final int lastPageIndex = INITIAL_TOKEN_BASED_DATA_ITEM_COUNT + eachPageTokenCount * 2 - 1;
        final int firstPageIndex = - eachPageTokenCount * 2;

        // Construct the data
        final String pageToken = "pageToken";
        StringBuilder initialItemsBuilder = new StringBuilder();
        for (int i = 0; i < INITIAL_TOKEN_BASED_DATA_ITEM_COUNT - 1; i++) {
            initialItemsBuilder.append(String.format(
                    "      { \"color\": \"%s\", \"text\": \"%d\" },\n",
                    COLOURS[i % COLOURS.length],
                    i
            ));
        }
        initialItemsBuilder.append(String.format(
                "      { \"color\": \"%s\", \"text\": \"%d\" }\n",
                COLOURS[(INITIAL_TOKEN_BASED_DATA_ITEM_COUNT - 1) % COLOURS.length],
                INITIAL_TOKEN_BASED_DATA_ITEM_COUNT - 1
        ));
        final String data = String.format("{\n" +
                "  \"type\": \"dynamicTokenList\",\n" +
                "  \"listId\": \"vQdpOESlok\",\n" +
                "  \"pageToken\": \"%s\",\n" +
                "  \"forwardPageToken\": \"%s\",\n" +
                "  \"backwardPageToken\": \"%s\",\n" +
                "  \"items\": [\n" +
                "%s" +
                "  ]\n" +
                "}", pageToken, FORWARD_PAGE_TOKEN, BACKWARD_PAGE_TOKEN, initialItemsBuilder.toString());

        // Perform the test
        final TestTokenDataSourceFetchCallback dataSourceFetchCallback = new TestTokenDataSourceFetchCallback();
        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(actionWithAssertions(inflate(DOC, data, dataSourceFetchCallback)))
                .check(hasRootContext());

        final Component sequence = mTestContext.getRootContext().findComponentById("sequence");
        final APLAbsoluteLayout container
                = (APLAbsoluteLayout) mTestContext.getPresenter().findView(sequence);
        mIdlingResource = new APLViewIdlingResource(container);
        IdlingRegistry.getInstance().register(mIdlingResource);

        // Check first initialized item is displayed
        onView(APLMatchers.withText(Integer.toString(0))).check(matches(isDisplayed()));

        // Swipe up to the first item
        for(int i = 0; i < eachPageTokenCount * 2; i += 5) {
            onView(withId(sequence.getComponentId().hashCode())).perform(swipeDown());
        }

        // Check first item is displayed
        onView(APLMatchers.withText(Integer.toString(firstPageIndex)))
                .check(matches(isDisplayed()));

        // Swipe to the last item
        for(int i = 0; i < totalItemCount; i += 5) {
            onView(withId(sequence.getComponentId().hashCode())).perform(swipeUp());
        }

        // Check last item is displayed
        onView(APLMatchers.withText(Integer.toString(lastPageIndex))).check(matches(isDisplayed()));
    }

    private class TestTokenDataSourceFetchCallback extends TestTokenBasedDataSourceFetchCallback {
        // Return {COLOURS.length} amount of items
        @Override
        JSONArray getItems(String pageToken) {
            return IntStream.range(0, COLOURS.length)
                    .mapToObj(index -> {
                        String color = COLOURS[index];
                        final JSONObject item = new JSONObject();
                        try {
                            item.put("color", color);
                            int globalIndex = index;
                            // Get the global index based on the page token:
                            // forward page start with {INITIAL_TOKEN_BASED_DATA_ITEM_COUNT} and increment
                            // backward page start with -1 and decrement
                            switch (pageToken) {
                                case FORWARD_PAGE_TOKEN:
                                    globalIndex = INITIAL_TOKEN_BASED_DATA_ITEM_COUNT + index;
                                    break;
                                case END_FORWARD_PAGE_TOKEN:
                                    globalIndex = INITIAL_TOKEN_BASED_DATA_ITEM_COUNT + COLOURS.length + index;
                                    break;
                                case BACKWARD_PAGE_TOKEN:
                                    globalIndex = - COLOURS.length + index;
                                    break;
                                case END_BACKWARD_PAGE_TOKEN:
                                    globalIndex = - COLOURS.length * 2 + index;
                                    break;
                            }
                            item.put("text", globalIndex);
                        } catch (JSONException e) {
                            Log.e("DynamicDataSourcePagerViewTest", e.toString());
                        }
                        return item;
                    }).collect(Collector.of(
                            JSONArray::new, //init accumulator
                            JSONArray::put, //processing each element
                            JSONArray::put  //confluence 2 accumulators in parallel execution
                    ));
        }

        @Override
        public String getNextPageToken(String pageToken) {
            switch (pageToken) {
                case BACKWARD_PAGE_TOKEN:
                    return END_BACKWARD_PAGE_TOKEN;
                case FORWARD_PAGE_TOKEN:
                    return END_FORWARD_PAGE_TOKEN;
                default:
                    return null;
            }
        }

        @Override
        APLController getAplController() {
            return mAplController;
        }
    }

    /**
     * Test updating a Sequence with InsertItem operation
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

        final MultiChildComponent sequence = (MultiChildComponent) mTestContext.getRootContext().findComponentById("sequence");
        final APLAbsoluteLayout container
                = (APLAbsoluteLayout) mTestContext.getPresenter().findView(sequence);

        // Verify initial items are present
        assertEquals(2, container.getChildCount());

        // Send InsertItem update
        final String updateInsertItemPayload = "{\n" +
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
        assertTrue(mAplController.updateDataSource("dynamicIndexList", updateInsertItemPayload));
        onView(withId(sequence.getComponentId().hashCode())).perform(longClick());

        // Verify new item has been added to RecyclerView
        assertEquals(3, container.getChildCount());

        // Verify new item Views have been rendered
        onView(APLMatchers.withText("101")).check(matches(isDisplayed()));
        onView(withBorderColor(Color.RED)).check(matches(isDisplayed()));

        // Verify the new item is in the expected position by grabbing the Views at the inserted index
        assertSquare(container.getChildAt(1), Color.RED, "101");

        // Verify the item that was previously in the position we inserted at is now in new position
        assertSquare(container.getChildAt(2), Color.BLUE, "102");
    }

    /**
     * Test updating a Sequence with InsertMultipleItems operation
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

        final MultiChildComponent sequence = (MultiChildComponent) mTestContext.getRootContext().findComponentById("sequence");
        final APLAbsoluteLayout container
                = (APLAbsoluteLayout) mTestContext.getPresenter().findView(sequence);

        // Verify we have initial items
        assertEquals(2, container.getChildCount());

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
        onView(withId(sequence.getComponentId().hashCode())).perform(longClick());

        // Verify new items have been added to RecyclerView
        assertEquals(4, container.getChildCount());

        // Verify new item Views have been rendered
        onView(APLMatchers.withText("101")).check(matches(isDisplayed()));
        onView(withBorderColor(Color.RED)).check(matches(isDisplayed()));
        onView(APLMatchers.withText("102")).check(matches(isDisplayed()));
        onView(withBorderColor(Color.rgb(0x00, 0xFF, 0x00))).check(matches(isDisplayed()));

        // Verify the new items are in the expected position by grabbing the Views at the inserted index
        assertSquare(container.getChildAt(1), Color.RED, "101");
        assertSquare(container.getChildAt(2), Color.rgb(0x00, 0xFF, 0x00), "102");

        // Verify the item that was previously in the position we inserted at is now in new position
        assertSquare(container.getChildAt(3), Color.BLUE, "103");
    }

    /**
     * Test updating a Sequence with DeleteItem operation
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

        final MultiChildComponent sequence = (MultiChildComponent) mTestContext.getRootContext().findComponentById("sequence");
        final APLAbsoluteLayout container
                = (APLAbsoluteLayout) mTestContext.getPresenter().findView(sequence);

        // Verify initial items are present
        assertEquals(3, container.getChildCount());

        // // Send DeleteItem update
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
        onView(withId(sequence.getComponentId().hashCode())).perform(longClick());

        // Verify RecyclerView has one less item
        assertEquals(2, container.getChildCount());

        // Verify deleted item no longer exists
        onView(APLMatchers.withText("101")).check(doesNotExist());
        onView(withBorderColor(Color.RED)).check(doesNotExist());

        // Verify the item that was after the deleted item is now shifted to the deleted items position
        assertSquare(container.getChildAt(1), Color.BLUE, "102");
    }

    /**
     * Test updating a Sequence with DeleteMultipleItems operation
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

        final MultiChildComponent sequence = (MultiChildComponent) mTestContext.getRootContext().findComponentById("sequence");
        final APLAbsoluteLayout container
                = (APLAbsoluteLayout) mTestContext.getPresenter().findView(sequence);

        // Verify initial items are present
        assertEquals(4, container.getChildCount());

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
        onView(withId(sequence.getComponentId().hashCode())).perform(longClick());

        // Verify RecyclerView has less items after delete
        assertEquals(2, container.getChildCount());

        // Verify deleted items no longer exists
        onView(APLMatchers.withText("101")).check(doesNotExist());
        onView(withBorderColor(Color.RED)).check(doesNotExist());
        onView(APLMatchers.withText("102")).check(doesNotExist());
        onView(withBorderColor(Color.rgb(0x00, 0xFF, 0x000))).check(doesNotExist());

        // Verify the item that was after the deleted items is now shifted to the deleted items position
        assertSquare(container.getChildAt(1), Color.BLUE, "103");
    }

    /**
     * Test updating a Sequence with SetItem operation
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

        final MultiChildComponent sequence = (MultiChildComponent) mTestContext.getRootContext().findComponentById("sequence");
        final APLAbsoluteLayout container
                = (APLAbsoluteLayout) mTestContext.getPresenter().findView(sequence);

        // Verify initial items are present
        assertEquals(2, container.getChildCount());

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
        onView(withId(sequence.getComponentId().hashCode())).perform(longClick());

        // Verify old item properties are gone
        onView(APLMatchers.withText("101")).check(doesNotExist());
        onView(withBorderColor(Color.RED)).check(doesNotExist());

        // Verify new item properties are rendered
        onView(APLMatchers.withText("101 updated")).check(matches(isDisplayed()));
        onView(withBorderColor(Color.rgb(0x00, 0xFF, 0x000))).check(matches(isDisplayed()));

        // Verify the new item properties are in the expected position by grabbing the Views at the updated index
        assertSquare(container.getChildAt(1), Color.rgb(0x00, 0xFF, 0x000), "101 updated");
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
