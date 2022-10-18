/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

package com.amazon.alexa.android.extension.discovery;

import com.amazon.alexa.android.extension.discovery.L2_IRemoteClient;

import android.os.Message;
import com.amazon.alexaext.ActivityDescriptor;
import com.amazon.alexaext.SessionDescriptor;

//
// Numeric method id must persist across versions.
//
interface L2_IRemoteServiceV2 {

//-- Transaction versionn --//

// Client and Server should be on same version
const int TRANSACT_VERSION = 2;
// verify this api with transaction versioning
boolean L2_supportsTransactVersion(int expectedVersion) = 10;

//-- IPC Connection handshake --//

// client is requesting service.
void L2_connect(L2_IRemoteClient client, String configuration) = 20;
// A client has ended its session.
void L2_connectionClosed(L2_IRemoteClient client, String message) = 30;

//-- Client/Server Messaging --//

// message from Client->Server
void L2_receive(int clientID, int routingID, in ActivityDescriptor activity, String message) = 70;
// message sent from Server->Client (wrapper for client "receive"
void L2_send(L2_IRemoteClient client, int routingID, in ActivityDescriptor activity, String message)  = 90;
// message sent from Server->Client
void L2_sendFailure(L2_IRemoteClient client, int routingID, in ActivityDescriptor activity, int errorCode, String error, String failedMessage) = 100;

// message received from Client->Server indicating successful retrieval of resource
void L2_onResourceAvailable(int clientID, int routingID, in ActivityDescriptor activity, in Surface surface, in Rect rect, in String resourceID) = 110;
// message received from Client->Server indicating failure in retrieval of resource
void L2_onResourceUnavailable(int clientID, int routingID, in ActivityDescriptor activity, in String resourceID) = 111;

void L2_onRegistered(int clientID, int routingID, in ActivityDescriptor activity) = 120;
void L2_onUnregistered(int clientID, int routingID, in ActivityDescriptor activity) = 121;

void L2_onSessionStarted(int clientID, int routingID, in SessionDescriptor session) = 130;
void L2_onSessionEnded(int clientID, int routingID, in SessionDescriptor session) = 131;

void L2_onForeground(int clientID, int routingID, in ActivityDescriptor activity) = 140;
void L2_onBackground(int clientID, int routingID, in ActivityDescriptor activity) = 141;
void L2_onHidden(int clientID, int routingID, in ActivityDescriptor activity) = 142;
}
