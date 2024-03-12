/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.viewhost.internal;

/**
 * Enums to define the document state.
 */
public enum DocumentState {
    //The first step in document creation when the document preparation is in progress.
    PENDING,
    //The pre-inflation dependencies have been satisfied (imported packages, extensions loaded).
    PREPARED,
    //The APL engine has inflated all of the components needed for the first frame.
    INFLATED,
    //The APL engine has prepared a visual representations of components needed for the first frame and has handed off
    // instructions to the platform for rendering. This corresponds to when the VUPL clock is stopped.
    DISPLAYED,
    //All resources associated with a document have been released and no further interaction is possible.
    FINISHED,
    //The document has resulted in a permanent failure.
    ERROR
}
