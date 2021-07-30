/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

#include <jni.h>
#include <elf.h>
#include <locale>
#include <codecvt>
#include "apl/apl.h"
#include "jniutil.h"
#include "jnicontent.h"
#include "loggingbridge.h"

namespace apl {
    namespace jni {

#ifdef __cplusplus
        extern "C" {
#endif

#pragma clang diagnostic push
#pragma ide diagnostic ignored "OCUnusedGlobalDeclarationInspection"
        const bool DEBUG_JNI = true;
#pragma clang diagnostic pop

#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wunused-parameter"

        // Access the View Host Content class.
        static jclass CONTENT_CLASS;
        static jmethodID REQUEST_PACKAGE;
        static jmethodID REQUEST_DATA;
        static jmethodID ON_READY;
        static jmethodID ON_ERROR;

        static jclass HASHSET_CLASS;
        static jmethodID HASHSET_CONSTRUCTOR;
        static jmethodID HASHSET_ADD;

        /**
         * Create a class and method cache for calls to View Host.
         */
        jboolean
        content_OnLoad(JavaVM *vm, void __unused *reserved) {

            LOG(apl::LogLevel::DEBUG) << "Loading View Host Content JNI environment.";

            JNIEnv *env;
            if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
                return JNI_FALSE;
            }

            // method signatures can be obtained with 'javap -s'
            CONTENT_CLASS = reinterpret_cast<jclass>(env->NewGlobalRef(
                    env->FindClass("com/amazon/apl/android/Content")));
            REQUEST_PACKAGE = env->GetMethodID(CONTENT_CLASS, "coreRequestPackage",
                                               "(JLjava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");
            REQUEST_DATA = env->GetMethodID(CONTENT_CLASS, "coreRequestData",
                                            "(Ljava/lang/String;)V");
            ON_READY = env->GetMethodID(CONTENT_CLASS, "coreComplete", "()V");
            ON_ERROR = env->GetMethodID(CONTENT_CLASS, "coreFailure", "()V");

            HASHSET_CLASS = reinterpret_cast<jclass>(env->NewGlobalRef(
                    env->FindClass("java/util/HashSet")));
            HASHSET_CONSTRUCTOR = env->GetMethodID(HASHSET_CLASS, "<init>", "()V");
            HASHSET_ADD = env->GetMethodID(HASHSET_CLASS, "add", "(Ljava/lang/Object;)Z");

            if (nullptr == CONTENT_CLASS
                || nullptr == REQUEST_PACKAGE
                || nullptr == REQUEST_DATA
                || nullptr == HASHSET_CLASS
                || nullptr == HASHSET_CONSTRUCTOR
                || nullptr == HASHSET_ADD) {

                LOG(apl::LogLevel::ERROR)
                        << "Could not load methods for class com.amazon.apl.android.content.Content";
                return JNI_FALSE;
            }

            return JNI_TRUE;
        }


        /**
         * Release the class and method cache.
         */
        void
        content_OnUnload(JavaVM *vm, void __unused *reserved) {
            LOG(apl::LogLevel::DEBUG) << "Unloading View Host Content JNI environment.";
            apl::LoggerFactory::instance().reset();

            JNIEnv *env;
            if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
                // environment failure, can't proceed.
                return;
            }

            env->DeleteGlobalRef(CONTENT_CLASS);
            env->DeleteGlobalRef(HASHSET_CLASS);
        }


        /**
         * Request packages from the view host.
         */
        void requestPackages(JNIEnv *env, jobject instance, const apl::ContentPtr &contentPtr) {

            auto request = contentPtr->getRequestedPackages();

            for (const auto &req : request) {

                // use handle instead of id as a ref to the native peer
                auto reqPtr = std::make_shared<apl::ImportRequest>(req);
                auto handle = createHandle<ImportRequest>(reqPtr);

                auto source = env->NewStringUTF(req.source().c_str());
                auto name = env->NewStringUTF(req.reference().name().c_str());
                auto version = env->NewStringUTF(req.reference().version().c_str());

                env->CallVoidMethod(instance, REQUEST_PACKAGE, handle, source, name,
                                    version);

                env->DeleteLocalRef(source);
                env->DeleteLocalRef(name);
                env->DeleteLocalRef(version);
            }

        }


