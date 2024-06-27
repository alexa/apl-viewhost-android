/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.controllers.impl;

import com.amazon.apl.devtools.controllers.IDTServer;

public final class NoOpDTServer implements IDTServer {
    @Override
    public void start(int portNumber) {
    }

    @Override
    public void stop() {
    }
}
