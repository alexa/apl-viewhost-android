/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.android.component;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.InsetDrawable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;

import com.amazon.apl.android.APLGradientDrawable;
import com.amazon.apl.android.EditText;
import com.amazon.apl.android.EditTextProxy;
import com.amazon.apl.android.font.TypefaceResolver;
import com.amazon.apl.android.primitive.Rect;
import com.amazon.apl.android.views.APLEditText;
import com.amazon.apl.enums.FontStyle;
import com.amazon.apl.enums.KeyboardType;
import com.amazon.apl.enums.LayoutDirection;
import com.amazon.apl.enums.PropertyKey;
import com.amazon.apl.enums.SubmitKeyType;

import org.junit.Test;
import org.mockito.Mock;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.amazon.apl.enums.PropertyKey.kPropertyBorderStrokeWidth;
import static com.amazon.apl.enums.SubmitKeyType.kSubmitKeyTypeDone;
import static com.amazon.apl.enums.UpdateType.kUpdateSubmit;
import static com.amazon.apl.enums.UpdateType.kUpdateTextChange;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class EditTextViewAdapterTest extends AbstractComponentViewAdapterTest<EditText, APLEditText> {

    private static final String DEFAULT_FONT_FAMILY = "sans-serif";
    private static final String HINT_FONT_FAMILY = "times new roman, times, georgia, serif";
    private static final String TEXT_FONT_FAMILY = "amazon-ember, times, times new roman";
    private static final int HINT_FONT_WEIGHT = 200;
    private static final int TEXT_FONT_WEIGHT = 400;
    private static final String TEXT_FONT_LANGUAGE = "ja-JP";

    @Mock
    private APLGradientDrawable mockGradientDrawable;
    @Mock
    private EditText mEditText;
    @Mock
    EditTextProxy mEditTextProxy;
    @Mock
    private Typeface mockDefaultTypeface;
    @Mock
    private Typeface mockHintTypeface;
    @Mock
    private Typeface mockTextTypeface;
    @Mock
    private Typeface mockDefaultLangTypeface;
    @Mock
    private TypefaceResolver mockTypefaceResolver;

    @Override
    EditText component() {
        return mEditText;
    }

    EditTextProxy proxy() {
        return mEditTextProxy;
    }

    void componentSetup() {
        when(mMockPresenter.findComponent(getView())).thenReturn(mEditText);
        when(mEditText.getProxy()).thenReturn(proxy());
        when(mockTypefaceResolver.getTypeface(DEFAULT_FONT_FAMILY, 0, false, TEXT_FONT_LANGUAGE, false)).thenReturn(mockDefaultTypeface);
        when(mockTypefaceResolver.getTypeface(DEFAULT_FONT_FAMILY, 0, false, "", false)).thenReturn(mockDefaultLangTypeface);

        // TODO: add remaining props.
        when(proxy().getHint()).thenReturn("");
        when(proxy().getText()).thenReturn("");
        when(proxy().getBorderWidth()).thenReturn(0);
        when(proxy().getDrawnBorderWidth()).thenReturn(0);
        when(proxy().getSubmitKeyType()).thenReturn(kSubmitKeyTypeDone);
        when(proxy().getKeyboardType()).thenReturn(KeyboardType.kKeyboardTypeNormal);
        when(proxy().isSecureInput()).thenReturn(false);
        when(proxy().isSelectOnFocus()).thenReturn(false);
        when(component().isFocusable()).thenReturn(true);
        when(proxy().getFontWeight()).thenReturn(0);
        when(component().isFocusableInTouchMode()).thenCallRealMethod();
        when(proxy().getColor()).thenReturn(Color.WHITE);
        when(proxy().getFontFamily()).thenReturn(DEFAULT_FONT_FAMILY);
        when(proxy().getFontLanguage()).thenReturn(TEXT_FONT_LANGUAGE);
        when(proxy().getFontStyle()).thenReturn(FontStyle.kFontStyleNormal);
        when(proxy().getHintFontStyle()).thenReturn(FontStyle.kFontStyleNormal);
        when(proxy().getHintFontWeight()).thenReturn(0);
        when(proxy().getHintFontStyle()).thenReturn(FontStyle.kFontStyleNormal);
        when(proxy().getTypefaceResolver()).thenReturn(mockTypefaceResolver);
        when(component().getLayoutDirection()).thenReturn(LayoutDirection.kLayoutDirectionLTR);
    }

    void assertDefaults() {
        // TODO: add remaining props.
        verifyLayoutDirection(getView());
        verifySubmitButton(EditorInfo.IME_ACTION_DONE, getView());
        assertEquals(0, getView().getFilters().length);
        assertEquals(InputType.TYPE_CLASS_TEXT, getView().getInputType());
        assertEquals(0, getView().getFilters().length);
        assertEquals("", getView().getText().toString());
        assertEquals("", getView().getHint().toString());
        assertEquals(mockDefaultTypeface, getView().getTypeface());
    }

    @Test
    public void test_padding() {
        applyAllProperties();
        assertEquals(0, getView().getPaddingTop());
        assertEquals(0, getView().getPaddingLeft());
        assertEquals(2, getView().getPaddingRight());
        assertEquals(2, getView().getPaddingBottom());
    }

    @Test
    public void testApplyProperties_backgroundBorder() {
        APLEditText editText = spy(getView());
        doReturn(mockGradientDrawable).when(editText).getGradientDrawable();

        when(proxy().getBorderWidth()).thenReturn(4);
        when(proxy().getDrawnBorderWidth()).thenReturn(4);
        applyAllProperties(editText);
        verify(mockGradientDrawable).setStroke(4, 0);
        when(proxy().getBorderWidth()).thenReturn(6);
        when(proxy().getDrawnBorderWidth()).thenReturn(2);
        refreshProperties(editText, kPropertyBorderStrokeWidth);
        verify(mockGradientDrawable).setStroke(2, 0);
    }

    @Test
    public void testApplyProperties_backgroundPadding() {
        Rect mockBounds = mock(Rect.class);
        Rect mockInnerBounds = mock(Rect.class);

        when(mockBounds.intWidth()).thenReturn(100);
        when(mockBounds.intHeight()).thenReturn(100);

        when(mockInnerBounds.intWidth()).thenReturn(50);
        when(mockInnerBounds.intHeight()).thenReturn(50);

        when(mockInnerBounds.intLeft()).thenReturn(10);
        when(mockInnerBounds.intTop()).thenReturn(10);

        when(component().getBounds()).thenReturn(mockBounds);
        when(component().getInnerBounds()).thenReturn(mockInnerBounds);

        APLEditText editText = spy(getView());
        doReturn(mockGradientDrawable).when(editText).getGradientDrawable();

        applyAllProperties(editText);

        android.graphics.Rect actualRect = new android.graphics.Rect();
        android.graphics.Rect expectedRect = new android.graphics.Rect(10, 10, 40, 40);

        InsetDrawable insetDrawable = (InsetDrawable) editText.getBackground();

        insetDrawable.getPadding(actualRect);

        assertEquals(actualRect, expectedRect);
    }

    /**
     * Verifies that text styles override hint styles when text is not empty.
     */
    @Test
    public void testApplyProperties_textStyleProperties_textNotEmpty() {
        setupComponentForTextAndHintStyleTests();
        when(mockTypefaceResolver.getTypeface(TEXT_FONT_FAMILY, TEXT_FONT_WEIGHT, false, TEXT_FONT_LANGUAGE, false)).thenReturn(mockTextTypeface);
        when(proxy().getHint()).thenReturn("hint");
        when(proxy().getText()).thenReturn("text");
        when(proxy().getFontFamily()).thenReturn(TEXT_FONT_FAMILY);
        applyAllProperties();
        assertEquals("hint", getView().getHint());
        assertEquals(Color.GRAY, getView().getCurrentHintTextColor());
        assertEquals("text", getView().getText().toString());
        verifyTextStyleApplied();
    }

    @Test
    public void testApplyProperties_hintStyleProperties_textEmpty() {
        setupComponentForTextAndHintStyleTests();
        when(mockTypefaceResolver.getTypeface(HINT_FONT_FAMILY, HINT_FONT_WEIGHT, true, TEXT_FONT_LANGUAGE,  false)).thenReturn(mockHintTypeface);
        when(proxy().getHint()).thenReturn("hint");
        when(proxy().getText()).thenReturn("");
        when(proxy().getFontFamily()).thenReturn(HINT_FONT_FAMILY);
        applyAllProperties();
        assertEquals("hint", getView().getHint());
        assertEquals(Color.GRAY, getView().getCurrentHintTextColor());
        verifyHintStyleApplied();
    }

    @Test
    public void testApplyProperties_hintAndTextStyleTransitions() {
        setupComponentForTextAndHintStyleTests();
        when(mockTypefaceResolver.getTypeface(TEXT_FONT_FAMILY, TEXT_FONT_WEIGHT, false, TEXT_FONT_LANGUAGE,  false)).thenReturn(mockTextTypeface);
        when(proxy().getHint()).thenReturn("hint");
        when(proxy().getText()).thenReturn("");
        when(proxy().getFontFamily()).thenReturn(TEXT_FONT_FAMILY);
        applyAllProperties();
        getView().getText().append("a");
        assertEquals("hint", getView().getHint());
        assertEquals("a", getView().getText().toString());
        verifyTextStyleApplied();
        reset(mockTypefaceResolver);

        getView().getText().append("b");
        assertEquals("ab", getView().getText().toString());
        getView().getText().delete(1, 2);
        assertEquals("a", getView().getText().toString());

        when(mockTypefaceResolver.getTypeface(HINT_FONT_FAMILY, HINT_FONT_WEIGHT, true, TEXT_FONT_LANGUAGE,  false)).thenReturn(mockHintTypeface);
        when(proxy().getFontFamily()).thenReturn(HINT_FONT_FAMILY);
        getView().getText().delete(0, 1);
        assertEquals("", getView().getText().toString());
        verifyHintStyleApplied();
    }

    @Test
    public void testApplyProperties_submitKeyType() {
        List<Pair<SubmitKeyType, Integer>> testCases = new ArrayList<>();
        testCases.add(new Pair<>(SubmitKeyType.kSubmitKeyTypeDone, EditorInfo.IME_ACTION_DONE));
        testCases.add(new Pair<>(SubmitKeyType.kSubmitKeyTypeGo, EditorInfo.IME_ACTION_GO));
        testCases.add(new Pair<>(SubmitKeyType.kSubmitKeyTypeNext, EditorInfo.IME_ACTION_NEXT));
        testCases.add(new Pair<>(SubmitKeyType.kSubmitKeyTypeSearch, EditorInfo.IME_ACTION_SEARCH));
        testCases.add(new Pair<>(SubmitKeyType.kSubmitKeyTypeSend, EditorInfo.IME_ACTION_SEND));

        for(Pair<SubmitKeyType, Integer> testCase : testCases) {
            SubmitKeyType keyType = testCase.first;
            int actionId = testCase.second;
            when(proxy().getSubmitKeyType()).thenReturn(keyType);
            applyAllProperties();
            verifySubmitButton(actionId, getView());
        }
    }

    @Test
    public void testApplyProperties_maxLength_initializing_text() {
        when(proxy().getMaxLength()).thenReturn(4);
        applyAllProperties();
        List<Pair<String, String>> testCases = new ArrayList<>();
        testCases.add(Pair.create("", ""));
        testCases.add(Pair.create("abc", "abc"));
        testCases.add(Pair.create("abcd", "abcd"));
        testCases.add(Pair.create("abcde", "abcd"));
        for(Pair<String, String> testCase : testCases) {
            getView().setText(testCase.first);
            assertEquals(testCase.second, getView().getText().toString());
        }
    }

    @Test
    public void testEmojis(){
        applyAllProperties();
        // This should not crash and render properly
        getView().setText("\uD83D\uDE20"); //Angry Emoji > 0xFFFF
        assertEquals("\uD83D\uDE20", getView().getText().toString()); //Persevere Emoji > 0xFFFF
        getView().getText().append("\uD83D\uDE23");
        assertEquals("\uD83D\uDE20\uD83D\uDE23", getView().getText().toString());
        getView().getText().append("\uD83D\uDE24"); //Face with look of triumph > 0xFFFF
        assertEquals("\uD83D\uDE20\uD83D\uDE23\uD83D\uDE24", getView().getText().toString());
    }

    @Test
    public void testApplyProperties_maxLength_user_event() {
        when(proxy().getMaxLength()).thenReturn(4);
        applyAllProperties();
        // Simulate user events.
        getView().setText("abc");
        assertEquals("abc", getView().getText().toString());
        getView().getText().append("d");
        assertEquals("abcd", getView().getText().toString());
        getView().getText().append("e");
        assertEquals("abcd", getView().getText().toString());
        getView().getText().insert(1, "e");
        assertEquals("abcd", getView().getText().toString());
    }

    @Test
    public void testApplyProperties_maxLength_zero() {
        when(proxy().getMaxLength()).thenReturn(0);
        applyAllProperties();
        assertEquals(0, getView().getFilters().length);
        getView().setText("abcd");
        assertEquals("abcd", getView().getText().toString());
    }

    @Test
    public void testApplyProperties_validCharacters() {
        when(proxy().getValidCharacters()).thenReturn("0-9a-f");
        applyAllProperties();
        InputFilter[] actualFiltersArray = getView().getFilters();
        assertEquals(1, actualFiltersArray.length);
        assertThat(actualFiltersArray[0], instanceOf(EditTextViewAdapter.ValidCharactersFilter.class));
    }

    @Test
    public void testApplyProperties_validCharacters_empty() {
        when(proxy().getValidCharacters()).thenReturn("");
        applyAllProperties();
        assertEquals(0, getView().getFilters().length);
        getView().setText("abcd");
        assertEquals("abcd", getView().getText().toString());
    }

    @Test
    public void testValidCharactersFilter_initializing_text() {
        when(proxy().getValidCharacters()).thenReturn("ac");
        when(component().isValidCharacter('a')).thenReturn(true);
        when(component().isValidCharacter('b')).thenReturn(false);
        when(component().isValidCharacter('c')).thenReturn(true);
        when(component().isValidCharacter('d')).thenReturn(false);

        List<Pair<String, String>> testCases = new ArrayList<>();
        testCases.add(Pair.create("ab", "a"));
        testCases.add(Pair.create("abc", "ac"));
        testCases.add(Pair.create("bd", ""));
        testCases.add(Pair.create("ac", "ac"));
        testCases.add(Pair.create("ba", "a"));
        testCases.add(Pair.create("dab", "a"));

        applyAllProperties();
        for (Pair<String, String> testCase : testCases) {
            getView().setText(testCase.first);
            assertEquals(testCase.second, getView().getText().toString());
        }
    }

    @Override
    @Test
    public void test_refresh_accessibility() {
        String accessibilityString = "accessibility";
        when(component().getAccessibilityLabel()).thenReturn(accessibilityString);

        refreshProperties(PropertyKey.kPropertyAccessibilityLabel);

        verify(component(), atLeastOnce()).getAccessibilityLabel();
        verify(component(), atLeastOnce()).isFocusable();
        verify(component(), atLeastOnce()).isDisabled();
        verify(component()).isFocusableInTouchMode();
        verifyNoMoreInteractions(component());

        assertEquals(accessibilityString, getView().getContentDescription());
        assertTrue(getView().isFocusable());
        assertTrue(getView().isFocusableInTouchMode());
    }

    @Test
    public void test_refresh_accessibility_disabled() {
        String accessibilityString = "accessibility";
        when(component().getAccessibilityLabel()).thenReturn(accessibilityString);
        when(component().isDisabled()).thenReturn(true);

        refreshProperties(PropertyKey.kPropertyAccessibilityLabel);

        verify(component(), atLeastOnce()).getAccessibilityLabel();
        verify(component(), atLeastOnce()).isFocusable();
        verify(component(), atLeastOnce()).isDisabled();
        verify(component()).isFocusableInTouchMode();
        verifyNoMoreInteractions(component());

        assertEquals(accessibilityString, getView().getContentDescription());
        assertFalse(getView().isFocusable());
        assertFalse(getView().isFocusableInTouchMode());
    }

    @Test
    public void testValidCharactersFilter_user_event() {
        when(proxy().getValidCharacters()).thenReturn("ac");
        when(component().isValidCharacter('a')).thenReturn(true);
        when(component().isValidCharacter('b')).thenReturn(false);
        when(component().isValidCharacter('c')).thenReturn(true);
        when(component().isValidCharacter('d')).thenReturn(false);

        applyAllProperties();
        // Simulate user events.
        getView().setText("a");
        assertEquals("a", getView().getText().toString());
        getView().getText().append("b");
        assertEquals("a", getView().getText().toString());
        getView().getText().append("c");
        assertEquals("ac", getView().getText().toString());
        getView().getText().insert(1, "b");
        assertEquals("ac", getView().getText().toString());
        getView().getText().delete(1, 2);
        assertEquals("a", getView().getText().toString());
    }

    @Test
    public void testApplyProperties_multiple_inputFilters() {
        when(proxy().getMaxLength()).thenReturn(4);
        when(proxy().getValidCharacters()).thenReturn("0-9a-f");

        applyAllProperties();

        InputFilter[] actualFiltersArray = getView().getFilters();
        assertEquals(2, actualFiltersArray.length);
        assertThat(actualFiltersArray[0], instanceOf(InputFilter.LengthFilter.class));
        assertThat(actualFiltersArray[1], instanceOf(EditTextViewAdapter.ValidCharactersFilter.class));
    }

    @Test
    public void testApplyProperties_keyboardTypes() {

        List<Pair<KeyboardType, Integer>> testCases = Arrays.asList(
                new Pair(KeyboardType.kKeyboardTypeDecimalPad, InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED),
                new Pair(KeyboardType.kKeyboardTypeEmailAddress, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS),
                new Pair(KeyboardType.kKeyboardTypeNumberPad, InputType.TYPE_CLASS_NUMBER),
                new Pair(KeyboardType.kKeyboardTypePhonePad, InputType.TYPE_CLASS_PHONE),
                new Pair(KeyboardType.kKeyboardTypeUrl, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI),
                new Pair(KeyboardType.kKeyboardTypeNormal, InputType.TYPE_CLASS_TEXT)
        );

        for (Pair<KeyboardType, Integer> t : testCases) {
            KeyboardType type = t.first;
            int expected = t.second;

            when(proxy().getKeyboardType()).thenReturn(type);
            applyAllProperties();
            assertEquals(expected, getView().getInputType());
        }
    }

    @Test
    public void testApplyProperties_secureInput() {
        when(proxy().isSecureInput()).thenReturn(true);

        List<Pair<KeyboardType, Integer>> testCases = Arrays.asList(
                new Pair(KeyboardType.kKeyboardTypeDecimalPad, InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED | InputType.TYPE_NUMBER_VARIATION_PASSWORD),
                new Pair(KeyboardType.kKeyboardTypeEmailAddress, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS | InputType.TYPE_TEXT_VARIATION_PASSWORD),
                new Pair(KeyboardType.kKeyboardTypeNumberPad, InputType.TYPE_CLASS_NUMBER |  InputType.TYPE_NUMBER_VARIATION_PASSWORD),
                new Pair(KeyboardType.kKeyboardTypePhonePad, InputType.TYPE_CLASS_PHONE), // not supported
                new Pair(KeyboardType.kKeyboardTypeUrl, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI | InputType.TYPE_TEXT_VARIATION_PASSWORD),
                new Pair(KeyboardType.kKeyboardTypeNormal, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)
        );

        for (Pair<KeyboardType, Integer> t : testCases) {
            KeyboardType type = t.first;
            int expected = t.second;

            when(proxy().getKeyboardType()).thenReturn(type);
            applyAllProperties();
            assertEquals(expected, getView().getInputType());
        }
        assertTrue(getView().getTransformationMethod() instanceof PasswordTransformationMethod);
    }

    @Test
    public void testApplyProperties_selectOnFocus_disabled() {
        when(proxy().getText()).thenReturn("abcd");
        when(proxy().isSelectOnFocus()).thenReturn(false);

        applyAllProperties();
        getView().requestFocus();

        assertTrue(getView().hasFocus());
        assertEquals(4, getView().getSelectionStart());
        assertEquals(4, getView().getSelectionEnd()); // zero characters selected
    }

    @Test
    public void testTypeface_withDefaultLanguage() {
        when(proxy().getText()).thenReturn("abcd");
        when(proxy().getFontLanguage()).thenReturn(null);
        when(mockTypefaceResolver.getTypeface(DEFAULT_FONT_FAMILY, 0, false, null, false)).thenReturn(mockDefaultTypeface);
        applyAllProperties();
        assertEquals(mockDefaultTypeface, getView().getTypeface());
    }

    @Test
    public void testApplyProperties_selectOnFocus() {
        when(proxy().getText()).thenReturn("abcd");
        when(proxy().isSelectOnFocus()).thenReturn(true);

        applyAllProperties();
        getView().requestFocus();

        assertTrue(getView().hasFocus());
        assertEquals(0, getView().getSelectionStart());
        assertEquals(4, getView().getSelectionEnd());
    }

    @Test
    public void testUpdate_UpdateTextChangeMessage_VerifyInvoke() {
        applyAllProperties();

        getView().getText().append("a");
        verify(mMockPresenter).updateComponent(getView(), kUpdateTextChange, "a");

        getView().getText().append("b");
        verify(mMockPresenter).updateComponent(getView(), kUpdateTextChange, "ab");
    }

    @Test
    public void testUpdate_UpdateTextChangeMessage_VerifyInvoke_secureInput() {
        when(proxy().isSecureInput()).thenReturn(true);
        applyAllProperties();

        getView().getText().append("a");
        verify(mMockPresenter).updateComponent(getView(), kUpdateTextChange, "a");

        getView().getText().append("b");
        verify(mMockPresenter).updateComponent(getView(), kUpdateTextChange, "ab");
    }

    @Test
    public void testUpdate_UpdateSubmitMessage_VerifyInvokes() {
        EditTextViewAdapter.SubmitListener submitListener = new EditTextViewAdapter.SubmitListener(getView());
        applyAllProperties();
        KeyEvent keyEvent = new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER);
        List<Integer> supportedImeActionIdList = new ArrayList<>();
        supportedImeActionIdList.add(EditorInfo.IME_ACTION_DONE);
        supportedImeActionIdList.add(EditorInfo.IME_ACTION_GO);
        supportedImeActionIdList.add(EditorInfo.IME_ACTION_NEXT);
        supportedImeActionIdList.add(EditorInfo.IME_ACTION_SEARCH);
        supportedImeActionIdList.add(EditorInfo.IME_ACTION_SEND);
        for(int imeActionId : supportedImeActionIdList) {
            submitListener.onEditorAction(getView(), imeActionId, keyEvent);
            verify(mMockPresenter).updateComponent(getView(), kUpdateSubmit, 0);
            reset(mMockPresenter);
        }
    }

    @Test
    public void testUpdate_UpdateSubmitMessage_VerifyInvokeOnUnsupportedSubmitKeyType() {
        EditTextViewAdapter.SubmitListener submitListener = new EditTextViewAdapter.SubmitListener(getView());
        applyAllProperties();
        int unsupportedImeActionId = EditorInfo.IME_ACTION_PREVIOUS;
        KeyEvent keyEvent = new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER);
        submitListener.onEditorAction(getView(), unsupportedImeActionId, keyEvent);
        verify(mMockPresenter, never()).updateComponent(any(getView().getClass()), eq(kUpdateSubmit), anyInt());
    }

    @Test
    public void test_refresh_text() {
        final String newText = "newText";
        when(proxy().getText()).thenReturn(newText);
        refreshProperties(PropertyKey.kPropertyText);
        verify(proxy()).getText();
        assertEquals(newText, getView().getText().toString());
        verify(mMockPresenter).updateComponent(getView(), kUpdateTextChange, newText);
    }

    @Test
    public void test_refresh_border() {
        APLEditText editText = spy(getView());
        doReturn(mockGradientDrawable).when(editText).getGradientDrawable();

        final int newColor = Color.RED;
        when(proxy().getBorderColor()).thenReturn(newColor);
        refreshProperties(editText, PropertyKey.kPropertyBorderColor);
        verify(proxy()).getBorderColor();
        verify(proxy()).getDrawnBorderWidth();
        verify(mockGradientDrawable).setStroke(0, newColor);
        verifyNoMoreInteractions(proxy());
    }

    @Test
    public void testRefreshProperties_fontLanguage() {
        when(proxy().getFontLanguage()).thenReturn("");
        refreshProperties(PropertyKey.kPropertyLang);

        //Test that only methods related to font language are called when kPropertyLang is dirty
        verify(proxy()).getTypefaceResolver();
        verify(proxy()).getFontLanguage();
        verify(proxy()).getFontWeight();
        verify(proxy()).getFontStyle();
        verify(proxy()).getFontFamily();
        assertEquals(mockDefaultLangTypeface, getView().getTypeface());
        verifyNoMoreInteractions(proxy());
    }

    @Test
    public void testRefreshLayoutDirection() {
        when(component().getLayoutDirection()).thenReturn(LayoutDirection.kLayoutDirectionRTL);
        refreshProperties(PropertyKey.kPropertyLayoutDirection);
        verify(component()).getLayoutDirection();
        verifyNoMoreInteractions(component());
    }

    @Test
    public void test_layoutDirection_rtl() {
        // See https://github.com/robolectric/robolectric/issues/3910
        // for need of spy
        when(component().getLayoutDirection()).thenReturn(LayoutDirection.kLayoutDirectionRTL);

        APLEditText spyView = spy(getView());

        applyAllProperties(spyView);

        verify(spyView).setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
    }

    @Test
    public void test_refresh_layoutDirection() {
        when(component().getLayoutDirection()).thenReturn(LayoutDirection.kLayoutDirectionRTL);

        APLEditText spyView = spy(getView());
        refreshProperties(spyView, PropertyKey.kPropertyLayoutDirection);

        verify(component()).getLayoutDirection();
        verifyNoMoreInteractions(component());

        verify(spyView).setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
    }

    private void setupComponentForTextAndHintStyleTests() {
        when(proxy().getHintColor()).thenReturn(Color.GRAY);
        when(proxy().getHintFontStyle()).thenReturn(FontStyle.kFontStyleItalic);
        when(proxy().getHintFontWeight()).thenReturn(HINT_FONT_WEIGHT);

        when(proxy().getColor()).thenReturn(Color.WHITE);
        when(proxy().getFontStyle()).thenReturn(FontStyle.kFontStyleNormal);
        when(proxy().getFontWeight()).thenReturn(TEXT_FONT_WEIGHT);
        when(proxy().getFontLanguage()).thenReturn(TEXT_FONT_LANGUAGE);
        verifyNoMoreInteractions(proxy());
    }

    private void verifyTextStyleApplied() {
        assertEquals(Color.WHITE, getView().getCurrentTextColor());
        assertEquals(mockTextTypeface, getView().getTypeface());
    }

    private void verifyHintStyleApplied() {
        assertEquals(Color.GRAY, getView().getCurrentHintTextColor());
        assertEquals(mockHintTypeface, getView().getTypeface());
    }

    private void verifySubmitButton(final int imeActionId, final APLEditText editTextView) {
        assertEquals(1, editTextView.getMaxLines());
        assertEquals(imeActionId, editTextView.getImeOptions());
    }

    private void verifyLayoutDirection(final APLEditText editTextView) {
        verify(component()).getLayoutDirection();
        assertEquals(View.TEXT_DIRECTION_FIRST_STRONG, editTextView.getTextDirection());
    }
}
