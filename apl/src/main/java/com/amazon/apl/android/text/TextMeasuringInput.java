/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.text;

import android.text.Layout;
import android.text.TextDirectionHeuristic;

import com.amazon.apl.android.RootContext;
import com.amazon.apl.android.primitive.Dimension;
import com.amazon.apl.android.primitive.StyledText;
import com.amazon.apl.enums.Display;

public class TextMeasuringInput {
    private StyledText styledText;
    private int innerWidth;
    private int innerHeight;
    private RootContext.MeasureMode widthMode;
    private int maxLines;
    private boolean limitLines;
    private float lineHeight;
    private Layout.Alignment alignment;
    private LineSpan karaokeSpan;
    private Dimension letterSpacing;
    private int shadowOffsetX;
    private int shadowOffsetY;
    private int shadowBlur;
    private int shadowColor;
    private int textColor;
    private int fontSize;
    private String fontFamily;
    private int fontWeight;
    private boolean italic;
    private String fontLanguage;
    private float density;
    private boolean classicFonts;
    private Display display;

    // The 4 attributes below are not part of measuring input, but calculated after layout is built.
    // Those might be requested as new input arguments on next pass. So we cache those and use to
    // determine if result output will be the same or not.
    private int measuredWidth;
    private int measuredHeight;
    private int unadjustedMeasuredHeight;
    private int boringWidth;
    private TextDirectionHeuristic textDirectionHeuristic;

    public RootContext.MeasureMode getWidthMode() {
        return widthMode;
    }

    public int getInnerWidth() {
        return innerWidth;
    }

    public int getInnerHeight() {
        return innerHeight;
    }
    public int getTextColor() {
        return textColor;
    }

    public int getFontSize() {
        return fontSize;
    }

    public String getFontFamily() {
        return fontFamily;
    }

    public String getFontLanguage() {
        return fontLanguage;
    }

    public int getFontWeight() {
        return fontWeight;
    }

    public boolean isItalic() {
        return italic;
    }

    public float getDensity() {
        return density;
    }

    public boolean isClassicFonts() {
        return classicFonts;
    }

    public Display getDisplay() {
        return display;
    }

    public StyledText getStyledText() {
        return styledText;
    }

    public float getLetterSpacing() {
        return letterSpacing == null ? 1.0f : letterSpacing.value();
    }

    public boolean getLimitLines() {
        return limitLines;
    }

    public int getMaxLines() {
        return maxLines;
    }

    public float getLineHeight() {
        return lineHeight;
    }

    public int getBoringWidth() {
        return boringWidth;
    }

    public int getShadowOffsetX() {
        return shadowOffsetX;
    }

    public int getShadowOffsetY() {
        return shadowOffsetY;
    }

    public int getShadowBlur() {
        return shadowBlur;
    }

    public int getShadowColor() {
        return shadowColor;
    }

    public Layout.Alignment getAlignment() {
        return alignment;
    }

    public TextDirectionHeuristic getTextDirectionHeuristic() { return textDirectionHeuristic; }

    public void updateMeasuredWidthAndHeight(int width, int height) {
        this.measuredWidth = width;
        this.measuredHeight = height;
    }

    public void updateBoringWidth(int boringWidth) {
        this.boringWidth = boringWidth;
    }

    public void updateUnadjustedMeasuredHeight(int unadjustedMeasuredHeight) {
        this.unadjustedMeasuredHeight = unadjustedMeasuredHeight;
    }

    public boolean paintsEqual(TextMeasuringInput other) {
        if (textColor != other.textColor) {
            return false;
        }

        if (classicFonts != other.classicFonts) {
            return false;
        }

        if (italic != other.italic) {
            return false;
        }

        if (!fontFamily.equals(other.fontFamily)) {
            return false;
        }

        if (fontSize != other.fontSize) {
            return false;
        }

        if (fontWeight != other.fontWeight) {
            return false;
        }

        if (!fontLanguage.equals(other.fontLanguage)) {
            return false;
        }
        
        if (Float.compare(getLetterSpacing(), other.getLetterSpacing()) != 0) {
            return false;
        }

        if (Float.compare(density, other.density) != 0) {
            return false;
        }

        if (shadowOffsetX != other.shadowOffsetX) {
            return false;
        }

        if (shadowOffsetY != other.shadowOffsetY) {
            return false;
        }

        if (shadowColor != other.shadowColor) {
            return false;
        }

        if (shadowBlur != other.shadowBlur) {
            return false;
        }

        return true;
    }

    public boolean textEqual(TextMeasuringInput other) {
        return this.styledText.equals(other.styledText);
    }

    private boolean karaokeSpanEqual(TextMeasuringInput otherInput) {
        if (karaokeSpan != null) {
            return karaokeSpan.equals(otherInput.karaokeSpan);
        }

        return otherInput.karaokeSpan == null;
    }

