/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.events;

import com.amazon.apl.android.Event;
import com.amazon.apl.android.RootContext;
import com.amazon.apl.android.providers.IImageLoaderProvider;
import com.amazon.apl.enums.EventMediaType;
import com.amazon.apl.enums.EventProperty;

/**
 * Initiate a media load request for the specific media type.
 */
public class LoadMediaRequestEvent extends Event {
    private final IImageLoaderProvider mImageLoaderProvider;

    protected LoadMediaRequestEvent(long nativeHandle, RootContext rootContext, IImageLoaderProvider imageLoaderProvider) {
        super(nativeHandle, rootContext);
        this.mImageLoaderProvider = imageLoaderProvider;
    }

    public static LoadMediaRequestEvent create(long nativeHandle, RootContext rootContext, IImageLoaderProvider imageLoaderProvider) {
        return new LoadMediaRequestEvent(nativeHandle, rootContext, imageLoaderProvider);
    }

    @Override
    public void execute() {
        String url = mProperties.getString(EventProperty.kEventPropertySource);
        EventMediaType mediaType = EventMediaType.valueOf(mProperties.getEnum(EventProperty.kEventPropertyMediaType));

        // TODO: Based on event type build abstract factory and execute on custom Image/Video requestor.
    }

    @Override
    public void terminate() {

    }
}
