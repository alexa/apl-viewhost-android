/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.content;

import android.net.Uri;

import androidx.annotation.NonNull;

import com.amazon.apl.android.dependencies.IContentRetriever;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

/**
 * A simple content retriever for https assets.
 */
public class HttpRequestHandler implements ContentRetriever.RequestHandler<String> {
    private final static int READ_TIMEOUT = 3000;

    /**
     * The executor service thread pool.
     */
    private static final String HTTP_METHOD_GET = "GET";


    /**
     * Constructor for the http content retriever.
     */
    public HttpRequestHandler() {

    }

    @Override
    @NonNull
    public List<String> supportedSchemes() {
        return Collections.singletonList("https");
    }

    @Override
    public void fetch(@NonNull Uri source, @NonNull IContentRetriever.SuccessCallback<Uri, String> successCallback, @NonNull IContentRetriever.FailureCallback<Uri> failureCallback) {
        try {
            successCallback.onSuccess(source, loadDocument(source.toString()));
        } catch (Exception e) {
            failureCallback.onFailure(source,"Unable to retrieve asset: " + e.getMessage());
        }
    }

    private String loadDocument(String url) throws IOException {
        InputStream stream = null;
        HttpsURLConnection connection = null;
        StringBuilder result = new StringBuilder();
        BufferedReader reader = null;

        try {
            connection = createHttpConnection(new URL(url));
            //disable caching on the
            connection.setUseCaches(false);
            connection.setDefaultUseCaches(false);
            // Retrieve the response body as an InputStream.
            stream = connection.getInputStream();

            reader = new BufferedReader(
                    new InputStreamReader(
                            stream));
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }
            return result.toString();
        } finally {
            // Close Stream and disconnect HTTPS connection.
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (connection != null) {
                connection.disconnect();
            }
        }

    }

    /**
     * Creates an https url connection.
     * @param url
     * @return the {@link HttpsURLConnection} connection.
     */
    private HttpsURLConnection createHttpConnection(URL url) throws IOException {
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        connection.setReadTimeout(READ_TIMEOUT);
        connection.setRequestMethod(HTTP_METHOD_GET);
        connection.setUseCaches(false);
        return connection;
    }
}
