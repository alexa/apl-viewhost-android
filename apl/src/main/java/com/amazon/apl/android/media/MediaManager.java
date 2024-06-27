package com.amazon.apl.android.media;

import android.graphics.BitmapFactory;
import android.util.Log;

import com.amazon.apl.android.APLController;
import com.amazon.apl.android.dependencies.IImageLoader;
import com.amazon.apl.android.image.filters.bitmap.Size;
import com.amazon.apl.android.utils.HttpUtils;
import com.amazon.common.BoundObject;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class MediaManager extends BoundObject {
    private static final String TAG = "MediaManager";
    private IImageLoader mImageLoader;
    private APLController mAplController;
    private Map<String, DownloadOnlyCallback> mPendingCallbacks;

    public MediaManager(APLController aplController, IImageLoader imageLoader) {
        mAplController = aplController;
        mImageLoader = imageLoader;
        mPendingCallbacks = new HashMap<>();
        bind(nCreate());
    }

    public void request(String url, int mediaType, String[] headers, long mediaObjectHandle) {
        MediaObject mediaObject = MediaObject.ensure(mediaObjectHandle);
        requestDownloadOnly(mediaObject, url, HttpUtils.listToHeadersMap(headers));
    }

    public void release(String url) {
        DownloadOnlyCallback pendingCallback = mPendingCallbacks.remove(url);
        if (pendingCallback != null) {
            pendingCallback.cancel();
        }
    }

    private static Size getIntrinsicSize(File imageFile) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        Size size = null;
        try (FileInputStream fis = new FileInputStream(imageFile)) {
            BitmapFactory.decodeFileDescriptor(fis.getFD(), null, options);
            if (options.outWidth > -1 && options.outHeight > -1) {
                size  = Size.create(options.outWidth, options.outHeight);
            }
        } catch (Exception ex) {
            Log.e(TAG, "Unable to decode image size", ex);
        }
        return size;
    }

    private void requestDownloadOnly(MediaObject mediaObject, String url, Map<String, String> headers) {
        DownloadOnlyCallback downloadOnlyCallback = new DownloadOnlyCallback(mAplController, mediaObject);
        IImageLoader.DownloadImageParams downloadImageParams = IImageLoader.DownloadImageParams.builder()
                .path(url)
                .headers(headers)
                .callback(downloadOnlyCallback)
                .build();

        mPendingCallbacks.put(url, downloadOnlyCallback);
        mImageLoader.downloadImage(downloadImageParams);
    }

    private static class DownloadOnlyCallback implements IImageLoader.DownloadImageCallback {
        private final APLController mAplController;
        private final MediaObject mMediaObject;
        private final AtomicBoolean mIsCancelled;

        DownloadOnlyCallback(APLController aplController, MediaObject mediaObject) {
            mAplController = aplController;
            mMediaObject = mediaObject;
            mIsCancelled = new AtomicBoolean(false);
        }

        /**
         * Allows for the the callback to be cancelled to ensure that we do not try to access
         * any already freed MediaObjects.
         */
        public void cancel() {
            mIsCancelled.set(true);
        }

        @Override
        public void onSuccess(File file, String source) {
            Size size = getIntrinsicSize(file);
            if (size != null) {
                mMediaObject.setFile(file);
                mAplController.executeOnCoreThread(() -> {
                    if (!mIsCancelled.get()) {
                        mMediaObject.onLoad(size.width(), size.height());
                    }
                });
            } else {
                Log.e(TAG,"Dropping image because size could not be determined");
            }
        }

        @Override
        public void onError(Exception exception, int errorCode, String source) {
            mAplController.executeOnCoreThread(() -> {
                if (!mIsCancelled.get()) {
                    mMediaObject.onError(errorCode, exception.getMessage());
                }
            });
        }
    }

    private native long nCreate();
}
