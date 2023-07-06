/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.viewhost;

/**
 * Represents the state of a document that was prepared but not yet rendered. Prepared documents,
 * minimally, are parsed and have their imports resolved. Different implementations can choose to
 * perform more preparation work, such as initializing requested extensions or fetching additional
 * resources (e.g. images).
 */
public abstract class PreparedDocument {
    /**
     * Determines whether the document has been successfully prepared. Only meaningful if @c isValid
     * is @c true.
     *
     * @return @c true if the document is fully prepared, @c false otherwise
     */
    public abstract boolean isReady();

    /**
     * @return @c true if this this document is being prepared or fully prepared already, @c false
     *         if preparation failed.
     */
    public abstract boolean isValid();

    /**
     * @return @c true if the document has a runtime-specified token, @c false if no token was set
     *         for this document.
     */
    public abstract boolean hasToken();

    /**
     * Returns the token provided for this document at the time it was prepared (defaults to an
     * empty string if no token is specified by the APL runtime). This is an opaque token, the
     * viewhost only ever performs an equality check on it.
     *
     * @return the opaque token for this document
     */
    public abstract String getToken();

    /**
     * The document's unique ID. This ID is create when the document is prepared, and does not
     * change for the entire lifecyle of the document.
     *
     * @return the unique ID of this document
     */
    public abstract String getUniqueID();

    /**
     * @return A handle to the document managed by this instance.
     */
    public abstract DocumentHandle getHandle();
}
