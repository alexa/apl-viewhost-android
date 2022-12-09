/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

#include <jni.h>
#include <locale>
#include <codecvt>
#include "jniaudioplayer.h"
#include "jniutil.h"
#include "jnicontent.h"

namespace apl {
    namespace jni {
#ifdef __cplusplus
        extern "C" {
#endif

        static jclass AUDIOPLAYERFACTORY_CLASS;
        static jmethodID AUDIOPLAYERFACTORY_CREATE_PLAYER;
        static JavaVM *AUDIOPLAYERFACTORY_VM_REFERENCE;

        jboolean
        audioplayerfactory_OnLoad(JavaVM *vm, void *reserved) {

            LOG(apl::LogLevel::kDebug) << "Loading View Host Audio Player Factory JNI environment.";

            JNIEnv *env;
            if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
                LOG(apl::LogLevel::kError) << "Environment failure, cannot proceed";
                return JNI_FALSE;
            }

            AUDIOPLAYERFACTORY_VM_REFERENCE = vm;

            AUDIOPLAYERFACTORY_CLASS = reinterpret_cast<jclass>(env->NewGlobalRef(
                    env->FindClass("com/amazon/apl/android/audio/AudioPlayerFactoryProxy")));

            AUDIOPLAYERFACTORY_CREATE_PLAYER = env->GetMethodID(
                    AUDIOPLAYERFACTORY_CLASS,
                    "createPlayer",
                    "(J)Lcom/amazon/apl/android/audio/AudioPlayer;");

            if (nullptr == AUDIOPLAYERFACTORY_CREATE_PLAYER) {
                return JNI_FALSE;
            }

            return JNI_TRUE;
        }

        void
        audioplayerfactory_OnUnload(JavaVM *vm, void *reserved) {
            LOG(apl::LogLevel::kDebug) << "Unloading View Host Audio Player Factory JNI environment.";
            apl::LoggerFactory::instance().reset();

            JNIEnv *env;
            if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
                LOG(apl::LogLevel::kError) << "Environment failure, cannot proceed";
                return;
            }

            env->DeleteGlobalRef(AUDIOPLAYERFACTORY_CLASS);
        }

        class AndroidAudioPlayerFactory : public AudioPlayerFactory {
        public:
            AudioPlayerPtr createPlayer(AudioPlayerCallback playerCallback,
                                        SpeechMarkCallback speechMarkCallback) override {
                JNIEnv *env;
                if (AUDIOPLAYERFACTORY_VM_REFERENCE->GetEnv(reinterpret_cast<void **>(&env),
                                                            JNI_VERSION_1_6) != JNI_OK) {
                    LOG(apl::LogLevel::kError) << "Environment failure, cannot proceed";
                    return nullptr;
                }

                jobject localRef = env->NewLocalRef(mWeakInstance);
                if (!localRef) {
                    return nullptr;
                }

                auto audioPlayer_ = std::make_shared<AndroidAudioPlayer>(std::move(playerCallback),
                                                                         std::move(speechMarkCallback));
                auto playerHandler = createHandle<AudioPlayer>(audioPlayer_);
                if (playerHandler == 0) return nullptr;

                auto instance = env->CallObjectMethod(localRef, AUDIOPLAYERFACTORY_CREATE_PLAYER,
                                                      playerHandler);
                audioPlayer_->setInstance(instance);

                auto player = get<AudioPlayer>(playerHandler);

                return player;
            }

            explicit AndroidAudioPlayerFactory(jweak weakInstance) : mWeakInstance(weakInstance) {}

            ~AndroidAudioPlayerFactory() override {
                JNIEnv *env;
                if (AUDIOPLAYERFACTORY_VM_REFERENCE->GetEnv(reinterpret_cast<void **>(&env),
                                                            JNI_VERSION_1_6) != JNI_OK) {
                    LOG(apl::LogLevel::kError) << "Environment failure, cannot proceed";
                    return;
                }
                env->DeleteWeakGlobalRef(mWeakInstance);
            }

        private:
            jweak mWeakInstance;
        };

        JNIEXPORT jlong JNICALL
        Java_com_amazon_apl_android_audio_AudioPlayerFactoryProxy_nCreate(JNIEnv *env, jobject instance) {
            auto audioPlayerFactory_ = std::make_shared<AndroidAudioPlayerFactory>(
                    env->NewWeakGlobalRef(instance));
            return createHandle<AudioPlayerFactory>(audioPlayerFactory_);
        }

#ifdef __cplusplus
        }
#endif
    } //namespace jni
} //namespace apl