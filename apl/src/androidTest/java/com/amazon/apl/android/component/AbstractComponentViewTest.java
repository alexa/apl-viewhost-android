/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.component;

import androidx.core.view.ViewCompat;
import android.view.View;
import android.view.ViewGroup;

import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.ViewAssertion;
import androidx.test.espresso.core.internal.deps.guava.base.Preconditions;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;

import com.amazon.apl.android.views.APLAbsoluteLayout;
import com.amazon.apl.android.APLController;
import com.amazon.apl.android.APLLayout;
import com.amazon.apl.android.APLOptions;
import com.amazon.apl.android.APLTestContext;
import com.amazon.apl.android.Component;
import com.amazon.apl.android.Content;
import com.amazon.apl.android.RootContext;
import com.amazon.apl.android.TestActivity;
import com.amazon.apl.android.primitive.Rect;

import org.hamcrest.Matcher;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import java.util.Map;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.actionWithAssertions;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayingAtLeast;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static com.amazon.apl.android.APLTestDocs.COMPONENT_BASE_DOC;
import static com.amazon.apl.android.APLTestDocs.COMPONENT_OPTIONAL_COMMON_PROPERTIES;
import static com.amazon.apl.android.espresso.APLViewActions.executeCommands;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * This abstract test class provides a basic framework for testing the view associated
 * with an APL Component. Subclasses should provide APL document Content segments for
 * <p>
 * REQUIRED_PROPERTIES - Component properties required and must be set for the document success.
 * OPTIONAL_PROPERTIES - Component properties that are optional and need not be set in the document.
 * CHILD_LAYOUT_PROPERTIES - Component properties that define children of the Component.
 */
public abstract class AbstractComponentViewTest<V extends View, C extends Component> {

    // Load the APL library.
    static {
        APLController.initializeAPL(InstrumentationRegistry.getInstrumentation().getContext());
    }

    protected APLTestContext mTestContext;

    @Rule
    public ActivityTestRule<TestActivity> activityRule = new ActivityTestRule<>(TestActivity.class);


    // Default required properties, default to empty.
    protected String REQUIRED_PROPERTIES = "";
    // Default optional properties, default to empty.
    protected String OPTIONAL_PROPERTIES = "";
    // Properties for Components with children.
    protected static String CHILD_LAYOUT_PROPERTIES = "";

    /**
     * @return The string representation used in the APL document for this component.  For example
     * when testing the Text Component this method should return "Text".
     */
    abstract String getComponentType();

    abstract Class<V> getViewClass();

    private class InflateAPLViewAction implements ViewAction {
        public InflateAPLViewAction(APLOptions options, String componentProps) {
            mTestContext = new APLTestContext()
                    .setDocument(COMPONENT_BASE_DOC, getComponentType(), componentProps, "")
                    .setAplOptions(options)
                    .buildRootContextDependencies();
        }

        @Override
        public Matcher<View> getConstraints() {
            Matcher<View> standardConstraint = isDisplayingAtLeast(90);
            return standardConstraint;
        }

        @Override
        public String getDescription() {
            return "Inflate a Document";
        }

        @Override
        public void perform(UiController uiController, View view) {
            Content content = mTestContext.getContent();
            Assert.assertTrue("Failed to create Content", content.isReady());

            APLLayout aplLayout = activityRule.getActivity().findViewById(com.amazon.apl.android.test.R.id.apl);
            try {
                APLController.renderDocument(content, mTestContext.getAplOptions(), mTestContext.getRootConfig(), aplLayout.getPresenter());
            } catch (APLController.APLException e) {
                Assert.fail(e.getMessage());
            }
        }
    }

    /**
     * Test the view after properties have been assigned.
     *
     * @param view The Component View for testing.
     **/
    abstract void testView_applyProperties(V view);

    Matcher<View> withComponent(Component component) {
        return withId(is(component.getComponentId().hashCode()));

    }

    public V getTestView() {
        return mTestContext.getTestView();
    }

    public C getTestComponent() {
        return mTestContext.getTestComponent();
    }

