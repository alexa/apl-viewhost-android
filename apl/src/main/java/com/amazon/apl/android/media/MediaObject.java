package com.amazon.apl.android.media;

import android.util.Size;

import java.io.File;
import java.nio.charset.StandardCharsets;

public class MediaObject {
    private final long mNativeHandle;
    private final String mUrl;
    private File mFile;
    private Size mSize;

    private MediaObject(long nativeHandle, String url) {
        mNativeHandle = nativeHandle;
        mUrl = url;
    }

    public static MediaObject ensure(long nativeHandle) {
        MediaObject mediaObject = nGetJavaObject(nativeHandle);
        if (mediaObject == null) {
            String url = nGetUrl(nativeHandle);
            mediaObject = new MediaObject(nativeHandle, url);
            nBind(nativeHandle, mediaObject);
        }
        return mediaObject;
    }

    public synchronized File getFile() {
        return mFile;
    }

    public synchronized void setFile(File file) {
        mFile = file;
    }

    public synchronized void onLoad(int width, int height) {
        mSize = new Size(width, height);
        nOnLoad(mNativeHandle, width, height);
    }

    public synchronized Size getSize() {
        return mSize;
    }

    public void onError(int errorCode, String errorDescription) {
        nOnError(mNativeHandle, errorCode, errorDescription.getBytes(StandardCharsets.UTF_8));
    }

    public String getUrl() {
        return nGetUrl(mNativeHandle);
    }

    public int hashCode() {
        return mUrl.hashCode();
    }

    private static native void nBind(long nativeHandle, Object instance);
    private native void nOnLoad(long nativeHandle, int width, int height);
    private native void nOnError(long nativeHandle, int errorCode, byte[] errorDescription);
    private static native MediaObject nGetJavaObject(long nativeHandle);
    private static native String nGetUrl(long nativeHandle);
}
