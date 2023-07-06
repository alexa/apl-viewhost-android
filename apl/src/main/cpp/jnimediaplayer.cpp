/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

#include <jni.h>
#include <locale>
#include <codecvt>
#include "jnicomplexproperty.h"
#include "jnimediaplayer.h"
#include "jniutil.h"
#include "jnicontent.h"

namespace apl {
    namespace jni {
#ifdef __cplusplus
        extern "C" {
#endif

        static jclass MEDIAPLAYER_CLASS;
        static jclass MEDIATRACK_CLASS;
        static jclass TEXTTRACK_CLASS;
        static jclass TEXTTYPE_CLASS;
        static jmethodID MEDIAPLAYER_RELEASE;
        static jmethodID MEDIAPLAYER_PLAY;
        static jmethodID MEDIAPLAYER_PAUSE;
        static jmethodID MEDIAPLAYER_NEXT;
        static jmethodID MEDIAPLAYER_PREVIOUS;
        static jmethodID MEDIAPLAYER_REWIND;
        static jmethodID MEDIAPLAYER_SEEK;
        static jmethodID MEDIAPLAYER_SEEK_TO;
        static jmethodID MEDIAPLAYER_MUTE;
        static jmethodID MEDIAPLAYER_UNMUTE;
        static jmethodID MEDIATRACK_CONSTRUCTOR;
        static jmethodID TEXTTRACK_CONSTRUCTOR;
        static jmethodID TEXTTYPE_VALUEOF;
        static jmethodID MEDIAPLAYER_SET_TRACK_LIST;
        static jmethodID MEDIAPLAYER_SET_TRACK_INDEX;
        static jmethodID MEDIAPLAYER_SET_AUDIO_TRACK;
        static jmethodID MEDIAPLAYER_SET_MUTE;
        static JavaVM *MEDIAPLAYER_VM_REFERENCE;
        static const bool DEBUG_MEDIA_PLAYER = false;
        /**
         * Create a class and method cache for calls to View Host.
        */
        jboolean mediaplayer_OnLoad(JavaVM *vm, void *reserved) {
            LOG(apl::LogLevel::kDebug) << "Loading View Host Media Player JNI environment.";

            JNIEnv *env;
            if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
                LOG(apl::LogLevel::kError) << "Environment failure, cannot proceed";
                return JNI_FALSE;
            }
            JAVA_LANG_STRING = reinterpret_cast<jclass>(env->NewGlobalRef(env->FindClass("java/lang/String")));
            MEDIAPLAYER_VM_REFERENCE = vm;
            MEDIAPLAYER_CLASS = reinterpret_cast<jclass>(env->NewGlobalRef(
                    env->FindClass("com/amazon/apl/android/media/MediaPlayer")));
            MEDIATRACK_CLASS = reinterpret_cast<jclass>(env->NewGlobalRef(
                    env->FindClass("com/amazon/apl/android/media/MediaTrack")));
            TEXTTRACK_CLASS = reinterpret_cast<jclass>(env->NewGlobalRef(
                    env->FindClass("com/amazon/apl/android/media/TextTrack")));

            MEDIATRACK_CONSTRUCTOR = env->GetMethodID(MEDIATRACK_CLASS, "<init>",
                                                      "(Ljava/lang/String;[Ljava/lang/String;III[Lcom/amazon/apl/android/media/TextTrack;)V");
            TEXTTRACK_CONSTRUCTOR = env->GetMethodID(TEXTTRACK_CLASS, "<init>",
                                                     "(ILjava/lang/String;Ljava/lang/String;)V");
            MEDIAPLAYER_SET_TRACK_LIST = env->GetMethodID(MEDIAPLAYER_CLASS, "setTrackList",
                                                          "(Ljava/util/List;)V");
            MEDIAPLAYER_SET_TRACK_INDEX = env->GetMethodID(MEDIAPLAYER_CLASS, "setTrackIndex",
                                                           "(I)V");
            MEDIAPLAYER_SET_AUDIO_TRACK = env->GetMethodID(MEDIAPLAYER_CLASS, "setAudioTrack",
                                                           "(I)V");
            MEDIAPLAYER_RELEASE = env->GetMethodID(MEDIAPLAYER_CLASS, "release","()V");
            MEDIAPLAYER_PLAY = env->GetMethodID(MEDIAPLAYER_CLASS, "play","()V");
            MEDIAPLAYER_PAUSE = env->GetMethodID(MEDIAPLAYER_CLASS, "pause","()V");
            MEDIAPLAYER_NEXT = env->GetMethodID(MEDIAPLAYER_CLASS, "next","()V");
            MEDIAPLAYER_PREVIOUS = env->GetMethodID(MEDIAPLAYER_CLASS, "previous","()V");
            MEDIAPLAYER_REWIND = env->GetMethodID(MEDIAPLAYER_CLASS, "rewind","()V");
            MEDIAPLAYER_SEEK = env->GetMethodID(MEDIAPLAYER_CLASS, "seek", "(I)V");
            MEDIAPLAYER_SEEK_TO = env->GetMethodID(MEDIAPLAYER_CLASS, "seekTo", "(I)V");
            MEDIAPLAYER_SET_MUTE = env->GetMethodID(MEDIAPLAYER_CLASS, "setMute", "(Z)V");

            if (nullptr == MEDIAPLAYER_VM_REFERENCE
                || nullptr == MEDIATRACK_CONSTRUCTOR
                || nullptr == TEXTTRACK_CONSTRUCTOR
                || nullptr == MEDIAPLAYER_SET_TRACK_LIST
                || nullptr == MEDIAPLAYER_SET_TRACK_INDEX
                || nullptr == MEDIAPLAYER_SET_AUDIO_TRACK
                || nullptr == MEDIAPLAYER_RELEASE
                || nullptr == MEDIAPLAYER_PLAY
                || nullptr == MEDIAPLAYER_PAUSE
                || nullptr == MEDIAPLAYER_NEXT
                || nullptr == MEDIAPLAYER_PREVIOUS
                || nullptr == MEDIAPLAYER_REWIND
                || nullptr == MEDIAPLAYER_SEEK
                || nullptr == MEDIAPLAYER_SEEK_TO
                || nullptr == MEDIAPLAYER_SET_MUTE) {
                return JNI_FALSE;
            }

            return JNI_TRUE;
        }

