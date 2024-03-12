/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.executers;

import android.graphics.Bitmap;
import android.util.Base64;
import android.util.Log;

import com.amazon.apl.devtools.controllers.DTConnection;
import com.amazon.apl.devtools.enums.CommandMethod;
import com.amazon.apl.devtools.models.Session;
import com.amazon.apl.devtools.models.ViewTypeTarget;
import com.amazon.apl.devtools.models.error.DTException;
import com.amazon.apl.devtools.models.view.ViewCaptureImageCommandRequestModel;
import com.amazon.apl.devtools.models.view.ViewCaptureImageCommandResponse;
import com.amazon.apl.devtools.util.CommandRequestValidator;

import com.amazon.apl.devtools.util.IDTCallback;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;

public final class ViewCaptureImageCommandRequest
        extends ViewCaptureImageCommandRequestModel implements ICommandValidator {
    private static final String TAG = ViewCaptureImageCommandRequest.class.getSimpleName();
    private final CommandRequestValidator mCommandRequestValidator;
    private final DTConnection mConnection;
    private ViewTypeTarget mViewTypeTarget;

    public ViewCaptureImageCommandRequest(CommandRequestValidator commandRequestValidator,
                                          JSONObject obj,
                                          DTConnection connection)
            throws JSONException, DTException {
        super(obj);
        mCommandRequestValidator = commandRequestValidator;
        mConnection = connection;
        validate();
    }

    private byte[] compressBitmapToBytes(Bitmap bitmap, Bitmap.CompressFormat compressFormat, int quality) {
        Log.i(TAG, "Compressing image to " + compressFormat.toString() + " format with " +
                quality + " quality");
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(compressFormat, quality, byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }

    @Override
    public void execute(IDTCallback<ViewCaptureImageCommandResponse> callback) {
        Log.i(TAG, "Executing " + CommandMethod.VIEW_CAPTURE_IMAGE + " command");
        mViewTypeTarget.getCurrentBitmap((bitmap, requestStatus) -> {
            // TODO:: Image compression type and quality is hardcoded, but this may change later
            String imageCompressionType = "image/png";
            byte[] bytes = compressBitmapToBytes(bitmap, Bitmap.CompressFormat.PNG, 100);

            String encodedImageData = Base64.encodeToString(bytes, Base64.DEFAULT);
            callback.execute(new ViewCaptureImageCommandResponse(getId(), getSessionId(), bitmap.getHeight(),
                bitmap.getWidth(), imageCompressionType, encodedImageData), requestStatus);
        });
    }

    @Override
    public void validate() throws DTException {
        Log.i(TAG, "Validating " + CommandMethod.VIEW_CAPTURE_IMAGE + " command");
        mCommandRequestValidator.validateBeforeGettingSession(getId(), getSessionId(), mConnection);
        Session session = mConnection.getSession(getSessionId());
        // TODO:: Validate target type before casting when more target types are added
        mViewTypeTarget = (ViewTypeTarget) session.getTarget();
    }
}
