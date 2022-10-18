/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.alexaext;

import java.util.Set;

/**
 * The Extension interface defines the contract exposed from the extension to a activity (e.g. a
 * typical activity for an APL extension is a rendering task for an APL document). Extensions are
 * typically lazily instantiated by an execution environment (e.g. APL or Alexa Web for Games) in
 * response to the extension being requested by an activity.
 *
 * The extension contract also defines the lifecycle of an extension. The lifecycle of an extension
 * starts with an activity requesting it. Each activity belongs to exactly one session for the
 * entire duration of the activity.
 *
 * During a activity interaction, an extension will receive a well-defined sequence of calls. For
 * example, consider a common extension use case: a single, standalone APL document requests an
 * extension to render its contents, and then gets finished (i.e. taken off screen) after a short
 * interaction. In this example, the activity corresponds to the rendering task for the APL document,
 * the session corresponds to the skill session.
 *
 * The extension would, for this example, receive the following sequence of calls as
 * follows:
 *
 * - @c onSessionStarted is called with the document's session descriptor
 * - @c createRegistration is called for the document
 * - @c onRegistered is called when the registration succeeds
 * - @c onForeground is called to indicate that the activity is being rendered in the foreground
 * - the activity can then send commands and receive events with the extension
 * - the document is finished by the APL execution environment after user interactions are done
 * - @c onUnregistered is called to indicate that the document is no longer active
 * - @c onSessionEnded is called (could be delayed)
 *
 * Consider the more complex case of an extension being requested by a set of related APL documents
 * interacting with each other via the APL backstack. For example, this could be a menu flow
 * implemented as a series of distinct documents. For a multi-document session, a typical flow
 * would instead be as follows:
 * - @c onSessionStarted is called with the first document's session descriptor
 * - @c createRegistration is called for the first document
 * - @c onRegistered is called when the registration succeeds
 * - @c onForeground is called to indicate that the activity is being rendered in the foreground
 * - the activity can then send commands and receive events with the extension
 * - a new document is rendered in the same session, and the current one is pushed to the backstack
 * - @c createRegistration is called for the second document
 * - @c onRegistered is called when the registration succeeds
 * - @c onHidden is called for the first activity to indicate it is now hidden
 * - @c onForeground is called to indicate that the activity is in the foreground
 * - the second document can now interact with the extension
 * - the second document restores the first document from the backstack
 * - @c onUnregistered is called to indicate that the second document is no longer active
 * - @c onForeground is called to indicate that the first document is now again in the foreground
 * - the first document is finished
 * - @c onUnregistered is called to indicate that the first document is no longer active
 * - @c onSessionEnded is called (could be delayed)
 */
public interface IExtension {
    /**
     * Get the URIs supported by the extension.
     *
     * @return The extension URIs.
     */
    Set<String> getURIs();

    interface IExtensionActivityEventCallback {
        boolean sendExtensionEvent(ActivityDescriptor activity, String event);
    }

    interface IExtensionActivityCommandResultCallback {
        void sendCommandResult(ActivityDescriptor activity, String event);
    }

    interface ILiveDataActivityUpdateCallback {
        boolean invokeLiveDataUpdate(ActivityDescriptor activity, String liveDataUpdate);
    }

    /**
     * Create a registration for the extension. The registration is returned in a "RegistrationSuccess" or
     * "RegistrationFailure" message. The extension is defined by a unique token per registration, an environment of
     * static properties, and the extension schema.
     *
     * The schema defines the extension api, including commands, events and live data.  The "RegistrationRequest"
     * parameter contains a schema version, which matches the schema versions supported by the runtime, and extension
     * settings defined by the requesting document.
     *
     * std::exception or ExtensionException thrown from this method are converted to "RegistrationFailure"
     * messages and returned to the caller.
     *
     * This method is called by the extension framework when the extension is requested by a document.
     *
     * @param @param activity ActivityDescriptor.
     * @param registrationRequest A "RegistrationRequest" message, includes extension settings.
     * @return A extension "RegistrationSuccess" or "RegistrationFailure"  message.
     */
    default String createRegistration(ActivityDescriptor activity, String registrationRequest) {
        return createRegistration(activity.getURI(), registrationRequest);
    }

    /**
     * Callback registration for extension "Event" messages. Guaranteed to be called before the document is mounted.
     * The callback forwards events to the document event handlers.
     *
     * @param callback The callback for events generating from the extension.
     */
    default void registerEventCallback(IExtensionActivityEventCallback callback) {}

