/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.primitive;

import com.amazon.common.BoundObject;
import com.amazon.apl.enums.APLEnum;
import com.google.auto.value.AutoValue;

import java.util.Collections;
import java.util.Map;

/**
 * UrlRequest Property
 * See https://developer.amazon.com/en-US/docs/alexa/alexa-presentation-language/apl-image.html#source-sources
 */
@AutoValue
public abstract class UrlRequests implements IterableProperty<UrlRequests.UrlRequest> {
    public static final int REPEAT_FOREVER = -1;

    public static UrlRequests create(BoundObject boundObject, APLEnum propertyKey) {
        return IterableProperty.create(new UrlRequestGetter(boundObject, propertyKey));
    }

    @AutoValue
    public static abstract class UrlRequest {
        public abstract String url();
        public abstract Map<String, String> headers();
        public static Builder builder() {
            return new AutoValue_UrlRequests_UrlRequest.Builder().headers(Collections.emptyMap());
        }

        @AutoValue.Builder
        public static abstract class Builder {
            public abstract Builder url(String url);
            public abstract Builder headers(Map<String, String> headers);
            public abstract UrlRequest build();
        }
    }
}

