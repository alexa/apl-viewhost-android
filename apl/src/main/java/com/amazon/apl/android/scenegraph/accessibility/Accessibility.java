/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.scenegraph.accessibility;

import androidx.annotation.NonNull;

import com.amazon.apl.android.IAccessibilityProvider;
import com.amazon.apl.android.primitive.AccessibilityActions;
import com.amazon.apl.android.utils.JNIUtils;
import com.amazon.apl.enums.Role;
import com.amazon.apl.enums.UpdateType;

import java.nio.charset.StandardCharsets;

public class Accessibility implements IAccessibilityProvider {
    private final long mAddress;
    private final String mId;
    private boolean mIsChecked;
    private boolean mIsDisabled;
    private boolean mIsScrollable;

    public Accessibility(long address, String id, boolean isScrollable, boolean isChecked, boolean isDisabled) {
        mAddress = address;
        mId = id;
        mIsScrollable = isScrollable;
        mIsChecked = isChecked;
        mIsDisabled = isDisabled;
    }

    @Override
    public boolean isScrollable() {
        return mIsScrollable;
    }

    @Override
    public AccessibilityActions getAccessibilityActions() {
        AccessibilityActions accessibilityActions = AccessibilityActions.create();
        int actionsSize = nGetActionsSize(mAddress);
        for (int i = 0; i < actionsSize; i++) {
            accessibilityActions.add(AccessibilityActions.AccessibilityAction.create(nGetActionNameAt(mAddress, i),
                                                                                     nGetActionLabelAt(mAddress, i)));
        }
        return accessibilityActions;
    }

    @Override
    public String getAccessibilityLabel() {
        String accessibilityLabel = nGetAccessibilityLabel(mAddress);
        if (accessibilityLabel != null) {
            // The API fails if null value is passed.
            return JNIUtils.safeStringValues(nGetAccessibilityLabel(mAddress));
        }
        return null;
    }

    @Override
    public String getId() {
        return mId;
    }

    public Role getRole() {
        return Role.valueOf(nGetRole(mAddress));
    }

    @Override
    public boolean isChecked() {
        return mIsChecked;
    }

    @Override
    public boolean isDisabled() {
        return mIsDisabled;
    }

    @Override
    public void update(@NonNull UpdateType updateType, String value) {
        nExecuteCallback(mAddress, (value + "\0").getBytes(StandardCharsets.UTF_8));
    }

    private static native void nExecuteCallback(long nativeAddress, byte[] value);
    private static native int nGetRole(long nativeAddress);
    private static native String nGetAccessibilityLabel(long nativeAddress);
    private static native int nGetActionsSize(long nativeAddress);
    private static native String nGetActionLabelAt(long nativeAddress, int index);
    private static native String nGetActionNameAt(long nativeAddress, int index);
}
