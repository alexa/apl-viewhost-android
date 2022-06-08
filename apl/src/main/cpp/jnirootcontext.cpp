/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

#include <jni.h>
#include <string>
#include "apl/apl.h"
#include "apl/dynamicdata.h"
#include <list>
#include <queue>

#include "jniutil.h"
#include "jnimetricstransform.h"
#include "jnitextmeasurecallback.h"
#include "loggingbridge.h"
#include "scaling.h"
#include "touch/pointerevent.h"
#include <codecvt>

namespace apl {
    namespace jni {

#ifdef __cplusplus
        extern "C" {
#endif
#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wunused-parameter"

        const bool DEBUG_JNI = true;

        static std::wstring_convert<std::codecvt_utf8_utf16<char16_t>, char16_t> converter;

        // Access APL view host RootContext class.
        static jclass ROOTCONTEXT_CLASS;
        static jmethodID ROOTCONTEXT_BUILD_COMPONENT;
        static jmethodID ROOTCONTEXT_UPDATE_COMPONENT;
        static jmethodID ROOTCONTEXT_HANDLE_EVENT;
        static jmethodID ROOTCONTEXT_COMPONENT_HANDLE;
        static jmethodID ROOTCONTEXT_TO_UPPER;
        static jmethodID ROOTCONTEXT_TO_LOWER;
        static jclass JAVA_UTIL_LINKEDHASHMAP;
        static jmethodID JAVA_UTIL_LINKEDHASHMAP_CONSTRUCTOR;
        static jmethodID JAVA_UTIL_LINKEDHASHMAP_PUT;

        static JavaVM* JAVA_VM;

        /**
         * Initialize and cache java class and method handles for callback to the rendering layer.
         * Called from JNI_OnLoad when the native library is loaded (for example, through System.loadLibrary).
         */
        jboolean
        rootcontext_OnLoad(JavaVM *vm, void *reserved) {

            apl::LoggerFactory::instance().initialize(std::make_shared<AndroidJniLogBridge>());

            LOG(apl::LogLevel::DEBUG) << "Loading View Host RootContext JNI environment.";

            JAVA_VM = vm;
            JNIEnv *env;
            if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
                return JNI_FALSE;
            }

            // method signatures can be obtained with 'javap -s'
            ROOTCONTEXT_CLASS = reinterpret_cast<jclass>(env->NewGlobalRef(
                    env->FindClass("com/amazon/apl/android/RootContext")));
            ROOTCONTEXT_BUILD_COMPONENT = env->GetMethodID(ROOTCONTEXT_CLASS,
                                                            "buildComponent",
                                                            "(Ljava/lang/String;JI)V");
            ROOTCONTEXT_UPDATE_COMPONENT = env->GetMethodID(ROOTCONTEXT_CLASS,
                                                            "callbackUpdateComponent",
                                                            "(Ljava/lang/String;[I)V");
            ROOTCONTEXT_HANDLE_EVENT = env->GetMethodID(ROOTCONTEXT_CLASS,
                                                        "callbackHandleEvent",
                                                        "(JI)V");
            ROOTCONTEXT_COMPONENT_HANDLE = env->GetMethodID(ROOTCONTEXT_CLASS, "getComponentHandle",
                                                   "(Ljava/lang/String;)J");
            ROOTCONTEXT_TO_UPPER = env->GetStaticMethodID(ROOTCONTEXT_CLASS, "callbackToUpperCase", "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;");
            ROOTCONTEXT_TO_LOWER = env->GetStaticMethodID(ROOTCONTEXT_CLASS, "callbackToLowerCase", "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;");

            JAVA_UTIL_LINKEDHASHMAP = reinterpret_cast<jclass>(env->NewGlobalRef(env->FindClass("java/util/LinkedHashMap")));
            JAVA_UTIL_LINKEDHASHMAP_CONSTRUCTOR = env->GetMethodID(JAVA_UTIL_LINKEDHASHMAP, "<init>", "()V");
            JAVA_UTIL_LINKEDHASHMAP_PUT = env->GetMethodID(JAVA_UTIL_LINKEDHASHMAP, "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");

            if (nullptr == ROOTCONTEXT_BUILD_COMPONENT
                || nullptr == ROOTCONTEXT_UPDATE_COMPONENT
                || nullptr == ROOTCONTEXT_HANDLE_EVENT
                || nullptr == ROOTCONTEXT_COMPONENT_HANDLE
                ) {
                LOG(apl::LogLevel::ERROR)
                        << "Could not load methods for class com.amazon.apl.android.RootContext";
                return JNI_FALSE;
            }

            return JNI_TRUE;
        }


