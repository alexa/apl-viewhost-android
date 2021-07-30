/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.views;

import android.content.Context;
import android.view.KeyEvent;

import com.amazon.apl.android.APLGradientDrawable;
import com.amazon.apl.android.IAPLViewPresenter;

/**
 * A subclass of {@link android.widget.EditText} required to provide access to APL view presenter.
 */
@SuppressWarnings("AppCompatCustomView")
public class APLEditText extends android.widget.EditText {
    private final IAPLViewPresenter mPresenter;
    private APLGradientDrawable mGradientDrawable = new APLGradientDrawable();

    public APLEditText(final Context context, final IAPLViewPresenter presenter) {
        super(context);
        mPresenter = presenter;
    }

    /*
     * Workaround for EditText to be handle cursorpositioning via DPAD
     * If the cursor is at the beginning, pass LEFT and UP to core
     * if the cursor is at the end, pass RIGHT and DOWN to core
     * otherwise consume event.
     */
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();

        if (getSelectionStart() == getSelectionEnd()) {

            switch(keyCode) {
                case KeyEvent.KEYCODE_DPAD_LEFT:
                case KeyEvent.KEYCODE_DPAD_UP:
                    if (getSelectionEnd() != 0) {
                        // if LEFT or UP but not at the beginning of the input, just move cursor
                        return super.dispatchKeyEvent(event);
                    }
                    break;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    // if RIGHT or DOWN but not at the end of the input, just move cursor
                    if (getSelectionEnd() != getText().length()) {
                        return super.dispatchKeyEvent(event);
                    }
                    break;
            }
        }
        return mPresenter.onKeyPress(event) || super.dispatchKeyEvent(event);
    }

    public IAPLViewPresenter getPresenter() {
        return mPresenter;
    }

    public APLGradientDrawable getGradientDrawable() {
        return mGradientDrawable;
    }
}
