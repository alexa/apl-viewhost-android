package com.amazon.apl.viewhost.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.os.Bundle;

import com.amazon.apl.android.APLLayout;
import com.amazon.apl.android.DocumentSession;
import com.amazon.apl.android.IAPLViewPresenter;
import com.amazon.apl.android.RootContext;
import com.amazon.apl.viewhost.AbstractUnifiedViewhostTest;
import com.amazon.apl.viewhost.BasicActivity;
import com.amazon.apl.viewhost.primitives.JsonStringDecodable;
import com.amazon.apl.viewhost.request.RenderDocumentRequest;

import org.junit.Test;
import org.robolectric.Robolectric;
import org.robolectric.android.controller.ActivityController;

public class DocumentReuseTest extends AbstractUnifiedViewhostTest {

    private static final String SIMPLE_DOCUMENT = "{" +
            "  \"type\": \"APL\"," +
            "  \"version\": \"2024.2\"," +
            "  \"onMount\": {" +
            "    \"type\": \"SendEvent\"," +
            "    \"arguments\": [" +
            "      \"${viewport.pixelWidth}x${viewport.pixelHeight}\"," +
            "      \"${viewport.dpi}\"" +
            "    ]" +
            "  }," +
            "  \"mainTemplate\": {" +
            "    \"items\": {" +
            "      \"type\": \"Text\"," +
            "      \"text\": \"Hello, World!\"" +
            "    }" +
            "  }" +
            "}";
    @Test
    public void testDocumentReuseOnNewActivity() {
        RenderDocumentRequest renderDocumentRequest = RenderDocumentRequest.builder()
                .document(new JsonStringDecodable(SIMPLE_DOCUMENT))
                .documentSession(DocumentSession.create())
                .build();

        DocumentHandleImpl documentHandle = (DocumentHandleImpl) mViewhost.render(renderDocumentRequest);
        assertSendEvent("640x480", 160);
        mMessageHandler.queue.clear();
        RootContext oldRootContext = documentHandle.getRootContext();
        IAPLViewPresenter oldPresenter = mActivity.getView().getPresenter();

        mViewhost.unBind();
        mActivity.finish();

        //Launch a new activity
        ActivityController<BasicActivity> controller = Robolectric.buildActivity(BasicActivity.class);
        controller.setup(); // Moves the Activity to the RESUMED state
        APLLayout newLayout = controller.get().getView();

        assertTrue(mMessageHandler.queue.isEmpty());
        //bind to new view
        mViewhost.bind(newLayout);

        assertDocumentStateChanged(DocumentState.INFLATED.toString());

        assertEquals(oldRootContext, documentHandle.getRootContext());
        assertNotEquals(oldPresenter, newLayout.getPresenter());
    }

    @Test
    public void testDocumentReuseOnNewActivityWithOrientationChange() {
        RenderDocumentRequest renderDocumentRequest = RenderDocumentRequest.builder()
                .document(new JsonStringDecodable(SIMPLE_DOCUMENT))
                .documentSession(DocumentSession.create())
                .build();

        DocumentHandleImpl documentHandle = (DocumentHandleImpl) mViewhost.render(renderDocumentRequest);
        assertSendEvent("640x480", 160);
        mMessageHandler.queue.clear();
        RootContext oldRootContext = documentHandle.getRootContext();
        IAPLViewPresenter oldPresenter = mActivity.getView().getPresenter();

        mViewhost.unBind();
        mActivity.finish();

        //Launch a new activity
        ActivityController<BasicActivity> controller = Robolectric.buildActivity(BasicActivity.class);
        Bundle savedInstanceStage = new Bundle();

        // Fake an orientation change that swaps width and height.
        savedInstanceStage.putInt("width", BasicActivity.APL_VIEW_HEIGHT);
        savedInstanceStage.putInt("height", BasicActivity.APL_VIEW_WIDTH);
        controller.setup(savedInstanceStage);
        APLLayout newLayout = controller.get().getView();

        assertTrue(mMessageHandler.queue.isEmpty());
        //bind to new view
        mViewhost.bind(newLayout);

        assertDocumentStateChanged(DocumentState.INFLATED.toString());

        assertEquals(oldRootContext, documentHandle.getRootContext());
        assertNotEquals(oldPresenter, newLayout.getPresenter());

        assertEquals(oldRootContext.getMetricsTransform().getScaledViewhostHeight(), BasicActivity.APL_VIEW_WIDTH);
        assertEquals(oldRootContext.getMetricsTransform().getScaledViewhostWidth(), BasicActivity.APL_VIEW_HEIGHT);
    }
}
