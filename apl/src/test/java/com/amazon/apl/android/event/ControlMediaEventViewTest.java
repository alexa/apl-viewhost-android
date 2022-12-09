package com.amazon.apl.android.event;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

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

import org.junit.Test;

public class ControlMediaEventViewTest extends AbstractDocViewTest {

    private static final String DISPLAY_AND_PLAY_VIDEO =
            "\"type\": \"TouchWrapper\",\n" +
                    "      \"width\": \"100%\",\n" +
                    "      \"height\": \"100%\",\n" +
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

    @Test
    public void testInflatingVideoPlayer_playsVideo_correctSize() {
        IMediaPlayer mediaPlayerMock = mock(IMediaPlayer.class);

        AbstractMediaPlayerProvider provider = new AbstractMediaPlayerProvider() {
            @Override
            public View createView(Context context) {
                return new View(context);
            }

            @Override
            public IMediaPlayer createPlayer(Context context, View view) {
                return mediaPlayerMock;
            }
        };

        RootConfig rootConfig = RootConfig.create("Unit Test", "1.0")
                .mediaPlayerFactory(new RuntimeMediaPlayerFactory(provider));
        inflateWithOptions(DISPLAY_AND_PLAY_VIDEO, "\"onMount\": " + "[\n" +
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
                "      ]\n" , APLOptions.builder().mediaPlayerProvider(provider), rootConfig);

        Component component = mTestContext.getRootContext().findComponentById("video");
        View view = mTestContext.getPresenter().findView(component);
        APLAbsoluteLayout.LayoutParams params = (APLAbsoluteLayout.LayoutParams) view.getLayoutParams();
        assertEquals(100, params.width);
        assertEquals(100, params.height);

        verify(mediaPlayerMock).play();
    }
}
