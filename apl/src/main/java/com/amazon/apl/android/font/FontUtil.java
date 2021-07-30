/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.font;

import android.graphics.Typeface;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import com.amazon.apl.android.BuildConfig;
import com.amazon.apl.android.thread.SequentialExecutor;
import com.amazon.apl.android.thread.Threading;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Utility for fetching Typeface for font-family + weight + italic state.  This class wraps
 * the Android api because it differs across versions, and is deficient when finding system
 * fonts based on weight.  The Amazon fonts are retrieved from resources to guarantee it's
 * availability across all devices.
 */
public final class FontUtil {

    static final SequentialExecutor SEQUENTIAL_EXECUTOR = Threading.createSequentialExecutor();

    private static final String TAG = "FontUtil";
    private static final String ARABIC_TEXT_PREFIX = "ar-";

    /**
     * DEBUG use only - this uses reflection.
     * Gets the system fonts.  Not included are resource based fonts.
     *
     * @return Map of fonts provided by system.
     */
    @SuppressWarnings("unused")
    private static Map<String, Typeface> debugGetSystemFontMap() {
        Map<String, Typeface> systemFontMap = null;

        if (BuildConfig.DEBUG) {

            try {
                // Get the system font map via reflection
                //noinspection JavaReflectionMemberAccess
                Field f = Typeface.class.getDeclaredField("sSystemFontMap");
                f.setAccessible(true);
                //noinspection unchecked
                systemFontMap = (Map<String, Typeface>) f.get(Typeface.DEFAULT);

                // Log all the system installed fonts
                for (Map.Entry<String, Typeface> typefaceEntry : systemFontMap.entrySet()) {
                    Typeface tf = typefaceEntry.getValue();
                    Log.i(TAG, "System FileFontKey: " + typefaceEntry.getKey() + "\n\t"
                            + "  bold: " + tf.isBold()
                            + "  Italic: " + tf.isItalic() + "  Weight: "
                            + ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.P ? tf.getWeight() : "??"))
                    );
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return systemFontMap;
    }

    /**
     * DEBUG use only
     * Gets a list of fonts associated with a system font family. Fonts from resources not included.
     *
     * @param value Typeface representing the font family.
     * @return List of fonts with the family. Empty list for any resource based font.
     */
    @SuppressWarnings("unused")
    public static List<String> debugGetSystemFontFamily(Map<String, Typeface> fontMap, Typeface value) {
        List<String> arr = new ArrayList<>();

        if (BuildConfig.DEBUG) {

            Set set = fontMap.entrySet();
            for (Object obj : set) {
                Map.Entry entry = (Map.Entry) obj;
                if (entry.getValue().equals(value)) {
                    String str = (String) entry.getKey();
                    arr.add(str);
                }
            }
        }

        return arr;
    }
    
    static boolean isArabicFontKey(FontKey key) {
        return !TextUtils.isEmpty(key.getLanguage()) && key.getLanguage().startsWith(ARABIC_TEXT_PREFIX);
    }
}
