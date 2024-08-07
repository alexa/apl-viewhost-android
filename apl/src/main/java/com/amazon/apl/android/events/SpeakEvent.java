/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.events;

import android.graphics.Rect;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Log;

import com.amazon.apl.android.Component;
import com.amazon.apl.android.Event;
import com.amazon.apl.android.RootContext;
import com.amazon.apl.android.Text;
import com.amazon.apl.android.dependencies.ITtsPlayer;
import com.amazon.apl.android.providers.ITtsPlayerProvider;
import com.amazon.apl.enums.EventHighlightMode;
import com.amazon.apl.enums.EventScrollAlign;
import com.amazon.apl.enums.ComponentType;
import com.amazon.apl.enums.EventProperty;

import java.lang.ref.WeakReference;


/**
 * @deprecated
 * TODO [JIRA-28202] Delete this and automatically update consumers to new api
 * This class is deprecated in favor of {@link com.amazon.apl.android.audio.AudioPlayer} since
 * TTS events are now driven by APLCore and do not need to be handled using SpeakEvent.
 * This class will be removed in future versions.
 * APL Speak Event generated by SpeakItem and SpeakList commands.
 * See @{link <a https://developer.amazon.com/en-US/docs/alexa/alexa-presentation-language/apl-standard-commands.html#speakitem-command>
 * APL Command Specification</a>}
 */
@Deprecated
public class SpeakEvent extends Event implements ITtsPlayer.IStateChangeListener, ITtsPlayer.ISpeechMarksListener {

    private static final String TAG = "SpeakEvent";

    @NonNull
    private final WeakReference<ITtsPlayerProvider> mTtsPlayerProvider;

    private EventHighlightMode mHighlightMode;
    private EventScrollAlign mAlign;
    private Component mComponent;

    /**
     * Count the offset for ssml tags
     */
    @NonNull
    private static final String mTags = "amazon:effect|audio|break|emphasis|lang|p|phoneme|prosody|s|say-as|speak|sub|voice|w";
    @NonNull
    private static final String mSsmlStartTag = "<(" + mTags + ")(.*)(>|/>)";
    @NonNull
    private static final String mSsmlEndTag = "</(" + mTags + ")(\\s*)/>";
    @Nullable
    private String mLines;
    @Nullable
    private int[] mSpans;
    private int mCurrLine;
    private int mCurrChar;

    /**
     * Constructs the Event.
     *
     * @param nativeHandle      Handle to the native event.
     * @param rootContext       The root context for the event.
     * @param ttsPlayerProvider The provider for the TTS player.
     */
    private SpeakEvent(long nativeHandle, RootContext rootContext, ITtsPlayerProvider ttsPlayerProvider) {
        super(nativeHandle, rootContext);
        mTtsPlayerProvider = new WeakReference<>(ttsPlayerProvider);
    }


    static public SpeakEvent create(long nativeHandle, RootContext rootContext,
                                    ITtsPlayerProvider ttsPlayerProvider) {
        return new SpeakEvent(nativeHandle, rootContext, ttsPlayerProvider);
    }

    /**
     * Execute the command.
     */
    @Override
    public void execute() {

        ITtsPlayerProvider provider = mTtsPlayerProvider.get();
        if (provider == null) {
            // Cancel the execution if resources aren't available, this will result in terminate
            Log.w(TAG, "Failed SpeakEvent execute");
            mRootContext.cancelExecution();
            return;
        }

        mComponent = getComponent();
        mHighlightMode = EventHighlightMode.valueOf(
                mProperties.getInt(EventProperty.kEventPropertyHighlightMode));
        mAlign = EventScrollAlign.valueOf(
                mProperties.getInt(EventProperty.kEventPropertyAlign));
        String source = mProperties.getString(EventProperty.kEventPropertySource);
        ITtsPlayer player = provider.getPlayer();
        if(source != null && !source.equals(player.getSource())) {
            resolve();
            return;
        }
        player.setStateChangeListener(this);
        player.setWordMarkListener(this);
        if (mComponent.getComponentType() == ComponentType.kComponentTypeText) {
            Text text = (Text) mComponent;
            mSpans = text.getSpans();
            String lines = text.getLines();
            if (lines != null) {
                mLines = lines.toLowerCase();
            }
            mCurrLine = 0;
            mCurrChar = 0;
        }

        mTtsPlayerProvider.get().getPlayer().play();
    }

