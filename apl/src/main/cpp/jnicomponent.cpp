/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

#include <jni.h>
#include <string>

#include "rapidjson/document.h"
#include "rapidjson/writer.h"
#include "rapidjson/stringbuffer.h"
#include "apl/apl.h"
#include "jniutil.h"
#include "jnimetricstransform.h"
#include "loggingbridge.h"

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
        Java_com_amazon_apl_android_Component_nUpdate__JILjava_lang_String_2(JNIEnv *env, jclass clazz, jlong handle,
                                                                              jint updateType, jstring value) {
            auto c = get<Component>(handle);
            auto ut = static_cast<UpdateType >(static_cast<int>(updateType));
            const char *expression = env->GetStringUTFChars(value, nullptr);
            c->update(ut, expression);
            env->ReleaseStringUTFChars(value, expression);
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

        JNIEXPORT void JNICALL
        Java_com_amazon_apl_android_Video_nUpdateMediaState(JNIEnv *env, jclass clazz, jlong handle,
                jint jTrackIndex, jint jTrackCount, jint jCurrentTime, jint jDuration,
                jboolean jPaused, jboolean jEnded, jboolean fromEvent) {
            auto c = get<Component>(handle);
            int trackIndex = static_cast<int>(jTrackIndex);
            int trackCount = static_cast<int>(jTrackCount);
            int currentTime = static_cast<int>(jCurrentTime);
            int duration = static_cast<int>(jDuration);
            bool paused = static_cast<bool>(jPaused);
            bool ended = static_cast<bool>(jEnded);
            c->updateMediaState(
                    MediaState(trackIndex, trackCount, currentTime, duration, paused, ended),
                    fromEvent);
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

#ifdef __cplusplus
        }
#endif

    } //namespace jni
} //namespace apl