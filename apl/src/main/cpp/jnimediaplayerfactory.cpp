/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */
#include <jni.h>
#include <locale>
#include <codecvt>
#include "jnimediaplayer.h"
#include "jniutil.h"
#include "jnicontent.h"

namespace apl {
    namespace jni {
#ifdef __cplusplus
        extern "C" {
#endif

        static jclass MEDIAPLAYERFACTORY_CLASS;
        static jmethodID MEDIAPLAYERFACTORY_CREATE_PLAYER;
        static JavaVM *MEDIAPLAYERFACTORY_VM_REFERENCE;

        jboolean mediaplayerfactory_OnLoad(JavaVM *vm, void *reserved) {

            LOG(apl::LogLevel::kDebug) << "Loading View Host Media Player Factory JNI environment.";

            JNIEnv *env;
            if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
                LOG(apl::LogLevel::kError) << "Environment failure, cannot proceed";
                return JNI_FALSE;
            }

            MEDIAPLAYERFACTORY_VM_REFERENCE = vm;

            MEDIAPLAYERFACTORY_CLASS = reinterpret_cast<jclass>(env->NewGlobalRef(
                    env->FindClass("com/amazon/apl/android/media/MediaPlayerFactoryProxy")));

            MEDIAPLAYERFACTORY_CREATE_PLAYER = env->GetMethodID(
                    MEDIAPLAYERFACTORY_CLASS,
                    "createPlayer",
                    "(J)Lcom/amazon/apl/android/media/MediaPlayer;");

            if (nullptr == MEDIAPLAYERFACTORY_CREATE_PLAYER) {
                return JNI_FALSE;
            }

            return JNI_TRUE;
        }

        void mediaplayerfactory_OnUnload(JavaVM *vm, void *reserved) {
            LOG(apl::LogLevel::kDebug) << "Unloading View Host Media Player Factory JNI environment.";
            apl::LoggerFactory::instance().reset();

            JNIEnv *env;
            if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
                LOG(apl::LogLevel::kError) << "Environment failure, cannot proceed";
                return;
            }

            env->DeleteGlobalRef(MEDIAPLAYERFACTORY_CLASS);
        }

        class AndroidMediaPlayerFactory : public MediaPlayerFactory {
        public:
            MediaPlayerPtr createPlayer(MediaPlayerCallback mediaPlayerCallback) override {
                JNIEnv *env;

                if (MEDIAPLAYERFACTORY_VM_REFERENCE->GetEnv(reinterpret_cast<void **>(&env),
                                                            JNI_VERSION_1_6) != JNI_OK) {
                    LOG(apl::LogLevel::kError) << "Environment failure, cannot proceed";
                    return nullptr;
                }

                jobject localRef = env->NewLocalRef(mWeakInstance);
                if (!localRef) {
                    return nullptr;
                }

                auto mediaPlayer_ = std::make_shared<AndroidMediaPlayer>(std::move(mediaPlayerCallback));
                auto playerHandler = reinterpret_cast<jlong>(mediaPlayer_.get());
                if (playerHandler == 0) return nullptr;
                auto instance = env->CallObjectMethod(localRef, MEDIAPLAYERFACTORY_CREATE_PLAYER,
                                                      playerHandler);
                mediaPlayer_->setInstance(instance);
                return mediaPlayer_;
            }

            explicit AndroidMediaPlayerFactory(jweak weakInstance) : mWeakInstance(weakInstance) {}

            ~AndroidMediaPlayerFactory() override {
                JNIEnv *env;
                if (MEDIAPLAYERFACTORY_VM_REFERENCE->GetEnv(reinterpret_cast<void **>(&env),
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
        Java_com_amazon_apl_android_media_MediaPlayerFactoryProxy_nCreate(JNIEnv *env, jobject instance) {
            auto mediaPlayerFactory_ = std::make_shared<AndroidMediaPlayerFactory>(
                    env->NewWeakGlobalRef(instance));
            return createHandle<MediaPlayerFactory>(mediaPlayerFactory_);
        }

#ifdef __cplusplus
        }
#endif
    } //namespace jni
} //namespace apl