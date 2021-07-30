/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.dynamicdatasource;

import android.util.Log;

import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.IdlingResource;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.amazon.apl.android.APLController;
import com.amazon.apl.android.Component;
import com.amazon.apl.android.espresso.APLMatchers;
import com.amazon.apl.android.espresso.APLViewIdlingResource;
import com.amazon.apl.android.views.APLAbsoluteLayout;

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
import static androidx.test.espresso.action.ViewActions.swipeLeft;
import static androidx.test.espresso.action.ViewActions.swipeRight;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class DynamicDataSourcePagerViewTest extends AbstractDynamicDataSourceComponentViewTest {

    private IdlingResource mIdlingResource = null;

    private static final String DOC = "{\n" +
            "  \"type\": \"APL\",\n" +
            "  \"version\": \"1.1\",\n" +
            "  \"theme\": \"light\",\n" +
            "  \"layouts\": {\n" +
            "    \"square\": {\n" +
            "      \"parameters\": [\"color\", \"text\"],\n" +
            "      \"item\": {\n" +
            "        \"type\": \"Frame\",\n" +
            "        \"width\": 200,\n" +
            "        \"height\": 200,\n" +
            "        \"id\": \"frame-${text}\",\n" +
            "        \"backgroundColor\": \"${color}\",\n" +
            "        \"item\": {\n" +
            "          \"type\": \"Text\",\n" +
            "          \"id\": \"text-${text}\",\n" +
            "          \"text\": \"${text}\",\n" +
            "          \"color\": \"black\",\n" +
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
            "          \"type\": \"Pager\",\n" +
            "          \"id\": \"pager\",\n" +
            "          \"data\": \"${dynamicSource}\",\n" +
            "          \"width\": \"100%\",\n" +
            "          \"height\": \"100%\",\n" +
            "          \"navigation\": \"normal\",\n" +
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

    private static final int PAGE_COUNT = 25;
    private static final int START_INDEX = 15;

    private static final String DATA = String.format("{\n" +
            "  \"type\": \"dynamicIndexList\",\n" +
            "  \"listId\": \"vQdpOESlok\",\n" +
            "  \"startIndex\": %d,\n" +
            "  \"minimumInclusiveIndex\": 0,\n" +
            "  \"maximumExclusiveIndex\": %d,\n" +
            "    \"items\": [\n" +
            "      { \"color\": \"blue\", \"text\": \"15\" },\n" +
            "      { \"color\": \"red\", \"text\": \"16\" },\n" +
            "      { \"color\": \"green\", \"text\": \"17\" },\n" +
            "      { \"color\": \"yellow\", \"text\": \"18\" },\n" +
            "      { \"color\": \"white\", \"text\": \"19\" }\n" +
            "    ]\n" +
            "  }", START_INDEX, PAGE_COUNT);


    private static final int INITIAL_TOKEN_BASED_DATA_ITEM_COUNT = 15;

    public static final String BACKWARD_PAGE_TOKEN = "backwardPageToken";
    public static final String FORWARD_PAGE_TOKEN = "forwardPageToken";
    public static final String END_FORWARD_PAGE_TOKEN = "endForwardPageToken";
    public static final String END_BACKWARD_PAGE_TOKEN = "endBackwardPageToken";

    private static final String[] COLOURS = {"blue", "red", "green", "yellow", "white"};

    @After
    public void doAfter() {
        if (mIdlingResource != null) {
            IdlingRegistry.getInstance().unregister(mIdlingResource);
            mIdlingResource = null;
        }
    }

    /**
     * Verify all Pages in are rendered correctly when most are fetched dynamically from the data
     * source
     */
    @Test
    @LargeTest
    public void testViewsRendered_indexList() {
        final TestDataSourceFetchCallback dataSourceFetchCallback = new TestDataSourceFetchCallback();
        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(actionWithAssertions(inflate(DOC, DATA, dataSourceFetchCallback)))
                .check(hasRootContext());

        final Component pager = mTestContext.getRootContext().findComponentById("pager");
        final APLAbsoluteLayout pagerView = (APLAbsoluteLayout) mTestContext.getPresenter().findView(pager);
        mIdlingResource = new APLViewIdlingResource(pagerView);
        IdlingRegistry.getInstance().register(mIdlingResource);

        // Swipe to the last page, checking that each Page View is rendered
        for(int i = START_INDEX; i < PAGE_COUNT; i++) {
            // Verify the current page is rendered correctly by checking the Text
            onView(APLMatchers.withText(Integer.toString(i))).check(matches(isDisplayed()));
            // Swipe to next page
            onView(APLMatchers.withComponent(pager))
                    .perform(swipeLeft());
        }

        // Now swipe to first page:
        for(int i = PAGE_COUNT - 1; i >= 0; i--) {
            onView(APLMatchers.withText(Integer.toString(i))).check(matches(isDisplayed()));
            onView(APLMatchers.withComponent(pager))
                    .perform(swipeRight());
        }

        // All items should now be loaded
        assertEquals(PAGE_COUNT, pager.getChildCount());
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
            return PAGE_COUNT;
        }

        @Override
        APLController getAplController() {
            return mAplController;
        }
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

        /**
         * test page tokens order:
         * endBackwardPageToken - backwardPageToken - pageToken - forwardPageToken - endForwardPageToken
         * @param pageToken current requested page token
         * @return next page token
         */
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
     * Verify all Pages in are rendered correctly when most are fetched dynamically from the data
     * source
     * page tokens order: endBackwardPageToken - backwardPageToken - pageToken - forwardPageToken - endForwardPageToken
     */
    @Test
    @LargeTest
    public void testViewsRendered_tokenList() {
        final int eachPageTokenCount = COLOURS.length;

        final int totalItemCount = INITIAL_TOKEN_BASED_DATA_ITEM_COUNT + eachPageTokenCount * 4;

        // the index of initial pageToken's item start with 0
        final int lastPageIndex = INITIAL_TOKEN_BASED_DATA_ITEM_COUNT + eachPageTokenCount * 2 - 1;
        final int firstPageIndex = - eachPageTokenCount * 2;

        // construct the data
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

        // perform the test
        final TestTokenDataSourceFetchCallback dataSourceFetchCallback = new TestTokenDataSourceFetchCallback();
        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(actionWithAssertions(inflate(DOC, data, dataSourceFetchCallback)))
                .check(hasRootContext());

        final Component pager = mTestContext.getRootContext().findComponentById("pager");
        final APLAbsoluteLayout pagerView = (APLAbsoluteLayout) mTestContext.getPresenter().findView(pager);

        mIdlingResource = new APLViewIdlingResource(pagerView);
        IdlingRegistry.getInstance().register(mIdlingResource);

        // Initially, not all items will be loaded
        assertTrue(pager.getChildCount() < totalItemCount);

        // Swipe backward to the first page, check each page view is rendered
        for (int i = 0; i > firstPageIndex; i--) {
            onView(APLMatchers.withText(Integer.toString(i))).check(matches(isDisplayed()));
            // Swipe to next page
            onView(APLMatchers.withComponent(pager))
                    .perform(swipeRight());
        }

        // Then swipe to the last page, checking that each Page View is rendered
        for(int i = firstPageIndex; i <= lastPageIndex; i++) {
            // Verify the current page is rendered correctly by checking the Text
            onView(APLMatchers.withText(Integer.toString(i))).check(matches(isDisplayed()));
            // Swipe to next page
            onView(APLMatchers.withComponent(pager))
                    .perform(swipeLeft());
        }

        // now all items should be loaded
        assertEquals(totalItemCount, pager.getChildCount());
    }

    /**
     * Test updating a Pager with InsertItem operation
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

        final Component pager = mTestContext.getRootContext().findComponentById("pager");
        final APLAbsoluteLayout pagerView = (APLAbsoluteLayout) mTestContext.getPresenter().findView(pager);
        mIdlingResource = new APLViewIdlingResource(pagerView);
        IdlingRegistry.getInstance().register(mIdlingResource);

        // Verify initial page are present
        assertEquals(2, pager.getChildCount());

        // Send InsertItem update at 2nd position
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
        onView(withId(pager.getComponentId().hashCode())).perform(longClick());

        // Verify new item has been added to Pager
        assertEquals(3, pager.getChildCount());

        // Swipe to 2nd page
        onView(APLMatchers.withComponent(pager)).perform(swipeLeft());

        // Verify new item has been rendered
        onView(APLMatchers.withText("101")).check(matches(isDisplayed()));

        // Verify the page that was previously in the position we inserted at is now in the position after the one we inserted
        onView(APLMatchers.withComponent(pager)).perform(swipeLeft()); // Swipe to next page
        onView(APLMatchers.withText("102")).check(matches(isDisplayed()));
    }

    /**
     * Test updating a Pager with InsertMultipleItems operation
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

        final Component pager = mTestContext.getRootContext().findComponentById("pager");
        final APLAbsoluteLayout pagerView = (APLAbsoluteLayout) mTestContext.getPresenter().findView(pager);
        mIdlingResource = new APLViewIdlingResource(pagerView);
        IdlingRegistry.getInstance().register(mIdlingResource);

        // Verify initial pages are present
        assertEquals(2, pager.getChildCount());

        // Send InsertMultipleItems update at 2nd position
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
        onView(withId(pager.getComponentId().hashCode())).perform(longClick());

        // Verify new items have been added to Pager
        assertEquals(4, pager.getChildCount());

        // Swipe to 2nd page
        onView(APLMatchers.withComponent(pager)).perform(swipeLeft());

        // Verify new item has been rendered
        onView(APLMatchers.withText("101")).check(matches(isDisplayed()));

        // Swipe to 3rd pge
        onView(APLMatchers.withComponent(pager)).perform(swipeLeft());

        // Verify other new item has been rendered
        onView(APLMatchers.withText("102")).check(matches(isDisplayed()));

        // Verify the page that was previously in the position we inserted at is now in the position after the inserted items
        onView(APLMatchers.withComponent(pager)).perform(swipeLeft()); // Swipe to next page
        onView(APLMatchers.withText("103")).check(matches(isDisplayed()));
    }

    /**
     * Test updating a Pager with InsertMultipleItems operation
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
                "      { \"color\": \"blue\", \"text\": \"101\" },\n" +
                "      { \"color\": \"blue\", \"text\": \"102\" }\n" +
                "    ]\n" +
                "  }";

        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(actionWithAssertions(inflate(DOC, data, (type, payload) -> {
                    // do nothing, not lazy loading
                })))
                .check(hasRootContext());

        final Component pager = mTestContext.getRootContext().findComponentById("pager");
        final APLAbsoluteLayout pagerView = (APLAbsoluteLayout) mTestContext.getPresenter().findView(pager);
        mIdlingResource = new APLViewIdlingResource(pagerView);
        IdlingRegistry.getInstance().register(mIdlingResource);

        // Verify initial pages are present
        assertEquals(3, pager.getChildCount());

        // Send DeleteItem update at 2nd position
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
        onView(withId(pager.getComponentId().hashCode())).perform(longClick());

        // Verify Pager has less items
        assertEquals(2, pager.getChildCount());

        // Swipe to 2nd page
        onView(APLMatchers.withComponent(pager)).perform(swipeLeft());

        // Verify deleted item isn't rendered
        onView(APLMatchers.withText("101")).check(doesNotExist());

        // Verify the item that was after the deleted item is now in this position
        onView(APLMatchers.withText("102")).check(matches(isDisplayed()));
    }

    /**
     * Test updating a Pager with DeleteMultipleItems operation
     */
    @Test
    @LargeTest
    public void testUpdate_DeleteMultipleItem() {
        final String data = "{\n" +
                "  \"type\": \"dynamicIndexList\",\n" +
                "  \"listId\": \"vQdpOESlok\",\n" +
                "  \"startIndex\": 0,\n" +
                "  \"listVersion\": 0,\n" +
                "  \"items\": [\n" +
                "      { \"color\": \"blue\", \"text\": \"100\" },\n" +
                "      { \"color\": \"blue\", \"text\": \"101\" },\n" +
                "      { \"color\": \"blue\", \"text\": \"102\" },\n" +
                "      { \"color\": \"blue\", \"text\": \"103\" }\n" +
                "    ]\n" +
                "  }";

        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(actionWithAssertions(inflate(DOC, data, (type, payload) -> {
                    // do nothing, not lazy loading
                })))
                .check(hasRootContext());

        final Component pager = mTestContext.getRootContext().findComponentById("pager");
        final APLAbsoluteLayout pagerView = (APLAbsoluteLayout) mTestContext.getPresenter().findView(pager);
        mIdlingResource = new APLViewIdlingResource(pagerView);
        IdlingRegistry.getInstance().register(mIdlingResource);

        // Verify initial pages are present
        assertEquals(4, pager.getChildCount());

        // Send DeleteMultipleItems update at 2nd position
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
        onView(withId(pager.getComponentId().hashCode())).perform(longClick());

        // Verify Pager has less items
        assertEquals(2, pager.getChildCount());

        // Swipe to 2nd page
        onView(APLMatchers.withComponent(pager)).perform(swipeLeft());

        // Verify deleted items aren't rendered
        onView(APLMatchers.withText("101")).check(doesNotExist());
        onView(APLMatchers.withText("102")).check(doesNotExist());

        // Verify the item that was after the deleted item is now in this position
        onView(APLMatchers.withText("103")).check(matches(isDisplayed()));
    }

    /**
     * Test updating a Pager with SetItem operation
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
                "      { \"color\": \"blue\", \"text\": \"101\" }\n" +
                "    ]\n" +
                "  }";

        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(actionWithAssertions(inflate(DOC, data, (type, payload) -> {
                    // do nothing, not lazy loading
                })))
                .check(hasRootContext());

        final Component pager = mTestContext.getRootContext().findComponentById("pager");
        final APLAbsoluteLayout pagerView = (APLAbsoluteLayout) mTestContext.getPresenter().findView(pager);
        mIdlingResource = new APLViewIdlingResource(pagerView);
        IdlingRegistry.getInstance().register(mIdlingResource);

        // Verify first item has been rendered
        onView(APLMatchers.withText("100")).check(matches(isDisplayed()));

        // Send SetItem update at first item
        String updatePayload = "{\n" +
                "    \"listId\": \"vQdpOESlok\",\n" +
                "    \"listVersion\": 1,\n" +
                "    \"operations\": [\n" +
                "      {\n" +
                "        \"type\": \"SetItem\",\n" +
                "        \"index\": 0,\n" +
                "        \"item\": {\n" +
                "           \"color\": \"purple\", " +
                "           \"text\": \"100 updated\"\n" +
                "        }\n" +
                "      }\n" +
                "    ]\n" +
                "  }";
        assertTrue(mAplController.updateDataSource("dynamicIndexList", updatePayload));
        onView(withId(pager.getComponentId().hashCode())).perform(longClick());

        // Verify first item has been updated
        onView(APLMatchers.withText("100")).check(doesNotExist());
        onView(APLMatchers.withText("100 updated")).check(matches(isDisplayed()));

        // Now update the 2nd page which is offscreen
        updatePayload = "{\n" +
                "    \"listId\": \"vQdpOESlok\",\n" +
                "    \"listVersion\": 2,\n" +
                "    \"operations\": [\n" +
                "      {\n" +
                "        \"type\": \"SetItem\",\n" +
                "        \"index\": 1,\n" +
                "        \"item\": {\n" +
                "           \"color\": \"green\", " +
                "           \"text\": \"101 updated\"\n" +
                "        }\n" +
                "      }\n" +
                "    ]\n" +
                "  }";
        assertTrue(mAplController.updateDataSource("dynamicIndexList", updatePayload));
        onView(withId(pager.getComponentId().hashCode())).perform(longClick());

        // Swipe to 2nd page
        onView(APLMatchers.withComponent(pager)).perform(swipeLeft());

        // Verify 2nd item has been updated
        onView(APLMatchers.withText("101")).check(doesNotExist());
        onView(APLMatchers.withText("101 updated")).check(matches(isDisplayed()));
    }
}
