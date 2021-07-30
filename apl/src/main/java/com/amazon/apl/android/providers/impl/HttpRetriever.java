/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.android.providers.impl;

import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import android.util.Log;

import com.amazon.apl.android.providers.IDataRetriever;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.HttpsURLConnection;

/**
 * Default HttpRetriever.
 * TODO - Converge Retrievers.
 */
public class HttpRetriever implements IDataRetriever {
    private static final String TAG = "HttpRetriever";
    private final static int READ_TIMEOUT = 3000;

    /**
     * The executor service thread pool.
     */
    private final ExecutorService mExecutor;

    private static final String HTTP_METHOD_GET = "GET";

    /**
     * Constructor for the http client.
     */
    public HttpRetriever() {
        this(Executors.newCachedThreadPool());
    }

    @VisibleForTesting
    HttpRetriever(ExecutorService executorService) {
        mExecutor = executorService;
    }

    @Override
    public void fetch(String source, @NonNull Callback callback) {
        mExecutor.submit(() -> getData(source, callback));
    }

    /**
     * Cancels all running requests
     */
    @Override
    public void cancelAll() {
        mExecutor.shutdownNow();
    }

    @VisibleForTesting
    public void getData(String url, @NonNull Callback callback) {
        try {
            String content = getData(url);
            postResult(callback, content);
        } catch (IOException e) {
            Log.e(TAG, "IOException is getting the document " + e);
            e.printStackTrace();
        }
    }

    /**
     * Gets Data from the url.
     * @param url
     * @return
     * @throws IOException
     */
    private String getData(String url) throws IOException {
        InputStream stream = null;
        HttpsURLConnection connection = null;
        StringBuilder result = new StringBuilder();
        BufferedReader reader = null;

        try {
            connection = createHttpConnection(new URL(url));
            //disable caching on the
            connection.setUseCaches(false);
            connection.setDefaultUseCaches(false);
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpsURLConnection.HTTP_OK) {
                Log.e(TAG, "HTTP error code: " + responseCode);
                return "";
            }
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
    @Nullable
    private HttpsURLConnection createHttpConnection(URL url) {
        HttpsURLConnection connection = null;
        try {
            connection = (HttpsURLConnection) url.openConnection();
            connection.setReadTimeout(READ_TIMEOUT);
            connection.setRequestMethod(HTTP_METHOD_GET);
            connection.setUseCaches(false);
        } catch (IOException e) {
            Log.e(TAG, String.format("Error while creating http connection", e.getMessage()));
        }
        return connection;
    }

    private void postResult(@NonNull Callback callback, String content) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(() -> {
            callback.success(content);
        });
    }

}
