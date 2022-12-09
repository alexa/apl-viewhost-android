/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.audio;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.os.Handler;
import android.os.Looper;

import com.amazon.apl.android.Action;
import com.amazon.apl.android.RootConfig;
import com.amazon.apl.android.dependencies.ITtsPlayer;
import com.amazon.apl.android.document.AbstractDocUnitTest;
import com.amazon.apl.android.providers.ITtsPlayerProvider;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Black-box style test for the newer AudioPlayer stack (Java->JNI->Core).
 *
 * We mock the TTS player and associated factories/providers because we don't
 * want actual TTS playback, but everything else exercises the normal code.
 *
 * Observable outputs:
 * - Side-effects on TTS player: When it is told to start/stop playback
 * - The Action from executeCommands which can be:
 *   1. Null if the command failed, was run in fast mode, or was complete no-op
 *   2. Non-null if the command has resulted in some action, which will be
 *      a) Resolved if it ran to completion
 *      b) Terminated if it was stopped before completion
 *      c) Pending while the action is waiting for something to happen
 *
 * Controllable inputs:
 * - TTS player state (IDLE, READY, ENDED, etc.)
 * - TTS speech marks
 * - Core time (via the AbstractDocUnitTest::update method)
 * - Stress on the main thread using secret knowledge that the AudioPlayer
 *   posts notifications to core on the main thread. Note that the test itself
 *   runs on a non-main thread, so it nicely simulates TTS inputs coming from
 *   an external thread.
 */
public class AudioPlayerTest extends AbstractDocUnitTest {
    private static final String MINIMAL_DOCUMENT = "{" +
            "  \"type\": \"APL\"," +
            "  \"version\": \"2022.2\"," +
            "  \"mainTemplate\": {" +
            "    \"items\": {" +
            "      \"type\": \"Text\"," +
            "      \"text\": \"Hello, <u>World</u>!\"," +
            "      \"speech\": \"speech.mp3\"," +
            "      \"id\": \"hello\"," +
            "      \"textAlign\": \"center\"," +
            "      \"textAlignVertical\": \"center\"" +
            "    }" +
            "  }" +
            "}";

    private static final String MINIMAL_COMMAND = "[" +
            "  {" +
            "    \"type\": \"SpeakItem\"," +
            "    \"componentId\": \"hello\"" +
            "  }" +
            "]";

    // Wait basically forever for some multi-threaded effect that takes non-zero time. This
    // is 100x longer than the expected duration of any effect in this test, since there is
    // no network, no media loading, no heavy lifting. After we've waited this much, we're
    // quite sure that something which should have happened "now" isn't going to happen.
    private static final int FOREVER_WAIT_SECONDS = 1;

    // Wait for some multi-threaded effect that takes non-zero time, but we can afford to be
    // less patient. We expect to trip this timeout and don't want to prolong the test. There's
    // a chance if we waited longer on a heavily used device, we may have gotten a different
    // result but it's unlikely.
    private static final int SHORT_WAIT_MILLISECONDS = 50;

    // Mock TTS factory, provider, player
    private final IAudioPlayerFactory mMockAudioPlayerFactory = mock(IAudioPlayerFactory.class);
    private final ITtsPlayerProvider mMockPlayerProvider = mock(ITtsPlayerProvider.class);
    private final PassThroughTtsPlayer mPlayer = mock(PassThroughTtsPlayer.class, Mockito.CALLS_REAL_METHODS);

    /**
     * Wraps command execution in a future.
     *
     * @param commands Commands to execute
     * @return A future that resolves to True if the commands returned an action that resolved
     *         and False if the action was terminated. The future is canceled if command execution
     *         failed to produce an action.
     */
    private Future<Boolean> executeCommandsWithFutureResult(String commands) {
        CompletableFuture<Boolean> result = new CompletableFuture<>();
        Action action = mRootContext.executeCommands(commands);
        if (action == null) {
            result.cancel(true);
        } else {
            action.then(() -> result.complete(Boolean.TRUE));
            action.addTerminateCallback(() -> result.complete(Boolean.FALSE));
        }
        return result;
    }

    @Before
    public void setup() throws JSONException {
        when(mMockAudioPlayerFactory.getTtsPlayerProvider()).thenReturn(mMockPlayerProvider);
        when(mMockPlayerProvider.getPlayer()).thenReturn(mPlayer);
        mRootConfig = RootConfig.create("Unit Test", "1.0")
                .audioPlayerFactory(mMockAudioPlayerFactory);
        loadDocument(MINIMAL_DOCUMENT);
    }

    @Test
    public void testCreateFactoryProxy() {
        AudioPlayerFactoryProxy factoryProxy = new AudioPlayerFactoryProxy(mMockAudioPlayerFactory);
        assertTrue(factoryProxy.isBound());
    }

    @Test
    public void testCommandFailsWithInvalidInput() throws ExecutionException, InterruptedException, TimeoutException {
        Future<Boolean> result = executeCommandsWithFutureResult("invalid");
        assertTrue(result.isCancelled());
    }

