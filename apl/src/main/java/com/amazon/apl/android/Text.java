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
import com.amazon.apl.android.utils.APLTextUtil;
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
     * Gets the line number by range
     */
    public int getLineNumberByRange(int rangeStart, int rangeEnd) {
        final Layout textLayout = getTextLayout();

        if (textLayout == null) {
            return -1;
        }

        int[] characterRange = APLTextUtil.calculateCharacterOffsetByRange(getText(), rangeStart, rangeEnd);

        return textLayout.getLineForOffset((characterRange[0] + characterRange[1]) / 2);
    }

    private Layout getTextLayout() {
        final Rect bounds = getProxy().getInnerBounds();
        // bounds dimensions are in pixel values, convert to dp before layout creation
        IMetricsTransform metricsTransform = getRenderingContext().getMetricsTransform();
        return getRenderingContext().getTextLayoutFactory().getOrCreateTextLayout(
                getRenderingContext().getDocVersion(),
                getProxy(),
                metricsTransform.toCore(bounds.getWidth()),
                TextMeasure.MeasureMode.Exactly,
                metricsTransform.toCore(bounds.getHeight()),
                TextMeasure.MeasureMode.Exactly,
                getKaraokeLineSpan(),
                getRenderingContext().getMetricsTransform()
        ).getLayout();
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
}
