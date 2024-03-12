package com.amazon.apl.viewhost.internal;

import com.amazon.apl.viewhost.DocumentHandle;

/**
 * Listener for any events related to pause or set timeout on the document.
 */
public interface InteractionTimerListener {

    void onPause(DocumentHandle handle);

    void onSetTimeout(DocumentHandle handle, int timeoutMS);
}
