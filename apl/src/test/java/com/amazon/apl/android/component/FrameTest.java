/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.android.component;


import android.graphics.Color;

import com.amazon.apl.android.views.APLAbsoluteLayout;
import com.amazon.apl.android.Frame;
import com.amazon.apl.android.primitive.Radii;

import org.junit.Before;

import static org.junit.Assert.assertEquals;

public class FrameTest extends AbstractComponentUnitTest<APLAbsoluteLayout, Frame> {


    @Before
    public void doBefore() {
        REQUIRED_PROPERTIES = ""; // no required properties in Frame Component.
        OPTIONAL_PROPERTIES =
                " \"borderTopLeftRadius\": 10,\n" +
                        " \"borderTopRightRadius\": 20,\n" +
                        " \"borderBottomRightRadius\": 30,\n" +
                        " \"borderBottomLeftRadius\": 40,\n" +
                        " \"borderWidth\": 10,\n" +
                        " \"borderColor\": \"red\",\n" +
                        " \"backgroundColor\": \"yellow\"";
    }


    /**
     * @return The string representation used in the APL document for this component.  For example
     * when testing the Text Component this method should return "Text".
     */
    @Override
    String getComponentType() {
        return "Frame";
    }

    /**
     * Test the required properties of the Component.
     *
     * @param component The Component for testing.
     */
    @Override
    void testProperties_required(Frame component) {
        // No required properties
    }

    /**
     * Test the optional properties of the Component.  This test should check for default value
     * and values. {@link #OPTIONAL_PROPERTIES} should be set prior to this test to ensure a valid
     * Component is created.
     *
     * @param component The Component for testing.
     */
    @Override
    void testProperties_optionalDefaultValues(Frame component) {
        assertEquals(Color.TRANSPARENT, component.getBackgroundColor());
        assertEquals(Color.TRANSPARENT, component.getBorderColor());
        assertEquals(0, component.getBorderWidth().intValue());
        Radii radii = component.getBorderRadii();
        assertEquals(0, radii.topLeft(), 0);
        assertEquals(0, radii.topRight(), 0);
        assertEquals(0, radii.bottomRight(), 0);
        assertEquals(0, radii.bottomLeft(), 0);
    }

    /**
     * Test the optional properties of the Component.  This test should check values when the property
     * is set explicitly, and should use values other than the default.  Set the {@link #OPTIONAL_PROPERTIES}
     * value before this test.
     *
     * @param component The Component for testing.
     */
    @Override
    void testProperties_optionalExplicitValues(Frame component) {
        assertEquals(Color.YELLOW, component.getBackgroundColor());
        assertEquals(Color.RED, component.getBorderColor());
        assertEquals(10, component.getBorderWidth().intValue());
        Radii radii = component.getBorderRadii();
        assertEquals(10, radii.topLeft(), 0);
        assertEquals(20, radii.topRight(), 0);
        assertEquals(30, radii.bottomRight(), 0);
        assertEquals(40, radii.bottomLeft(), 0);

        // verify shadow corners match frame corners
        float[] shadowRadii = component.getShadowCornerRadius();
        assertEquals(radii.topLeft(), shadowRadii[0], 0.01f);
        assertEquals(radii.topRight(), shadowRadii[1], 0.01f);
        assertEquals(radii.bottomRight(), shadowRadii[2], 0.01f);
        assertEquals(radii.bottomLeft(), shadowRadii[3], 0.01f);
    }
}
