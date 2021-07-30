/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android;

import android.graphics.Typeface;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.BoringLayout;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.StaticLayout;
import android.text.TextDirectionHeuristics;
import android.text.TextPaint;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.SubscriptSpan;
import android.text.style.SuperscriptSpan;
import android.text.style.TypefaceSpan;
import android.text.style.UnderlineSpan;
import android.util.Log;

import com.amazon.apl.android.primitive.Dimension;
import com.amazon.apl.android.primitive.Rect;
import com.amazon.apl.android.primitive.StyledText;
import com.amazon.apl.android.text.LineSpan;
import com.amazon.apl.android.text.TextMeasuringInput;
import com.amazon.apl.enums.FontStyle;
import com.amazon.apl.enums.LayoutDirection;
import com.amazon.apl.enums.PropertyKey;
import com.amazon.apl.enums.SpanAttributeName;
import com.amazon.apl.enums.SpanType;
import com.amazon.apl.enums.TextAlign;
import com.amazon.apl.enums.TextAlignVertical;
import com.amazon.apl.enums.TokenType;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * Creates a APL Text Component.
 * See @{link <a https://developer.amazon.com/docs/alexa-presentation-language/apl-text.html>
 * APL Text Specification</a>}
 */
public class Text extends Component implements ITextMeasurable {
    private static final String TAG = Text.class.getSimpleName();
    private static final char WORD_JOINER_CHAR = '\u2060';
    private static final char WORD_BREAK_OPPORTUNITY_CHAR = '\u200B';

    // The measured width in pixels
    private int mMeasuredWidthPx = 0;

    // The measured height in pixels
    private int mMeasuredHeightPx = 0;

    /**
     * Used for calculating the which line is highlighted
     */
    @Nullable
    private LineSpan mCurrentLineSpan;

    // Classic Karaoke
    private boolean mClassicKaraokeMode;

    // Classic Fonts
    private boolean mClassicFonts;

    private final ITextMeasurementCache mTextMeasurementCache;

    /**
     * Text constructor.
     * {@inheritDoc}
     */
    Text(long nativeHandle, String componentId, @NonNull RenderingContext renderingContext) {
        super(nativeHandle, componentId, renderingContext);
        mClassicKaraokeMode = mClassicFonts = (renderingContext.getDocVersion() < APLVersionCodes.APL_1_1);
        mTextMeasurementCache = renderingContext.getTextMeasurementCache();
    }

    /**
     * Measure the Text Component. This method builds a StaticLayout based on the Component
     * properties and saves it in {@link TextMeasurementCache}.
     *
     * @param density    The viewport density
     * @param widthPx    Horizontal pixel width dimension as imposed by the parent.
     * @param widthMode  Horizontal width requirements as imposed by the parent.
     * @param heightPx   Vertical pixel width dimension as imposed by the parent.
     * @param heightMode Vertical width requirements as imposed by the parent.
     */
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

