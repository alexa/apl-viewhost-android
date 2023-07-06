/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android;


import android.text.Layout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.amazon.apl.android.primitive.Rect;
import com.amazon.apl.android.scaling.IMetricsTransform;
import com.amazon.apl.android.text.LineSpan;
import com.amazon.apl.enums.PropertyKey;

import java.util.LinkedList;
import java.util.List;


/**
 * Creates a APL Text Component.
 * See @{link <a https://developer.amazon.com/docs/alexa-presentation-language/apl-text.html>
 * APL Text Specification</a>}
 */
public class Text extends Component {

    /**
     * Used for calculating the which line is highlighted
     */
    @Nullable
    private LineSpan mCurrentLineSpan;

    // Color properties are interpreted differently in older APL versions during karaoke
    private boolean mClassicKaraokeMode;

    /**
     * Text constructor.
     * {@inheritDoc}
     */
    Text(long nativeHandle, String componentId, @NonNull RenderingContext renderingContext) {
        super(nativeHandle, componentId, renderingContext);
        mClassicKaraokeMode = (renderingContext.getDocVersion() < APLVersionCodes.APL_1_1);
    }

    /**
     * Override the standard component property map to use a TextProxy.
     * The text proxy has Text specific property getters.
     * @return
     */
    @Override
    protected PropertyMap createPropertyMap() {
        return new TextProxy<Text>() {
            @NonNull
            @Override
            public Text getMapOwner() {
                return Text.this;
            }

            @NonNull
            @Override
            public IMetricsTransform getMetricsTransform() {
                return getRenderingContext().getMetricsTransform();
            }

            @Override
            public int getColor() {
                if (mClassicKaraokeMode && mCurrentLineSpan != null) {
                    return getColor(PropertyKey.kPropertyColorNonKaraoke);
                }
                return super.getColor();
            }
        };
    }

    public TextProxy getProxy() {
        return (TextProxy) mProperties;
    }

    @Nullable
    public String getLines() {
        final Layout textLayout = getTextLayout();

        if (textLayout == null) {
            return null;
        }

        return textLayout.getText().toString();
    }

    @Nullable
    public void invalidateNullLine(@NonNull IAPLViewPresenter presenter){
        List<PropertyKey> dirtyProperties = new LinkedList<>();
        dirtyProperties.add(PropertyKey.kPropertyColorKaraokeTarget);
        presenter.onComponentChange(this,dirtyProperties);
    }

    @Nullable
    public int[] getSpans() {
        final Layout textLayout = getTextLayout();

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
        final Layout textLayout = getTextLayout();

        final LineSpan oldLineSpan = mCurrentLineSpan;
        mCurrentLineSpan = null;

        if (textLayout == null || line == null) {
            return false;
        }
        final int start = textLayout.getLineStart(line);
        final int end = textLayout.getLineEnd(line);

        mCurrentLineSpan = new LineSpan(start, end, getProxy().getColorKaraokeTarget());
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
        final Layout textLayout = getTextLayout();

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

    public int calculateCharacterOffsetByRange(int rangeStart, int rangeEnd) {
        // rangeStart and rangeEnd are byte ranges, so we need to convert them to character ranges.

        // Since the ranges are inclusive, we need to add 1 to turn it into a size.
        int rangeSize = (rangeEnd - rangeStart) + 1;

        // Count the number of utf8 codepoints between 0 and rangeStart.
        int characterRangeStart = nCountCharactersInRange(getNativeHandle(), 0, rangeStart);

        // Similar to the above, we also need to count the number of utf8 codepoints for rangeEnd. However, we know the number of code points
        // for rangeStart, so we just need to count the ones between rangeStart and rangeEnd.
        int characterRangeCount = nCountCharactersInRange(getNativeHandle(), rangeStart, rangeSize);

        // Subtract 1 to turn it back into an inclusive range.
        int characterRangeEnd = (characterRangeStart + characterRangeCount - 1);

        // -1 is a failure due to formatting or bad range.
        if (characterRangeStart == -1 || characterRangeCount == -1) {
            return -1;
        }

        // Keeping previous behaviour to get the character offset in the middle of the range.
        return (characterRangeStart + characterRangeEnd) / 2;
    }

    /**
     * Gets the line number by range
     */
    public int getLineNumberByRange(int rangeStart, int rangeEnd) {
        final Layout textLayout = getTextLayout();

        if (textLayout == null) {
            return -1;
        }

        return textLayout.getLineForOffset(calculateCharacterOffsetByRange(rangeStart, rangeEnd));
    }

    private Layout getTextLayout() {
        final Rect bounds = getProxy().getInnerBounds();
        return getRenderingContext().getTextLayoutFactory().getOrCreateTextLayout(
                getRenderingContext().getDocVersion(),
                getProxy(),
                bounds.intWidth(),
                TextMeasure.MeasureMode.Exactly,
                bounds.intHeight(),
                getKaraokeLineSpan()
        );
    }

    @Override
    public boolean shouldDrawBoxShadow() {
        return false;
    }

    public LineSpan getKaraokeLineSpan() {
        return mCurrentLineSpan;
    }

    /**
     * Raw text for test utilities.
     */
    @VisibleForTesting
    public String getText() {
        return getProxy().getStyledText().getUnprocessedText();
    }

    private static native int nCountCharactersInRange(long nativeHandle, int index, int count);
}
