/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.viewhost.internal.message.action;

import androidx.annotation.NonNull;
import android.util.Log;

import com.amazon.apl.viewhost.DocumentHandle;
import com.amazon.apl.viewhost.message.action.ActionMessage;
import com.amazon.apl.viewhost.primitives.Decodable;

import java.lang.ref.WeakReference;

/**
 * Implement the action message contract
 */
public class ActionMessageImpl extends ActionMessage {
    private static final String TAG = "ActionMessageImpl";

    /**
     * Internal interface for routing action message responses from the runtime. Implementers of
     * this interface should not block runtime thread and should defer work to an appropriate
     * worker internal to the view host.
     */
    public interface ResponseListener {
        void onSuccess(Decodable response);

        void onFailure(String reason);
    }

    private final int mId;
    @NonNull
    private final DocumentHandle mDocument;
    private final String mType;
    @NonNull
    private final Decodable mPayload;
    private final WeakReference<ResponseListener> mWeakListener;

    public ActionMessageImpl(int id, DocumentHandle document, String type, Decodable payload,
                             ResponseListener listener) {
        mId = id;
        mDocument = document;
        mType = type;
        mPayload = payload;
        mWeakListener = new WeakReference<>(listener);
    }

    public ActionMessageImpl(int id, DocumentHandle document, String type, Decodable payload) {
        // Provide default response listener which simply logs the success or failure
        this(id, document, type, payload, new ResponseListener() {
            @Override
            public void onSuccess(Decodable response) {
                Log.i(TAG, String.format("%s message (id=%d) succeeded.", type, id));
            }

            @Override
            public void onFailure(String reason) {
                Log.w(TAG, String.format("%s message (id=%d) failed. Reason given was: %s", type, id, reason));
            }
        });
    }

    @Override
    public int getId() {
        return mId;
    }

    @Override
    public DocumentHandle getDocument() {
        return mDocument;
    }

    @Override
    public String getType() {
        return mType;
    }

    @Override
    public Decodable getPayload() {
        return mPayload;
    }

    @Override
    public boolean succeed() {
        return succeed(null);
    }

    @Override
    public boolean succeed(Decodable payload) {
        ResponseListener listener = mWeakListener.get();
        if (listener == null) {
            Log.w(TAG, String.format("Ignoring success for %s message, consumer has gone away.", mType));
            return false;
        }
        listener.onSuccess(payload);
        return true;
    }

    @Override
    public boolean fail(String reason) {
        ResponseListener listener = mWeakListener.get();
        if (listener == null) {
            Log.w(TAG, String.format("Ignoring failure for %s message, consumer has gone away.", mType));
            return false;
        }
        listener.onFailure(reason);
        return true;
    }
}
