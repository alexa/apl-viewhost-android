/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.util;

import static org.junit.Assert.assertEquals;

import com.amazon.apl.devtools.enums.CommandMethod;

import org.junit.Before;
import org.junit.Test;

public class CommandMethodUtilUnitTest {
    private CommandMethodUtil mCommandMethodUtil;

    @Before
    public void setup() {
        mCommandMethodUtil = new CommandMethodUtil();
    }

    @Test
    public void parseMethod_fromTargetAttachToTargetText_returnsCorrectly() {
        CommandMethod expected = CommandMethod.TARGET_ATTACH_TO_TARGET;
        CommandMethod actual = mCommandMethodUtil.parseMethod("Target.attachToTarget");
        assertEquals(expected, actual);
    }

    @Test
    public void parseMethod_fromViewSetDocumentText_returnsCorrectly() {
        CommandMethod expected = CommandMethod.VIEW_SET_DOCUMENT;
        CommandMethod actual = mCommandMethodUtil.parseMethod("View.setDocument");
        assertEquals(expected, actual);
    }

    @Test
    public void parseMethod_fromViewCaptureImageText_returnsCorrectly() {
        CommandMethod expected = CommandMethod.VIEW_CAPTURE_IMAGE;
        CommandMethod actual = mCommandMethodUtil.parseMethod("View.captureImage");
        assertEquals(expected, actual);
    }

    @Test
    public void parseMethod_fromUnknownText_returnsCorrectly() {
        CommandMethod expected = CommandMethod.EMPTY;
        CommandMethod actual = mCommandMethodUtil.parseMethod("Unknown text");
        assertEquals(expected, actual);
    }
}
