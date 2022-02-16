/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.document;

import android.graphics.Color;
import android.graphics.Paint;
import android.text.Layout;
import android.view.View;

import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;

import com.amazon.apl.android.APLGradientDrawable;
import com.amazon.apl.android.APLLayout;
import com.amazon.apl.android.APLLayoutParams;
import com.amazon.apl.android.RootConfig;
import com.amazon.apl.android.Text;
import com.amazon.apl.android.espresso.APLViewIdlingResource;
import com.amazon.apl.android.primitive.StyledText;
import com.amazon.apl.android.scaling.Scaling;
import com.amazon.apl.android.views.APLAbsoluteLayout;
import com.amazon.apl.android.views.APLTextView;
import com.amazon.apl.enums.ViewportMode;

import org.hamcrest.Matcher;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static com.amazon.apl.android.espresso.APLViewActions.performConfigurationChange;
import static com.amazon.apl.android.espresso.APLViewActions.resizeApl;
import static org.junit.Assert.assertEquals;

public class ConfigChangeTest extends AbstractDocViewTest {
    private static final String DOC = "\"type\": \"Frame\",\n" +
            "        \"borderRadius\": 40,\n" +
            "        \"borderWidth\": 10,\n" +
            "        \"borderColor\": \"blue\",\n" +
            "        \"backgroundColor\": \"blue\",\n" +
            "        \"width\":\"50vw\",\n" +
            "        \"height\": \"50vh\"\n";

    private static final String DOCUMENT_PROPERTIES = "\"onConfigChange\": [" +
            "    { \"type\": \"SetValue\", \"componentId\": \"testcomp\", \"property\": \"borderColor\", \"value\": \"red\" }" +
            "  ]";

    /**
     * Checks that SetValue command to change a dynamic property of Frame component executes on a configuration change
     */
    @Test
    public void testHandleConfigurationChange() {
        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(inflate(DOC, DOCUMENT_PROPERTIES))
                .check(hasRootContext());

        APLAbsoluteLayout view = mTestContext.getTestView();
        APLGradientDrawable drawable = (APLGradientDrawable) view.getBackground();
        assertEquals(Color.BLUE, drawable.getBorderColor());
        // Trigger a configuration change.
        onView(isRoot()).perform(performConfigurationChange(mTestContext.getRootContext(), mTestContext.getRootContext().createConfigurationChange().fontScale(2.0f).build()));
        drawable = (APLGradientDrawable) view.getBackground();
        assertEquals(Color.RED, drawable.getBorderColor());
    }

    private static final String ENV_DOC = "\"text\": \"Motion State: ${environment.motionState}\",\n" +
            "\"id\": \"movingState\",\n" +
            "\"fontSize\": \"48dp\",\n" +
            "\"type\": \"Text\"\n";

    private static final String ENV_DOC_PROPERTIES = "\"onConfigChange\": [\n" +
            "    {\n" +
            "      \"type\": \"Reinflate\"\n" +
            "    }\n" +
            "  ]\n";


    @Test
    public void testHandleConfigurationChangeWithUpdatedEnvironment() {
        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(inflate(ENV_DOC, ENV_DOC_PROPERTIES, "payload", "{}", null, RootConfig.create().setEnvironmentValue("motionState", "PARKED")))
                .check(hasRootContext());

        // Trigger a configuration change.
        onView(isRoot()).perform(performConfigurationChange(mTestContext.getRootContext(),
                mTestContext.getRootContext().createConfigurationChange()
                        .environmentValue("motionState", "MOVING").build()));

        Text text = (Text) mTestContext.getRootContext().getTopComponent();
        StyledText styled = text.getProxy().getStyledText();
        String unprocessed = styled.getUnprocessedText();
        assertEquals("Motion State: MOVING", unprocessed);
    }

    private static final String RESIZING_FRAME_DOC =
            "      \n" +
            "        \"type\": \"Frame\",\n" +
            "        \"id\": \"frame\",\n" +
            "        \"width\": \"100%\",\n" +
            "        \"height\": \"100%\",\n" +
            "        \"backgroundColor\": \"red\"\n" +
            "      ";

