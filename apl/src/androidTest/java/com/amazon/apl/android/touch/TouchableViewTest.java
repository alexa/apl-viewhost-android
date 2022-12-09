/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.touch;

import android.graphics.Color;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;

import com.amazon.apl.android.APLLayout;
import com.amazon.apl.android.APLOptions;
import com.amazon.apl.android.configuration.ConfigurationChange;
import com.amazon.apl.android.dependencies.ISendEventCallbackV2;
import com.amazon.apl.android.document.AbstractDocViewTest;
import com.amazon.apl.android.Component;
import com.amazon.apl.android.espresso.APLViewActions;

import org.hamcrest.Matcher;
import org.junit.Test;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.swipeLeft;
import static androidx.test.espresso.action.ViewActions.swipeUp;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static com.amazon.apl.android.espresso.APLViewActions.motionEvent;
import static com.amazon.apl.android.espresso.APLViewActions.performClick;
import static com.amazon.apl.android.espresso.APLViewActions.requestFocus;
import static com.amazon.apl.android.espresso.APLViewActions.waitFor;
import static com.amazon.apl.android.espresso.APLViewAssertions.hasBackgroundColor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

abstract class TouchableViewTest extends AbstractDocViewTest {
    static final String INITIAL_OUTER_COLOR = "black";
    static final String INITIAL_INNER_COLOR = "white";
    static final String DOWN_COLOR = "red";
    static final String UP_COLOR = "blue";
    static final String MOVE_COLOR = "magenta";
    static final String PRESS_COLOR = "purple";
    static final String CANCEL_COLOR = "yellow";

    static final String ON_DOWN =
            "\"onDown\": {\n" +
                    "  \"type\": \"SetValue\",\n" +
                    "  \"componentId\": \"testcomp\",\n" +
                    "  \"property\": \"backgroundColor\",\n" +
                    "  \"value\": \"" + DOWN_COLOR + "\"\n" +
                    "}\n";

    static final String ON_MOVE =
            "\"onMove\": {\n" +
                    "  \"type\": \"SetValue\",\n" +
                    "  \"componentId\": \"testcomp\",\n" +
                    "  \"property\": \"backgroundColor\",\n" +
                    "  \"value\": \"" + MOVE_COLOR + "\"\n" +
                    "}\n";

    static final String ON_UP =
            "\"onUp\": [{\n" +
                    "  \"type\": \"SetValue\",\n" +
                    "  \"componentId\": \"inner\",\n" +
                    "  \"property\": \"backgroundColor\",\n" +
                    "  \"value\": \"" + UP_COLOR + "\"\n" +
                    "}\n," +
                    "{\n" +
                    "  \"type\": \"SetValue\",\n" +
                    "  \"componentId\": \"testcomp\",\n" +
                    "  \"property\": \"backgroundColor\",\n" +
                    "  \"value\": \"" + UP_COLOR + "\"\n" +
                    "}]";

    static final String ARGUMENT_FIRED_1 = "fired1";
    static final String ARGUMENT_FIRED_2 = "fired2";

    static final String ON_PRESS =
                    "\"onPress\": [\n" +
                    "        {\n" +
                    "          \"type\": \"Sequential\",\n" +
                    "          \"sequencer\": \"MAGIC\",\n" +
                    "          \"commands\": [\n" +
                    "            {\n" +
                    "              \"type\": \"SendEvent\",\n" +
                    "              \"arguments\": [\"" + ARGUMENT_FIRED_1 + "\"]" +
                    "            },\n" +
                    "            {\n" +
                    "              \"type\": \"SendEvent\",\n" +
                    "              \"delay\": 800,\n" +
                    "              \"arguments\": [\"" + ARGUMENT_FIRED_2 + "\"]" +
                    "            }\n" +
                    "          ],\n" +
                    "          \"finally\": [\n" +
                    "            {\n" +
                    "              \"type\": \"AnimateItem\",\n" +
                    "              \"componentId\": \"slide-0\",\n" +
                    "              \"value\": [\n" +
                    "                {\n" +
                    "                  \"property\": \"opacity\",\n" +
                    "                  \"from\": 0,\n" +
                    "                  \"to\": 1\n" +
                    "                }\n" +
                    "              ],\n" +
                    "              \"duration\": 1000\n" +
                    "            }\n" +
                    "          ]\n" +
                    "        }\n" +
                    "      ]\n";

