/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

#include <jni.h>
#include "apl/apl.h"
#include "jniutil.h"

namespace apl {
    namespace jni {

#ifdef __cplusplus
        extern "C" {
#endif

#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wunused-parameter"

        /**
         * Create a Content instance and attach it to the view host peer.
         */
        JNIEXPORT jlong JNICALL
        Java_com_amazon_apl_android_LiveArray_nCreate(JNIEnv *env, jclass clazz) {
            auto liveArray = LiveArray::create();
            return createHandle<LiveArray>(liveArray);
        }

        JNIEXPORT void JNICALL
        Java_com_amazon_apl_android_LiveArray_nClear(JNIEnv *env, jclass clazz, jlong contentHandle) {
            auto liveArray = get<LiveArray>(contentHandle);
            liveArray->clear();
        }

        JNIEXPORT jint JNICALL
        Java_com_amazon_apl_android_LiveArray_nSize(JNIEnv *env, jclass clazz, jlong contentHandle) {
            auto liveArray = get<LiveArray>(contentHandle);
            return static_cast<jint>(liveArray->size());
        }

        JNIEXPORT jobject JNICALL
        Java_com_amazon_apl_android_LiveArray_nAt(JNIEnv *env, jclass clazz,
                                                  jlong contentHandle, jint position) {
            auto liveArray = get<LiveArray>(contentHandle);
            return getJObject(env, liveArray->at(static_cast<LiveArray::size_type>(position)));
        }

        JNIEXPORT jboolean JNICALL
        Java_com_amazon_apl_android_LiveArray_nInsert(JNIEnv *env, jclass clazz,
                                                      jlong contentHandle, jint position, jobject value) {
            auto liveArray = get<LiveArray>(contentHandle);
            return static_cast<jboolean>(liveArray->insert(
                    static_cast<LiveArray::size_type>(position),
                    getAPLObject(env, value)));
        }

        JNIEXPORT jboolean JNICALL
        Java_com_amazon_apl_android_LiveArray_nInsertRange(JNIEnv *env, jclass clazz,
                                                           jlong contentHandle, jint position,
                                                           jobjectArray objectArray) {
            auto liveArray = get<LiveArray>(contentHandle);
            auto length = env->GetArrayLength(objectArray);
            auto array = std::vector<apl::Object>();

            for (int i = 0; i < length; i++)
                array.emplace_back(getAPLObject(env, env->GetObjectArrayElement(objectArray, i)));

            return static_cast<jboolean>(liveArray->insert(
                    static_cast<LiveArray::size_type>(position), array.begin(), array.end()));
        }

        JNIEXPORT jboolean JNICALL
        Java_com_amazon_apl_android_LiveArray_nRemove(JNIEnv *env, jclass clazz,
                                                      jlong contentHandle, jint position, jint count) {
            auto liveArray = get<LiveArray>(contentHandle);
            return static_cast<jboolean>(liveArray->remove(
                    static_cast<LiveArray::size_type>(position),
                    static_cast<LiveArray::size_type>(count)));
        }

        JNIEXPORT jboolean JNICALL
        Java_com_amazon_apl_android_LiveArray_nUpdate(JNIEnv *env, jclass clazz,
                                                      jlong contentHandle, jint position, jobject value) {
            auto liveArray = get<LiveArray>(contentHandle);
            return static_cast<jboolean>(liveArray->update(
                    static_cast<LiveArray::size_type>(position),
                    getAPLObject(env, value)));
        }

        JNIEXPORT jboolean JNICALL
        Java_com_amazon_apl_android_LiveArray_nUpdateRange(JNIEnv *env, jclass clazz,
                                                           jlong contentHandle, jint position,
                                                           jobjectArray objectArray) {
            auto liveArray = get<LiveArray>(contentHandle);
            auto length = env->GetArrayLength(objectArray);
            auto array = std::vector<apl::Object>();

            for (int i = 0; i < length; i++)
                array.emplace_back(getAPLObject(env, env->GetObjectArrayElement(objectArray, i)));

            return static_cast<jboolean>(liveArray->update(
                    static_cast<LiveArray::size_type>(position), array.begin(), array.end()));
        }

        JNIEXPORT void JNICALL
        Java_com_amazon_apl_android_LiveArray_nPushBack(JNIEnv *env, jclass clazz,
                                                        jlong contentHandle, jobject value) {
            auto liveArray = get<LiveArray>(contentHandle);
            liveArray->push_back(getAPLObject(env, value));
        }

        JNIEXPORT void JNICALL
        Java_com_amazon_apl_android_LiveArray_nPushBackRange(JNIEnv *env, jclass clazz,
                                                             jlong contentHandle, jobjectArray objectArray) {
            auto liveArray = get<LiveArray>(contentHandle);
            auto length = env->GetArrayLength(objectArray);
            auto array = std::vector<apl::Object>();

            for (int i = 0 ; i < length ; i++)
                array.emplace_back(getAPLObject(env, env->GetObjectArrayElement(objectArray, i)));

            liveArray->push_back(array.begin(), array.end());
        }

        JNIEXPORT jlong JNICALL
        Java_com_amazon_apl_android_LiveMap_nCreate(JNIEnv *env, jclass clazz) {
            auto liveMap = LiveMap::create();
            return createHandle<LiveMap>(liveMap);
        }

        JNIEXPORT jint JNICALL
        Java_com_amazon_apl_android_LiveMap_nSize(JNIEnv *env, jclass clazz, jlong handle) {
            auto liveMap = get<LiveMap>(handle);
            return static_cast<jint>(liveMap->getMap().size());
        }

        JNIEXPORT jboolean JNICALL
        Java_com_amazon_apl_android_LiveMap_nEmpty(JNIEnv *env, jclass clazz, jlong handle) {
            auto liveMap = get<LiveMap>(handle);
            return static_cast<jboolean>(liveMap->empty());
        }

        JNIEXPORT void JNICALL
        Java_com_amazon_apl_android_LiveMap_nClear(JNIEnv *env, jclass clazz, jlong handle) {
            auto liveMap = get<LiveMap>(handle);
            liveMap->clear();
        }

        JNIEXPORT jboolean JNICALL
        Java_com_amazon_apl_android_LiveMap_nHas(JNIEnv *env, jclass clazz, jlong handle, jstring key) {
            auto liveMap = get<LiveMap>(handle);
            auto key_ = env->GetStringUTFChars(key, nullptr);
            auto has = static_cast<jboolean>(liveMap->has(key_));
            env->ReleaseStringUTFChars(key, key_);
            return has;
        }

        JNIEXPORT jobject JNICALL
        Java_com_amazon_apl_android_LiveMap_nGet(JNIEnv *env, jclass clazz, jlong handle, jstring key) {
            auto liveMap = get<LiveMap>(handle);
            auto key_ = env->GetStringUTFChars(key, nullptr);
            auto object = getJObject(env, liveMap->get(key_));
            env->ReleaseStringUTFChars(key, key_);
            return object;
        }

        JNIEXPORT void JNICALL
        Java_com_amazon_apl_android_LiveMap_nSet(JNIEnv *env, jclass clazz, jlong handle, jstring key, jobject value) {
            auto liveMap = get<LiveMap>(handle);
            auto key_ = env->GetStringUTFChars(key, nullptr);
            liveMap->set(key_, getAPLObject(env, value));
            env->ReleaseStringUTFChars(key, key_);
        }

        JNIEXPORT jboolean JNICALL
        Java_com_amazon_apl_android_LiveMap_nRemove(JNIEnv *env, jclass clazz, jlong handle, jstring key) {
            auto liveMap = get<LiveMap>(handle);
            auto key_ = env->GetStringUTFChars(key, nullptr);
            jboolean removed = static_cast<jboolean>(liveMap->remove(key_));
            env->ReleaseStringUTFChars(key, key_);
            return removed;
        }

#pragma clang diagnostic pop

#ifdef __cplusplus
        }
#endif

    } //namespace jni
} //namespace apl
