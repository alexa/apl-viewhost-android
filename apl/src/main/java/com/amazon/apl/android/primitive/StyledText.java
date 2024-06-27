/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.primitive;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amazon.apl.android.TextProxy;
import com.amazon.apl.android.scaling.IMetricsTransform;
import com.amazon.apl.android.text.LineSpan;
import com.amazon.apl.android.utils.ColorUtils;
import com.amazon.apl.enums.APLEnum;
import com.amazon.apl.enums.SpanAttributeName;
import com.amazon.apl.enums.SpanType;
import com.amazon.apl.enums.TokenType;
import com.amazon.common.BoundObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import static com.amazon.apl.enums.SpanAttributeName.kSpanAttributeNameColor;
import static com.amazon.apl.enums.SpanAttributeName.kSpanAttributeNameFontSize;

import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.SubscriptSpan;
import android.text.style.SuperscriptSpan;
import android.text.style.TypefaceSpan;
import android.text.style.UnderlineSpan;
import android.util.Log;
import com.google.auto.value.AutoValue;

/**
 * StyledText.
 */
public class StyledText {
    private static final String TAG = "StyledText";
    private final String mText;
    private final long mNativeHandle;
    private int mPropertyIndex = -1;
    private final List<Span> mSpans;

    public StyledText(BoundObject boundObject, APLEnum propertyKey) {
        this.mNativeHandle = boundObject.getNativeHandle();
        this.mPropertyIndex = propertyKey.getIndex();
        this.mText = nGetText(mNativeHandle, mPropertyIndex);
        this.mSpans = buildSpans();
    }

    public StyledText(long nativeHandle) {
        this.mNativeHandle = nativeHandle;
        this.mText = nGetTextFromChunk(mNativeHandle);
        this.mSpans = buildSpans();
    }

    /**
     * Get the text string of the styled text BEFORE the spans have been processed. This text can
     * differ from the styled text calculated by {@link com.amazon.apl.android.TextProxy#getText(StyledText)}
     */
    public String getUnprocessedText() {
        return mText;
    }

    public String getHash() {
        return String.valueOf(nGetHash(mNativeHandle));
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        if (!(other instanceof StyledText)) {
            return false;
        }

        StyledText s = (StyledText) other;
        return mSpans.equals(s.mSpans) && mText.equals(s.mText);
    }

    /**
     * APL Core StyledText representation.
     * @see StyledText
     */
    @AutoValue
    protected static abstract class Span {
        public abstract SpanType type();
        public abstract int start();
        public abstract int end();
        public static Builder builder() {
            return new AutoValue_StyledText_Span.Builder();
        }

        @AutoValue.Builder
        static abstract class Builder {
            abstract Builder type(SpanType type);
            abstract Builder start(int start);
            abstract Builder end(int end);
            abstract Span build();
        }
    }

    @AutoValue
    public static abstract class SpanTransition {
        public abstract TokenType getTokenType();
        @Nullable
        public abstract String getString();
        @Nullable
        public abstract SpanType getSpanType();
        @Nullable
        public abstract Map<SpanAttributeName, Object> getAttributes();

        static Builder builder() {
            return new AutoValue_StyledText_SpanTransition.Builder();
        }

        @AutoValue.Builder
        static abstract class Builder {
            abstract SpanTransition.Builder setTokenType(TokenType type);
            abstract SpanTransition.Builder setString(String string);
            abstract SpanTransition.Builder setSpanType(SpanType type);
            abstract SpanTransition.Builder setAttributes(Map<SpanAttributeName, Object> attributes);
            abstract SpanTransition build();
        }
    }

    public class StyledTextIterator implements Iterator<SpanTransition> {
        private final long nIteratorPtr;
        private SpanTransition mCurrentTransition;

        IMetricsTransform mMetricsTransform;

        public StyledTextIterator(IMetricsTransform metricsTransform) {
            this.mMetricsTransform = metricsTransform;
            this.nIteratorPtr = createStyledTextIterator();
        }

