/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.scenegraph.layers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.view.View;

import androidx.test.core.app.ApplicationProvider;

import com.amazon.apl.android.APLOptions;
import com.amazon.apl.android.RenderingContext;
import com.amazon.apl.android.RootConfig;
import com.amazon.apl.android.dependencies.IMediaPlayer;
import com.amazon.apl.android.document.AbstractDocUnitTest;
import com.amazon.apl.android.media.RuntimeMediaPlayerFactory;
import com.amazon.apl.android.providers.AbstractMediaPlayerProvider;
import com.amazon.apl.android.scenegraph.APLLayer;
import com.amazon.apl.android.scenegraph.APLScenegraph;
import com.amazon.apl.android.scenegraph.media.APLVideoLayer;
import com.amazon.apl.android.utils.TestClock;
import com.amazon.apl.android.views.APLView;

import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;

/**
 * Test class to test the Layer and View creation for non APLContentLayer classes
 */
public class APLLayerTest extends AbstractDocUnitTest {

    // Video layer
    private static final String APL_VIDEO = "{\n" +
            "  \"type\": \"APL\",\n" +
            "  \"version\": \"1.0\",\n" +
            "  \"theme\": \"auto\",\n" +
            "  \"mainTemplate\": {\n" +
            "    \"items\": [\n" +
            "      {\n" +
            "        \"type\": \"Video\",\n" +
            "        \"height\": \"100%\",\n" +
            "        \"width\": \"100%\",\n" +
            "        \"id\": \"VideoPlayer\",\n" +
            "        \"source\": [\n" +
            "          {\n" +
            "            \"description\": \"The first video clip to play\",\n" +
            "            \"repeatCount\": 0,\n" +
            "            \"url\": \"dummy-url-1\"\n" +
            "          },\n" +
            "          {\n" +
            "            \"description\": \"The second video clip to play\",\n" +
            "            \"url\": \"dummy-url-2\",\n" +
            "            \"repeatCount\": -1\n" +
            "          },\n" +
            "          {\n" +
            "            \"description\": \"This video clip will only be reached by a command\",\n" +
            "            \"url\": \"dummy-url-3\",\n" +
            "            \"repeatCount\": 2\n" +
            "          }\n" +
            "        ]\n" +
            "      }\n" +
            "    ]\n" +
            "  }\n" +
            "}";

    // TODO: Add layer level tests for all APL components.

    @Mock
    private RenderingContext mMockRenderingContext;
    @Mock
    private IMediaPlayer mMockMediaPlayer;
    @Mock
    private AbstractMediaPlayerProvider<View> mMockMediaPlayerProvider;
    @Mock
    private View mMockVideoView;
    @Mock
    private APLView mMockAPLView;

    @Override
    public void initChoreographer() {
        super.initChoreographer();
        when(mMockVideoView.getContext()).thenReturn(ApplicationProvider.getApplicationContext());
        when(mMockAPLView.getContext()).thenReturn(ApplicationProvider.getApplicationContext());
        when(mMockMediaPlayerProvider.createView(any(Context.class))).thenReturn(mMockVideoView);
        when(mMockMediaPlayerProvider.getNewPlayer(any(Context.class), any(View.class))).thenReturn(mMockMediaPlayer);
        when(mMockRenderingContext.getMediaPlayerProvider()).thenReturn(mMockMediaPlayerProvider);
        mOptions = APLOptions.builder()
                .aplClockProvider(callback -> new TestClock(callback))
                .scenegraphEnabled(true)
                .build();
        mRootConfig = RootConfig.create("Unit Test", "1.0")
                .mediaPlayerFactory(new RuntimeMediaPlayerFactory(mMockMediaPlayerProvider));
    }

    @Ignore
    @Test
    public void testAPLVideoLayer() {
        loadDocument(APL_VIDEO);
        verifyViewForLayer(mMockVideoView);
    }

    private void verifyViewForLayer(View view) {
        APLScenegraph aplScenegraph = new APLScenegraph(mRootContext);
        APLLayer layer = APLLayer.ensure(aplScenegraph.getTop(), mMockRenderingContext);
        APLView aplView = new APLView(getApplication(), layer);
        layer.attachView(aplView);
        APLView topView = (APLView) layer.mChildView;
        assertTrue(topView.mAplLayer instanceof APLVideoLayer);
        assertEquals(1, topView.getChildCount());
        assertEquals(view, topView.getChildAt(0));
    }
}
