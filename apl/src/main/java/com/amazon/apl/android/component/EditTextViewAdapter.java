/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.component;

import android.content.Context;
import android.graphics.drawable.InsetDrawable;
import androidx.annotation.VisibleForTesting;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

import com.amazon.apl.android.Component;
import com.amazon.apl.android.EditText;
import com.amazon.apl.android.EditTextProxy;
import com.amazon.apl.android.IAPLViewPresenter;
import com.amazon.apl.android.font.TypefaceResolver;
import com.amazon.apl.android.primitive.Rect;
import com.amazon.apl.android.views.APLEditText;
import com.amazon.apl.enums.FontStyle;
import com.amazon.apl.enums.LayoutDirection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.amazon.apl.enums.PropertyKey.kPropertyBorderColor;
import static com.amazon.apl.enums.PropertyKey.kPropertyBorderStrokeWidth;
import static com.amazon.apl.enums.PropertyKey.kPropertyLang;
import static com.amazon.apl.enums.PropertyKey.kPropertyLayoutDirection;
import static com.amazon.apl.enums.PropertyKey.kPropertyText;
import static com.amazon.apl.enums.UpdateType.kUpdateSubmit;
import static com.amazon.apl.enums.UpdateType.kUpdateTextChange;

/**
 * ComponentViewAdapter responsible for applying {@link EditText} properties to an {@link android.widget.EditText}.
 */
public class EditTextViewAdapter extends ComponentViewAdapter<EditText, APLEditText> {

    private static EditTextViewAdapter INSTANCE;

    private EditTextViewAdapter() {
        super();
        putPropertyFunction(kPropertyBorderColor, this::applyBackgroundBorder);
        putPropertyFunction(kPropertyBorderStrokeWidth, this::applyBackgroundBorder);
        putPropertyFunction(kPropertyText, this::applyText);
        putPropertyFunction(kPropertyLang, this::applyTextTypeface);
        putPropertyFunction(kPropertyLayoutDirection, this::applyLayoutDirection);
    }

