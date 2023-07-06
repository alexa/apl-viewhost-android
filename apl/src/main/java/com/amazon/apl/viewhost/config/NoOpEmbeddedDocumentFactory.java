/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.viewhost.config;

public class NoOpEmbeddedDocumentFactory implements EmbeddedDocumentFactory{
    @Override
    public void onDocumentRequested(EmbeddedDocumentRequest request) {
    }
}
