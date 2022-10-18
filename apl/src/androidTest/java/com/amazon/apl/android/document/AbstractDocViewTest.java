/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.document;

import android.view.View;

import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.IdlingResource;
import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.ViewAssertion;
import androidx.test.espresso.core.internal.deps.guava.base.Preconditions;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;

import com.amazon.apl.android.APLController;
import com.amazon.apl.android.APLLayout;
import com.amazon.apl.android.APLOptions;
import com.amazon.apl.android.APLTestContext;
import com.amazon.apl.android.Component;
import com.amazon.apl.android.RootConfig;
import com.amazon.apl.android.RootContext;
import com.amazon.apl.android.TestActivity;

import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.mockito.MockitoAnnotations;

import static androidx.test.espresso.action.ViewActions.actionWithAssertions;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayingAtLeast;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;

/**
 * This base class serves as initial setup for Espresso Tests.
 *
 * In order to inflate the document, you should invoke it:
 *
 *         onView(withId(com.amazon.apl.android.test.R.id.apl))
 *                 .perform(inflate(myDocument))
 *                 .check(hasRootContext());
 *
 * where it will populate `APLTestContext` where it contains a reference
 * of the real RootContext of the application.
 */
public abstract class AbstractDocViewTest {

    // The document will reinflate on configuration change
    public static final String BASE_DOC_REINFLATE_SEND_EVENT_ARGUMENT = "reinflating the APL document";

    /**
     * The preserve property is used to specify an array of sequencer that should continue
     * to execute during and after reinflation.
     * onConfigChange handler with Reinflate command preserving
     * the named sequencer "MAGIC"
     */
    public static final String DOCUMENT_PROPERTIES ="\"onConfigChange\": [\n" +
            "      {\n" +
            "        \"type\": \"SendEvent\",\n" +
            "        \"sequencer\": \"ConfigSendEvent\",\n" +
            "        \"arguments\": [ \"" + BASE_DOC_REINFLATE_SEND_EVENT_ARGUMENT + "\" ]\n" +
            "      },\n" +
            "      {\n" +
            "        \"type\": \"Reinflate\",\n" +
            "        \"preservedSequencers\": [\"MAGIC\"]" +
            "      }\n" +
            "    ]\n";

    final static String BASE_DOC = "{" +
            "  \"type\": \"APL\"," +
            "  \"version\": \"1.4\"," +
            "  \"mainTemplate\": {" +
            "    \"parameters\": [\n" +
            "      \"%s\"\n" +
            "    ],\n" +
            "    \"item\":" +
            "    {" +
            "      \"id\": \"testcomp\" " +
            "      %s" +
            "    }" +
            "  }" +
            "  %s" +
            "}";

    // Load the APL library.
    static {
        APLController.initializeAPL(InstrumentationRegistry.getInstrumentation().getContext());
    }

    protected APLTestContext mTestContext;
    protected APLController mAplController;
    protected IdlingResource mIdlingResource;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @After
    public void teardown() {
        if (mIdlingResource != null) {
            IdlingRegistry.getInstance().unregister(mIdlingResource);
        }
    }


    @Rule
    public ActivityTestRule<TestActivity> activityRule = new ActivityTestRule<>(TestActivity.class);

    private class InflateAPLViewAction implements ViewAction {
        private final String mComponentProps;
        private final String mDocumentProps;
        private final String mPayloadId;
        private final String mData;
        private final APLOptions mOptions;
        private final RootConfig mRootConfig;
        private final String mDocument;

        InflateAPLViewAction(String document, String componentProps, String documentProps, String payloadId, String data, APLOptions options, RootConfig rootConfig) {
            mComponentProps = componentProps;
            mDocumentProps = documentProps;
            mPayloadId = payloadId;
            mData = data;
            mOptions = options;
            mRootConfig = rootConfig;
            mDocument = document;
        }

        InflateAPLViewAction(String componentProps, String documentProps, String payloadId, String data, APLOptions options, RootConfig rootConfig) {
            this(BASE_DOC, componentProps, documentProps, payloadId, data, options, rootConfig);
        }

        InflateAPLViewAction(String document, String payloadId, String data, APLOptions options, RootConfig rootConfig) {
            this(document, "", "", payloadId, data, options, rootConfig);
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
            mTestContext = new APLTestContext()
                    .setDocument(mDocument, mPayloadId, mComponentProps, mDocumentProps)
                    .setDocumentPayload(mPayloadId, mData)
                    .setAplOptions(mOptions)
                    .buildRootContextDependencies();

            if (mRootConfig != null) {
                mTestContext.setRootConfig(mRootConfig);
            }

            APLLayout aplLayout = activityRule.getActivity().findViewById(com.amazon.apl.android.test.R.id.apl);
            try {
                mAplController = APLController.renderDocument(mTestContext.getContent(), mTestContext.getAplOptions(), mTestContext.getRootConfig(), aplLayout.getPresenter());
            } catch (APLController.APLException e) {
                Assert.fail(e.getMessage());
            }
        }
    }

    public Matcher<View> withComponent(Component component) {
        return withId(is(component.getComponentId().hashCode()));
    }


    private class RootContextViewAssertion implements ViewAssertion {
        @Override
        public void check(View view, NoMatchingViewException noViewFoundException) {
            Preconditions.checkNotNull(view);
            APLLayout layout = (APLLayout) view;
            RootContext rc = layout.getAPLContext();
            // Store the RC configuration made by this test
            mTestContext.setRootContext(rc);
            mTestContext.setPresenter(layout.getPresenter());
            assertNotNull("RootContext create failed", mTestContext.getRootContext());
        }
    }

    public ViewAction inflate(String componentProps, String documentProps) {
        return inflateWithOptions(componentProps, documentProps, null);
    }

    public ViewAction inflateWithOptions(String document, APLOptions options) {
        return actionWithAssertions(new InflateAPLViewAction(document, "payload", "{}", options, null));
    }

    public ViewAction inflateWithOptions(String componentProps, String documentProps, APLOptions options) {
        return inflate(componentProps, documentProps, "payload", "{}", options);
    }

    public ViewAction inflateWithOptions(String componentProps, String documentProps, APLOptions options, RootConfig rootConfig) {
        return inflate(componentProps, documentProps, "payload", "{}", options, rootConfig);
    }

    public ViewAction inflate(String componentProps, String documentProps, String payloadId, String data, APLOptions options) {
        return inflate(componentProps, documentProps, payloadId, data, options, null);
    }

    public ViewAction inflate(String componentProps, String documentProps, String payloadId, String data, APLOptions options, RootConfig rootConfig) {
        if (componentProps.length() > 0) {
            componentProps =  ", " + componentProps;
        }
        if (documentProps.length() > 0) {
            documentProps =  ", " + documentProps;
        }

        return actionWithAssertions(new InflateAPLViewAction(componentProps, documentProps, payloadId, data, options, rootConfig));
    }


    public ViewAssertion hasRootContext() {
        return new RootContextViewAssertion();
    }
}