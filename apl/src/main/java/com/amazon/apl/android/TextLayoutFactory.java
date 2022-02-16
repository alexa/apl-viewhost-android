/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.android;

import android.text.BoringLayout;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextDirectionHeuristic;
import android.text.TextDirectionHeuristics;
import android.text.TextPaint;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.amazon.apl.android.scaling.IMetricsTransform;
import com.amazon.apl.android.scaling.ViewportMetrics;
import com.amazon.apl.android.text.LineSpan;
import com.amazon.apl.enums.LayoutDirection;

import java.util.Objects;

/**
 * Factory for Text (and EditText) Layouts, which returns a cached layout
 * whenever possible, rather than creating a new layout every time.
 *
 * This factory serves two purposes: (1) To cache layouts which can be used to
 * provide text measurements to core, and (2) to cache layouts used for
 * display. The same actual layouts serve both purposes.
 **/
public class TextLayoutFactory {
    private static final String TAG = "TextMeasure";
    private static final boolean DEBUG = false;

    // Typical APL displays are 160dip, consider this the default when MetricsTransform is absent.
    // equivalent to DisplayMetrics.DENSITY_MEDIUM / 160;
    static float DEFAULT_DENSITY = 1.0f;

    private final TextLayoutCache mTextLayoutCache;
    private final float mDensity;
    private final AndroidTextMeasure mAndroidTextMeasure;

    /**
     * @return factory for use with specified display density metrics.
     */
    public static TextLayoutFactory create(IMetricsTransform metricsTransform) {
        ViewportMetrics metrics = metricsTransform.getUnscaledMetrics();
        float density = metrics == null ? DEFAULT_DENSITY : metrics.density();
        if (density == DEFAULT_DENSITY)
            return defaultFactory();
        return new TextLayoutFactory(density, new AndroidTextMeasure() {});
    }

    private static TextLayoutFactory sFACTORY = null;

    /**
     * @return Singleton default factory for common displays, and use when no
     * metricsTransform is available.
     */
    public static TextLayoutFactory defaultFactory() {
        if (sFACTORY == null)
            sFACTORY = new TextLayoutFactory(DEFAULT_DENSITY, new AndroidTextMeasure() {});
        return sFACTORY;
    }

    /**
     * Visible for mocking the boring text measure.
     */
    @VisibleForTesting
    TextLayoutFactory(AndroidTextMeasure androidTextMeasure) {
        this(DEFAULT_DENSITY, androidTextMeasure);
    }

    private TextLayoutFactory(float density, AndroidTextMeasure androidTextMeasure) {
        mDensity = density;
        mTextLayoutCache = new TextLayoutCache();
        mAndroidTextMeasure = androidTextMeasure;
    }

