/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.events;

import android.view.View;

import androidx.annotation.NonNull;

import com.amazon.apl.android.Event;
import com.amazon.apl.android.IAPLViewPresenter;
import com.amazon.apl.android.RootContext;
import com.amazon.apl.android.Video;
import com.amazon.apl.android.component.VideoViewAdapter;
import com.amazon.apl.android.dependencies.IMediaPlayer;
import com.amazon.apl.android.primitive.Rect;
import com.amazon.apl.enums.EventControlMediaCommand;
import com.amazon.apl.enums.EventProperty;


/**
 * APL ControlMedia Event
 * See @{link <a https://developer.amazon.com/docs/alexa-presentation-language/apl-commands-media.html#controlmedia>
 *     APL Command Specification</a>}
 */
public class ControlMediaEvent extends Event {

    private static final String TAG = "ControlMediaEvent";

    /**
     * Constructs the Event.
     *
     * @param nativeHandle Handle to the native event.
     * @param rootContext The root context for the event.
     */
    private ControlMediaEvent(long nativeHandle, RootContext rootContext) {
        super(nativeHandle, rootContext);
    }


    static public ControlMediaEvent create(long nativeHandle, RootContext rootContext) {
        return new ControlMediaEvent(nativeHandle, rootContext);
    }

    /**
     * Execute the command.
     */
    @Override
    public void execute() {
        final Video video = (Video) getComponent();

        // Temporary fix to inflate the video if it doesn't exist for backwards compatibility
        // TODO: remove this once long term fix added
        IAPLViewPresenter viewPresenter = mRootContext.getViewPresenter();
        View view = viewPresenter.findView(video);
        Rect bounds = video.getBounds();

        if (view == null && (bounds.getWidth() == 0.0f || bounds.getHeight() == 0.0f)) {
            VideoViewAdapter.getInstance().inflateViewWithNonZeroDimensions(video, viewPresenter);
        }

        EventControlMediaCommand commandControlMedia = EventControlMediaCommand.valueOf(
                mProperties.getEnum(EventProperty.kEventPropertyCommand));
        int value = mProperties.getInt(EventProperty.kEventPropertyValue);
        video.setFromEvent(true);
        handleControlMedia(video.getMediaPlayer(), commandControlMedia, value);
        // resolve this event right away.
        resolve();
    }

    /**
     * Terminate the event process.
     */
    @Override
    public void terminate() {
        // no-op
    }

    private static void handleControlMedia(@NonNull IMediaPlayer mediaPlayer,
                                           EventControlMediaCommand controlMedia,
                                           int value) {
        switch (controlMedia) {
            case kEventControlMediaPlay:
                mediaPlayer.play();
                break;
            case kEventControlMediaPause:
                mediaPlayer.pause();
                break;
            case kEventControlMediaNext:
                mediaPlayer.next();
                break;
            case kEventControlMediaPrevious:
                mediaPlayer.previous();
                break;
            case kEventControlMediaRewind:
                mediaPlayer.rewind();
                break;
            case kEventControlMediaSeek:
                mediaPlayer.seek(value + mediaPlayer.getCurrentSeekPosition());
                break;
            case kEventControlMediaSetTrack:
                mediaPlayer.setTrack(value);
                break;
            default:
                break;
        }
    }
}
