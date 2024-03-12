/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.views;

import com.amazon.apl.android.LiveData;
import com.amazon.apl.devtools.enums.DTError;
import com.amazon.apl.devtools.models.view.ExecuteCommandStatus;
import com.amazon.apl.devtools.util.IDTCallback;
import com.amazon.apl.devtools.util.RequestStatus;
import org.json.JSONObject;

import java.util.List;

/**
 * Interface which exposes various methods to provide the necessary functionality to the dev tools protocol.
 */
public interface IAPLView {

    /**
     * Start recording frame metrics.
     *
     * @param id The request id.
     * @param callback The callback to provide the correct response to the request.
     */
    void startFrameMetricsRecording(int id, IDTCallback<String> callback);

    /**
     * Stops recording frame metrics.
     *
     * @param id The request id.
     * @param callback The callback to provide the correct response to the request.
     */
    void stopFrameMetricsRecording(int id, IDTCallback<List<JSONObject>> callback);

    /**
     * Handles the Document Domain Command Request from dev tools.
     *
     * @param method The type of dev tools document command.
     * @param params The document command request's command parameters.
     * @param callback The Document Domain response callback.
     */
    void documentCommandRequest(int id, String method, JSONObject params, IDTCallback<String> callback);

    /**
     * Adds a runnable to be added to the message queue.
     *
     * @param runnable The {@link Runnable} to execute.
     * @return true if the {@link Runnable} was successfully queued. False otherwise.
     */
    boolean post(Runnable runnable);

    /**
     * @param aplDocument The APL document that should be renderer.
     * @param aplDocumentData The APL document's data.
     */
    default void renderAPLDocument(String aplDocument, String aplDocumentData) {
        // by default no op
    }

    /**
     * @param name The name of the LiveData Object
     * @param liveData The LiveData to be added.
     */
    default void addLiveData(String name, LiveData liveData) {
        // by default no op
    }

    /**
     * @param name The name of the LiveData Object.
     * @param operations The Specific operations to be executed.
     * @param callback The callback to provide the correct response to the request.
     */
    default void updateLiveData(String name, List<LiveData.Update> operations, IDTCallback<Boolean> callback) {
        callback.execute(RequestStatus.failed(0, DTError.METHOD_NOT_IMPLEMENTED));
    }

    /**
     * @param commands The Command to be executed.
     * @param callback The callback to provide the correct response to the request.
     */
    default void executeCommands(String commands, IDTCallback<ExecuteCommandStatus> callback) {
        callback.execute(RequestStatus.failed(0, DTError.METHOD_NOT_IMPLEMENTED));
    }
}
