/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android;

import androidx.annotation.NonNull;

/**
 * Callbacks for responding to document lifecycle events.
 *
 * Should be registered in {@link IAPLViewPresenter#addDocumentLifecycleListener(IDocumentLifecycleListener)}
 * before {@link APLController#renderDocument(Content, APLOptions, RootConfig, IAPLViewPresenter)}
 * is called to receive all callbacks.
 *
 * Order of callbacks is {@link #onDocumentRender(RootContext)} -> {@link #onDocumentDisplayed()} -> {@link #onDocumentFinish()}
 *
 * Note: This provides default implementations so that implementers can choose what aspects of the document
 * lifecycle they care about.
 */
public interface IDocumentLifecycleListener {
    /**
     * Callback for when a new document has been rendered.
     * @param rootContext the RootContext for that document.
     */
    default void onDocumentRender(@NonNull final RootContext rootContext) {}

    /**
     * Callback for when the document is visible to the user.
     */
    default void onDocumentDisplayed() {}

    /**
     * Callback for when the document has been paused (is visible but not actively processing).
     */
    default void onDocumentPaused() {}

    /**
     * Callback for when the document has been resumed from a paused state.
     */
    default void onDocumentResumed() {}

    /**
     * Callback for when the document is finished.
     */
    default void onDocumentFinish() {}
}
