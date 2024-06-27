/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.scenegraph.rendering;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.Rect;
import android.text.Layout;
import android.text.TextDirectionHeuristics;
import android.text.TextPaint;

import com.amazon.apl.android.RenderingContext;
import com.amazon.apl.android.scenegraph.APLLayer;
import com.amazon.apl.android.scenegraph.text.APLTextLayout;
import com.amazon.apl.android.scenegraph.text.APLTextProperties;
import com.amazon.apl.android.sgcontent.Node;
import com.amazon.apl.android.sgcontent.Paint;
import com.amazon.apl.android.sgcontent.PathOp;
import com.amazon.apl.enums.GradientSpreadMethod;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 22, manifest = Config.NONE)
public class APLRenderTest {
    @Mock
    private APLLayer mockAPLLayer;
    @Mock
    private RenderingContext mockRenderingContext;
    @Mock
    private Node mockNode;
    @Mock
    private Canvas mockCanvas;
    @Mock
    private APLTextLayout mockAPLTextLayout;
    @Mock
    private APLTextProperties mockAPLTextProperties;
    @Mock
    private Layout mockLayout;
    @Mock
    private PathOp mockPathOp;
    @Mock
    private Paint mockSGPaint;
    // Dummy paint instance
    private TextPaint mTextPaint = new TextPaint();

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        when(mockNode.isVisible()).thenReturn(true);
        when(mockPathOp.getPaint()).thenReturn(mockSGPaint);
        when(mockNode.getOp()).thenReturn(mockPathOp);
        when(mockLayout.getPaint()).thenReturn(mTextPaint);
        when(mockCanvas.getMatrix()).thenReturn(Matrix.IDENTITY_MATRIX);
    }

    @Test
    public void testDrawTextNode_null_textLayout_does_not_crash() {
        when(mockNode.getType()).thenReturn("Text");
        APLRender.drawNode(mockAPLLayer, mockRenderingContext, mockNode, 1.0f, mockCanvas);
    }

    @Test
    public void testDrawTextNode_bounds_not_computed_when_pathOp_paint_type_is_color() {
        when(mockSGPaint.getType()).thenReturn("Color");
        when(mockSGPaint.getColor()).thenReturn(Color.BLUE);
        setUpMocksForTextNode();
        APLRender.drawNode(mockAPLLayer, mockRenderingContext, mockNode, 1.0f, mockCanvas);
        verify(mockAPLTextLayout).getLayout();
        verify(mockLayout, never()).getLineCount();
    }

    @Test
    public void testDrawTextNode_bounds_computed_when_pathOp_paint_type_is_not_color() {
        when(mockSGPaint.getType()).thenReturn("LinearGradient");
        when(mockSGPaint.getSpreadMethod()).thenReturn(GradientSpreadMethod.PAD);
        when(mockSGPaint.getLinearGradientStart()).thenReturn(new PointF(0.0f, 0.0f));
        when(mockSGPaint.getLinearGradientEnd()).thenReturn(new PointF(1.0f, 1.0f));
        when(mockSGPaint.getColors()).thenReturn(new int[]{0, 255});
        when(mockSGPaint.getPoints()).thenReturn(new float[]{0f, 1.0f});
        setUpMocksForTextNode();
        APLRender.drawNode(mockAPLLayer, mockRenderingContext, mockNode, 1.0f, mockCanvas);
        verify(mockAPLTextLayout).getLayout();
        verify(mockLayout).getLineCount();
    }

    @Test
    public void testDrawTextNode_computeBounds_for_single_line_text() {
        when(mockSGPaint.getType()).thenReturn("LinearGradient");
        when(mockSGPaint.getSpreadMethod()).thenReturn(GradientSpreadMethod.PAD);
        when(mockSGPaint.getLinearGradientStart()).thenReturn(new PointF(0.0f, 0.0f));
        when(mockSGPaint.getLinearGradientEnd()).thenReturn(new PointF(1.0f, 1.0f));
        when(mockSGPaint.getColors()).thenReturn(new int[]{0, 255});
        when(mockSGPaint.getPoints()).thenReturn(new float[]{0f, 1.0f});
        when(mockLayout.getLineCount()).thenReturn(1);
        setUpMocksForTextNode();
        APLRender.drawNode(mockAPLLayer, mockRenderingContext, mockNode, 1.0f, mockCanvas);
        verify(mockAPLTextLayout).getLayout();
        verify(mockLayout).getLineCount();
        verify(mockLayout).getLineBounds(eq(0), any(Rect.class));
        // TODO: Add more assertions.
    }

    @Test
    public void testDrawTextNode_computeBounds_for_multiple_line_text() {
        when(mockSGPaint.getType()).thenReturn("RadialGradient");
        when(mockSGPaint.getSpreadMethod()).thenReturn(GradientSpreadMethod.PAD);
        when(mockSGPaint.getRadialGradientCenter()).thenReturn(new PointF(0.0f, 0.0f));
        when(mockSGPaint.getRadialGradientRadius()).thenReturn(1.0f);
        when(mockSGPaint.getColors()).thenReturn(new int[]{0, 255});
        when(mockSGPaint.getPoints()).thenReturn(new float[]{0f, 1.0f});
        when(mockLayout.getLineCount()).thenReturn(2);
        setUpMocksForTextNode();
        APLRender.drawNode(mockAPLLayer, mockRenderingContext, mockNode, 1.0f, mockCanvas);
        verify(mockAPLTextLayout).getLayout();
        verify(mockLayout).getLineCount();
        for (int i = 0; i < 2; i++) {
            verify(mockLayout).getLineBounds(eq(i), any(Rect.class));
        }
        // TODO: Add more assertions.
    }

    @Test
    public void testApplyPaintProps_applies_opacity_correctly() {
        Rect bounds = new Rect();
        android.graphics.Paint paint = new android.graphics.Paint();
        when(mockSGPaint.getOpacity()).thenReturn(0.5f);
        when(mockSGPaint.getType()).thenReturn("Color");
        APLRender.applyPaintProps(mockRenderingContext, mockSGPaint, bounds, 1.0f, 0.5f, paint);
        float expectedValue = (int) (255 * 0.5f * 0.5f);
        assertEquals(expectedValue, paint.getAlpha(), 0.01f);
    }

    private void setUpMocksForTextNode() {
        when(mockPathOp.getType()).thenReturn("Fill");
        when(mockLayout.getText()).thenReturn("abcd");
        when(mockAPLTextLayout.getLayout()).thenReturn(mockLayout);
        when(mockAPLTextLayout.getTextProperties()).thenReturn(mockAPLTextProperties);
        when(mockAPLTextProperties.getTextAlignment()).thenReturn(Layout.Alignment.ALIGN_CENTER);
        when(mockAPLTextProperties.getDirectionHeuristic()).thenReturn(TextDirectionHeuristics.LTR);
        when(mockNode.getType()).thenReturn("Text");
        when(mockNode.getAplTextLayout()).thenReturn(mockAPLTextLayout);
    }
}
