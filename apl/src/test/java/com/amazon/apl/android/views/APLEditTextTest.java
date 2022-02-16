/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.views;

import android.view.KeyEvent;

import com.amazon.apl.android.IAPLViewPresenter;
import com.amazon.apl.android.robolectric.ViewhostRobolectricTest;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class APLEditTextTest extends ViewhostRobolectricTest {
    private APLEditText mAPLEditTextView;

    @Mock
    private IAPLViewPresenter mockPresenter;
    @Mock
    private KeyEvent mockDownKeyEvent;
    @Mock
    private KeyEvent mockUpKeyEvent;
    @Mock
    private KeyEvent mockRightKeyEvent;
    @Mock
    private KeyEvent mockLeftKeyEvent;

    @Before
    public void setup() {
        when(mockDownKeyEvent.getKeyCode()).thenReturn(KeyEvent.KEYCODE_DPAD_DOWN);
        when(mockUpKeyEvent.getKeyCode()).thenReturn(KeyEvent.KEYCODE_DPAD_UP);
        when(mockRightKeyEvent.getKeyCode()).thenReturn(KeyEvent.KEYCODE_DPAD_RIGHT);
        when(mockLeftKeyEvent.getKeyCode()).thenReturn(KeyEvent.KEYCODE_DPAD_LEFT);
        mAPLEditTextView = spy(new APLEditText(getApplication(), mockPresenter));
    }

    @Test
    public void testDispactKeyEvent_left_and_up_when_not_at_the_beginning_consumes_event() {
        when(mAPLEditTextView.getSelectionStart()).thenReturn(1);
        when(mAPLEditTextView.getSelectionEnd()).thenReturn(1);
        mAPLEditTextView.dispatchKeyEvent(mockLeftKeyEvent);
        mAPLEditTextView.dispatchKeyEvent(mockUpKeyEvent);
        verifyZeroInteractions(mockPresenter);
    }

    @Test
    public void testDispactKeyEvent_left_and_up_when_at_the_beginning_calls_core() {
        when(mAPLEditTextView.getSelectionStart()).thenReturn(0);
        when(mAPLEditTextView.getSelectionEnd()).thenReturn(0);
        mAPLEditTextView.dispatchKeyEvent(mockLeftKeyEvent);
        mAPLEditTextView.dispatchKeyEvent(mockUpKeyEvent);
        verify(mockPresenter).onKeyPress(mockLeftKeyEvent);
        verify(mockPresenter).onKeyPress(mockUpKeyEvent);
    }

    @Test
    public void testDispactKeyEvent_right_and_down_when_not_at_the_end_consumes_event() {
        when(mAPLEditTextView.getSelectionStart()).thenReturn(1);
        when(mAPLEditTextView.getSelectionEnd()).thenReturn(1);
        mAPLEditTextView.dispatchKeyEvent(mockRightKeyEvent);
        mAPLEditTextView.dispatchKeyEvent(mockDownKeyEvent);
        verifyZeroInteractions(mockPresenter);
    }

    @Test
    public void testDispactKeyEvent_right_and_down_when_at_the_end_calls_core() {
        when(mAPLEditTextView.getSelectionStart()).thenReturn(0);
        when(mAPLEditTextView.getSelectionEnd()).thenReturn(0);
        mAPLEditTextView.dispatchKeyEvent(mockRightKeyEvent);
        mAPLEditTextView.dispatchKeyEvent(mockDownKeyEvent);
        verify(mockPresenter).onKeyPress(mockRightKeyEvent);
        verify(mockPresenter).onKeyPress(mockDownKeyEvent);
    }
}
