/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.dynamicdatasource;

import android.graphics.Color;
import android.util.Pair;
import android.view.KeyEvent;

import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.IdlingResource;

import com.amazon.apl.android.APLOptions;
import com.amazon.apl.android.MultiChildComponent;
import com.amazon.apl.android.dependencies.ISendEventCallbackV2;
import com.amazon.apl.android.document.AbstractDocViewTest;
import com.amazon.apl.android.espresso.APLMatchers;
import com.amazon.apl.android.espresso.APLViewIdlingResource;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.actionWithAssertions;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.pressKey;
import static androidx.test.espresso.action.ViewActions.swipeLeft;
import static androidx.test.espresso.assertion.PositionAssertions.isCompletelyAbove;
import static androidx.test.espresso.assertion.PositionAssertions.isCompletelyBelow;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static com.amazon.apl.android.espresso.APLViewActions.waitFor;
import static org.junit.Assert.fail;

public class SwipeAndDeleteItemSequenceViewTest extends AbstractDocViewTest {
    private int listVersion = 1;

    private static final String DELETE_ITEM = "DeleteItem";
    private static final String INSERT_ITEM = "InsertItem";

    private ISendEventCallbackV2 mSendEventCallback = (args, components, sources, flags) -> {
        try {
            JSONObject payload = new JSONObject();
            payload.put("listId", "vQdpOESlok");
            payload.put("listVersion", listVersion++);
            payload.put("operations", new JSONArray()
                    .put(createOperation(args))
            );
            mAplController.updateDataSource("dynamicIndexList", payload.toString());
        } catch (JSONException e) {
            fail("JSON Exception");
        }
    };