        /**
         * Release the class and method cache.
         * Called from JNI_OnUnload when the class loader containing the native library is garbage collected.
         */
        void
        rootcontext_OnUnload(JavaVM *vm, void *reserved) {

            LOG(apl::LogLevel::DEBUG) << "Unloading RootContext JNI environment.";
            apl::LoggerFactory::instance().reset();

            JNIEnv *env;
            if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
                // environment failure, can't proceed.
                return;
            }

            JAVA_VM = nullptr;

            env->DeleteGlobalRef(ROOTCONTEXT_CLASS);
        }

        /**
         * LocaleMethods delegate for JNI.
         */
        class JniLocaleMethods : public LocaleMethods {
        public:
            JniLocaleMethods() {}

            std::string toUpperCase( const std::string& value, const std::string& locale ) {
                return callJavaMethod(value, locale, ROOTCONTEXT_TO_UPPER);
            }

            std::string toLowerCase( const std::string& value, const std::string& locale ) {
                return callJavaMethod(value, locale, ROOTCONTEXT_TO_LOWER);
            }

        private:
            std::string callJavaMethod(const std::string& value, const std::string& locale, jmethodID java_method) {
                JNIEnv *env;
                if (JAVA_VM->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
                    // environment failure, can't proceed.
                    return "";
                }

                std::u16string u16 = converter.from_bytes(value.c_str());
                jstring stringParam = env->NewString(reinterpret_cast<const jchar *>(u16.c_str()), u16.length());
                jstring localeParam = env->NewStringUTF(locale.c_str());
                jstring resultString = (jstring)env->CallStaticObjectMethod(ROOTCONTEXT_CLASS, java_method, stringParam, localeParam);
                const char* chars = env->GetStringUTFChars(resultString, nullptr);
                std::string result = std::string(chars);

                env->DeleteLocalRef(stringParam);
                env->DeleteLocalRef(localeParam);
                env->ReleaseStringUTFChars(resultString, chars);
                env->DeleteLocalRef(resultString);
                return result;
            }
        };



        /**
         * Create a RootContext from a Metrics and Content handle and attaches it to the view host peer.
         */
        JNIEXPORT jlong JNICALL
        Java_com_amazon_apl_android_RootContext_nCreate(JNIEnv *env, jobject instance,
                                                                  jlong contentHandle,
                                                                  jlong rootConfigHandle,
                                                                  jlong textMeasureHandle,
                                                                  jint width,
                                                                  jint height,
                                                                  jint dpi,
                                                                  jint shape,
                                                                  jstring theme_,
                                                                  jint mode) {
            auto content = get<Content>(contentHandle);
            auto rootConfig = get<RootConfig>(rootConfigHandle);
            auto measure = get<JniTextMeasurement>(textMeasureHandle);

            // If the document states version 1.0, then set the old defaults
            // for backwards compatibility
            if(content->getAPLVersion() == "1.0") {
                LOG(LogLevel::DEBUG) << "Setting APL 1.0 default property values";
                Dimension _100p(DimensionType::Relative, 100);
                rootConfig->defaultComponentSize(kComponentTypeSequence, _100p, _100p);
                rootConfig->defaultComponentSize(kComponentTypeScrollView, _100p, _100p);
                rootConfig->defaultComponentSize(kComponentTypePager, _100p, _100p);
            }

            rootConfig->measure(measure);

            auto localeMethods = std::make_shared<JniLocaleMethods>();
            rootConfig->localeMethods(localeMethods);

            // TODO: Ignore for now.To be changed when imports versions fixed.
            rootConfig->enforceAPLVersion(APLVersion::kAPLVersionIgnore);

            const char* theme = env->GetStringUTFChars(theme_, nullptr);
            auto metrics = Metrics()
                    .size(static_cast<int>(width), static_cast<int>(height))
                    .shape(static_cast<ScreenShape>(shape))
                    .theme(theme)
                    .dpi(static_cast<int>(dpi))
                    .mode(static_cast<ViewportMode>(mode));
            env->ReleaseStringUTFChars(theme_, theme);
            RootContextPtr context = RootContext::create(metrics, content, *rootConfig);

            if(!context) {
                LOG(LogLevel::ERROR) << "Error creating RootContext";
                return static_cast<jlong>(0);
            }
            return createHandle<RootContext>(context);
        }


