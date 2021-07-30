/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.dependencies;

/**
 * Interface for handling parameter requests:
 * https://developer.amazon.com/en-US/docs/alexa/alexa-presentation-language/apl-layout.html#parameters
 * for the mainTemplate: https://developer.amazon.com/en-US/docs/alexa/alexa-presentation-language/apl-document.html#maintemplate
 * during {@link com.amazon.apl.android.Content} inflation.
 */
public interface IContentDataRetriever extends IContentRetriever<String, String> { }