    static final String ON_CANCEL =
            "\"onCancel\": {\n" +
                    "  \"type\": \"SetValue\",\n" +
                    "  \"componentId\": \"testcomp\",\n" +
                    "  \"property\": \"backgroundColor\",\n" +
                    "  \"value\": \"" + CANCEL_COLOR + "\"\n" +
                    "}\n";

    static String ACTIONS_WITHOUT_COMMANDS =
            "\"actions\": [\n" +
            "  {\n" +
            "    \"name\": \"activate\",\n" +
            "    \"label\": \"Activate simulation\"\n" +
            "  }\n" +
            "]\n";

    static final String TOUCHABLE_COMPONENT =
            "\"type\": \"%s\",\n" +
            "%s" +
            "\"width\": \"25vw\",\n" +
            "\"height\": \"25vh\",\n" +
            "\"id\": \"touchable\",\n" +
            ON_DOWN + ",\n" +
            ON_MOVE + ",\n" +
            ON_UP + ",\n" +
            ON_PRESS + ",\n" +
            ON_CANCEL;

    static String ACTIONS_WITH_COMMANDS =
            "\"actions\": [\n" +
            "  {\n" +
            "    \"name\": \"activate\",\n" +
            "    \"label\": \"Activate simulation\",\n" +
            "    \"command\": {\n" +
                    "  \"type\": \"SetValue\",\n" +
                    "  \"componentId\": \"testcomp\",\n" +
                    "  \"property\": \"backgroundColor\",\n" +
                    "  \"value\": \"" + PRESS_COLOR + "\"\n" +
                    "}\n" +
            "  }\n" +
            "]\n";

    static String TOUCHABLE_ACTIONS_WITHOUT_COMMANDS =
            "      \"type\": \"Frame\",\n" +
                    "      \"width\": \"100vw\",\n" +
                    "      \"height\": \"100vh\",\n" +
                    "      \"backgroundColor\": \"" + INITIAL_OUTER_COLOR + "\",\n" +
                    "      \"item\": {\n" +
                    "        \"type\": \"Frame\",\n" +
                    "        \"id\": \"inner\",\n" +
                    "        \"width\": \"50vw\",\n" +
                    "        \"height\": \"50vh\",\n" +
                    "        \"backgroundColor\": \"" + INITIAL_INNER_COLOR + "\",\n" +
                    "        \"item\": {\n" +
                    TOUCHABLE_COMPONENT + ",\n" +
                    ACTIONS_WITHOUT_COMMANDS +
                    "        }\n" +
                    "      }\n" +
                    "    }";

    static String TOUCHABLE_ACTIONS_WITH_COMMANDS =
            "      \"type\": \"Frame\",\n" +
                    "      \"width\": \"100vw\",\n" +
                    "      \"height\": \"100vh\",\n" +
                    "      \"backgroundColor\": \"" + INITIAL_OUTER_COLOR + "\",\n" +
                    "      \"item\": {\n" +
                    "        \"type\": \"Frame\",\n" +
                    "        \"id\": \"inner\",\n" +
                    "        \"width\": \"50vw\",\n" +
                    "        \"height\": \"50vh\",\n" +
                    "        \"backgroundColor\": \"" + INITIAL_INNER_COLOR + "\",\n" +
                    "        \"item\": {\n" +
                    TOUCHABLE_COMPONENT + ",\n" +
                    ACTIONS_WITH_COMMANDS +
                    "        }\n" +
                    "      }\n" +
                    "    }";

