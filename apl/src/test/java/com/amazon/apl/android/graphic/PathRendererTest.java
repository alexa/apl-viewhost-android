package com.amazon.apl.android.graphic;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.internal.matchers.Any;
import org.mockito.junit.MockitoJUnitRunner;
import static org.junit.Assert.assertEquals;


import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Shader;

import com.amazon.apl.android.PropertyMap;
import com.amazon.apl.android.RenderingContext;
import com.amazon.apl.android.bitmap.IBitmapFactory;
import com.amazon.apl.android.graphic.GraphicContainerElement;
import com.amazon.apl.android.graphic.GraphicPathElement;
import com.amazon.apl.android.primitive.Gradient;
import com.amazon.apl.android.robolectric.ViewhostRobolectricTest;
import com.amazon.apl.enums.GradientSpreadMethod;
import com.amazon.apl.enums.GradientType;
import com.amazon.apl.enums.GradientUnits;
import com.amazon.apl.enums.GraphicPropertyKey;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

public class PathRendererTest extends ViewhostRobolectricTest {

    @Mock
    private GraphicContainerElement element;

    @Mock
    private Canvas canvas;

    @Mock
    private IBitmapFactory bitmapFactory;

    @Mock
    private GraphicPathElement pathElement;

    @Mock
    private PropertyMap<GraphicElement, GraphicPropertyKey> properties;

    @Mock
    private Paint fillPaint;

    @Mock
    private Paint strokePaint;

    @Mock
    private Shader fillShader;

    @Mock
    private Shader strokeShader;

    @Mock
    private Matrix matrix;

    @Mock
    private Gradient gradient;

    @Mock
    private RenderingContext context;

    @Mock
    private GradientSpreadMethod gradientSpreadMethod;

    @Mock
    private GradientUnits gradientUnits;

    private Path path;


    @Before
    public void setup() {
        path = new Path();
        when(pathElement.getFillPaint(anyFloat())).thenReturn(fillPaint);
        when(pathElement.getStrokePaint(anyFloat())).thenReturn(strokePaint);
        when(fillPaint.getShader()).thenReturn(fillShader);
        when(strokePaint.getShader()).thenReturn(strokeShader);
        when(pathElement.getGradient(any())).thenReturn(gradient);
        when(pathElement.getProperties()).thenReturn(properties);
        Matrix matrix = new Matrix();
        matrix.setValues(new float[]{
                1, 0, 0,
                0, 1, 0,
                0, 0, 1
        });
        when(element.getStackedMatrix()).thenReturn(matrix);
        when(element.getLocalMatrix()).thenReturn(matrix);

        List<GraphicElement> pathElementList = new ArrayList<>();
        pathElementList.add(pathElement);
        when(element.getChildren()).thenReturn(pathElementList);
        when(pathElement.getPath()).thenReturn(path);
        when(pathElement.getProperties()).thenReturn(properties);
        when(pathElement.getFillTransform()).thenReturn(matrix);
        when(pathElement.getStrokeTransform()).thenReturn(matrix);
        when(pathElement.getRenderingContext()).thenReturn(context);
        when(pathElement.getStrokeDashArray()).thenReturn(new float[]{});
    }

    @Test
    public void test_draw_with_linear_gradient_and_uniform_scaling() {
        // Configure mock behavior for linear gradient
        when(gradient.getType()).thenReturn(GradientType.LINEAR);
        when(properties.isGradient(GraphicPropertyKey.kGraphicPropertyFill)).thenReturn(true);
        when(properties.isGradient(GraphicPropertyKey.kGraphicPropertyStroke)).thenReturn(true);

        PathRenderer pathRenderer = new PathRenderer(element);

        pathRenderer.draw(canvas, 200, 200, bitmapFactory, true);

        // Verifying if setLocalMatrix is called on the shaders
        verify(fillShader).setLocalMatrix(any(Matrix.class));
        verify(strokeShader).setLocalMatrix(any(Matrix.class));
        verify(canvas, never()).concat(any(Matrix.class));
    }

    @Test
    public void test_draw_with_pattern_and_uniform_scaling() {
        PathRenderer pathRenderer = new PathRenderer(element);

        pathRenderer.draw(canvas, 200, 200, bitmapFactory, true);

        // Verifying if setLocalMatrix is called on the shaders
        verify(fillShader).setLocalMatrix(any(Matrix.class));
        verify(strokeShader).setLocalMatrix(any(Matrix.class));
        verify(canvas, never()).concat(any(Matrix.class));
    }

