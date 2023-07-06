/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.alexaext;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Local extension interface.
 * @apiNote PLEASE ENSURE THAT YOUR EXTENSION FITS WITH LOCAL/BUILT-IN EXTENSION USECASES.
 * GENERALLY IT'S EXCEPTION FROM THE RULES AND NOT RECOMMENDED.
 */
public abstract class LocalExtension implements IExtension {
    private static String COMMAND_SUCCESS = "{" +
            "    \"version\": \"1.0\"," +
            "    \"method\": \"CommandSuccess\"," +
            "    \"id\": %d" +
            "}";

    private static String COMMAND_FAILURE = "{" +
            "    \"version\": \"1.0\"," +
            "    \"method\": \"CommandFailure\"," +
            "    \"id\": %d," +
            "    \"code\": %d," +
            "    \"message\": %s" +
            "}";

    private Set<String> mUris = new HashSet<>();

    IExtensionEventCallback mEventCallback;
    IExtensionCommandResultCallback mCommandCallback;
    ILiveDataUpdateCallback mLiveDataCallback;

    IExtensionActivityEventCallback mActivityEventCallback;
    IExtensionActivityCommandResultCallback mActivityCommandCallback;
    ILiveDataActivityUpdateCallback mActivityLiveDataCallback;

    public LocalExtension(String uri) {
        mUris.add(uri);
    }

    public LocalExtension(List<String> uris) {
        mUris.addAll(uris);
    }

    @Override
    public Set<String> getURIs() {
        return mUris;
    }

    @Override
    public void registerEventCallback(IExtensionEventCallback callback) {
        mEventCallback = callback;
    }

    @Override
    public void registerLiveDataUpdateCallback(ILiveDataUpdateCallback callback) {
        mLiveDataCallback = callback;
    }

    @Override
    public void registerCommandResultCallback(IExtensionCommandResultCallback callback) {
        mCommandCallback = callback;
    }

    @Override
    public void registerEventCallback(IExtensionActivityEventCallback callback) {
        mActivityEventCallback = callback;
    }

    @Override
    public void registerLiveDataUpdateCallback(ILiveDataActivityUpdateCallback callback) {
        mActivityLiveDataCallback = callback;
    }

    @Override
    public void registerCommandResultCallback(IExtensionActivityCommandResultCallback callback) {
        mActivityCommandCallback = callback;
    }

    /**
     * Invoke an extension event handler in the document.
     *
     * @param activity ActivityDescriptor.
     * @param event The extension generated event.
     * @return true if the event is delivered, false if there is no callback registered.
     */
    protected boolean invokeExtensionEventHandler(ActivityDescriptor activity, String event) {
        if (mActivityEventCallback != null) {
            mActivityEventCallback.sendExtensionEvent(activity, event);
            return true;
        }
        return false;
    }

    /**
     * Invoke an live data binding change, or data update handler in the document.
     *
     * @param activity ActivityDescriptor.
     * @param liveDataUpdate The extension generated event.
     * @return true if the event is delivered, false if there is no callback registered.
     */
    protected boolean invokeLiveDataUpdate(ActivityDescriptor activity, String liveDataUpdate) {
        if (mActivityLiveDataCallback != null) {
            mActivityLiveDataCallback.invokeLiveDataUpdate(activity, liveDataUpdate);
            return true;
        }
        return false;
    }

    /**
     * Notify system about command success.
     *
     * @param activity ActivityDescriptor.
     * @param id Command id.
     */
    protected void commandSuccess(ActivityDescriptor activity, int id) {
        if (mActivityCommandCallback != null) {
            mActivityCommandCallback.sendCommandResult(activity, String.format(COMMAND_SUCCESS, id));
        }
    }

    /**
     * Notify system about command failure.
     *
     * @param activity ActivityDescriptor.
     * @param id Command id.
     * @param code error code.
     * @param message error message.
     */
    protected void commandFailure(ActivityDescriptor activity, int id, int code, String message) {
        if (mActivityCommandCallback != null) {
            mActivityCommandCallback.sendCommandResult(activity, String.format(COMMAND_FAILURE, id, code, message));
        }
    }

    /**
     * Invoke an extension event handler in the document.
     *
     * @param uri The extension URI.
     * @param event The extension generated event.
     * @return true if the event is delivered, false if there is no callback registered.
     * @deprecated see {@link #invokeExtensionEventHandler(ActivityDescriptor,String)}
     */
    @Deprecated
    protected boolean invokeExtensionEventHandler(String uri, String event) {
        if (mEventCallback != null) {
            mEventCallback.sendExtensionEvent(uri, event);
            return true;
        }
        return false;
    }

    /**
     * Invoke an live data binding change, or data update handler in the document.
     *
     * @param uri The extension URI.
     * @param liveDataUpdate The extension generated event.
     * @return true if the event is delivered, false if there is no callback registered.
     * @deprecated see {@link #invokeLiveDataUpdate(ActivityDescriptor,String)}
     */
    @Deprecated
    protected boolean invokeLiveDataUpdate(String uri, String liveDataUpdate) {
        if (mLiveDataCallback != null) {
            mLiveDataCallback.invokeLiveDataUpdate(uri, liveDataUpdate);
            return true;
        }
        return false;
    }

    /**
     * Notify system about command success.
     *
     * @param uri The extension URI.
     * @param id Command id.
     * @deprecated see {@link #commandSuccess(ActivityDescriptor,int)}
     */
    @Deprecated
    protected void commandSuccess(String uri, int id) {
        if (mCommandCallback != null) {
            mCommandCallback.sendCommandResult(uri, String.format(COMMAND_SUCCESS, id));
        }
    }

    /**
     * Notify system about command failure.
     *
     * @param uri The extension URI.
     * @param id Command id.
     * @param code error code.
     * @param message error message.
     * @deprecated see {@link #commandFailure(ActivityDescriptor,int,int,String)}
     */
    @Deprecated
    protected void commandFailure(String uri, int id, int code, String message) {
        if (mCommandCallback != null) {
            mCommandCallback.sendCommandResult(uri, String.format(COMMAND_FAILURE, id, code, message));
        }
    }

}
