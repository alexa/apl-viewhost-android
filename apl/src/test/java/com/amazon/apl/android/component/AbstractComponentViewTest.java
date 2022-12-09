/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.component;

import static com.amazon.apl.android.APLTestDocs.COMPONENT_BASE_DOC;
import static com.amazon.apl.android.APLTestDocs.COMPONENT_OPTIONAL_COMMON_PROPERTIES;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import android.view.View;
import android.view.ViewGroup;

import androidx.core.view.ViewCompat;
import com.amazon.apl.android.APLOptions;
import com.amazon.apl.android.Component;
import com.amazon.apl.android.RootContext;
import com.amazon.apl.android.primitive.Rect;
import com.amazon.apl.android.robolectric.ActivityTest;
import com.amazon.apl.android.utils.TestClock;
import com.amazon.apl.android.views.APLAbsoluteLayout;

import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

/**
 * This abstract test class provides a basic framework for testing the view associated
 * with an APL Component. Subclasses should provide APL document Content segments for
 * <p>
 * REQUIRED_PROPERTIES - Component properties required and must be set for the document success.
 * OPTIONAL_PROPERTIES - Component properties that are optional and need not be set in the document.
 * CHILD_LAYOUT_PROPERTIES - Component properties that define children of the Component.
 */
public abstract class AbstractComponentViewTest<V extends View, C extends Component> extends ActivityTest {

    // Default required properties, default to empty.
    protected String REQUIRED_PROPERTIES = "";
    // Default optional properties, default to empty.
    protected String OPTIONAL_PROPERTIES = "";
    // Properties for Components with children.
    protected static String CHILD_LAYOUT_PROPERTIES = "";

    protected TestClock testClock = new TestClock();

    /**
     * @return The string representation used in the APL document for this component.  For example
     * when testing the Text Component this method should return "Text".
     */
    abstract String getComponentType();

    abstract Class<V> getViewClass();

    /**
     * Test the view after properties have been assigned.
     *
     * @param view The Component View for testing.
     **/
    abstract void testView_applyProperties(V view);

    public V getTestView() {
        return mTestContext.getTestView();
    }

    public C getTestComponent() {
        return mTestContext.getTestComponent();
    }

    protected void inflate(String... componentProps) {
        StringBuilder props = new StringBuilder();
        for (int i = 0; i < componentProps.length; i++) {
            if (componentProps[i].length() > 0) {
                props.append(",");
                props.append(componentProps[i]);
            }
        }

        inflate(String.format(COMPONENT_BASE_DOC, "1.0", getComponentType(), props.toString(), ""),
                null, null, APLOptions.builder(), null);
    }

    @Test
    public void testView_create() {
        inflate(REQUIRED_PROPERTIES, CHILD_LAYOUT_PROPERTIES);

        Component component = mTestContext.getTestComponent();
        activityRule.getScenario().onActivity(activity -> {
            assertNotNull(activity.findViewById(component.getComponentId().hashCode()));
        });
    }

    @Test
    public void testView_layout() {
        inflate(REQUIRED_PROPERTIES, CHILD_LAYOUT_PROPERTIES);

        RootContext rootContext = mTestContext.getRootContext();
        activityRule.getScenario().onActivity(activity -> {
            Map<String, Component> components = rootContext.getComponents();
            for (Component component : components.values()) {
                assertNotNull(activity.findViewById(component.getComponentId().hashCode()));
            }
        });
    }

    @Test
    public void testView_absoluteLayout() {
        inflate(CHILD_LAYOUT_PROPERTIES);

        if (!APLAbsoluteLayout.class.isAssignableFrom(mTestContext.getTestView().getClass())) {
            return;
        }
        if ("".equals(CHILD_LAYOUT_PROPERTIES)) {
            fail("Component needs test for child layout");
        }

        C component = mTestContext.getTestComponent();
        ViewGroup view = mTestContext.getTestView();
        assertTrue(getViewClass().isAssignableFrom(view.getClass()));

        for (int i = 0; i < Math.min(component.getChildCount(), view.getChildCount()); i++) {
            Component child = component.getChildAt(i);
            View childView = view.getChildAt(i);

            // Not strictly required that component order match view order, but that is
            // the current implementation, and a mismatch indicates something went wrong
            assertEquals("View hierarchy is incorrect", component.getComponentId().hashCode(), view.getId());

            Rect bounds = child.getBounds();
            int left = bounds.intLeft();
            int top = bounds.intTop();
            int right = bounds.intRight();
            int bottom = bounds.intBottom();

            assertEquals("Invalid left position", left, childView.getLeft());
            assertEquals("Invalid top position", top, childView.getTop());
            assertEquals("Invalid right position", right, childView.getRight());
            assertEquals("Invalid bottom position", bottom, childView.getBottom());
        }
    }


    /**
     * Test that the view was correctly assigned properties. This test uses REQUIRED_PROPERTIES
     * and OPTIONAL_PROPERTIES.
     */
    @Test
    public void testView_applyProperties() {
        inflate(REQUIRED_PROPERTIES, OPTIONAL_PROPERTIES);

        V view = mTestContext.getTestView();
        // Call down to the subclass for tests.
        testView_applyProperties(view);
    }

    /**
     * Test that the view is correctly assigned properties that are common to all Components.
     * This test uses COMPONENT_OPTIONAL_COMMON_PROPERTIES.
     */
    @Test
    public void testView_applyCommonProperties() {
        inflate(REQUIRED_PROPERTIES, COMPONENT_OPTIONAL_COMMON_PROPERTIES);

        View view = mTestContext.getTestView();

        Assert.assertEquals("Go APL", view.getContentDescription());
        assertFalse(view.isEnabled());
        Assert.assertEquals(View.INVISIBLE, view.getVisibility());
        Assert.assertEquals(.5f, view.getAlpha(), 0.0f);
        Assert.assertTrue(ViewCompat.hasAccessibilityDelegate(view));
    }

    /**
     * Tests dynamically updating common component properties via SetValue commands.
     */
    @Test
    public void testView_dynamicProperties() {
        inflate(REQUIRED_PROPERTIES, CHILD_LAYOUT_PROPERTIES);

        View view = getTestView();

        // Test initial conditions
        assertEquals(1.0f, view.getAlpha());
        assertEquals(View.VISIBLE, view.getVisibility());
        assertTrue(view.isEnabled());
        assertEquals("", view.getContentDescription());

        // Change opacity
        executeCommands(setValueCommand("opacity", "0.5"));
        assertEquals(0.5f, view.getAlpha());

        // Change display
        executeCommands(setValueCommand("display", "none"));
        assertEquals(View.GONE, view.getVisibility());

        // Change disabled
        executeCommands(setValueCommand("disabled", "true"));
        assertFalse(view.isEnabled());

        // Change accessibilityLabel
        executeCommands(setValueCommand("accessibilityLabel", "Accessibility Label 1"));
        assertEquals("Accessibility Label 1", view.getContentDescription());
    }

    static String setValueCommand(String property, String value) {
        final String baseCommand = "[\n" +
                "                {\n" +
                "                    \"type\": \"SetValue\",\n" +
                "                    \"property\": \"%s\",\n" +
                "                    \"value\": \"%s\",\n" +
                "                    \"componentId\": \"testcomp\"\n" +
                "                }\n" +
                "            ]";

        return String.format(baseCommand, property, value);
    }
}
