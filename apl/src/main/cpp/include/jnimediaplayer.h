/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

#ifndef APLVIEWHOSTANDROID_JNIMEDIAPLAYER_H
#define APLVIEWHOSTANDROID_JNIMEDIAPLAYER_H
#include "apl/apl.h"

#ifdef __cplusplus
extern "C" {
#endif
/**
 * Initialize and cache java class and method handles for callback to the rendering layer.
 */
jboolean mediaplayer_OnLoad(JavaVM *vm, void *reserved __attribute__((__unused__)));

/**
 * Release the class and method cache.
 */
void mediaplayer_OnUnload(JavaVM *vm, void *reserved __attribute__((__unused__)));

namespace apl {
    namespace jni {
        class AndroidMediaPlayer : public MediaPlayer {
        public:
            AndroidMediaPlayer(MediaPlayerCallback callback);

            void setInstance(jobject instance);

            jobject getInstance() { return mInstance; }

            /// apl::MediaPlayer methods
            void release() override;

            void halt() override;

            void setTrackList(std::vector<MediaTrack> tracks) override;

            void play(ActionRef actionRef) override;

            void pause() override;

            void next() override;

            void previous() override;

            void rewind() override;

            void seek( int offset ) override;

            void setTrackIndex(int trackIndex) override;

            void setAudioTrack(AudioTrack audioTrack) override;

            void setMute(bool mute) override;

            void updateMediaState(MediaPlayerEventType eventType, MediaState &mediaState);

            void resolveExistingAction();

            /**
             * @return @c true if this player is currently able to play video media, @c false otherwise.
             */
            bool isActive() const;

        private:
            jobject mInstance;
            ActionRef mActionRef;
            /// The index of the current track.  This is always valid unless there are no defined media tracks.
            int mTrackIndex = 0;
            bool mReleased = false; // Set when the media player is released and should not be used
            bool mHalted = false;   // Set when the media player was asked to halt all playback
            AudioTrack mAudioTrack;
        };
    } // namespace jni
} // namespace apl

#ifdef __cplusplus
}
#endif
#endif //APLVIEWHOSTANDROID_JNIMEDIAPLAYER_H
