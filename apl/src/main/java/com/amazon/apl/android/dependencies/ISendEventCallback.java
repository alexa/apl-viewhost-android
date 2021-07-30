/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.dependencies;

import java.util.Map;

public interface ISendEventCallback {

    /**
     * Called when a SendEvent command is executed as a result of an onPress event.
     * @param args Arguments corresponding to the evaluated `arguments` array of SendEvent.
     * @param components Map of component ids to component values.
     * @param sources The rich source object describing who raised this event.
     */
    void onSendEvent(Object[] args, Map<String, Object> components, Map<String, Object> sources);
}
