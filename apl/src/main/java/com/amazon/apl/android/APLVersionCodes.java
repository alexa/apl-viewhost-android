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

    public static final int APL_1_8 = 8;

    public static final int APL_1_9 = 9;

    public static final int APL_2022_1 = 10;

    public static final int APL_2022_2 = 11;

    public static final int APL_2023_1 = 12;

    public static final int APL_2023_2 = 13;

    public static final int APL_2023_3 = 14;

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
        sVersionCodes.put("1.8", APL_1_8);
        sVersionCodes.put("1.9", APL_1_9);
        sVersionCodes.put("1.10", APL_2022_1);
        sVersionCodes.put("2022.1", APL_2022_1);
        sVersionCodes.put("2022.2", APL_2022_2);
        sVersionCodes.put("2023.1", APL_2023_1);
        sVersionCodes.put("2023.2", APL_2023_2);
        sVersionCodes.put("2023.3", APL_2023_3);
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
