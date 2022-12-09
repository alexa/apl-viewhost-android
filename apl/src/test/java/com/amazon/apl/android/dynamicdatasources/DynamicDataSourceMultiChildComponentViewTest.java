/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.dynamicdatasources;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import android.graphics.Color;
import android.view.View;

import androidx.test.filters.LargeTest;

import com.amazon.apl.android.APLController;
import com.amazon.apl.android.APLGradientDrawable;
import com.amazon.apl.android.APLLayout;
import com.amazon.apl.android.Component;
import com.amazon.apl.android.MultiChildComponent;
import com.amazon.apl.android.Text;
import com.amazon.apl.android.functional.Consumer;
import com.amazon.apl.android.views.APLAbsoluteLayout;
import com.amazon.apl.android.views.APLTextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.robolectric.shadows.ShadowLooper;

import java.util.List;
import java.util.Map;

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
        inflate(DOC, DATA_TOKEN, new TestTokenBasedDataSourceFetchCallback() {
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
                });

        final Component container =  mTestContext.getRootContext().findComponentById("container");
        final APLAbsoluteLayout absoluteLayout = (APLAbsoluteLayout) mTestContext.getPresenter().findView(container);

        // Verify initial item is loaded
        assertEquals(container.getChildCount(), 1);
        assertEquals(absoluteLayout.getChildCount(), 1);

        // Frame -> Text
        assertEquals("1", ((Text)container.getChildAt(0).getChildAt(0)).getText());

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
        mTestContext.getRootContext().updateDataSource("dynamicTokenList", updatePayload);
        testClock.doFrameUpdate(1000);
        ShadowLooper.idleMainLooper();

        // Verify new items 2-6 are loaded
        assertEquals(6, container.getChildCount()); // 1 initial + 5 new ones
        assertEquals(6, absoluteLayout.getChildCount());

        // Verify the new item Views are rendered. Only check the first because depending on device,
        // the rest may not render due to screen size.
        assertEquals("2", ((Text)container.getDisplayedChildAt(1).getDisplayedChildAt(0)).getText());
        assertEquals(absoluteLayout.getChildAt(1).getWidth(), container.getChildAt(1).getBounds().intWidth());
    }

    /**
     * Verify new child Views are rendered after a DataSource update which inserts new Components to the
     * Container
     */
    @Test
    @LargeTest
    public void testDataSourceUpdate_ViewsRendered() {
        inflate(DOC, DATA, new TestIndexBasedDataSourceFetchCallback() {
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
                });

        final MultiChildComponent multiChildComponent = (MultiChildComponent) mTestContext.getRootContext().findComponentById("container");
        final APLAbsoluteLayout absoluteLayout = (APLAbsoluteLayout) mTestContext.getPresenter().findView(multiChildComponent);

        // Verify initial item is loaded
        assertEquals(multiChildComponent.getChildCount(), 1);
        assertEquals(absoluteLayout.getChildCount(), 1);
        assertEquals("10", ((Text)multiChildComponent.getChildAt(0).getChildAt(0)).getText());

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
        mTestContext.getRootContext().updateDataSource("dynamicIndexList", updatePayload);
        testClock.doFrameUpdate(1000);
        ShadowLooper.idleMainLooper();

        // Verify new items 5-9 are loaded
        assertEquals(6, multiChildComponent.getChildCount()); // 1 initial + 5 new ones
        assertEquals(6, absoluteLayout.getChildCount());

        // Verify the new item Views are rendered
        assertEquals("5", ((Text)multiChildComponent.getChildAt(0).getChildAt(0)).getText());
        assertEquals(absoluteLayout.getChildAt(0).getWidth(), multiChildComponent.getChildAt(1).getBounds().intWidth());

        assertEquals("6", ((Text)multiChildComponent.getDisplayedChildAt(1).getChildAt(0)).getText());
        assertEquals(absoluteLayout.getChildAt(1).getWidth(), multiChildComponent.getChildAt(2).getBounds().intWidth());

        assertEquals("7", ((Text)multiChildComponent.getDisplayedChildAt(2).getChildAt(0)).getText());
        assertEquals(absoluteLayout.getChildAt(2).getWidth(), multiChildComponent.getChildAt(3).getBounds().intWidth());

        assertEquals("8", ((Text)multiChildComponent.getDisplayedChildAt(3).getChildAt(0)).getText());
        assertEquals(absoluteLayout.getChildAt(3).getWidth(), multiChildComponent.getChildAt(4).getBounds().intWidth());

        assertEquals("9", ((Text)multiChildComponent.getDisplayedChildAt(4).getChildAt(0)).getText());
        assertEquals(absoluteLayout.getChildAt(4).getWidth(), multiChildComponent.getChildAt(5).getBounds().intWidth());


        // Send an update for item 11, after the initial startIndex
        updatePayload = "{\n" +
                "  \"listId\": \"vQdpOESlok\",\n" +
                "  \"startIndex\": 11,\n" +
                "  \"items\": [\n" +
                "    { \"color\": \"purple\", \"text\": \"11\" }\n" +
                "  ]\n" +
                "}";
        mTestContext.getRootContext().updateDataSource("dynamicIndexList", updatePayload);
        testClock.doFrameUpdate(2000);
        ShadowLooper.idleMainLooper();

        // Verify item 11 loaded
        assertEquals(7, multiChildComponent.getChildCount()); // added 1 item to the previous 6
        assertEquals(7, absoluteLayout.getChildCount());


        // Verify the new item View is rendered
        assertEquals("9", ((Text)multiChildComponent.getDisplayedChildAt(4).getChildAt(0)).getText());
        assertEquals(absoluteLayout.getChildAt(6).getWidth(), multiChildComponent.getChildAt(6).getBounds().intWidth());
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