    private void removeLineHighlighting() {
        if (mComponent.getComponentType() == ComponentType.kComponentTypeText) {
            Text text = (Text) mComponent;
            text.setCurrentKaraokeLine(mRootContext.getViewPresenter(), null);
        }
    }

    /**
     * Terminate the event process.
     */
    @Override
    public void terminate() {
        removeLineHighlighting();
        if (mTtsPlayerProvider != null && mTtsPlayerProvider.get() != null) {
            ITtsPlayer player = mTtsPlayerProvider.get().getPlayer();
            player.stop();
            player.setStateChangeListener(null);
            player.setWordMarkListener(null);
            mTtsPlayerProvider.clear();
        }
    }

    @Override
    public void onStateChange(ITtsPlayer.IStateChangeListener.AudioPlayerState state) {

        if (state == ITtsPlayer.IStateChangeListener.AudioPlayerState.STATE_ENDED || state == ITtsPlayer.IStateChangeListener.AudioPlayerState.STATE_ERROR) {

            ITtsPlayerProvider provider = mTtsPlayerProvider.get();
            if (provider == null) {
                // Cancel the execution if resources aren't available, this will result in terminate
                Log.w(TAG, "Failed SpeakEvent onStateChange");
                mRootContext.cancelExecution();
                return;
            }

            ITtsPlayer player = mTtsPlayerProvider.get().getPlayer();
            player.setStateChangeListener(null);
            player.setWordMarkListener(null);
            mRootContext.post(this::removeLineHighlighting);
            mRootContext.post(this::resolve);
        }
    }

    private Boolean findSpeechMark(String mark) {
        if (!mark.isEmpty() && mLines != null && mSpans != null) {
            int offset = mLines.indexOf(mark, mCurrChar);
            if (offset >= 0) {
                // exact match
                for (int i = mCurrLine; i < mSpans.length; i++) {
                    if (offset < mSpans[i]) {
                        if (i - mCurrLine <= 1) {
                            // advance 0 or 1 line
                            mCurrLine = i;
                            mCurrChar = offset + mark.length();
                        } else {
                            // limit advance of no more than 1 line at a time
                            // to avoid jumping
                            mCurrChar = mSpans[mCurrLine];
                            mCurrLine++;
                        }
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public void onSpeechMark(@NonNull ITtsPlayer.ISpeechMarksListener.SpeechMark mark) {
        if (mHighlightMode != EventHighlightMode.kEventHighlightModeLine) {
            return;
        }
        // AVS inserts SSML tags into the mp3's id3 tags, so we need to ignore them
        if (mark.value == null || mark.value.isEmpty() || mark.value.matches(mSsmlStartTag) || mark.value.matches(mSsmlEndTag)) {
            return;
        }
        // to handle unicode text (which can not be broken up into words by looking for space separators)
        // so can't do word counting
        // now do speech marks handling the old (GloriaDataBindingAPML) way,
        // just search for the mark in the text
        String[] marks = mark.value.split("-");
        for (String value: marks) {
             if (findSpeechMark(value.toLowerCase())) {
                Text text = (Text) mComponent;
                // If this is in a scroll container and this is line by line highlight mode then scroll every line
                boolean success = text.setCurrentKaraokeLine(mRootContext.getViewPresenter(), mCurrLine);
                if (success) {
                    Rect bounds = text.getLineBounds(mCurrLine);
                    mRootContext.scrollToRectInComponent(mComponent, bounds.left,
                            bounds.top, bounds.width(), bounds.height(), mAlign);
                }
            }
        }
    }
}
