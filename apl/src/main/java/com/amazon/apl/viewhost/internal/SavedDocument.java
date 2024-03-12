/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.viewhost.internal;

import androidx.annotation.NonNull;

import com.amazon.apl.enums.DisplayState;
import com.amazon.apl.viewhost.DocumentHandle;
import com.google.auto.value.AutoValue;

/**
 * Represents the state of a document when added to the backstack.
 *
 * Only exposed as an opaque type to clients.
 */
@AutoValue
public abstract class SavedDocument {
    @NonNull
    public abstract DocumentHandle getDocumentHandle();

    public static Builder builder() {
        return new AutoValue_SavedDocument.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder documentHandle(DocumentHandle handle);
        public abstract SavedDocument build();
    }
}
