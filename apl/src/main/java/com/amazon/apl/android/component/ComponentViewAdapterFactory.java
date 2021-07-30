/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.component;

import com.amazon.apl.android.Component;
import com.amazon.apl.android.EditText;
import com.amazon.apl.android.Frame;
import com.amazon.apl.android.Image;
import com.amazon.apl.android.MultiChildComponent;
import com.amazon.apl.android.Pager;
import com.amazon.apl.android.Text;
import com.amazon.apl.android.VectorGraphic;
import com.amazon.apl.android.Video;

import java.util.HashMap;
import java.util.Map;

/**
 * Factory for {@link ComponentViewAdapter}.
 */
public class ComponentViewAdapterFactory {
    private static Map<Class<? extends Component>, ComponentViewAdapter> sAdapterMap = new HashMap<>();

    static {
        sAdapterMap.put(Frame.class, FrameViewAdapter.getInstance());
        sAdapterMap.put(EditText.class, EditTextViewAdapter.getInstance());
        sAdapterMap.put(Image.class, ImageViewAdapter.getInstance());
        sAdapterMap.put(MultiChildComponent.class, MultiChildViewAdapter.getInstance());
        sAdapterMap.put(Pager.class, MultiChildViewAdapter.getInstance());
        sAdapterMap.put(Text.class, TextViewAdapter.getInstance());
        sAdapterMap.put(VectorGraphic.class, VectorGraphicViewAdapter.getInstance());
        sAdapterMap.put(Video.class, VideoViewAdapter.getInstance());
    }

    /**
     * Return a ComponentViewAdapter for a given Component.
     * @param component a component
     * @return          a view adapter
     */
    public static ComponentViewAdapter getAdapter(Component component) {
        return sAdapterMap.get(component.getClass());
    }
}
