/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.android;

import android.text.TextDirectionHeuristic;
import android.text.TextDirectionHeuristics;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amazon.apl.android.primitive.Dimension;
import com.amazon.apl.android.primitive.Rect;
import com.amazon.apl.android.primitive.StyledText;
import com.amazon.common.BoundObject;
import com.amazon.apl.enums.Display;
import com.amazon.apl.enums.FontStyle;
import com.amazon.apl.enums.LayoutDirection;
import com.amazon.apl.enums.PropertyKey;
import com.amazon.apl.enums.TextAlign;
import com.amazon.apl.enums.TextAlignVertical;


public abstract class TextProxy<B extends BoundObject>
        extends PropertyMap<B, PropertyKey> implements ITextProxy {

    private static final String TAG = "TextProxy";

    public static final char WORD_JOINER_CHAR = '\u2060';
    public static final char WORD_BREAK_OPPORTUNITY_CHAR = '\u200B';

    public String getVisualHash() {
        return getString(PropertyKey.kPropertyVisualHash);
    }

    @Override
    public float getScalingFactor() {
        return getMetricsTransform().toViewhost(1.f);
    }

    @Override
    public int getColor() {
        return getColor(PropertyKey.kPropertyColor);
    }

    /**
     * @return Text color for the KaraokeTarget.
     */
    public int getColorKaraokeTarget() {
        return getColor(PropertyKey.kPropertyColorKaraokeTarget);
    }

    @Override
    @Nullable
    public String getFontFamily() {
        return getString(PropertyKey.kPropertyFontFamily);
    }

    @Override
    @NonNull
    public String getFontLanguage() {
        return getString(PropertyKey.kPropertyLang);
    }

    @Override
    public float getFontSize() {
        return getDimension(PropertyKey.kPropertyFontSize).value();
    }

    @Override
    public FontStyle getFontStyle() {
        return FontStyle.valueOf(getEnum(PropertyKey.kPropertyFontStyle));
    }

    @Override
    public int getFontWeight() {
        return getInt(PropertyKey.kPropertyFontWeight);
    }

    @Override
    @Nullable
    public Dimension getLetterSpacing() {
        return getDimension(PropertyKey.kPropertyLetterSpacing);
    }

    @Override
    public float getLineHeight() {
        return getFloat(PropertyKey.kPropertyLineHeight);
    }

    @Override
    public int getMaxLines() {
        return getInt(PropertyKey.kPropertyMaxLines);
    }

    @Override
    public TextAlign getTextAlign() {
        return TextAlign.valueOf(getEnum(PropertyKey.kPropertyTextAlign));
    }

    @Override
    public TextAlignVertical getTextAlignVertical() {
        return TextAlignVertical.valueOf(getEnum(PropertyKey.kPropertyTextAlignVertical));
    }

    /**
     * @return the text to display.
     */
    public StyledText getStyledText() {
        return getStyledText(PropertyKey.kPropertyText);
    }

    @Override
    public TextDirectionHeuristic getDirectionHeuristic() {
        TextDirectionHeuristic textDirectionHeuristic = getLayoutDirection() == LayoutDirection.kLayoutDirectionRTL
                ? TextDirectionHeuristics.RTL
                : TextDirectionHeuristics.LTR;
        return textDirectionHeuristic;
    }

    //TODO ////////  Below items Copied from Component - move to ComponentProxy //////////////////

    /**
     * @return The component inner bounds in DP. This is the bounds minus padding.
     */
    public Rect getInnerBounds() {
        return getRect(PropertyKey.kPropertyInnerBounds);
    }

    /**
     * @return the layout direction
     */
    public LayoutDirection getLayoutDirection() {
        return LayoutDirection.valueOf(getEnum(PropertyKey.kPropertyLayoutDirection));
    }

    /**
     * @return Control if the component is displayed on the screen.
     */
    public Display getDisplay() {
        return Display.valueOf(getEnum(PropertyKey.kPropertyDisplay));
    }

    /**
     * @return The horizontal offset of the Component shadow in pixels.
     */
    public int getShadowOffsetHorizontal() {
        Dimension top = getDimension(PropertyKey.kPropertyShadowHorizontalOffset);
        return top == null ? 0 : top.intValue();
    }

    /**
     * @return The vertical offset of the Component shadow in pixels.
     */
    public int getShadowOffsetVertical() {
        Dimension offset = getDimension(PropertyKey.kPropertyShadowVerticalOffset);
        return offset == null ? 0 : offset.intValue();
    }

    /**
     * @return The blur radius of the Component shadow in pixels.
     */
    public int getShadowRadius() {
        Dimension radius = getDimension(PropertyKey.kPropertyShadowRadius);
        return radius == null ? 0 : radius.intValue();
    }

    /**
     * @return The Component shadow color. Defaults to transparent.
     */
    public int getShadowColor() {
        return getColor(PropertyKey.kPropertyShadowColor);
    }

}
