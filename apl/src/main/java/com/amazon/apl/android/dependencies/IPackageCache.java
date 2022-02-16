/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.dependencies;

import com.amazon.apl.android.APLJSONData;
import com.amazon.apl.android.Content;
import com.amazon.apl.android.utils.ICache;

/**
 * Interface for an in memory Cache for import requests.
 */
public interface IPackageCache extends ICache<Content.ImportRef, APLJSONData> {}
