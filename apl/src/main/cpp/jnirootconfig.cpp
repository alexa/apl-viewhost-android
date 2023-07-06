/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

#include <jni.h>
#include <string>
#include <jnidocumentmanager.h>
#include "apl/apl.h"
#include "apl/dynamicdata.h"
#include "apl/embed/documentmanager.h"

#include "jnirootconfig.h"
#include "jniutil.h"

namespace apl {
    namespace jni {

        template<typename T> DataSourceProviderPtr createDataSourceProvider() { return std::make_shared<T>(); }

#ifdef __cplusplus
        extern "C" {
#endif

#pragma clang diagnostic push

#pragma clang diagnostic push
#pragma ide diagnostic ignored "OCUnusedGlobalDeclarationInspection"
        const bool DEBUG_JNI = true;
#pragma clang diagnostic pop

        // Access APL view host RootContext class.
        static jclass ROOTCONTEXT_CLASS;
        static jclass BOOLEAN_CLASS;
        static jclass INTEGER_CLASS;

        static jmethodID BOOLEAN_VALUE;
        static jmethodID INT_VALUE;

        /**
         * Create a class and method cache for calls to View Host.
         */
         jboolean
         rootconfig_OnLoad(JavaVM *vm, void *reserved) {

             LOG(apl::LogLevel::kDebug) << "Loading View Host RootConfig JNI environment.";

             JNIEnv *env;
             if (vm ->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
                 return JNI_FALSE;
             }

            // For reading Integer and Boolean types
            BOOLEAN_CLASS = reinterpret_cast<jclass>(env->NewGlobalRef(
                    env->FindClass("java/lang/Boolean")));
            BOOLEAN_VALUE = env->GetMethodID(BOOLEAN_CLASS, "booleanValue", "()Z");
            INTEGER_CLASS = reinterpret_cast<jclass>(env->NewGlobalRef(
                    env->FindClass("java/lang/Integer")));
            INT_VALUE = env->GetMethodID(INTEGER_CLASS, "intValue", "()I");

            if (nullptr == BOOLEAN_CLASS
                || nullptr == BOOLEAN_VALUE
                || nullptr == INTEGER_CLASS
                || nullptr == INT_VALUE
                ) {
                LOG(apl::LogLevel::kError)
                        << "Could not load methods for class com.amazon.apl.android.RootContext";
                return JNI_FALSE;
            }

             return JNI_TRUE;
         }

         void
         rootconfig_OnUnload(JavaVM *vm, void *reserved) {

             LOG(apl::LogLevel::kDebug) << "Unload View Host RootConfig JNI environment.";

             JNIEnv *env;
             if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
                 // environment failure, can't proceed.
                 return;
             }

             env->DeleteGlobalRef(ROOTCONTEXT_CLASS);
             env->DeleteGlobalRef(BOOLEAN_CLASS);
             env->DeleteGlobalRef(INTEGER_CLASS);
         }

         JNIEXPORT jlong JNICALL
         Java_com_amazon_apl_android_RootConfig_nCreate(JNIEnv *env, jclass clazz) {
             // TODO remove when Core has this enabled by default.
             auto rc = RootConfig()
                     .enableExperimentalFeature(RootConfig::kExperimentalFeatureRequestKeyboard)
                     .enableExperimentalFeature(RootConfig::kExperimentalFeatureExtensionProvider);
             // Enable by default to support mediaLoad/mediaLoadFailed callbacks.
             rc.enableExperimentalFeature(RootConfig::kExperimentalFeatureManageMediaRequests);
             auto rc_ = std::make_shared<apl::RootConfig>(rc);
             return createHandle<RootConfig>(rc_);
         }

