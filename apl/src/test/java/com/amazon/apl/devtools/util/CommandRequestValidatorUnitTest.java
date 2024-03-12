/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.util;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.amazon.apl.devtools.controllers.DTConnection;
import com.amazon.apl.devtools.models.Target;
import com.amazon.apl.devtools.models.error.DTException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.Collection;

public class CommandRequestValidatorUnitTest {
    @Mock
    private TargetCatalog mTargetCatalog;

    // Mock for validateBeforeCreatingSession
    @Mock
    private DTConnection mConnection;

    // Mock for validateBeforeCreatingSession and validateBeforeGettingSession
    @Mock
    private Target mTarget;


    private CommandRequestValidator mCommandRequestValidator;

    @Before
    public void setup() {
        mTargetCatalog = mock(TargetCatalog.class);

        // Setup for validateBeforeCreatingSession
        mConnection = mock(DTConnection.class);
        mTarget = mock(Target.class);
        Collection<String> registeredSessionIds = new ArrayList<>();
        registeredSessionIds.add("target100");
        when(mTarget.getRegisteredSessionIds()).thenReturn(registeredSessionIds);

        mCommandRequestValidator = new CommandRequestValidator(mTargetCatalog);
    }

    @Test
    public void validateBeforeGettingTargetFromTargetCatalog_whenTargetExists_doesNotThrow() {
        when(mTargetCatalog.has(any())).thenReturn(true);
        try {
            mCommandRequestValidator.validateBeforeGettingTargetFromTargetCatalog(100,
                    "target100");
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void validateBeforeGettingTargetFromTargetCatalog_whenTargetDoesNotExist_throws() {
        when(mTargetCatalog.has(any())).thenReturn(false);
        try {
            mCommandRequestValidator.validateBeforeGettingTargetFromTargetCatalog(100,
                    "target100");
            fail("Method should throw exception");
        } catch (Exception e) {
            assertTrue(e instanceof DTException);
        }
    }

    @Test
    public void validateBeforeCreatingSession_whenConnectionIsNotAttachedToTarget_doesNotThrow() {
        when(mConnection.hasSession(any())).thenReturn(false);
        try {
            mCommandRequestValidator.validateBeforeCreatingSession(100, mConnection, mTarget);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void validateBeforeCreatingSession_whenConnectionIsAlreadyAttachedToTarget_throws() {
        when(mConnection.hasSession(any())).thenReturn(true);
        try {
            mCommandRequestValidator.validateBeforeCreatingSession(100, mConnection, mTarget);
            fail("Method should throw exception");
        } catch (Exception e) {
            assertTrue(e instanceof DTException);
        }
    }

    @Test
    public void validateBeforeGettingSession_whenConnectionOwnsSession_doesNotThrow() {
        when(mConnection.hasSession(any())).thenReturn(true);
        try {
            mCommandRequestValidator.validateBeforeGettingSession(100, "session100",
                    mConnection);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void validateBeforeGettingSession_whenConnectionDoesNotOwnSession_throws() {
        when(mConnection.hasSession(any())).thenReturn(false);
        try {
            mCommandRequestValidator.validateBeforeGettingSession(100, "session100",
                    mConnection);
            fail("Method should throw exception");
        } catch (Exception e) {
            assertTrue(e instanceof DTException);
        }
    }

    @Test
    public void validatePerformanceEnabled_whenPerformanceEnabled_doesNotThrows() {
        try {
            mCommandRequestValidator.validatePerformanceEnabled(100, "session100",
                    true);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void validatePerformanceEnabled_whenPerformanceDisabled_throws() {
        try {
            mCommandRequestValidator.validatePerformanceEnabled(100, "session100",
                    false);
            fail("Method should throw exception");
        } catch (Exception e) {
            assertTrue(e instanceof DTException);
        }
    }

    @Test
    public void validateLogIsEnabled_whenLogIsDisabled_throws() {
        try {
            mCommandRequestValidator.validateLogEnabled(100, "session100",
                    false);
            fail("Method should throw exception");
        } catch (Exception e) {
            assertTrue(e instanceof DTException);
        }
    }

    @Test
    public void validateLogIsEnabled_whenLogIsEnabled_doesNotThrows() {
        try {
            mCommandRequestValidator.validateLogEnabled(100, "session100",
                    true);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }
}
