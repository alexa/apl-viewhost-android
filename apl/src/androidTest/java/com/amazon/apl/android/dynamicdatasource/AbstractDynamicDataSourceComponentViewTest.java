/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.dynamicdatasource;

import android.util.Log;
import android.view.View;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;

import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.ViewAssertion;
import androidx.test.espresso.core.internal.deps.guava.base.Preconditions;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;

import com.amazon.apl.android.views.APLAbsoluteLayout;
import com.amazon.apl.android.APLController;
import com.amazon.apl.android.APLGradientDrawable;
import com.amazon.apl.android.APLLayout;
import com.amazon.apl.android.APLOptions;
import com.amazon.apl.android.APLTestContext;
import com.amazon.apl.android.RootConfig;
import com.amazon.apl.android.component.AbstractComponentViewTest;
import com.amazon.apl.android.Content;
import com.amazon.apl.android.RootContext;
import com.amazon.apl.android.TestActivity;
import com.amazon.apl.android.dependencies.IDataSourceFetchCallback;
import com.amazon.apl.enums.RootProperty;
import com.amazon.common.test.LeakRulesBaseClass;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;

import java.util.Objects;

import static androidx.test.espresso.action.ViewActions.actionWithAssertions;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayingAtLeast;

import static org.junit.Assert.assertNotNull;

/**
 * Like {@link AbstractComponentViewTest}, but for testing Components backed by a Dynamic DataSource
 */
abstract class AbstractDynamicDataSourceComponentViewTest extends LeakRulesBaseClass {


    // Load the APL library.
    static {
        APLController.initializeAPL(InstrumentationRegistry.getInstrumentation().getContext());
    }

    APLTestContext mTestContext;
    APLController mAplController;

    @After
    public void finishDocument() {
        try {
            activityRule.runOnUiThread(() -> mTestContext.getRootContext().finishDocument());
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    @Rule
    public ActivityTestRule<TestActivity> activityRule = new ActivityTestRule<>(TestActivity.class);

    private class InflateDynamicDataSourceViewAction implements ViewAction {
        private final String mDocument;
        private final String mData;
        private final IDataSourceFetchCallback mDataSourceFetchCallback;
        private final String mDynamicDataSourceType;

        InflateDynamicDataSourceViewAction(
                final String document,
                final String data,
                final IDataSourceFetchCallback dataSourceFetchCallback) {
            mDocument = document;
            mData = data;
            mDataSourceFetchCallback = dataSourceFetchCallback;
            String dynamicDataSourceType = null;
            try {
                final JSONObject dataObject = new JSONObject(mData);
                dynamicDataSourceType = dataObject.getString("type");
            } catch (JSONException e) {
                Log.e("InflateDynamicDataSourceViewAction", "JSON parsing error for data: " + data);
            }
            mDynamicDataSourceType = Objects.nonNull(dynamicDataSourceType) ?
                    dynamicDataSourceType : "dynamicIndexList";
        }

        @Override
        public Matcher<View> getConstraints() {
            Matcher<View> standardConstraint = isDisplayingAtLeast(90);
            return standardConstraint;
        }

        @Override
        public String getDescription() {
            return "Inflate a Document with an IDataSourceFetchCallback and dynamic datasource";
        }

        @Override
        public void perform(UiController uiController, View view) {
            final Content content;
            try {
                content = Content.create(mDocument, new Content.Callback() {
                    @Override
                    public void onDataRequest(Content content, String dataId) {
                        content.addData(dataId, mData);
                    }
                });
                final APLOptions options = APLOptions.builder()
                        .dataSourceFetchCallback(mDataSourceFetchCallback)
                        .build();
                final RootConfig testRootConfig = RootConfig.create("Unit Test", "1.0")
                        .registerDataSource(mDynamicDataSourceType)
                        .set(RootProperty.kTapOrScrollTimeout, 50) // Half of the Fast swipe speed.
                        .pagerChildCache(3)
                        .sequenceChildCache(3);
                mTestContext = new APLTestContext()
                        .setAplOptions(options)
                        .setDocument(mDocument)
                        .setContent(content)
                        .setRootConfig(testRootConfig)
                        .buildRootContextDependencies();
                APLLayout aplLayout = activityRule.getActivity().findViewById(com.amazon.apl.android.test.R.id.apl);
                try {
                    mAplController = APLController.renderDocument(content, mTestContext.getAplOptions(), mTestContext.getRootConfig(), aplLayout.getPresenter());
                } catch (APLController.APLException e) {
                    Assert.fail(e.getMessage());
                }
            } catch (Content.ContentException e) {
                Assert.fail(e.getMessage());
            }
        }
    }

    ViewAction updateDynamicDataSource(final String type, final String payload) {
        return actionWithAssertions(new UpdateDynamicDataSourceViewAction(
                type,
                payload));
    }

    private class UpdateDynamicDataSourceViewAction implements ViewAction {
        private final String mType;
        private final String mPayload;

        UpdateDynamicDataSourceViewAction(
                final String type,
                final String payload) {
            mType = type;
            mPayload = payload;
        }

        @Override
        public Matcher<View> getConstraints() {
            Matcher<View> standardConstraint = isDisplayingAtLeast(90);
            return standardConstraint;
        }

        @Override
        public String getDescription() {
            return "Updates a dynamic datasource";
        }

        @Override
        public void perform(UiController uiController, View view) {
            try {
                mAplController.updateDataSource(mType, mPayload);
            } catch (Exception e) {
                Assert.fail(e.getMessage());
            }
        }
    }

    ViewAction inflate(String document, String dataSource, IDataSourceFetchCallback dataSourceFetchCallback) {
        return actionWithAssertions(new InflateDynamicDataSourceViewAction(
                document,
                dataSource,
                dataSourceFetchCallback));
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

    ViewAssertion hasRootContext() {
        return new RootContextViewAssertion();
    }

    /**
     * A customized {@link Matcher} for testing that if a color matches the border color of
     * current view.
     *
     * @param borderColor A color int value.
     *
     * @return Match or not.
     */
    static Matcher<View> withBorderColor(final int borderColor) {
        return new TypeSafeMatcher<View>(APLAbsoluteLayout.class) {

            @Override
            public boolean matchesSafely(View view) {
                LayerDrawable drawable = (LayerDrawable) view.getBackground();
                return drawable != null && ((ShapeDrawable) drawable.getDrawable(0)).getPaint().getColor() == borderColor;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("with border color value: " + borderColor);
            }
        };
    }
}