        JNIEXPORT void JNICALL
        Java_com_amazon_apl_android_RootConfig_nRegisterDataSource(
                JNIEnv *env,
                jclass clazz,
                jlong nativeHandle,
                jstring type_) {
            const char *type = env->GetStringUTFChars(type_, nullptr);
            auto rc = get<RootConfig>(nativeHandle);

            std::map<std::string, DataSourceProviderPtr (*)()> typeToProviderMap = {
                    { apl::DynamicTokenListConstants::DEFAULT_TYPE_NAME,
                            &createDataSourceProvider<DynamicTokenListDataSourceProvider> },
                    { apl::DynamicIndexListConstants::DEFAULT_TYPE_NAME,
                            &createDataSourceProvider<DynamicIndexListDataSourceProvider> }
            };

            rc->dataSourceProvider(type, typeToProviderMap[type]());
            env->ReleaseStringUTFChars(type_, type);
        }

         JNIEXPORT void JNICALL
         Java_com_amazon_apl_android_RootConfig_nAgent(JNIEnv *env, jclass clazz, jlong nativeHandle, jstring agentName_, jstring agentVersion_) {
             const char *agentName = env->GetStringUTFChars(agentName_, nullptr);
             const char *agentVersion = env->GetStringUTFChars(agentVersion_, nullptr);

             auto rc = get<RootConfig>(nativeHandle);
             rc->agent(agentName, agentVersion);

             env->ReleaseStringUTFChars(agentName_, agentName);
             env->ReleaseStringUTFChars(agentVersion_, agentVersion);
         }

        JNIEXPORT jobject JNICALL
        Java_com_amazon_apl_android_RootConfig_nGetProperty(JNIEnv *env, jclass clazz,
                                                          jlong nativeHandle,
                                                          jint property) {
            auto prop = static_cast<RootProperty>(property);

            auto rc = get<RootConfig>(nativeHandle);
            auto value = rc->getProperty(prop);

            return getJObject(env, value);
        }

        JNIEXPORT void JNICALL
        Java_com_amazon_apl_android_RootConfig_nSetByName(JNIEnv *env, jclass clazz, jlong nativeHandle,
                                                    jstring name_, jobject value) {
            const char *name = env->GetStringUTFChars(name_, nullptr);
            auto obj = getAPLObject(env, value);

            auto rc = get<RootConfig>(nativeHandle);
            rc->set(name, obj);

            env->ReleaseStringUTFChars(name_, name);
        }

        JNIEXPORT void JNICALL
        Java_com_amazon_apl_android_RootConfig_nSetByProperty(JNIEnv *env, jclass clazz, jlong nativeHandle,
                                                    jint property, jobject value) {

            auto prop = static_cast<RootProperty>(property);
            auto obj = getAPLObject(env, value);

            auto rc = get<RootConfig>(nativeHandle);
            rc->set(prop, obj);
        }

         JNIEXPORT jstring JNICALL
         Java_com_amazon_apl_android_RootConfig_nGetAgentName(JNIEnv *env, jclass clazz, jlong nativeHandle) {
             auto rc = get<RootConfig>(nativeHandle);
             return env->NewStringUTF(rc->getAgentName().c_str());
         }

        JNIEXPORT jstring JNICALL
        Java_com_amazon_apl_android_RootConfig_nGetAgentVersion(JNIEnv *env, jclass clazz, jlong nativeHandle) {
            auto rc = get<RootConfig>(nativeHandle);
            return env->NewStringUTF(rc->getAgentVersion().c_str());
        }

        JNIEXPORT jboolean JNICALL
        Java_com_amazon_apl_android_RootConfig_nGetDisallowVideo(JNIEnv *env, jclass clazz, jlong nativeHandle) {
            auto rc = get<RootConfig>(nativeHandle);
            return static_cast<jboolean>(rc->getDisallowVideo());
        }

        JNIEXPORT jboolean JNICALL
        Java_com_amazon_apl_android_RootConfig_nGetAllowOpenUrl(JNIEnv *env, jclass clazz, jlong nativeHandle) {
            auto rc = get<RootConfig>(nativeHandle);
            return static_cast<jboolean>(rc->getAllowOpenUrl());
        }

