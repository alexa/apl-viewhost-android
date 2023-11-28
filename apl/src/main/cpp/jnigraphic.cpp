/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

#include <jni.h>
#include <string>
#include "apl/apl.h"
#include "jniutil.h"
#include "jnimetricstransform.h"

namespace apl {
    namespace jni {

#ifdef __cplusplus

        extern "C" {
#endif

        static jclass GRAPHIC_CLASS;
        static jclass HASHSET_CLASS;
        static jclass INTEGER_CLASS;
        static jmethodID GRAPHIC_ADD_CHILDREN;
        static jmethodID HASHSET_CONSTRUCTOR;
        static jmethodID HASHSET_ADD;
        static jmethodID INTEGER_VALUEOF;


        /**
         * Create a class and method cache for calls to View Host.
         */
        jboolean
        graphic_OnLoad(JavaVM *vm, void *reserved) {

            LOG(apl::LogLevel::kDebug) << "Loading View Host Graphics JNI environment.";

            JNIEnv *env;
            if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
                return JNI_FALSE;
            }

            GRAPHIC_CLASS = (jclass)env->NewGlobalRef(
                                     env->FindClass("com/amazon/apl/android/graphic/GraphicElement"));
            GRAPHIC_ADD_CHILDREN = env->GetMethodID(GRAPHIC_CLASS, "addChildren", "(J)V");
            HASHSET_CLASS = reinterpret_cast<jclass>(env->NewGlobalRef(
                    env->FindClass("java/util/HashSet")));
            HASHSET_CONSTRUCTOR = env->GetMethodID(HASHSET_CLASS, "<init>", "()V");
            HASHSET_ADD = env->GetMethodID(HASHSET_CLASS, "add", "(Ljava/lang/Object;)Z");

            INTEGER_CLASS = (jclass)env->NewGlobalRef(
                    env->FindClass("java/lang/Integer"));
            INTEGER_VALUEOF = env->GetStaticMethodID(INTEGER_CLASS, "valueOf", "(I)Ljava/lang/Integer;");

            if (nullptr == GRAPHIC_CLASS
                || nullptr == GRAPHIC_ADD_CHILDREN
                || nullptr == HASHSET_CLASS
                || nullptr == HASHSET_CONSTRUCTOR
                || nullptr == HASHSET_ADD
                || nullptr == INTEGER_CLASS
                || nullptr == INTEGER_VALUEOF) {
                LOG(LogLevel::kError) << "Could not find class GraphicElement Constructor or GraphicElement::addChildren method";
                return JNI_FALSE;
            }

            return JNI_TRUE;
        }

        /**
         * Release the class and method cache.
         */
        void
        graphic_OnUnload(JavaVM *vm, void *reserved) {
            LOG(apl::LogLevel::kDebug) << "Unloading View Host Graphics JNI environment.";
            apl::LoggerFactory::instance().reset();

            JNIEnv *env;
            if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
                // environment failure, can't proceed.
                return;
            }
            env->DeleteGlobalRef(GRAPHIC_CLASS);
            env->DeleteGlobalRef(HASHSET_CLASS);
        }

        /**
         * Creates the handle to the root graphic element.
         * @param env
         * @param instance
         * @param componentHandle
         * @param propertyId
         * @return the constructed GraphicElement object.
         */
        JNIEXPORT jlong JNICALL
        Java_com_amazon_apl_android_VectorGraphic_nGetGraphic(JNIEnv *env,
                                                              jobject instance,
                                                              jlong componentHandle,
                                                              jint propertyId) {
            auto c = get<Component>(componentHandle);
            auto prop = static_cast<PropertyKey>(static_cast<int>(propertyId));
            auto g = c->getCalculated(prop).get<Graphic>();
            const auto root = g->getRoot();
            return createHandle<GraphicElement, GraphicPropertyLookup>(root);
        }

        /**
         * Calls the core layer for all the children of the current element.
         * @param env
         * @param instance
         * @param handle of the avg component.
         */
        JNIEXPORT void JNICALL
        Java_com_amazon_apl_android_graphic_GraphicElement_nInflateChildren(JNIEnv *env,
                                                                        jobject instance,
                                                                        jlong handle) {
            auto gc = get<apl::GraphicElement>(handle);
            for (size_t i = 0; i < gc->getChildCount(); ++i) {
                auto child = gc->getChildAt(i);
                auto childHandle = createHandle<GraphicElement, GraphicPropertyLookup>(child);
                env->CallVoidMethod(instance, GRAPHIC_ADD_CHILDREN,
                                    reinterpret_cast<jlong>(childHandle));
            }
        }

        /**
         * Returns the type of the graphic element
         * @param env  pointer to JNIEnv
         * @param clazz the GraphicElement class reference
         * @param handle pointer to the graphic element.
         */
        JNIEXPORT jint JNICALL
        Java_com_amazon_apl_android_graphic_GraphicElement_nGetType(JNIEnv *env,
                                                                    jobject instance,
                                                                    jlong handle) {
            auto gc = get<apl::GraphicElement>(handle);
            return static_cast<jint>(gc->getType());
        }