        // In all cases for the width measureTextContent mode (undefined, at most, exactly) the text layout
        // will match or fit within the width dimension
        buildLayout(Math.round(widthPx), widthMode, Math.round(heightPx), density);
        final StaticLayout textLayout = mTextMeasurementCache.getStaticLayout(getComponentId());

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
            case Exactly:
                // force the height as directed by the parent
                mMeasuredHeightPx = Math.round(heightPx);
                break;
        }

        // Width mode is already taken into account when building the layout. No need to do extra magic.
        mMeasuredWidthPx = textLayout.getWidth();

        final TextMeasuringInput measuringInput = mTextMeasurementCache.getMeasuringInput(getComponentId());
        if (measuringInput != null) {
            measuringInput.updateMeasuredWidthAndHeight(mMeasuredWidthPx, mMeasuredHeightPx);
        }
    }

    @Nullable
    public String getLines() {
        final StaticLayout textLayout = mTextMeasurementCache.getStaticLayout(getComponentId());

        if (textLayout == null) {
            return null;
        }

        return textLayout.getText().toString();
    }

    @Nullable
    public int[] getSpans() {
        final StaticLayout textLayout = mTextMeasurementCache.getStaticLayout(getComponentId());

        if (textLayout == null) {
            return null;
        }
        int lineCount = textLayout.getLineCount();
        if (lineCount == 0) {
            return null;
        }
        int[] spans = new int[lineCount];
        for (int i = 0; i < lineCount; i++) {
            spans[i] = textLayout.getLineEnd(i);
        }
        return spans;
    }

    /**
     * @param presenter
     * @param line
     */
    public boolean setCurrentKaraokeLine(@NonNull IAPLViewPresenter presenter, @Nullable Integer line) {
        final StaticLayout textLayout = mTextMeasurementCache.getStaticLayout(getComponentId());

        final LineSpan oldLineSpan = mCurrentLineSpan;
        mCurrentLineSpan = null;

        if (textLayout == null || line == null) {
            return false;
        }
        final int start = textLayout.getLineStart(line);
        final int end = textLayout.getLineEnd(line);

        mCurrentLineSpan = new LineSpan(start, end, getColorKaraokeTarget());
        // Don't update unless we need to highlight a new line
        if (oldLineSpan != null && oldLineSpan.equals(mCurrentLineSpan)) {
            return false;
        }
        // Now update

        List<PropertyKey> dirtyProperties = new LinkedList<>();
        dirtyProperties.add(PropertyKey.kPropertyColorKaraokeTarget);
        presenter.onComponentChange(this, dirtyProperties);
        return true;
    }

    /**
     * Gets the bounds of a line
     *
     * @param line Line Number
     * @return The bounds
     */
    @NonNull
    public android.graphics.Rect getLineBounds(int line) {
        final StaticLayout textLayout = mTextMeasurementCache.getStaticLayout(getComponentId());

        if (textLayout == null) {
            return new android.graphics.Rect();
        }

        final Rect b = getInnerBounds();
        final int top = textLayout.getLineTop(line) + b.intTop();
        final int bottom = textLayout.getLineTop(line + 1) + b.intTop();
        final int left = b.intLeft();
        final int right = b.intRight();
        return new android.graphics.Rect(left, top, right, bottom);
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
            if (output.charAt(output.length()-1) != WORD_JOINER_CHAR)
                Log.wtf(TAG, "<nobr> tag ended but there was no tailing word joiner char");
            // Trim end WORD_JOINER_CHAR
            output.delete(output.length()-1, output.length());
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
     * @param styledText StyledText to process.
     * @return SpannableStringBuilder with result.
     */
    @NonNull
    private SpannableStringBuilder processStyledText(@NonNull StyledText styledText) {
        final SpannableStringBuilder output = new SpannableStringBuilder();
        StyledText.StyledTextIterator iter = styledText.new StyledTextIterator();

        final Stack<SpanType> nobrTags = new Stack<>();
        final Stack<StyledText.SpanTransition> openSpans = new Stack<>();
        final Stack<Integer> startPositions = new Stack<>();
        while(iter.hasNext()) {
            StyledText.SpanTransition it = iter.next();
            TokenType currToken = it.getTokenType();
            switch(currToken) {
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
        if (mCurrentLineSpan != null) {
            ForegroundColorSpan lineSpan = new ForegroundColorSpan(mCurrentLineSpan.getColor());
            output.setSpan(lineSpan, mCurrentLineSpan.getStart(), mCurrentLineSpan.getEnd(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return output;
    }

    /**
     * Creates a new StaticLayout.  The layout is used for measureTextContent, then added to
     * any TextView that represents this component.  Views may be recycled so no assumption
     * should be made that this layout remains assigned to the TextView.
     * <p>
     * Layout creation is optimized against input parameters. The assumption is: if input arguments
     * for {@link StaticLayout} are the same, then result {@link StaticLayout} does not change. The
     * output {@link StaticLayout} and {@link TextMeasuringInput} are cached in {@link TextMeasurementCache}.
     * <p>
     * {@link TextPaint} and {@link BoringLayout} are created before actual {@link StaticLayout}. Both
     * creations are expensive operations and in order to avoid duplicate job input arguments for
     * {@link TextPaint} and {@link BoringLayout} are evaluated against previously called ones.
     */
    private void buildLayout(int innerWidth, @NonNull RootContext.MeasureMode widthMode, int innerHeight,
                             float density) {

        // Step 1: get cached text layout and cached measuring input if those exist.
        final StaticLayout cachedTextLayout = mTextMeasurementCache.
                getStaticLayout(getComponentId());
        final TextMeasuringInput cachedMeasuringInput =
                mTextMeasurementCache.getMeasuringInput(getComponentId());

        // Step 2: create new measuring input for further comparison
        final TextMeasuringInput requestedMeasuringInput = prepareMeasuringInput(innerWidth, widthMode, innerHeight, density);

        // Step 3: Create or get text paint from requested measuring input. Then compare paints from
        // cached measuring input and current one. If input arguments required to create TextPaint are
        // the same then a cached TextPaint is returned.
        final TextPaint textPaint = mTextMeasurementCache.getTextPaint(requestedMeasuringInput);

        // Step 4: If paints and texts are equal for a new and a previous MeasuringInput, then we
        // will be using cached BoringLayout. Otherwise we will create a new one.
        final CharSequence text = getText(requestedMeasuringInput.getStyledText());
        final int boringTextWidth = calculateBoringLayoutWidth(cachedMeasuringInput, requestedMeasuringInput, text, textPaint);
        requestedMeasuringInput.updateBoringWidth(boringTextWidth);

        // Step 5: By this time we know all inputs and intermediate results. If all inputs are the
        // same, then result StaticLayout is the same too. So ignoring everything below.
        if (cachedMeasuringInput != null && cachedTextLayout != null && cachedMeasuringInput.equals(requestedMeasuringInput)) {
            return;
        }

        // Step 6: create a new StaticLayout and cache it.
        try {
            final StaticLayout textLayout = createStaticLayout(text, textPaint, requestedMeasuringInput,
                    widthMode, innerWidth, innerHeight, boringTextWidth);
            mTextMeasurementCache.put(getComponentId(), requestedMeasuringInput, textLayout);
        } catch (StaticLayoutBuilder.LayoutBuilderException e) {
            Log.wtf(TAG, "Layout build failed.", e);
        }

    }

    private int calculateBoringLayoutWidth(TextMeasuringInput cachedMeasuringInput,
                                           TextMeasuringInput requestedMeasuringInput, CharSequence text,
                                           TextPaint textPaint) {
        final boolean paintsEqual = cachedMeasuringInput != null && cachedMeasuringInput.paintsEqual(requestedMeasuringInput);
        final boolean textsEqual = cachedMeasuringInput != null && cachedMeasuringInput.textEqual(requestedMeasuringInput);

        if (paintsEqual && textsEqual) {
            return cachedMeasuringInput.getBoringWidth();
        } else {
            final BoringLayout.Metrics boring = BoringLayout.isBoring(text, textPaint);
            return (boring != null) ? boring.width :
                    (int) Math.ceil(Layout.getDesiredWidth(text, textPaint));
        }
    }

    private TextMeasuringInput prepareMeasuringInput(int innerWidth, @NonNull RootContext.MeasureMode widthMode, int innerHeight, float density) {
        return new TextMeasuringInput.Builder().
                innerWidth(innerWidth).
                innerHeight(innerHeight).
                text(getStyledText()).
                widthMode(widthMode).
                maxLines(mProperties.hasProperty(PropertyKey.kPropertyMaxLines) ? getMaxLines() : -1).
                lineHeight(getLineHeight()).
                textColor(getColor()).
                fontSize(getFontSize().intValue()).
                fontFamily(getFontFamily()).
                fontWeight(getFontWeight()).
                fontLanguage(getFontLanguage()).
                italic(FontStyle.kFontStyleItalic == getFontStyle()).
                letterSpacing(getLetterSpacing()).
                density(density).
                lineHeight(getLineHeight()).
                alignment(getTextAlignment()).
                classicFonts(mClassicFonts).
                karaokeLine(mCurrentLineSpan).
                shadowBlur(getShadowRadius()).
                shadowColor(getShadowColor()).
                shadowOffsetX(getShadowOffsetHorizontal()).
                shadowOffsetY(getShadowOffsetVertical()).
                display(getDisplay()).
                textDirectionHeuristic(getLayoutDirection() == LayoutDirection.kLayoutDirectionRTL ? TextDirectionHeuristics.RTL : TextDirectionHeuristics.LTR).
                build();
    }

    private Layout.Alignment getTextAlignment() {
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

    private StaticLayout createStaticLayout(CharSequence text, TextPaint textPaint, TextMeasuringInput measuringInput,
                                            RootContext.MeasureMode widthMode,
                                            int innerWidth, int innerHeight,
                                            int boringTextWidth) throws StaticLayoutBuilder.LayoutBuilderException {
        final int layoutWidth = getLayoutWidthForWidthMode(widthMode, innerWidth, boringTextWidth);
        final boolean limitLines = measuringInput.getLimitLines();
        final int maxLines = measuringInput.getMaxLines();
        StaticLayoutBuilder.Builder builder = StaticLayoutBuilder.create().
                                                        text(text).
                                                        textPaint(textPaint).
                                                        lineSpacing(measuringInput.getLineHeight()).
                                                        innerWidth(layoutWidth).
                                                        alignment(measuringInput.getAlignment()).
                                                        limitLines(limitLines).
                                                        maxLines(maxLines).
                                                        ellipsizedWidth(innerWidth).
                                                        textDirection(measuringInput.getTextDirectionHeuristic()).
                                                        aplVersionCode(getRenderingContext().getDocVersion());

        StaticLayout textLayout = builder.build();

        // In case the text does not fit into the view, we need to indicate the user that there is more
        // text remaining now being shown. We'd proceed truncating the text until the last fully
        // visible line with an ellipsis.
        //
        // After creating the layout, we are able to know the final height of
        // the entire spannable text. Truncates text if layout exceeds the view
        // box dimensions.
        final int staticLayoutHeight = textLayout.getHeight();
        measuringInput.updateUnadjustedMeasuredHeight(staticLayoutHeight);

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

    private int getLayoutWidthForWidthMode(RootContext.MeasureMode widthMode, int innerWidth, int boringTextWidth) {
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
     * @return Text color. Defaults to #FAFAFA for a dark theme and #1E2222 for a light theme.
     */
    final public int getColor() {
        if (mClassicKaraokeMode && mCurrentLineSpan != null) {
            return mProperties.getColor(PropertyKey.kPropertyColorNonKaraoke);
        }
        return mProperties.getColor(PropertyKey.kPropertyColor);
    }

    final public int getColorKaraokeTarget() {
        if (mClassicKaraokeMode && mCurrentLineSpan != null) {
            return mProperties.getColor(PropertyKey.kPropertyColor);
        }
        return mProperties.getColor(PropertyKey.kPropertyColorKaraokeTarget);
    }

    /**
     * @return The name of the font family. Defaults to sans-serif in most markets.
     * Defaults to Noto Sans CJK in Japan.
     */
    @Nullable
    final public String getFontFamily() {
        return mProperties.getString(PropertyKey.kPropertyFontFamily);
    }

    /**
     * @return The name of the font language. Defaults to "".
     * For example to select the japanese characters of the "Noto Sans CJK" font family set lang to "ja-JP"
     */
    @NonNull
    public String getFontLanguage() {
        return mProperties.getString(PropertyKey.kPropertyLang);
    }

    /**
     * @return The size of the text. Defaults to 40dp.
     */
    @Nullable
    final public Dimension getFontSize() {
        return mProperties.getDimension(PropertyKey.kPropertyFontSize);
    }

    /**
     * @return The style of the font. Defaults to normal.
     */
    final public FontStyle getFontStyle() {
        return FontStyle.valueOf(mProperties.getEnum(PropertyKey.kPropertyFontStyle));
    }

    /**
     * @return Weight of the font. Defaults to normal.
     */
    final public int getFontWeight() {
        return mProperties.getInt(PropertyKey.kPropertyFontWeight);
    }


    /**
     * @return Additional space to add between letters.
     */
    @Nullable
    final public Dimension getLetterSpacing() {
        return mProperties.getDimension(PropertyKey.kPropertyLetterSpacing);
    }


    /**
     * @return Line-height multiplier. Defaults to 125%.
     */
    final public float getLineHeight() {
        return mProperties.getFloat(PropertyKey.kPropertyLineHeight);
    }


    /**
     * @return Maximum number of lines to display. Defaults to 0 (indicates no maximum).
     */
    final public int getMaxLines() {
        return mProperties.getInt(PropertyKey.kPropertyMaxLines);
    }


    /**
     * @return Data to display in the text block.
     */
    public final CharSequence getText(StyledText styledText) {
        if (styledText != null) {
            final SpannableStringBuilder styled = processStyledText(styledText);
            return Spannable.Factory.getInstance().newSpannable(styled);
        } else {
            return "";
        }
    }


    /**
     * @return Horizontal alignment. Defaults to auto.
     */
    public final TextAlign getTextAlign() {
        return TextAlign.valueOf(mProperties.getEnum(PropertyKey.kPropertyTextAlign));
    }


    /**
     * @return TVertical alignment. Defaults to auto.
     */
    final public TextAlignVertical getTextAlignVertical() {
        return TextAlignVertical.valueOf(mProperties.getEnum(PropertyKey.kPropertyTextAlignVertical));
    }

    /**
     * @return the text to display.
     */
    final public StyledText getStyledText() {
        return mProperties.getStyledText(PropertyKey.kPropertyText);
    }

    /***
     * The pixel width of this layout as calculated by
     * {@link #measureTextContent(float, boolean, float, RootContext.MeasureMode, float, RootContext.MeasureMode)}
     *
     * @return The raw measured width of this Text layout.
     */
    @Override
    public int getMeasuredWidthPx() {
        return mMeasuredWidthPx;
    }


    /***
     * The pixel height of this layout as calculated by
     * {@link #measureTextContent(float, boolean, float, RootContext.MeasureMode, float, RootContext.MeasureMode)}
     *
     * @return The raw measured width of this Text layout.
     */
    @Override
    public int getMeasuredHeightPx() {
        return mMeasuredHeightPx;
    }

    @Override
    public boolean shouldDrawBoxShadow() {
        return false;
    }

    public ITextMeasurementCache getTextMeasurementCache() {
        return mTextMeasurementCache;
    }
}
