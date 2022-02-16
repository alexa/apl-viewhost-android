/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.primitive;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amazon.common.BoundObject;
import com.amazon.apl.android.scaling.IMetricsTransform;
import com.amazon.apl.android.utils.ColorUtils;
import com.amazon.apl.enums.APLEnum;
import com.amazon.apl.enums.SpanAttributeName;
import com.amazon.apl.enums.SpanType;
import com.amazon.apl.enums.TokenType;
import com.google.auto.value.AutoValue;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.amazon.apl.enums.SpanAttributeName.kSpanAttributeNameColor;
import static com.amazon.apl.enums.SpanAttributeName.kSpanAttributeNameFontSize;

/**
 * StyledText.
 */
public class StyledText {

    private final String mText;
    private final long mNativeHandle;
    private final int mPropertyIndex;
    private final IMetricsTransform mTransform;
    private final List<Span> mSpans;

    /**
     * Get the text string of the styled text BEFORE the spans have been processed. This text can
     * differ from the styled text calculated by {@link com.amazon.apl.android.TextProxy#getText(StyledText)}
     */
    public String getUnprocessedText() {
        return mText;
    }

    private List<Span> buildSpans() {
        int size = nGetSpanCount(mNativeHandle, mPropertyIndex);
        List<Span> spans = new ArrayList<>(size);
        for (int i = 0; i < size; ++i) {
            Span s = Span.builder()
                    .type(SpanType.valueOf(nGetSpanTypeAt(mNativeHandle, mPropertyIndex, i)))
                    .start(nGetSpanStartAt(mNativeHandle, mPropertyIndex, i))
                    .end(nGetSpanEndAt(mNativeHandle, mPropertyIndex, i))
                    .build();
            spans.add(s);
        }
        return spans;
    }

    public StyledText(BoundObject boundObject, APLEnum propertyKey, IMetricsTransform mTransform) {
        this.mNativeHandle = boundObject.getNativeHandle();
        this.mPropertyIndex = propertyKey.getIndex();
        this.mTransform = mTransform;
        this.mText = nGetText(mNativeHandle, mPropertyIndex);
        this.mSpans = buildSpans();
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

        public StyledTextIterator() {
            this.nIteratorPtr = nCreateStyledTextIterator(mNativeHandle, mPropertyIndex);
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
                    builder.setAttributes(getSpanAttributes());
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

        private Map<SpanAttributeName, Object> getSpanAttributes() {
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
                        attrValue = Dimension.create(mTransform.toViewhost((float) dim));
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

    @NonNull
    private static native String nGetText(long componentHandle, int propertyKey);
    private static native int nGetSpanCount(long componentHandle, int propertyKey);
    private static native int nGetSpanTypeAt(long componentHandle, int propertyKey, int index);
    private static native int nGetSpanStartAt(long componentHandle, int propertyKey, int index);
    private static native int nGetSpanEndAt(long componentHandle, int propertyKey, int index);
    private static native long nCreateStyledTextIterator(long componentHandle, int propertyKey);
    private static native int nStyledTextIteratorNext(long nIteratorPtr);
    private static native byte[] nStyledTextIteratorGetString(long nIteratorPtr);
    private static native int nStyledTextIteratorGetSpanType(long nIteratorPtr);
    private static native int[] nStyledTextIteratorGetSpanAttributesNames(long nIteratorPtr);
    private static native long nStyledTextIteratorGetSpanAttributeGetColor(long nativeHandle, int attributeKey);
    private static native double nStyledTextIteratorGetSpanAttributeGetDimension(long nativeHandle, int attributeKey);
    private static native void nDestroyStyledTextIterator(long nIteratorPtr);
}
