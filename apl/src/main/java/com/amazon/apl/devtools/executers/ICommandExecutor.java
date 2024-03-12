/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.executers;

import com.amazon.apl.devtools.models.common.Response;
import com.amazon.apl.devtools.util.IDTCallback;
import com.amazon.apl.devtools.util.RequestStatus;

public interface ICommandExecutor<TResponse extends Response> {
    default TResponse execute() {
        return null;
    }

    /**
     * execute accepting a callback should be used when a value from another callback is needed in
     * the web socket response. For example, see how status is used in an execute commands response.
     * This method consumes the result of execute() by default, so that the caller is not forced to
     * implement this method.
     */
    default void execute(IDTCallback<TResponse> callback) {
        callback.execute(execute(), RequestStatus.successful());
    }
}
