/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.graphic;

import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;

import androidx.annotation.NonNull;

import com.amazon.apl.android.PropertyMap;
import com.amazon.apl.android.RenderingContext;
import com.amazon.apl.android.font.TypefaceResolver;
import com.amazon.apl.enums.FontStyle;
import com.amazon.apl.enums.GraphicLayoutDirection;
import com.amazon.apl.enums.GraphicPropertyKey;
import com.amazon.apl.enums.GraphicTextAnchor;

import static com.amazon.apl.enums.GraphicPropertyKey.kGraphicPropertyCoordinateX;
import static com.amazon.apl.enums.GraphicPropertyKey.kGraphicPropertyCoordinateY;
import static com.amazon.apl.enums.GraphicPropertyKey.kGraphicPropertyFill;
import static com.amazon.apl.enums.GraphicPropertyKey.kGraphicPropertyFontFamily;
import static com.amazon.apl.enums.GraphicPropertyKey.kGraphicPropertyFontSize;
import static com.amazon.apl.enums.GraphicPropertyKey.kGraphicPropertyFontStyle;
import static com.amazon.apl.enums.GraphicPropertyKey.kGraphicPropertyFontWeight;
import static com.amazon.apl.enums.GraphicPropertyKey.kGraphicPropertyLetterSpacing;
import static com.amazon.apl.enums.GraphicPropertyKey.kGraphicPropertyStroke;
import static com.amazon.apl.enums.GraphicPropertyKey.kGraphicPropertyText;
import static com.amazon.apl.enums.GraphicPropertyKey.kGraphicPropertyTextAnchor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.SortedSet;

/**
 * Represents text avg object.
 */
public class GraphicTextElement extends FillStrokeGraphicElement {

    /**
     * Cached variables needed for drawing.
     */
    private int mX;
    private int mY;

    private Rect mBounds;

    private GraphicTextElement(@NonNull GraphicElementMap map, long nativeHandle, RenderingContext renderingContext) {
        super(map, nativeHandle, renderingContext);
    }

    static GraphicTextElement create(@NonNull GraphicElementMap map, long graphicHandle, RenderingContext renderingContext) {
        return new GraphicTextElement(map, graphicHandle, renderingContext);
    }

    /**
     * The font family of the text element.
     * @return the value of the font family of the text element.
     */
    String getFontFamily() {
        return mProperties.getString(kGraphicPropertyFontFamily);
    }

    /**
     * The font size of the text element.
     * @return the value of the font size of the text element.
     */
    int getFontSize() {
        return mProperties.getInt(kGraphicPropertyFontSize);
    }

    /**
     * The font weight of the text element.
     * @return the value of the font weight of the text element.
     */
    int getFontWeight() {
        return mProperties.getInt(kGraphicPropertyFontWeight);
    }

    /**
     * The font style of the text element.
     * @return the value of the font style of the text element.
     */
    int getFontStyle() {
        return mProperties.getInt(kGraphicPropertyFontStyle);
    }

    /**
     * The letter spacing of the text element.
     * @return the value of the letter spacing of the text element.
     */
    float getLetterSpacing() {
        return mProperties.getFloat(kGraphicPropertyLetterSpacing);
    }

    /**
     * The text of the text element.
     * @return the value of the text of the text element.
     */
    String getText() {
        return mProperties.getString(kGraphicPropertyText);
    }

    /**
     * The text anchor of the text element.
     * @return the value of the text anchor of the text element.
     */
    int getTextAnchor() {
        return mProperties.getInt(kGraphicPropertyTextAnchor);
    }

    /**
     * The coordinate x of the text element.
     * @return the value of the coordinate x of the text element.
     */
    int getCoordinateX() {
        return mProperties.getInt(kGraphicPropertyCoordinateX);
    }

    /**
     * The coordinate y of the text element.
     * @return the value of the coordinate y of the text element.
     */
    int getCoordinateY() {
        return mProperties.getInt(kGraphicPropertyCoordinateY);
    }

