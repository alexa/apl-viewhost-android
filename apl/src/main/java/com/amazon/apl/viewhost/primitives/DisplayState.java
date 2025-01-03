/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.viewhost.primitives;

/**
 * Represents the display state of the APL view host
 */
public enum DisplayState {
    // The view is not visible on the screen.
    HIDDEN,
    // The view may be visible on the screen or it may be largely obscured by other content on the
    // screen. The view is not the primary focus of the system.
    BACKGROUND,
    // The view is visible on the screen and at the front.
    FOREGROUND
}
