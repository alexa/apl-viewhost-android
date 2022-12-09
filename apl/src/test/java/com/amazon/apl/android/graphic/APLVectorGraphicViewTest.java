/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.android.graphic;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.util.Pair;

import com.amazon.apl.android.IAPLViewPresenter;
import com.amazon.apl.android.bitmap.IBitmapFactory;
import com.amazon.apl.android.bitmap.IBitmapPool;
import com.amazon.apl.android.bitmap.PooledBitmapFactory;
import com.amazon.apl.android.providers.ITelemetryProvider;
import com.amazon.apl.android.robolectric.ViewhostRobolectricTest;
import com.amazon.apl.enums.VectorGraphicAlign;
import com.amazon.apl.enums.VectorGraphicScale;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Spy;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.atLeastOnce;

public class APLVectorGraphicViewTest extends ViewhostRobolectricTest {
    @Spy
    private Drawable mMockImageDrawable;
    @Mock
    private IAPLViewPresenter mMockPresenter;

    @Mock
    private IBitmapPool mMockBitmapPool;

    private static final int VIEW_SIZE = 100;  // 100 pixels per side
    private static final int VIEW_PADDING = 10; // 10 pixels of padding per side
    private static final int VIEW_INNER_SIZE = VIEW_SIZE - 2 * VIEW_PADDING;

    private APLVectorGraphicView mAPLVectorGraphicView;

    @Before
    public void setUp() {
        IBitmapFactory bitmapFactory = PooledBitmapFactory.create(mock(ITelemetryProvider.class), mMockBitmapPool);
        when(mMockPresenter.getBitmapFactory()).thenReturn(bitmapFactory);
        mAPLVectorGraphicView = spy(new APLVectorGraphicView(ViewhostRobolectricTest.getApplication().getApplicationContext(), mMockPresenter));
        mAPLVectorGraphicView.setFrame(0, 0, VIEW_SIZE, VIEW_SIZE);
        mAPLVectorGraphicView.setPadding(VIEW_PADDING, VIEW_PADDING, VIEW_PADDING, VIEW_PADDING);
    }

    @Test
    public void testOnDraw_intrinsicDrawableFits(){
        when(mMockBitmapPool.get(anyInt(), anyInt(), any())).thenReturn(Bitmap.createBitmap(VIEW_INNER_SIZE, VIEW_INNER_SIZE, Bitmap.Config.ARGB_8888));

        mAPLVectorGraphicView.setScale(VectorGraphicScale.kVectorGraphicScaleBestFit);

        when(mMockImageDrawable.getIntrinsicWidth()).thenReturn(VIEW_INNER_SIZE);
        when(mMockImageDrawable.getIntrinsicHeight()).thenReturn(VIEW_INNER_SIZE);
        mAPLVectorGraphicView.setImageDrawable(mMockImageDrawable);

        assertTrue(mAPLVectorGraphicView.getImageMatrix().isIdentity());
        verify(mMockImageDrawable, atLeastOnce())
                .setBounds(0,0, VIEW_INNER_SIZE, VIEW_INNER_SIZE);
    }


    @Test
    public void testCalculateScale_default(){
        // scale wont matter if drawableWidth and Height is 0
        float scale = mAPLVectorGraphicView.calculateScale(100,100,0,100);
        assertEquals(1.0f, scale);
        scale = mAPLVectorGraphicView.calculateScale(100,100,100,0);
        assertEquals(1.0f, scale);

        // scale wont matter if scale prop is None or Fill
        mAPLVectorGraphicView.setScale(VectorGraphicScale.kVectorGraphicScaleNone);
        assertEquals(1.0f, scale);
        mAPLVectorGraphicView.setScale(VectorGraphicScale.kVectorGraphicScaleFill);
        assertEquals(1.0f, scale);
    }

    @Test
    public void testCalculateScale_BestFill(){
        mAPLVectorGraphicView.setScale(VectorGraphicScale.kVectorGraphicScaleBestFill);
        float scale = mAPLVectorGraphicView.calculateScale(100,100,10,10);
        assertEquals(10.0f, scale);

        scale = mAPLVectorGraphicView.calculateScale(200,100,100,100);
        assertEquals(2.0f, scale);

        scale = mAPLVectorGraphicView.calculateScale(80,100,25,30);
        assertEquals(3.3f, scale, 0.1f);

        scale = mAPLVectorGraphicView.calculateScale(100,100,200,300);
        assertEquals(0.5f, scale, 0.1f);
    }


