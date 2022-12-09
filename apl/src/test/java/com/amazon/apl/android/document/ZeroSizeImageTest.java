package com.amazon.apl.android.document;

import com.amazon.apl.android.APLOptions;
import com.amazon.apl.android.dependencies.IImageLoader;

import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ZeroSizeImageTest extends AbstractDocViewTest {

    String DOC = "{\n" +
            "  \"type\": \"APL\",\n" +
            "  \"version\": \"%s\",\n" +
            "  \"mainTemplate\": {\n" +
            "    \"item\": {\n" +
            "      \"type\": \"Container\",\n" +
            "      \"width\": 100,\n" +
            "      \"height\": 100,\n" +
            "      \"item\": [\n" +
            "        {\n" +
            "          \"type\": \"Image\",\n" +
            "          \"width\": 100,\n" +
            "          \"height\": 100,\n" +
            "          \"id\": \"A\",\n" +
            "          \"display\": \"none\",\n" +
            "          \"source\": \"https://a.png\"\n" +
            "        },\n" +
            "        {\n" +
            "          \"type\": \"Image\",\n" +
            "          \"width\": 100,\n" +
            "          \"height\": 100,\n" +
            "          \"id\": \"B\",\n" +
            "          \"display\": \"none\",\n" +
            "          \"source\": \"https://b.png\"\n" +
            "        }\n" +
            "      ]\n" +
            "    }\n" +
            "  }\n" +
            "}";

    String DOC_LEGACY = String.format(DOC, "1.5");

    String DOC_NON_LEGACY = String.format(DOC, "1.6");

    String DISPLAY_A_COMMAND = "[{\n" +
            "      \"type\": \"SetValue\",\n" +
            "      \"componentId\": \"A\",\n" +
            "      \"property\": \"display\",\n" +
            "      \"value\": \"normal\"\n" +
            "    }]";

    String DISPLAY_B_COMMAND = "[\n" +
            "        {\n" +
            "          \"type\": \"SetValue\",\n" +
            "          \"componentId\": \"B\",\n" +
            "          \"property\": \"display\",\n" +
            "          \"value\": \"normal\"\n" +
            "        },\n" +
            "        {\n" +
            "          \"type\": \"SetValue\",\n" +
            "          \"componentId\": \"A\",\n" +
            "          \"property\": \"display\",\n" +
            "          \"value\": \"none\"\n" +
            "        }\n" +
            "      ]";

    @Mock
    private IImageLoader mImageLoader;

    @Test
    public void test_inflatingImageZeroSize_doesNotLoadImage_legacy() {
        executeTest(DOC_LEGACY);
    }

    @Test
    public void test_inflatingImageZeroSize_doesNotLoadImage_nonLegacy() {
        executeTest(DOC_NON_LEGACY);
    }

    private void executeTest(String doc) {
        when(mImageLoader.withTelemetry(any())).thenReturn(mImageLoader);
        APLOptions.Builder optionsBuilder = APLOptions.builder()
                .imageProvider(context -> mImageLoader);

        inflateWithOptions(doc, optionsBuilder);

        executeCommands(DISPLAY_A_COMMAND);

        verify(mImageLoader).loadImage(argThat((params) -> "https://a.png".equals(params.path())));
        verify(mImageLoader, never()).loadImage(argThat((params) -> "https://b.png".equals(params.path())));

        executeCommands(DISPLAY_B_COMMAND);

        verify(mImageLoader).loadImage(argThat((params) -> "https://b.png".equals(params.path())));
    }
}