    /**
     * Callback for extension "LiveDataUpdate" messages. Guaranteed to be called before the document is mounted.
     * The callback forwards live data changes to the document data binding and live data handlers.
     *
     * @param callback The callback for live data updates generating from the extension.
     */
    default void registerLiveDataUpdateCallback(ILiveDataActivityUpdateCallback callback) {}

    /**
     * Callback for extension command results. Guaranteed to be called before the document is mounted.
     * The callback forwards command results to the document.
     *
     * @param callback The callback for live data updates generating from the extension.
     */
    default void registerCommandResultCallback(IExtensionActivityCommandResultCallback callback) {}

    /**
     * Execute a Command that was initiated by the document.
     *
     * std::exception or ExtensionException thrown from this method are converted to "CommandFailure"
     * messages and returned to the caller.
     *
     * @param activity ActivityDescriptor
     * @param id command ID.
     * @param command The requested Command message.
     * @return true if the command succeeded.
     */
    default boolean onCommand(ActivityDescriptor activity, int id, String command)  {
        return onCommand(id, activity.getURI(), command); }

    /**
     * Process message requested by a component. DOes not require an answer.
     * @param activity ActivityDescriptor.
     * @param message The requested Command message.
     * @return tru if succeeds, false otherwise.
     */
    default boolean onMessage(ActivityDescriptor activity, String message) {
        return onMessage(activity.getURI(), message);
    }

    /**
     * Invoked when a system resource, such as display surface, is ready for use. This method
     * will be called after the extension receives a message indicating the resource is "Ready".
     * Messages supporting shared resources: "Component"
     * Not all execution environments support shared resources.
     * @param activity ActivityDescriptor
     * @param resourceHolder corresponding resource holder.
     */
    default void onResourceReady(ActivityDescriptor activity, ResourceHolder resourceHolder) {
        onResourceReady(activity.getURI(), resourceHolder);
    }

    /**
     * Invoked after registration has been completed successfully. This is useful for
     * stateful extensions that require initializing activity data upfront.
     *
     * @param activity ActivityDescriptor.
     */
    default void onRegistered(ActivityDescriptor activity) {
        onRegistered(activity.getURI(), activity.getActivityId());
    }

    /**
     * Invoked after extension unregistered. This is useful for stateful extensions that require
     * cleaning up activity data.
     * @param activity ActivityDescriptor.
     */
    default void onUnregistered(ActivityDescriptor activity) {
        onUnregistered(activity.getURI(), activity.getActivityId());
    }

    /**
     * Called whenever a new session that requires this extension is started. This is guaranteed
     * to be called before @c onRegistered for any activity that belongs to the specified
     * session.
     *
     * No guarantees are made regarding the time at which this is invoked, only that if
     * @c onRegistered is invoked, this call will have happened prior to it. For example, a
     * typical implementation will withhold the call until extension registration is triggered in
     * order to avoid spurious notifications about contexts being created / destroyed that do not
     * require this extension.
     *
     * This call is guaranteed to be made only once for a given session and extension pair.
     *
     * @param session The session being started.
     */
    default void onSessionStarted(SessionDescriptor session) {}

    /**
     * Invoked when a previously started session has ended. This is only called when
     * @c onSessionStarted was previously called for the same session.
     *
     * This call is guaranteed to be made only once for a given session and extension pair.
     *
     * @param session The session that ended.
     */
    default void onSessionEnded(SessionDescriptor session) {}

    /**
     * Invoked when a visual activity becomes in the foreground. If an activity does not
     * have any associated visual presentation, this method is never called for it. If a
     * visual activity starts in the foreground, this method will be called right after
     * a successful registration.
     *
     * @param activity The activity using this extension.
     */
    default void onForeground(ActivityDescriptor activity) {}

    /**
     * Invoked when a visual activity becomes in the background, i.e. it is still completely or
     * partially visible, but is no longer the active visual presentation. If an activity does not
     * have any associated visual presentation, this method is never called for it. If a
     * visual activity starts in the background, this method will be called right after
     * a successful registration.
     *
     * Extensions are encouraged to avoid publishing updates to backgrounded activities as
     * they may not be able to process them.
     *
     * @param activity The activity using this extension.
     */
    default void onBackground(ActivityDescriptor activity) {}

    /**
     * Invoked when a visual activity becomes in hidden, i.e. it is no longer visible (e.g. it was
     * pushed to the backstack, or was temporarily replaced by another presentation activity). If an
     * activity does not have any associated visual presentation, this method is never called for
     * it. If a visual activity starts in the background, this method will be called right after
     * a successful registration.
     *
     * This method is not called when an activity leaves the screen because it ended.
     *
     * Extensions are encouraged to avoid publishing updates to hidden activities as
     * they are typically not able to process them.
     *
     * @param activity The activity using this extension.
     */
    default void onHidden(ActivityDescriptor activity) {}