    @Test
    public void testCalculateScale_BestFit(){
        mAPLVectorGraphicView.setScale(VectorGraphicScale.kVectorGraphicScaleBestFit);
        float scale = mAPLVectorGraphicView.calculateScale(100,100,10,10);
        assertEquals(10.0f, scale);

        scale = mAPLVectorGraphicView.calculateScale(200,100,100,100);
        assertEquals(1.0f, scale);

        scale = mAPLVectorGraphicView.calculateScale(80,100,25,30);
        assertEquals(3.2f, scale, 0.1f);

        scale = mAPLVectorGraphicView.calculateScale(100,100,200,300);
        assertEquals(0.3f, scale, 0.1f);
    }

    @Test
    public void testCalculateScale_Fill() {
        mAPLVectorGraphicView.setScale(VectorGraphicScale.kVectorGraphicScaleFill);
        float scale = mAPLVectorGraphicView.calculateScale(100,100,10,10);
        assertEquals(1.0f, scale);
    }

    @Test
    public void testCalculateScale_None() {
        mAPLVectorGraphicView.setScale(VectorGraphicScale.kVectorGraphicScaleNone);
        float scale = mAPLVectorGraphicView.calculateScale(100,100,10,10);
        assertEquals(1.0f, scale);
    }



    Pair<Integer, Integer> calculateAlignment(int viewWidth, int viewHeight,
                                              int drawableWidth, int drawableHeight) {
        return new Pair<>(mAPLVectorGraphicView.deltaLeft(viewWidth, drawableWidth),
                mAPLVectorGraphicView.deltaTop(viewHeight, drawableHeight));
    }

    @Test
    public void testCalculateAlign_Bottom(){
        mAPLVectorGraphicView.setAlign(VectorGraphicAlign.kVectorGraphicAlignBottom);

        Pair<Integer, Integer> position = calculateAlignment(100,100,10,10);
        assertEquals((int)position.first ,45); //x
        assertEquals((int)position.second ,90); //y

        position = calculateAlignment(200,100,100,100);
        assertEquals((int)position.first ,50); //x
        assertEquals((int)position.second ,0); //y

        position = calculateAlignment(80,100,25,30);
        assertEquals((int)position.first ,28); //x
        assertEquals((int)position.second ,70); //y

        position = calculateAlignment(100,100,200,300);
        assertEquals((int)position.first ,-50); //x
        assertEquals((int)position.second ,-200); //y
    }

    @Test
    public void testCalculateAlign_BottomLeft(){
        mAPLVectorGraphicView.setAlign(VectorGraphicAlign.kVectorGraphicAlignBottomLeft);

        Pair<Integer, Integer> position = calculateAlignment(100,100,10,10);
        assertEquals((int)position.first ,0); //x
        assertEquals((int)position.second ,90); //y

        position = calculateAlignment(200,100,100,100);
        assertEquals((int)position.first ,0); //x
        assertEquals((int)position.second ,0); //y

        position = calculateAlignment(80,100,25,30);
        assertEquals((int)position.first ,0); //x
        assertEquals((int)position.second ,70); //y

        position = calculateAlignment(100,100,200,300);
        assertEquals((int)position.first ,0); //x
        assertEquals((int)position.second ,-200); //y
    }

    @Test
    public void testCalculateAlign_BottomRight(){
        mAPLVectorGraphicView.setAlign(VectorGraphicAlign.kVectorGraphicAlignBottomRight);

        Pair<Integer, Integer> position = calculateAlignment(100,100,10,10);
        assertEquals((int)position.first ,90); //x
        assertEquals((int)position.second ,90); //y

        position = calculateAlignment(200,100,100,100);
        assertEquals((int)position.first ,100); //x
        assertEquals((int)position.second ,0); //y

        position = calculateAlignment(80,100,25,30);
        assertEquals((int)position.first ,55); //x
        assertEquals((int)position.second ,70); //y

        position = calculateAlignment(100,100,200,300);
        assertEquals((int)position.first ,-100); //x
        assertEquals((int)position.second ,-200); //y
    }