        /**
         * Release the class and method cache.
         */
        void mediaplayer_OnUnload(JavaVM *vm, void *reserved) {
            LOG(apl::LogLevel::kDebug) << "Unloading View Host Media Player JNI environment.";
            apl::LoggerFactory::instance().reset();

            JNIEnv *env;
            if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
                LOG(apl::LogLevel::kError) << "Environment failure, cannot proceed";
                return;
            }

            env->DeleteGlobalRef(MEDIAPLAYER_CLASS);
        }

        JNIEXPORT void JNICALL
        Java_com_amazon_apl_android_media_MediaPlayer_nUpdateMediaState(JNIEnv *env, jclass clazz, jlong handle,
                                                                        jint jTrackIndex, jint jTrackCount, jint jCurrentTime, jint jDuration,
                                                                        jboolean jPaused, jboolean jEnded, jboolean jMuted, jint trackState,
                                                                        jint errorCode, jint eventType) {
            auto mediaplayer = get<AndroidMediaPlayer>(handle);
            int trackIndex = static_cast<int>(jTrackIndex);
            int trackCount = static_cast<int>(jTrackCount);
            int currentTime = static_cast<int>(jCurrentTime);
            int duration = static_cast<int>(jDuration);
            bool paused = static_cast<bool>(jPaused);
            bool ended = static_cast<bool>(jEnded);
            bool muted = static_cast<bool>(jMuted);
            MediaState state = MediaState(trackIndex, trackCount, currentTime,
                                          duration, paused, ended, muted)
                    .withTrackState(static_cast<TrackState>(trackState));
            if(state.isError()) {
                state.withErrorCode(errorCode);
            }
            mediaplayer->updateMediaState(static_cast<MediaPlayerEventType>(eventType), state);
        }

        AndroidMediaPlayer::AndroidMediaPlayer(MediaPlayerCallback playerCallback) :
                MediaPlayer(std::move(playerCallback)),
                mActionRef(ActionRef(nullptr)) {}

        void
        AndroidMediaPlayer::release() {
            JNIEnv *env;
            if (MEDIAPLAYER_VM_REFERENCE->GetEnv(reinterpret_cast<void **>(&env),
                                                 JNI_VERSION_1_6) != JNI_OK) {
                LOG(apl::LogLevel::kError) << "Environment failure, cannot proceed";
                return;
            }

            resolveExistingAction();
            mReleased = true;
            env->CallVoidMethod(mInstance, MEDIAPLAYER_RELEASE);
            env->DeleteGlobalRef(mInstance);
        }

        void
        AndroidMediaPlayer::halt() {
            if (!isActive()) return;
            resolveExistingAction();
            mHalted = true;
        }

