/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.dependencies.impl;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaPlayer.OnSeekCompleteListener;
import android.media.MediaPlayer.OnVideoSizeChangedListener;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;

import com.amazon.apl.android.primitive.MediaSources;
import com.amazon.apl.android.primitive.MediaSources.MediaSource;
import com.amazon.apl.android.dependencies.IMediaPlayer;
import com.amazon.apl.android.dependencies.IMediaPlayer.IMediaListener.MediaState;
import com.amazon.apl.enums.AudioTrack;
import com.amazon.apl.enums.VideoScale;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Default media player for APL.
 */
public class MediaPlayer implements IMediaPlayer {

    /**
     * Lists all the actions that can be initiated on the media player.
     */
    private enum Action {
        // No action
        NONE,
        // Play current track
        PLAY,
        // Pause current track
        PAUSE,
        // Stop playback
        STOP,
        // Perform seek on the current track
        SEEK,
        // Next/Previous/Set a track from the provided list
        USER_TRACK_UPDATE,
        // Player has finished playing current track along with it's repeat count, move to the next track
        INTERNAL_TRACK_UPDATE
    }

    private static final String TAG = "MediaPlayer";

    private static final float ATTENUATED_VOLUME = 0.25f; // audio ducking volume level
    private static final int POSITION_UPDATE_INTERVAL_MS = 1000; // post track update every second
    private static final AudioAttributes AUDIO_ATTRIBUTES = new AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .build();

    private MediaSources mSources;
    @Nullable
    private android.media.MediaPlayer mMediaPlayer;
    @Nullable
    private TextureView mTextureView;
    @Nullable
    private Surface mSurface;
    private final List<IMediaListener> mListeners;
    private final AudioManager mAudioManager;
    private Handler mHandler;
    @NonNull
    private AudioTrack mAudioTrack = AudioTrack.kAudioTrackForeground;
    @NonNull
    private MediaState mCurrentState = MediaState.IDLE;
    private Action mAction = Action.NONE;
    @NonNull
    private VideoScale mScale = VideoScale.kVideoScaleBestFit;
    private Context mContext;
    private boolean mHasAudioFocus = false;
    private boolean mShouldNotifyProgress = false;
    private boolean mWasPlaying = false;
    private int mCurrentTrackIndex = 0;
    private int mLoopCount = 0;
    private int mCurrentSeekPosition = 0;

    /**
     * Constructs the media player.
     *
     * @param context     The Context of the component.
     * @param textureView The view required to display the video.
     */
    public MediaPlayer(@NonNull Context context, @NonNull TextureView textureView) {
        this(new android.media.MediaPlayer(), textureView, context,
                (AudioManager) context.getSystemService(Context.AUDIO_SERVICE));
    }