    /**
     * Document with an outer frame and inner frame and a touchable component.
     */
    static String TOUCHABLE =
            "      \"type\": \"Frame\",\n" +
            "      \"width\": \"100vw\",\n" +
            "      \"height\": \"100vh\",\n" +
            "      \"backgroundColor\": \"" + INITIAL_OUTER_COLOR + "\",\n" +
            "      \"item\": {\n" +
            "        \"type\": \"Frame\",\n" +
            "        \"id\": \"inner\",\n" +
            "        \"width\": \"50vw\",\n" +
            "        \"height\": \"50vh\",\n" +
            "        \"backgroundColor\": \"" + INITIAL_INNER_COLOR + "\",\n" +
            "        \"item\": {\n" +
            TOUCHABLE_COMPONENT +
            "        }\n" +
            "    }";

    /**
     * Document with a touchable component inside a scrollable component.
     */
    static String TOUCHABLE_IN_SCROLLABLE =
            "      \"type\": \"Frame\",\n" +
            "      \"width\": \"100vw\",\n" +
            "      \"height\": \"100vh\",\n" +
            "      \"backgroundColor\": \"" + INITIAL_OUTER_COLOR + "\",\n" +
            "      \"item\": {\n" +
            "        \"type\": \"%s\",\n" +
            "        \"width\": \"50vw\",\n" +
            "        \"height\": \"50vh\",\n" +
            "        \"item\": {\n" +
            "          \"type\": \"Container\",\n" +
            "          \"items\": [\n" +
            "            {\n" +
            TOUCHABLE_COMPONENT +
            "            },\n" +
            "            {\n" +
            "              \"type\": \"Frame\",\n" +
            "              \"id\": \"inner\",\n" +
            "              \"width\": \"50vw\",\n" +
            "              \"height\": \"50vh\"\n" +
            "            }\n" +
            "          ]\n" +
            "        }\n" +
            "      }\n" +
            "    }";


    /**
     * Doc with a touchable component inside a scrollable component.
     */
    static final String TOUCHABLE_IN_PAGER =
            "\"type\": \"Frame\",\n" +
            "      \"width\": \"100vw\",\n" +
            "      \"height\": \"100vh\",\n" +
            "      \"backgroundColor\": \"" + INITIAL_OUTER_COLOR + "\",\n" +
            "      \"item\": {\n" +
            "        \"type\": \"Pager\",\n" +
            "        \"width\": \"50vw\",\n" +
            "        \"height\": \"50vh\",\n" +
            "          \"items\": [\n" +
            "            {\n" +
            TOUCHABLE_COMPONENT +
            "            },\n" +
            "            {\n" +
            "              \"type\": \"Frame\",\n" +
            "              \"id\": \"inner\",\n" +
            "              \"width\": \"50vw\",\n" +
            "              \"height\": \"50vh\"\n" +
            "            }\n" +
            "          ]\n" +
            "      }";

    protected ISendEventCallbackV2 mSendEventCallback = mock(ISendEventCallbackV2.class);

    String getComponentProps() { return ""; }

    String getDocumentProps() { return DOCUMENT_PROPERTIES; }

    abstract String getComponentType();

    APLOptions getOptions() { return APLOptions.builder().sendEventCallbackV2(mSendEventCallback).build(); }

    void inflate() {
        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(inflateWithOptions(String.format(TOUCHABLE, getComponentType(), getComponentProps()), getDocumentProps(), getOptions()))
                .check(hasRootContext());
    }

    void inflateActionsWithoutCommands() {
        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(inflateWithOptions(String.format(TOUCHABLE_ACTIONS_WITHOUT_COMMANDS, getComponentType(), getComponentProps()), getDocumentProps(), getOptions()))
                .check(hasRootContext());
    }

    void inflateActionsWithCommands() {
        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(inflateWithOptions(String.format(TOUCHABLE_ACTIONS_WITH_COMMANDS, getComponentType(), getComponentProps()), getDocumentProps(), getOptions()))
                .check(hasRootContext());
    }

    void inflateDisabled() {
        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(inflate(String.format(TOUCHABLE, getComponentType(), getComponentProps() + "\"disabled\": \"true\","), getDocumentProps()))
                .check(hasRootContext());
    }

