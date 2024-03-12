/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.util;

import com.amazon.apl.devtools.enums.CommandMethod;

public final class CommandMethodUtil {
    public CommandMethod parseMethod(String methodStr) {
        for (CommandMethod commandMethod : CommandMethod.values()) {
            if (commandMethod.toString().equals(methodStr)) {
                return commandMethod;
            }
        }
        return CommandMethod.EMPTY;
    }
}
