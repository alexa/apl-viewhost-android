/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android;

import android.view.KeyEvent;

import com.amazon.apl.android.robolectric.ViewhostRobolectricTest;
import com.amazon.apl.enums.KeyHandlerType;

import static org.hamcrest.core.Is.is;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.nullValue;

public class KeyboardTranslatorTest extends ViewhostRobolectricTest {

    @Test
    public void translate_WhenCalledWithUnicodeKey_Translates() {
        // Arrange
        final KeyboardTranslator translator = createKeyboardTranslator();
        final APLKeyboard expected = APLKeyboard.builder()
                .type(KeyHandlerType.kKeyDown)
                .key("g")
                .code("KeyG")
                .build();

        // Act
        final APLKeyboard result = translator.translate(new KeyEvent(
                KeyEvent.ACTION_DOWN,
                KeyEvent.KEYCODE_G));

        // Assert
        assertThat(result, is(expected));
    }

    @Test
    public void translate_WhenCalledWithShift_Translates() {
        // Arrange
        final KeyboardTranslator translator = createKeyboardTranslator();
        final APLKeyboard expected = APLKeyboard.builder()
                .type(KeyHandlerType.kKeyDown)
                .key("2")
                .code("Digit2")
                .shift(true)
                .build();

        // Act
        final APLKeyboard result = translator.translate(new KeyEvent(
                0,
                0,
                KeyEvent.ACTION_DOWN,
                KeyEvent.KEYCODE_2,
                0,
                KeyEvent.META_SHIFT_ON));

        // Assert
        assertThat(result, is(expected));
    }

    @Test
    public void translate_WhenCalledWithAlt_Translates() {
        // Arrange
        final KeyboardTranslator translator = createKeyboardTranslator();
        final APLKeyboard expected = APLKeyboard.builder()
                .type(KeyHandlerType.kKeyDown)
                .key("2")
                .code("Digit2")
                .alt(true)
                .build();

        // Act
        final APLKeyboard result = translator.translate(new KeyEvent(
                0,
                0,
                KeyEvent.ACTION_DOWN,
                KeyEvent.KEYCODE_2,
                0,
                KeyEvent.META_ALT_ON));

        // Assert
        assertThat(result, is(expected));
    }

    @Test
    public void translate_WhenCalledWithCtrl_Translates() {
        // Arrange
        final KeyboardTranslator translator = createKeyboardTranslator();
        final APLKeyboard expected = APLKeyboard.builder()
                .type(KeyHandlerType.kKeyDown)
                .key("2")
                .code("Digit2")
                .ctrl(true)
                .build();

        // Act
        final APLKeyboard result = translator.translate(new KeyEvent(
                0,
                0,
                KeyEvent.ACTION_DOWN,
                KeyEvent.KEYCODE_2,
                0,
                KeyEvent.META_CTRL_ON));

        // Assert
        assertThat(result, is(expected));
    }

    @Test
    public void translate_WhenCalledWithMeta_Translates() {
        // Arrange
        final KeyboardTranslator translator = createKeyboardTranslator();
        final APLKeyboard expected = APLKeyboard.builder()
                .type(KeyHandlerType.kKeyDown)
                .key("2")
                .code("Digit2")
                .meta(true)
                .build();

        // Act
        final APLKeyboard result = translator.translate(new KeyEvent(
                0,
                0,
                KeyEvent.ACTION_DOWN,
                KeyEvent.KEYCODE_2,
                0,
                KeyEvent.META_META_ON));

        // Assert
        assertThat(result, is(expected));
    }

    @Test
    public void translate_WhenCalledWithKeyUp_Translates() {
        // Arrange
        final KeyboardTranslator translator = createKeyboardTranslator();
        final APLKeyboard expected = APLKeyboard.builder()
                .type(KeyHandlerType.kKeyUp)
                .key("2")
                .code("Digit2")
                .build();

        // Act
        final APLKeyboard result = translator.translate(new KeyEvent(
                0,
                0,
                KeyEvent.ACTION_UP,
                KeyEvent.KEYCODE_2,
                0,
                0));

        // Assert
        assertThat(result, is(expected));
    }

    @Test
    public void translate_WhenCalledWithUnknownKeyAction_TranslatesAsKeyDown() {
        // Arrange
        final KeyboardTranslator translator = createKeyboardTranslator();
        final APLKeyboard expected = APLKeyboard.builder()
                .type(KeyHandlerType.kKeyDown)
                .key("2")
                .code("Digit2")
                .build();

        // Act
        final APLKeyboard result = translator.translate(new KeyEvent(
                0,
                0,
                KeyEvent.ACTION_MULTIPLE,
                KeyEvent.KEYCODE_2,
                0,
                0));

        // Assert
        assertThat(result, is(expected));
    }

    @Test
    public void translate_WhenCalledWithPositiveRepeatCount_Translates() {
        // Arrange
        final KeyboardTranslator translator = createKeyboardTranslator();
        final APLKeyboard expected = APLKeyboard.builder()
                .type(KeyHandlerType.kKeyDown)
                .key("2")
                .code("Digit2")
                .repeat(true)
                .build();

        // Act
        final APLKeyboard result = translator.translate(new KeyEvent(
                0,
                0,
                KeyEvent.ACTION_DOWN,
                KeyEvent.KEYCODE_2,
                1,
                0));

        // Assert
        assertThat(result, is(expected));
    }

    @Test
    public void translate_WhenNotFound_DropsTranslation() {
        // Arrange
        final KeyboardTranslator translator = createKeyboardTranslator();

        // Act
        final APLKeyboard result = translator.translate(new KeyEvent(KeyEvent.ACTION_DOWN, 999999));

        // Assert
        assertThat(result, is(nullValue()));
    }

    @Test
    public void translate_WithNullEvent_DropsTranslation() {
        // Arrange
        final KeyboardTranslator translator = createKeyboardTranslator();

        // Act
        final APLKeyboard result = translator.translate(null);

        // Assert
        assertThat(result, is(nullValue()));
    }

    private static KeyboardTranslator createKeyboardTranslator() {
        return KeyboardTranslator.getInstance();
    }
}