        void
        AndroidMediaPlayer::setTrackList(std::vector<MediaTrack> trackList) {
            JNIEnv *env;
            if (MEDIAPLAYER_VM_REFERENCE->GetEnv(reinterpret_cast<void **>(&env),
                                                 JNI_VERSION_1_6) != JNI_OK) {
                LOG(apl::LogLevel::kError) << "Environment failure, cannot proceed";
                return;
            }

            if (!isActive()) return;

            resolveExistingAction();

            jclass clazz = env->FindClass("java/util/ArrayList");
            jobject arrayObject = env->NewObject(clazz, env->GetMethodID(clazz, "<init>", "()V"));
            for (auto &track : trackList) {
                auto url = env->NewStringUTF(track.url.c_str());
                const auto &headers = track.headers;
                int trackSize = track.textTracks.size();
                auto textTrackObjectArray = env->NewObjectArray(trackSize, TEXTTRACK_CLASS,
                                                                nullptr);
                for (int i = 0; i < trackSize; i++) {
                    auto textUrl = env->NewStringUTF(track.textTracks[i].url.c_str());
                    auto textDescription = env->NewStringUTF(
                            track.textTracks[i].description.c_str());
                    int textType = static_cast<int>(track.textTracks[i].type);
                    auto textTrackObj = env->NewObject(TEXTTRACK_CLASS, TEXTTRACK_CONSTRUCTOR,
                                                       textType,
                                                       textUrl, textDescription);
                    env->SetObjectArrayElement(textTrackObjectArray, i, textTrackObj);
                    env->DeleteLocalRef(textUrl);
                    env->DeleteLocalRef(textDescription);
                    env->DeleteLocalRef(textTrackObj);
                }

                auto trackObj = env->NewObject(MEDIATRACK_CLASS, MEDIATRACK_CONSTRUCTOR, url,
                                               getStringArray(env, headers), track.offset,
                                               track.duration, track.repeatCount,
                                               textTrackObjectArray);
                env->CallBooleanMethod(arrayObject,
                                       env->GetMethodID(clazz, "add", "(Ljava/lang/Object;)Z"),
                                       trackObj);
                env->DeleteLocalRef(url);
                env->DeleteLocalRef(textTrackObjectArray);
                env->DeleteLocalRef(trackObj);
            }
            env->CallVoidMethod(mInstance, MEDIAPLAYER_SET_TRACK_LIST, arrayObject);
            env->DeleteLocalRef(arrayObject);
        }

        void
        AndroidMediaPlayer::play(ActionRef actionRef) {
            JNIEnv *env;
            if (MEDIAPLAYER_VM_REFERENCE->GetEnv(reinterpret_cast<void **>(&env),
                                                 JNI_VERSION_1_6) != JNI_OK) {
                LOG(apl::LogLevel::kError) << "Environment failure, cannot proceed";
                return;
            }

            if (!isActive()) {
                LOG_IF(DEBUG_MEDIA_PLAYER) << "Cannot Play: Media player not active";
                if (!actionRef.empty()) actionRef.resolve();
                return;
            }
            resolveExistingAction();
            if (!actionRef.empty()) {
                // Only hold onto the ActionRef in foreground mode
                if (mAudioTrack == apl::kAudioTrackForeground) {
                    mActionRef = actionRef;

                    // On a termination we need to discard the action reference or there is a memory cycle
                    mActionRef.addTerminateCallback(
                            [&](const apl::TimersPtr&) { mActionRef = apl::ActionRef(nullptr); });
                }
                else {
                    actionRef.resolve();
                }
            }

            env->CallVoidMethod(mInstance, MEDIAPLAYER_PLAY);
        }

        void
        AndroidMediaPlayer::pause() {
            JNIEnv *env;
            if (MEDIAPLAYER_VM_REFERENCE->GetEnv(reinterpret_cast<void **>(&env),
                                                 JNI_VERSION_1_6) != JNI_OK) {
                LOG(apl::LogLevel::kError) << "Environment failure, cannot proceed";
                return;
            }

            if (!isActive()) return;
            resolveExistingAction();

            env->CallVoidMethod(mInstance, MEDIAPLAYER_PAUSE);
        }

        void
        AndroidMediaPlayer::next() {
            JNIEnv *env;
            if (MEDIAPLAYER_VM_REFERENCE->GetEnv(reinterpret_cast<void **>(&env),
                                                 JNI_VERSION_1_6) != JNI_OK) {
                LOG(apl::LogLevel::kError) << "Environment failure, cannot proceed";
                return;
            }

            if (!isActive()) return;
            resolveExistingAction();

            env->CallVoidMethod(mInstance, MEDIAPLAYER_NEXT);
        }

        void
        AndroidMediaPlayer::previous() {
            JNIEnv *env;
            if (MEDIAPLAYER_VM_REFERENCE->GetEnv(reinterpret_cast<void **>(&env),
                                                 JNI_VERSION_1_6) != JNI_OK) {
                LOG(apl::LogLevel::kError) << "Environment failure, cannot proceed";
                return;
            }

            if (!isActive()) return;
            resolveExistingAction();

            env->CallVoidMethod(mInstance, MEDIAPLAYER_PREVIOUS);
        }