        @Override
        public boolean hasNext() {
            return mCurrentTransition == null || mCurrentTransition.getTokenType() != TokenType.kEnd;
        }

        @Override
        public SpanTransition next() {
            TokenType tt = TokenType.valueOf(nStyledTextIteratorNext(this.nIteratorPtr));
            SpanTransition.Builder builder = SpanTransition.builder().setTokenType(tt);
            switch(tt) {
                case kStartSpan:
                    builder.setSpanType(SpanType.valueOf(nStyledTextIteratorGetSpanType(this.nIteratorPtr)));
                    builder.setAttributes(getSpanAttributes(mMetricsTransform));
                    break;
                case kEndSpan:
                    builder.setSpanType(SpanType.valueOf(nStyledTextIteratorGetSpanType(this.nIteratorPtr)));
                    break;
                case kString:
                    String str = new String(nStyledTextIteratorGetString(this.nIteratorPtr), StandardCharsets.UTF_8);
                    builder.setString(str);
                    break;
                case kEnd:
                    break;
            }
            mCurrentTransition = builder.build();
            return mCurrentTransition;
        }

        private Map<SpanAttributeName, Object> getSpanAttributes(IMetricsTransform metricsTransform) {
            int[] attrNames = nStyledTextIteratorGetSpanAttributesNames(this.nIteratorPtr);
            Map<SpanAttributeName, Object> spanAttributes = new HashMap<>();
            for (int attrName : attrNames) {
                Object attrValue = null;
                SpanAttributeName spanAttributeName = SpanAttributeName.valueOf(attrName);
                switch (spanAttributeName) {
                    case kSpanAttributeNameColor:
                        long value = nStyledTextIteratorGetSpanAttributeGetColor(this.nIteratorPtr, kSpanAttributeNameColor.getIndex());
                        attrValue = ColorUtils.toARGB(value);
                        break;
                    case kSpanAttributeNameFontSize:
                        double dim = nStyledTextIteratorGetSpanAttributeGetDimension(this.nIteratorPtr, kSpanAttributeNameFontSize.getIndex());
                        attrValue = Dimension.create(metricsTransform.toViewhost((float) dim));
                        break;
                }
                spanAttributes.put(spanAttributeName, attrValue);
            }
            return spanAttributes;
        }

        /**
         * The StyledTextIterator will only exists for the scope of the Text::processStyledText
         * method. To keep things simple we allocate the c++ StyledText::Iterator in the constructor
         * of this class and free it when the garbage collector calls finalize
         */
        @Override
        public void finalize() {
            nDestroyStyledTextIterator(this.nIteratorPtr);
        }
    }

    private List<Span> buildSpans() {
        int size = getSpanCount();
        List<Span> spans = new ArrayList<>(size);
        for (int i = 0; i < size; ++i) {
            Span s = Span.builder()
                    .type(SpanType.valueOf(getSpanTypeAt(i)))
                    .start(getSpanStartAt(i))
                    .end(getSpanEndAt(i))
                    .build();
            spans.add(s);
        }
        return spans;
    }