    private boolean displaysMatch(TextMeasuringInput otherInput) {
        // If both have equal Display, then no layout recalculation is required.
        if (display == otherInput.display) {
            return true;
        }

        // If newly requested input has Display.kDisplayNone, this equals to View.GONE and no layout
        // calculation is required at all.
        if (otherInput.display == Display.kDisplayNone) {
            return true;
        }

        // At this point we know that current and newly requested Displays are not equal and newly
        // requested is not Display.kDisplayNone. So we need to check just against the current one.
        return display != Display.kDisplayNone;
    }

    private boolean textDirectionMatch(TextMeasuringInput otherInput) {
        return textDirectionHeuristic.equals(otherInput.textDirectionHeuristic);
    }

    /**
     * Definitions
     * boringWidth - how much text would occupy if it was laid out in one line. boringWidth does not
     * have any constraints and equals to a sum of font metrics of characters for a given text.
     * boringWidth depends only on text formatting (spans and/or TextPaint applied) and text itself.
     *
     * innerWidth - desired width for text layout. this can be seen as a horizontal constraint for
     * text layout. laid out lines for the text layout cannot be more (in pixels), then a given
     * innerWidth.
     *
     * =============================================================================================
     * Possible Layout Cases
     * # provided text occupies less or equals to requested innerWidth. this will result in
     * TextLayout created with a given innerWidth, but text WILL NOT WRAP
     *
     * # provided text occupies more than requested innerWidth. this will result in TextLayout
     * created with a given innerWidth, but text WILL WRAP
     *
     * # special case, when requested innerWidth is 0 (or less than minimal font metric). In this
     * case layout procedure is not defined and depending on platform level will behave differently
     * (either wrap text so that a single character is on each line, or will not render it at all,
     * or will render just first character)
     *
     * =============================================================================================
     * Logic
     * # We check all parameters that might affect boring width (see equals method) before getting
     * into widthsMatch. This allows to avoid boring width recalculation as it is also an expensive operation.
     *
     * # Once we are in widthsMatch we need to check last input parameter that will affect final
     * layout: innerWidth. InnerWidth is checked against measuredWidth from previous pass. If all
     * input arguments are equal or last measuredWidth equals to requested inner width then we exit
     * with result set to true, which means no layout recalculation is required.
     *
     * # If requested innerWidth > boringWidth, this means that text occupies less space than
     * requested innerWidth and no recalculation pass is required too.
     */
    private boolean widthsMatch(TextMeasuringInput otherInput) {
        // If input parameters are the same, then nothing has changed.
        if (widthMode == otherInput.widthMode && innerWidth == otherInput.innerWidth) {
            return true;
        }


        // If previously used width mode and measured width same as requested, then result should be the same too.
        if (widthMode == otherInput.widthMode && measuredWidth == otherInput.innerWidth) {
            return true;
        }

        // For Exactly the calling width is innerWidth. If last measuredWidth same as measured width, then we can skip layout pass.
        if (otherInput.widthMode == RootContext.MeasureMode.Exactly && measuredWidth == otherInput.innerWidth) {
            return true;
        }

        // For AtMost the calling width is a min of boring and inner width. If the min result is same as measured width, we can also layout pass.
        if (otherInput.widthMode == RootContext.MeasureMode.AtMost && measuredWidth == Math.min(otherInput.boringWidth, otherInput.innerWidth)) {
            return true;
        }

        return false;
    }

