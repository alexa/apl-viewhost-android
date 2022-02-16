/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.alexaext;

public interface IExtensionProvider {
    /**
     * Create extension proxy. To be overriden by extending class.
     * @param uri Requested extension URI.
     * @return ExtensionProxy corresponding to requested extension, null if can't be provided.
     */
    ExtensionProxy getExtension(String uri);

    /**
     * Check if able to provide requested extension.
     * @param uri Requested extension URI.
     * @return true if can provide, false otherwise.
     */
    boolean hasExtension(String uri);

    /**
     * Clear provider state. Provider invalid after that.
     */
    void finish();
}
