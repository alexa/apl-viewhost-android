/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.scenegraph.accessibility;

import static com.amazon.apl.enums.Role.kRoleSearch;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doAnswer;

import com.amazon.apl.android.APLOptions;
import com.amazon.apl.android.RenderingContext;
import com.amazon.apl.android.document.AbstractDocUnitTest;
import com.amazon.apl.android.primitive.AccessibilityActions;
import com.amazon.apl.android.scenegraph.APLLayer;
import com.amazon.apl.android.scenegraph.APLScenegraph;
import com.amazon.apl.android.utils.TestClock;
import com.amazon.apl.android.views.APLView;

import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;

public class AccessibilityTest extends AbstractDocUnitTest {
    private final String APL_DOC_TEXT = "{\n" +
            "  \"type\": \"APL\",\n" +
            "  \"version\": \"1.4\",\n" +
            "  \"mainTemplate\": {\n" +
            "    \"items\": [\n" +
            "      {\n" +
            "        \"type\": \"Container\",\n" +
            "        \"height\": \"100vh\",\n" +
            "        \"alignItems\": \"center\",\n" +
            "        \"items\": [\n" +
            "          {\n" +
            "            \"type\": \"Text\",\n" +
            "            \"id\": \"textComponent\",\n" +
            "            \"width\": \"30%\",\n" +
            "            \"fontSize\": \"60dp\",\n" +
            "            \"textAlign\": \"center\",\n" +
            "            \"style\": \"textStylePrimary2\",\n" +
            "            \"text\": \"accessibilityLabel + role\",\n" +
            "            \"accessibilityLabel\": \"Accessibility label 2\",\n" +
            "            \"role\": \"search\",\n" +
            "            \"paddingTop\": \"30dp\",\n" +
            "            \"actions\": [\n" +
            "               {\n" +
            "                 \"name\": \"activate\",\n" +
            "                 \"label\": \"message to server\"\n" +
            "               },\n" +
            "               {\n" +
            "                 \"name\": \"doubletap\",\n" +
            "                 \"label\": \"Play double tap message\"\n" +
            "               }\n" +
            "             ]" +
            "          }\n" +
            "        ]\n" +
            "      }\n" +
            "    ]\n" +
            "  }\n" +
            "}";

    private final String APL_DOC_TOUCHWRAPPER = "{\n" +
            "  \"type\": \"APL\",\n" +
            "  \"version\": \"2023.1\",\n" +
            "  \"theme\": \"dark\",\n" +
            "  \"mainTemplate\": {\n" +
            "    \"items\": [\n" +
            "      {\n" +
            "        \"type\": \"TouchWrapper\",\n" +
            "        \"id\": \"touchWrapper\",\n" +
            "        \"disabled\": true,\n" +
            "        \"checked\": false,\n" +
            "        \"item\": {\n" +
            "          \"inheritParentState\": true,\n" +
            "          \"type\": \"Text\",\n" +
            "          \"text\": \"Hello ${disabled}\"\n" +
            "        }\n" +
            "      }\n" +
            "    ]\n" +
            "  }\n" +
            "}";

    private final String APL_DOC_CONTAINER = "{\n" +
            "  \"type\": \"APL\",\n" +
            "  \"version\": \"2023.1\",\n" +
            "  \"theme\": \"dark\",\n" +
            "  \"mainTemplate\": {\n" +
            "    \"items\": [\n" +
            "      {\n" +
            "        \"type\": \"Container\",\n" +
            "        \"id\": \"container\",\n" +
            "        \"disabled\": true,\n" +
            "        \"checked\": false,\n" +
            "        \"item\": {\n" +
            "          \"inheritParentState\": true,\n" +
            "          \"type\": \"Text\",\n" +
            "          \"text\": \"Hello ${disabled}\"\n" +
            "        }\n" +
            "      }\n" +
            "    ]\n" +
            "  }\n" +
            "}";

    private final String APL_DOC_SCROLLVIEW = "{\n" +
            "  \"type\": \"APL\",\n" +
            "  \"version\": \"2023.1\",\n" +
            "  \"theme\": \"dark\",\n" +
            "  \"mainTemplate\": {\n" +
            "    \"items\": [\n" +
            "      {\n" +
            "        \"type\": \"ScrollView\",\n" +
            "        \"id\": \"scrollView\",\n" +
            "        \"disabled\": true,\n" +
            "        \"checked\": false,\n" +
            "        \"item\": {\n" +
            "          \"inheritParentState\": true,\n" +
            "          \"type\": \"Text\",\n" +
            "          \"text\": \"Hello ${disabled}\"\n" +
            "        }\n" +
            "      }\n" +
            "    ]\n" +
            "  }\n" +
            "}";

    @Mock
    private RenderingContext mockRenderingContext;

    @Override
    public void initChoreographer() {
        super.initChoreographer();
        mOptions = APLOptions.builder()
                .aplClockProvider(callback -> new TestClock(callback))
                .scenegraphEnabled(true)
                .build();
    }