        /**
         * Request data from the view host.
         */
        void requestData(JNIEnv *env, jobject instance, const apl::ContentPtr &contentPtr) {

            auto paramCount = contentPtr->getParameterCount();

            for (size_t i = 0; i < paramCount; i++) {
                auto p = contentPtr->getParameterAt(i);
                auto name = env->NewStringUTF(p.c_str());
                env->CallVoidMethod(instance, REQUEST_DATA, name);
                env->DeleteLocalRef(name);
                if (contentPtr->isError()) {
                    break;
                }
            }
        }

        /**
         * Update the View Host with outstanding data or package requests.
         */
        void update(JNIEnv *env, jobject instance, const apl::ContentPtr &content,
                    bool updatePackages, bool updateData) {

            // update any outstanding requests for packages or data;
            if (!content->isError()) {
                // request packages
                if (updatePackages) {
                    requestPackages(env, instance, content);
                }
                // request all data - set is fixed, and only updated if updateData = true
                if (updateData && !content->isWaiting()) {
                    // prevent requesting package and data in parallel
                    // avoiding data race in content->addPackage() and content->addData()
                    requestData(env, instance, content);
                }
            }

            if (content->isError()) {
                LOG(LogLevel::ERROR) << "Content Error";
                env->CallVoidMethod(instance, ON_ERROR);
            } else if (content->isReady()) {
                LOG(LogLevel::DEBUG) << "Content Ready";
                env->CallVoidMethod(instance, ON_READY);
            }
        }


        /**
         * Create a Content instance and attach it to the view host peer.
         */
        JNIEXPORT jlong JNICALL
        Java_com_amazon_apl_android_Content_nCreate(JNIEnv *env, jobject instance,
                                                    jstring mainTemplate_) {

            // for emoji characters, GetStringUTFChars returns the surrogate pair
            // eg. U+1F33D -> \uD83C\uDF3D
            // but rapidjson expects the 4-byte format \xF0\x9F\x8C\xBD
            // and returns a parsing error
            // so get UTF-16 using GetStringChars and convert it to UTF-8 manually
            // before passing it rapidjson
            const jchar* mainTemplate = env->GetStringChars(mainTemplate_, nullptr);
            jsize mainTemplateLen = env->GetStringLength(mainTemplate_);
            std::wstring_convert<std::codecvt_utf8_utf16<char16_t>, char16_t> converter;

            auto content = apl::Content::create(converter.to_bytes(std::u16string(mainTemplate, mainTemplate+mainTemplateLen)));
            if (!content) {
                LOG(LogLevel::ERROR) << "Error creating Content";
                return static_cast<jboolean>(false);
            }

            env->ReleaseStringChars(mainTemplate_, mainTemplate);

            return createHandle<Content>(content);
        }


        /**
         * Create a Content instance and attach it to the view host peer.
         */
        JNIEXPORT void JNICALL
        Java_com_amazon_apl_android_Content_nUpdate(JNIEnv *env, jobject instance, jlong handle) {
            auto content = get<Content>(handle);
            update(env, instance, content, true, true); // update packages, data, status
        }

        /**
         * Add a requested package to the document.
         */
        JNIEXPORT void JNICALL
        Java_com_amazon_apl_android_Content_nAddPackage(JNIEnv *env, jobject instance,
                                                        jlong contentHandle,
                                                        jlong requestHandle,
                                                        jstring docContents_) {
            const char *docContents = env->GetStringUTFChars(docContents_, nullptr);
            // Get the content from the handle
            auto c = get<Content>(contentHandle);
            // add the package
            auto it = get<ImportRequest>(requestHandle);
            c->addPackage(*it, docContents);
            env->ReleaseStringUTFChars(docContents_, docContents);
            update(env, instance, c, true, true); // update packages, data, and status
        }