        /**
         * Inflates a Component hierarchy from given root Component into the rendering layer.
         * I.e. this method informs the Java layer about Component objects in Core-layer so that it
         * can create the corresponding Component Objects in Java.
         *
         * @param env
         * @param instance
         * @param rootContextHandle
         * @param rootComponent The root of the hierarchy to inflate. This does not necessarily need
         *                      to be the root of the document. E.g. it could be a lazily-loaded Sequence/Pager child
         */
        static void inflateComponentHierarchy(
                JNIEnv *env,
                jobject instance,
                const std::shared_ptr<RootContext>& rootContext,
                const ComponentPtr& rootComponent) {

            std::queue<ComponentPtr> queue;
            queue.push(rootComponent);

            /* NOTE
             * this loop could move to view host and the v.h. call into jni.
             * However multiple values are needed to create a component, this would require
             * a jni call per value, wrapping the collection of params in a complex object,
             * or instantiating the view host Component via JNI calls to constructor in
             * these cases it's less performance overhead to have the v.h. initiate
             * a callback procedure.
             */
            while (!queue.empty()) {
                // pop apl component
                auto component = queue.front();
                queue.pop();

                auto id = env->NewStringUTF(component->getUniqueId().c_str());
                //Reuse the handle if we have already created this component before, otherwise
                //create a new handle.
                auto handle = env->CallLongMethod(instance, ROOTCONTEXT_COMPONENT_HANDLE, id);
                if (handle == 0) {
                    handle = createHandle<Component, ComponentPropertyLookup>(component);
                }

                // Call the rendering layer to add/update the component
                env->CallVoidMethod(instance, ROOTCONTEXT_BUILD_COMPONENT,
                                    id,
                                    handle,
                                    static_cast<int>(component->getType()));
                env->DeleteLocalRef(id);
                if (env->ExceptionCheck()) {
                    env->ExceptionDescribe();
                    LOG(apl::LogLevel::ERROR)
                            << " Failed to update component. type:" << component->getType()
                            << " id:" << component->getUniqueId();
                    env->ExceptionDescribe();
                    env->ExceptionClear();
                }

                // insert children into the stack
                for (size_t i = 0; i < component->getChildCount(); i++) {
                    queue.push(component->getChildAt(i));
                }
            }
        }

        /**
         * Inflates document from root, initiated by the view host.
         */
        JNIEXPORT void JNICALL
        Java_com_amazon_apl_android_RootContext_nInflate(JNIEnv *env, jobject instance,
                                                                    jlong nativeHandle) {
            auto rootContext = get<RootContext>(nativeHandle);

            inflateComponentHierarchy(
                    env,
                    instance,
                    rootContext,
                    rootContext->topComponent());
        }

        /**
         * Re-inflates document from root, initiated by the view host.
         */
        JNIEXPORT jboolean JNICALL
        Java_com_amazon_apl_android_RootContext_nReinflate(JNIEnv *env, jobject instance,
                                                         jlong nativeHandle) {
            auto rootContext = get<RootContext>(nativeHandle);
            rootContext->reinflate();

            if (rootContext->topComponent() == nullptr) {
                return false;
            }
            inflateComponentHierarchy(
                    env,
                    instance,
                    rootContext,
                    rootContext->topComponent());
            return true;
        }

        JNIEXPORT void JNICALL
        Java_com_amazon_apl_android_RootContext_nInflateComponentWithUniqueId(JNIEnv *env, jobject instance,
                                                         jlong nativeHandle,
                                                         jstring uid_) {
            auto rootContext = get<RootContext>(nativeHandle);
            const char *uid = env->GetStringUTFChars(uid_, nullptr);
            auto component = rootContext->findComponentById(uid);

            if (component) {
                inflateComponentHierarchy(
                        env,
                        instance,
                        rootContext,
                        component);
            }

            env->ReleaseStringUTFChars(uid_, uid);
        }

