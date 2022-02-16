/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android;

import androidx.annotation.NonNull;

import com.amazon.apl.android.scaling.IMetricsTransform;

/**
 * Creates a APL EditText Component.
 * See @{link <a https://developer.amazon.com/en-US/docs/alexa/alexa-presentation-language/apl-edittext.html>
 * APL EditText Specification</a>}
 */
public class EditText extends Component {

    private static final String TAG = "EditText";

    /**
     * Override the standard component property map to use a TextProxy.
     * The text proxy has Text specific property getters.
     *
     * @return
     */
    @Override
    protected PropertyMap createPropertyMap() {
        return new EditTextProxy<EditText>() {
            @NonNull
            @Override
            public EditText getMapOwner() {
                return EditText.this;
            }

            @NonNull
            @Override
            public IMetricsTransform getMetricsTransform() {
                return getRenderingContext().getMetricsTransform();
            }
        };
    }

    public EditTextProxy getProxy() {
        return (EditTextProxy)mProperties;
    }

    EditText(long nativeHandle, String componentId, RenderingContext renderingContext) {
        super(nativeHandle, componentId, renderingContext);
    }

    @Override
    public boolean isFocusableInTouchMode() {
        return !isDisabled();
    }

    public boolean isValidCharacter(final char character) {
        return nIsValidCharacter(getNativeHandle(), character);
    }

    private static native boolean nIsValidCharacter(long nativeHandle, char character);

}