    @Test
    public void test_draw_with_null_shader_and_uniform_scaling() {
        when(fillPaint.getShader()).thenReturn(null);
        when(strokePaint.getShader()).thenReturn(null);
        PathRenderer pathRenderer = new PathRenderer(element);

        pathRenderer.draw(canvas, 200, 200, bitmapFactory, true);

        // Verifying if setLocalMatrix is called on the shaders
        verifyNoInteractions(fillShader);
        verifyNoInteractions(strokeShader);
        verify(canvas, never()).concat(any(Matrix.class));
    }

    @Test
    public void test_draw_with_radial_gradient_and_uniform_scaling() {
        // Configure mock behavior for radial gradient
        when(gradient.getType()).thenReturn(GradientType.RADIAL);
        when(properties.isGradient(GraphicPropertyKey.kGraphicPropertyFill)).thenReturn(true);
        when(gradient.getSpreadMethod()).thenReturn(gradientSpreadMethod);
        when(gradient.getUnits()).thenReturn(gradientUnits);

        PathRenderer pathRenderer = new PathRenderer(element);
        pathRenderer.draw(canvas, 200, 200, bitmapFactory, true);

        // Since we do not create an actual Path with radial gradients we verify that setShader is called with null
        // This is as the shaderFactory cannot create an actual gradient using mocks
        verify(fillPaint).setShader(null);
    }

    @Test
    public void test_draw_uniform_scaling_scaled_stroke_width() {
        when(gradient.getType()).thenReturn(GradientType.LINEAR);
        when(properties.isGradient(GraphicPropertyKey.kGraphicPropertyFill)).thenReturn(false);
        when(element.getViewportHeightActual()).thenReturn(100.f);
        when(element.getViewportWidthActual()).thenReturn(100.f);
        when(element.getWidthActual()).thenReturn(100.0f);
        when(element.getHeightActual()).thenReturn(100.0f);
        when(pathElement.getStrokeWidth()).thenReturn(1.0f);

        PathRenderer pathRenderer = new PathRenderer(element);
        pathRenderer.applyBaseAndViewportDimensions();
        pathRenderer.draw(canvas, 200, 200, bitmapFactory, true);

        ArgumentCaptor<Float> captor = ArgumentCaptor.forClass(Float.class);
        verify(strokePaint).setStrokeWidth(captor.capture());
        float strokeWidthValue = captor.getValue();

        assertEquals(2, strokeWidthValue, 0.0);
    }

    @Test
    public void test_draw_uniformScaling_miterLimitNotChanged() {
        // Setup to trigger uniform scaling by a factor of 2 in the draw pathway
        when(gradient.getType()).thenReturn(GradientType.LINEAR);
        when(properties.isGradient(GraphicPropertyKey.kGraphicPropertyFill)).thenReturn(false);
        when(element.getViewportHeightActual()).thenReturn(100.f);
        when(element.getViewportWidthActual()).thenReturn(100.f);
        when(element.getWidthActual()).thenReturn(100.0f);
        when(element.getHeightActual()).thenReturn(100.0f);
        when(pathElement.getStrokeWidth()).thenReturn(1.0f);

        final float before = strokePaint.getStrokeMiter();

        PathRenderer pathRenderer = new PathRenderer(element);
        pathRenderer.applyBaseAndViewportDimensions();
        pathRenderer.draw(canvas, 200, 200, bitmapFactory, true);

        final float after = strokePaint.getStrokeMiter();

        // Setter not called to update strokeMiter, and the value remains unchanged
        verify(strokePaint, times(0)).setStrokeMiter(any(Float.class));
        assertEquals(before, after, 0.0);
    }

    @Test
    public void test_draw_uniform_scaling_with_stroke_dash_path_effect() {
        when(element.getViewportHeightActual()).thenReturn(100.f);
        when(element.getViewportWidthActual()).thenReturn(100.f);
        when(element.getWidthActual()).thenReturn(100.0f);
        when(element.getHeightActual()).thenReturn(100.0f);
        when(pathElement.getStrokeWidth()).thenReturn(1.0f);
        when(pathElement.getStrokeDashArray()).thenReturn(new float[] {1, 2, 4, 8});
        when(pathElement.getStrokeDashOffset()).thenReturn(1.0f);

        PathRenderer pathRenderer = new PathRenderer(element);
        pathRenderer.applyBaseAndViewportDimensions();
        pathRenderer.draw(canvas, 200, 200, bitmapFactory, true);
        // DashPathEffect class does not have any getters, so only call is verified.
        verify(strokePaint).setPathEffect(any(DashPathEffect.class));
    }
}