    /**
     * Create Text layout or retrieve an appropriate one from cache.
     * <p>
     * Layout creation is optimized against input parameters. The assumption is: if input arguments
     * for {@link Layout} are the same, then result {@link Layout} does not change. The
     * output {@link Layout} is cached in a {@link TextLayoutCache}.
     * <p>
     * {@link TextPaint} and {@link BoringLayout} are created before actual {@link StaticLayout}. Both
     * creations are expensive operations and in order to avoid duplicate job input arguments for
     * {@link TextPaint} and {@link BoringLayout} are evaluated against previously called ones.
     * </p>
     *
     * @param versionCode The document version.
     * @param textProxy   Proxy for component property lookup.
     * @param innerWidth  The inner width of the text area
     * @param widthMode   The text measurement width strategy.
     * @param innerHeight The inner height of the text area
     * @param karaokeLine The current karaoke LineSpan.
     *
     * @return Layout
     */
    public Layout getOrCreateTextLayout(int versionCode, @NonNull TextProxy textProxy,
                                        int innerWidth, @NonNull TextMeasure.MeasureMode widthMode,
                                        int innerHeight, LineSpan karaokeLine) {
        // The key must consist of everything that could result in a different layout, otherwise
        // the wrong layout may be returned (due to erroneously resolving to the same key)
        final Float scalingFactor = textProxy.getScalingFactor();
        final String scaledVisualHash = textProxy.getVisualHash() + "x" + scalingFactor.hashCode();
        final String key = versionCode + ":" + scaledVisualHash + ":" + widthMode +
                           (karaokeLine != null ? ":" + karaokeLine.hashCode() : "");

        CharSequence text = null;
        int desiredTextWidth = -1;
        // use cached layout if possible
        final Layout cachedTextLayout = mTextLayoutCache.getLayout(key);
        if (cachedTextLayout != null) {
            final int textLayoutWidth = cachedTextLayout.getWidth();
            final int textLayoutHeight = cachedTextLayout.getHeight();
            // Early check if size hasn't changed then just reuse
            if (textLayoutWidth == innerWidth && textLayoutHeight == innerHeight) {
                if (DEBUG) Log.d(TAG, "TextLayout cache hit: " + key);
                return cachedTextLayout;
            }

            final Layout.Alignment alignment = textProxy.getTextAlignment();
            final TextDirectionHeuristic textDirectionHeuristic = textProxy.getDirectionHeuristic();
            final boolean isLeftAligned =
                    (alignment == Layout.Alignment.ALIGN_NORMAL && textDirectionHeuristic == TextDirectionHeuristics.LTR) ||
                    (alignment == Layout.Alignment.ALIGN_OPPOSITE && textDirectionHeuristic == TextDirectionHeuristics.RTL);
            // Presently only left aligned text can be reused when the width is changing,
            // since right and center aligned text will always need a new Layout created if the width
            // changes.
            // Further optimization consideration, if the text is 1 line we could use Gravity
            // to position the text (thereby allowing more layout reuse) instead of Layout.Align.
            if (isLeftAligned)  {
                final TextPaint textPaint = mTextLayoutCache.getOrCreateTextPaint(versionCode, scaledVisualHash, textProxy, mDensity);
                // Calculate desired width
                text = textProxy.getText(textProxy.getStyledText(), karaokeLine);

                desiredTextWidth = getOrCalculateDesiredWidth(scaledVisualHash, text, textPaint);

                // If both the bounds and the built static layout can contain the text then we can reuse
                if (desiredTextWidth <= innerWidth &&
                        desiredTextWidth <= textLayoutWidth) {
                    if (DEBUG) Log.d(TAG, "TextLayout cache inner hit: " + key);
                    return cachedTextLayout;
                }
            }
        }

        // create one if necessary
        final Layout newTextLayout = createTextLayout(versionCode, scaledVisualHash, textProxy,
                innerWidth, widthMode, innerHeight, karaokeLine, text, desiredTextWidth);
        if (DEBUG) Log.d(TAG, "TextLayout cache miss: " + key);
        mTextLayoutCache.putLayout(key, newTextLayout);
        return newTextLayout;
    }

    /**
     * Creates a new layout for an EditText component
     * <p>
     * EditText layouts are sized to fit a non-static string of "M"'s.
     * For this reason, layouts are not optimized for caching.
     * </p>
     *
     * @param versionCode   The document version.
     * @param editTextProxy Proxy for component property lookup.
     */
    public Layout createEditTextLayout(int versionCode, EditTextProxy editTextProxy,
                                       int innerWidth, int innerHeight) {
        String text = editTextProxy.getMeasureText();
        TextPaint textPaint = editTextProxy.getTextPaint(mDensity);

        final BoringLayout.Metrics boring = BoringLayout.isBoring(text, textPaint);
        final int boringTextWidth = (boring != null) ? boring.width :
                (int) Math.ceil(Layout.getDesiredWidth(text, textPaint));
        final int layoutWidth = Math.min(innerWidth, boringTextWidth);

        try {
            // TODO should be BoringLayout
            StaticLayout textLayout = StaticLayoutBuilder.create().
                    text(text).
                    textPaint(textPaint).
                    innerWidth(layoutWidth).
                    alignment(Layout.Alignment.ALIGN_NORMAL).
                    limitLines(true).
                    maxLines(1).
                    ellipsizedWidth(innerWidth).
                    aplVersionCode(versionCode).
                    build();
            return textLayout;
        } catch (
                StaticLayoutBuilder.LayoutBuilderException e) {
            Log.wtf(TAG, "Layout build failed.", e);
        }

        return null;
    }


