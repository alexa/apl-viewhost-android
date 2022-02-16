/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.android;

import android.graphics.Typeface;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextDirectionHeuristic;
import android.text.TextDirectionHeuristics;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.SubscriptSpan;
import android.text.style.SuperscriptSpan;
import android.text.style.TypefaceSpan;
import android.text.style.UnderlineSpan;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amazon.apl.android.primitive.Dimension;
import com.amazon.apl.android.primitive.Rect;
import com.amazon.apl.android.primitive.StyledText;
import com.amazon.apl.android.text.LineSpan;
import com.amazon.common.BoundObject;
import com.amazon.apl.enums.Display;
import com.amazon.apl.enums.FontStyle;
import com.amazon.apl.enums.LayoutDirection;
import com.amazon.apl.enums.PropertyKey;
import com.amazon.apl.enums.SpanAttributeName;
import com.amazon.apl.enums.SpanType;
import com.amazon.apl.enums.TextAlign;
import com.amazon.apl.enums.TextAlignVertical;
import com.amazon.apl.enums.TokenType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public abstract class TextProxy<B extends BoundObject>
        extends PropertyMap<B, PropertyKey> {

    private static final String TAG = "TextProxy";

    private static final char WORD_JOINER_CHAR = '\u2060';
    private static final char WORD_BREAK_OPPORTUNITY_CHAR = '\u200B';

    public String getVisualHash() {
        return getString(PropertyKey.kPropertyVisualHash);
    }

    /**
     * Gets the scaling factor implied by the metrics transform
     *
     * @return The multiplier used to apply scaling to view-host dimensions
     *         (1.0 means no scaling, 2.0 means view-host dimensions are double those in core)
     */
    public float getScalingFactor() {
        return getMetricsTransform().toViewhost(1.f);
    }

    /**
     * Transform and add APL style span to Android Spannable.
     *
     * @param output SpannableStringBuilder to contain result.
     */
    private void addSpan(@NonNull SpannableStringBuilder output, int start, int end, SpanType type, Map<SpanAttributeName, Object> spanAttributes) {
        List<Object> textSpans = new ArrayList<>();
        switch (type) {
            case kSpanTypeLineBreak:
                output.append('\n');
                return;
            case kSpanTypeStrong:
                textSpans.add(new StyleSpan(Typeface.BOLD));
                break;
            case kSpanTypeItalic:
                textSpans.add(new StyleSpan(Typeface.ITALIC));
                break;
            case kSpanTypeStrike:
                textSpans.add(new StrikethroughSpan());
                break;
            case kSpanTypeUnderline:
                textSpans.add(new UnderlineSpan());
                break;
            case kSpanTypeMonospace:
                textSpans.add(new TypefaceSpan("monospace"));
                break;
            case kSpanTypeSuperscript:
                textSpans.add(new SuperscriptSpan());
                break;
            case kSpanTypeSubscript:
                textSpans.add(new SubscriptSpan());
                break;
            case kSpanTypeSpan:
                if (spanAttributes.get(SpanAttributeName.kSpanAttributeNameColor) != null) {
                    int cl = (int) spanAttributes.get(SpanAttributeName.kSpanAttributeNameColor);
                    textSpans.add(new ForegroundColorSpan(cl));
                }
                if (spanAttributes.get(SpanAttributeName.kSpanAttributeNameFontSize) != null) {
                    Dimension dimension = (Dimension) spanAttributes.get(SpanAttributeName.kSpanAttributeNameFontSize);
                    textSpans.add(new AbsoluteSizeSpan(dimension.intValue()));
                }
            default:
                if (textSpans.isEmpty()) {
                    textSpans.add(new StyleSpan(Typeface.NORMAL));
                }
                break;
        }
        for (Object textSpan : textSpans) {
            output.setSpan(textSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    private void handleStartSpan(Stack<SpanType> nobrTags, SpannableStringBuilder output, SpanType spanType) {
        if (spanType != SpanType.kSpanTypeNoBreak) return;

        if (nobrTags.empty()) {
            // Allow word breaking at the start of nobr but only if this is the outermost nobr tag
            output.append(String.valueOf(WORD_BREAK_OPPORTUNITY_CHAR));
        }
        nobrTags.push(SpanType.kSpanTypeNoBreak);
    }

    private void handleEndSpan(Stack<SpanType> nobrTags, SpannableStringBuilder output, SpanType spanType) {
        if (spanType != SpanType.kSpanTypeNoBreak) return;
        if (nobrTags.empty()) return;

        nobrTags.pop();

        if (nobrTags.empty() && output.length() > 0) {
            if (output.charAt(output.length() - 1) != WORD_JOINER_CHAR)
                Log.wtf(TAG, "<nobr> tag ended but there was no tailing word joiner char");
            // Trim end WORD_JOINER_CHAR
            output.delete(output.length() - 1, output.length());
        }
    }

    private String handleString(Stack<SpanType> nobrTags, String s) {
        if (nobrTags.empty()) return s;
        if (s.length() == 0) return s;

        // To prevent word breaks within nobr tags we add word joiner unicode chars
        s = s.replace("", String.valueOf(WORD_JOINER_CHAR));
        //strip first WORD_JOINER_CHAR
        s = s.substring(1, s.length());
        return s;
    }


    /**
     * Process APL StyledText into Android Spannable.
     *
     * @param styledText      StyledText to process.
     * @param currentLineSpan
     * @return SpannableStringBuilder with result.
     */
    @NonNull
    private SpannableStringBuilder processStyledText(@NonNull StyledText styledText, LineSpan currentLineSpan) {
        final SpannableStringBuilder output = new SpannableStringBuilder();
        StyledText.StyledTextIterator iter = styledText.new StyledTextIterator();

        final Stack<SpanType> nobrTags = new Stack<>();
        final Stack<StyledText.SpanTransition> openSpans = new Stack<>();
        final Stack<Integer> startPositions = new Stack<>();
        while (iter.hasNext()) {
            StyledText.SpanTransition it = iter.next();
            TokenType currToken = it.getTokenType();
            switch (currToken) {
                case kStartSpan:
                    openSpans.push(it);
                    startPositions.push(output.length());
                    handleStartSpan(nobrTags, output, it.getSpanType());
                    break;
                case kEndSpan:
                    int startPos = startPositions.pop();
                    StyledText.SpanTransition openSpan = openSpans.pop();
                    addSpan(output, startPos, output.length(), it.getSpanType(), openSpan.getAttributes());
                    handleEndSpan(nobrTags, output, it.getSpanType());
                    break;
                case kString:
                    String s = handleString(nobrTags, it.getString());
                    output.append(s);
                    break;
                case kEnd:
                    break;
            }
        }

        // Apply Karaoke span if we have a current line span
        if (currentLineSpan != null) {
            ForegroundColorSpan lineSpan = new ForegroundColorSpan(currentLineSpan.getColor());
            output.setSpan(lineSpan, currentLineSpan.getStart(), currentLineSpan.getEnd(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return output;
    }

    /**
     * @return Data to display in the text block.
     */
    public CharSequence getText(StyledText styledText, LineSpan currentLineSpan) {
        if (styledText != null) {
            final SpannableStringBuilder styled = processStyledText(styledText, currentLineSpan);
            return Spannable.Factory.getInstance().newSpannable(styled);
        } else {
            return "";
        }
    }

    public Layout.Alignment getTextAlignment() {
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
     * @return Text color. Defaults to #FAFAFA for a dark theme and #1E2222 for a light theme.
     */
    public int getColor() {
        return getColor(PropertyKey.kPropertyColor);
    }

    /**
     * @return Text color for the KaraokeTarget.
     */
    public int getColorKaraokeTarget() {
        return getColor(PropertyKey.kPropertyColorKaraokeTarget);
    }

    public boolean isItalic() {
        return FontStyle.kFontStyleItalic == getFontStyle();
    }

    /**
     * @return The name of the font family. Defaults to sans-serif in most markets.
     * Defaults to Noto Sans CJK in Japan.
     */
    @Nullable
    public String getFontFamily() {
        return getString(PropertyKey.kPropertyFontFamily);
    }

    /**
     * @return The name of the font language. Defaults to "".
     * For example to select the japanese characters of the "Noto Sans CJK" font family set lang to "ja-JP"
     */
    @NonNull
    public String getFontLanguage() {
        return getString(PropertyKey.kPropertyLang);
    }

    /**
     * @return The size of the text. Defaults to 40dp.
     */
    @Nullable
    public Dimension getFontSize() {
        return getDimension(PropertyKey.kPropertyFontSize);
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
     * @return Additional space to add between letters.
     */
    @Nullable
    public Dimension getLetterSpacing() {
        return getDimension(PropertyKey.kPropertyLetterSpacing);
    }


    /**
     * @return Line-height multiplier. Defaults to 125%.
     */
    public float getLineHeight() {
        return getFloat(PropertyKey.kPropertyLineHeight);
    }


    /**
     * @return Maximum number of lines to display. Defaults to 0 (indicates no maximum).
     */
    public int getMaxLines() {
        return getInt(PropertyKey.kPropertyMaxLines);
    }

    /**
     * @return Maximum number of lines to display. Defaults to 0 (indicates no maximum).
     */
    public boolean limitLines() {
        return getMaxLines() > 0;
    }

    /**
     * @return Horizontal alignment. Defaults to auto.
     */
    public TextAlign getTextAlign() {
        return TextAlign.valueOf(getEnum(PropertyKey.kPropertyTextAlign));
    }


    /**
     * @return TVertical alignment. Defaults to auto.
     */
    public TextAlignVertical getTextAlignVertical() {
        return TextAlignVertical.valueOf(getEnum(PropertyKey.kPropertyTextAlignVertical));
    }

    /**
     * @return the text to display.
     */
    public StyledText getStyledText() {
        return getStyledText(PropertyKey.kPropertyText);
    }

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
