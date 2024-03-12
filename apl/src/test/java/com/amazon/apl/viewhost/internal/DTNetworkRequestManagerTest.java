package com.amazon.apl.viewhost.internal;

import com.amazon.apl.devtools.enums.DTNetworkRequestType;
import com.amazon.apl.devtools.models.network.IDTNetworkRequestHandler;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

public class DTNetworkRequestManagerTest {

    @Mock
    private IDTNetworkRequestHandler mDTNetworkRequestHandler;

    private DTNetworkRequestManager mDTNetworkRequestManager;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);

        mDTNetworkRequestManager = new DTNetworkRequestManager();
    }

    @Test
    public void testBindDTNetworkRequest_AllStoredNetworkRequestEventAreReported() {
        int requestId = IDTNetworkRequestHandler.IdGenerator.generateId();
        String testUrl = "testUrl";
        mDTNetworkRequestManager.requestWillBeSent(requestId,0, testUrl, DTNetworkRequestType.PACKAGE);
        mDTNetworkRequestManager.loadingFinished(requestId, 0, 0);
        mDTNetworkRequestManager.loadingFailed(requestId, 0);

        mDTNetworkRequestManager.bindDTNetworkRequest(mDTNetworkRequestHandler);

        verify(mDTNetworkRequestHandler).requestWillBeSent(eq(requestId), anyDouble(),eq(testUrl), eq(DTNetworkRequestType.PACKAGE));
        verify(mDTNetworkRequestHandler).loadingFinished(eq(requestId), anyDouble(), anyInt());
        verify(mDTNetworkRequestHandler).loadingFailed(eq(requestId), anyDouble());
    }

    @Test
    public void testWhenAlreadyBindDTNetworkRequest_allNetworkEventsAreRerouted() {
        mDTNetworkRequestManager.bindDTNetworkRequest(mDTNetworkRequestHandler);
        int requestId = IDTNetworkRequestHandler.IdGenerator.generateId();
        String testUrl = "testUrl";
        mDTNetworkRequestManager.requestWillBeSent(requestId,0, testUrl, DTNetworkRequestType.IMAGE);
        mDTNetworkRequestManager.loadingFinished(requestId, 0, 0);
        mDTNetworkRequestManager.loadingFailed(requestId, 0);

        verify(mDTNetworkRequestHandler).requestWillBeSent(eq(requestId), anyDouble(),eq(testUrl), eq(DTNetworkRequestType.IMAGE));
        verify(mDTNetworkRequestHandler).loadingFinished(eq(requestId), anyDouble(), anyInt());
        verify(mDTNetworkRequestHandler).loadingFailed(eq(requestId), anyDouble());
    }
}