    void inflateSequence() {
        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(inflateWithOptions(String.format(TOUCHABLE_IN_SCROLLABLE, "Sequence", getComponentType(), getComponentProps()), getDocumentProps(), getOptions()))
                .check(hasRootContext());
    }

    void inflateScrollView() {
        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(inflateWithOptions(String.format(TOUCHABLE_IN_SCROLLABLE, "ScrollView", getComponentType(), getComponentProps()), getDocumentProps(), getOptions()))
                .check(hasRootContext());
    }

    void inflatePager() {
        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(inflateWithOptions(String.format(TOUCHABLE_IN_PAGER, getComponentType(), getComponentProps()), getDocumentProps(), getOptions()))
                .check(hasRootContext());
    }

    Component getTouchableComponent() {
        return mTestContext.getRootContext().findComponentById("touchable");
    }

    Component getOuterFrame() {
        return mTestContext.getRootContext().findComponentById("testcomp");
    }

    Component getInnerFrame() {
        return mTestContext.getRootContext().findComponentById("inner");
    }

    @Test
    public void testView_click() {
        inflate();
        verifyClick(click());
    }

    @Test
    public void testView_click_sequential_continues_after_reinflation() {
        inflate();
        // Press TouchWrapper to start the Sequential command sequence in the onPress handler
        // The verifyClick method verifies that the first SendEvent command is executed
        verifyClick(click());
        // Reinflate
        // Dummy config
        ConfigurationChange configChange = mTestContext.getRootContext().createConfigurationChange()
                .build();

        mTestContext.getRootContext().handleConfigurationChange(configChange);
        onView(isRoot())
                .perform(waitFor(500));
        // The reinflate command is defined in BASE_DOC which sends an event before re-inflation
        String[] expectedArgument = new String[]{BASE_DOC_REINFLATE_SEND_EVENT_ARGUMENT};
        verify(mSendEventCallback).onSendEvent(eq(expectedArgument), any(), any(), any());
        onView(isRoot())
                .perform(waitFor(800));
        // Verify that the remaining command in Sequential is executed after remaining delay
        expectedArgument = new String[]{ARGUMENT_FIRED_2};
        verify(mSendEventCallback).onSendEvent(eq(expectedArgument), any(), any(), any());
    }

    @Test
    public void testView_clickAccessibility() {
        inflate();
        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(new ViewAction() {
                    @Override
                    public Matcher<View> getConstraints() {
                        return isDisplayed();
                    }

                    @Override
                    public String getDescription() {
                        return null;
                    }

                    @Override
                    public void perform(UiController uiController, View view) {
                        ((APLLayout) view).onAccessibilityStateChanged(true);
                    }
                });

        verifyClick(performClick());
    }

    @Test
    public void testView_clickAccessibilityInactive() {
        inflate();

        onView(withComponent(getOuterFrame()))
                .check(hasBackgroundColor(Color.parseColor(INITIAL_OUTER_COLOR)));

        onView(withComponent(getTouchableComponent()))
                .perform(requestFocus())
                .perform(performClick());

        // Wait for key handling
        onView(isRoot())
                .perform(waitFor(100));

        onView(withComponent(getOuterFrame()))
                .check(hasBackgroundColor(Color.parseColor(INITIAL_OUTER_COLOR)));
    }

