/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.media;

import com.amazon.apl.android.dependencies.IMediaPlayer;
import com.amazon.apl.android.primitive.MediaSources;
import com.amazon.apl.android.utils.HttpUtils;
import com.amazon.apl.enums.AudioTrack;

import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class LocalMediaPlayer extends MediaPlayer {
    private static final String TAG = "LocalMediaPlayer";
    private WeakReference<IMediaPlayer> mWeakPlayer;
    private final Queue<Runnable> mQueuedTasks;

    public LocalMediaPlayer(long nativeHandle) {
        super(nativeHandle);
        mQueuedTasks = new LinkedList<>();
    }

    @Override
    public void play() {
        runOrDeferFunction("play", () -> {
            IMediaPlayer player = mWeakPlayer.get();
            if (player != null) {
                player.play();
            }
        });
    }

    @Override
    public void pause() {
        runOrDeferFunction("pause", () -> {
            IMediaPlayer player = mWeakPlayer.get();
            if (player != null) {
                player.pause();
            }
        });
    }

    @Override
    public void next() {
        runOrDeferFunction("next", () -> {
            IMediaPlayer player = mWeakPlayer.get();
            if (player != null) {
                player.next();
            }
        });
    }

    @Override
    public void previous() {
        runOrDeferFunction("previous", () -> {
            IMediaPlayer player = mWeakPlayer.get();
            if (player != null) {
                player.previous();
            }
        });
    }

    @Override
    public void rewind() {
        runOrDeferFunction("rewind", () -> {
            IMediaPlayer player = mWeakPlayer.get();
            if (player != null) {
                player.rewind();
            }
        });
    }

    @Override
    public void seek(int offset) {
        runOrDeferFunction("seek", () -> {
            IMediaPlayer player = mWeakPlayer.get();
            if (player != null) {
                player.seek(player.getCurrentSeekPosition() + offset);
            }
        });
    }

    @Override
    public void setTrackList(List<MediaTrack> trackList) {
        MediaSources sources = MediaSources.create();
        for (MediaTrack mediaTrack : trackList) {
            MediaSources.MediaSource source = MediaSources.MediaSource.builder()
                    .url(mediaTrack.getUrl())
                    .duration(mediaTrack.getDuration())
                    .offset(mediaTrack.getOffset())
                    .repeatCount(mediaTrack.getRepeatCount())
                    .headers(HttpUtils.listToHeadersMap(mediaTrack.getHeaders()))
                    .build();
            sources.add(source);
        }
        runOrDeferFunction("setTrackList", () -> {
            IMediaPlayer player = mWeakPlayer.get();
            if (player != null) {
                player.setMediaSources(sources);
                if (player.getCurrentMediaState() == MediaState.PLAYING) {
                    player.setTrack(0);
                }
            }
        });
    }

    @Override
    public void setTrackIndex(int trackIndex) {
        runOrDeferFunction("setTrackIndex", () -> {
            IMediaPlayer player = mWeakPlayer.get();
            if (player != null) {
                player.setTrack(trackIndex);
            }
        });
    }

    @Override
    public void release() {
        runOrDeferFunction("release", () -> {
            IMediaPlayer player = mWeakPlayer.get();
            if (player != null) {
                player.stop();
                player.release();
            }
        });
    }

    @Override
    public void setAudioTrack(int index) {
        runOrDeferFunction("setAudioTrack", () -> {
            IMediaPlayer player = mWeakPlayer.get();
            if (player != null) {
                player.setAudioTrack(AudioTrack.valueOf(index));
            }
        });
    }

    @Override
    public void setMute(boolean mute) {
        runOrDeferFunction("setMute", () -> {
            IMediaPlayer player = mWeakPlayer.get();
            if (player != null) {
                if (mute) {
                    player.mute();
                } else {
                    player.unmute();
                }
            }
        });
    }

    @Override
    public IMediaPlayer getMediaPlayer() {
        if (mWeakPlayer == null) {
            return null;
        }
        return mWeakPlayer.get();
    }

    @Override
    public void setMediaPlayer(IMediaPlayer player) {
        mWeakPlayer = new WeakReference<>(player);
        player.addMediaStateListener(this);
        runQueuedOperations();
    }

    private void runOrDeferFunction(String functionName, Runnable task) {
        // So queue all the operations until MediaPlayer is available.
        if (mWeakPlayer != null) {
            task.run();
        } else {
            mQueuedTasks.add(task);
        }
    }

    private void runQueuedOperations() {
        Runnable task = mQueuedTasks.poll();
        while (task != null) {
            task.run();
            task = mQueuedTasks.poll();
        }
    }
}
