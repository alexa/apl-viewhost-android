/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

#include <jni.h>
#include <string>
#include <jnimediaplayer.h>

#include "rapidjson/document.h"
#include "rapidjson/writer.h"
#include "rapidjson/stringbuffer.h"
#include "apl/apl.h"
#include "jniutil.h"
#include "jnimetricstransform.h"

namespace apl {
    namespace jni {

#ifdef __cplusplus
        extern "C" {
#endif

        const bool DEBUG_JNI = true;

        JNIEXPORT jint JNICALL
        Java_com_amazon_apl_android_Component_nGetType(JNIEnv *env, jclass clazz, jlong handle) {

            auto c = get<Component>(handle);
            return static_cast<jint>(c->getType());
        }

        JNIEXPORT jstring JNICALL
        Java_com_amazon_apl_android_Component_nGetUniqueId(JNIEnv *env, jclass clazz,
                                                           jlong handle) {

            auto c = get<Component>(handle);
            return env->NewStringUTF(c->getUniqueId().c_str());
        }

        JNIEXPORT jstring JNICALL
        Java_com_amazon_apl_android_Component_nGetId(JNIEnv *env, jclass clazz, jlong handle) {
            auto c = get<Component>(handle);
            return env->NewStringUTF(c->getId().c_str());
        }

        JNIEXPORT jstring JNICALL
        Java_com_amazon_apl_android_Component_nGetParentId(JNIEnv *env, jclass clazz,
                                                           jlong handle) {

            auto c = get<Component>(handle);
            auto p = c->getParent();
            if (p == nullptr) {
                return nullptr;
            }
            return env->NewStringUTF(p->getUniqueId().c_str());
        }

        JNIEXPORT jint JNICALL
        Java_com_amazon_apl_android_Component_nGetParentType(JNIEnv *env, jclass clazz,
                                                           jlong handle) {

            auto c = get<Component>(handle);
            auto p = c->getParent();
            if (p == nullptr) {
                return -1;
            }
            return static_cast<jint>(p->getType());
        }

        JNIEXPORT void JNICALL
        Java_com_amazon_apl_android_Component_nUpdate__JII(JNIEnv *env, jclass clazz, jlong handle,
                                                      jint updateType, jint value) {
            auto c = get<Component>(handle);
            auto ut = static_cast<UpdateType >(static_cast<int>(updateType));
            auto b = static_cast<int>(value);
            c->update(ut, b);
        }

        JNIEXPORT void JNICALL
        Java_com_amazon_apl_android_Component_nUpdate__JI_3B(JNIEnv *env, jclass clazz, jlong handle,
        jint updateType, jbyteArray value) {
            auto c = get<Component>(handle);
            auto ut = static_cast<UpdateType >(static_cast<int>(updateType));
            jbyte * expression = env->GetByteArrayElements(value, nullptr);
            c->update(ut, reinterpret_cast<char *>(expression));
            env->ReleaseByteArrayElements(value, expression, 0);
        }

        JNIEXPORT jint JNICALL
        Java_com_amazon_apl_android_Component_nGetChildCount(JNIEnv *env, jclass clazz,
                                                             jlong handle) {

            auto c = get<Component>(handle);
            return static_cast<jint>(c->getChildCount());
        }

        JNIEXPORT jint JNICALL
        Java_com_amazon_apl_android_Component_nGetDisplayedChildCount(JNIEnv *env, jclass clazz,
                                                             jlong handle) {

            auto c = get<Component>(handle);
            return static_cast<jint>(c->getDisplayedChildCount());
        }

        JNIEXPORT jstring JNICALL
        Java_com_amazon_apl_android_Component_nGetDisplayedChildId(JNIEnv *env, jclass clazz,
                                                             jlong handle, jint displayedChildIndex) {

            auto c = get<Component>(handle);
            auto cid = static_cast<size_t>(displayedChildIndex);
            return env->NewStringUTF(c->getDisplayedChildAt(cid)->getUniqueId().c_str());
        }

        JNIEXPORT jstring JNICALL
        Java_com_amazon_apl_android_Component_nGetChildId(JNIEnv *env, jclass clazz, jlong handle,
                                                          jint childId) {

            auto c = get<Component>(handle);
            auto cid = static_cast<size_t>(childId);
            return env->NewStringUTF(c->getChildAt(cid)->getUniqueId().c_str());
        }

        JNIEXPORT void JNICALL
        Java_com_amazon_apl_android_Component_nEnsureLayout(JNIEnv *env, jclass clazz,
                                                            jlong handle) {

            auto c = get<Component>(handle);
            c->ensureLayout();
        }

        JNIEXPORT jstring JNICALL
        Java_com_amazon_apl_android_Component_nGetHierarchySignature(JNIEnv *env, jclass clazz,
                                                                     jlong handle) {

            auto c = get<Component>(handle);
            auto hs = c->getHierarchySignature();
            return env->NewStringUTF(hs.c_str());
        }

        JNIEXPORT jobject JNICALL Java_com_amazon_apl_android_Video_nGetMediaPlayer(JNIEnv *env, jclass clazz,
                                                                                    jlong handle) {
            auto c = get<Component>(handle);

            auto player = c->getMediaPlayer();
            if (player) {
                auto androidMediaPlayer = std::static_pointer_cast<AndroidMediaPlayer>(player);
                return androidMediaPlayer->getInstance();
            }
            return nullptr;
        }

        JNIEXPORT jboolean JNICALL
        Java_com_amazon_apl_android_Component_nCheckDirtyProperty(JNIEnv *env, jclass clazz,
                                                                  jlong handle,
                                                                  jint propertyId) {

            auto c = get<Component>(handle);
            bool dirty = c->getDirty().count(static_cast<apl::PropertyKey>(propertyId)) != 0;
            return static_cast<jboolean>(dirty);
        }

        JNIEXPORT jboolean JNICALL
        Java_com_amazon_apl_android_Component_nCheckDirty(JNIEnv *env, jclass clazz,
                                                          jlong handle) {

            auto c = get<Component>(handle);
            bool dirty = c->getDirty().size() > 0;
            return static_cast<jboolean>(dirty);
        }

        JNIEXPORT jboolean JNICALL
        Java_com_amazon_apl_android_EditText_nIsValidCharacter(JNIEnv *env, jclass clazz,
                                                               jlong componentHandle,
                                                               jchar character) {
            auto c = get<Component>(componentHandle);
            return static_cast<jboolean>(c->isCharacterValid(character));
        }

        JNIEXPORT jfloat JNICALL
        Java_com_amazon_apl_android_Component_nGetCalculatedWidth(JNIEnv *env, jclass clazz,
                                                                        jlong componentHandle) {
            auto component = get<Component>(componentHandle);
            auto calculatedWidth = component->getCalculated(apl::kPropertyBounds).get<apl::Rect>().getWidth();

            return calculatedWidth;
        }

        JNIEXPORT jfloat JNICALL
        Java_com_amazon_apl_android_Component_nGetCalculatedHeight(JNIEnv *env, jclass clazz,
                                                                  jlong componentHandle) {
            auto component = get<Component>(componentHandle);
            auto calculatedHeight = component->getCalculated(apl::kPropertyBounds).get<apl::Rect>().getHeight();

            return calculatedHeight;
        }

        JNIEXPORT jfloatArray JNICALL
        Java_com_amazon_apl_android_Component_nGetGlobalPointCoordinates(JNIEnv *env, jclass clazz,
                                                               jlong componentHandle,
                                                               jfloat pointA,
                                                               jfloat pointB) {
            auto component = get<Component>(componentHandle);
            auto point = component->localToGlobal({pointA, pointB});

            float buffer[2] = { float (point.getX()),
                               float (point.getY())};

            jfloatArray pointArray = env->NewFloatArray(2);
            env->SetFloatArrayRegion(pointArray, 0, 2, buffer);

            return pointArray;
        }

#ifdef __cplusplus
        }
#endif

    } //namespace jni
} //namespace apl