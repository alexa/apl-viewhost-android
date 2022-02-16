/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.dependencies;

import com.amazon.apl.android.APLJSONData;
import com.amazon.apl.android.Content;

/**
 * Interface for handling import requests https://developer.amazon.com/en-US/docs/alexa/alexa-presentation-language/apl-document.html#import
 * during {@link Content} inflation.
 */
public interface IPackageLoader extends IContentRetriever<Content.ImportRequest, APLJSONData> { }