        JNIEXPORT jint JNICALL
        Java_com_amazon_apl_android_RootConfig_nGetAnimationQuality(JNIEnv *env, jclass clazz, jlong nativeHandle) {
            auto rc = get<RootConfig>(nativeHandle);
            return static_cast<jint>(rc->getAnimationQuality());
        }

        JNIEXPORT jlong JNICALL
        Java_com_amazon_apl_android_RootConfig_nGetUTCTime(JNIEnv *env, jclass clazz, jlong nativeHandle) {
             auto rc = get<RootConfig>(nativeHandle);
             return static_cast<jlong>(rc->getUTCTime());
        }

        JNIEXPORT jlong JNICALL
        Java_com_amazon_apl_android_RootConfig_nGetLocalTimeAdjustment(JNIEnv *env, jclass clazz, jlong nativeHandle) {
            auto rc = get<RootConfig>(nativeHandle);
            return static_cast<jlong>(rc->getLocalTimeAdjustment());
        }

        JNIEXPORT jfloat JNICALL
        Java_com_amazon_apl_android_RootConfig_nGetFontScale(JNIEnv *env, jclass clazz, jlong nativeHandle) {
            auto rc = get<RootConfig>(nativeHandle);
            return static_cast<jfloat>(rc->getFontScale());
        }

        JNIEXPORT jstring JNICALL
        Java_com_amazon_apl_android_RootConfig_nGetScreenMode(JNIEnv *env, jclass clazz, jlong nativeHandle) {
            auto rc = get<RootConfig>(nativeHandle);
            return env->NewStringUTF(rc->getScreenMode());
        }

        JNIEXPORT jint JNICALL
        Java_com_amazon_apl_android_RootConfig_nGetScreenModeEnumerated(JNIEnv *env, jclass clazz, jlong nativeHandle) {
            auto rc = get<RootConfig>(nativeHandle);
            return static_cast<jint>(rc->getScreenModeEnumerated());
        }

        JNIEXPORT jboolean JNICALL
        Java_com_amazon_apl_android_RootConfig_nGetScreenReader(JNIEnv *env, jclass clazz, jlong nativeHandle) {
            auto rc = get<RootConfig>(nativeHandle);
            return static_cast<jboolean >(rc->getScreenReaderEnabled());
        }

        JNIEXPORT jint JNICALL
        Java_com_amazon_apl_android_RootConfig_nGetDoublePressTimeout(JNIEnv *env, jclass clazz, jlong nativeHandle) {
            auto rc = get<RootConfig>(nativeHandle);
            return rc->getDoublePressTimeout();
        }

        JNIEXPORT jint JNICALL
        Java_com_amazon_apl_android_RootConfig_nGetLongPressTimeout(JNIEnv *env, jclass clazz, jlong nativeHandle) {
            auto rc = get<RootConfig>(nativeHandle);
            return rc->getLongPressTimeout();
        }

        JNIEXPORT jint JNICALL
        Java_com_amazon_apl_android_RootConfig_nGetMinimumFlingVelocity(JNIEnv *env, jclass clazz, jlong nativeHandle) {
            auto rc = get<RootConfig>(nativeHandle);
            return rc->getMinimumFlingVelocity();
        }

        JNIEXPORT jint JNICALL
        Java_com_amazon_apl_android_RootConfig_nGetPressedDuration(JNIEnv *env, jclass clazz, jlong nativeHandle) {
            auto rc = get<RootConfig>(nativeHandle);
            return rc->getPressedDuration();
        }

        JNIEXPORT jint JNICALL
        Java_com_amazon_apl_android_RootConfig_nGetTapOrScrollTimeout(JNIEnv *env, jclass clazz, jlong nativeHandle) {
            auto rc = get<RootConfig>(nativeHandle);
            return rc->getTapOrScrollTimeout();
        }