    ////////////////// Backwards compatibility interface below ////////////////////////////////////

    @Deprecated
    interface IExtensionEventCallback {
        boolean sendExtensionEvent(String uri, String event);
    }

    @Deprecated
    interface IExtensionCommandResultCallback {
        void sendCommandResult(String uri, String event);
    }

    /**
     * Create a registration for the extension. The registration is returned in a "RegistrationSuccess" or
     * "RegistrationFailure" message. The extension is defined by a unique token per registration, an environment of
     * static properties, and the extension schema.
     *
     * The schema defines the extension api, including commands, events and live data.  The "RegistrationRequest"
     * parameter contains a schema version, which matches the schema versions supported by the runtime, and extension
     * settings defined by the requesting document.
     *
     * std::exception or ExtensionException thrown from this method are converted to "RegistrationFailure"
     * messages and returned to the caller.
     *
     * This method is called by the extension framework when the extension is requested by a document.
     *
     * @param uri The extension URI.
     * @param registrationRequest A "RegistrationRequest" message, includes extension settings.
     * @return A extension "RegistrationSuccess" or "RegistrationFailure"  message.
     * @deprecated see {@link #createRegistration(String,String)}
     */
    @Deprecated
    String createRegistration(String uri, String registrationRequest);

    /**
     * Callback registration for extension "Event" messages. Guaranteed to be called before the document is mounted.
     * The callback forwards events to the document event handlers.
     *
     * @param callback The callback for events generating from the extension.
     * @deprecated see {@link #registerEventCallback(IExtensionActivityEventCallback)}
     */
    @Deprecated
    void registerEventCallback(IExtensionEventCallback callback);

    /**
     * Callback for extension "LiveDataUpdate" messages. Guaranteed to be called before the document is mounted.
     * The callback forwards live data changes to the document data binding and live data handlers.
     *
     * @param callback The callback for live data updates generating from the extension.
     * @deprecated see {@link #registerLiveDataUpdateCallback(ILiveDataActivityUpdateCallback)}
     */
    @Deprecated
    void registerLiveDataUpdateCallback(ILiveDataUpdateCallback callback);

    /**
     * Callback for extension command results. Guaranteed to be called before the document is mounted.
     * The callback forwards command results to the document.
     *
     * @param callback The callback for live data updates generating from the extension.
     * @deprecated see {@link #registerCommandResultCallback(IExtensionActivityCommandResultCallback)}
     */
    @Deprecated
    void registerCommandResultCallback(IExtensionCommandResultCallback callback);

    /**
     * Execute a Command that was initiated by the document.
     *
     * std::exception or ExtensionException thrown from this method are converted to "CommandFailure"
     * messages and returned to the caller.
     *
     * @param uri The extension URI.
     * @param command The requested Command message.
     * @return true if the command succeeded.
     * @deprecated see {@link #onCommand(ActivityDescriptor,int,String)}
     */
    @Deprecated
    default boolean onCommand(int id, String uri, String command)  { return false; }

    /**
     * Process message requested by a component. Does not require an answer.
     * @param uri The extension URI.
     * @param message The requested Command message.
     * @return tru if succeeds, false otherwise.
     * @deprecated see {@link #onMessage(ActivityDescriptor,String)}
     */
    @Deprecated
    default boolean onMessage(String uri, String message) { return false; }

    /**
     * Notify extension about Resource being ready for use.
     * @param uri The extension URI.
     * @param resourceHolder corresponding resource holder.
     * @deprecated see {@link #onResourceReady(ActivityDescriptor, ResourceHolder)}
     */
    @Deprecated
    default void onResourceReady(String uri, ResourceHolder resourceHolder) {}

    /**
     * Invoked after registration has been completed successfully. This is useful for
     * stateful extensions that require initializing session data upfront.
     *
     * @param uri The extension URI used during registration.
     * @param token The client token issued during registration.
     * @deprecated see {@link #onRegistered(ActivityDescriptor)}
     */
    @Deprecated
    default void onRegistered(String uri, String token) {}

    /**
     * Notification to extension on when it was un-registered.
     * @param uri URI
     * @param token Registration token.
     * @deprecated see {@link #onUnregistered(ActivityDescriptor)}
     */
    @Deprecated
    default void onUnregistered(String uri, String token) {}
}
