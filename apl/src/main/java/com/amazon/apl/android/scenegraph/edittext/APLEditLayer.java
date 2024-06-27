/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.scenegraph.edittext;

import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import com.amazon.apl.android.RenderingContext;
import com.amazon.apl.android.font.TypefaceResolver;
import com.amazon.apl.android.scaling.IMetricsTransform;
import com.amazon.apl.android.scenegraph.APLLayer;
import com.amazon.apl.android.scenegraph.text.APLTextProperties;
import com.amazon.apl.android.sgcontent.EditTextNode;
import com.amazon.apl.enums.FontStyle;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class APLEditLayer extends APLLayer {
    public APLEditLayer(RenderingContext renderingContext) {
        super(renderingContext);
    }

    @Override
    public void attachView(ViewGroup view) {
        super.attachView(view);
        EditText editText = new EditText(view.getContext());
        editText.setBackground(null);
        editText.setPadding(0, 0, 0, 0);
        EditTextNode editTextNode = (EditTextNode) getContent()[0];
        applyHighlightColor(editTextNode, editText);
        applyTextColor(editTextNode, editText);
        applyTextSize(editTextNode, editText);
        applyKeyboardType(editTextNode, editText);
        applySubmitButtonType(editTextNode, editText);
        applySelectOnFocus(editTextNode, editText);
        applyTextTypeface(editTextNode, editText);
        applyText(editTextNode, editText);
        editText.setOnEditorActionListener(new SubmitListener(editTextNode.getEditText()));
        editText.addTextChangedListener(new TextChangeListener(editTextNode));
        editText.setOnFocusChangeListener((v, hasFocus) -> editTextNode.getEditText().focusChanged(hasFocus));
        view.addView(editText);
    }

    private void applyText(EditTextNode editTextNode, EditText editText) {
        editText.setText(editTextNode.getText());
    }

    private void applyHighlightColor(EditTextNode editTextNode, EditText editText) {
        editText.setHighlightColor(editTextNode.getEditTextConfig().getHighlightColor());
    }

    private void applyTextColor(EditTextNode editTextNode, EditText editText) {
        editText.setTextColor(editTextNode.getEditTextConfig().getTextColor());
    }

    private void applyTextSize(EditTextNode editTextNode, EditText editText) {
        IMetricsTransform transform = getRenderingContext().getMetricsTransform();
        // The text size is expected to be in scaled pixel (sp) units and the current implementation is off.
        // https://developer.android.com/guide/topics/resources/more-resources.html#Dimension
        // TODO: Dimension px coversions need to be put in a nicer way
        final float finalTextSize = editTextNode.getEditTextConfig().getTextProperties(transform).getFontSize();
        editText.setTextSize(finalTextSize);
    }

    private void applyKeyboardType(EditTextNode editTextNode, EditText view) {
        int inputType;
        EditTextConfig editTextConfig = editTextNode.getEditTextConfig();
        // configure keyboard input
        switch (editTextConfig.getKeyboardType()) {
            case kKeyboardTypeDecimalPad:
                inputType = InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED;
                break;
            case kKeyboardTypeEmailAddress:
                inputType = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS;
                break;
            case kKeyboardTypeNumberPad:
                inputType = InputType.TYPE_CLASS_NUMBER;
                break;
            case kKeyboardTypePhonePad:
                inputType = InputType.TYPE_CLASS_PHONE;
                break;
            case kKeyboardTypeUrl:
                inputType = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI;
                break;
            case kKeyboardTypeNormal:
            default:
                inputType = InputType.TYPE_CLASS_TEXT;
        }

        // password mask based on keyboard input
        if (editTextConfig.isSecureInput()) {
            view.setTransformationMethod(PasswordTransformationMethod.getInstance());
            switch (inputType & InputType.TYPE_MASK_CLASS) {
                case InputType.TYPE_CLASS_NUMBER:
                    inputType |= InputType.TYPE_NUMBER_VARIATION_PASSWORD;
                    break;
                case InputType.TYPE_CLASS_TEXT:
                    inputType |= InputType.TYPE_TEXT_VARIATION_PASSWORD;
                    break;
                case InputType.TYPE_CLASS_PHONE:
                    // this class currently supports no variations or flags for password
                    break;
            }
        }

        view.setInputType(inputType);
    }

    private void applySubmitButtonType(EditTextNode editTextNode, EditText view) {
        view.setSingleLine(true);
        EditTextConfig editTextConfig = editTextNode.getEditTextConfig();
        switch(editTextConfig.getSubmitKeyType()) {
            case kSubmitKeyTypeGo:
                view.setImeOptions(EditorInfo.IME_ACTION_GO);
                break;
            case kSubmitKeyTypeNext:
                view.setImeOptions(EditorInfo.IME_ACTION_NEXT);
                break;
            case kSubmitKeyTypeSearch:
                view.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
                break;
            case kSubmitKeyTypeSend:
                view.setImeOptions(EditorInfo.IME_ACTION_SEND);
                break;
            case kSubmitKeyTypeDone:
            default:
                view.setImeOptions(EditorInfo.IME_ACTION_DONE);
        }
    }

    private void applySelectOnFocus(EditTextNode editTextNode, EditText view) {
        if (editTextNode.getEditTextConfig().isSelectOnFocus()) {
            view.setSelectAllOnFocus(true);
        }
    }

    private void applyTextTypeface(EditTextNode editTextNode, EditText view) {
        APLTextProperties proxy = editTextNode.getEditTextConfig().getTextProperties(getRenderingContext().getMetricsTransform());
        final FontStyle fontStyle = proxy.getFontStyle();
        final int fontWeight = proxy.getFontWeight();
        final boolean italic = fontStyle == FontStyle.kFontStyleItalic;
        final String fontLanguage = proxy.getFontLanguage();
        TypefaceResolver typefaceResolver = TypefaceResolver.getInstance();
        view.setTypeface(typefaceResolver.getTypeface(proxy.getFontFamily(),
                fontWeight,
                italic,
                fontLanguage,
                false));
    }

    private static class SubmitListener implements TextView.OnEditorActionListener {
        private final com.amazon.apl.android.scenegraph.edittext.EditText mNativeEditText;

        SubmitListener(final com.amazon.apl.android.scenegraph.edittext.EditText nativeEditText) {
            mNativeEditText = nativeEditText;
        }

        private static final Set<Integer> supportedEditorActions = new HashSet<>(Arrays.asList(
                EditorInfo.IME_ACTION_GO,
                EditorInfo.IME_ACTION_NEXT,
                EditorInfo.IME_ACTION_SEARCH,
                EditorInfo.IME_ACTION_SEND,
                EditorInfo.IME_ACTION_DONE));

        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if(supportedEditorActions.contains(actionId)) {
                mNativeEditText.submit();
            }
            return false;
        }
    }

    private static class TextChangeListener implements TextWatcher {

        private final EditTextNode mEditTextNode;
        private String mPrevious = "";

        TextChangeListener(EditTextNode editTextNode) {
            mEditTextNode = editTextNode;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            // Check required to guard against infinitely recursive loop since any change to
            // Editable s results in a call to this same method.
            if (!mPrevious.equals(s.toString())) {
                String newString = mEditTextNode.getEditTextConfig().strip(s.toString());
                if (!mPrevious.equals(newString)) {
                    mEditTextNode.getEditText().textChanged(newString);
                }
                mPrevious = newString;
                // The last thing should be to update the Editable s if needed.
                s.replace(0, s.length(), newString);
            }
        }
    }
}