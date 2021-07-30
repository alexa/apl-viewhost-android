/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android;


import java.util.HashMap;
import java.util.Map;

/**
 * Enumeration of the currently known SDK version codes. Version numbers
 * increment monotonically with each official platform release.
 */
@SuppressWarnings("WeakerAccess")
public class APLVersionCodes {
    public static final int APL_1_0 = 0;

    public static final int APL_1_1 = 1;

    public static final int APL_1_2 = 2;

    public static final int APL_1_3 = 3;

    public static final int APL_1_4 = 4;

    public static final int APL_1_5 = 5;

    public static final int APL_1_6 = 6;

    public static final int APL_1_7 = 7;

    public static final int APL_LATEST = Integer.MAX_VALUE;

    private static final Map<String, Integer> sVersionCodes = new HashMap<>();
    static {
        sVersionCodes.put("1.0", APL_1_0);
        sVersionCodes.put("1.1", APL_1_1);
        sVersionCodes.put("1.2", APL_1_2);
        sVersionCodes.put("1.3", APL_1_3);
        sVersionCodes.put("1.4", APL_1_4);
        sVersionCodes.put("1.5", APL_1_5);
        sVersionCodes.put("1.6", APL_1_6);
        sVersionCodes.put("1.7", APL_1_7);
    }

    /**
     * Derive the version code from a string.
     *
     * @param code The string representing the code.
     * @return APL_VERSION_CODE value, defaults to APL_LATEST if unknown;
     */
    public static int getVersionCode(String code) {
        Integer versionCode = sVersionCodes.get(code);
        return versionCode != null ? versionCode : APL_LATEST;
    }

}