        /**
         * Add data payload.
         */
        JNIEXPORT void JNICALL
        Java_com_amazon_apl_android_Content_nAddData(JNIEnv *env, jobject instance,
                                                     jlong contentHandle,
                                                     jstring dataName_,
                                                     jstring docContents_) {

            // for emoji characters, GetStringUTFChars returns the surrogate pair
            // eg. U+1F33D -> \uD83C\uDF3D
            // but rapidjson expects the 4-byte format \xF0\x9F\x8C\xBD
            // and returns a parsing error
            // so get UTF-16 using GetStringChars and convert it to UTF-8 manually
            // before passing it rapidjson
            const jchar* docContents = env->GetStringChars(docContents_, nullptr);
            jsize docContentsLen = env->GetStringLength(docContents_);
            std::wstring_convert<std::codecvt_utf8_utf16<char16_t>, char16_t> converter;

            const char *dataName = env->GetStringUTFChars(dataName_, nullptr);

            // Get the content from the handle
            auto c = get<Content>(contentHandle);

            c->addData(dataName, converter.to_bytes(std::u16string(docContents, docContents+docContentsLen)));

            env->ReleaseStringChars(docContents_, docContents);
            env->ReleaseStringUTFChars(dataName_, dataName);
            update(env, instance, c, false, false); // only update status, no packages or data
        }


        JNIEXPORT jstring JNICALL Java_com_amazon_apl_android_Content_nGetAPLVersion
                (JNIEnv *env, jclass clazz, jlong contentHandle) {

            auto c = get<Content>(contentHandle);
            return env->NewStringUTF(c->getAPLVersion().c_str());
        }

        JNIEXPORT jboolean JNICALL Java_com_amazon_apl_android_Content_nIsWaiting
                (JNIEnv *env, jclass clazz, jlong contentHandle) {

            // Get the content from the NativeOwner
            auto c = get<Content>(contentHandle);
            return static_cast<jboolean>(c->isWaiting());
        }


        JNIEXPORT jboolean JNICALL Java_com_amazon_apl_android_Content_nIsReady
                (JNIEnv *env, jclass clazz, jlong contentHandle) {

            // Get the content from the NativeOwner
            auto c = get<Content>(contentHandle);
            return static_cast<jboolean>(c->isReady());
        }


        JNIEXPORT jboolean JNICALL Java_com_amazon_apl_android_Content_nIsError
                (JNIEnv *env, jclass clazz, jlong contentHandle) {

            // Get the content from the NativeOwner
            auto c = get<Content>(contentHandle);
            return static_cast<jboolean>(c->isError());
        }


        /**
         * Returns the custom device- or runtime-specific setting from the core to viewhost.
         */
        JNIEXPORT jobject JNICALL
        Java_com_amazon_apl_android_Content_nSetting(JNIEnv *env,
                                                         jclass clazz,
                                                         jlong handle,
                                                         jstring propertyName_) {

            const char *propertyName = env->GetStringUTFChars(propertyName_, nullptr);

            auto content = get<Content>(handle);
            auto settings = content->getDocumentSettings();

            Object value = Object::NULL_OBJECT();
            if (settings) {
                value = settings->getValue(propertyName);
            }

            env->ReleaseStringUTFChars(propertyName_, propertyName);

            return getJObject(env, value);
        }


        JNIEXPORT jobject JNICALL
        Java_com_amazon_apl_android_Content_nGetExtensionRequests(JNIEnv *env, jclass type,
                                                                  jlong nativeHandle) {

            auto c = get<Content>(nativeHandle);
            auto requests = c->getExtensionRequests();

            jobject javaSet = env->NewObject(HASHSET_CLASS, HASHSET_CONSTRUCTOR);
            for (auto it : requests) {
                jstring val = env->NewStringUTF(it.data());
                env->CallBooleanMethod(javaSet, HASHSET_ADD, val);
            }
            return javaSet;

        }


