/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android;

import java.util.List;

/**
 * @deprecated Use com.amazon.alexaext.LocalExtension instead.
 */
public abstract class LocalExtension extends com.amazon.alexaext.LocalExtension {
    public LocalExtension(String uri) {
        super(uri);
    }

    public LocalExtension(List<String> uris) {
        super(uris);
    }
}