        JNIEXPORT void JNICALL
        Java_com_amazon_apl_android_RootConfig_nAllowOpenUrl(JNIEnv *env, jclass clazz, jlong nativeHandle, jboolean allowOpenUrl) {
            auto rc = get<RootConfig>(nativeHandle);
            rc->allowOpenUrl(allowOpenUrl);
        }

        JNIEXPORT void JNICALL
        Java_com_amazon_apl_android_RootConfig_nDisallowVideo(JNIEnv *env, jclass clazz, jlong nativeHandle, jboolean disallowVideo) {
            auto rc = get<RootConfig>(nativeHandle);
            rc->disallowVideo(disallowVideo);
        }

        JNIEXPORT void JNICALL
        Java_com_amazon_apl_android_RootConfig_nAnimationQuality(JNIEnv *env, jclass clazz, jlong nativeHandle, jint animationQuality) {
            auto rc = get<RootConfig>(nativeHandle);
            rc->animationQuality(static_cast<RootConfig::AnimationQuality>(animationQuality));
        }

        JNIEXPORT void JNICALL
        Java_com_amazon_apl_android_RootConfig_nUTCTime(JNIEnv *env, jclass clazz, jlong nativeHandle, jlong utcTime) {
             auto rc = get<RootConfig>(nativeHandle);
             rc->utcTime(utcTime);
        }

        JNIEXPORT void JNICALL
        Java_com_amazon_apl_android_RootConfig_nLocalTimeAdjustment(JNIEnv *env, jclass clazz, jlong nativeHandle, jlong adjustment) {
             auto rc = get<RootConfig>(nativeHandle);
             rc->localTimeAdjustment(adjustment);
        }

        JNIEXPORT void JNICALL
        Java_com_amazon_apl_android_RootConfig_nLiveData(JNIEnv *env, jclass clazz, jlong nativeHandle,
                                                         jstring name, jlong liveDataHandle) {
            auto rc = get<RootConfig>(nativeHandle);
            auto liveArray = get<LiveObject>(liveDataHandle);

            rc->liveData(getStdString(env, name), liveArray);
        }



         JNIEXPORT void JNICALL
         Java_com_amazon_apl_android_RootConfig_nRegisterExtension(JNIEnv *env, jclass clazz,
                                                                   jlong nativeHandle,
                                                                   jstring uri_) {

             const char * uri = env->GetStringUTFChars(uri_, nullptr);

             auto rc = get<RootConfig>(nativeHandle);
             rc->registerExtension(uri);

             env->ReleaseStringUTFChars(uri_, uri);
         }

        JNIEXPORT void JNICALL
        Java_com_amazon_apl_android_RootConfig_nRegisterExtensionEnvironment(JNIEnv *env,
                                                                             jclass clazz,
                                                                             jlong nativeHandle,
                                                                             jstring uri_,
                                                                             jobject environment) {

            const char *uri = env->GetStringUTFChars(uri_, nullptr);
            auto obj = getAPLObject(env, environment);

            auto rc = get<RootConfig>(nativeHandle);
            rc->registerExtensionEnvironment(uri, obj);

            env->ReleaseStringUTFChars(uri_, uri);
        }

        JNIEXPORT void JNICALL
        Java_com_amazon_apl_android_RootConfig_nSetEnvironmentValue(JNIEnv *env,
                                                                    jclass clazz,
                                                                    jlong nativeHandle,
                                                                    jstring name_,
                                                                    jobject value) {

            const char *name = env->GetStringUTFChars(name_, nullptr);
            auto val = getAPLObject(env, value);

            auto rc = get<RootConfig>(nativeHandle);
            rc->setEnvironmentValue(name, val);

            env->ReleaseStringUTFChars(name_, name);
        }

        JNIEXPORT void JNICALL
        Java_com_amazon_apl_android_RootConfig_nExtensionProvider(JNIEnv *env,
                                                                  jclass clazz,
                                                                  jlong nativeHandle,
                                                                  jlong providerNativeHandle) {
            auto rc = get<RootConfig>(nativeHandle);
            auto ep = get<alexaext::ExtensionProvider>(providerNativeHandle);
            rc->extensionProvider(ep);
        }