        void
        AndroidMediaPlayer::rewind() {
            JNIEnv *env;
            if (MEDIAPLAYER_VM_REFERENCE->GetEnv(reinterpret_cast<void **>(&env),
                                                 JNI_VERSION_1_6) != JNI_OK) {
                LOG(apl::LogLevel::kError) << "Environment failure, cannot proceed";
                return;
            }

            if (!isActive()) return;
            resolveExistingAction();

            env->CallVoidMethod(mInstance, MEDIAPLAYER_REWIND);
        }

        void
        AndroidMediaPlayer::seek(int offset) {
            JNIEnv *env;
            if (MEDIAPLAYER_VM_REFERENCE->GetEnv(reinterpret_cast<void **>(&env),
                                                 JNI_VERSION_1_6) != JNI_OK) {
                LOG(apl::LogLevel::kError) << "Environment failure, cannot proceed";
                return;
            }

            if (!isActive()) return;
            resolveExistingAction();

            env->CallVoidMethod(mInstance, MEDIAPLAYER_SEEK, offset);
        }

        void
        AndroidMediaPlayer::seekTo(int offset) {
            JNIEnv *env;
            if (MEDIAPLAYER_VM_REFERENCE->GetEnv(reinterpret_cast<void **>(&env),
                                                 JNI_VERSION_1_6) != JNI_OK) {
                LOG(apl::LogLevel::kError) << "Environment failure, cannot proceed";
                return;
            }

            if (!isActive()) return;
            resolveExistingAction();

            env->CallVoidMethod(mInstance, MEDIAPLAYER_SEEK_TO, offset);
        }

        void
        AndroidMediaPlayer::setTrackIndex(int trackIndex) {
            JNIEnv *env;
            if (MEDIAPLAYER_VM_REFERENCE->GetEnv(reinterpret_cast<void **>(&env),
                                                 JNI_VERSION_1_6) != JNI_OK) {
                LOG(apl::LogLevel::kError) << "Environment failure, cannot proceed";
                return;
            }

            if (!isActive()) return;
            resolveExistingAction();

            env->CallVoidMethod(mInstance, MEDIAPLAYER_SET_TRACK_INDEX, trackIndex);
        }

        void
        AndroidMediaPlayer::setAudioTrack(AudioTrack audioTrack) {
            JNIEnv *env;
            if (MEDIAPLAYER_VM_REFERENCE->GetEnv(reinterpret_cast<void **>(&env),
                                                 JNI_VERSION_1_6) != JNI_OK) {
                LOG(apl::LogLevel::kError) << "Environment failure, cannot proceed";
                return;
            }

            if (!isActive()) {
                return;
            }

            mAudioTrack = audioTrack;
            env->CallVoidMethod(mInstance, MEDIAPLAYER_SET_AUDIO_TRACK, static_cast<int>(audioTrack));
        }

        void
        AndroidMediaPlayer::setMute(bool mute) {
            JNIEnv *env;
            if (MEDIAPLAYER_VM_REFERENCE->GetEnv(reinterpret_cast<void **>(&env),
                                                 JNI_VERSION_1_6) != JNI_OK) {
                LOG(apl::LogLevel::kError) << "Environment failure, cannot proceed";
                return;
            }

            if (!isActive()) {
                return;
            }

            env->CallVoidMethod(mInstance, MEDIAPLAYER_SET_MUTE, mute);
        }

        void
        AndroidMediaPlayer::updateMediaState(MediaPlayerEventType eventType,
                                             MediaState &mediaState) {
            if (mediaState.isEnded())
                resolveExistingAction();
            if (!isActive()) {
                return;
            }
            mCallback(eventType, mediaState);
        }

        void
        AndroidMediaPlayer::resolveExistingAction() {
            if (!mActionRef.empty() && mActionRef.isPending()) {
                mActionRef.resolve();
            }
            mActionRef = apl::ActionRef(nullptr);
        }

        bool
        AndroidMediaPlayer::isActive() const {
            return !mReleased && !mHalted;
        }

        void
        AndroidMediaPlayer::setInstance(jobject instance) {
            JNIEnv *env;
            if (MEDIAPLAYER_VM_REFERENCE->GetEnv(reinterpret_cast<void **>(&env),
                                                 JNI_VERSION_1_6) != JNI_OK) {
                LOG(apl::LogLevel::kError) << "Environment failure, cannot proceed";
                return;
            }

            mInstance = env->NewGlobalRef(instance);
        }
#ifdef __cplusplus
        }
#endif
    } //namespace jni
} //namespace apl