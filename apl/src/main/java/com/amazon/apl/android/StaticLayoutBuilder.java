/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android;

import android.annotation.SuppressLint;
import android.graphics.Paint;
import android.os.Build;
import androidx.annotation.NonNull;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextDirectionHeuristic;
import android.text.TextDirectionHeuristics;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.Log;

import com.google.auto.value.AutoValue;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

@AutoValue
public abstract class StaticLayoutBuilder {
    private static final String TAG = "StaticLayoutBuilder";
    private static Constructor sConstructor;

    static {
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                sConstructor =
                        StaticLayout.class.getConstructor(CharSequence.class,
                                int.class,
                                int.class,
                                TextPaint.class,
                                int.class,
                                Layout.Alignment.class,
                                TextDirectionHeuristic.class,
                                float.class,
                                float.class,
                                boolean.class,
                                TextUtils.TruncateAt.class,
                                int.class,
                                int.class);
            }
        } catch (NoSuchMethodException e) {
            Log.e(TAG, "NoSuchMethodException " + e);
        }
    }

    abstract CharSequence text();
    abstract TextPaint textPaint();
    abstract float lineSpacing();
    abstract int innerWidth();
    abstract Layout.Alignment alignment();
    abstract boolean limitLines();
    abstract int maxLines();
    abstract int ellipsizedWidth();
    abstract int aplVersionCode();
    abstract TextDirectionHeuristic textDirection();

    @AutoValue.Builder
    public static abstract class Builder {
        public abstract Builder text(@NonNull CharSequence text);
        public abstract Builder textPaint(@NonNull TextPaint textPaint);
        public abstract Builder lineSpacing(float lineSpacing);
        public abstract Builder innerWidth(int innerWidth);
        public abstract Builder alignment(Layout.Alignment alignment);
        public abstract Builder limitLines(boolean limitLines);
        public abstract Builder maxLines(int maxLines);
        public abstract Builder ellipsizedWidth(int ellipsizedWidth);
        public abstract Builder aplVersionCode(int aplVersionCode);
        public abstract Builder textDirection(TextDirectionHeuristic textDirectionHeuristic);

        // make this package-private to avoid potentially using it
        abstract StaticLayoutBuilder get();

        public StaticLayout build() throws LayoutBuilderException {
            return get().build();
        }
    }

    public static StaticLayoutBuilder.Builder create() {
        // Defaults are set here
        return new AutoValue_StaticLayoutBuilder.Builder().
                text("").
                lineSpacing(0.0f).
                aplVersionCode(APLVersionCodes.APL_1_0).
                textDirection(TextDirectionHeuristics.LTR);
    }

    @NonNull
    public StaticLayout build() throws LayoutBuilderException {
        StaticLayout layout;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {

            // including extra padding in the old school StaticLayouts breaks vertically centering
            // text. However, prior to APL 1.4, we were including the extra padding so we will
            // continue to maintain that behavior for docs referencing earlier versions of APL.
            boolean includePadding;
            if (aplVersionCode() < APLVersionCodes.APL_1_4) {
                includePadding = true;
            } else {
                includePadding = false;
            }

            try {
                layout = (StaticLayout) sConstructor.newInstance(text(), 0, text().length(),
                        textPaint(), innerWidth(), alignment(), textDirection(),
                        getAdjustedLineSpacing(), 0.0f, includePadding,
                        limitLines() ? TextUtils.TruncateAt.END : null, ellipsizedWidth(),
                        maxLines() != 0 ? maxLines() : Integer.MAX_VALUE);
            } catch (IllegalAccessException e) {
                throw new LayoutBuilderException("Failed to build StaticLayout", e);
            } catch (InstantiationException e) {
                throw new LayoutBuilderException("Failed to build StaticLayout", e);
            } catch (InvocationTargetException e) {
                throw new LayoutBuilderException("Failed to build StaticLayout", e);
            }
        } else {
            // build new-school
            // Lint says to use `LineBreaker` but this isn't available in our API level.
            @SuppressLint("WrongConstant")
            StaticLayout.Builder builder = StaticLayout.Builder.obtain(
                    text(), 0, text().length(), textPaint(), innerWidth())
                    .setAlignment(alignment())
                    .setLineSpacing(0.0f, getAdjustedLineSpacing())
                    .setBreakStrategy(Layout.BREAK_STRATEGY_SIMPLE)
                    .setMaxLines(limitLines() ? maxLines() : Integer.MAX_VALUE)
                    .setTextDirection(textDirection());
            if (limitLines()) {
                builder.setEllipsize(TextUtils.TruncateAt.END)
                        .setEllipsizedWidth(ellipsizedWidth());
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                builder.setUseLineSpacingFromFallbacks(true);
            }

            layout = builder.build();
        }

        return layout;
    }

    /**
     * Returns the adjusted line spacing to match the css implementation of
     * line-height. Android implementation sums {@link Paint.FontMetrics#ascent}
     * and {@link Paint.FontMetrics#descent} while calculating line-height where as
     * css implementation only considers font ascent.
     *
     * @return Adjusted value for Android calculations
     */
    private float getAdjustedLineSpacing() {
        Paint.FontMetrics fontMetrics = textPaint().getFontMetrics();
        float sum = Math.abs(fontMetrics.ascent) + Math.abs(fontMetrics.descent);

        if (sum == 0) return lineSpacing();

        return Math.abs(lineSpacing() * fontMetrics.ascent) / sum;
    }

    public static class LayoutBuilderException extends Exception {
        public LayoutBuilderException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
