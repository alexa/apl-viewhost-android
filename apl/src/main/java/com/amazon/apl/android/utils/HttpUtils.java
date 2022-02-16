/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.utils;

import java.util.HashMap;
import java.util.Map;

public class HttpUtils {
    private static final String HEADER_DELIMITER = ": ";
    /**
     * Regex to trim trailing whitespace from header name which is
     * invalid according to RFC 7230
     */
    private static final String HEADER_NAME_REGEX = "\\s+$";

    /**
     * Parses a list of {@link String}'s into a {@link Map<String, String>} of
     * HTTP header keys to HTTP header values which are more Java friendly.
     *
     * The input list can be strings of the form:
     *   "headerKey: headerValue"
     *   "headerKeyWithEmptyValue"
     *
     * Empty value strings will have an empty string as the value in the returned Map.
     *
     * @param headers
     * @return
     */
    public static Map<String, String> listToHeadersMap(String[] headers) {
        Map<String, String> headersMap = new HashMap<>();
        if (headers == null) return headersMap;
        for (String header : headers) {
            if (header == null) continue;
            // parse the header key/value like "headerKey: headerValue"
            int splitIndex = header.indexOf(HEADER_DELIMITER);
            if (splitIndex == -1) {
                // Assume it is an empty value
                headersMap.put(header.replaceAll(HEADER_NAME_REGEX, ""), "");
            } else {
                String headerKey = header.substring(0, splitIndex)
                        .replaceAll(HEADER_NAME_REGEX, "");
                headersMap.put(headerKey,
                        header.substring(splitIndex + 1).trim());
            }
        }
        return headersMap;
    }
}