        JNIEXPORT void JNICALL
        Java_com_amazon_apl_android_RootContext_nScrollToRectInComponent(JNIEnv *env, jclass clazz,
                jlong handle, jlong componentHandle, jint x, jint y, jint w, jint h, jint align) {
            auto rc = get<RootContext>(handle);
            auto component = get<Component>(componentHandle);
            Rect rect(static_cast<int>(x), static_cast<int>(y),
                 static_cast<int>(w), static_cast<int>(h));

            rc->scrollToRectInComponent(component, rect, static_cast<CommandScrollAlign>(align));
        }

        /**
         * @return The String identifier of the top Component in the Component hierarchy.
         */
        JNIEXPORT jstring JNICALL
        Java_com_amazon_apl_android_RootContext_nGetTopComponent(JNIEnv *env, jclass clazz,
                                                                 jlong handle) {

            auto rc = get<RootContext>(handle);
            auto top = rc->topComponent();
            if (top == nullptr) {
                return NULL;
            }
            // TODO get an int value for id instead of JNI string overhead.
            return env->NewStringUTF(top->getUniqueId().c_str());
        }


        /**
         * Executes APL Commands.
         */
        JNIEXPORT jlong JNICALL
        Java_com_amazon_apl_android_RootContext_nExecuteCommands(JNIEnv *env, jclass clazz,
                                                                 jlong handle,
                                                                 jstring commands_) {
            auto rc = get<RootContext>(handle);
            auto* doc = new rapidjson::Document();
            const char* commands = env->GetStringUTFChars(commands_, nullptr);
            doc->Parse(commands);
            env->ReleaseStringUTFChars(commands_, commands);
            apl::Object obj = apl::Object(*doc);
            auto action = rc->executeCommands(obj, false);
            if (action == nullptr) {
                return 0;
            }
            action->setUserData(doc);
            // The action is always bound to a jniaction, which adds its own then callback.
            // No need to do it here.

            return createHandle<Action>(action);
        }

        /**
        * Invoke an extension handller.
        */
        JNIEXPORT jlong JNICALL
        Java_com_amazon_apl_android_RootContext_nInvokeExtensionEventHandler(JNIEnv *env,
                                                                             jclass clazz,
                                                                             jlong handle,
                                                                             jstring uri_,
                                                                             jstring name_,
                                                                             jobject data_,
                                                                             jboolean fastmode) {
            auto rc = get<RootContext>(handle);


            const char *uri = env->GetStringUTFChars(uri_, nullptr);
            const char *name = env->GetStringUTFChars(name_, nullptr);

            auto data = getAPLObject(env, data_);

            auto obj = getAPLObject(env, data_);
            auto map = obj.isNull() ? *std::make_shared<ObjectMap>() : obj.getMap();

            auto action = rc->invokeExtensionEventHandler(uri, name, map, fastmode);

            env->ReleaseStringUTFChars(uri_, uri);
            env->ReleaseStringUTFChars(name_, name);
            if (action == nullptr) {
                return 0;
            }

            return createHandle<Action>(action);


        }

        JNIEXPORT jlong JNICALL
        Java_com_amazon_apl_android_RootContext_nElapsedTime(JNIEnv *env, jclass clazz,
                                                                 jlong handle) {
            auto rc = get<RootContext>(handle);
            return static_cast<jlong>(rc->currentTime());
        }

        JNIEXPORT void JNICALL
        Java_com_amazon_apl_android_RootContext_nCancelExecution(JNIEnv *env, jclass clazz,
                                                                 jlong handle) {

            auto rc = get<RootContext>(handle);
            rc->cancelExecution();
        }

        JNIEXPORT void JNICALL
        Java_com_amazon_apl_android_RootContext_updateTime(JNIEnv *env, jclass clazz, jlong handle,
                                                           jlong frameTime, jlong utcTime) {

            auto rc = get<RootContext>(handle);
            rc->updateTime(static_cast<apl_time_t >(frameTime), static_cast<apl_time_t>(utcTime));
        }

        JNIEXPORT void JNICALL
        Java_com_amazon_apl_android_RootContext_nSetLocalTimeAdjustment(JNIEnv *env, jclass clazz,
                                                                        jlong handle, jlong adjustment) {
            auto rc = get<RootContext>(handle);
            rc->setLocalTimeAdjustment(static_cast<apl_duration_t>(adjustment));
        }

        JNIEXPORT void JNICALL
        Java_com_amazon_apl_android_RootContext_nClearPending(JNIEnv *env, jobject instance,
                                                              jlong nativeHandle) {
            auto rootContext = get<RootContext>(nativeHandle);
            rootContext->clearPending();
        }

