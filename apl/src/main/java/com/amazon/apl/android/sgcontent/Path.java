package com.amazon.apl.android.sgcontent;

import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.shapes.RoundRectShape;

import com.amazon.apl.android.primitive.SGRRect;
import com.amazon.apl.android.primitive.SGRect;

public class Path {
    public long mAddress;

    public Path(long address) {
        mAddress = address;
    }

    public String getType() {
        return nGetType(mAddress);
    }

    public String getValue() {
        return nGetValue(mAddress);
    }

    public float[] getPoints() {
        return nGetPoints(mAddress);
    }

    public RectF getRectPathRect() {
        float[] rectCorners = nRectPathGetRect(mAddress);
        return new RectF(rectCorners[0], rectCorners[1], rectCorners[2], rectCorners[3]);
    }

    public SGRRect getFramePathRRect() {
        float[] rrect = nFramePathGetRRect(mAddress);
        return SGRRect.builder()
                .left(rrect[0])
                .top(rrect[1])
                .right(rrect[2])
                .bottom(rrect[3])
                .radii(new float[]{rrect[4],rrect[5],rrect[6],rrect[7]})
                .build();
    }

    public SGRRect getFramePathInset() {
        float[] rrect = nFramePathGetInset(mAddress);
        return SGRRect.builder()
                .left(rrect[0])
                .top(rrect[1])
                .right(rrect[2])
                .bottom(rrect[3])
                .radii(new float[]{rrect[4],rrect[5],rrect[6],rrect[7]})
                .build();
    }

    public SGRRect getRRectPathRRect() {
        float[] rrect = nRRectPathGetRRect(mAddress);
        return SGRRect.builder()
                .left(rrect[0])
                .top(rrect[1])
                .right(rrect[2])
                .bottom(rrect[3])
                .radii(new float[]{rrect[4],rrect[5],rrect[6],rrect[7]})
                .build();
    }

    private static native String nGetType(long address);

    // General Path
    private static native String nGetValue(long address);
    private static native float[] nGetPoints(long address);
    // Rect Path
    private static native float[] nRectPathGetRect(long address);
    // Frame Path
    private static native float[] nFramePathGetRRect(long address);
    private static native float[] nFramePathGetInset(long address);
    // Rounded Rect Path
    private static native float[] nRRectPathGetRRect(long address);

}
