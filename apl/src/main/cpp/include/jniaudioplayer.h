/**
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

#ifndef ANDROID_JNIAUDIOPLAYER_H
#define ANDROID_JNIAUDIOPLAYER_H

#include "apl/apl.h"

#ifdef __cplusplus
extern "C" {
#endif

/**
 *  Initialize and cache java class and method handles for callback to the rendering layer.
 */
jboolean
audioplayer_OnLoad(JavaVM *vm, void *reserved);

/**
 * Release the class and method cache.
 */
void
audioplayer_OnUnload(JavaVM *vm, void *reserved);

namespace apl {
    namespace jni {

        class AndroidAudioPlayer : public AudioPlayer {
        public:
            AndroidAudioPlayer(AudioPlayerCallback playerCallback,
                               SpeechMarkCallback speechMarkCallback);

            void setInstance(jobject instance);

            /// apl::AudioPlayer methods
            void release() override;

            void setTrack(MediaTrack track) override;

            void play(ActionRef actionRef) override;

            void pause() override;

            bool isActive();

            void resolveExistingAction();

            void onSpeechMark(const std::vector <SpeechMark> &speechMark);

            void onStateUpdate(AudioPlayerEventType eventType, const AudioState &audioState);

        private:
            jobject mInstance;
            ActionRef mPlayRef;
        };

    } // namespace jni
} // namespace apl

#ifdef __cplusplus
}
#endif
#endif //ANDROID_JNIAUDIOPLAYER_H