    @Test
    public void testCalculateAlign_Top(){
        mAPLVectorGraphicView.setAlign(VectorGraphicAlign.kVectorGraphicAlignTop);

        Pair<Integer, Integer> position = calculateAlignment(100,100,10,10);
        assertEquals((int)position.first ,45); //x
        assertEquals((int)position.second ,0); //y

        position = calculateAlignment(200,100,100,100);
        assertEquals((int)position.first ,50); //x
        assertEquals((int)position.second ,0); //y

        position = calculateAlignment(80,100,25,30);
        assertEquals((int)position.first ,28); //x
        assertEquals((int)position.second ,0); //y

        position = calculateAlignment(100,100,200,300);
        assertEquals((int)position.first ,-50); //x
        assertEquals((int)position.second ,0); //y
    }

    @Test
    public void testCalculateAlign_TopLeft(){
        mAPLVectorGraphicView.setAlign(VectorGraphicAlign.kVectorGraphicAlignTopLeft);

        Pair<Integer, Integer> position = calculateAlignment(100,100,10,10);
        assertEquals((int)position.first ,0); //x
        assertEquals((int)position.second ,0); //y

        position = calculateAlignment(200,100,100,100);
        assertEquals((int)position.first ,0); //x
        assertEquals((int)position.second ,0); //y

        position = calculateAlignment(80,100,25,30);
        assertEquals((int)position.first ,0); //x
        assertEquals((int)position.second ,0); //y

        position = calculateAlignment(100,100,200,300);
        assertEquals((int)position.first ,0); //x
        assertEquals((int)position.second ,0); //y
    }

    @Test
    public void testCalculateAlign_TopRight(){
        mAPLVectorGraphicView.setAlign(VectorGraphicAlign.kVectorGraphicAlignTopRight);

        Pair<Integer, Integer> position = calculateAlignment(100,100,10,10);
        assertEquals((int)position.first ,90); //x
        assertEquals((int)position.second ,0); //y

        position = calculateAlignment(200,100,100,100);
        assertEquals((int)position.first ,100); //x
        assertEquals((int)position.second ,0); //y

        position = calculateAlignment(80,100,25,30);
        assertEquals((int)position.first ,55); //x
        assertEquals((int)position.second ,0); //y

        position = calculateAlignment(100,100,200,300);
        assertEquals((int)position.first ,-100); //x
        assertEquals((int)position.second ,0); //y
    }


    @Test
    public void testCalculateAlign_Left(){
        mAPLVectorGraphicView.setAlign(VectorGraphicAlign.kVectorGraphicAlignLeft);

        Pair<Integer, Integer> position = calculateAlignment(100,100,10,10);
        assertEquals((int)position.first ,0); //x
        assertEquals((int)position.second ,45); //y

        position = calculateAlignment(200,100,100,100);
        assertEquals((int)position.first ,0); //x
        assertEquals((int)position.second ,0); //y

        position = calculateAlignment(80,100,25,30);
        assertEquals((int)position.first ,0); //x
        assertEquals((int)position.second ,35); //y

        position = calculateAlignment(100,100,200,300);
        assertEquals((int)position.first ,0); //x
        assertEquals((int)position.second ,-100); //y
    }

    @Test
    public void testCalculateAlign_Right(){
        mAPLVectorGraphicView.setAlign(VectorGraphicAlign.kVectorGraphicAlignRight);

        Pair<Integer, Integer> position = calculateAlignment(100,100,10,10);
        assertEquals((int)position.first ,90); //x
        assertEquals((int)position.second ,45); //y

        position = calculateAlignment(200,100,100,100);
        assertEquals((int)position.first ,100); //x
        assertEquals((int)position.second ,0); //y

        position = calculateAlignment(80,100,25,30);
        assertEquals((int)position.first ,55); //x
        assertEquals((int)position.second ,35); //y

        position = calculateAlignment(100,100,200,300);
        assertEquals((int)position.first ,-100); //x
        assertEquals((int)position.second ,-100); //y
    }

    @Test
    public void testCalculateAlign_Center(){
        mAPLVectorGraphicView.setAlign(VectorGraphicAlign.kVectorGraphicAlignCenter);

        Pair<Integer, Integer> position = calculateAlignment(100,100,10,10);
        assertEquals((int)position.first ,45); //x
        assertEquals((int)position.second ,45); //y

        position = calculateAlignment(200,100,100,100);
        assertEquals((int)position.first ,50); //x
        assertEquals((int)position.second ,0); //y

        position = calculateAlignment(80,100,25,30);
        assertEquals((int)position.first ,28); //x
        assertEquals((int)position.second ,35); //y

        position = calculateAlignment(100,100,200,300);
        assertEquals((int)position.first ,-50); //x
        assertEquals((int)position.second ,-100); //y
    }


}