        void
        handleEvent(JNIEnv *env, jobject instance, apl::Event event) {
            // we need to make this a long lasting object, so use copy constructor
            // to put this event on the heap.
            std::shared_ptr<Event> eventPtr(new apl::Event(std::move(event)));
            env->CallVoidMethod(instance,
                    ROOTCONTEXT_HANDLE_EVENT,
                                createHandle<Event, EventCommandPropertyLookup>(eventPtr),
                    eventPtr->getType());
        }


        /**
        * Update outstanding events.
        */
        JNIEXPORT void JNICALL
        Java_com_amazon_apl_android_RootContext_nHandleEvents(JNIEnv *env, jobject instance, jlong handle) {
            auto rc = get<RootContext>(handle);
            while (rc->hasEvent()) {
                auto evt = rc->popEvent();
                handleEvent(env, instance, evt);

                /* If an unexpected error occurs during the execution of the event,
                 * the frame loop will recover and will continue executing the remaining events.
                 */
                if (env->ExceptionCheck()) {
                    LOG(apl::LogLevel::ERROR)
                            << " Failed to handle event. Type:" << evt.getType();
                    env->ExceptionDescribe();
                    env->ExceptionClear();
                }
                // Stop processing events if Reinflate event is just handled.
                if (evt.getType() == kEventTypeReinflate) {
                    break;
                }
            }
        }

        /**
         * Push dirty properties to the rendering layer. Called by render layer on kDocumentDirty.
         */
        JNIEXPORT void JNICALL
        Java_com_amazon_apl_android_RootContext_nHandleDirtyProperties(JNIEnv *env,
                                                                       jobject instance,
                                                                       jlong handle) {
            /* NOTE
             * this loop could move to view host and the v.h. call into jni.
             * However multiple values are needed to create a component, this would require
             * a jni call per value, wrapping the collection of params in a complex object,
             * or instantiating the view host Component via JNI calls to constructor in
             * these cases it's less performance overhead to have the v.h. initiate
             * a callback proceedure.
             */
            auto rc = get<RootContext>(handle);
            if (rc->isDirty()) {

                // core could potentially add to the dirty set while set is being iterated below
                // so make a copy of the dirty component ptr(s) and their dirty properties
                std::set<std::pair<ComponentPtr, std::vector<int>>> dirtyComponents;
                for (auto &c : rc->getDirty()) {
                    dirtyComponents.emplace(c, std::vector<int>(c->getDirty().begin(), c->getDirty().end()));
                }

                for (const auto& dirtyComponent : dirtyComponents) {
                    auto c = dirtyComponent.first;

                    // Pass the dirty properties for this Component to the Java callback
                    auto dirtyProperties = dirtyComponent.second;
                    unsigned int size = dirtyProperties.size();
                    jintArray jniDirtyPropertiesArray = env->NewIntArray(size);
                    env->SetIntArrayRegion(jniDirtyPropertiesArray, 0, size, &dirtyProperties[0]);

                    // TODO get an int value for id instead of JNI string overhead.
                    jstring cid = env->NewStringUTF(c->getUniqueId().c_str());

                    env->CallVoidMethod(instance, ROOTCONTEXT_UPDATE_COMPONENT, cid, jniDirtyPropertiesArray);

                    env->DeleteLocalRef(cid);
                    env->DeleteLocalRef(jniDirtyPropertiesArray);
                }
                // clearDirty will result in core also clearing the kPropertyNotifyChildrenChanged values, so don't clearDirty until after all the dirty properties have been processed
                rc->clearDirty();

            }
        }

        /**
         * Returns the custom device- or runtime-specific setting from the core to viewhost.
         */
        JNIEXPORT jobject JNICALL
        Java_com_amazon_apl_android_RootContext_nSetting(JNIEnv *env,
                                                         jclass clazz,
                                                         jlong handle,
                                                         jstring propertyName_) {
            auto rc = get<RootContext>(handle);
            const char *propertyName = env->GetStringUTFChars(propertyName_, nullptr);
            auto value = rc->content()->getDocumentSettings()->getValue(propertyName);
            env->ReleaseStringUTFChars(propertyName_, propertyName);
            return getJObject(env, value);
        }

