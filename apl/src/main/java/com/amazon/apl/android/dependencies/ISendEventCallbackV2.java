/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.dependencies;

import java.util.Map;

public interface ISendEventCallbackV2 {

    /**
     * Called when a SendEvent command is executed as a result of an onPress event.
     * @param args Arguments corresponding to the evaluated `arguments` array of SendEvent.
     * @param components Map of component ids to component values.
     * @param sources The rich source object describing who raised this event.
     * @param flags  When SendEvent is executed it's possible to set an additional bag of properties
     *   		     that may be used by the runtime or message routing system to customize UserEvent behavior.
     *   		     This is an optional attribute.
     *   		     https://developer.amazon.com/en-US/docs/alexa/alexa-presentation-language/apl-standard-commands.html#flags
     */
    void onSendEvent(Object[] args, Map<String, Object> components, Map<String, Object> sources,
                     Map<String, Object> flags);
}