    @Test
    public void testCommandSucceedsWithNoOpInput() throws ExecutionException, InterruptedException, TimeoutException {
        Future<Boolean> result = executeCommandsWithFutureResult("[{ \"type\": \"Idle\" }]");
        update(5);
        assertTrue(result.get(FOREVER_WAIT_SECONDS, TimeUnit.SECONDS));
    }

    @Test
    public void testCommandRequiresTimeToSucceed() throws ExecutionException, InterruptedException, TimeoutException {
        Future<Boolean> result = executeCommandsWithFutureResult("[{ \"type\": \"Idle\", \"delay\": 10 }]");
        try {
            result.get(SHORT_WAIT_MILLISECONDS, TimeUnit.MILLISECONDS);
            fail("Expected to timeout since command has not completed");
        } catch (java.util.concurrent.TimeoutException e) {
            // Success
        }
        update(5);
        try {
            result.get(SHORT_WAIT_MILLISECONDS, TimeUnit.MILLISECONDS);
            fail("Still, not enough APL time has ticked by");
        } catch (java.util.concurrent.TimeoutException e) {
            // Success
        }
        update(5);
        assertTrue(result.get(SHORT_WAIT_MILLISECONDS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testCommandCanBeTerminated() throws ExecutionException, InterruptedException, TimeoutException {
        Future<Boolean> result = executeCommandsWithFutureResult("[{ \"type\": \"Idle\", \"delay\": 10 }]");
        assertFalse(result.isCancelled());
        update(5);
        try {
            result.get(SHORT_WAIT_MILLISECONDS, TimeUnit.MILLISECONDS);
            fail("Expected to timeout since command has not completed");
        } catch (java.util.concurrent.TimeoutException e) {
            // Success
        }
        mRootContext.cancelExecution();
        assertFalse(result.get(SHORT_WAIT_MILLISECONDS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testCommandResolvesWhenPlaybackEnds() throws ExecutionException, InterruptedException, TimeoutException {
        Future<Boolean> result = executeCommandsWithFutureResult(MINIMAL_COMMAND);
        verify(mPlayer).setStateChangeListener(any());
        verify(mPlayer).setWordMarkListener(any());
        verify(mMockPlayerProvider).prepare("speech.mp3");

        mPlayer.simulateState(ITtsPlayer.IStateChangeListener.AudioPlayerState.STATE_IDLE);
        mPlayer.simulateState(ITtsPlayer.IStateChangeListener.AudioPlayerState.STATE_PREPARING);
        mPlayer.simulateState(ITtsPlayer.IStateChangeListener.AudioPlayerState.STATE_READY);
        Robolectric.flushForegroundThreadScheduler();
        update(5);
        assertTrue(mPlayer.isPlaying());

        try {
            result.get(SHORT_WAIT_MILLISECONDS, TimeUnit.MILLISECONDS);
            fail("Expected to timeout since playback has not ended");
        } catch (java.util.concurrent.TimeoutException e) {
            // Success
        }

        assertFalse(result.isCancelled());
        assertFalse(result.isDone());

        mPlayer.simulateState(ITtsPlayer.IStateChangeListener.AudioPlayerState.STATE_ENDED);
        Robolectric.flushForegroundThreadScheduler();
        update(5);
        assertFalse(mPlayer.isPlaying());

        assertTrue(result.get(FOREVER_WAIT_SECONDS, TimeUnit.SECONDS));
    }

    @Test
    public void testCommandResolvesWhenPlaybackWithSpeechMarksEnds() throws ExecutionException, InterruptedException, TimeoutException {
        Future<Boolean> result = executeCommandsWithFutureResult(MINIMAL_COMMAND);

        mPlayer.simulateState(ITtsPlayer.IStateChangeListener.AudioPlayerState.STATE_IDLE);
        mPlayer.simulateState(ITtsPlayer.IStateChangeListener.AudioPlayerState.STATE_PREPARING);
        mPlayer.simulateState(ITtsPlayer.IStateChangeListener.AudioPlayerState.STATE_READY);
        Robolectric.flushForegroundThreadScheduler();
        update(5);
        assertTrue(mPlayer.isPlaying());

        try {
            for (int i = 0; i <= 30; i += 10) {
                Thread.sleep(50);
                mPlayer.simulateSpeechMark(i);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            fail("Unexpected interrupt while updating speech marks");
        }

        mPlayer.simulateState(ITtsPlayer.IStateChangeListener.AudioPlayerState.STATE_ENDED);
        Robolectric.flushForegroundThreadScheduler();
        update(5);
        assertFalse(mPlayer.isPlaying());

        assertTrue(result.get(FOREVER_WAIT_SECONDS, TimeUnit.SECONDS));
    }

    @Test
    public void testCommandTerimationPropagated() throws ExecutionException, InterruptedException, TimeoutException {
        Future<Boolean> result = executeCommandsWithFutureResult(MINIMAL_COMMAND);

        mPlayer.simulateState(ITtsPlayer.IStateChangeListener.AudioPlayerState.STATE_IDLE);
        mPlayer.simulateState(ITtsPlayer.IStateChangeListener.AudioPlayerState.STATE_PREPARING);
        mPlayer.simulateState(ITtsPlayer.IStateChangeListener.AudioPlayerState.STATE_READY);

        mRootContext.cancelExecution();
        Robolectric.flushForegroundThreadScheduler();
        update(5);

        assertFalse(result.get(FOREVER_WAIT_SECONDS, TimeUnit.SECONDS));
    }

    @Test
    public void testCommandNeverResolvesIfPlaybackNeverEnds() throws ExecutionException, InterruptedException, TimeoutException {
        Future<Boolean> result = executeCommandsWithFutureResult(MINIMAL_COMMAND);
        mPlayer.simulateState(ITtsPlayer.IStateChangeListener.AudioPlayerState.STATE_IDLE);
        mPlayer.simulateState(ITtsPlayer.IStateChangeListener.AudioPlayerState.STATE_PREPARING);
        mPlayer.simulateState(ITtsPlayer.IStateChangeListener.AudioPlayerState.STATE_READY);
        Robolectric.flushForegroundThreadScheduler();
        update(5);

        try {
            result.get(FOREVER_WAIT_SECONDS, TimeUnit.SECONDS);
            fail("Expected to timeout since playback never ended");
        } catch (java.util.concurrent.TimeoutException e) {
            // "Success" command is never resolved
        }
    }

    @Test
    public void testCommandResolvesWhenPlaybackEndsAfterThreadDelay() throws ExecutionException, InterruptedException, TimeoutException {
        Future<Boolean> result = executeCommandsWithFutureResult(MINIMAL_COMMAND);
        mPlayer.simulateState(ITtsPlayer.IStateChangeListener.AudioPlayerState.STATE_IDLE);
        mPlayer.simulateState(ITtsPlayer.IStateChangeListener.AudioPlayerState.STATE_PREPARING);

        // This creates the scenario where the main thread is a little busy and state updates
        // are going to step on each other if not synchronized properly.
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(() -> {
            try {
                Thread.sleep(SHORT_WAIT_MILLISECONDS * 2);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        mPlayer.simulateState(ITtsPlayer.IStateChangeListener.AudioPlayerState.STATE_READY);
        mPlayer.simulateState(ITtsPlayer.IStateChangeListener.AudioPlayerState.STATE_ENDED);
        // this idle state update after ENDED overwrites any internal state before the ended update
        // can run on the main thread
        mPlayer.simulateState(ITtsPlayer.IStateChangeListener.AudioPlayerState.STATE_IDLE);
        Robolectric.flushForegroundThreadScheduler();

        // Need to provide time for the sleep above and the state changes above to be processed on
        // the main thread
        Thread.sleep(SHORT_WAIT_MILLISECONDS * 3);

        // now give core a chance to run so it can process the state updates and resolve the action
        update(5);

         assertTrue(result.get(FOREVER_WAIT_SECONDS, TimeUnit.SECONDS));
    }

    public class PassThroughTtsPlayer implements ITtsPlayer {
        ISpeechMarksListener mSpeechMarksListener;
        IStateChangeListener mStateChangeListener;
        boolean mIsPlaying = false;
        boolean mIsReleased = false;

        @Override
        public void prepare(String source, InputStream stream) {
            fail("Prepare on ITtsPlayer is unexpected");
        }

        @Override
        public void prepare(String source, URL url) {
            fail("Prepare on ITtsPlayer is unexpected");
        }

        @Override
        public void play() {
            assertFalse(mIsPlaying);
            assertFalse(mIsReleased);
            mIsPlaying = true;
        }

        @Override
        public void stop() {
            assertTrue(mIsPlaying);
            assertFalse(mIsReleased);
            mIsPlaying = false;
        }

        @Override
        public void release() {
            assertFalse(mIsReleased);
            mIsReleased = true;
        }

        @Override
        public String getSource() {
            fail("getSource on ITtsPlayer is unexpected");
            return "";
        }

        @Override
        public void setWordMarkListener(ITtsPlayer.ISpeechMarksListener listener) {
            mSpeechMarksListener = listener;
        }

        @Override
        public void setStateChangeListener(ITtsPlayer.IStateChangeListener listener) {
            mStateChangeListener = listener;
        }

        public void simulateState(IStateChangeListener.AudioPlayerState state) {
            mStateChangeListener.onStateChange(state);
        }

        public void simulateSpeechMark(int time) {
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("type", "word");
                jsonObject.put("time", time);
                jsonObject.put("value", "Hello");
                jsonObject.put("start", time);
                jsonObject.put("end", time + 100);
                ISpeechMarksListener.SpeechMark mark = ISpeechMarksListener.SpeechMark.create(jsonObject);
                mSpeechMarksListener.onSpeechMark(mark);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        boolean isPlaying() {
            return mIsPlaying;
        }

        boolean isReleased() {
            return mIsReleased;
        }
    }
}
