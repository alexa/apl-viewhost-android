/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.image.filters;

import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.ScriptIntrinsic;

import org.junit.After;
import org.junit.Before;
import org.mockito.Mock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public abstract class RenderScriptOperationTest<T extends FilterOperation, U extends ScriptIntrinsic> extends FilterOperationTest<T> {
    @Mock
    Allocation mAllocIn;
    @Mock
    Allocation mAllocOut;
    boolean mAllocsUsed;

    public abstract Class<U> getScriptClass();
    public abstract U getScript();

    @Before
    public void init() {
        super.setup();

        when(mAllocIn.getElement()).thenReturn(mock(Element.class));
        when(mRenderScript.createScript(any(), eq(getScriptClass()))).thenAnswer(
                invocation -> {
                    mAllocsUsed = true;
                    return getScript();
                }
        );
    }

    @After
    public void teardown() {
        if (mAllocsUsed) {
            verify(mAllocIn).destroy();
            verify(mAllocOut).destroy();
            verify(getScript()).destroy();
        }
    }
}