        /**
         * Returns true if the root context is dirty
         */
        JNIEXPORT jboolean JNICALL
        Java_com_amazon_apl_android_RootContext_nIsDirty(JNIEnv *env,
                                                         jclass clazz,
                                                         jlong handle) {
            auto rc = get<RootContext>(handle);
            return static_cast<jboolean>(rc->isDirty());
        }

        /**
         * Returns true if the screenlock is on.
         */
        JNIEXPORT jboolean JNICALL
        Java_com_amazon_apl_android_RootContext_nIsScreenLocked(JNIEnv *env,
                                                                jclass clazz,
                                                                jlong handle) {
            auto rc = get<RootContext>(handle);
            return static_cast<jboolean>(rc->screenLock());
        }

        JNIEXPORT jstring JNICALL
        Java_com_amazon_apl_android_RootContext_nGetVersionCode(JNIEnv *env,
                                                               jclass clazz,
                                                               jlong handle) {
            auto rc = get<RootContext>(handle);
            auto content = rc->content();
            return env->NewStringUTF(content->getAPLVersion().c_str());
        }

        /**
         * Sends an update message to the focused component when a key is pressed.
         */
        JNIEXPORT jboolean JNICALL
        Java_com_amazon_apl_android_RootContext_nHandleKeyboard(JNIEnv *env,
                                                                jclass clazz,
                                                                jlong handle,
                                                                jint keyHandlerType,
                                                                jstring code_,
                                                                jstring key_,
                                                                jboolean repeat,
                                                                jboolean shiftKey,
                                                                jboolean altKey,
                                                                jboolean ctrlKey,
                                                                jboolean metaKey) {
            auto rc = get<RootContext>(handle);
            auto type = static_cast<KeyHandlerType>(keyHandlerType);

            const char *code = env->GetStringUTFChars(code_, nullptr);
            const char *key = env->GetStringUTFChars(key_, nullptr);

            auto keyboard = Keyboard(code, key)
                    .repeat(static_cast<bool>(repeat))
                    .shift(static_cast<bool>(shiftKey))
                    .alt(static_cast<bool>(altKey))
                    .ctrl(static_cast<bool>(ctrlKey))
                    .meta(static_cast<bool>(metaKey));
            auto processed = rc->handleKeyboard(type, keyboard);
            env->ReleaseStringUTFChars(code_, code);
            env->ReleaseStringUTFChars(key_, key);
            return static_cast <jboolean>(processed);
        }

        JNIEXPORT jboolean JNICALL
        Java_com_amazon_apl_android_RootContext_nHandlePointerEvent(JNIEnv *env, jclass clazz, jlong handle, jint pointerId, jint pointerType, jint pointerEventType, jfloat x, jfloat y) {
            auto rc = get<RootContext>(handle);
            auto pointerEvent = PointerEvent(
                    static_cast<PointerEventType>(pointerEventType),
                    Point(static_cast<float>(x), static_cast<float>(y)),
                    static_cast<id_type>(pointerId),
                    static_cast<PointerType>(pointerType));
            return static_cast<jboolean>(rc->handlePointerEvent(pointerEvent));
        }

        JNIEXPORT void JNICALL
        Java_com_amazon_apl_android_RootContext_nHandleConfigurationChange(JNIEnv *env,
                                                                           jclass clazz,
                                                                           jlong handle,
                                                                           jint width, jint height,
                                                                           jstring theme_,
                                                                           jint viewportMode,
                                                                           jfloat fontScale,
                                                                           jint screenMode,
                                                                           jboolean screenReader,
                                                                           jboolean disallowVideo,
                                                                           jobject environmentValues) {
            auto rc = get<RootContext>(handle);
            const char* theme = env->GetStringUTFChars(theme_, nullptr);
            auto configurationChange = ConfigurationChange(width, height)
                    .theme(theme)
                    .mode(static_cast<ViewportMode>(viewportMode))
                    .fontScale(fontScale)
                    .screenMode(static_cast<RootConfig::ScreenMode>(screenMode))
                    .screenReader(screenReader)
                    .disallowVideo(disallowVideo);
            auto envValues = getAPLObject(env, environmentValues);
            if (!envValues.isNull()) {
                const auto &envMap = envValues.getMap();
                for (auto &item: envMap) {
                    configurationChange.environmentValue(item.first, item.second);
                }
            }
            env->ReleaseStringUTFChars(theme_, theme);
            rc->configurationChange(configurationChange);
        }

