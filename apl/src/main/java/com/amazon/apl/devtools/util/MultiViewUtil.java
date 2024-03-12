/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.util;

public final class MultiViewUtil {
    public int computeNumberOfColumns(int numberOfViews) {
        if (numberOfViews <= 1) {
            // Return 1 column to avoid divide by zero error when result is used in a grid view
            return 1;
        }
        return (int) Math.ceil(Math.sqrt(numberOfViews));
    }

    public int computeNumberOfRows(int numberOfViews, int numberOfColumns) {
        if (numberOfViews <= 1 || numberOfColumns <= 0) {
            // Return 1 row when the result of division will error or return a non-positive number
            return 1;
        }
        return (int) Math.ceil((double) numberOfViews / numberOfColumns);
    }
}
