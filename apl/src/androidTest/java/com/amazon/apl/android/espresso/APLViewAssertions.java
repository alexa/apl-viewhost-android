/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.espresso;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;

import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.ViewAssertion;

import com.amazon.apl.android.APLLayout;

import org.junit.Assert;

import java.util.Objects;

public class APLViewAssertions {
    private APLViewAssertions() {}

    public static ViewAssertion isFinished() { return new FinishViewAssertion(); }

    public static ViewAssertion hasBackgroundColor(int color) { return  new ColorViewAssertion(color); }

    static class FinishViewAssertion implements ViewAssertion {
        @Override
        public void check(View view, NoMatchingViewException noViewFoundException) {
            APLLayout layout = (APLLayout) view;
            Objects.requireNonNull(layout);
            Assert.assertEquals(0, layout.getChildCount());
            Assert.assertNull("Root context should be null.", layout.getAPLContext());
        }
    }

    static class ColorViewAssertion implements ViewAssertion {
        final int mExpectedColor;

        public ColorViewAssertion(int expectedColor) {
            mExpectedColor = expectedColor;
        }

        @Override
        public void check(View view, NoMatchingViewException noViewFoundException) {
            final Drawable background = view.getBackground();
            final Bitmap bitmap = getBitmapFromDrawable(background);
            final int actual = bitmap.getPixel(0,0);
            final String message = String.format("Expected: 0x%08X, Actual: 0x%08X\n", mExpectedColor, actual);
            Assert.assertEquals(message, mExpectedColor, actual);
        }
    }

    static Bitmap getBitmapFromDrawable(final Drawable drawable) {
        final Bitmap bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }
}