        JNIEXPORT void JNICALL
        Java_com_amazon_apl_android_RootConfig_nExtensionMediator(JNIEnv *env,
                                                                  jclass clazz,
                                                                  jlong nativeHandle,
                                                                  jlong mediatorNativeHandle) {
            auto rc = get<RootConfig>(nativeHandle);
            auto em = get<ExtensionMediator>(mediatorNativeHandle);
            rc->extensionMediator(em);
        }

        JNIEXPORT void JNICALL
        Java_com_amazon_apl_android_RootConfig_nRegisterExtensionEventHandler(JNIEnv *env,
                                                                              jclass clazz,
                                                                              jlong nativeHandle,
                                                                              jlong handlerNativeHandle) {
            auto rc = get<RootConfig>(nativeHandle);
            auto eeh = get<ExtensionEventHandler>(handlerNativeHandle);
            rc->registerExtensionEventHandler(*eeh);
        }

        JNIEXPORT void JNICALL
        Java_com_amazon_apl_android_RootConfig_nRegisterExtensionFlags(JNIEnv *env, jclass clazz,
                                                                       jlong nativeHandle,
                                                                       jstring uri_,
                                                                       jobject flags) {

            const char * uri = env->GetStringUTFChars(uri_, nullptr);
            auto obj = getAPLObject(env, flags);

            auto rc = get<RootConfig>(nativeHandle);
            rc->registerExtensionFlags(uri, obj);

            env->ReleaseStringUTFChars(uri_, uri);
        }

        JNIEXPORT jobject JNICALL
        Java_com_amazon_apl_android_RootConfig_nGetExtensionFlags(JNIEnv *env, jclass clazz,
                                                                  jlong nativeHandle,
                                                                  jstring uri_) {
            const char * uri = env->GetStringUTFChars(uri_, nullptr);
            auto rc = get<RootConfig>(nativeHandle);
            auto flags = rc->getExtensionFlags(uri);

            jobject obj = getJObject(env, flags);

            env->ReleaseStringUTFChars(uri_, uri);

            return obj;
        }

        JNIEXPORT void JNICALL
        Java_com_amazon_apl_android_RootConfig_nRegisterExtensionCommand(JNIEnv *env, jclass clazz,
                                                                         jlong nativeHandle,
                                                                         jlong commandNativeHandle) {
            auto rc = get<RootConfig>(nativeHandle);
            auto cmd = get<ExtensionCommandDefinition>(commandNativeHandle);
            rc->registerExtensionCommand(*cmd);
        }

        JNIEXPORT void JNICALL
        Java_com_amazon_apl_android_RootConfig_nRegisterExtensionFilter(JNIEnv *env, jclass clazz,
                                                                         jlong nativeHandle,
                                                                         jlong filterNativeHandle) {
            auto rc = get<RootConfig>(nativeHandle);
            auto filter = get<ExtensionFilterDefinition>(filterNativeHandle);
            rc->registerExtensionFilter(*filter);
        }

        JNIEXPORT void JNICALL
        Java_com_amazon_apl_android_RootConfig_nSequenceChildCache(JNIEnv *env,jclass clazz, jlong nativeHandle, jint cacheSize) {
            auto rc = get<RootConfig>(nativeHandle);
            rc->sequenceChildCache(cacheSize);
        }

        JNIEXPORT void JNICALL
        Java_com_amazon_apl_android_RootConfig_nPagerChildCache(JNIEnv *env,jclass clazz, jlong nativeHandle, jint cacheSize) {
            auto rc = get<RootConfig>(nativeHandle);
            rc->pagerChildCache(cacheSize);
        }

        JNIEXPORT void JNICALL
        Java_com_amazon_apl_android_RootConfig_nFontScale(JNIEnv *env,jclass clazz, jlong nativeHandle, jfloat scale) {
            auto rc = get<RootConfig>(nativeHandle);
            rc->fontScale(scale);
        }

