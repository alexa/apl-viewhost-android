/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android;

import android.graphics.Paint;
import android.graphics.Typeface;
import androidx.annotation.NonNull;

import android.text.BoringLayout;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.Log;

import com.amazon.apl.android.font.TypefaceResolver;
import com.amazon.apl.enums.FontStyle;
import com.amazon.apl.enums.KeyboardType;
import com.amazon.apl.enums.PropertyKey;
import com.amazon.apl.enums.SubmitKeyType;

import static com.amazon.apl.enums.PropertyKey.kPropertyBorderColor;
import static com.amazon.apl.enums.PropertyKey.kPropertyBorderWidth;
import static com.amazon.apl.enums.PropertyKey.kPropertyColor;
import static com.amazon.apl.enums.PropertyKey.kPropertyDrawnBorderWidth;
import static com.amazon.apl.enums.PropertyKey.kPropertyFontFamily;
import static com.amazon.apl.enums.PropertyKey.kPropertyLang;
import static com.amazon.apl.enums.PropertyKey.kPropertyFontSize;
import static com.amazon.apl.enums.PropertyKey.kPropertyHighlightColor;
import static com.amazon.apl.enums.PropertyKey.kPropertyHint;
import static com.amazon.apl.enums.PropertyKey.kPropertyHintColor;
import static com.amazon.apl.enums.PropertyKey.kPropertyMaxLength;
import static com.amazon.apl.enums.PropertyKey.kPropertySecureInput;
import static com.amazon.apl.enums.PropertyKey.kPropertySelectOnFocus;
import static com.amazon.apl.enums.PropertyKey.kPropertySize;
import static com.amazon.apl.enums.PropertyKey.kPropertyText;
import static com.amazon.apl.enums.PropertyKey.kPropertyValidCharacters;

/**
 * Creates a APL EditText Component.
 * See @{link <a https://developer.amazon.com/en-US/docs/alexa/alexa-presentation-language/apl-edittext.html>
 * APL EditText Specification</a>}
 */
public class EditText extends Component implements ITextMeasurable {

    private static final String TAG = "EditText";

    // The measured width in pixels
    private int mMeasuredWidthPx = 0;

    // The measured height in pixels
    private int mMeasuredHeightPx = 0;

    EditText(long nativeHandle, String componentId, RenderingContext renderingContext) {
        super(nativeHandle, componentId, renderingContext);
    }

    @Override
    public boolean isFocusableInTouchMode() {
        return !isDisabled();
    }

    public boolean isValidCharacter(final char character) {
        return nIsValidCharacter(getNativeHandle(), character);
    }

    /**
     * @return Color of the border. Defaults to transparent.
     */
    public int getBorderColor() {
        return mProperties.getColor(kPropertyBorderColor);
    }

    /**
     * @return Width of the border. Defaults to 0.
     */
    public int getBorderWidth() {
        return mProperties.getDimension(kPropertyBorderWidth).intValue();
    }

    /**
     * @return Width of the border. Defaults to 0.
     */
    public int getDrawnBorderWidth() {
        return mProperties.getDimension(kPropertyDrawnBorderWidth).intValue();
    }

    /**
     * @return Text color. Defaults to <theme-dependent>.
     */
    public int getColor() {
        return mProperties.getColor(kPropertyColor);
    }

    /**
     * @return The name of the font family. Defaults to sans-serif.
     */
    @NonNull
    public String getFontFamily() {
        return mProperties.getString(kPropertyFontFamily);
    }

    /**
     * @return The name of the font language. Defaults to "".
     * For example to select the japanese characters of the "Noto Sans CJK" font family set lang to "ja-JP"
     */
    @NonNull
    public String getFontLanguage() {
        return mProperties.getString(kPropertyLang);
    }

    /**
     * @return The size of the text. Defaults to 40dp.
     */
    public int getFontSize() {
        return mProperties.getDimension(kPropertyFontSize).intValue();
    }

    /**
     * @return The style of the font. Defaults to normal.
     */
    public FontStyle getFontStyle() {
        return FontStyle.valueOf(mProperties.getEnum(PropertyKey.kPropertyFontStyle));
    }

    /**
     * @return Weight of the font. Defaults to normal.
     */
    public int getFontWeight() {
        return mProperties.getInt(PropertyKey.kPropertyFontWeight);
    }

    /**
     * @return The highlight color to show behind selected text. Defaults to empty string.
     */
    public int getHighlightColor() {
        return mProperties.getColor(kPropertyHighlightColor);
    }

    /**
     * @return Hint text to display when no text has been entered. Defaults to empty string.
     */
    @NonNull
    public String getHint() {
        return mProperties.getString(kPropertyHint);
    }

    /**
     * @return The color of the hint text. Defaults to <theme-dependent>.
     */
    public int getHintColor() {
        return mProperties.getColor(kPropertyHintColor);
    }

    public TypefaceResolver getTypefaceResolver() {
        return TypefaceResolver.getInstance();
    }

    /**
     * @return The style of the hint font. Defaults to normal.
     */
    public FontStyle getHintFontStyle() {
        return FontStyle.valueOf(mProperties.getEnum(PropertyKey.kPropertyHintStyle));
    }

    /**
     * @return The weight of the hint font. Defaults to normal.
     */
    public int getHintFontWeight() {
        return mProperties.getInt(PropertyKey.kPropertyHintWeight);
    }

    /**
     * @return The type of keyboard to display. Defaults to normal.
     */
    public KeyboardType getKeyboardType() {
        return KeyboardType.valueOf(mProperties.getEnum(PropertyKey.kPropertyKeyboardType));
    }

