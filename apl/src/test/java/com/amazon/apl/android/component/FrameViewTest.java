/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.android.component;


import android.graphics.Color;
import android.os.Build;

import com.amazon.apl.android.views.APLAbsoluteLayout;
import com.amazon.apl.android.APLGradientDrawable;
import com.amazon.apl.android.Frame;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

//TODO remove this test when we have tests for execute commands
public class FrameViewTest extends AbstractComponentViewTest<APLAbsoluteLayout, Frame> {
    private static final Map<String, Integer> COLORS = new HashMap<>();
    static {
        COLORS.put("blue", Color.BLUE);
        COLORS.put("red", Color.RED);
        COLORS.put("black", Color.BLACK);
        COLORS.put("yellow", Color.YELLOW);
        COLORS.put("white", Color.WHITE);
    }

    @Before
    public void doBefore() {
        REQUIRED_PROPERTIES = ""; // no required properties in Frame Component.
        CHILD_LAYOUT_PROPERTIES = "\"item\": {\n" +
                "        \"type\": \"Frame\",\n" +
                "        \"borderRadius\": 40,\n" +
                "        \"borderWidth\": 10,\n" +
                "        \"borderColor\": \"green\",\n" +
                "        \"backgroundColor\": \"blue\",\n" +
                "        \"width\":\"50vw\",\n" +
                "        \"height\": \"50vh\"\n" +
                "      }";
    }

    /**
     * @return The string representation used in the APL document for this component.  For example
     * when testing the Text Component this method should return "Text".
     */
    @Override
    String getComponentType() {
        return "Frame";
    }

    @Override
    Class<APLAbsoluteLayout> getViewClass() {
        return APLAbsoluteLayout.class;
    }

    /**
     * Test the view after properties have been assigned.
     *
     * @param view The Component View for testing.
     **/
    @Override
    void testView_applyProperties(APLAbsoluteLayout view) {
        // Covered by FrameViewAdapterTest
    }

    @Test
    public void testView_dynamicBackgroundColor() {
        inflate(REQUIRED_PROPERTIES, CHILD_LAYOUT_PROPERTIES);

        APLAbsoluteLayout view = getTestView();
        for (String expectedColor : COLORS.keySet()) {

            executeCommands(setValueCommand("backgroundColor", expectedColor));

            APLGradientDrawable drawable = (APLGradientDrawable) view.getBackground();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                assertEquals((int)COLORS.get(expectedColor), drawable.getColor().getDefaultColor());
            }
        }
    }

    @Test
    public void testView_dynamicBorderColor() {
        inflate(REQUIRED_PROPERTIES, CHILD_LAYOUT_PROPERTIES);

        APLAbsoluteLayout view = getTestView();
        for (String expectedColor : COLORS.keySet()) {
            executeCommands(setValueCommand("borderColor", expectedColor));

            APLGradientDrawable drawable = (APLGradientDrawable) view.getBackground();
            assertEquals((int)COLORS.get(expectedColor), drawable.getBorderColor());
        }
    }
}