    @Test
    public void testConfigChange_resizesViews() {
        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(inflate(RESIZING_FRAME_DOC, ""))
                .check(hasRootContext());

        View testView = mTestContext.getTestView();
        final int initialWidth = testView.getWidth();
        final int initialHeight = testView.getHeight();

        mIdlingResource = new APLViewIdlingResource(testView);
        IdlingRegistry.getInstance().register(mIdlingResource);

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
                        APLLayout aplLayout = (APLLayout) view;
                        aplLayout.setHandleConfigurationChangeOnSizeChanged(true);
                        aplLayout.setLayoutParams(new APLLayoutParams(view.getWidth() / 2, view.getHeight() / 2, 0, 0));
                    }
                });

        onView(withComponent(mTestContext.getTestComponent()))
                .check((View view, NoMatchingViewException noViewFoundException) -> {
                    assertEquals(initialWidth / 2, view.getWidth());
                    assertEquals(initialHeight / 2, view.getHeight());
                });
    }

    private static final String RESIZING_TEXT_WITH_SCALING_DOC =
            "\n" +
            "\"type\": \"Frame\",\n" +
            "        \"height\": \"100%\",\n" +
            "        \"width\": \"100%\",\n" +
            "        \"backgroundColor\": \"red\",\n" +
            "        \"items\": [\n" +
            "          {\n" +
            "            \"type\": \"Text\",\n" +
            "            \"id\": \"myText\",\n" +
            "            \"fontSize\": 24,\n" +
            "            \"text\": \"My text\"\n" +
            "          }\n" +
            "        ]\n";

    @Test
    public void testConfigChange_withScaling_reSizesTextToScaledViewport() {
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
                        APLLayout aplLayout = (APLLayout) view;
                        aplLayout.setHandleConfigurationChangeOnSizeChanged(true);
                        // Only one viewport to choose from 200x100 rectangle
                        Scaling.ViewportSpecification[] specsArray = {
                                Scaling.ViewportSpecification.builder()
                                        .minWidth(200)
                                        .maxWidth(200)
                                        .minHeight(100)
                                        .maxHeight(100)
                                        .round(false)
                                        .mode(ViewportMode.kViewportModeHub)
                                        .build()
                        };

                        List<Scaling.ViewportSpecification> specs = Arrays.asList(specsArray);
                        Scaling scaling = new Scaling(1.0, specs);
                        aplLayout.setScaling(scaling);
                        aplLayout.setLayoutParams(new APLLayoutParams(200, 400, 0, 0));
                    }
                });

        // APL layout is a 200x400 box
        // |-----|
        // |     |
        // |     |
        // |     |
        // |     |
        // |-----|
        // but the viewport specifications say that we only work with 200x100 so we have a scaled viewport
        // like:
        // |-----|
        // |     |
        // |#####|
        // |#####|
        // |     |
        // |-----|
        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(inflate(RESIZING_TEXT_WITH_SCALING_DOC, ""))
                .check(hasRootContext());

        // Frame should fill scaled viewport 200x100 and should be positioned center so translated
        // down 150 px
        onView(withComponent(mTestContext.getTestComponent()))
                .check((View view, NoMatchingViewException exception) -> {
                    assertEquals(0, view.getLeft());
                    assertEquals(150, view.getTop());
                    assertEquals(200, view.getRight());
                    assertEquals(250, view.getBottom());
                });

        onView(withComponent(mTestContext.getRootContext().findComponentById("myText")))
                .check((View view, NoMatchingViewException exception) -> {
                    APLTextView aplTextView = (APLTextView) view;
                    Layout layout = aplTextView.getLayout();
                    Paint paint = layout.getPaint();

                    // Check the paint has the right text size
                    assertEquals(layout.getWidth(), view.getWidth());
                    assertEquals(24f, paint.getTextSize(), 0.01f);
                });

        // APL layout is now a 400x200 box
        // |------------|
        // |            |
        // |            |
        // |------------|
        // but the viewport specifications say that we work with 200x100 so we scale it to fill.
        // |------------|
        // |############|
        // |############|
        // |------------|
        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(resizeApl(true, 400, 200));

        onView(withComponent(mTestContext.getTestComponent()))
                .check((View view, NoMatchingViewException exception) -> {
                    assertEquals(0, view.getLeft());
                    assertEquals(0, view.getTop());
                    assertEquals(400, view.getRight());
                    assertEquals(200, view.getBottom());
                });

        onView(withComponent(mTestContext.getRootContext().findComponentById("myText")))
                .check((View view, NoMatchingViewException exception) -> {
                    APLTextView aplTextView = (APLTextView) view;
                    Layout layout = aplTextView.getLayout();
                    Paint paint = layout.getPaint();

                    // Text size is doubled since we are scaling the 200x100 template to fill
                    assertEquals(layout.getWidth(), view.getWidth());
                    assertEquals(48f, paint.getTextSize(), 0.01f);
                });

        // Switch back to 200x400:
        // |-----|
        // |     |
        // |#####|
        // |#####|
        // |     |
        // |-----|
        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(resizeApl(true, 200, 400));


        // Check that we're back to 200x100 frame
        onView(withComponent(mTestContext.getTestComponent()))
                .check((View view, NoMatchingViewException exception) -> {
                    assertEquals(0, view.getLeft());
                    assertEquals(150, view.getTop());
                    assertEquals(200, view.getRight());
                    assertEquals(250, view.getBottom());
                });

        // Check the Text has changed back
        onView(withComponent(mTestContext.getRootContext().findComponentById("myText")))
                .check((View view, NoMatchingViewException exception) -> {
                    APLTextView aplTextView = (APLTextView) view;
                    Layout layout = aplTextView.getLayout();
                    Paint paint = layout.getPaint();

                    // Check the paint has the right text size
                    assertEquals(layout.getWidth(), view.getWidth());
                    assertEquals(24f, paint.getTextSize(), 0.01f);
                });

    }
}