    private JSONObject createOperation(Object[] args) throws JSONException {
        JSONObject operations = new JSONObject();
        String type = (String) args[0];
        int index = (int) args[1];
        operations.put("type", type)
                .put("index", index);
        if (INSERT_ITEM.equals(type)) {
            Map<String, Object> map = (Map) args[2];
            JSONObject item = new JSONObject();
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                item.put(entry.getKey(), entry.getValue());
            }
            operations.put("item", item);
        }
        return operations;
    }

    private static String DATA = "{\n" +
            "  \"type\": \"dynamicIndexList\",\n" +
            "  \"listId\": \"vQdpOESlok\",\n" +
            "  \"startIndex\": 0,\n" +
            "  \"minimumInclusiveIndex\": 0,\n" +
            "  \"maximumExclusiveIndex\": %d,\n" +
            "  \"items\": %s" +
            "}";

    private static final Pair<String,String>[] ITEMS = new Pair[] {
            new Pair<>("blue", "Item 0"),
            new Pair<>("red", "Item 1"),
            new Pair<>("green", "Item 2"),
            new Pair<>("yellow", "Item 3"),
            new Pair<>("magenta", "Item 4"),
            new Pair<>("white", "Item 5"),
            new Pair<>("blue", "Item 6"),
    };

    private static final String PAYLOAD_ID = "dynamicSource";

    private static final String COMPONENT_PROPS =
            "        \"type\": \"Sequence\",\n" +
            "        \"width\": \"100%\",\n" +
            "        \"height\": \"100%\",\n" +
            "        \"alignItems\": \"center\",\n" +
            "        \"justifyContent\": \"spaceAround\",\n" +
            "        \"data\": \"${" + PAYLOAD_ID + "}\",\n" +
            "        \"items\": [\n" +
            "          {\n" +
            "            \"type\": \"swipeAway\",\n" +
            "            \"color\": \"${data.color}\",\n" +
            "            \"text\": \"${data.text}\"\n" +
            "          }\n" +
            "        ]\n";

    private static final String DOCUMENT_PROPS =
            "\"styles\": {\n" +
            "    \"textStylePressable\": {\n" +
            "      \"values\": [\n" +
            "        {\n" +
            "          \"color\": \"black\"\n" +
            "        },\n" +
            "        {\n" +
            "          \"when\": \"${state.focused}\",\n" +
            "          \"color\": \"red\"\n" +
            "        }\n" +
            "      ]\n" +
            "    }\n" +
            "  },\n" +
            "  \"layouts\": {\n" +
            "    \"swipeAway\" : %s\n"+
            "  }\n";

    static final String LAYOUT = "{\n" +
            "      \"parameters\": [\"color\", \"text\"],\n" +
            "      \"item\": {\n" +
            "        \"type\": \"TouchWrapper\",\n" +
            "        \"width\": 100,\n" +
            "        \"item\": {\n" +
            "          \"type\": \"Frame\",\n" +
            "          \"backgroundColor\": \"${color}\",\n" +
            "          \"inheritParentState\": true,\n" +
            "          \"height\": 100,\n" +
            "          \"items\": {\n" +
            "            \"inheritParentState\": true,\n" +
            "            \"style\": \"textStylePressable\",\n" +
            "            \"type\": \"Text\",\n" +
            "            \"text\": \"${text}\"\n" +
            "          }\n" +
            "        },\n" +
            "        \"gestures\": [\n" +
            "          {\n" +
            "            \"type\": \"SwipeAway\",\n" +
            "            \"direction\": \"left\",\n" +
            "            \"action\":\"reveal\",\n" +
            "            \"items\": {\n" +
            "              \"type\": \"Frame\",\n" +
            "              \"backgroundColor\": \"purple\",\n" +
            "              \"width\": \"100%\",\n" +
            "              \"items\": {\n" +
            "                \"type\": \"Text\",\n" +
            "                \"text\": \"Swiped!\",\n" +
            "                \"color\": \"white\"\n" +
            "              }\n" +
            "            },\n" +
            "            \"onSwipeDone\": {\n" +
            "              \"type\": \"SendEvent\",\n" +
            "              \"arguments\": [\"DeleteItem\", \"${index}\"]\n" +
            "            }\n" +
            "          }\n" +
            "        ]\n" +
            "      }\n" +
            "    }\n";

    private static JSONObject constructLayout(String onPressArgs) {
        try {
            JSONObject layoutJson = new JSONObject(LAYOUT);
            layoutJson.getJSONObject("item").put("onPress", new JSONObject()
                    .put("type", "SendEvent")
                    .put("arguments", new JSONArray(onPressArgs))
            );
            return layoutJson;
        } catch (JSONException e) {
            return null;
        }
    }

    private static String createDocumentPropsWithOnPress(String onPressArgs) {
        JSONObject layout = constructLayout(onPressArgs);
        return String.format(DOCUMENT_PROPS, layout.toString());
    }

    private APLOptions mOptions;
    private IdlingResource mIdlingResource;

    @Before
    public void setup() {
        mOptions = APLOptions.builder().sendEventCallbackV2(mSendEventCallback).build();
        try {
            JSONArray jsonArray = new JSONArray();
            for (Pair<String, String> pair : ITEMS) {
                jsonArray.put(new JSONObject()
                        .put("color", pair.first)
                        .put("text", pair.second)
                );
            }
            DATA = String.format(DATA, ITEMS.length, jsonArray.toString());
        } catch (JSONException e) {
            fail(e.getMessage());
        }

    }

    @After
    public void teardown() {
        if (mIdlingResource != null) {
            IdlingRegistry.getInstance().unregister(mIdlingResource);
        }
    }

    @Test
    public void testDeletingFirstItem_drawsRemainingItems() {
        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(actionWithAssertions(inflate(COMPONENT_PROPS, createDocumentPropsWithOnPress("[\"DeleteItem\", \"${index}\"]"), PAYLOAD_ID, DATA, mOptions)))
                .check(hasRootContext());

        MultiChildComponent sequence = mTestContext.getTestComponent();
        mIdlingResource = new APLViewIdlingResource(mTestContext.getTestView());
        IdlingRegistry.getInstance().register(mIdlingResource);

        for (int i = 0; i < ITEMS.length; i++) {
            onView(APLMatchers.withText(ITEMS[i].second))
                    .check(matches(isDisplayed()));

            onView(withComponent(sequence.getChildAt(0)))
                    .perform(swipeLeft());

            onView(isRoot())
                    .perform(waitFor(500));

            onView(APLMatchers.withText(ITEMS[i].second))
                    .check(doesNotExist());
        }
    }

    @Test
    public void testDeletingSecondItem_drawsRemainingItems() {
        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(actionWithAssertions(inflate(COMPONENT_PROPS, createDocumentPropsWithOnPress("[\"DeleteItem\", \"${index}\"]"), PAYLOAD_ID, DATA, mOptions)))
                .check(hasRootContext());

        MultiChildComponent sequence = mTestContext.getTestComponent();
        mIdlingResource = new APLViewIdlingResource(mTestContext.getTestView());
        IdlingRegistry.getInstance().register(mIdlingResource);

        for (int i = 1; i < ITEMS.length; i++) {
            onView(APLMatchers.withText(ITEMS[i].second))
                    .check(matches(isDisplayed()));

            onView(withComponent(sequence.getChildAt(1)))
                    .perform(swipeLeft());

            onView(isRoot())
                    .perform(waitFor(500));

            onView(APLMatchers.withText(ITEMS[i].second))
                    .check(doesNotExist());
        }
    }

    @Test
    public void testDeletingThreeItems_deletesThreeItems() {
        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(actionWithAssertions(inflate(COMPONENT_PROPS, createDocumentPropsWithOnPress("[\"DeleteItem\", \"${index}\"]"), PAYLOAD_ID, DATA, mOptions)))
                .check(hasRootContext());

        MultiChildComponent sequence = mTestContext.getTestComponent();
        mIdlingResource = new APLViewIdlingResource(mTestContext.getTestView());
        IdlingRegistry.getInstance().register(mIdlingResource);

        onView(withComponent(sequence.getChildAt(2)))
                .perform(swipeLeft());

        onView(withComponent(sequence.getChildAt(1)))
                .perform(swipeLeft());

        onView(withComponent(sequence.getChildAt(0)))
                .perform(swipeLeft());

        onView(isRoot())
                .perform(waitFor(500));

        onView(APLMatchers.withText(ITEMS[0].second))
                .check(doesNotExist());

        onView(APLMatchers.withText(ITEMS[1].second))
                .check(doesNotExist());

        onView(APLMatchers.withText(ITEMS[2].second))
                .check(doesNotExist());
    }

    @Test
    public void testDelete_focusesNextItem() {
        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(actionWithAssertions(inflate(COMPONENT_PROPS, createDocumentPropsWithOnPress("[\"DeleteItem\", \"${index}\"]"), PAYLOAD_ID, DATA, mOptions)))
                .check(hasRootContext());

        // Initial focus on first item
        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(pressKey(KeyEvent.KEYCODE_DPAD_DOWN));
        onView(isRoot())
                .perform(waitFor(100));

        onView(APLMatchers.withText("Item 0"))
                .check(matches(APLMatchers.withTextColor(Color.RED)))
                .check(matches(isDisplayed()));

        // Delete the first item
        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(pressKey(KeyEvent.KEYCODE_DPAD_CENTER));
        onView(isRoot())
                .perform(waitFor(1000));

        // First item is deleted and second item is focused
        onView(APLMatchers.withText("Item 0"))
                .check(doesNotExist());
        onView(APLMatchers.withText("Item 1"))
                .check(matches(APLMatchers.withTextColor(Color.RED)))
                .check(matches(isDisplayed()));

        // Go down
        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(pressKey(KeyEvent.KEYCODE_DPAD_DOWN));
        onView(isRoot())
                .perform(waitFor(100));

        // Third item is focused
        onView(APLMatchers.withText("Item 2"))
                .check(matches(APLMatchers.withTextColor(Color.RED)))
                .check(matches(isDisplayed()));

        // Delete the third item
        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(pressKey(KeyEvent.KEYCODE_DPAD_CENTER));
        onView(isRoot())
                .perform(waitFor(1000));

        // Third item is deleted and fourth item is focused
        onView(APLMatchers.withText("Item 2"))
                .check(doesNotExist());
        onView(APLMatchers.withText("Item 3"))
                .check(matches(APLMatchers.withTextColor(Color.RED)))
                .check(matches(isDisplayed()));

        // Go up
        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(pressKey(KeyEvent.KEYCODE_DPAD_UP));
        onView(isRoot())
                .perform(waitFor(100));

        // Second item is focused since third item was deleted
        onView(APLMatchers.withText("Item 1"))
                .check(matches(APLMatchers.withTextColor(Color.RED)))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testAddAfterDelete_drawsAddedItem() {
        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(actionWithAssertions(inflate(COMPONENT_PROPS, createDocumentPropsWithOnPress("[\"InsertItem\", \"${index + 1}\", {\"color\": \"orange\", \"text\": \"Item 100\"}]"), PAYLOAD_ID, DATA, mOptions)))
                .check(hasRootContext());

        mIdlingResource = new APLViewIdlingResource(mTestContext.getTestView());
        IdlingRegistry.getInstance().register(mIdlingResource);

        // Delete second item in list
        onView(APLMatchers.withText("Item 1"))
                .perform(swipeLeft());

        // Add in item
        onView(APLMatchers.withText("Item 0"))
                .perform(click());

        // Wait for animation to complete
        onView(isRoot())
                .perform(waitFor(500));

        onView(APLMatchers.withText("Item 100"))
                .check(matches(isCompletelyDisplayed()))
                .check(isCompletelyBelow(APLMatchers.withText("Item 0")))
                .check(isCompletelyAbove(APLMatchers.withText("Item 2")));
    }
}
