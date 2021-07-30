/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.primitive;

import com.amazon.apl.android.BoundObject;
import com.amazon.apl.enums.APLEnum;
import com.google.auto.value.AutoValue;

import java.util.ArrayList;

/**
 * AccessibilityActions Property
 * See https://developer.amazon.com/en-US/docs/alexa/alexa-presentation-language/apl-component.html#actions
 */
@AutoValue
public abstract class AccessibilityActions implements IterableProperty<AccessibilityActions.AccessibilityAction> {
    public static AccessibilityActions create(BoundObject boundObject, APLEnum propertyKey) {
        return IterableProperty.create(new AccessibilityActionGetter(boundObject, propertyKey));
    }

    private static class AccessibilityActionGetter extends ArrayGetter<AccessibilityActions, AccessibilityAction> {
        public AccessibilityActionGetter(final BoundObject boundObject, final APLEnum propertyKey) {
            super(boundObject, propertyKey);
        }

        @Override
        AccessibilityActions builder() {
            return new AutoValue_AccessibilityActions(new ArrayList<>(size()));
        }

        @Override
        public AccessibilityAction get(int index) {
            return AccessibilityAction.create(
                    nGetAccessibilityActionNameAt(getNativeHandle(), getIndex(), index),
                    nGetAccessibilityActionLabelAt(getNativeHandle(), getIndex(), index));
        }
    }

    @AutoValue
    public static abstract class AccessibilityAction {
        public abstract String name();
        public abstract String label();

        public static AccessibilityAction create(String name, String label) {
            return new AutoValue_AccessibilityActions_AccessibilityAction(name, label);
        }
    }

    private static native String nGetAccessibilityActionNameAt(long componentHandle, int propertyKey, int index);
    private static native String nGetAccessibilityActionLabelAt(long componentHandle, int propertyKey, int index);
}
