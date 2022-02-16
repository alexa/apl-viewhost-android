/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.component;

import com.amazon.apl.android.EditText;
import com.amazon.apl.android.views.APLEditText;

import org.junit.Before;
import org.junit.Test;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertEquals;

public class EditTextViewTest extends AbstractComponentViewTest<APLEditText, EditText> {

    @Before
    public void doBefore() {
        REQUIRED_PROPERTIES = ""; // no required properties in EditText Component.
        OPTIONAL_PROPERTIES =
                " \"borderColor\": \"black\"," +
                        " \"borderStrokeWidth\": 20," +
                        " \"borderWidth\": 30," +
                        " \"color\": \"red\"," +
                        " \"fontFamily\": \"times new roman, times, georgia, serif\"," +
                        " \"fontSize\": 18," +
                        " \"fontStyle\": \"italic\"," +
                        " \"fontWeight\": 500," +
                        " \"highlightColor\": \"yellow\"," +
                        " \"hint\": \"This is a hint\"," +
                        " \"hintColor\":\"blue\" ," +
                        " \"hintStyle\": \"italic\"," +
                        " \"hintWeight\": 300," +
                        " \"keyboardType\": \"emailAddress\"," +
                        " \"secureInput\": true," +
                        " \"selectOnFocus\": true," +
                        " \"size\": 10," +
                        " \"submitKeyType\": \"send\"";
    }

    @Override
    String getComponentType() {
        return "EditText";
    }

    @Override
    Class<APLEditText> getViewClass() {
        return APLEditText.class;
    }

    @Override
    void testView_applyProperties(APLEditText view) {
        assertEquals("This is a hint", getTestView().getHint());
    }

    @Test
    public void testProperty_restrictCharacters_maxLength() {
        OPTIONAL_PROPERTIES = "\"maxLength\": 4, \"text\": \"!@12#$a%^b&*AB()\"," + OPTIONAL_PROPERTIES;
        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(inflate(REQUIRED_PROPERTIES, OPTIONAL_PROPERTIES))
                .check(hasRootContext());
        assertEquals("!@12", getTestView().getText().toString());
    }

    @Test
    public void testProperty_validCharacters_hexadecimal() {
        OPTIONAL_PROPERTIES = "\"validCharacters\": \"0-9a-fA-F\", \"text\": \"!@12#$a%^b&*AB()\"," + OPTIONAL_PROPERTIES;
        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(inflate(REQUIRED_PROPERTIES, OPTIONAL_PROPERTIES))
                .check(hasRootContext());
        assertEquals("12abAB", getTestView().getText().toString());
    }

    @Test
    public void testProperty_validCharacters_pin_pad_keyboard() {
        OPTIONAL_PROPERTIES = "\"validCharacters\": \"0-9\", \"text\": \"!@12#$a%^b&*AB()\"," + OPTIONAL_PROPERTIES;
        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(inflate(REQUIRED_PROPERTIES, OPTIONAL_PROPERTIES))
                .check(hasRootContext());
        assertEquals("12", getTestView().getText().toString());
    }

    @Test
    public void testProperty_validCharacters_cash_atm() {
        OPTIONAL_PROPERTIES = "\"validCharacters\": \"0-9.\", \"text\": \"$12.99\"," + OPTIONAL_PROPERTIES;
        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(inflate(REQUIRED_PROPERTIES, OPTIONAL_PROPERTIES))
                .check(hasRootContext());
        assertEquals("12.99", getTestView().getText().toString());
    }

    @Test
    public void testProperty_validCharacters_email_address() {
        OPTIONAL_PROPERTIES = "\"validCharacters\": \"-+a-zA-Z0-9_@.\", \"text\": \"*&apl@example.com%$\"," + OPTIONAL_PROPERTIES;
        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(inflate(REQUIRED_PROPERTIES, OPTIONAL_PROPERTIES))
                .check(hasRootContext());
        assertEquals("apl@example.com", getTestView().getText().toString());
    }

    @Test
    public void testProperty_validCharacters_single_character() {
        OPTIONAL_PROPERTIES = "\"validCharacters\": \"a\", \"text\": \"*&apl@example.com%$\"," + OPTIONAL_PROPERTIES;
        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(inflate(REQUIRED_PROPERTIES, OPTIONAL_PROPERTIES))
                .check(hasRootContext());
        assertEquals("aa", getTestView().getText().toString());
    }
}
