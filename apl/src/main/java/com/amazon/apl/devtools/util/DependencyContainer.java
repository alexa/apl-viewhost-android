/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.util;

public final class DependencyContainer {
    private static DependencyContainer sInstance;
    private final TargetCatalog mTargetCatalog;
    private final CommandRequestFactory mCommandRequestFactory;
    private final MultiViewUtil mMultiViewUtil;

    private DependencyContainer() {
        mTargetCatalog = new TargetCatalog();
        CommandMethodUtil commandMethodUtil = new CommandMethodUtil();
        CommandRequestValidator commandRequestValidator =
                new CommandRequestValidator(mTargetCatalog);
        mCommandRequestFactory = new CommandRequestFactory(mTargetCatalog, commandMethodUtil,
                commandRequestValidator);
        mMultiViewUtil = new MultiViewUtil();
    }

    public static DependencyContainer getInstance() {
        if (sInstance == null) {
            sInstance = new DependencyContainer();
        }
        return sInstance;
    }

    public TargetCatalog getTargetCatalog() {
        return mTargetCatalog;
    }

    public CommandRequestFactory getCommandRequestFactory() {
        return mCommandRequestFactory;
    }

    public MultiViewUtil getMultiViewUtil() {
        return mMultiViewUtil;
    }
}
