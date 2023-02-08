/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

#include <jni.h>
#include <locale>
#include <codecvt>
#include "jnicomplexproperty.h"
#include "jniaudioplayer.h"
#include "jniutil.h"
#include "jnicontent.h"

namespace apl {
    namespace jni {
#ifdef __cplusplus
        extern "C" {
#endif

        static jclass AUDIOPLAYER_CLASS;
        static jclass MEDIATRACK_CLASS;
        static jmethodID MEDIATRACK_CONSTRUCTOR;
        static jmethodID AUDIOPLAYER_SET_TRACK;
        static jmethodID AUDIOPLAYER_RELEASE;
        static jmethodID AUDIOPLAYER_PLAY;
        static jmethodID AUDIOPLAYER_PAUSE;
        static JavaVM *AUDIOPLAYER_VM_REFERENCE;

        /**
         * Create a class and method cache for calls to View Host.
         */
        jboolean
        audioplayer_OnLoad(JavaVM *vm, void *reserved) {

            LOG(apl::LogLevel::kDebug) << "Loading View Host Audio Player JNI environment.";

            JNIEnv *env;
            if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
                LOG(apl::LogLevel::kError) << "Environment failure, cannot proceed";
                return JNI_FALSE;
            }

            AUDIOPLAYER_VM_REFERENCE = vm;

            AUDIOPLAYER_CLASS = reinterpret_cast<jclass>(env->NewGlobalRef(
                    env->FindClass("com/amazon/apl/android/audio/AudioPlayer")));

            MEDIATRACK_CLASS = reinterpret_cast<jclass>(env->NewGlobalRef(
                    env->FindClass("com/amazon/apl/android/media/MediaTrack")));

            MEDIATRACK_CONSTRUCTOR = env->GetMethodID(MEDIATRACK_CLASS,
                                                      "<init>",
                                                      "(Ljava/lang/String;[Ljava/lang/String;III[Lcom/amazon/apl/android/media/TextTrack;)V");

            AUDIOPLAYER_SET_TRACK = env->GetMethodID(
                    AUDIOPLAYER_CLASS,
                    "setTrack",
                    "(Lcom/amazon/apl/android/media/MediaTrack;)V");

            AUDIOPLAYER_RELEASE = env->GetMethodID(
                    AUDIOPLAYER_CLASS,
                    "release",
                    "()V");

            AUDIOPLAYER_PLAY = env->GetMethodID(
                    AUDIOPLAYER_CLASS,
                    "play",
                    "()V");

            AUDIOPLAYER_PAUSE = env->GetMethodID(
                    AUDIOPLAYER_CLASS,
                    "pause",
                    "()V");

            if (nullptr == AUDIOPLAYER_VM_REFERENCE
                || nullptr == AUDIOPLAYER_SET_TRACK
                || nullptr == AUDIOPLAYER_RELEASE
                || nullptr == AUDIOPLAYER_PLAY
                || nullptr == AUDIOPLAYER_PAUSE) {
                return JNI_FALSE;
            }

            return JNI_TRUE;
        }

        /**
         * Release the class and method cache.
         */
        void
        audioplayer_OnUnload(JavaVM *vm, void *reserved) {
            LOG(apl::LogLevel::kDebug) << "Unloading View Host Audio Player JNI environment.";
            apl::LoggerFactory::instance().reset();

            JNIEnv *env;
            if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
                LOG(apl::LogLevel::kError) << "Environment failure, cannot proceed";
                return;
            }

            env->DeleteGlobalRef(AUDIOPLAYER_CLASS);
        }

        AndroidAudioPlayer::AndroidAudioPlayer(
                AudioPlayerCallback playerCallback, SpeechMarkCallback speechMarkCallback) :
                AudioPlayer(std::move(playerCallback), std::move(speechMarkCallback)),
                mPlayRef(ActionRef(nullptr)) {}

        void
        AndroidAudioPlayer::release() {
            JNIEnv *env;
            if (AUDIOPLAYER_VM_REFERENCE->GetEnv(reinterpret_cast<void **>(&env),
                                                 JNI_VERSION_1_6) != JNI_OK) {
                return;
            }

            resolveExistingAction();
            env->CallVoidMethod(mInstance, AUDIOPLAYER_RELEASE);
            env->DeleteGlobalRef(mInstance);
        }

        void
        AndroidAudioPlayer::setTrack(MediaTrack track) {
            JNIEnv *env;
            if (AUDIOPLAYER_VM_REFERENCE->GetEnv(reinterpret_cast<void **>(&env),
                                                 JNI_VERSION_1_6) != JNI_OK) {
                LOG(apl::LogLevel::kError) << "Environment failure, cannot proceed";
                return;
            }

            auto url = env->NewStringUTF(track.url.c_str());
            const auto &headers = track.headers;
            auto trackObj = env->NewObject(MEDIATRACK_CLASS, MEDIATRACK_CONSTRUCTOR, url,
                                           getStringArray(env, headers), track.offset, track.duration,
                                           track.repeatCount, nullptr);

            env->CallVoidMethod(mInstance, AUDIOPLAYER_SET_TRACK, trackObj);
            env->DeleteLocalRef(url);
            env->DeleteLocalRef(trackObj);
        }

