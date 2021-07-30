/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.dependencies;

import org.json.JSONObject;

/**
 * Defines API for listening to visual context updates.
 */
public interface IVisualContextListener {

    /**
     * Called when the visual context is updated.
     *
     * @param visualContext the updated visual context.
     */
    void onVisualContextUpdate(JSONObject visualContext);

}
