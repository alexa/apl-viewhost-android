/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.event;

import android.content.Context;
import android.view.View;

import com.amazon.apl.android.APLOptions;
import com.amazon.apl.android.Component;
import com.amazon.apl.android.RootConfig;
import com.amazon.apl.android.dependencies.IMediaPlayer;
import com.amazon.apl.android.document.AbstractDocViewTest;
import com.amazon.apl.android.media.RuntimeMediaPlayerFactory;
import com.amazon.apl.android.providers.AbstractMediaPlayerProvider;
import com.amazon.apl.android.views.APLAbsoluteLayout;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static com.amazon.apl.android.espresso.APLViewActions.waitFor;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;

public class ControlMediaViewTest extends AbstractDocViewTest {
    private static final String DISPLAY_AND_PLAY_VIDEO =
            "\"type\": \"TouchWrapper\",\n" +
            "      \"width\": \"100%\",\n" +
            "      \"height\": \"100%\",\n" +
            "      \"onPress\": [\n" +
            "        {\n" +
            "          \"type\": \"SetValue\",\n" +
            "          \"componentId\": \"frame\",\n" +
            "          \"property\": \"display\",\n" +
            "          \"value\": \"normal\"\n" +
            "        },\n" +
            "        {\n" +
            "          \"type\": \"ControlMedia\",\n" +
            "          \"componentId\": \"video\",\n" +
            "          \"command\": \"play\"\n" +
            "        }\n" +
            "      ],\n" +
            "      \"items\": {\n" +
            "        \"type\": \"Frame\",\n" +
            "        \"width\": \"100%\",\n" +
            "        \"height\": \"100%\",\n" +
            "        \"display\": \"none\",\n" +
            "        \"id\": \"frame\",\n" +
            "        \"items\": {\n" +
            "          \"type\": \"Video\",\n" +
            "          \"id\": \"video\",\n" +
            "          \"source\": \"sourceUrl\",\n" +
            "          \"audioTrack\": \"foreground\",\n" +
            "          \"width\": \"100\",\n" +
            "          \"height\": \"100\"\n" +
            "        }\n" +
            "      }";

    @Mock private IMediaPlayer mMockPlayer;
    APLOptions mOptions;

    private final AbstractMediaPlayerProvider mMediaPlayerProvider = new AbstractMediaPlayerProvider() {
        @Override
        public View createView(Context context) {
            return new View(context);
        }

        @Override
        public IMediaPlayer createPlayer(Context context, View view) {
            return mMockPlayer;
        }
    };

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mOptions = APLOptions.builder().mediaPlayerProvider(mMediaPlayerProvider).build();
    }

    @Test
    public void testInflatingVideoPlayer_playsVideo_correctSize() {
        RootConfig rootConfig = RootConfig.create("Unit Test", "1.0")
                .mediaPlayerFactory(new RuntimeMediaPlayerFactory(mMediaPlayerProvider));
        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(inflateWithOptions(DISPLAY_AND_PLAY_VIDEO, "", mOptions, rootConfig))
                .check(hasRootContext());

        onView(withComponent(mTestContext.getTestComponent()))
                .perform(click());

        onView(isRoot())
                .perform(waitFor(100));

        Component component = mTestContext.getRootContext().findComponentById("video");
        View view = mTestContext.getPresenter().findView(component);
        APLAbsoluteLayout.LayoutParams params = (APLAbsoluteLayout.LayoutParams) view.getLayoutParams();
        assertEquals(100, params.width);
        assertEquals(100, params.height);

        verify(mMockPlayer).play();
    }

}