        void
        AndroidAudioPlayer::play(ActionRef actionRef) {
            JNIEnv *env;
            if (AUDIOPLAYER_VM_REFERENCE->GetEnv(reinterpret_cast<void **>(&env),
                                                 JNI_VERSION_1_6) != JNI_OK) {
                LOG(apl::LogLevel::kError) << "Environment failure, cannot proceed";
                return;
            }

            resolveExistingAction();
            if (!actionRef.empty()) {
                mPlayRef = actionRef;
                mPlayRef.addTerminateCallback([&](const apl::TimersPtr&) { mPlayRef = apl::ActionRef(nullptr); });
            } else {
                actionRef.resolve();
            }

            env->CallVoidMethod(mInstance, AUDIOPLAYER_PLAY);
        }

        void
        AndroidAudioPlayer::pause() {
            JNIEnv *env;
            if (AUDIOPLAYER_VM_REFERENCE->GetEnv(reinterpret_cast<void **>(&env),
                                                 JNI_VERSION_1_6) != JNI_OK) {
                LOG(apl::LogLevel::kError) << "Environment failure, cannot proceed";
                return;
            }

            resolveExistingAction();
            env->CallVoidMethod(mInstance, AUDIOPLAYER_PAUSE);
        }

        bool
        AndroidAudioPlayer::isActive() {
            return !mPlayRef.empty() && (!mPlayRef.isResolved() && !mPlayRef.isTerminated());
        }

        void
        AndroidAudioPlayer::resolveExistingAction() {
            if (!mPlayRef.empty() && mPlayRef.isPending()) {
                mPlayRef.resolve();
            }
            mPlayRef = apl::ActionRef(nullptr);
        }

        void
        AndroidAudioPlayer::setInstance(jobject instance) {
            JNIEnv *env;
            if (AUDIOPLAYER_VM_REFERENCE->GetEnv(reinterpret_cast<void **>(&env),
                                                 JNI_VERSION_1_6) != JNI_OK) {
                LOG(apl::LogLevel::kError) << "Environment failure, cannot proceed";
                return;
            }

            // TODO consider refactoring this such that the Java instance is passed into the constructor
            mInstance = env->NewGlobalRef(instance);
        }

        void
        AndroidAudioPlayer::onSpeechMark(const std::vector<SpeechMark> &speechMark) {
            if (mSpeechMarkCallback) {
                mSpeechMarkCallback(speechMark);
            }
        }

        void
        AndroidAudioPlayer::onStateUpdate(AudioPlayerEventType eventType,
                                          const AudioState &audioState) {
            if (!isActive()) return;
            mPlayerCallback(eventType, audioState);
            if (eventType == AudioPlayerEventType::kAudioPlayerEventEnd && audioState.isEnded()) {
                resolveExistingAction();
            }
        }

        /**
         * Calls the core layer for AudioPlayer Speech Mark.
         */
        JNIEXPORT void JNICALL
        Java_com_amazon_apl_android_audio_AudioPlayer_nSpeechMark(JNIEnv *env,
                                                                  jclass clazz,
                                                                  jlong handle, jint type,
                                                                  jint start, jint end, jlong time,
                                                                  jstring value) {
            auto audioPlayer = get<AndroidAudioPlayer>(handle);
            const char *valueStr = env->GetStringUTFChars(value, nullptr);
            SpeechMark sm = {
                    static_cast<SpeechMarkType>(type),
                    static_cast<unsigned int>(start),
                    static_cast<unsigned int>(end),
                    static_cast<unsigned long>(time),
                    std::string(valueStr)
            };
            auto marks = std::vector<SpeechMark>();
            marks.push_back(sm);
            audioPlayer->onSpeechMark(marks);
            env->ReleaseStringUTFChars(value, valueStr);
        }

        /**
         * Calls the core layer for AudioPlayer state change.
         */
        JNIEXPORT void JNICALL
        Java_com_amazon_apl_android_audio_AudioPlayer_nStateChange(JNIEnv *env,
                                                                   jclass clazz,
                                                                   jlong handle, jint type,
                                                                   jint current_time, jint duration,
                                                                   jboolean paused, jboolean ended,
                                                                   jint trackState) {
            auto audioPlayer = get<AndroidAudioPlayer>(handle);
            auto state = AudioState(current_time, duration, paused, ended,
                                    static_cast<TrackState>(trackState));
            audioPlayer->onStateUpdate(static_cast<AudioPlayerEventType>(type), state);
        }

#ifdef __cplusplus
        }
#endif
    } //namespace jni
} //namespace apl