    private class RootContextViewAssertion implements ViewAssertion {
        @Override
        public void check(View view, NoMatchingViewException noViewFoundException) {
            Preconditions.checkNotNull(view);
            RootContext rc = ((APLLayout) view).getAPLContext();
            // Store the RC configuration made by this test
            mTestContext.setRootContext(rc);
            mTestContext.setPresenter(rc.getViewPresenter());
            assertNotNull("RootContext create failed", mTestContext.getRootContext());
        }
    }

    ViewAction inflate(String... componentProps) {
        return inflateWithOptions(null, componentProps);
    }

    ViewAction inflateWithOptions(APLOptions options, String... componentProps) {
        StringBuilder props = new StringBuilder();
        for (int i = 0; i < componentProps.length; i++) {
            if (componentProps[i].length() > 0) {
                props.append(",");
                props.append(componentProps[i]);
            }
        }
        return actionWithAssertions(new InflateAPLViewAction(options, props.toString()));
    }

    ViewAssertion hasRootContext() {
        return new RootContextViewAssertion();
    }


    @Test
    @SmallTest
    public void testView_create() {
        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(inflate(REQUIRED_PROPERTIES, CHILD_LAYOUT_PROPERTIES))
                .check(hasRootContext());

        Component component = mTestContext.getTestComponent();
        onView(withComponent(component))
                .check(matches(isDisplayed()));
    }

    @Test
    @LargeTest
    public void testView_layout() {
        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(inflate(REQUIRED_PROPERTIES, CHILD_LAYOUT_PROPERTIES))
                .check(hasRootContext());

        RootContext rootContext = mTestContext.getRootContext();
        Map<String, Component> components = rootContext.getComponents();
        for (Component component : components.values()) {
            onView(withComponent(component))
                    .check(matches(isDisplayed()));
        }
    }


    @Test
    @LargeTest
    public void testView_absoluteLayout() {


        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(inflate(CHILD_LAYOUT_PROPERTIES))
                .check(hasRootContext());

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
            RootContext rootContext = mTestContext.getRootContext();
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
    @LargeTest
    public void testView_applyProperties() {
        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(inflate(REQUIRED_PROPERTIES, OPTIONAL_PROPERTIES))
                .check(hasRootContext());

        V view = mTestContext.getTestView();
        // Call down to the subclass for tests.
        testView_applyProperties(view);
    }


    /**
     * Test that the view is correctly assigned properties that are common to all Components.
     * This test uses COMPONENT_OPTIONAL_COMMON_PROPERTIES.
     */
    @Test
    @LargeTest
    public void testView_applyCommonProperties() {
        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(inflate(REQUIRED_PROPERTIES, COMPONENT_OPTIONAL_COMMON_PROPERTIES))
                .check(hasRootContext());

        View view = mTestContext.getTestView();

        Assert.assertEquals("Go APL", view.getContentDescription());
        assertFalse(view.isEnabled());
        Assert.assertEquals(View.INVISIBLE, view.getVisibility());
        Assert.assertEquals(.5f, view.getAlpha(), 0.0f);
        Assert.assertTrue(ViewCompat.hasAccessibilityDelegate(view));
    }

    // TODO: Padding tests
    // TODO: transform tests

    /**
     * Tests dynamically updating common component properties via SetValue commands.
     */
    @Test
    public void testView_dynamicProperties() {
        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(inflate(REQUIRED_PROPERTIES, CHILD_LAYOUT_PROPERTIES))
                .check(hasRootContext());

        View view = getTestView();

        // Test initial conditions
        assertEquals(1.0f, view.getAlpha());
        assertEquals(View.VISIBLE, view.getVisibility());
        assertTrue(view.isEnabled());
        assertEquals("", view.getContentDescription());

        // Change opacity
        onView(isRoot())
                .perform(executeCommands(mTestContext.getRootContext(), setValueCommand("opacity", "0.5")));
        assertEquals(0.5f, view.getAlpha());

        // Change display
        onView(isRoot())
                .perform(executeCommands(mTestContext.getRootContext(), setValueCommand("display", "none")));
        assertEquals(View.GONE, view.getVisibility());

        // Change disabled
        onView(isRoot())
                .perform(executeCommands(mTestContext.getRootContext(), setValueCommand("disabled", "true")));
        assertFalse(view.isEnabled());

        // Change accessibilityLabel
        onView(isRoot())
                .perform(executeCommands(mTestContext.getRootContext(), setValueCommand("accessibilityLabel", "Accessibility Label 1")));
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