    /**
     * @return The maximum number of characters that can be entered. Defaults to 0.
     */
    public int getMaxLength() {
        return mProperties.getInt(kPropertyMaxLength);
    }

    /**
     * @return Hide characters as they are typed if true. Defaults to false.
     */
    public boolean isSecureInput() {
        return mProperties.getBoolean(kPropertySecureInput);
    }

    /**
     * @return If true the text will be selected on a focus event. Defaults to false.
     */
    public boolean isSelectOnFocus() {
        return mProperties.getBoolean(kPropertySelectOnFocus);
    }

    /**
     * @return Specifies approximately how many characters can be displayed. Defaults to 8.
     */
    public int getSize() {
        return mProperties.getInt(kPropertySize);
    }

    /**
     * @return The label of the return key. Defaults to done.
     */
    public SubmitKeyType getSubmitKeyType() {
        return SubmitKeyType.valueOf(mProperties.getEnum(PropertyKey.kPropertySubmitKeyType));
    }

    /**
     * @return The text to display. Defaults to empty string.
     */
    @NonNull
    public String getText() {
        return mProperties.getString(kPropertyText);
    }

    /**
     * @return Restrict the characters that can be entered. Defaults to empty string.
     */
    @NonNull
    public String getValidCharacters() {
        return mProperties.getString(kPropertyValidCharacters);
    }

    private static native boolean nIsValidCharacter(long nativeHandle, char character);

    @Override
    public void measureTextContent(
            final float density,
            final float widthPx,
            @NonNull final RootContext.MeasureMode widthMode,
            final float heightPx,
            @NonNull final RootContext.MeasureMode heightMode) {
        // Performance optimization: do not build layouts for text components with Display.kDisplayNone,
        // as this equals to View.GONE and does not affect layout pass. Also do not do layout pass
        // if both requested width and height are Exactly or AtMost 0.
        if (shouldSkipMeasurementPass(widthPx, heightPx, widthMode, getDisplay())) {
            mMeasuredWidthPx = Math.round(widthPx);
            mMeasuredHeightPx = Math.round(heightPx);
            return;
        }

        final String text = obtainText();
        final TextPaint textPaint = getTextPaint(density);
        final int boringTextWidth = calculateBoringLayoutWidth(text, textPaint);

        if (heightMode == RootContext.MeasureMode.Exactly) {
            mMeasuredHeightPx = Math.round(heightPx);
            mMeasuredWidthPx = boringTextWidth;
            return;
        }

        // In all cases for the width measureTextContent mode (undefined, at most, exactly) the text layout
        // will match or fit within the width dimension
        try {
            final StaticLayout textLayout = createStaticLayout(text, textPaint, Math.round(widthPx), boringTextWidth);

            // The measured height is derived from the calculated layout, and adjusted / overridden
            // based on the measureTextContent mode
            switch (heightMode) {
                case Undefined:
                    // use the layout preferred height
                    mMeasuredHeightPx = textLayout.getHeight();
                    break;
                case AtMost:
                    // use the measured height, unless it exceeds the limit
                    mMeasuredHeightPx = Math.min(Math.round(heightPx), textLayout.getHeight());
                    break;
            }

            // Width mode is already taken into account when building the layout. No need to do extra magic.
            mMeasuredWidthPx = textLayout.getWidth();
        } catch (StaticLayoutBuilder.LayoutBuilderException e) {
            Log.wtf(TAG, "Layout build failed.", e);
        }
    }

    @Override
    public int getMeasuredWidthPx() {
        return mMeasuredWidthPx;
    }

    @Override
    public int getMeasuredHeightPx() {
        return mMeasuredHeightPx;
    }

    /**
     * @return creates and returns {@link TextPaint}.
     */
    @NonNull
    private TextPaint getTextPaint(final float density) {
        TypefaceResolver typefaceResolver = TypefaceResolver.getInstance();
        final Typeface tf = typefaceResolver.getTypeface(getFontFamily(),
                getFontWeight(), FontStyle.kFontStyleItalic == getFontStyle(), getFontLanguage(),  false);

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

    private int calculateBoringLayoutWidth(CharSequence text, TextPaint textPaint) {
        final BoringLayout.Metrics boring = BoringLayout.isBoring(text, textPaint);
        return (boring != null) ? boring.width :
                (int) Math.ceil(Layout.getDesiredWidth(text, textPaint));
    }

    private StaticLayout createStaticLayout(CharSequence text, TextPaint textPaint,
                                            int innerWidth, int boringTextWidth)
            throws StaticLayoutBuilder.LayoutBuilderException {
        final int layoutWidth = Math.min(innerWidth, boringTextWidth);

        StaticLayout textLayout = StaticLayoutBuilder.create().
                                            text(text).
                                            textPaint(textPaint).
                                            innerWidth(layoutWidth).
                                            alignment(Layout.Alignment.ALIGN_NORMAL).
                                            limitLines(true).
                                            maxLines(1).
                                            ellipsizedWidth(innerWidth).
                                            aplVersionCode(getRenderingContext().getDocVersion()).
                                            build();
        return textLayout;
    }

    private String obtainText() {
        // Use size property to find the approximate width of the EditText component as per the spec:
        // https://developer.amazon.com/en-US/docs/alexa/alexa-presentation-language/apl-edittext.html#size
        StringBuilder dummyText = new StringBuilder(getSize());
        for(int i = 0; i < getSize(); i++) {
            dummyText.append("M");
        }
        return dummyText.toString();
    }
}
