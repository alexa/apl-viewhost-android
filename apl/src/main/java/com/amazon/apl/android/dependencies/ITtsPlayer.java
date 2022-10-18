/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.dependencies;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.URL;

/**
 * The TTS Player interface.
 */
public interface ITtsPlayer {
    /**
     * Prepares the audio. The {@code stream} should not be processed on the UI thread.
     * @param stream the TTS stream.
     */
    void prepare(String source, InputStream stream);
    /**
     * Prepares the audio. The {@code url} should not be processed on the UI thread.
     * @param url the TTS URL.
     */
    void prepare(String source, URL url);

    /**
     * Play TTS.
     */
    void play();

    /**
     * Stop TTS playback.
     */
    void stop();

    /**
     * Release resources.
     */
    void release();

    /**
     * @return the source string of TTS mp3.
     */
    String getSource();

    /**
     * Sets the onSpeechMark listener.
     * @param listener the listener for speech marks.
     */
    void setWordMarkListener(ISpeechMarksListener listener);

    /**
     * Sets the onStateChange listener.
     * @param listener the listener for player state changes.
     */
    void setStateChangeListener(IStateChangeListener listener);

    /**
     * Callback for receiving Speech Mark descriptors
     */
    interface ISpeechMarksListener {
        /*
         * @param mark String representation of the Speech Mark
         *  For example, the speech mark could contain JSON
         *  formatted positional and time indicators:
         * e.g. -
         * [
         *     {
         *         "time": 65,            # Offset, in milliseconds, when the word in "value" is spoken.
         *         "type": "word",        # Speech tag type. One of: word, sentence, ssml. ssml tags are not sent down generally,
         *                                  and are ignored for our purposes.
         *         "start": 7,            # Start letter offset. Ignored for our purposes.
         *         "end": 11,             # End letter offset. Ignored for our purposes.
         *         "value": "Mary"        # Word or phrase being spoken.
         *     },
         * ]
         *
         */
        void onSpeechMark(SpeechMark mark);

        class SpeechMark {
            public static SpeechMark create(JSONObject obj) throws JSONException {
                MarkType markType = MarkType.UNKNOWN;
                String type = obj.getString("type");
                if (type.equalsIgnoreCase("word")) {
                    markType = MarkType.WORD;
                } else if (type.equalsIgnoreCase("sentence")) {
                    markType = MarkType.SENTENCE;
                } else if (type.equalsIgnoreCase("ssml")) {
                    markType = MarkType.SSML;
                }
                return new SpeechMark(
                        obj.getString("value"),
                        obj.getLong("time"),
                        obj.getInt("start"),
                        obj.getInt("end"),
                        markType);
            }

            private SpeechMark(String value, long time, int start, int end, MarkType markType) {
                this.value = value;
                this.time = time;
                this.start = start;
                this.end = end;
                this.markType = markType;
            }
            public int start;
            public int end;
            public String value;
            public long time;
            public MarkType markType;
        }

        enum MarkType {
            UNKNOWN, WORD, SENTENCE, SSML
        }
    }

    /**
     * Callback to listen for TTS Player state changes.
     */
    interface IStateChangeListener {
        void onStateChange(AudioPlayerState state);

        /**
         * A state enum created to mirror the functionality of ExoPlayer's state enum
         * Used in the external state changed callback of the external listener
         */
        enum AudioPlayerState {
            /**
             * Unused zero state
             */
            STATE_DEFAULT,
            /**
             * Uninit before methods called
             */
            STATE_IDLE,
            /**
             * Internal config
             */
            STATE_PREPARING,
            /**
             * Async task setup
             */
            STATE_BUFFERING,
            /**
             * Playing
             */
            STATE_READY,
            /**
             * Playback finished
             */
            STATE_ENDED,
            /**
             * Playback error
             */
            STATE_ERROR
        }
    }
}
