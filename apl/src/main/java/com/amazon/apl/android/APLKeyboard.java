/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android;

import androidx.annotation.NonNull;

import com.amazon.apl.enums.KeyHandlerType;
import com.google.auto.value.AutoValue;

/**
 * Class encapsulating an APL Keyboard Event.
 * See https://developer.amazon.com/en-US/docs/alexa/alexa-presentation-language/apl-keyboard.html#keyboard-event
 */
@AutoValue
public abstract class APLKeyboard {

    @NonNull
    public abstract KeyHandlerType type();
    @NonNull
    public abstract String key();
    @NonNull
    public abstract String code();
    public abstract boolean repeat();
    public abstract boolean shift();
    public abstract boolean alt();
    public abstract boolean ctrl();
    public abstract boolean meta();

    public static Builder builder() {
        return new AutoValue_APLKeyboard.Builder()
                .repeat(false)
                .shift(false)
                .alt(false)
                .ctrl(false)
                .meta(false);
    }

    @AutoValue.Builder
    public static abstract class Builder {
        /**
         * The {@link KeyHandlerType}.
         * @param type up or down
         * @return this builder
         */
        public abstract Builder type(KeyHandlerType type);

        /**
         * The APL Key String see https://www.w3.org/TR/uievents-key/
         * @param key the key string
         * @return this builder
         */
        public abstract Builder key(String key);

        /**
         * The APL Key code see https://www.w3.org/TR/uievents-code/
         * @param code the key code
         * @return this builder
         */
        public abstract Builder code(String code);

        /**
         * If the key is repeating
         * @param repeat if the key is repeating
         * @return this builder
         */
        public abstract Builder repeat(boolean repeat);

        /**
         * @param shift if the shift key is pressed
         * @return this builder
         */
        public abstract Builder shift(boolean shift);

        /**
         * @param alt if the alt key is pressed
         * @return this builder
         */
        public abstract Builder alt(boolean alt);

        /**
         * @param ctrl if the ctrl key is pressed
         * @return this builder
         */
        public abstract Builder ctrl(boolean ctrl);

        /**
         * @param meta if the meta key is pressed
         * @return this builder
         */
        public abstract Builder meta(boolean meta);

        /**
         * Construct an instance of {@link APLKeyboard}
         * @return a new {@link APLKeyboard}
         */
        public abstract APLKeyboard build();
    }
}
