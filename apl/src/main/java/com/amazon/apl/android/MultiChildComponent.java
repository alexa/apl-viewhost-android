/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;

import com.amazon.apl.android.primitive.Rect;
import com.amazon.apl.enums.PropertyKey;
import com.amazon.apl.enums.ScrollDirection;
import com.amazon.apl.enums.UpdateType;

/**
 * Component for Container/TouchWrapper/ScrollView/Sequence/GridSequence/Pager.
 * See {@link <a https://developer.amazon.com/docs/alexa-presentation-language/apl-container.html>
 * APL Container Specification </a>}
 */
public class MultiChildComponent extends Component {
    /**
     * MultiChild constructor
     * {@inheritDoc}
     */
    MultiChildComponent(long nativeHandle, String componentId, @NonNull RenderingContext renderingContext) {
        super(nativeHandle, componentId, renderingContext);
    }

    static class MultiChildAccessibilityDelegate extends APLAccessibilityDelegate<MultiChildComponent> {
        private static final float SCROLL_MULTIPLIER = 0.9f;

        public MultiChildAccessibilityDelegate(MultiChildComponent multiChildComponent, Context context) {
            super(multiChildComponent, context);
        }

        @Override
        public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfoCompat info) {
            super.onInitializeAccessibilityNodeInfo(host, info);
            if (mComponent.hasProperty(PropertyKey.kPropertyScrollPosition)) {
                ScrollDirection scrollDirection = ScrollDirection.valueOf(mComponent.getProperties().getEnum(PropertyKey.kPropertyScrollDirection));
                int scrollPosition = mComponent.getProperties().getDimension(PropertyKey.kPropertyScrollPosition).intValue();
                if (scrollPosition > 0) {
                    info.addAction(AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_SCROLL_BACKWARD);
                }

                // TODO We should get this information from core since they already compute it.
                Component lastChild = mComponent.getChildCount() == 0 ? null : mComponent.getChildAt(mComponent.getChildCount() - 1);
                if (lastChild != null) {
                    Rect lastChildBounds = lastChild.getBounds();
                    Rect scrollableBounds = mComponent.getBounds();
                    int childEnd, scrollableSize;
                    if (ScrollDirection.kScrollDirectionHorizontal == scrollDirection) {
                        childEnd = lastChildBounds.intRight();
                        scrollableSize = scrollableBounds.intWidth();
                    } else {
                        childEnd = lastChildBounds.intBottom();
                        scrollableSize = scrollableBounds.intHeight();
                    }

                    if (!lastChild.isLaidOut() || scrollPosition + scrollableSize < childEnd) {
                        info.addAction(AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_SCROLL_FORWARD);
                    }
                }
            }
        }

        @Override
        public boolean performAccessibilityAction(View host, int action, Bundle args) {
            if ((action == AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD || action == AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD)
                    && mComponent.hasProperty(PropertyKey.kPropertyScrollPosition)) {
                int scrollPosition = mComponent.getProperties().getDimension(PropertyKey.kPropertyScrollPosition).intValue();
                ScrollDirection scrollDirection = ScrollDirection.valueOf(mComponent.getProperties().getEnum(PropertyKey.kPropertyScrollDirection));
                Rect bounds = mComponent.getBounds();
                float scrollMultiplier = (action == AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD ? SCROLL_MULTIPLIER : -SCROLL_MULTIPLIER);
                int distance;
                if (ScrollDirection.kScrollDirectionHorizontal == scrollDirection) {
                    // Scroll most of a page to maintain previously focused item in view so focus finder doesn't lose it.
                    distance = Math.round(bounds.getWidth() * scrollMultiplier);
                } else {
                    distance = Math.round(bounds.getHeight() * scrollMultiplier);
                }

                mComponent.update(UpdateType.kUpdateScrollPosition, scrollPosition + distance);

                host.post(() -> {
                    // enforce reevaluating onInitializeAccessibilityNodeInfo to an updated action list.
                    host.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_SCROLLED);
                });
                return true;
            }

            return super.performAccessibilityAction(host, action, args);
        }
    }
}
