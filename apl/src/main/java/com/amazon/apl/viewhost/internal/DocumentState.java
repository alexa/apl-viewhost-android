/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.viewhost.internal;

//https://aplspec.aka.corp.amazon.com/apl-view-host-1.0/latest/html/main.html#document-message-documentstatechanged a
enum DocumentState {
    PENDING,
    PREPARED,
    INFLATED,
    DISPLAYED,
    FINISHED,
    ERROR
}