    @Ignore
    @Test
    public void testAccessibilityLabel_is_correctly_fetched_and_updated() {
        loadDocument(APL_DOC_TEXT);
        APLView childView = getViewForTextLayer();
        assertEquals("Accessibility label 2", childView.getContentDescription());
        // Try updating the accessibility label
        String command = "{\n" +
                "             \"type\": \"SetValue\",\n" +
                "             \"property\": \"accessibilityLabel\",\n" +
                "             \"value\": \"Some other accessibility label\",\n" +
                "             \"componentId\": \"textComponent\"\n" +
                "          }";
        // Configure the mock presenter to call for scenegraph updates
        doAnswer(invocation -> {
            new APLScenegraph(mRootContext).applyUpdates();
            return null;
        }).when(mAPLPresenter).inflateScenegraph();
        mRootContext.executeCommands("[" + command + "]");
        // Tick
        update(1);
        assertEquals("Some other accessibility label", childView.getContentDescription());
    }

    @Ignore
    @Test
    public void testAccessibilityRole() {
        loadDocument(APL_DOC_TEXT);
        APLView childView = getViewForTextLayer();
        assertEquals(kRoleSearch, childView.mAplLayer.getAccessibility().getRole());
    }

    //@Test
    public void testAccessibilityActions() {
        loadDocument(APL_DOC_TEXT);
        APLView childView = getViewForTextLayer();
        AccessibilityActions actions = childView.mAplLayer.getAccessibility().getAccessibilityActions();
        assertEquals(2, actions.size());
        for (int i = 0; i < actions.size(); i++) {
            if ("activate".equals(actions.at(i).name())) {
                assertEquals("message to server", actions.at(i).label());
            }
            if ("doubleTap".equals(actions.at(i).name())) {
                assertEquals("Play double tap message", actions.at(i).label());
            }
        }
    }

    @Ignore
    @Test
    public void testDisabled_is_correctly_fetched_and_updated() {
        loadDocument(APL_DOC_TOUCHWRAPPER);
        APLView childView = getTopLayerView();
        assertTrue(childView.mAplLayer.getAccessibility().isDisabled());
        // Try updating the accessibility label
        String command = "{\n" +
                "             \"type\": \"SetValue\",\n" +
                "             \"property\": \"disabled\",\n" +
                "             \"value\": false,\n" +
                "             \"componentId\": \"touchWrapper\"\n" +
                "          }";
        // Configure the mock presenter to call for scenegraph updates
        doAnswer(invocation -> {
            new APLScenegraph(mRootContext).applyUpdates();
            return null;
        }).when(mAPLPresenter).inflateScenegraph();
        mRootContext.executeCommands("[" + command + "]");
        // Tick
        update(1);
        assertFalse(childView.mAplLayer.getAccessibility().isDisabled());
    }

    @Ignore
    @Test
    public void testChecked_is_correctly_fetched_and_updated() {
        loadDocument(APL_DOC_TOUCHWRAPPER);
        APLView childView = getTopLayerView();
        assertFalse(childView.mAplLayer.getAccessibility().isChecked());
        // Try updating the accessibility label
        String command = "{\n" +
                "             \"type\": \"SetValue\",\n" +
                "             \"property\": \"checked\",\n" +
                "             \"value\": true,\n" +
                "             \"componentId\": \"touchWrapper\"\n" +
                "          }";
        // Configure the mock presenter to call for scenegraph updates
        doAnswer(invocation -> {
            new APLScenegraph(mRootContext).applyUpdates();
            return null;
        }).when(mAPLPresenter).inflateScenegraph();
        mRootContext.executeCommands("[" + command + "]");
        // Tick
        update(1);
        assertTrue(childView.mAplLayer.getAccessibility().isChecked());
    }

    @Ignore
    @Test
    public void testScrollable_is_false_for_Container_component() {
        loadDocument(APL_DOC_CONTAINER);
        APLView childView = getTopLayerView();
        assertFalse(childView.mAplLayer.getAccessibility().isScrollable());
    }

    @Ignore
    @Test
    public void testScrollable_is_true_for_ScrollView_component() {
        loadDocument(APL_DOC_SCROLLVIEW);
        APLView childView = getTopLayerView();
        assertTrue(childView.mAplLayer.getAccessibility().isScrollable());
    }

    private APLView getViewForTextLayer() {
        APLView topView = getTopLayerView();
        assertEquals(1, topView.getChildCount());
        APLView childView = (APLView)topView.getChildAt(0);
        return childView;
    }

    private APLView getTopLayerView() {
        APLScenegraph aplScenegraph = new APLScenegraph(mRootContext);
        APLLayer layer = APLLayer.ensure(aplScenegraph.getTop(), mockRenderingContext);
        APLView aplView = new APLView(getApplication(), layer);
        layer.attachView(aplView);
        APLView childView = (APLView) layer.mChildView;
        return childView;
    }
}