    /**
     * @return the x coordinate for this text element.
     */
    int getX() {
        return mX;
    }

    /**
     * @return the y coordinate for this text element.
     */
    int getY() {
        return mY;
    }

    /**
     * Update cached properties when Graphic is marked dirty.
     */
    @Override
    void applyProperties() {
        // Convert the set of dirty properties to a set for faster lookups.
        HashSet dirtyProperties = nGetDirtyProperties(this.getNativeHandle());

        Rect existingTextBoundingBox = mBounds != null ? calculateTextBoundingBox() : null;

        // This needs to happen first because both `applyXCoordinate` and `applyFillPaint` need it.
        calculateBounds();

        applyXCoordinate();

        applyYCoordinate();

        Rect calculatedTextBoundingBox = calculateTextBoundingBox();
        Boolean boundingBoxChanged = !calculatedTextBoundingBox.equals(existingTextBoundingBox);

        // Properties need to be applied in this order as fill paint will determine bounds
        // and is needed by stroke paint.
        applyFillPaint(calculatedTextBoundingBox, boundingBoxChanged, dirtyProperties);

        applyStrokePaint(calculatedTextBoundingBox, boundingBoxChanged, dirtyProperties);
    }

    private void calculateBounds() {
        applyFontPropsToPaint(mFillPaint);
        mBounds = new Rect();
        mFillPaint.getTextBounds(getText(), 0, getText().length(), mBounds);
    }

    private void applyFillPaint(Rect textBoundingBox, Boolean boundsChanged, HashSet dirtyProperties) {
        applyFillPaintProperties(textBoundingBox, boundsChanged, dirtyProperties);
    }

    private void applyStrokePaint(Rect textBoundingBox, Boolean boundsChanged, HashSet dirtyProperties) {
        applyStrokePaintProperties(textBoundingBox, boundsChanged, dirtyProperties);
        applyFontPropsToPaint(mStrokePaint);
    }

    private void applyFontPropsToPaint(final Paint paint) {
        boolean isItalic = getFontStyle() == FontStyle.kFontStyleItalic.getIndex();
        TypefaceResolver typefaceResolver = TypefaceResolver.getInstance();
        paint.setTypeface(typefaceResolver.getTypeface(getFontFamily(), getFontWeight(),
                isItalic, getRootContainer().getFontLanguage(), false));
        paint.setTextSize(getFontSize());
        paint.setLetterSpacing(getLetterSpacing() / getFontSize());
    }

    /**
     * Based on Layout direction, flip the offset.
     */
    private void applyXCoordinate() {
        int dx = GraphicLayoutDirection.kGraphicLayoutDirectionRTL == getRootContainer().getLayoutDirection() ?
                mBounds.width() : 0;
        if (getTextAnchor() == GraphicTextAnchor.kGraphicTextAnchorMiddle.getIndex()) {
            dx = mBounds.width() / 2;
        } else if (getTextAnchor() == GraphicTextAnchor.kGraphicTextAnchorEnd.getIndex()) {
            dx = GraphicLayoutDirection.kGraphicLayoutDirectionRTL == getRootContainer().getLayoutDirection() ?
                    0 : mBounds.width();
        }

        mX = getCoordinateX() - dx;
    }

    private void applyYCoordinate() {
        mY = getCoordinateY();
    }

    /**
     * Get the true text bounding box {@link Rect}
     * For text, mX represents the left x coordinate and mY represents the middle y coordinate
     * So need to add width to get the right x coordinate
     * And subtract height/2 to get the top y coordinate and add height/2 to get the bottom y coordinate
     * @return Rect - the bounding box of the text
     */
    private Rect calculateTextBoundingBox() {
        return new Rect(mX, mY - mBounds.height()/2, mX + mBounds.width(), mY + mBounds.height()/2);
    }
}
