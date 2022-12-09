package com.amazon.apl.android.utils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.view.View;

import com.amazon.apl.android.Component;
import com.amazon.apl.android.IAPLViewPresenter;
import com.amazon.apl.android.primitive.Rect;
import com.amazon.apl.android.robolectric.ViewhostRobolectricTest;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class TransformUtilsTest extends ViewhostRobolectricTest {
    private static final int LEFT = 100;
    private static final int TOP = 50;

    @Mock
    private View mView;

    @Mock
    private Canvas mCanvas;

    @Before
    public void setup() {
        when(mView.getLeft()).thenReturn(LEFT);
        when(mView.getTop()).thenReturn(TOP);
    }

    @Test
    public void test_applyChildTransformToParentCanvas_hasTransformIdentity() {
        // Given
        Matrix mockMatrix = new Matrix();

        // When
        TransformUtils.applyChildTransformToParentCanvas(mockMatrix, mView, mCanvas);

        // Then
        verify(mCanvas).concat(eq(new Matrix()));
    }

    @Test
    public void test_applyChildTransformToParentCanvas_hasTransformWithRotation() {
        // Given
        Matrix mockMatrix = new Matrix();
        int degrees = 45;
        mockMatrix.setRotate(degrees);

        // When
        TransformUtils.applyChildTransformToParentCanvas(mockMatrix, mView, mCanvas);

        // Then: Expected matrix is translated to child views coordinates
        Matrix expected = new Matrix();
        expected.setRotate(degrees);
        expected.preTranslate(-LEFT, -TOP);
        expected.postTranslate(LEFT, TOP);
        verify(mCanvas).concat(eq(expected));
    }
}
