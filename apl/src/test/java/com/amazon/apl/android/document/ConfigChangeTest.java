/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.document;

import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.text.Layout;
import android.view.View;

import com.amazon.apl.android.APLLayout;
import com.amazon.apl.android.APLLayoutParams;
import com.amazon.apl.android.APLOptions;
import com.amazon.apl.android.RootConfig;
import com.amazon.apl.android.Text;
import com.amazon.apl.android.primitive.StyledText;
import com.amazon.apl.android.scaling.Scaling;
import com.amazon.apl.android.views.APLAbsoluteLayout;
import com.amazon.apl.android.views.APLTextView;
import com.amazon.apl.enums.ViewportMode;

import org.junit.Test;
import org.robolectric.shadows.ShadowLooper;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

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

    private ShapeDrawable getBorder() {
        return (ShapeDrawable) ((LayerDrawable)mTestContext.getTestView().getBackground()).getDrawable(0);
    }

    /**
     * Checks that SetValue command to change a dynamic property of Frame component executes on a configuration change
     */
    @Test
    public void testHandleConfigurationChange() {
        inflate(DOC, DOCUMENT_PROPERTIES);

        assertEquals(Color.BLUE, getBorder().getPaint().getColor());

        // Trigger a configuration change.
        mTestContext.getRootContext().handleConfigurationChange(mTestContext.getRootContext().createConfigurationChange().fontScale(2.0f).build());
        testClock.doFrameUpdate(100);

        assertEquals(Color.RED, getBorder().getPaint().getColor());
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
        inflateWithOptions(ENV_DOC, ENV_DOC_PROPERTIES, APLOptions.builder(), RootConfig.create().setEnvironmentValue("motionState", "PARKED"));

        // Trigger a configuration change.
        mTestContext.getRootContext().handleConfigurationChange(mTestContext.getRootContext().createConfigurationChange()
                        .environmentValue("motionState", "MOVING").build());
        testClock.doFrameUpdate(100);

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
        inflate(RESIZING_FRAME_DOC, "");

        View testView = mTestContext.getTestView();
        final int initialWidth = testView.getWidth();
        final int initialHeight = testView.getHeight();

        APLAbsoluteLayout aplLayout = ((APLAbsoluteLayout)testView);
        aplLayout.setLayoutParams(new APLLayoutParams(testView.getWidth() / 2, testView.getHeight() / 2, 0, 0));

        ShadowLooper.idleMainLooper();

        assertEquals(initialWidth / 2, testView.getWidth());
        assertEquals(initialHeight / 2, testView.getHeight());
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
        activityRule.getScenario().onActivity(activity -> {
            APLLayout aplLayout = activity.findViewById(com.amazon.apl.android.test.R.id.apl);
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
        inflate(RESIZING_TEXT_WITH_SCALING_DOC, "");

        // Frame should fill scaled viewport 200x100 and should be positioned center so translated
        // down 150 px
        activityRule.getScenario().onActivity(activity -> {
            View view = activity.findViewById(mTestContext.getTestComponent().getComponentId().hashCode()) ;
            assertEquals(0, view.getLeft());
            assertEquals(150, view.getTop());
            assertEquals(200, view.getRight());
            assertEquals(250, view.getBottom());

            APLTextView textView = activity.findViewById(mTestContext.getRootContext().findComponentById("myText").getComponentId().hashCode());
            Layout layout = textView.getLayout();
            Paint paint = layout.getPaint();

            // Check the paint has the right text size
            assertEquals(7, layout.getWidth());
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
        activityRule.getScenario().onActivity(activity -> {
            APLLayout aplLayout = activity.findViewById(com.amazon.apl.android.test.R.id.apl);
            aplLayout.setHandleConfigurationChangeOnSizeChanged(true);
            aplLayout.setLayoutParams(new APLLayoutParams(400, 200, 0, 0));
        });

        activityRule.getScenario().onActivity(activity -> {
            View view = activity.findViewById(mTestContext.getTestComponent().getComponentId().hashCode()) ;
            assertEquals(0, view.getLeft());
            assertEquals(0, view.getTop());
            assertEquals(400, view.getRight());
            assertEquals(200, view.getBottom());

            APLTextView textView = activity.findViewById(mTestContext.getRootContext().findComponentById("myText").getComponentId().hashCode());
            Layout layout = textView.getLayout();
            Paint paint = layout.getPaint();

            // Check the paint has the right text size
            assertEquals(14, layout.getWidth());
            assertEquals(48f, paint.getTextSize(), 0.01f);
        });



        // Switch back to 200x400:
        // |-----|
        // |     |
        // |#####|
        // |#####|
        // |     |
        // |-----|
        activityRule.getScenario().onActivity(activity -> {
            APLLayout aplLayout = activity.findViewById(com.amazon.apl.android.test.R.id.apl);
            aplLayout.setHandleConfigurationChangeOnSizeChanged(true);
            aplLayout.setLayoutParams(new APLLayoutParams(200, 400, 0, 0));
        });

        activityRule.getScenario().onActivity(activity -> {
            View view = activity.findViewById(mTestContext.getTestComponent().getComponentId().hashCode()) ;
            assertEquals(0, view.getLeft());
            assertEquals(150, view.getTop());
            assertEquals(200, view.getRight());
            assertEquals(250, view.getBottom());

            APLTextView textView = activity.findViewById(mTestContext.getRootContext().findComponentById("myText").getComponentId().hashCode());
            Layout layout = textView.getLayout();
            Paint paint = layout.getPaint();

            // Check the paint has the right text size
            assertEquals(7, layout.getWidth());
            assertEquals(24f, paint.getTextSize(), 0.01f);
        });
    }
}