    /**
     * Creates a new layout for a Text component
     */
    private Layout createTextLayout(int versionCode, String key, TextProxy textProxy,
                                    int innerWidth, @NonNull TextMeasure.MeasureMode widthMode,
                                    int innerHeight, LineSpan karaokeLine, CharSequence text, int desiredTextWidth) {

        // Create or get text paint.
        // TODO ask core for a paint hash
        final TextPaint textPaint = mTextLayoutCache.getOrCreateTextPaint(versionCode, key, textProxy, mDensity);

        if (text == null) {
            // Calculate desired width
            text = textProxy.getText(textProxy.getStyledText(), karaokeLine);
            desiredTextWidth = getOrCalculateDesiredWidth(key, text, textPaint);
        }

        // Create a new StaticLayout.
        try {
            //TODO - return a boring layout if we don't need all the features of StaticLayout
            final StaticLayout textLayout = createStaticLayout(versionCode,
                    text, textPaint, textProxy,
                    widthMode, innerWidth, innerHeight, desiredTextWidth);
            return textLayout;
        } catch (StaticLayoutBuilder.LayoutBuilderException e) {
            Log.wtf(TAG, "Layout build failed.", e);
        }

        return null;
    }

    /**
     * Creates a new static layout for a Text component
     */
    private StaticLayout createStaticLayout(int versionCode, CharSequence text,
                                            TextPaint textPaint, TextProxy proxy,
                                            TextMeasure.MeasureMode widthMode,
                                            int innerWidth, int innerHeight,
                                            int boringTextWidth) throws StaticLayoutBuilder.LayoutBuilderException {
        final int layoutWidth = getLayoutWidthForWidthMode(widthMode, innerWidth, boringTextWidth);
        final boolean limitLines = proxy.limitLines();
        final int maxLines = proxy.getMaxLines();

        StaticLayoutBuilder.Builder builder = StaticLayoutBuilder.create().
                text(text).
                textPaint(textPaint).
                lineSpacing(proxy.getLineHeight()).
                innerWidth(layoutWidth).
                alignment(proxy.getTextAlignment()).
                limitLines(limitLines).
                maxLines(maxLines).
                ellipsizedWidth(innerWidth).
                textDirection(proxy.getDirectionHeuristic()).
                aplVersionCode(versionCode);

        StaticLayout textLayout = builder.build();

        // In case the text does not fit into the view, we need to indicate the user that there is more
        // text remaining now being shown. We'd proceed truncating the text until the last fully
        // visible line with an ellipsis.
        //
        // After creating the layout, we are able to know the final height of
        // the entire spannable text. Truncates text if layout exceeds the view
        // box dimensions.
        final int staticLayoutHeight = textLayout.getHeight();

        if (!limitLines && staticLayoutHeight > innerHeight) {
            final int linesNeeded = textLayout.getLineCount();
            final int lineHeight = staticLayoutHeight / linesNeeded;
            final int linesFullyVisible = innerHeight / lineHeight;

            // Create a new and similar layout but setting maxLines param.
            builder.limitLines(true).maxLines(linesFullyVisible);
            textLayout = builder.build();
        }

        return textLayout;
    }

    private int getOrCalculateDesiredWidth(String key, CharSequence text, TextPaint paint) {
        Integer desiredTextWidth = mTextLayoutCache.getTextWidth(key);
        if (desiredTextWidth != null) {
            return desiredTextWidth;
        }
        desiredTextWidth = mAndroidTextMeasure.getDesiredTextWidth(text, paint);
        mTextLayoutCache.putTextWidth(key, desiredTextWidth);
        return desiredTextWidth;
    }

    private int getLayoutWidthForWidthMode(TextMeasure.MeasureMode widthMode, int innerWidth, int boringTextWidth) {
        switch (widthMode) {
            case Exactly:
                // force the width as directed by the parent or current one
                return innerWidth;
            case Undefined:
            case AtMost:
            default:
                // use the measured boringTextWidth, unless it exceeds the view's limit
                return Math.min(innerWidth, boringTextWidth);
        }
    }

    /**
     * Interface for mocking
     */
    interface AndroidTextMeasure {
        default int getDesiredTextWidth(CharSequence text, TextPaint textPaint) {
            final BoringLayout.Metrics boring = BoringLayout.isBoring(text, textPaint);
            return (boring != null) ? boring.width :
                    (int) Math.ceil(Layout.getDesiredWidth(text, textPaint));
        }
    }

    @VisibleForTesting
    public TextLayoutCache getLayoutCache() {
        return mTextLayoutCache;
    }

    public void clear() {
        mTextLayoutCache.clear();
    }
}
