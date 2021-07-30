/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.primitive;

import com.amazon.apl.android.BoundObject;
import com.amazon.apl.android.utils.ColorUtils;
import com.amazon.apl.enums.APLEnum;
import com.amazon.apl.enums.GraphicFilterProperty;
import com.amazon.apl.enums.GraphicFilterType;
import com.google.auto.value.AutoValue;

import java.util.ArrayList;


/**
 * APL Graphic Filters data type.
 * See {@link <a https://developer.amazon.com/en-US/docs/alexa/alexa-presentation-language/apl-avg-format.html#avg-filters>
 * APL Graphic Filters Specification </a>}
 */
@AutoValue
public abstract class GraphicFilters implements IterableProperty<GraphicFilters.GraphicFilter> {
    public static GraphicFilters create(BoundObject boundObject, APLEnum propertyKey) {
        return IterableProperty.create(new GraphicFilterGetter(boundObject, propertyKey));
    }

    @AutoValue
    public static abstract class GraphicFilter {
        public abstract GraphicFilterType filterType();
        public abstract int color();
        public abstract float radius();
        public abstract float horizontalOffset();
        public abstract float verticalOffset();
        public static Builder builder() {
            return new AutoValue_GraphicFilters_GraphicFilter.Builder();
        }

        @AutoValue.Builder
        public static abstract class Builder {
            public abstract Builder filterType(GraphicFilterType filterType);
            public abstract Builder color(int color);
            public abstract Builder radius(float radius);
            public abstract Builder horizontalOffset(float horizontalOffset);
            public abstract Builder verticalOffset(float verticalOffset);
            public abstract GraphicFilter build();
        }
    }

    private static class GraphicFilterGetter extends ArrayGetter<GraphicFilters, GraphicFilter> {
        private GraphicFilterGetter(BoundObject boundObject, APLEnum propertyKey) {
            super(boundObject, propertyKey);
        }

        @Override
        GraphicFilters builder() {
            return new AutoValue_GraphicFilters(new ArrayList<>(size()));
        }

        @Override
        public GraphicFilter get(int index) {
            return GraphicFilter.builder()
                    .filterType(GraphicFilterType.valueOf(nGetGraphicFilterTypeAt(getNativeHandle(), getIndex(), index)))
                    .color(ColorUtils.toARGB(nGetColorAt(getNativeHandle(), getIndex(), index)))
                    .radius(getFloatAt(GraphicFilterProperty.kGraphicPropertyFilterRadius, index))
                    .horizontalOffset(getFloatAt(GraphicFilterProperty.kGraphicPropertyFilterHorizontalOffset, index))
                    .verticalOffset(getFloatAt(GraphicFilterProperty.kGraphicPropertyFilterVerticalOffset, index))
                    .build();
        }

        private float getFloatAt(GraphicFilterProperty graphicFilterProperty, int index) {
            return nGetFloatAt(getNativeHandle(), getIndex(), graphicFilterProperty.getIndex(), index);
        }
    }

    private static native int nGetGraphicFilterTypeAt(long componentHandle, int propertyKey, int index);
    private static native long nGetColorAt(long componentHandle, int propertyKey, int index);
    private static native float nGetFloatAt(long componentHandle, int propertyKey, int graphicFilterPropertyKey, int index);
}