        JNIEXPORT void JNICALL
        Java_com_amazon_apl_android_RootContext_nUpdateDisplayState(JNIEnv *env,
                                                                    jclass clazz,
                                                                    jlong handle,
                                                                    jint displayState) {
            auto rc = get<RootContext>(handle);
            rc->updateDisplayState(static_cast<DisplayState>(displayState));
        }

        JNIEXPORT jboolean JNICALL
        Java_com_amazon_apl_android_RootContext_nUpdateDataSource(
                JNIEnv *env,
                jclass clazz,
                jlong handle,
                jstring type_,
                jstring payload_) {
            auto rc = get<RootContext>(handle);

            const char* type = env->GetStringUTFChars(type_, nullptr);

            auto provider = rc->getRootConfig().getDataSourceProvider(type);

            if (!provider)
                return JNI_FALSE;

            const char *payload = env->GetStringUTFChars(payload_, nullptr);
            bool processed = provider->processUpdate(payload);
            env->ReleaseStringUTFChars(payload_, payload);
            env->ReleaseStringUTFChars(type_, type);

            return static_cast <jboolean>(processed);
        }

        JNIEXPORT jobject JNICALL
        Java_com_amazon_apl_android_RootContext_nGetDataSourceErrors(
                JNIEnv *env,
                jclass clazz,
                jlong handle) {
            auto rc = get<RootContext>(handle);

            auto knownDataSources = {
                    apl::DynamicIndexListConstants::DEFAULT_TYPE_NAME,
                    apl::DynamicTokenListConstants::DEFAULT_TYPE_NAME,
            };

            std::vector<apl::Object> errorArray;

            for (auto& type : knownDataSources) {

                auto provider = rc->getRootConfig().getDataSourceProvider(type);
                if (provider) {
                    auto pendingErrors = provider->getPendingErrors();
                    if (!pendingErrors.empty() && pendingErrors.isArray()) {
                        errorArray.insert(errorArray.end(), pendingErrors.getArray().begin(), pendingErrors.getArray().end());
                    }
                }
            }
            auto errors = apl::Object(std::make_shared<apl::ObjectArray>(errorArray));

            if(!errors.isArray() || errors.empty()) {
                return nullptr;
            }
            return getJObject(env, errors);
        }

        JNIEXPORT jboolean JNICALL
        Java_com_amazon_apl_android_RootContext_nIsVisualContextDirty(JNIEnv *env,
                                                                 jclass clazz,
                                                                 jlong handle) {
            auto rc = get<RootContext>(handle);
            return static_cast<jboolean>(rc->isVisualContextDirty());
        }

        JNIEXPORT jboolean JNICALL
        Java_com_amazon_apl_android_RootContext_nIsDataSourceContextDirty(JNIEnv *env,
                                                                          jclass clazz,
                                                                          jlong handle) {
            auto rc = get<RootContext>(handle);
            return static_cast<jboolean>(rc->isDataSourceContextDirty());
        }

        JNIEXPORT jstring JNICALL
        Java_com_amazon_apl_android_RootContext_nSerializeVisualContext(JNIEnv *env,
                                                                        jclass clazz,
                                                                        jlong handle) {
            auto rc = get<RootContext>(handle);
            rapidjson::Document document(rapidjson::kObjectType);
            auto context = rc->serializeVisualContext(document.GetAllocator());
            rapidjson::StringBuffer buffer;
            rapidjson::Writer<rapidjson::StringBuffer> writer(buffer);
            context.Accept(writer);
            std::u16string u16 = converter.from_bytes(buffer.GetString());
            return env->NewString(reinterpret_cast<const jchar *>(u16.c_str()), u16.length());
        }

        JNIEXPORT jstring JNICALL
        Java_com_amazon_apl_android_RootContext_nSerializeDataSourceContext(JNIEnv *env,
                                                                            jclass clazz,
                                                                            jlong handle) {
            auto rc = get<RootContext>(handle);
            rapidjson::Document document(rapidjson::kObjectType);
            auto context = rc->serializeDataSourceContext(document.GetAllocator());
            rapidjson::StringBuffer buffer;
            rapidjson::Writer<rapidjson::StringBuffer> writer(buffer);
            context.Accept(writer);
            return env->NewStringUTF(buffer.GetString());
        }

        JNIEXPORT void JNICALL
        Java_com_amazon_apl_android_RootContext_nUpdateFrameTime(JNIEnv *env,
                                                                 jclass clazz,
                                                                 jlong handle, jlong time) {
            auto rc = get<RootContext>(handle);
            rc->updateTime(time);
        }