    @Test
    public void testView_complexMovement() {
        inflate();
        View touchableView = mTestContext.getPresenter().findView(getTouchableComponent());

        float centerX = touchableView.getX() + touchableView.getWidth() / 2f;
        float centerY = touchableView.getY() + touchableView.getHeight() / 2f;

        // Verify initial state
        onView(withComponent(getOuterFrame()))
                .check(hasBackgroundColor(Color.parseColor(INITIAL_OUTER_COLOR)));
        onView(withComponent(getInnerFrame()))
                .check(hasBackgroundColor(Color.parseColor(INITIAL_INNER_COLOR)));

        // DOWN -> MOVE -> UP
        onView(withComponent(getOuterFrame()))
                .perform(motionEvent(mTestContext.getPresenter(), MotionEvent.ACTION_DOWN, centerX, centerY))
                .check(hasBackgroundColor(Color.parseColor(DOWN_COLOR)))
                .perform(motionEvent(mTestContext.getPresenter(), MotionEvent.ACTION_MOVE, centerX + 10, centerY + 10))
                .check(hasBackgroundColor(Color.parseColor(MOVE_COLOR)));

        verifyZeroInteractions(mSendEventCallback);

        onView(withComponent(getOuterFrame()))
                .perform(motionEvent(mTestContext.getPresenter(), MotionEvent.ACTION_UP, centerX + 10, centerY + 10));

        onView(withComponent(getInnerFrame()))
                .check(hasBackgroundColor(Color.parseColor(UP_COLOR))); // verify up handler invoked

        String[] expectedArguments = new String[]{ARGUMENT_FIRED_1};
        verify(mSendEventCallback).onSendEvent(eq(expectedArguments), any(), any(), any());
    }

    @Test
    public void testView_upOutsideOfBounds() {
        inflate();
        View touchableView = mTestContext.getPresenter().findView(getTouchableComponent());

        float centerX = touchableView.getX() + touchableView.getWidth() / 2f;
        float centerY = touchableView.getY() + touchableView.getHeight() / 2f;

        float outOfBoundsX = centerX + touchableView.getWidth() / 2f + 2;
        float outOfBoundsY = centerY + touchableView.getHeight() / 2f + 2;

        // DOWN -> MOVE -> UP
        onView(withComponent(getOuterFrame()))
                .perform(motionEvent(mTestContext.getPresenter(), MotionEvent.ACTION_DOWN, centerX, centerY))
                .check(hasBackgroundColor(Color.parseColor(DOWN_COLOR)))
                .perform(motionEvent(mTestContext.getPresenter(), MotionEvent.ACTION_MOVE, outOfBoundsX, outOfBoundsY))
                .check(hasBackgroundColor(Color.parseColor(MOVE_COLOR)))
                .perform(motionEvent(mTestContext.getPresenter(), MotionEvent.ACTION_UP, outOfBoundsX, outOfBoundsY))
                .check(hasBackgroundColor(Color.parseColor(UP_COLOR)));

        onView(withComponent(getInnerFrame()))
                .check(hasBackgroundColor(Color.parseColor(UP_COLOR)));
    }

    @Test
    public void testView_cancelStopsOnPress() {
        inflate();
        final View touchableView = mTestContext.getPresenter().findView(getTouchableComponent());

        float centerX = touchableView.getX() + touchableView.getWidth() / 2f;
        float centerY = touchableView.getY() + touchableView.getHeight() / 2f;

        onView(withComponent(getOuterFrame()))
                .perform(motionEvent(mTestContext.getPresenter(), MotionEvent.ACTION_DOWN, centerX, centerY))
                .check(hasBackgroundColor(Color.parseColor(DOWN_COLOR)));

        mTestContext.getPresenter().cancelTouchEvent();

        onView(isRoot())
                .perform(waitFor(100));

        onView(withComponent(getOuterFrame()))
                .check(hasBackgroundColor(Color.parseColor(CANCEL_COLOR)));
    }

    @Test
    public void testView_enterKeyPress() {
        inflate();
        // Focus the component with core.
        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(APLViewActions.pressKey(KeyEvent.KEYCODE_DPAD_DOWN));
        verifyClick(APLViewActions.pressKey(KeyEvent.KEYCODE_ENTER));
    }

    @Test
    public void testView_dpadCenterKeyPress() {
        inflate();
        // gain initial focus
        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(APLViewActions.pressKey(KeyEvent.KEYCODE_DPAD_RIGHT));

        verifyClick(APLViewActions.pressKey(KeyEvent.KEYCODE_DPAD_CENTER));
    }

