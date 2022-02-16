/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.android;

import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.TextPaint;

import androidx.annotation.NonNull;

import com.amazon.apl.android.font.TypefaceResolver;
import com.amazon.common.BoundObject;
import com.amazon.apl.enums.FontStyle;
import com.amazon.apl.enums.KeyboardType;
import com.amazon.apl.enums.PropertyKey;
import com.amazon.apl.enums.SubmitKeyType;

import static com.amazon.apl.enums.PropertyKey.kPropertyBorderColor;
import static com.amazon.apl.enums.PropertyKey.kPropertyBorderWidth;
import static com.amazon.apl.enums.PropertyKey.kPropertyColor;
import static com.amazon.apl.enums.PropertyKey.kPropertyDrawnBorderWidth;
import static com.amazon.apl.enums.PropertyKey.kPropertyFontFamily;
import static com.amazon.apl.enums.PropertyKey.kPropertyFontSize;
import static com.amazon.apl.enums.PropertyKey.kPropertyHighlightColor;
import static com.amazon.apl.enums.PropertyKey.kPropertyHint;
import static com.amazon.apl.enums.PropertyKey.kPropertyHintColor;
import static com.amazon.apl.enums.PropertyKey.kPropertyLang;
import static com.amazon.apl.enums.PropertyKey.kPropertyMaxLength;
import static com.amazon.apl.enums.PropertyKey.kPropertySecureInput;
import static com.amazon.apl.enums.PropertyKey.kPropertySelectOnFocus;
import static com.amazon.apl.enums.PropertyKey.kPropertySize;
import static com.amazon.apl.enums.PropertyKey.kPropertyText;
import static com.amazon.apl.enums.PropertyKey.kPropertyValidCharacters;

public abstract class EditTextProxy<B extends BoundObject>
        extends PropertyMap<B, PropertyKey> {


    /**
     * @return Color of the border. Defaults to transparent.
     */
    public int getBorderColor() {
        return getColor(kPropertyBorderColor);
    }

    /**
     * @return Width of the border. Defaults to 0.
     */
    public int getBorderWidth() {
        return getDimension(kPropertyBorderWidth).intValue();
    }

    /**
     * @return Width of the border. Defaults to 0.
     */
    public int getDrawnBorderWidth() {
        return getDimension(kPropertyDrawnBorderWidth).intValue();
    }

    /**
     * @return Text color. Defaults to <theme-dependent>.
     */
    public int getColor() {
        return getColor(kPropertyColor);
    }

    /**
     * @return The name of the font family. Defaults to sans-serif.
     */
    @NonNull
    public String getFontFamily() {
        return getString(kPropertyFontFamily);
    }

    /**
     * @return The name of the font language. Defaults to "".
     * For example to select the japanese characters of the "Noto Sans CJK" font family set lang to "ja-JP"
     */
    @NonNull
    public String getFontLanguage() {
        return getString(kPropertyLang);
    }

    /**
     * @return The size of the text. Defaults to 40dp.
     */
    public int getFontSize() {
        // TODO normalize with Text component
        return getDimension(kPropertyFontSize).intValue();
    }

    /**
     * @return The style of the font. Defaults to normal.
     */
    public FontStyle getFontStyle() {
        return FontStyle.valueOf(getEnum(PropertyKey.kPropertyFontStyle));
    }

    /**
     * @return Weight of the font. Defaults to normal.
     */
    public int getFontWeight() {
        return getInt(PropertyKey.kPropertyFontWeight);
    }

    /**
     * @return The highlight color to show behind selected text. Defaults to empty string.
     */
    public int getHighlightColor() {
        return getColor(kPropertyHighlightColor);
    }

    /**
     * @return Hint text to display when no text has been entered. Defaults to empty string.
     */
    @NonNull
    public String getHint() {
        return getString(kPropertyHint);
    }

    /**
     * @return The color of the hint text. Defaults to <theme-dependent>.
     */
    public int getHintColor() {
        return getColor(kPropertyHintColor);
    }

    public TypefaceResolver getTypefaceResolver() {
        return TypefaceResolver.getInstance();
    }

    /**
     * @return The style of the hint font. Defaults to normal.
     */
    public FontStyle getHintFontStyle() {
        return FontStyle.valueOf(getEnum(PropertyKey.kPropertyHintStyle));
    }

    /**
     * @return The weight of the hint font. Defaults to normal.
     */
    public int getHintFontWeight() {
        return getInt(PropertyKey.kPropertyHintWeight);
    }

    /**
     * @return The type of keyboard to display. Defaults to normal.
     */
    public KeyboardType getKeyboardType() {
        return KeyboardType.valueOf(getEnum(PropertyKey.kPropertyKeyboardType));
    }

    /**
     * @return The maximum number of characters that can be entered. Defaults to 0.
     */
    public int getMaxLength() {
        return getInt(kPropertyMaxLength);
    }

    /**
     * @return Hide characters as they are typed if true. Defaults to false.
     */
    public boolean isSecureInput() {
        return getBoolean(kPropertySecureInput);
    }

    /**
     * @return If true the text will be selected on a focus event. Defaults to false.
     */
    public boolean isSelectOnFocus() {
        return getBoolean(kPropertySelectOnFocus);
    }

    /**
     * @return Specifies approximately how many characters can be displayed. Defaults to 8.
     */
    public int getSize() {
        return getInt(kPropertySize);
    }

    /**
     * @return The label of the return key. Defaults to done.
     */
    public SubmitKeyType getSubmitKeyType() {
        return SubmitKeyType.valueOf(getEnum(PropertyKey.kPropertySubmitKeyType));
    }

    /**
     * @return The text to display. Defaults to empty string.
     */
    @NonNull
    public String getText() {
        return getString(kPropertyText);
    }

    /**
     * @return Restrict the characters that can be entered. Defaults to empty string.
     */
    @NonNull
    public String getValidCharacters() {
        return getString(kPropertyValidCharacters);
    }


    /**
     * @return creates and returns {@link TextPaint}.
     */
    @NonNull
    public TextPaint getTextPaint(final float density) {
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


    public String getMeasureText() {
        // Use size property to find the approximate width of the EditText component as per the spec:
        // https://developer.amazon.com/en-US/docs/alexa/alexa-presentation-language/apl-edittext.html#size
        StringBuilder dummyText = new StringBuilder(getSize());
        for (int i = 0; i < getSize(); i++) {
            dummyText.append("M");
        }
        return dummyText.toString();
    }

}