    /**
     * @return the EditTextViewAdapter instance.
     */
    public static EditTextViewAdapter getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new EditTextViewAdapter();
        }
        return INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public APLEditText createView(Context context, IAPLViewPresenter presenter) {
        APLEditText view = new APLEditText(context, presenter);
        view.addTextChangedListener(new TextChangeListener(view));
        view.addTextChangedListener(new TypefaceSetter(view, this));
        view.setOnEditorActionListener(new SubmitListener(view));
        return view;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void applyAllProperties(EditText component, APLEditText view) {
        super.applyAllProperties(component, view);
        applyLayoutDirection(component, view);
        applyHint(component, view);
        applySubmitButtonType(component, view);
        applyKeyboardType(component, view);
        applyBackgroundBorder(component, view);
        // Filter needs to be applied first since validCharacters restriction applies to all text as per spec.
        // https://developer.amazon.com/en-US/docs/alexa/alexa-presentation-language/apl-edittext.html#validcharacters
        applyCharacterFilter(component, view);
        applyText(component, view);
        applySelectOnFocus(component, view);
        applyTextStyle(component, view);
    }

    private void applySubmitButtonType(EditText component, APLEditText view) {
        view.setSingleLine(true);
        switch(component.getProxy().getSubmitKeyType()) {
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

    private void applyBackgroundBorder(EditText component, APLEditText view) {
        EditTextProxy proxy = component.getProxy();
        view.getGradientDrawable().setStroke(proxy.getDrawnBorderWidth(), proxy.getBorderColor());
    }

    private void applyBackgroundPadding(EditText component, APLEditText view) {
        Rect innerBounds = component.getInnerBounds();
        Rect bounds = component.getBounds();
        int right = bounds.intWidth() - innerBounds.intWidth() - innerBounds.intLeft();
        int bottom = bounds.intHeight() - innerBounds.intHeight() - innerBounds.intTop();

        view.setBackground(new InsetDrawable(view.getGradientDrawable(), innerBounds.intLeft(), innerBounds.intTop(), right, bottom));
    }

    private void applyCharacterFilter(EditText component, APLEditText view) {
        EditTextProxy proxy = component.getProxy();
        List<InputFilter> filtersList = new ArrayList<>();
        final int maxLength = proxy.getMaxLength();
        if(maxLength > 0) {
            filtersList.add(new InputFilter.LengthFilter(maxLength));
        }
        final String validCharacters = proxy.getValidCharacters();
        if(!TextUtils.isEmpty(validCharacters)) {
            filtersList.add(new ValidCharactersFilter(component));
        }
        InputFilter[] filtersArray = filtersList.toArray(new InputFilter[0]);
        view.setFilters(filtersList.toArray(filtersArray));
    }

    private void applyKeyboardType(EditText component, APLEditText view) {
        int inputType;
        EditTextProxy proxy = component.getProxy();

        // configure keyboard input
        switch (proxy.getKeyboardType()) {
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
        if (proxy.isSecureInput()) {
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

    private void applyHint(EditText component, APLEditText view) {
        EditTextProxy proxy = component.getProxy();
        view.setHint(proxy.getHint());
        view.setHintTextColor(proxy.getHintColor());
    }

    private void applyText(EditText component, APLEditText view) {
        view.setText(component.getProxy().getText());
    }

    private void applySelectOnFocus(EditText component, APLEditText view) {
        if (component.getProxy().isSelectOnFocus()) {
            view.setSelectAllOnFocus(true);
        }
    }

    private void applyTextStyle(EditText component, APLEditText view) {
        EditTextProxy proxy = component.getProxy();
        view.setTextSize(proxy.getFontSize());
        view.setHighlightColor(proxy.getHighlightColor());;
        view.setTextColor(proxy.getColor());
        if(proxy.getText().length() == 0) {
            applyHintTypeface(component, view);
        } else {
            applyTextTypeface(component, view);
        }
    }

    private void applyTextTypeface(EditText component, APLEditText view) {
        EditTextProxy proxy = component.getProxy();
        final FontStyle fontStyle = proxy.getFontStyle();
        final int fontWeight = proxy.getFontWeight();
        final boolean italic = fontStyle == FontStyle.kFontStyleItalic;
        final String fontLanguage = proxy.getFontLanguage();
        TypefaceResolver typefaceResolver = proxy.getTypefaceResolver();
        view.setTypeface(typefaceResolver.getTypeface(proxy.getFontFamily(),
                fontWeight,
                italic,
                fontLanguage,
                false));
    }

    private void applyHintTypeface(EditText component, APLEditText view) {
        EditTextProxy proxy = component.getProxy();
        final FontStyle fontStyle = proxy.getHintFontStyle();
        final int fontWeight = proxy.getHintFontWeight();
        final boolean italic = fontStyle == FontStyle.kFontStyleItalic;
        TypefaceResolver typefaceResolver = proxy.getTypefaceResolver();
        view.setTypeface(typefaceResolver.getTypeface(proxy.getFontFamily(),
                fontWeight,
                italic,
                proxy.getFontLanguage(),
                false));
    }

    private void applyLayoutDirection(EditText component, APLEditText view) {
        final LayoutDirection layoutDirection =  component.getLayoutDirection();
        final int textLayoutDirection = LayoutDirection.kLayoutDirectionRTL == layoutDirection ? View.TEXT_DIRECTION_RTL : View.TEXT_DIRECTION_LTR;
        final int viewLayoutDirection = LayoutDirection.kLayoutDirectionRTL == layoutDirection ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LTR;
        view.setTextDirection(textLayoutDirection);
        view.setLayoutDirection(viewLayoutDirection);
    }

    /**
     * {@inheritDoc}
     */
    void applyPadding(EditText component, APLEditText view) {
        setPaddingFromBounds(component, view, false);
        applyBackgroundPadding(component, view);
    }

    @VisibleForTesting
    public static class ValidCharactersFilter implements InputFilter {

        private final EditText mEditText;

        public ValidCharactersFilter(EditText editText) {
            mEditText = editText;
        }

        @Override
        public CharSequence filter(CharSequence source, int start, int end,
                                   Spanned dest, int dstart, int dend) {

            if (source instanceof SpannableStringBuilder) {
                SpannableStringBuilder sourceAsSpannableBuilder = (SpannableStringBuilder)source;
                for (int i = end - 1; i >= start; i--) {
                    if (!mEditText.isValidCharacter(source.charAt(i))) {
                        sourceAsSpannableBuilder.delete(i, i+1);
                    }
                }
                return source;
            } else {
                StringBuilder filteredStringBuilder = new StringBuilder();
                for (int i = start; i < end; i++) {
                    final char currentChar = source.charAt(i);
                    if (mEditText.isValidCharacter(currentChar)) {
                        filteredStringBuilder.append(currentChar);
                    }
                }
                return filteredStringBuilder.toString();
            }
        }
    }

    private static class TextChangeListener implements TextWatcher {

        private final APLEditText mEditTextView;
        private final IAPLViewPresenter mPresenter;
        private String mPrevious = "";

        public TextChangeListener(final APLEditText editTextView) {
            mEditTextView = editTextView;
            mPresenter = editTextView.getPresenter();
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            final Component component = mPresenter.findComponent(mEditTextView);
            final String newString = s.toString();
            if(component != null && !mPrevious.equals(newString)) {
                mPresenter.updateComponent(mEditTextView, kUpdateTextChange, newString);
            }
            mPrevious = newString;
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    }

    /**
     * Utility class to set hint and text typeface separately,
     * since Android normally does not support that.
     */
    private static class TypefaceSetter implements TextWatcher {

        private final APLEditText mEditTextView;
        private final IAPLViewPresenter mPresenter;
        private int mTextLength;
        private final EditTextViewAdapter mAdapter;

        public TypefaceSetter(final APLEditText editTextView, final EditTextViewAdapter adapter) {
            mEditTextView = editTextView;
            mPresenter = editTextView.getPresenter();
            mAdapter = adapter;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // Change typeface only when EditText changes from empty to non-empty and vice-versa.
            final EditText editTextComponent = (EditText) mPresenter.findComponent(mEditTextView);
            if(editTextComponent != null) {
                if(s.length() == 0) {
                    mAdapter.applyHintTypeface(editTextComponent, mEditTextView);
                } else if(mTextLength == 0 && s.length() > 0) {
                    mAdapter.applyTextTypeface(editTextComponent, mEditTextView);
                }
            }
            mTextLength = s.length();
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    }

    @VisibleForTesting
    public static class SubmitListener implements TextView.OnEditorActionListener {

        private final APLEditText mEditText;
        private final IAPLViewPresenter mPresenter;

        public SubmitListener(final APLEditText editText) {
            mEditText = editText;
            mPresenter = editText.getPresenter();
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
                mPresenter.updateComponent(mEditText, kUpdateSubmit, 0);
            }
            return false;
        }
    }
}