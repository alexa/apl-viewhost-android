/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.utils;

import android.content.Context;
import android.view.inputmethod.InputMethodManager;

import androidx.test.platform.app.InstrumentationRegistry;

public class KeyboardHelper {
    /**
     * Reference: https://stackoverflow.com/questions/33970956/test-if-soft-keyboard-is-visible-using-espresso
     *
     * @return true if soft keyboard is open, else false.
     */
    public static boolean isKeyboardOpen() {
        return getInputMethodManager().isAcceptingText();
    }

    /**
     * Retrieves the input method manager wired to instrumentation currently running in the test
     *
     * @return InputMethodManager
     */
    public static InputMethodManager getInputMethodManager() {
        return (InputMethodManager) InstrumentationRegistry.getInstrumentation()
                .getTargetContext().getSystemService(Context.INPUT_METHOD_SERVICE);
    }
}