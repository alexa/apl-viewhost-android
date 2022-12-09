/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.dynamicdatasources;

import android.util.Log;
import android.view.View;

import com.amazon.apl.android.APLGradientDrawable;
import com.amazon.apl.android.APLOptions;
import com.amazon.apl.android.RootConfig;
import com.amazon.apl.android.component.AbstractComponentViewTest;
import com.amazon.apl.android.dependencies.IDataSourceFetchCallback;
import com.amazon.apl.android.robolectric.ActivityTest;
import com.amazon.apl.android.views.APLAbsoluteLayout;
import com.amazon.apl.enums.RootProperty;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;

/**
 * Like {@link AbstractComponentViewTest}, but for testing Components backed by a Dynamic DataSource
 */
abstract class AbstractDynamicDataSourceComponentViewTest extends ActivityTest {

    void inflate(String document, String dataSource, IDataSourceFetchCallback dataSourceFetchCallback) {
        String dynamicDataSourceType = null;
        try {
            final JSONObject dataObject = new JSONObject(dataSource);
            dynamicDataSourceType = dataObject.getString("type");
        } catch (JSONException e) {
        }
        String mDynamicDataSourceType = Objects.nonNull(dynamicDataSourceType) ?
                dynamicDataSourceType : "dynamicIndexList";

        APLOptions.Builder optionsBuilder = APLOptions.builder()
                .dataSourceFetchCallback(dataSourceFetchCallback);

        final RootConfig testRootConfig = RootConfig.create("Unit Test", "1.0")
                .registerDataSource(mDynamicDataSourceType)
                .set(RootProperty.kTapOrScrollTimeout, 50) // Half of the Fast swipe speed.
                .pagerChildCache(3)
                .sequenceChildCache(3);

        inflate(document, "dynamicSource", dataSource, optionsBuilder, testRootConfig);
    }

    /**
     * A customized {@link Matcher} for testing that if a color matches the border color of
     * current view.
     *
     * @param borderColor A color int value.
     *
     * @return Match or not.
     */
    static Matcher<View> withBorderColor(final int borderColor) {
        return new TypeSafeMatcher<View>(APLAbsoluteLayout.class) {

            @Override
            public boolean matchesSafely(View view) {
                APLGradientDrawable drawable = (APLGradientDrawable) view.getBackground();
                return drawable != null && drawable.getBorderColor() == borderColor;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("with border color value: " + borderColor);
            }
        };
    }
}