/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.models.network;

import android.net.Uri;
import com.amazon.apl.devtools.enums.DTNetworkRequestType;
import com.amazon.apl.devtools.util.IdGenerator;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * This dev tool interface is to listen to when there is a network call.
 */
public interface IDTNetworkRequestHandler {

    /**
     * The DT Network Request Id Generator.
     */
    IdGenerator IdGenerator = new IdGenerator();

    /**
     * The valid accepted URL schemes.
     * null represents an HTTP URL without any scheme specified, which is supported in APL <= 1.1
     */
    Set<String> ValidUrlScheme = new HashSet<>(Arrays.asList("https", "http", null));

    /**
     * When a network call is about to be made.
     *
     * @param requestId The specific requestId to the network request.
     * @param timestamp The timestamp when the event happened.
     * @param url The URL to where the network call is going to be made to.
     * @param type The type of network request.
     */
    void requestWillBeSent(int requestId, double timestamp, String url, DTNetworkRequestType type);

    /**
     * When a network request fails.
     *
     * @param requestId The specific requestId to the network request.
     * @param timestamp The timestamp when the event happened.
     */
    void loadingFailed(int requestId, double timestamp);

    /**
     * When a network request has successfully finished.
     *
     * @param requestId The specific requestId to the network request.
     * @param timestamp The timestamp when the event happened.
     * @param encodedDataLength The size of the data downloaded.
     */
    void loadingFinished(int requestId, double timestamp, int encodedDataLength);


    /**
     * Checks if the provided path is a URL Request.
     *
     * @param path The path to check if it's a url request.
     * @return true if the path is an url request, false otherwise.
     */
    static boolean isUrlRequest(String path) {
        if (path == null) {
            return false;
        }

        String scheme = Uri.parse(path).getScheme();
        if (scheme == null) {
            return true;
        }
        return ValidUrlScheme.contains(scheme.toLowerCase());
    }

    /**
     * When the source is empty for a package, we need to use the default url.
     *
     * @param packageName The package name we want to import
     * @param version The version of the package.
     * @return the default package url.
     */
    static String getDefaultPackageUrl(String packageName, String version) {
        final String cloudFrontLocationPrefix = "https://arl.assets.apl-alexa.com/packages/";
        final String cloudFrontLocationSuffix = "/document.json";

        return cloudFrontLocationPrefix + packageName + "/" + version + cloudFrontLocationSuffix;
    }
}
