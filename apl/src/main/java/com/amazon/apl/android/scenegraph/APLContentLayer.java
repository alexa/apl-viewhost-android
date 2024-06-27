package com.amazon.apl.android.scenegraph;

import android.view.ViewGroup;

import com.amazon.apl.android.RenderingContext;
import com.amazon.apl.android.views.APLView;

import java.util.HashSet;
import java.util.Set;

public class APLContentLayer extends APLLayer {
    public APLContentLayer(RenderingContext renderingContext) {
        super(renderingContext);
    }

    @Override
    public void attachView(ViewGroup view) {
        super.attachView(view);
        long[] childrenLayers = getChildren();
        createChildView(childrenLayers, view);
    }

    private void createChildView(long[] childrenLayers, ViewGroup view) {

        for (long coreChildLayer : childrenLayers) {
            APLLayer layer = ensure(coreChildLayer, getRenderingContext());
            APLView childView = new APLView(view.getContext(), layer);
            layer.attachView(childView);
            view.addView(childView);
        }
    }

    @Override
    void updateDirtyProperties(int flags) {
        super.updateDirtyProperties(flags);

        if ((flags & 256) != 0) { // Children changed
            // Ensure all updated children and build a Set for rapidly identifying old views
            // that have been removed.
            long[] children = getChildren();
            final int finalCount = children.length;
            if (finalCount == 0) {
                mChildView.removeAllViews();
                return;
            }
            final APLLayer[] finalArray = new APLLayer[finalCount];
            final Set<ViewGroup> finalSet = new HashSet<>();
            int index = 0;
            for (long child : children) {
                APLLayer childLayer = ensure(child, getRenderingContext());
                finalArray[index++] = childLayer;
                finalSet.add(childLayer.mChildView);
            }

            // Remove all existing subviews that are not in the final array
            index = 0;
            while (index < mChildView.getChildCount()) {
                APLView view = (APLView) mChildView.getChildAt(index);
                if (finalSet.contains(view)) {
                    index++;
                } else {
                    mChildView.removeViewInLayout(view);
                }
            }

            // Walk the final array and put in subviews that are missing
            // This method assumes that views are NOT reordered.  If APL in the future
            // allows reordering of child views, this will have to be changed.
            index = 0;
            for (APLLayer layer : finalArray) {
                // If layer does not have a child view, create and attach one.
                if (layer.mChildView == null) { // Layer does not have a child
                    ViewGroup childView = new APLView(mChildView.getContext(), layer);
                    layer.attachView(childView);
                }
                // If layer child view is not at the same index in the view hierarchy, adjust it
                if (mChildView.getChildAt(index) != layer.mChildView) {
                    mChildView.removeView(layer.mChildView);
                    mChildView.addView(layer.mChildView, index);
                }
                index++;
            }
        }
    }
}
