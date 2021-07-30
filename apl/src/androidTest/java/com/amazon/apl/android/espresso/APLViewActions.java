/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.espresso;

import android.view.MotionEvent;
import android.view.View;

import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.action.ViewActions;

import com.amazon.apl.android.APLController;
import com.amazon.apl.android.Component;
import com.amazon.apl.android.DocumentState;
import com.amazon.apl.android.IAPLViewPresenter;
import com.amazon.apl.android.RootContext;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.junit.Assert;

import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayingAtLeast;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

public final class APLViewActions {

    private APLViewActions() {
    }

    public static ViewAction requestFocus() {
        return new RequestFocus();
    }

    public static ViewAction clearFocus() {
        return new ClearFocus();
    }

    public static ViewAction waitFor(long millis) { return new WaitFor(millis); }

    public static ViewAction executeCommands(RootContext rootContext, String command) { return new ExecuteCommandViewAction(rootContext, command); }

    public static ViewAction restore(IAPLViewPresenter presenter, DocumentState documentState) { return ViewActions.actionWithAssertions(new RestoreViewAction(presenter, documentState)); }

    public static ViewAction finish(APLController controller) { return ViewActions.actionWithAssertions(new FinishAPLViewAction(controller)); }

    public static ViewAction applyProperties(IAPLViewPresenter presenter, Component component) { return new ApplyPropertiesViewAction(presenter, component); }

    public static ViewAction motionEvent(IAPLViewPresenter presenter, int action, float x, float y) { return new MotionEventViewAction(presenter, action, x, y); }

    public static ViewAction performClick() { return new AccessibilityClick(); }

    public static ViewAction performConfigurationChange(RootContext rootContext) {
        return new ConfigurationChangeEvent(rootContext);
    }

    static class RequestFocus implements ViewAction {

        @Override
        public Matcher<View> getConstraints() {
            return CoreMatchers.any(View.class);
        }

        @Override
        public String getDescription() {
            return "Request focus on the given view";
        }

        @Override
        public void perform(UiController uiController, View view) {
            view.requestFocus();
            uiController.loopMainThreadUntilIdle();
        }
    }

    static class ClearFocus implements ViewAction {

        @Override
        public Matcher<View> getConstraints() {
            return CoreMatchers.any(View.class);
        }

        @Override
        public String getDescription() {
            return "Clear focus on the given view";
        }

        @Override
        public void perform(UiController uiController, View view) {
            view.clearFocus();
            uiController.loopMainThreadUntilIdle();
        }
    }

    /**
     * Perform action of waiting for a specific time.
     */
    static class WaitFor implements ViewAction {
        private final long millis;

        public WaitFor(final long millis) {
            this.millis = millis;
        }

        @Override
        public Matcher<View> getConstraints() {
            return isRoot();
        }

        @Override
        public String getDescription() {
            return "Wait for " + millis + " milliseconds.";
        }

        @Override
        public void perform(UiController uiController, final View view) {
            uiController.loopMainThreadForAtLeast(millis);
        }
    }

    static class RestoreViewAction implements ViewAction {
        private IAPLViewPresenter mPresenter;
        private DocumentState mDocumentState;

        public RestoreViewAction(IAPLViewPresenter presenter, DocumentState documentState) {
            mPresenter = presenter;
            mDocumentState = documentState;
        }

        @Override
        public Matcher<View> getConstraints() {
            Matcher<View> standardConstraint = isDisplayingAtLeast(90);
            return standardConstraint;
        }

        @Override
        public String getDescription() {
            return "Restore a Document";
        }

        @Override
        public void perform(UiController uiController, View view) {
            try {
                APLController.restoreDocument(mDocumentState, mPresenter);
            } catch (APLController.APLException e) {
                Assert.fail(e.getMessage());
            }
        }
    }

    static class FinishAPLViewAction implements ViewAction {
        private final APLController mAplController;

        public FinishAPLViewAction(APLController controller) {
            mAplController = controller;
        }

        @Override
        public Matcher<View> getConstraints() {
            return isDisplayingAtLeast(90);
        }

        @Override
        public String getDescription() {
            return "Finish a document";
        }

        @Override
        public void perform(UiController uiController, View view) {
            mAplController.finishDocument();
        }
    }

    static class ExecuteCommandViewAction implements ViewAction {
        private final RootContext mRootContext;
        private final String mCommand;

        public ExecuteCommandViewAction(RootContext rootContext, String command) {
            mRootContext = rootContext;
            mCommand = command;
        }

        @Override
        public Matcher<View> getConstraints() {
            return isDisplayingAtLeast(90);
        }

        @Override
        public String getDescription() {
            return "Execute command";
        }

        @Override
        public void perform(UiController uiController, View view) {
            mRootContext.executeCommands(mCommand);
            mRootContext.doFrame(1);
        }
    }

    static class ApplyPropertiesViewAction implements ViewAction {
        private final IAPLViewPresenter mPresenter;
        private final Component mComponent;

        public ApplyPropertiesViewAction(IAPLViewPresenter presenter, Component component) {
            mPresenter = presenter;
            mComponent = component;
        }

        @Override
        public Matcher<View> getConstraints() {
            return withId(mComponent.getUniqueId().hashCode());
        }

        @Override
        public String getDescription() {
            return "ApplyProperties to: " + mComponent;
        }

        @Override
        public void perform(UiController uiController, View view) {
            mPresenter.applyAllProperties(mComponent, view);
        }
    }

    static class MotionEventViewAction implements ViewAction {
        final IAPLViewPresenter mPresenter;
        final int mMotionEventAction;
        final float mX, mY;

        public MotionEventViewAction(IAPLViewPresenter presenter, int action, float x, float y) {
            mPresenter = presenter;
            mMotionEventAction = action;
            mX = x;
            mY = y;
        }

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
            mPresenter.handleTouchEvent(MotionEvent.obtain(100, System.currentTimeMillis(), mMotionEventAction, mX, mY, 0));
            uiController.loopMainThreadForAtLeast(100);
        }
    }

    static class AccessibilityClick implements ViewAction {
        @Override
        public Matcher<View> getConstraints() {
            return isDisplayed();
        }

        @Override
        public String getDescription() {
            return "perform an accessibility click";
        }

        @Override
        public void perform(UiController uiController, View view) {
            view.performClick();
        }
    }

    static class ConfigurationChangeEvent implements ViewAction {

        private final RootContext mRootContext;

        public ConfigurationChangeEvent(RootContext rootContext) {
            mRootContext = rootContext;
        }
        @Override
        public Matcher<View> getConstraints() {
            return isDisplayed();
        }

        @Override
        public String getDescription() {
            return "perform a configuration change";
        }

        @Override
        public void perform(UiController uiController, View view) {
            mRootContext.handleConfigurationChange(mRootContext.createConfigurationChange().fontScale(2.0f).build());
            mRootContext.doFrame(1);
        }
    }
}