        /**
         * Returns the type of the graphic element
         * @param env  pointer to JNIEnv
         * @param clazz the GraphicElement class reference
         * @param handle pointer to the graphic element.
         */
        JNIEXPORT jint JNICALL
        Java_com_amazon_apl_android_graphic_GraphicElementFactory_nGetType(JNIEnv *env,
                                                                           jobject instance,
                                                                           jlong handle) {
            auto gc = get<apl::GraphicElement>(handle);
            return static_cast<jint>(gc->getType());
        }

        JNIEXPORT jint JNICALL
        Java_com_amazon_apl_android_graphic_GraphicElementFactory_nGetUniqueId(JNIEnv *env,
                                                                               jobject instance,
                                                                               jlong handle) {
            auto gc = get<apl::GraphicElement>(handle);
            return static_cast<jint>(gc->getId());
        }

        /**
         * Creates the handle to the root graphic element.
         * @param env
         * @param instance
         * @param componentHandle
         * @param propertyId
         * @return the constructed GraphicElement object.
         */
        JNIEXPORT void JNICALL
        Java_com_amazon_apl_android_VectorGraphic_nUpdateGraphic(JNIEnv *env,
                                                              jobject instance,
                                                              jlong componentHandle,
                                                              jstring json) {
            const char *data = env->GetStringUTFChars(json, nullptr);
            auto c = get<Component>(componentHandle);
            std::shared_ptr<GraphicContent> graphicContent = GraphicContent::create(data);
            c->updateGraphic(graphicContent);
            env->ReleaseStringUTFChars(json, data);
        }

        JNIEXPORT jint JNICALL
        Java_com_amazon_apl_android_graphic_GraphicElement_nGetUniqueId(JNIEnv *env, jobject instance, jlong handle) {
            auto g = get<GraphicElement>(handle);
            return static_cast<jint>(g->getId());
        }

        JNIEXPORT jobject JNICALL
        Java_com_amazon_apl_android_graphic_GraphicElement_nGetDirtyProperties(JNIEnv *env, jobject instance, jlong handle) {
            auto g = get<GraphicElement>(handle);
            auto dirtyProperties = g->getDirtyProperties();

            jobject javaSet = env->NewObject(HASHSET_CLASS, HASHSET_CONSTRUCTOR);

            for (auto key : dirtyProperties) {
                jobject intValue = env->CallStaticObjectMethod(INTEGER_CLASS, INTEGER_VALUEOF, static_cast<int>(key));
                env->CallBooleanMethod(javaSet, HASHSET_ADD, intValue);
            }

            return javaSet;
        }

        JNIEXPORT jobject JNICALL
        Java_com_amazon_apl_android_VectorGraphic_nGetDirtyGraphics(JNIEnv *env, jobject instance,
                                                                    jlong componentHandle) {
            auto g = get<Component>(componentHandle);
            auto graphicProperty = g->getCalculated(kPropertyGraphic);
            jobject javaSet = env->NewObject(HASHSET_CLASS, HASHSET_CONSTRUCTOR);
            if (graphicProperty.isNull()) {
                return javaSet;
            }

            auto graphic = graphicProperty.get<Graphic>();
            auto dirtyChildren = graphic->getDirty();
            for (const auto &it : dirtyChildren) {
                auto val = getJObject(env, it->getId());
                env->CallBooleanMethod(javaSet, HASHSET_ADD, val);
            }
            return javaSet;
        }

        JNIEXPORT jfloat JNICALL
        Java_com_amazon_apl_android_graphic_GraphicPattern_nGetWidth(JNIEnv *env, jclass clazz,
                jlong handle, jint propertyId) {

            auto value = getLookup<PropertyLookup>(handle)->getObject(static_cast<int>(propertyId), handle);
            const auto &g = value.get<GraphicPattern>();

            return static_cast<jfloat>(g->getWidth());
        }

        JNIEXPORT jfloat JNICALL
                Java_com_amazon_apl_android_graphic_GraphicPattern_nGetHeight(JNIEnv *env, jclass clazz,
                        jlong handle, jint propertyId) {

            auto value = getLookup<PropertyLookup>(handle)->getObject(static_cast<int>(propertyId), handle);
            const auto &g = value.get<GraphicPattern>();

            return static_cast<jfloat>(g->getHeight());
        }

        JNIEXPORT jlongArray JNICALL
        Java_com_amazon_apl_android_graphic_GraphicPattern_nGetItems(JNIEnv *env, jclass clazz,
                                                                     jlong handle, jint propertyId) {
            auto value = getLookup<PropertyLookup>(handle)->getObject(static_cast<int>(propertyId), handle);
            const auto &g = value.get<GraphicPattern>();


            int numItems = g->getItems().size();
            jlong graphicElementItems[numItems];
            for (int i = 0; i < numItems; i++) {
                GraphicElementPtr pattern = g->getItems()[i];
                graphicElementItems[i] = createHandle<GraphicElement, GraphicPropertyLookup>(pattern);
            }
            jlongArray graphicElementRange = env->NewLongArray(numItems);
            env->SetLongArrayRegion(graphicElementRange, 0, numItems, graphicElementItems);
            return graphicElementRange;
        }


#ifdef __cplusplus
}



#endif

} //namespace jni
} //namespace apl