    /* Package private for testing */
    @VisibleForTesting
    MediaPlayer(@NonNull android.media.MediaPlayer mediaPlayer, @NonNull TextureView textureView,
                @NonNull Context context, @NonNull AudioManager audioManager) {
        mMediaPlayer = mediaPlayer;
        mTextureView = textureView;
        mAudioManager = audioManager;
        mContext = context;
        mHandler = new Handler(Looper.getMainLooper());
        mListeners = new LinkedList<>();
        try {
            mMediaPlayer.setAudioAttributes(AUDIO_ATTRIBUTES);
            mMediaPlayer.setOnErrorListener(mOnErrorListener);
            mMediaPlayer.setOnPreparedListener(mOnPreparedListener);
            mMediaPlayer.setOnSeekCompleteListener(mOnSeekCompleteListener);
            mMediaPlayer.setOnVideoSizeChangedListener(mOnVideoSizeChangedListener);
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        } catch (Exception e) {
            onPlayerError("Error while initializing media player.", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setAudioTrack(@NonNull AudioTrack audioTrack) {
        mAudioTrack = audioTrack;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setVideoScale(@NonNull VideoScale scale) {
        mScale = scale;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setMediaSources(@NonNull MediaSources mediaSource) {
        mSources = mediaSource;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addMediaStateListener(@NonNull IMediaListener listener) {
        mListeners.add(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeMediaStateListener(@NonNull IMediaListener listener) {
        mListeners.remove(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void play() {
        if (mAction == Action.PLAY) {
            return;
        }
        if (mAction == Action.SEEK) {
            return;
        }

        mAction = Action.PLAY;
        mWasPlaying = false;
        try {
            preparePlayer();
        } catch (Exception e) {
            onPlayerError("Media player error on play.", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void pause() {
        if (!isPlayerReady()) {
            return;
        }
        pauseInternal(true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        if (!isPlayerReady()) {
            return;
        }

        if (mAction == Action.STOP) {
            Log.e(TAG, "Stop already requested, ignoring this request.");
            return;
        }

        mAction = Action.STOP;
        mLoopCount = 0;
        try {
            releaseAudioFocus();
            stopInternal();
            notifyMediaState();
        } catch (Exception e) {
            onPlayerError("Media player error on stop.", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void next() {
        if (!isPlayerReady()) {
            return;
        }

        try {
            setTrackInternal(mCurrentTrackIndex + 1, Action.USER_TRACK_UPDATE);
        } catch (Exception e) {
            onPlayerError("Media player error next.", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void previous() {
        if (!isPlayerReady()) {
            return;
        }

        try {
            setTrackInternal(mCurrentTrackIndex - 1, Action.USER_TRACK_UPDATE);
        } catch (Exception e) {
            onPlayerError("Media player error on previous.", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setTrack(int trackIndex) {
        mAction = Action.USER_TRACK_UPDATE;
        try {
            setTrackInternal(trackIndex, Action.USER_TRACK_UPDATE);
        } catch (Exception e) {
            onPlayerError("Media player error on set track.", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void seek(int msec) {
        try {
            if (!isPlayerReady()) {
                mCurrentSeekPosition = msec;
                mAction = Action.SEEK;
                preparePlayer();
            } else {
                seekInternal(msec, mLoopCount, Action.SEEK);
            }
        } catch (Exception e) {
            onPlayerError("Media player error on seek", e);
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void rewind() {
        if (!isPlayerReady()) {
            return;
        }
        seekInternal(0, 0, Action.SEEK);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getCurrentTrackIndex() {
        return mCurrentTrackIndex;
    }

    @Override
    public int getTrackCount() {
        return mSources == null ? 0 : mSources.size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getDuration() {
        if (mSources != null && mCurrentTrackIndex < mSources.size() && isPlayerReady()) {
            MediaSource source = mSources.at(mCurrentTrackIndex);
            int trackDuration = source.duration() <= 0 ? Integer.MAX_VALUE :
                    source.duration();
            int playerDuration = mMediaPlayer.getDuration();
            return trackDuration < playerDuration ? trackDuration : playerDuration;
        } else
            return 0;
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public MediaState getCurrentMediaState() {
        return mCurrentState;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getCurrentSeekPosition() {
        if (isPlayerReady()) {
            mCurrentSeekPosition = mMediaPlayer.getCurrentPosition();
        }
        return mCurrentSeekPosition;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isPlaying() {
        return mMediaPlayer != null && mMediaPlayer.isPlaying();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void release() {
        if (mMediaPlayer == null) {
            return;
        }
        try {
            pauseInternal(true);
            stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
            mTextureView.setSurfaceTextureListener(null);
            mTextureView = null;
            if (mSurface != null) {
                mSurface.release();
                mSurface = null;
            }
            mCurrentState = MediaState.RELEASED;
            notifyMediaState();
            mListeners.clear();
        } catch (Exception e) {
            onPlayerError("Media player on release.", e);
        }
    }

    private void pauseInternal(boolean shouldReleaseAudioFocus) {
        mWasPlaying = !shouldReleaseAudioFocus && isPlaying();
        if (mAction == Action.PAUSE) {
            Log.e(TAG, "Pause already requested, ignoring this request.");
            return;
        }

        mAction = Action.PAUSE;
        try {
            if (shouldReleaseAudioFocus) {
                releaseAudioFocus();
            }
            stopProgressUpdateTask();
            mMediaPlayer.pause();
            mCurrentState = MediaState.PAUSED;
            notifyMediaState();
            mShouldNotifyProgress = false;
        } catch (Exception e) {
            onPlayerError("Media player error on pause.", e);
        }
    }

    private void setTrackInternal(int trackIndex, Action action)
            throws IOException {
        if (mSources == null || trackIndex < 0 || trackIndex >= mSources.size()) {
            return;
        }
        mAction = action;
        mLoopCount = 0;
        mCurrentTrackIndex = trackIndex;
        mCurrentState = MediaState.TRACK_UPDATE;
        notifyMediaState();
        if (isPlayerReady()) {
            stopInternal();
            preparePlayer();
        }
    }

    private void seekInternal(int position, int loopCount, Action action)
            throws IllegalStateException {
        mAction = action;
        mLoopCount = loopCount;
        MediaSource mediaSource = mSources.at(mCurrentTrackIndex);
        position += mediaSource.offset();
        int trackDuration = mediaSource.duration() <= 0 ?
                Integer.MAX_VALUE : mediaSource.duration();

        position = position >= trackDuration ? trackDuration : position;
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
            mMediaPlayer.seekTo(position, android.media.MediaPlayer.SEEK_CLOSEST);
        } else {
            mMediaPlayer.seekTo(position);
        }
    }

    private void stopInternal() {
        if (mCurrentState == MediaState.IDLE) {
            // already in idle state
            return;
        }
        stopProgressUpdateTask();
        mMediaPlayer.stop();
        mMediaPlayer.reset();
        mCurrentState = MediaState.IDLE;
    }

    private void preparePlayer() throws IllegalStateException, IOException {
        if (mSurface == null) {
            return;
        }
        if (mSources == null || mSources.size() == 0) {
            Log.e(TAG, "No source to play");
            return;
        }

        if (isPlayerReady()) {
            playInternal(true);
        } else if (mCurrentState != MediaState.PREPARING && mMediaPlayer != null) {
            stopInternal();
            MediaSource mediaSource = mSources.at(mCurrentTrackIndex);
            Map<String, String> headers = mediaSource.headers();
            setCompletionListener(mediaSource);
            if (headers.size() > 0) {
                mMediaPlayer.setDataSource(mContext, Uri.parse(mediaSource.url()), mediaSource.headers());
            } else {
                mMediaPlayer.setDataSource(mediaSource.url());
            }
            mMediaPlayer.setSurface(mSurface);
            mMediaPlayer.setLooping(mediaSource.repeatCount() == MediaSources.REPEAT_FOREVER);
            mMediaPlayer.prepareAsync();
            mCurrentState = MediaState.PREPARING;
        } else {
            Log.w(TAG, "Cannot prepare player or the player is already preparing itself");
        }
    }

    private void playInternal(boolean shouldNotifyState) throws IllegalStateException {
        requestAudioFocus();
        mMediaPlayer.start();
        startProgressUpdateTask();
        mCurrentState = MediaState.PLAYING;
        if (shouldNotifyState) {
            notifyMediaState();
        }
    }

    private boolean isPlayerReady() {
        return (mMediaPlayer != null &&
                mCurrentState != MediaState.ERROR &&
                mCurrentState != MediaState.IDLE &&
                mCurrentState != MediaState.PREPARING);
    }

    private void setCompletionListener(MediaSource mediaSource) {
        mMediaPlayer.setOnCompletionListener(mediaSource.duration() <= 0 ?
                mOnCompletionListener : null);
    }

    private void onTrackCompletion() {
        try {
            int repeatCount = mSources.at(mCurrentTrackIndex).repeatCount();
            if (mLoopCount < repeatCount) {
                seekInternal(0, mLoopCount + 1, Action.INTERNAL_TRACK_UPDATE); // keep looping the current track
            } else if (mCurrentTrackIndex + 1 >= mSources.size()) { // last track?
                mCurrentState = MediaState.END;
                notifyMediaState();
                stop();
            } else {
                setTrackInternal(mCurrentTrackIndex + 1, Action.INTERNAL_TRACK_UPDATE); // move on to the next track
            }
        } catch (Exception e) {
            onPlayerError("Error while updating track.", e);
        }
    }

    private void onPlayerError(String msg, @Nullable Exception e) {
        Log.e(TAG, msg);
        if (e != null) {
            e.printStackTrace();
        }

        mCurrentState = MediaState.ERROR;
        notifyMediaState();
    }

    private void requestAudioFocus() {
        if (mAudioTrack == AudioTrack.kAudioTrackNone) {
            // audio focus not needed and also mute the player
            setVolume(0);
            return;
        }

        if (mHasAudioFocus) {
            // already has focus
            return;
        }

        int response = mAudioManager.requestAudioFocus(mAudioFocusChangeListener,
                // Use the music stream.
                AudioManager.STREAM_MUSIC,
                // Request permanent focus.
                AudioManager.AUDIOFOCUS_GAIN);
        mHasAudioFocus = response == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }

    @Override
    public void releaseAudioFocus() {
        if (mHasAudioFocus) {
            mAudioManager.abandonAudioFocus(mAudioFocusChangeListener);
            mHasAudioFocus = false;
        }
    }

    private void setVolume(float volume) {
        mMediaPlayer.setVolume(volume, volume);
    }

    private void startProgressUpdateTask() {
        if (!mShouldNotifyProgress) {
            mHandler.postDelayed(mProgressUpdateTask, 0);
            mShouldNotifyProgress = true;
        }
    }

    private void stopProgressUpdateTask() {
        if (mShouldNotifyProgress) {
            mHandler.removeCallbacks(mProgressUpdateTask);
            mShouldNotifyProgress = false;
        }
    }

    private void notifyMediaState() {
        // create a new array to avoid a ConcurrentModificationException
        List<IMediaListener> listeners = new ArrayList<>(mListeners);
        for (IMediaListener listener : listeners) {
            listener.updateMediaState(this);
        }
    }

    @SuppressWarnings("FieldCanBeLocal")
    private final OnPreparedListener mOnPreparedListener = mp -> {
        mCurrentState = MediaState.READY;
        if (mAction == Action.PLAY || mAction == Action.INTERNAL_TRACK_UPDATE || mAction == Action.SEEK) {
            try {
                MediaSource mediaSource = mSources.at(mCurrentTrackIndex);
                if (mAction == Action.SEEK) {
                    mAction = Action.PLAY;
                    seekInternal(mCurrentSeekPosition, mLoopCount, mAction);
                } else if (mediaSource.offset() > 0) {
                    // pass offset=0 here, seekInternal handles the offset value by itself.
                    seekInternal(0, mLoopCount, mAction);
                } else {
                    playInternal(mAction == Action.PLAY);
                }
            } catch (Exception e) {
                onPlayerError("Media player error on starting playback.", e);
            }
        }
    };

    @SuppressWarnings("FieldCanBeLocal")
    private final OnSeekCompleteListener mOnSeekCompleteListener = mp -> {
        if (mAction == Action.PLAY || mAction == Action.INTERNAL_TRACK_UPDATE) {
            playInternal(mAction == Action.PLAY);
        } else {
            try {
                notifyMediaState();
            } catch (Exception e) {
                onPlayerError("Error on progress update", e);
            }
        }
    };

    @SuppressWarnings("FieldCanBeLocal")
    private final SurfaceTextureListener mSurfaceTextureListener = new SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
            mSurface = new Surface(texture);

            if (mAction == Action.PLAY || mAction == Action.SEEK) {
                try {
                    preparePlayer();
                } catch (Exception e) {
                    onPlayerError("Media player error on preparing track.", e);
                }
            }
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            // We handle releasing the surface in the release method called in onDocumentFinish.
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };

    /*
     * Code referred from https://github.com/google/grafika/blob/master/app/src/main/java/com/android/grafika/PlayMovieActivity.java
     */
    @SuppressWarnings("FieldCanBeLocal")
    private final OnVideoSizeChangedListener mOnVideoSizeChangedListener =
            (mp, videoWidth, videoHeight) -> {
                if (mTextureView == null) {
                    Log.e(TAG, "View not available, skipping scale adjustment.");
                }
                int viewWidth = mTextureView.getWidth();
                int viewHeight = mTextureView.getHeight();
                double aspectRatio = (double) videoHeight / videoWidth;
                int newWidth, newHeight;
                switch (mScale) {
                    case kVideoScaleBestFill: {
                        if (viewHeight > (int) (viewWidth * aspectRatio)) { // vertical view
                            newHeight = viewHeight;
                            newWidth = (int) (viewHeight / aspectRatio);
                        } else {    // horizontal view
                            newWidth = viewWidth;
                            newHeight = (int) (viewWidth * aspectRatio);
                        }
                        break;
                    }
                    default:
                    case kVideoScaleBestFit: {
                        if (viewHeight > (int) (viewWidth * aspectRatio)) { // vertical view
                            newWidth = viewWidth;
                            newHeight = (int) (viewWidth * aspectRatio);
                        } else {    // horizontal view
                            newWidth = (int) (viewHeight / aspectRatio);
                            newHeight = viewHeight;
                        }
                        break;
                    }
                }

                int pivotX = (viewWidth - newWidth) / 2;
                int pivotY = (viewHeight - newHeight) / 2;
                Matrix transform = new Matrix();
                mTextureView.getTransform(transform);
                transform.setScale((float) newWidth / viewWidth, (float) newHeight / viewHeight);
                transform.postTranslate(pivotX, pivotY);
                mTextureView.setTransform(transform);
            };

    private final OnCompletionListener mOnCompletionListener = mp -> onTrackCompletion();

    private final Runnable mProgressUpdateTask = () -> {
        try {
            notifyMediaState();
            if (mShouldNotifyProgress) {
                mHandler.postDelayed(this.mProgressUpdateTask, POSITION_UPDATE_INTERVAL_MS);
            }

            // check if the player has completed the track duration
            MediaSource mediaSource = mSources.at(mCurrentTrackIndex);
            if (mediaSource.duration() <= 0) {
                return;
            }
            int elapsedTime = mMediaPlayer.getCurrentPosition();
            int duration = mediaSource.duration();
            duration = mMediaPlayer.getDuration() < duration ? mMediaPlayer.getDuration() : duration;
            if (elapsedTime >= duration) {
                onTrackCompletion();
            }
        } catch (Exception e) {
            onPlayerError("Error on progress update", e);
        }
    };

    private final OnAudioFocusChangeListener mAudioFocusChangeListener = focusChange -> {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN: {
                setVolume(1f);
                if (mWasPlaying) {
                    play();
                }
                break;
            }
            case AudioManager.AUDIOFOCUS_LOSS:
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT: {
                pauseInternal(false);
                break;
            }
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK: {
                if (mAudioTrack == AudioTrack.kAudioTrackBackground) {
                    setVolume(ATTENUATED_VOLUME);
                } else {
                    pauseInternal(false);
                }
                break;
            }
        }
    };

    @SuppressWarnings("FieldCanBeLocal")
    private final OnErrorListener mOnErrorListener = (mp, what, extra) -> {
        String errorMsg;
        switch (what) {
            case android.media.MediaPlayer.MEDIA_ERROR_IO:
                errorMsg = "Network error.";
                break;
            case android.media.MediaPlayer.MEDIA_ERROR_MALFORMED:
                errorMsg = "Media bitstream does not conform to the related coding standard.";
                break;
            case android.media.MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
                errorMsg = "The video container is not valid for progressive playback.";
                break;
            case android.media.MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                errorMsg = "Media server died.";
                break;
            case android.media.MediaPlayer.MEDIA_ERROR_TIMED_OUT:
                errorMsg = "Time out.";
                break;
            case android.media.MediaPlayer.MEDIA_ERROR_UNSUPPORTED:
                errorMsg = "Unsupported media format.";
                break;
            case android.media.MediaPlayer.MEDIA_ERROR_UNKNOWN:
            default:
                errorMsg = "Unknown error.";
                break;
        }

        onPlayerError("Media player error. " + errorMsg + " extra code " + extra, null);
        try {
            mMediaPlayer.reset();
        } catch (Exception e) {
            Log.e(TAG, "Media player error on reset.");
        } finally {
            mCurrentState = MediaState.IDLE;
        }
        return true;
    };
}
