/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.document;

import android.graphics.Color;

import com.amazon.apl.android.APLGradientDrawable;
import com.amazon.apl.android.views.APLAbsoluteLayout;

import org.junit.Test;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static com.amazon.apl.android.espresso.APLViewActions.performConfigurationChange;
import static org.junit.Assert.assertEquals;

public class ConfigChangeTest extends AbstractDocViewTest {
    private static final String DOC = "\"type\": \"Frame\",\n" +
            "        \"borderRadius\": 40,\n" +
            "        \"borderWidth\": 10,\n" +
            "        \"borderColor\": \"blue\",\n" +
            "        \"backgroundColor\": \"blue\",\n" +
            "        \"width\":\"50vw\",\n" +
            "        \"height\": \"50vh\"\n";

    private static final String DOCUMENT_PROPERTIES = "\"onConfigChange\": [" +
            "    { \"type\": \"SetValue\", \"componentId\": \"testcomp\", \"property\": \"borderColor\", \"value\": \"red\" }" +
            "  ]";

    /**
     * Checks that SetValue command to change a dynamic property of Frame component executes on a configuration change
     */
    @Test
    public void testHandleConfigurationChange() {
        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(inflate(DOC, DOCUMENT_PROPERTIES))
                .check(hasRootContext());

        APLAbsoluteLayout view = mTestContext.getTestView();
        APLGradientDrawable drawable = (APLGradientDrawable) view.getBackground();
        assertEquals(Color.BLUE, drawable.getBorderColor());
        // Trigger a configuration change.
        onView(isRoot()).perform(performConfigurationChange(mTestContext.getRootContext()));
        drawable = (APLGradientDrawable) view.getBackground();
        assertEquals(Color.RED, drawable.getBorderColor());
    }
}
