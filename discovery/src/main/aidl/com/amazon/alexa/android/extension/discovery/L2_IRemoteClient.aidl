/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.alexa.android.extension.discovery;

import android.os.Message;

//
// Numeric method id must persist across versions.
//
interface L2_IRemoteClient {

//-- Transaction versionn --//

// Client Transaction version.
const int TRANSACT_VERSION = 1;

// verify this api with transaction versioning
const int TRANASCT_INCOMPATIBLE = -100;
boolean L2_supportsTransactVersion(int expectedVersion) = 10;

//-- IPC Connection handshake --//

// The connection ID
int L2_connectionID() = 20;
// Server->Client, connection accepted by server
void L2_connectionAccept() = 30;
// Server->Client, connection rejected by server
void L2_connectionReject(int errorCode, String error) = 40;
// Server->Client connection, is closed by server
void L2_connectionClosed(String message) = 50;


//-- Client/Server Messaging --//

// Server->Client message
void L2_receive(int routingID, String message) = 70;
// Server->Client message
void L2_receiveBroadcast(String message) = 80;
// Client->Server message
void L2_send(int routingID, String message) = 90;
// Client->Server message
void L2_setFocusLost(int routingID) = 91;
// Client->Server message
void L2_setFocusGained(int routingID) = 92;
// Client->Server message
void L2_pause(int routingID) = 93;
// Client->Server message
void L2_resume(int routingID) = 94;
// Server->Client message, the server could not process a message
void L2_messageFailure(int routingID, int errorCode, String error, String message) = 100;

// message from Server->Client requesting the resource for given resource ID
void L2_onRequestResource(int routingID, String resourceId) = 101;
// message from Client->Server indicating successful retrieval of resource
void L2_resourceAvailable(int routingID, in Surface surface, in Rect rect, in String resourceID) = 102;
// message from Client->Server indicating failure in retrieving resource
void L2_resourceUnavailable(int routingID, in String resourceID) = 103;
}
