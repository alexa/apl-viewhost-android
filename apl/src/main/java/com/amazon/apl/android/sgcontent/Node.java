package com.amazon.apl.android.sgcontent;

import android.graphics.Matrix;

import com.amazon.apl.android.bitmap.BitmapKey;
import com.amazon.apl.android.primitive.SGRect;
import com.amazon.apl.android.scenegraph.text.APLTextLayout;
import com.amazon.apl.android.sgcontent.filters.Filter;
import com.amazon.apl.android.utils.ColorUtils;

import java.util.ArrayList;
import java.util.List;

public class Node {
    final long mAddress;
    // used right now to dedupe filter requests because we are stuck in a draw loop due to the enable/disable hw acceleration
    // which results in us flooding the queue.
    public BitmapKey mFilterKey;

    public Node(long address) {
        mAddress = address;
    }

    public static Node ensure(long address) {
        Node node = nGetNodeObject(address);
        if (node == null) {
            node = new Node(address);
            node.nSetNodeObject(address);
        }
        return node;
    }

    public String getType() {
        return nGetType(mAddress);
    }

    public boolean isVisible() {
        return nIsVisible(mAddress);
    }

    public Node next() {
        long address = nNext(mAddress);
        return address != 0 ? Node.ensure(address) : null;
    }

    public List<Node> getChildren() {
        long[] children = nGetChildren(mAddress);
        List<Node> childrenNodes = new ArrayList<Node>(children.length);
        for (int i = 0; i < children.length; i++) {
            childrenNodes.add(Node.ensure(children[i]));
        }
        return childrenNodes;
    }

    public PathOp getOp() {
        return new PathOp(nGetOp(mAddress));
    }

    public float getOpacity() {
        return nGetOpacity(mAddress);
    }

    public Matrix getTransform() {
        Matrix m = new Matrix();
        float[] toMatrix = toMatrix(nGetTransform(mAddress));
        m.setValues(toMatrix);
        return m;
    }

    public APLTextLayout getAplTextLayout() {
        return nGetTextLayout(mAddress);
    }

    private float[] toMatrix(float[] transform2D) {
        return new float[] {
                transform2D[0], transform2D[2], transform2D[4],
                transform2D[1], transform2D[3], transform2D[5],
                0,              0,              1
        };
    }

    public Path getPath() {
        long pathHandle = nGetPath(mAddress);
        if (pathHandle == 0) {
            return null;
        } else {
            return new Path(pathHandle);
        }
    }

    public Path getClipPath() {
        long clipPathHandle = nGetClipPath(mAddress);
        if (clipPathHandle == 0) {
            return null;
        } else {
            return new Path(clipPathHandle);
        }
    }

    public int getShadowColor() {
        return ColorUtils.toARGB(nShadowGetColor(mAddress));
    }

    public float[] getShadowOffset() {
        return nShadowGetOffset(mAddress);
    }

    public float getRadius() {
        return nShadowGetRadius(mAddress);
    }

    public Filter getFilter() {
       long filterHandle = nImageGetFilter(mAddress);
       if (filterHandle == 0) {
           return null;
       } else {
           return Filter.create(filterHandle);
       }
    }

    public SGRect getSourceRect() {
        return SGRect.create(nImageGetSourceRect(mAddress));
    }

    public SGRect getTargetRect() {
        return SGRect.create(nImageGetTargetRect(mAddress));
    }

    private static native String nGetType(long address);

    private static native boolean nIsVisible(long address);

    private static native long nNext(long address);

    private static native long[] nGetChildren(long address);

    private static native long nGetPath(long address);

    private static native long nGetClipPath(long address);

    private static native long nGetOp(long address);

    private static native float nGetOpacity(long address);

    private static native float[] nGetTransform(long address);

    private static native APLTextLayout nGetTextLayout(long address);

    private static native int nShadowGetColor(long address);

    private static native float[] nShadowGetOffset(long address);

    private static native float nShadowGetRadius(long address);

    // Image node
    private static native float[] nImageGetSourceRect(long address);
    private static native float[] nImageGetTargetRect(long address);
    private static native long nImageGetFilter(long address);

    private native void nSetNodeObject(long address);
    private static native Node nGetNodeObject(long address);
}
