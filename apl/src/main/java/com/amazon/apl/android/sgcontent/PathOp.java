package com.amazon.apl.android.sgcontent;

import com.amazon.apl.enums.GraphicLineCap;
import com.amazon.apl.enums.GraphicLineJoin;

public class PathOp {
    public long mAddress;

    public PathOp(long address) {
        mAddress = address;
    }

    public String getType() {
        return nGetType(mAddress);
    }

    public PathOp getNextSibbling() {
        long address = nGetNextSibbling(mAddress);
        if (address == 0) {
            return null;
        }
        return new PathOp(address);
    }

    public Paint getPaint() {
        return new Paint(nGetPaint(mAddress));
    }

    public float getStrokeWidth() {
       return nGetStokeWidth(mAddress);
    }

    public float[] getStrokeDashArray() {
        return nGetStrokeDashArray(mAddress);
    }

    public float getStrokeDashOffset() {
        return nGetStrokeDashOffset(mAddress);
    }

    public float getPathLength() {
        return nGetPathLength(mAddress);
    }

    public float getMiterLimit() {
        return nGetMiterLimit(mAddress);
    }

    public GraphicLineCap getLineCap() {
        return GraphicLineCap.valueOf(nGetLineCap(mAddress));
    }

    public GraphicLineJoin getLineJoin() {
        return GraphicLineJoin.valueOf(nGetLineJoin(mAddress));
    }

    public int getFillType() {
        return nGetFillType(mAddress);
    }

    // common
    private static native String nGetType(long address);
    private static native long nGetNextSibbling(long address);
    private static native long nGetPaint(long address);

    // stroke props
    private static native float nGetStokeWidth(long address);
    private static native float nGetMiterLimit(long address);
    private static native float nGetPathLength(long address);
    private static native float nGetStrokeDashOffset(long address);
    private static native float[] nGetStrokeDashArray(long address);
    private static native int nGetLineCap(long address);
    private static native int nGetLineJoin(long address);

    // fill props
    private static native int nGetFillType(long address);

    //TODO:
    // visible, maxWidth needed?
    //GraphicLineCap lineCap = kGraphicLineCapButt;
    //GraphicLineJoin lineJoin = kGraphicLineJoinMiter;
    //std::vector<float> dashes;  // Always should be an even number

    // fill props
    //private static native int nGetFillType(long address);
}
