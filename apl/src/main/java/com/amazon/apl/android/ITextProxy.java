/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.android;

import static com.amazon.apl.enums.TextAlign.kTextAlignAuto;
import static com.amazon.apl.enums.TextAlignVertical.kTextAlignVerticalAuto;

import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.TextDirectionHeuristic;
import android.text.TextDirectionHeuristics;
import android.text.TextPaint;

import androidx.annotation.NonNull;

import com.amazon.apl.android.font.TypefaceResolver;
import com.amazon.apl.android.primitive.Dimension;
import com.amazon.apl.enums.Display;
import com.amazon.apl.enums.FontStyle;
import com.amazon.apl.enums.LayoutDirection;
import com.amazon.apl.enums.TextAlign;
import com.amazon.apl.enums.TextAlignVertical;

/**
 * Interface for retrieving text properties from Core.
 */
public interface ITextProxy {
    default Display getDisplay() {
        return Display.kDisplayNormal;
    }

    /**
     * Gets the scaling factor implied by the metrics transform
     *
     * @return The multiplier used to apply scaling to view-host dimensions
     *         (1.0 means no scaling, 2.0 means view-host dimensions are double those in core)
     */
    default float getScalingFactor() {
        return 1.0f;
    }

    default String getVisualHash() {
        return "";
    }

    default Layout.Alignment getTextAlignment() {
        boolean ltr = getLayoutDirection() == LayoutDirection.kLayoutDirectionLTR;
        final TextAlign textAlign = getTextAlign();
        final Layout.Alignment alignment;
        switch (textAlign) {
            case kTextAlignLeft:
                alignment = ltr ? Layout.Alignment.ALIGN_NORMAL : Layout.Alignment.ALIGN_OPPOSITE;
                break;
            case kTextAlignCenter:
                alignment = Layout.Alignment.ALIGN_CENTER;
                break;
            case kTextAlignRight:
                alignment = ltr ? Layout.Alignment.ALIGN_OPPOSITE : Layout.Alignment.ALIGN_NORMAL;
                break;
            case kTextAlignAuto:
            default:
                alignment = Layout.Alignment.ALIGN_NORMAL;
        }
        return alignment;
    }

    /**
     * @return Horizontal alignment. Defaults to auto.
     */
    default TextAlign getTextAlign() {
        return kTextAlignAuto;
    }

    /**
     * @return Vertical alignment. Defaults to auto.
     */
    default TextAlignVertical getTextAlignVertical() {
        return kTextAlignVerticalAuto;
    }

    default TextDirectionHeuristic getDirectionHeuristic() {
        return TextDirectionHeuristics.LTR;
    }

    /**
     * @return The name of the font family. Defaults to sans-serif in most markets.
     * Defaults to Noto Sans CJK in Japan.
     */
    String getFontFamily();

    /**
     * @return Weight of the font. Defaults to normal.
     */
    int getFontWeight();

    default LayoutDirection getLayoutDirection() {
        return LayoutDirection.kLayoutDirectionLTR;
    }

    default boolean isItalic() {
        return FontStyle.kFontStyleItalic == getFontStyle();
    }

    /**
     * @return The name of the font language. Defaults to "".
     * For example to select the japanese characters of the "Noto Sans CJK" font family set lang to "ja-JP"
     */
    String getFontLanguage();

    /**
     * @return The style of the font. Defaults to normal.
     */
    FontStyle getFontStyle();

    /// Shadow properties
    default int getShadowOffsetHorizontal() {
        return 0;
    }

    default int getShadowOffsetVertical() {
        return 0;
    }

    default int getShadowColor() {
        return 0;
    }

    default int getShadowRadius() {
        return 0;
    }

    /**
     * @return The size of the text. Defaults to 40dp.
     */
    float getFontSize();

    /**
     * @return Additional space to add between letters.
     */
    default Dimension getLetterSpacing() {
        return Dimension.create(1.0f);
    }

    /**
     * @return Text color. Defaults to #FAFAFA for a dark theme and #1E2222 for a light theme.
     */
    default int getColor() {
        return 0;
    }

    /**
     * @return True if limits number of lines, false otherwise.
     */
    default boolean limitLines() {
        return getMaxLines() > 0;
    }

    /**
     * @return Maximum number of lines to display. Defaults to 0 (indicates no maximum).
     */
    default int getMaxLines() {
        return 0;
    }

    /**
     * @return Line-height multiplier. Defaults to 125%.
     */
    default float getLineHeight() {
        return 1.0f;
    }

    /**
     * @return EditText size property
     */
    default int getSize() {
        return 0;
    }

    /**
     * @return Generated measurement text for an EditText.
     */
    static String getMeasureText(int size) {
        // Use size property to find the approximate width of the EditText component as per the spec:
        // https://developer.amazon.com/en-US/docs/alexa/alexa-presentation-language/apl-edittext.html#size
        StringBuilder dummyText = new StringBuilder(size);
        for (int i = 0; i < size; i++) {
            dummyText.append("M");
        }
        return dummyText.toString();
    }

    /**
     * Clips provided height measurement as specified by MeasureMode.
     * @param heightPx Height provided in the measurement.
     * @param mode MeasureMode.
     * @param layout Actual Text Layout.
     * @return Clipped height.
     */
    static float adjustHeightByMode(float heightPx, TextMeasure.MeasureMode mode, Layout layout) {
        int measureHeightPx = 0;
        switch (mode) {
            case Undefined:
                // use the layout preferred height
                measureHeightPx = layout.getHeight();
                break;
            case AtMost:
                // use the measured height, unless it exceeds the limit
                measureHeightPx = Math.min(Math.round(heightPx), layout.getHeight());
                break;
            case Exactly:
                // force the height as directed by the parent
                measureHeightPx = Math.round(heightPx);
                break;
        }
        return measureHeightPx;
    }

    /**
     * @return creates and returns {@link TextPaint}.
     */
    @NonNull
    default TextPaint getTextPaint(final float density) {
        TypefaceResolver typefaceResolver = TypefaceResolver.getInstance();
        final Typeface tf = typefaceResolver.getTypeface(getFontFamily(),
                getFontWeight(), FontStyle.kFontStyleItalic == getFontStyle(), getFontLanguage(), false);

        // Create the text paint
        final TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(getColor());
        textPaint.setTextSize(getFontSize());
        textPaint.setTypeface(tf);
        // letterSpacing needs to be calculated as EM
        textPaint.density = density;
        textPaint.setTextScaleX(1.0f);

        return textPaint;
    }
}