    /**
     * @return Data to display in the text block.
     */
    public CharSequence getText(LineSpan currentLineSpan, IMetricsTransform metricsTransform) {
        final SpannableStringBuilder styled = processStyledText(currentLineSpan, metricsTransform);
        return Spannable.Factory.getInstance().newSpannable(styled);
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
            output.append(String.valueOf(TextProxy.WORD_BREAK_OPPORTUNITY_CHAR));
        }
        nobrTags.push(SpanType.kSpanTypeNoBreak);
    }

    private void handleEndSpan(Stack<SpanType> nobrTags, SpannableStringBuilder output, SpanType spanType) {
        if (spanType != SpanType.kSpanTypeNoBreak) return;
        if (nobrTags.empty()) return;

        nobrTags.pop();

        if (nobrTags.empty() && output.length() > 0) {
            if (output.charAt(output.length() - 1) != TextProxy.WORD_JOINER_CHAR)
                Log.wtf(TAG, "<nobr> tag ended but there was no tailing word joiner char");
            // Trim end WORD_JOINER_CHAR
            output.delete(output.length() - 1, output.length());
        }
    }

    private String handleString(Stack<SpanType> nobrTags, String s) {
        if (nobrTags.empty()) return s;
        if (s.length() == 0) return s;

        // To prevent word breaks within nobr tags we add word joiner unicode chars
        s = s.replace("", String.valueOf(TextProxy.WORD_JOINER_CHAR));
        //strip first WORD_JOINER_CHAR
        s = s.substring(1, s.length());
        return s;
    }

    @NonNull
    private SpannableStringBuilder processStyledText(LineSpan currentLineSpan, IMetricsTransform metricsTransform) {
        final SpannableStringBuilder output = new SpannableStringBuilder();
        StyledText.StyledTextIterator iter = this.new StyledTextIterator(metricsTransform);

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

    ////////////////////////////////// JNI type wrappers //////////////////////////////////////////
    private int getSpanCount() {
        if (mPropertyIndex < 0) {
            return nGetSpanCountFromChunk(mNativeHandle);
        } else {
            return nGetSpanCount(mNativeHandle, mPropertyIndex);
        }
    }

    private int getSpanTypeAt(int index) {
        if (mPropertyIndex < 0) {
            return nGetSpanTypeAtFromChunk(mNativeHandle, index);
        } else {
            return nGetSpanTypeAt(mNativeHandle, mPropertyIndex, index);
        }
    }

    private int getSpanStartAt(int index) {
        if (mPropertyIndex < 0) {
            return nGetSpanStartAtFromChunk(mNativeHandle, index);
        } else {
            return nGetSpanStartAt(mNativeHandle, mPropertyIndex, index);
        }
    }

    private int getSpanEndAt(int index) {
        if (mPropertyIndex < 0) {
            return nGetSpanEndAtFromChunk(mNativeHandle, index);
        } else {
            return nGetSpanEndAt(mNativeHandle, mPropertyIndex, index);
        }
    }

    private long createStyledTextIterator() {
        if (mPropertyIndex < 0) {
            return nCreateStyledTextIteratorFromChunk(mNativeHandle);
        } else {
            return nCreateStyledTextIterator(mNativeHandle, mPropertyIndex);
        }
    }

    ////////////////////////////////// JNI methods //////////////////////////////////////////
    private static native int nStyledTextIteratorNext(long nIteratorPtr);
    private static native byte[] nStyledTextIteratorGetString(long nIteratorPtr);
    private static native int nStyledTextIteratorGetSpanType(long nIteratorPtr);
    private static native int[] nStyledTextIteratorGetSpanAttributesNames(long nIteratorPtr);
    private static native long nStyledTextIteratorGetSpanAttributeGetColor(long nativeHandle, int attributeKey);
    private static native double nStyledTextIteratorGetSpanAttributeGetDimension(long nativeHandle, int attributeKey);
    private static native void nDestroyStyledTextIterator(long nIteratorPtr);

    @NonNull
    private static native String nGetText(long componentHandle, int propertyKey);
    private static native int nGetSpanCount(long componentHandle, int propertyKey);
    private static native int nGetSpanTypeAt(long componentHandle, int propertyKey, int index);
    private static native int nGetSpanStartAt(long componentHandle, int propertyKey, int index);
    private static native int nGetSpanEndAt(long componentHandle, int propertyKey, int index);
    private static native long nCreateStyledTextIterator(long componentHandle, int propertyKey);

    @NonNull
    private static native String nGetTextFromChunk(long chunkHandle);
    private static native int nGetSpanCountFromChunk(long chunkHandle);
    private static native int nGetSpanTypeAtFromChunk(long chunkHandle, int index);
    private static native int nGetSpanStartAtFromChunk(long chunkHandle, int index);
    private static native int nGetSpanEndAtFromChunk(long chunkHandle, int index);
    private static native long nCreateStyledTextIteratorFromChunk(long componentHandle);
    private static native long nGetHash(long componentHandle);
}
