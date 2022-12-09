package com.amazon.apl.android.utils;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.view.View;

import com.amazon.apl.android.Component;

public class TransformUtils {

    /**
     * This function applies the child's transformation, to the parent view's canvas.
     * Transformations can transform the views outside of its bounds, so this needs to be
     * applied at the parent view's level. In addition, the transform has to be applied
     * w.r.t to the child components coordinate system, so will need to be translated according
     * to the childs position.
     * @param childComponent
     * @param child
     * @param parentCanvas
     */
    public static void applyChildTransformToParentCanvas(Matrix transform, View child, Canvas parentCanvas) {
        int top = child.getTop();
        int left = child.getLeft();
        // Translate the transform to be in the coordinates of the child view, not the parent.
        transform.preTranslate(-left, -top);
        transform.postTranslate(left, top);
        parentCanvas.concat(transform);
    }
}
