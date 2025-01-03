package com.amazon.alexaext;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Local extension V2 interface.
 * @apiNote PLEASE ENSURE THAT YOUR EXTENSION FITS WITH LOCAL/BUILT-IN EXTENSION USECASES.
 * GENERALLY IT'S EXCEPTION FROM THE RULES AND NOT RECOMMENDED.
 */

public abstract class LocalExtensionV2 implements IExtension {
    private Set<String> mUris = new HashSet<>();

    IExtensionEventCallback mEventCallback;
    IExtensionCommandResultCallback mCommandCallback;
    ILiveDataUpdateCallback mLiveDataCallback;

    private IExtensionActivityEventCallback mActivityEventCallback;
    private IExtensionActivityCommandResultCallback mActivityCommandResultCallback;
    private ILiveDataActivityUpdateCallback mActivityLiveDataCallback;

    public LocalExtensionV2(String uri) {
        mUris.add(uri);
    }

    public LocalExtensionV2(List<String> uris) {
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
    public void registerCommandResultCallback(IExtensionActivityCommandResultCallback callback) {
        mActivityCommandResultCallback = callback;
    }

    @Override
    public void registerLiveDataUpdateCallback(ILiveDataActivityUpdateCallback callback) {
        mActivityLiveDataCallback = callback;
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
            return mActivityEventCallback.sendExtensionEvent(activity, event);
        }
        return false;
    }

    protected void invokeCommandResultHandler(ActivityDescriptor activity, String result) {
        if (mActivityCommandResultCallback != null) {
            mActivityCommandResultCallback.sendCommandResult(activity, result);
        }
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
            return mActivityLiveDataCallback.invokeLiveDataUpdate(activity, liveDataUpdate);
        }
        return false;
    }
}