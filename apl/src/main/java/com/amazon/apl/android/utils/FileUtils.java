/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.zip.GZIPInputStream;

public final class FileUtils {
    private FileUtils() {}

    /**
     * Reads a resource into a String. Handles gzipped files so long as the extension is .gz.
     * @param path              the path to the file
     * @param fileInputStream   the input stream
     * @return                  the string from the input stream
     * @throws IOException
     */
    public static String readString(String path, InputStream fileInputStream) throws IOException {
        InputStream gzipStream = null;
        Reader decoder = null;
        BufferedReader bufferedReader = null;
        try {
            if (path.endsWith(".gz")) {
                gzipStream = new GZIPInputStream(fileInputStream);
            }
            decoder = new InputStreamReader(gzipStream != null ? gzipStream : fileInputStream);
            bufferedReader = new BufferedReader(decoder);

            StringBuilder result = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                result.append(line);
            }

            return result.toString();
        } finally {
            if (gzipStream != null) {
                gzipStream.close();
            }
            if (decoder != null) {
                decoder.close();
            }
            if (bufferedReader != null) {
                bufferedReader.close();
            }
        }
    }
}
