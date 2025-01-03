/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

#include <jni.h>
#include <locale>
#include <codecvt>
#include "apl/apl.h"
#include "jniutil.h"
#include "jniembeddeddocumentrequest.h"

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
        static jmethodID IS_USE_PACKAGE_MANAGER;
        static jmethodID NOTIFY_CALLBACKS;

        static jclass HASHSET_CLASS;
        static jmethodID HASHSET_CONSTRUCTOR;
        static jmethodID HASHSET_ADD;
        static JavaVM * VM_REFERENCE;

        // Forward prototype to allow use before the declaration of function
        void update(JNIEnv *env, jobject instance, const ContentPtr &content,
                    bool updatePackages, bool updateData);

        /**
         * Create a class and method cache for calls to View Host.
         */
        jboolean
        content_OnLoad(JavaVM *vm, void *reserved) {

            LOG(apl::LogLevel::kDebug) << "Loading View Host Content JNI environment.";

            VM_REFERENCE = vm;

            JNIEnv *env;
            if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
                return JNI_FALSE;
            }

            // method signatures can be obtained with 'javap -s'
            CONTENT_CLASS = reinterpret_cast<jclass>(env->NewGlobalRef(
                    env->FindClass("com/amazon/apl/android/Content")));
            REQUEST_DATA = env->GetMethodID(CONTENT_CLASS, "coreRequestData",
                                            "(Ljava/lang/String;)V");
            ON_READY = env->GetMethodID(CONTENT_CLASS, "coreComplete", "()V");
            ON_ERROR = env->GetMethodID(CONTENT_CLASS, "coreFailure", "()V");
            IS_USE_PACKAGE_MANAGER = env->GetMethodID(CONTENT_CLASS, "shouldUsePackageManager", "()Z");
            NOTIFY_CALLBACKS = env->GetMethodID(CONTENT_CLASS, "notifyCallback", "(ZZ)V");

            HASHSET_CLASS = reinterpret_cast<jclass>(env->NewGlobalRef(
                    env->FindClass("java/util/HashSet")));
            HASHSET_CONSTRUCTOR = env->GetMethodID(HASHSET_CLASS, "<init>", "()V");
            HASHSET_ADD = env->GetMethodID(HASHSET_CLASS, "add", "(Ljava/lang/Object;)Z");
            REQUEST_PACKAGE = env->GetMethodID(CONTENT_CLASS, "coreRequestPackage",
                                               "(JLjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");

            if (nullptr == CONTENT_CLASS
                || nullptr == REQUEST_PACKAGE
                || nullptr == REQUEST_DATA
                || nullptr == HASHSET_CLASS
                || nullptr == HASHSET_CONSTRUCTOR
                || nullptr == HASHSET_ADD
                || nullptr == IS_USE_PACKAGE_MANAGER
                || nullptr == NOTIFY_CALLBACKS) {

                LOG(apl::LogLevel::kError)
                        << "Could not load methods for class com.amazon.apl.android.content.Content";
                return JNI_FALSE;
            }

            LOG(apl::LogLevel::kDebug)
                    << "content_OnLoad successful";

            return JNI_TRUE;
        }


        /**
         * Release the class and method cache.
         */
        void
        content_OnUnload(JavaVM *vm, void *reserved) {
            LOG(apl::LogLevel::kDebug) << "Unloading View Host Content JNI environment.";
            apl::LoggerFactory::instance().reset();

            JNIEnv *env;
            if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
                // environment failure, can't proceed.
                return;
            }

            env->DeleteGlobalRef(CONTENT_CLASS);
            env->DeleteGlobalRef(HASHSET_CLASS);
        }

        JNIEnv * jenv() {
            JNIEnv *env;
            if (VM_REFERENCE->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
                // environment failure, can't proceed.
                return nullptr;
            }
            return env;
        }

        bool shouldUsePackageManager(JNIEnv * env, jobject instance) {
             jboolean usePackageManager  = env->CallBooleanMethod(instance, IS_USE_PACKAGE_MANAGER);
             return usePackageManager;
        }

        void handleLoadingResults(bool updateData, const ContentPtr &contentPtr, jweak iweak) {
            JNIEnv * env = jenv();
            auto instance = env->NewLocalRef(iweak);

            if(instance != nullptr) {
                update(env, instance, contentPtr, false, updateData);

                // notify callbacks, since there is no "addPackage()" call in the java object
                // in "addPackage()" flow we have addPackage() method to call notify_callback().
                // in content->load(), pathway, we have to compensate for that from jni layer.
                env->CallVoidMethod(instance, NOTIFY_CALLBACKS, true, true);
            }

            env->DeleteLocalRef(instance);
            env->DeleteWeakGlobalRef(iweak);
        } 

        void loadPackagesWithPackageManager(const ContentPtr &contentPtr, jobject instance) {
            jweak iweak = jenv()->NewWeakGlobalRef(instance);

            contentPtr->load([iweak, contentPtr]() {
                              // only updateData params to be fetched and status  because
                              // all packages would be imported by this time
                              // by core via package manager. Unlike addPackage pathway
                              // load() gets all of the packages in one shot.
                                 handleLoadingResults(true, contentPtr, iweak);
                             },
                          [iweak, contentPtr]() { // failure
                             // No need to update data because import of packages have failed
                             // just update status
                              handleLoadingResults(false, contentPtr, iweak);
                          });
        }

        /**
         * Request packages from the view host.
         */
        void requestPackages(JNIEnv *env, jobject instance, const apl::ContentPtr &contentPtr) {

            if(shouldUsePackageManager(env, instance)) {
                loadPackagesWithPackageManager(contentPtr, instance);
            } else {

                // In case of no package manager, eg rootConfig is null, we go the old fashioned way
                auto request = contentPtr->getRequestedPackages();
                for (const auto &req: request) {

                    // use handle instead of id as a ref to the native peer
                    auto reqPtr = std::make_shared<apl::ImportRequest>(req);
                    auto handle = createHandle<ImportRequest>(reqPtr);

                    auto source = env->NewStringUTF(req.source().c_str());
                    auto name = env->NewStringUTF(req.reference().name().c_str());
                    auto version = env->NewStringUTF(req.reference().version().c_str());
                    auto domain = env->NewStringUTF(req.reference().domain().c_str());

                    env->CallVoidMethod(instance, REQUEST_PACKAGE, handle, source, name,
                                        version, domain);

                    env->DeleteLocalRef(source);
                    env->DeleteLocalRef(name);
                    env->DeleteLocalRef(version);
                    env->DeleteLocalRef(domain);

                }
            }

        }

        /**
         * Request data from the view host.
         */
        void requestData(JNIEnv *env, jobject instance, const ContentPtr &contentPtr) {

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
        void update(JNIEnv *env, jobject instance, const ContentPtr &content,
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
                LOG(LogLevel::kError) << "Content Error";
                env->CallVoidMethod(instance, ON_ERROR);
            } else if (content->isReady()) {
                LOG(LogLevel::kDebug) << "Content Ready";
                env->CallVoidMethod(instance, ON_READY);
            }
        }


        /**
         * Create a Content instance and attach it to the view host peer.
         */
        JNIEXPORT jlong JNICALL
        Java_com_amazon_apl_android_Content_nCreate(JNIEnv *env, jobject instance,
                                                    jstring mainTemplate_,
                                                    jlong _rootConfigHandler,
                                                    jlong _sessionHandler) {

            // for emoji characters, GetStringUTFChars returns the surrogate pair
            // eg. U+1F33D -> \uD83C\uDF3D
            // but rapidjson expects the 4-byte format \xF0\x9F\x8C\xBD
            // and returns a parsing error
            // so get UTF-16 using GetStringChars and convert it to UTF-8 manually
            // before passing it rapidjson
            const jchar* mainTemplate = env->GetStringChars(mainTemplate_, nullptr);
            jsize mainTemplateLen = env->GetStringLength(mainTemplate_);
            std::wstring_convert<std::codecvt_utf8_utf16<char16_t>, char16_t> converter;
            auto session = get<Session>(_sessionHandler);
            auto content = (_rootConfigHandler != 0) ? (apl::Content::create(converter.to_bytes(std::u16string(mainTemplate, mainTemplate+mainTemplateLen)), session, Metrics(), *(get<RootConfig>(_rootConfigHandler))))
                    : apl::Content::create(converter.to_bytes(std::u16string(mainTemplate, mainTemplate+mainTemplateLen)));
             if (!content) {
                LOG(LogLevel::kError) << "Error creating Content";
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
         * Refresh embedded document content with documentConfig
         */
        JNIEXPORT void JNICALL
        Java_com_amazon_apl_android_Content_nRefresh(JNIEnv *env, jobject obj,
                                                     jlong contentHandle,
                                                     jlong embeddedRequestHandle,
                                                     jlong documentConfigHandle) {
            auto content = get<Content>(contentHandle);
            auto embeddedRequest = get<AndroidEmbeddedDocumentRequest>(embeddedRequestHandle);
            auto documentConfig = get<DocumentConfig>(documentConfigHandle);

            content->refresh(*embeddedRequest->mEmbedRequest.get(), documentConfig);
        }

        /**
         * Add a requested package to the document.
         */
        JNIEXPORT void JNICALL
        Java_com_amazon_apl_android_Content_nAddPackage(JNIEnv *env, jobject instance,
                                                        jlong contentHandle,
                                                        jlong requestHandle,
                                                        jlong jsonDataHandle) {
            auto sharedJsonData = get<SharedJsonData>(jsonDataHandle);
            auto  jsonData = JsonData(*sharedJsonData);
            // Get the content from the handle
            auto jContent = get<Content>(contentHandle);
            // add the package
            auto it = get<ImportRequest>(requestHandle);
            jContent->addPackage(*it, jsonData.get());
            update(env, instance, jContent, true, true); // update packages, data, and status
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
            auto jContent = get<Content>(contentHandle);

            jContent->addData(dataName, converter.to_bytes(std::u16string(docContents, docContents+docContentsLen)));

            env->ReleaseStringChars(docContents_, docContents);
            env->ReleaseStringUTFChars(dataName_, dataName);
            update(env, instance, jContent, false, false); // only update status, no packages or data
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

        JNIEXPORT jstring JNICALL
        Java_com_amazon_apl_android_Content_nGetSerializedDocumentSettings(JNIEnv *env, jclass clazz, jlong contentHandle, jlong rootConfigHandle) {
            auto content = get<Content>(contentHandle);
            auto settings = content->getDocumentSettings();
            auto rootConfig = get<RootConfig>(rootConfigHandle);
            rapidjson::Document document(rapidjson::kObjectType);
            auto serializedSettings = settings->serialize(document.GetAllocator(), *rootConfig);
            rapidjson::StringBuffer buffer;
            rapidjson::Writer<rapidjson::StringBuffer> writer(buffer);
            serializedSettings.Accept(writer);
            std::u16string u16 = converter.from_bytes(buffer.GetString());
            return env->NewString(reinterpret_cast<const jchar *>(u16.c_str()), u16.length());
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
                LOG(LogLevel::kError) << "Error cannot get document background without content and rootConfig";
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
            jboolean isGradient = background.is<Gradient>();
            jboolean isColor = background.is<Color>();
            jint type = 0;
            jfloat angle = 0;
            jlong color = 0;

            if (isGradient) {
                auto gradient = background.get<Gradient>();
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
                LOG(LogLevel::kError) << "Error: document background should be color or gradient";
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
