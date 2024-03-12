/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.developer.views;

import android.annotation.TargetApi;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.PixelCopy;
import android.view.View;

import androidx.annotation.NonNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Class to capture screenshot of a view
 * Uses different APIs to capture screenshot since the APIs were added at different SDK versions
 * Refer https://developer.android.com/reference/android/view/View#getDrawingCache()
 * Refer https://developer.android.com/reference/android/view/PixelCopy
 */
@TargetApi(24)
public class CaptureImageHelper {
    private static final String TAG = "CaptureImageHelper";
    private final Bitmap mUnitBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);

    @NonNull
    public Bitmap captureImage(View view) {
        // Refer https://developer.android.com/reference/android/view/View#getDrawingCache()
        // PixelCopy API is the recommended way and is only available on API levels >= 26
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && view.getContext() instanceof Activity) {
            return captureImage(view, (Activity) view.getContext());
        } else {
            return captureImageLegacy(view);
        }
    }

    @TargetApi(26)
    @NonNull
    private Bitmap captureImage(View view, Activity activity) {
        CompletableFuture<Bitmap> completableFuture = new CompletableFuture<>();

        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        int[] location = new int[2];
        view.getLocationInWindow(location);

        Rect rect = new Rect(location[0], location[1], location[0] + view.getWidth(), location[1] + view.getHeight());
        PixelCopy.OnPixelCopyFinishedListener listener = copyResult -> {

            if (copyResult == PixelCopy.SUCCESS) {
                completableFuture.complete(bitmap);
            } else {
                completableFuture.complete(mUnitBitmap);
            }
        };

        try {
            view.post(() -> PixelCopy.request(activity.getWindow(), rect, bitmap, listener, new Handler()));
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Failed to capture screenshot because: ", e);
        }
        try {
            return completableFuture.get(1000, TimeUnit.MILLISECONDS);
        } catch (ExecutionException e) {
            Log.e(TAG, "Failed to capture screenshot because: ", e);
            return mUnitBitmap;
        } catch (InterruptedException e) {
            Log.e(TAG, "Failed to capture screenshot because: ", e);
            return mUnitBitmap;
        } catch (TimeoutException e) {
            Log.e(TAG, "Failed to capture screenshot because: ", e);
            return mUnitBitmap;
        }
    }

    @NonNull
    private Bitmap captureImageLegacy(@NonNull View view) {
        Bitmap screenshot = Bitmap.createBitmap(view.getMeasuredWidth(), view.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(screenshot);
        view.draw(canvas);
        return screenshot;
    }
}