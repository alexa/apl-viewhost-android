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

import com.amazon.apl.android.primitive.StyledText;
import com.amazon.apl.android.scaling.IMetricsTransform;
import com.amazon.apl.android.scaling.ViewportMetrics;
import com.amazon.apl.android.scenegraph.text.APLTextLayout;
import com.amazon.apl.android.text.LineSpan;

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
    public static float DEFAULT_DENSITY = 1.0f;

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
     * @param innerWidthDp  The inner width of the text area in dp
     * @param widthMode   The text measurement width strategy.
     * @param innerHeightDp The inner height of the text area in dp
     * @param karaokeLine The current karaoke LineSpan.
     * @param metricsTransform {@link IMetricsTransform} The metrics transform
     *
     * @return Layout
     */
    public APLTextLayout getOrCreateTextLayout(
            int versionCode, @NonNull TextProxy textProxy,
            float innerWidthDp, @NonNull TextMeasure.MeasureMode widthMode,
            float innerHeightDp, @NonNull TextMeasure.MeasureMode heightMode,
            LineSpan karaokeLine, IMetricsTransform metricsTransform) {
        // The key must consist of everything that could result in a different layout, otherwise
        // the wrong layout may be returned (due to erroneously resolving to the same key)
        final Float scalingFactor = textProxy.getScalingFactor();
        final String scaledVisualHash = textProxy.getVisualHash() + "x" + scalingFactor.hashCode();
        final String key = versionCode + ":" + scaledVisualHash + ":" + widthMode +
                (karaokeLine != null ? ":" + karaokeLine.hashCode() : "");

        StyledText styledText = null;
        CharSequence text = null;
        int desiredTextWidth = -1;
        // use cached layout if possible
        final APLTextLayout cachedTextLayout = mTextLayoutCache.getLayout(key);
        final TextPaint textPaint = mTextLayoutCache.getOrCreateTextPaint(versionCode, scaledVisualHash, textProxy, mDensity);
        if (cachedTextLayout != null) {
            final int textLayoutWidth = cachedTextLayout.getLayout().getWidth();
            final int textLayoutHeight = cachedTextLayout.getLayout().getHeight();
            // Early check if size hasn't changed then just reuse
            final int innerWidthPx = getPermissiblePixelDimension(widthMode, innerWidthDp, metricsTransform);
            final int innerHeightPx = getPermissiblePixelDimension(heightMode, innerHeightDp, metricsTransform);
            if (textLayoutWidth == innerWidthPx && textLayoutHeight == innerHeightPx) {
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
                // Calculate desired width
                styledText = textProxy.getStyledText();
                text = styledText.getText(karaokeLine, textProxy.getMetricsTransform());
                desiredTextWidth = getOrCalculateDesiredWidth(scaledVisualHash, text, textPaint);

                // If both the bounds and the built static layout can contain the text then we can reuse
                if (desiredTextWidth <= innerWidthPx &&
                        desiredTextWidth <= textLayoutWidth) {
                    if (DEBUG) Log.d(TAG, "TextLayout cache inner hit: " + key);
                    return cachedTextLayout;
                }
            }
        }

        // create one if necessary
        if (styledText == null || text == null) {
            // Calculate desired width
            styledText = textProxy.getStyledText();
            text = styledText.getText(karaokeLine, textProxy.getMetricsTransform());
            desiredTextWidth = getOrCalculateDesiredWidth(key, text, textPaint);
        }
        final APLTextLayout newTextLayout = createTextLayout(versionCode, textProxy, textPaint,
                innerWidthDp, widthMode, innerHeightDp, heightMode, styledText, text, desiredTextWidth,
                metricsTransform);
        if (DEBUG) Log.d(TAG, "TextLayout cache miss: " + key);
        mTextLayoutCache.putLayout(key, newTextLayout);
        return newTextLayout;
    }

    /**
     * Create Text layout or retrieve an appropriate one from cache.
     * @param versionCode The document version.
     * @param aplTextProperties   Proxy for component property lookup.
     * @param innerWidthDp  The inner width of the text area in dp
     * @param widthMode   The text measurement width strategy.
     * @param innerHeightDp The inner height of the text area in dp
     * @param metricsTransform The {@link IMetricsTransform} to use to convert dp values to px
     *
     * @return Layout
     */
    public APLTextLayout getOrCreateTextLayoutForTextMeasure(
            int versionCode, @NonNull ITextProxy aplTextProperties, final StyledText text,
            float innerWidthDp, @NonNull TextMeasure.MeasureMode widthMode,
            float innerHeightDp, @NonNull TextMeasure.MeasureMode heightMode, IMetricsTransform metricsTransform) {
        // The key must consist of everything that could result in a different layout, otherwise
        // the wrong layout may be returned (due to erroneously resolving to the same key)
        String combinedTextPropertyHash = aplTextProperties.getVisualHash() + ":" + text.getHash();
        final int innerWidthPx = getPermissiblePixelDimension(widthMode, innerWidthDp, metricsTransform);
        final int innerHeightPx = getPermissiblePixelDimension(heightMode, innerHeightDp, metricsTransform);
        final String key = innerWidthPx + ":" + widthMode + ":" + innerHeightPx + ":" + heightMode + ":" + combinedTextPropertyHash;

        APLTextLayout cachedLayout = mTextLayoutCache.getLayout(key);
        if (cachedLayout != null) {
            if (DEBUG) Log.d(TAG, "TextMeasure layout cache hit: " + key);
            return cachedLayout;
        }
        final TextPaint textPaint = mTextLayoutCache.getOrCreateTextPaint(versionCode, combinedTextPropertyHash, aplTextProperties, mDensity);
        CharSequence t = text.getText(null, metricsTransform);
        final int desiredTextWidth = getOrCalculateDesiredWidth(combinedTextPropertyHash, t, textPaint);
        APLTextLayout newLayout = createTextLayout(versionCode, aplTextProperties, textPaint, innerWidthDp, widthMode,
                innerHeightDp, heightMode, text, t, desiredTextWidth, metricsTransform);

        if (DEBUG) Log.d(TAG, "TextMeasure layout cache miss: " + key);
        mTextLayoutCache.putLayout(key, newLayout);

        return newLayout;
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
    public Layout createEditTextLayout(int versionCode, ITextProxy editTextProxy,
                                       int innerWidth, int innerHeight, int size) {
        String text = ITextProxy.getMeasureText(size);
        TextPaint textPaint = editTextProxy.getTextPaint(mDensity);

        final BoringLayout.Metrics boring = BoringLayout.isBoring(text, textPaint);
        final int boringTextWidth = (boring != null) ? boring.width :
                (int) Math.ceil(Layout.getDesiredWidth(text, textPaint));
        final int layoutWidth = Math.min(innerWidth, boringTextWidth);

        try {
            // TODO should be BoringLayout
            return StaticLayoutBuilder.create().
                    text(text).
                    textPaint(textPaint).
                    innerWidth(layoutWidth).
                    alignment(Layout.Alignment.ALIGN_NORMAL).
                    limitLines(true).
                    maxLines(1).
                    ellipsizedWidth(innerWidth).
                    aplVersionCode(versionCode).
                    build();
        } catch (
                StaticLayoutBuilder.LayoutBuilderException e) {
            Log.wtf(TAG, "Layout build failed.", e);
        }

        return null;
    }

    /**
     * Creates a new layout for a Text component
     */
    private APLTextLayout createTextLayout(
            int versionCode, ITextProxy textProxy, TextPaint textPaint,
            float innerWidthDp, @NonNull final TextMeasure.MeasureMode widthMode,
            float innerHeightDp, @NonNull final TextMeasure.MeasureMode heightMode,
            StyledText styledText, CharSequence text, int desiredTextWidth, IMetricsTransform metricsTransform) {
        // Create a new StaticLayout.
        try {
            //TODO - return a boring layout if we don't need all the features of StaticLayout
            return createStaticLayout(versionCode, styledText, text, textPaint, textProxy,
                    innerWidthDp, widthMode, innerHeightDp, heightMode, desiredTextWidth, metricsTransform);
        } catch (StaticLayoutBuilder.LayoutBuilderException e) {
            Log.wtf(TAG, "Layout build failed.", e);
        }
        return null;
    }

    /**
     * Creates a new static layout for a Text component
     */
    private APLTextLayout createStaticLayout(int versionCode, StyledText styledText,
                                             CharSequence text,
                                             TextPaint textPaint, ITextProxy proxy,
                                             float innerWidthDp, @NonNull final TextMeasure.MeasureMode widthMode,
                                             float innerHeightDp, @NonNull final TextMeasure.MeasureMode heightMode,
                                             int boringTextWidth,
                                             IMetricsTransform metricsTransform) throws StaticLayoutBuilder.LayoutBuilderException {
        final int layoutWidth = getLayoutWidthForWidthMode(widthMode, innerWidthDp, boringTextWidth, metricsTransform);
        final boolean limitLines = proxy.limitLines();
        final int maxLines = proxy.getMaxLines();

        // Ellipsized width would be the maximum width that can be used
        int ellipsizedWidth = getPermissiblePixelDimension(widthMode, innerWidthDp, metricsTransform);

        StaticLayoutBuilder.Builder builder = StaticLayoutBuilder.create().
                text(text).
                textPaint(textPaint).
                lineSpacing(proxy.getLineHeight()).
                innerWidth(layoutWidth).
                alignment(proxy.getTextAlignment()).
                limitLines(limitLines).
                maxLines(maxLines).
                ellipsizedWidth(ellipsizedWidth).
                textDirection(proxy.getDirectionHeuristic()).
                aplVersionCode(versionCode);

        StaticLayout textLayout = builder.build();
        int lineCount = textLayout.getLineCount();
        int linesFullyVisible = lineCount;

        // In case the text does not fit into the view, we need to indicate the user that there is more
        // text remaining now being shown. We'd proceed truncating the text until the last fully
        // visible line with an ellipsis.
        //
        // After creating the layout, we are able to know the final height of
        // the entire spannable text. Truncates text if layout exceeds the view
        // box dimensions.
        final int staticLayoutHeight = textLayout.getHeight();

        int innerHeight = Math.round(metricsTransform.toViewhost(innerHeightDp));
        if (!limitLines && staticLayoutHeight > innerHeight) {
            final int lineHeight = staticLayoutHeight / lineCount;
            linesFullyVisible = innerHeight / lineHeight;

            // Create a new and similar layout but setting maxLines param.
            builder.limitLines(true).maxLines(linesFullyVisible);
            textLayout = builder.build();
        }

        // Width mode is already taken into account when building the layout. No need to do extra magic.
        int measureHeightPx = Math.round(ITextProxy.adjustHeightByMode(innerHeight, heightMode, textLayout));

        // Convert layout width and height back to dp, ensuring maximum integer value permissible within limits
        float layoutWidthDp = (float)Math.min(Math.ceil(metricsTransform.toCore((float)textLayout.getWidth())), innerWidthDp);
        float layoutHeightDp = (float)Math.min(Math.ceil(metricsTransform.toCore((float)measureHeightPx)), innerHeightDp);

        APLTextLayout result = new APLTextLayout(textLayout, styledText, lineCount > linesFullyVisible, layoutWidthDp, layoutHeightDp);
        return result;
    }

    private int getOrCalculateDesiredWidth(String key, CharSequence text, TextPaint paint) {
        Integer desiredTextWidth = mTextLayoutCache.getTextWidth(key);
        if (desiredTextWidth != null) {
            return desiredTextWidth;
        }
        desiredTextWidth = calculateDesiredWidth(text, paint);
        mTextLayoutCache.putTextWidth(key, desiredTextWidth);
        return desiredTextWidth;
    }

    private int calculateDesiredWidth(CharSequence text, TextPaint paint) {
        return mAndroidTextMeasure.getDesiredTextWidth(text, paint);
    }

    private int getPermissiblePixelDimension(TextMeasure.MeasureMode mode, float dimensionDp, IMetricsTransform metricsTransform) {
        return mode == TextMeasure.MeasureMode.Exactly ? Math.round(metricsTransform.toViewhost(dimensionDp)) : (int)Math.ceil(metricsTransform.toViewhost(dimensionDp));
    }

    private int getLayoutWidthForWidthMode(TextMeasure.MeasureMode widthMode, float innerWidthDp, int boringTextWidth, IMetricsTransform metricsTransform) {
        switch (widthMode) {
            case Exactly:
                // force the width as directed by the parent or current one
                return Math.round(metricsTransform.toViewhost(innerWidthDp));
            case Undefined:
            case AtMost:
            default:
                // use the measured boringTextWidth, unless it exceeds the view's limit
                return Math.min((int)Math.ceil(metricsTransform.toViewhost(innerWidthDp)), boringTextWidth);
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
