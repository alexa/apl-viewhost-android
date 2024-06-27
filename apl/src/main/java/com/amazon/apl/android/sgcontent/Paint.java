package com.amazon.apl.android.sgcontent;

import android.graphics.Matrix;
import android.graphics.PointF;

import com.amazon.apl.android.scenegraph.APLLayer;
import com.amazon.apl.android.utils.ColorUtils;
import com.amazon.apl.enums.GradientSpreadMethod;

public class Paint {
    public long mAddress;

    public Paint(long address) {
        mAddress = address;
    }

    public String getType() {
        return nGetType(mAddress);
    }

    public int getColor() {
        return ColorUtils.toARGB(nGetColor(mAddress));
    }

    public float getOpacity() {
        return nGetOpacity(mAddress);
    }

    public float[] getSize() {
        return nPatternGetSize(mAddress);
    }

    public Node getNode() {
        return Node.ensure(nPatternGetNode(mAddress));
    }


    public float[] getPoints() {
        return nGradientGetPoints(mAddress);
    }

    public int[] getColors() {

        int[] colors = nGradientGetColors(mAddress);
        int[] argbColors = new int[colors.length];
        for (int i = 0; i < colors.length; i++) {
            argbColors[i] = ColorUtils.toARGB(colors[i]);
        }
        return argbColors;
    }

    public Matrix getTransform() {
        Matrix m = new Matrix();
        float[] toMatrix = toMatrix(nGetTransform(mAddress));
        m.setValues(toMatrix);
        return m;
    }

    private float[] toMatrix(float[] transform2D) {
        return new float[] {
                transform2D[0], transform2D[2], transform2D[4],
                transform2D[1], transform2D[3], transform2D[5],
                0,              0,              1
        };
    }

    public GradientSpreadMethod getSpreadMethod() {
        return GradientSpreadMethod.valueOf(nGradientGetSpreadMethod(mAddress));
    }

    public boolean getUseBoundingBox() {
        return nGradientGetUseBoundingBox(mAddress);
    }

    public PointF getLinearGradientStart() {
        float[] point = nLinearGradientGetStart(mAddress);
        return new PointF(point[0], point[1]);
    }

    public PointF getLinearGradientEnd() {
        float[] point = nLinearGradientGetEnd(mAddress);
        return new PointF(point[0], point[1]);
    }

    public PointF getRadialGradientCenter() {
        float[] center = nRadialGradientGetCenter(mAddress);
        return new PointF(center[0], center[1]);
    }

    public float getRadialGradientRadius() {
        return nRadialGradientGetRadius(mAddress);
    }

    private static native String nGetType(long address);

    @Override
    public boolean equals(Object o) {
        if (o instanceof Paint) {
            return this.hashCode() == o.hashCode();
        }
        return false;
    }

    @Override
    public int hashCode() {
        return nGetHashCode(mAddress);
    }


    // Color paints
    private static native int nGetColor(long address);

    private static native float[] nGetTransform(long address);

    private static native float nGetOpacity(long address);

    // pattern paint
    private static native float[] nPatternGetSize(long address);
    private static native long nPatternGetNode(long address);

    // gradient paint
    private static native float[] nGradientGetPoints(long address);
    private static native int[] nGradientGetColors(long address);
    private static native int nGradientGetSpreadMethod(long address);
    private static native boolean nGradientGetUseBoundingBox(long address);
    // linear gradient paint
    private static native float[] nLinearGradientGetStart(long address);
    private static native float[] nLinearGradientGetEnd(long address);
    // radial gradient paint
    private static native float[] nRadialGradientGetCenter(long address);
    private static native float nRadialGradientGetRadius(long address);

    private static native int nGetHashCode(long address);
}
