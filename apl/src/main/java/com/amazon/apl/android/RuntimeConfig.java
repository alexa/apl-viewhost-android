/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android;

import androidx.annotation.NonNull;
import com.amazon.apl.android.font.CompatFontResolver;
import com.amazon.apl.android.font.IFontResolver;
import com.google.auto.value.AutoValue;

/*
 *  Runtime Configuration
 */
@AutoValue
public abstract class RuntimeConfig {
    // Runtime Font Resolver
    public abstract IFontResolver getFontResolver();

    // Runtime handles accessibility state changes
    public abstract boolean getAccessibilityHandledByRuntime();

    public abstract boolean isPreloadingFontsEnabled();

    /**
     * @return gets a builder for {@link RuntimeConfig} with default implementation
     */
    public static Builder builder() {
        return new AutoValue_RuntimeConfig.Builder()
                .fontResolver(new CompatFontResolver())
                .accessibilityHandledByRuntime(false)
                .preloadingFontsEnabled(true);
    }

    @AutoValue.Builder
    public abstract static class Builder {
        /**
         * Defaults to {@link CompatFontResolver}
         * @param fontResolver runtime font resolver
         * @return this builder
         */
        public abstract Builder fontResolver(@NonNull IFontResolver fontResolver);

        /**
         * Defaults to true
         * @param shouldPreloadFonts specifies whether the viewhost should preload common fonts
         * @return this builder
         */
        public abstract Builder preloadingFontsEnabled(boolean shouldPreloadFonts);

        /**
         * Defaults to false
         * @param accessibilityHandledByRuntime
         * @return this builder
         */
        public abstract Builder accessibilityHandledByRuntime(boolean accessibilityHandledByRuntime);

        /**
         * Builds the config
         * @return the {@link RuntimeConfig}
         */
        public abstract RuntimeConfig build();
    }
}
