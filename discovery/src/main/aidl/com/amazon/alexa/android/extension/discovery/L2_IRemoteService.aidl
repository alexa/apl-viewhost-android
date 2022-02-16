/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

package com.amazon.alexa.android.extension.discovery;

import com.amazon.alexa.android.extension.discovery.L2_IRemoteClient;

import android.os.Message;

//
// Numeric method id must persist across versions.
//
interface L2_IRemoteService {

//-- Transaction versionn --//

// Client and Server should be on same version
const int TRANSACT_VERSION = 1;
// verify this api with transaction versioning
boolean L2_supportsTransactVersion(int expectedVersion) = 10;

//-- IPC Connection handshake --//

// client is requesting service.
void L2_connect(L2_IRemoteClient client, String configuration) = 20;
// A client has ended its session.
void L2_connectionClosed(L2_IRemoteClient client, String message) = 30;

//-- Client/Server Messaging --//

// message from Client->Server
void L2_receive(int clientID, int routingID,  String message) = 50;
// message from Client->Server
void L2_onFocusLost(int clientID, int routingID) = 51;
// message from Client->Server
void L2_onFocusGained(int clientID, int routingID) = 52;
// message from Client->Server
void L2_onPause(int clientID, int routingID) = 53;
// message from Client->Server
void L2_onResume(int clientID, int routingID) = 54;
// message from Client->Server
void L2_onExit(int clientID, int routingID) = 90;
// message sent from Server->Client (wrapper for client "receive"
void L2_send(L2_IRemoteClient client, int routingID,  String message)  = 60;
// message sent from Server->Client
void L2_sendBroadcast(String message) = 70;
// message sent from Server->Client
void L2_sendFailure(L2_IRemoteClient client, int routingID, int errorCode, String error, String failedMessage) = 80;

// message sent from Server->Client requesting for resource with resource ID
void L2_requestResource(L2_IRemoteClient client, int routingID, String resourceId) = 100;
// message received from Client->Server indicating successful retrieval of resource
void L2_onResourceAvailable(int clientID, int routingID, in Surface surface, in Rect rect, in String resourceID) = 110;
// message received from Client->Server indicating failure in retrieval of resource
void L2_onResourceUnavailable(int clientID, int routingID, in String resourceID) = 120;
}