        JNIEXPORT jboolean JNICALL
        Java_com_amazon_apl_android_RootContext_nNextFocus(JNIEnv *env, jclass clazz, jlong handle,
                                                           jint focus_direction) {
            auto rc = get<RootContext>(handle);
            auto direction = static_cast<FocusDirection>(focus_direction);
            return static_cast<jboolean>(rc->nextFocus(direction));
        }

        JNIEXPORT void JNICALL
        Java_com_amazon_apl_android_RootContext_nClearFocus(JNIEnv *env, jclass clazz, jlong handle) {
            auto rc = get<RootContext>(handle);
            rc->clearFocus();
        }

        JNIEXPORT jobject JNICALL
        Java_com_amazon_apl_android_RootContext_nGetFocusableAreas(JNIEnv *env, jclass clazz,
                                                                   jlong handle) {
            auto rc = get<RootContext>(handle);
            const std::map<std::string, Rect> &map = rc->getFocusableAreas();

            jobject jmap = env->NewObject(JAVA_UTIL_LINKEDHASHMAP, JAVA_UTIL_LINKEDHASHMAP_CONSTRUCTOR);

            for (auto const& x : map)
            {
                float buffer[4] = { x.second.getX(),
                                x.second.getY(),
                                x.second.getWidth(),
                                x.second.getHeight()};

                jfloatArray jrect = env->NewFloatArray(4);
                env->SetFloatArrayRegion(jrect, 0, 4, buffer);
                jstring key = env->NewStringUTF(x.first.c_str());

                env->CallObjectMethod(
                        jmap,
                        JAVA_UTIL_LINKEDHASHMAP_PUT,
                        key,
                        jrect);
            }

            return jmap;
        }

        JNIEXPORT jboolean JNICALL
        Java_com_amazon_apl_android_RootContext_nSetFocus(JNIEnv *env, jclass clazz, jlong handle,
                                                          jint focus_direction,
                                                          jfloat origin_x,
                                                          jfloat origin_y,
                                                          jfloat origin_width,
                                                          jfloat origin_height,
                                                          jstring target_id_) {
            auto rc = get<RootContext>(handle);
            FocusDirection direction = static_cast<FocusDirection>(focus_direction);

            const char *target_id = env->GetStringUTFChars(target_id_, nullptr);
            Rect origin_rect = Rect(origin_x,origin_y,origin_width, origin_height);

            bool value = rc->setFocus(direction, origin_rect, target_id);
            env->ReleaseStringUTFChars(target_id_, target_id);

            return value;
        }

        JNIEXPORT jstring JNICALL
        Java_com_amazon_apl_android_RootContext_nGetFocusedComponent(JNIEnv *env, jclass clazz,
                                                                   jlong handle) {
            auto rc = get<RootContext>(handle);
            auto focusedComponent = rc->getFocused();
            return env->NewStringUTF(focusedComponent.c_str());
        }

        /**
         * Notifies core when media is loaded.
         */
        JNIEXPORT void JNICALL
        Java_com_amazon_apl_android_RootContext_nMediaLoaded(JNIEnv *env, jclass clazz,
                                                             jlong handle,
                                                             jstring url_) {
            const char* url = env->GetStringUTFChars(url_, nullptr);

            auto rc = get<RootContext>(handle);
            rc->mediaLoaded(url);
            env->ReleaseStringUTFChars(url_, url);
        }

        /**
         * Notifies core when a media load has failed
         */
        JNIEXPORT void JNICALL
        Java_com_amazon_apl_android_RootContext_nMediaLoadFailed(JNIEnv *env,
                                                                 jclass clazz,
                                                                 jlong handle,
                                                                 jstring url_,
                                                                 jint errorCode,
                                                                 jstring error_) {
            const char* url = env->GetStringUTFChars(url_, nullptr);
            const char* error = env->GetStringUTFChars(error_, nullptr);

            auto rc = get<RootContext>(handle);
            rc->mediaLoadFailed(url, errorCode, error);
            env->ReleaseStringUTFChars(url_, url);
            env->ReleaseStringUTFChars(error_, error);
        }

#pragma clang diagnostic pop

#ifdef __cplusplus
        }
#endif

    } //namespace jni
} //namespace apl


