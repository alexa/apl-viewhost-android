/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.utils;

import android.os.Build;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

public class JNIUtils {
    private static final String TAG = "JNIUtils";
    private static final Charset cs = Charset.forName("CESU-8");

    /**
     * removes unmappable characters from strings for SDK 22 and below
     * In every other case, just returns the unmodified input string
     * @param string the string to be checked
     * @return a string without unmappable or broken characters for SDK <+ 22
     */
    public static String safeStringValues(String string) {
        /**
         * SDK 22 doesn't support the full UTF chart, which will result in failures in upstream parsing methods (e.g. styledtextstate.cpp)
         * as a workaround we just ignore unmappable characters.
         */
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1) {
            CharsetDecoder decoder = cs.newDecoder().onMalformedInput(CodingErrorAction.IGNORE).onUnmappableCharacter(CodingErrorAction.IGNORE);
            try {
                return decoder.decode(ByteBuffer.wrap(string.getBytes(StandardCharsets.UTF_8))).toString();
            } catch (CharacterCodingException e) {
                Log.e(TAG, "failed conversion", e);
                return "";
            }
        } else {
            return string;
        }
    }
}