        JNIEXPORT void JNICALL
        Java_com_amazon_apl_android_RootConfig_nScreenMode(JNIEnv *env,jclass clazz, jlong nativeHandle, jint screenMode) {
            auto rc = get<RootConfig>(nativeHandle);
            rc->screenMode(static_cast<RootConfig::ScreenMode>(screenMode));
        }

        JNIEXPORT void JNICALL
        Java_com_amazon_apl_android_RootConfig_nScreenReader(JNIEnv *env,jclass clazz, jlong nativeHandle, jboolean enabled) {
            auto rc = get<RootConfig>(nativeHandle);
            rc->screenReader(static_cast<bool>(enabled));
        }

        JNIEXPORT void JNICALL
        Java_com_amazon_apl_android_RootConfig_nDoublePressTimeout(JNIEnv *env,jclass clazz, jlong nativeHandle, jint timeout) {
            auto rc = get<RootConfig>(nativeHandle);
            rc->doublePressTimeout(timeout);
        }

        JNIEXPORT void JNICALL
        Java_com_amazon_apl_android_RootConfig_nLongPressTimeout(JNIEnv *env,jclass clazz, jlong nativeHandle, jint timeout) {
            auto rc = get<RootConfig>(nativeHandle);
            rc->longPressTimeout(timeout);
        }

        JNIEXPORT void JNICALL
        Java_com_amazon_apl_android_RootConfig_nMinimumFlingVelocity(JNIEnv *env,jclass clazz, jlong nativeHandle, jint velocity) {
            auto rc = get<RootConfig>(nativeHandle);
            rc->minimumFlingVelocity(velocity);
        }

        JNIEXPORT void JNICALL
        Java_com_amazon_apl_android_RootConfig_nPressedDuration(JNIEnv *env,jclass clazz, jlong nativeHandle, jint timeout) {
            auto rc = get<RootConfig>(nativeHandle);
            rc->pressedDuration(timeout);
        }

        JNIEXPORT void JNICALL
        Java_com_amazon_apl_android_RootConfig_nTapOrScrollTimeout(JNIEnv *env,jclass clazz, jlong nativeHandle, jint timeout) {
            auto rc = get<RootConfig>(nativeHandle);
            rc->tapOrScrollTimeout(timeout);
        }

        JNIEXPORT void JNICALL
        Java_com_amazon_apl_android_RootConfig_nSession(JNIEnv *env, jclass clazz, jlong nativeHandle,
                                                        jlong sessionHandler) {
            auto rc = get<RootConfig>(nativeHandle);
            rc->session(get<Session>(sessionHandler));
        }

        JNIEXPORT void JNICALL
        Java_com_amazon_apl_android_RootConfig_nAudioPlayerFactory(JNIEnv *env, jclass clazz, jlong nativeHandle,
                                                                   jlong factoryHandler) {
            auto rc = get<RootConfig>(nativeHandle);
            rc->audioPlayerFactory(get<AudioPlayerFactory>(factoryHandler));
        }

        JNIEXPORT void JNICALL
        Java_com_amazon_apl_android_RootConfig_nMediaPlayerFactory(JNIEnv *env, jclass clazz, jlong nativeHandle,
                                                                    jlong nativeHandler) {
            auto rc = get<RootConfig>(nativeHandle);
            rc->mediaPlayerFactory(get<MediaPlayerFactory>(nativeHandler));
        }

        JNIEXPORT void JNICALL
        Java_com_amazon_apl_android_RootConfig_nSetDocumentManager(JNIEnv *env, jclass clazz, jlong nativeHandle, jlong managerHandle) {
            auto rc = get<RootConfig>(nativeHandle);
            auto documentManager = get<DocumentManager>(managerHandle);
            rc->documentManager(documentManager);
        }
#ifdef __cplusplus
        }
#endif
    }
}
