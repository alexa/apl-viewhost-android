/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.espresso;

import android.text.StaticLayout;
import android.text.TextUtils;
import android.view.View;

import com.amazon.apl.android.views.APLAbsoluteLayout;
import com.amazon.apl.android.APLGradientDrawable;
import com.amazon.apl.android.Component;
import com.amazon.apl.android.views.APLTextView;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.is;

public class APLMatchers {

    private APLMatchers() {
    }


    public static Matcher<View> withComponent(Component component) {
        return withId(is(component.getComponentId().hashCode()));
    }

    /**
     * A customized {@link Matcher} for testing that
     * if one color match the background color of current view.
     * @param backgroundColor A color int value.
     *
     * @return Match or not.
     */
    public static Matcher<View> withBackgroundColor(final int backgroundColor) {
        return new TypeSafeMatcher<View>(APLAbsoluteLayout.class) {

            @Override
            public boolean matchesSafely(View view) {
                APLGradientDrawable drawable = (APLGradientDrawable)((APLAbsoluteLayout)view).getBackground();
                return drawable.getDefaultColor() == backgroundColor;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("with background color value: " + backgroundColor);
            }
        };
    }

    /**
     * Customized Matcher for {@link com.amazon.apl.android.views.APLTextView}
     */
    public static Matcher<View> withText(final String text) {
        return new TypeSafeMatcher<View>(APLTextView.class) {
            @Override
            protected boolean matchesSafely(View view) {
                boolean isStringEqual = false;
                StaticLayout layout = ((APLTextView) view).getLayout();
                if(layout != null) {
                    String real_text = ((APLTextView) view).getLayout().getText().toString();
                    isStringEqual = TextUtils.equals(text, real_text);
                }
                return isStringEqual;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("with text: " + text);
            }
        };
    }

    /**
     * Customized Matcher for {@link com.amazon.apl.android.views.APLTextView}
     */
    public static Matcher<View> withTextColor(final int textColor) {
        return new TypeSafeMatcher<View>(APLTextView.class) {
            @Override
            protected boolean matchesSafely(View view) {
                boolean isColorEqual = false;
                StaticLayout layout = ((APLTextView) view).getLayout();
                if(layout != null) {
                    int layoutTextColor = layout.getPaint().getColor();
                    isColorEqual = textColor == layoutTextColor;
                }
                return isColorEqual;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("with text color: #" + Integer.toHexString(textColor));
            }
        };
    }
}