        JNIEXPORT jobject JNICALL
        Java_com_amazon_apl_android_Content_nGetExtensionSettings(JNIEnv *env, jclass type,
                                                                  jlong nativeHandle,
                                                                  jstring uri_) {
            const char *uri = env->GetStringUTFChars(uri_, 0);

            auto c = get<Content>(nativeHandle);
            auto settings = c->getExtensionSettings(uri);

            jobject obj = getJObject(env, settings);

            env->ReleaseStringUTFChars(uri_, uri);

            return obj;
        }

        JNIEXPORT void JNICALL
        Java_com_amazon_apl_android_Content_nCreateDocumentBackground(JNIEnv *env, jobject obj,
                                                                  jlong contentHandle,
                                                                  jlong rootConfigHandle,
                                                                  //metrics parameters
                                                                  jint width, jint height, jint dpi,
                                                                  jint screenShape, jstring theme_,
                                                                  jint viewportMode) {

            auto content = get<Content>(contentHandle);
            auto rootConfig = get<RootConfig>(rootConfigHandle);
            if (content == NULL || rootConfig == NULL) {
                LOG(LogLevel::ERROR) << "Error cannot get document background without content and rootConfig";
                return;
            }

            auto metrics = Metrics();
            auto shape = static_cast<ScreenShape>(screenShape);
            auto mode = static_cast<ViewportMode>(viewportMode);
            const char *theme = env->GetStringUTFChars(theme_, nullptr);
            metrics.size(width, height)
                .dpi(dpi)
                .shape(shape)
                .theme(theme)
                .mode(mode);

            auto background = content->getBackground(metrics, *rootConfig);
            jboolean isGradient = background.isGradient();
            jboolean isColor = background.isColor();
            jint type = 0;
            jfloat angle = 0;
            jlong color = 0;

            if (isGradient) {
                auto gradient = background.getGradient();
                type = gradient.getType();
                angle = gradient.getAngle();

                int colorCount = gradient.getColorRange().size();
                jlong colors[colorCount];
                for (int i = 0; i < colorCount; i++) {
                    apl::Color color = gradient.getColorRange()[i];
                    colors[i] = static_cast<long>(color.get());
                }
                jlongArray colorRange = env->NewLongArray(colorCount);
                env->SetLongArrayRegion(colorRange, 0, colorCount, colors);

                int inputCount = gradient.getInputRange().size();
                jfloat range[inputCount];
                for (int i = 0; i < inputCount; i++) {
                    range[i] = static_cast<float>(gradient.getInputRange()[i]);
                }
                jfloatArray inputRange = env->NewFloatArray(gradient.getInputRange().size());
                env->SetFloatArrayRegion(inputRange, 0, inputCount, range);

                jmethodID mid = env->GetMethodID(CONTENT_CLASS, "callbackBackgroundGradient", "(IF[J[F)V");
                env->CallVoidMethod(obj, mid, type, angle, colorRange, inputRange);

                env->DeleteLocalRef(colorRange);
                env->DeleteLocalRef(inputRange);
            } else if (isColor) {
                color = static_cast<long>(background.getColor());
                jmethodID mid = env->GetMethodID(CONTENT_CLASS, "callbackBackgroundColor", "(J)V");
                env->CallVoidMethod(obj, mid, color);
            } else {
                LOG(LogLevel::ERROR) << "Error: document background should be color or gradient";
            }

            env->ReleaseStringUTFChars(theme_, theme);
            return;
        }


#pragma clang diagnostic pop

#ifdef __cplusplus
        }
#endif

    } //namespace jni
} //namespace apl
