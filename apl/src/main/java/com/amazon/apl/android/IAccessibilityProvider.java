/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android;

import static com.amazon.apl.enums.Role.kRoleNone;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amazon.apl.android.primitive.AccessibilityActions;
import com.amazon.apl.enums.Role;
import com.amazon.apl.enums.UpdateType;

/**
 * Interim interface to reuse {@link APLAccessibilityDelegate} logic
 */
public interface IAccessibilityProvider {
    AccessibilityActions getAccessibilityActions();

    /**
     * Update core when an accessibility action happens
     * @param updateType
     * @param value
     */
    void update(@NonNull UpdateType updateType, String value);
    default Role getRole() {
        return kRoleNone;
    }
    @Nullable
    String getAccessibilityLabel();
    String getId();
    boolean isChecked();
    boolean isDisabled();
    default boolean isScrollable() {
        return false;
    }

    default boolean hasTextProperty() {
        return false;
    }

    default boolean isFocusable() {
        return false;
    }

    default boolean isFocusableInTouchMode() {
        return false;
    }

    default String getAccessibilityNodeText() {
        return null;
    }
}