    @Test
    public void testView_disabled() {
        inflateDisabled();
        onView(withComponent(getTouchableComponent()))
                .perform(click());

        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(APLViewActions.pressKey(KeyEvent.KEYCODE_DPAD_CENTER))
                .perform(APLViewActions.pressKey(KeyEvent.KEYCODE_ENTER));

        // Frame is still black
        onView(withComponent(mTestContext.getTestComponent()))
                .check(hasBackgroundColor(Color.parseColor(INITIAL_OUTER_COLOR)));
    }

    @Test
    public void testView_sequence_onCancel() {
        inflateSequence();
        verifyCancelBehavior(swipeLeft(), swipeUp());
    }

    @Test
    public void testView_scrollView_onCancel() {
        inflateScrollView();
        verifyCancelBehavior(swipeLeft(), swipeUp());
    }

    @Test
    public void testView_pager_onCancel() {
        inflatePager();
        verifyCancelBehavior(swipeUp(), swipeLeft());
    }

    @Test
    public void testView_accessibility_action_defaultHandler() {
        inflateActionsWithoutCommands();
        final View touchableView = mTestContext.getPresenter().findView(getTouchableComponent());

        // simulation of screen reader enabled
        activityRule.getScenario().onActivity(
                activity -> ViewCompat.onInitializeAccessibilityNodeInfo(touchableView, AccessibilityNodeInfoCompat.obtain(touchableView)));

        // simulation of accessibility "activate' action
        activityRule.getScenario().onActivity(
                activity -> ViewCompat.performAccessibilityAction(touchableView, AccessibilityNodeInfoCompat.ACTION_CLICK, null));

        onView(isRoot())
                .perform(waitFor(100));

        // since "activate" action does not have any command, it will invoke its default event handler (onPress)
        String[] expectedArguments = new String[]{ARGUMENT_FIRED_1};
        verify(mSendEventCallback).onSendEvent(eq(expectedArguments), any(), any(), any());
    }

    @Test
    public void testView_accessibility_action() {
        inflateActionsWithCommands();
        final View touchableView = mTestContext.getPresenter().findView(getTouchableComponent());

        // simulation of screen reader enabled
        activityRule.getScenario().onActivity(
                activity -> ViewCompat.onInitializeAccessibilityNodeInfo(touchableView, AccessibilityNodeInfoCompat.obtain(touchableView)));

        // simulation of accessibility "activate' action
        activityRule.getScenario().onActivity(
                activity -> ViewCompat.performAccessibilityAction(touchableView, AccessibilityNodeInfoCompat.ACTION_CLICK, null));

        onView(isRoot())
                .perform(waitFor(100));

        // execute "activate" action commands
        onView(withComponent(getOuterFrame()))
                .check(hasBackgroundColor(Color.parseColor(PRESS_COLOR)));

        // it should not trigger "onPress" event handler
        verifyZeroInteractions(mSendEventCallback);
    }

    void verifyCancelBehavior(ViewAction moveAction, ViewAction cancelAction) {
        final Component touchableComponent = getTouchableComponent();

        // swipe but don't trigger cancel
        onView(withComponent(touchableComponent))
                .perform(moveAction);

        onView(withComponent(getOuterFrame()))
                .check(hasBackgroundColor(Color.parseColor(UP_COLOR)));

        // swipe to let intrinsic touch handler take over
        onView(withComponent(touchableComponent))
                .perform(cancelAction);

        onView(withComponent(getOuterFrame()))
                .check(hasBackgroundColor(Color.parseColor(CANCEL_COLOR)));
    }

    void verifyClick(ViewAction action) {
        onView(withComponent(getOuterFrame()))
                .check(hasBackgroundColor(Color.parseColor(INITIAL_OUTER_COLOR)));

        onView(withComponent(getTouchableComponent()))
                .perform(requestFocus())
                .perform(action);

        // Wait for key handling
        onView(isRoot())
                .perform(waitFor(100));

        String[] expectedArgument = new String[]{ARGUMENT_FIRED_1};
        verify(mSendEventCallback).onSendEvent(eq(expectedArgument), any(), any(), any());
    }

}