    private boolean heightsMatch(TextMeasuringInput otherInput) {
        // If input parameters are the same, then nothing has changed.
        if (innerHeight == otherInput.innerHeight) {
            return true;
        }

        // If previously measured height same as requested, then does not affect anything.
        if (measuredHeight == otherInput.innerHeight) {
            return true;
        }

        // Measured height can be adjusted if maxLines is not defined. See Text#buildLayout. So we are interested
        // if unadjusted is smaller than requested. If previously unadjusted measured height same or less then
        // requested, then does not affect anything too.
        if (unadjustedMeasuredHeight <= otherInput.innerHeight) {
            return true;
        }

        return false;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof TextMeasuringInput)) {
            return false;
        }

        final TextMeasuringInput otherInput = (TextMeasuringInput) other;

        if (!textEqual(otherInput)) {
            return false;
        }

        if (!karaokeSpanEqual(otherInput)) {
            return false;
        }

        if (maxLines != otherInput.maxLines) {
            return false;
        }

        if (!paintsEqual(otherInput)) {
            return false;
        }

        if (lineHeight != otherInput.lineHeight) {
            return false;
        }

        if (alignment != otherInput.alignment) {
            return false;
        }

        // Widths assessed before heights as heights depend on widths. Captain Obvious.
        if (!widthsMatch(otherInput)) {
            return false;
        }

        if (!heightsMatch(otherInput)) {
            return false;
        }

        if (!displaysMatch(otherInput)) {
            return false;
        }

        if (!textDirectionMatch(otherInput)) {
            return false;
        }

        return true;
    }

    public static class Builder {
        private StyledText styledText;
        private int innerWidth;
        private int innerHeight;
        private RootContext.MeasureMode widthMode;
        private int maxLines;
        private boolean limitLines;
        private int textColor;
        private int fontSize;
        private String fontFamily;
        private String fontLanguage;
        private int fontWeight;
        private boolean italic;
        private Dimension letterSpacing;
        private float density;
        private float lineHeight;
        private Layout.Alignment alignment;
        private boolean classicFonts;
        private LineSpan karaokeSpan;
        private int shadowOffsetY;
        private int shadowOffsetX;
        private int shadowBlur;
        private int shadowColor;
        private Display display;
        private TextDirectionHeuristic textDirectionHeuristic;

        public Builder text(StyledText styledText) {
            this.styledText = styledText;

            return this;
        }

        public Builder innerWidth(int innerWidth) {
            this.innerWidth = innerWidth;
            return this;
        }

        public Builder innerHeight(int innerHeight) {
            this.innerHeight = innerHeight;
            return this;
        }

        public Builder widthMode(RootContext.MeasureMode widthMode) {
            this.widthMode = widthMode;
            return this;
        }

        public Builder maxLines(int maxLines) {
            this.maxLines = maxLines;
            this.limitLines = maxLines > 0;
            return this;
        }

        public Builder textColor(int textColor) {
            this.textColor = textColor;
            return this;
        }

        public Builder fontSize(int fontSize) {
            this.fontSize = fontSize;
            return this;
        }

        public Builder fontFamily(String fontFamily) {
            this.fontFamily = fontFamily;
            return this;
        }

        public Builder fontLanguage(String fontLanguage) {
            this.fontLanguage = fontLanguage;
            return this;
        }

        public Builder fontWeight(int fontWeight) {
            this.fontWeight = fontWeight;
            return this;
        }

        public Builder italic(boolean italic) {
            this.italic = italic;
            return this;
        }

        public Builder letterSpacing(Dimension letterSpacing) {
            this.letterSpacing = letterSpacing;
            return this;
        }

        public Builder density(float density) {
            this.density = density;
            return this;
        }

        public Builder lineHeight(float lineHeight) {
            this.lineHeight = lineHeight;
            return this;
        }

        public Builder alignment(Layout.Alignment alignment) {
            this.alignment = alignment;
            return this;
        }

        public Builder classicFonts(boolean classicFonts) {
            this.classicFonts = classicFonts;
            return this;
        }

        public Builder karaokeLine(LineSpan lineSpan) {
            this.karaokeSpan = lineSpan;
            return this;
        }

        public Builder shadowOffsetY(int shadowOffsetY) {
            this.shadowOffsetY = shadowOffsetY;
            return this;
        }

        public Builder shadowOffsetX(int shadowOffsetX) {
            this.shadowOffsetX = shadowOffsetX;
            return this;
        }

        public Builder shadowBlur(int shadowBlur) {
            this.shadowBlur = shadowBlur;
            return this;
        }

        public Builder shadowColor(int shadowColor) {
            this.shadowColor = shadowColor;
            return this;
        }

        public Builder display(Display display) {
            this.display = display;
            return this;
        }

        public Builder textDirectionHeuristic(TextDirectionHeuristic textDirectionHeuristic) {
            this.textDirectionHeuristic = textDirectionHeuristic;
            return this;
        }

        public TextMeasuringInput build() {
            final TextMeasuringInput measuringInput = new TextMeasuringInput();
            measuringInput.styledText = styledText;
            measuringInput.innerWidth = innerWidth;
            measuringInput.innerHeight = innerHeight;
            measuringInput.widthMode = widthMode;
            measuringInput.maxLines = maxLines;
            measuringInput.limitLines = limitLines;
            measuringInput.textColor = textColor;
            measuringInput.fontSize = fontSize;
            measuringInput.fontFamily = fontFamily;
            measuringInput.fontLanguage = fontLanguage;
            measuringInput.fontWeight = fontWeight;
            measuringInput.italic = italic;
            measuringInput.letterSpacing = letterSpacing;
            measuringInput.density = density;
            measuringInput.lineHeight = lineHeight;
            measuringInput.alignment = alignment;
            measuringInput.classicFonts = classicFonts;
            measuringInput.karaokeSpan = karaokeSpan;
            measuringInput.shadowBlur = shadowBlur;
            measuringInput.shadowColor = shadowColor;
            measuringInput.shadowOffsetY = shadowOffsetY;
            measuringInput.shadowOffsetX = shadowOffsetX;
            measuringInput.display = display;
            measuringInput.textDirectionHeuristic = textDirectionHeuristic;

            return measuringInput;
        }
    }
}
