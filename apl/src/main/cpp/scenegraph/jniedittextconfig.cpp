/**
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */


#include <jni.h>
#include "jniutil.h"

#ifdef SCENEGRAPH
namespace apl {
    namespace jni {
#ifdef __cplusplus
        extern "C" {
#endif
        JNIEXPORT jint JNICALL
        Java_com_amazon_apl_android_scenegraph_edittext_EditTextConfig_nGetTextColor(JNIEnv *env, jclass clazz,
                                                                                     jlong handle) {
            auto coreEditTextConfig = reinterpret_cast<apl::sg::EditTextConfig *>(handle);
            auto color = coreEditTextConfig->textColor();
            return color.get();
        }

        JNIEXPORT jint JNICALL
        Java_com_amazon_apl_android_scenegraph_edittext_EditTextConfig_nGetHighlightColor(JNIEnv *env, jclass clazz,
                                                                                          jlong handle) {
            auto coreEditTextConfig = reinterpret_cast<apl::sg::EditTextConfig *>(handle);
            auto color = coreEditTextConfig->highlightColor();
            return color.get();
        }

        JNIEXPORT jint JNICALL
        Java_com_amazon_apl_android_scenegraph_edittext_EditTextConfig_nGetKeyboardType(JNIEnv *env, jclass clazz,
                                                                                        jlong handle) {
            auto coreEditTextConfig = reinterpret_cast<apl::sg::EditTextConfig *>(handle);
            auto keyboardType = coreEditTextConfig->keyboardType();
            return static_cast<jint>(keyboardType);
        }

        JNIEXPORT jint JNICALL
        Java_com_amazon_apl_android_scenegraph_edittext_EditTextConfig_nGetSubmitKeyType(JNIEnv *env, jclass clazz,
                                                                                         jlong handle) {
            auto coreEditTextConfig = reinterpret_cast<apl::sg::EditTextConfig *>(handle);
            auto submitKeyType = coreEditTextConfig->submitKeyType();
            return static_cast<jint>(submitKeyType);
        }

        JNIEXPORT jstring JNICALL
        Java_com_amazon_apl_android_scenegraph_edittext_EditTextConfig_nStrip(JNIEnv *env, jclass clazz,
                                                                              jbyteArray text,
                                                                              jlong handle) {
            auto coreEditTextConfig = reinterpret_cast<apl::sg::EditTextConfig *>(handle);
            jbyte * expression = env->GetByteArrayElements(text, nullptr);
            auto stripped = coreEditTextConfig->strip(reinterpret_cast<char *>(expression));
            std::u16string u16 = converter.from_bytes(stripped.c_str());
            env->ReleaseByteArrayElements(text, expression, 0);
            return env->NewString(reinterpret_cast<const jchar *>(u16.c_str()), u16.length());
        }

        JNIEXPORT jboolean JNICALL
        Java_com_amazon_apl_android_scenegraph_edittext_EditTextConfig_nIsSecureInput(JNIEnv *env, jclass clazz,
                                                                                      jlong handle) {
            auto coreEditTextConfig = reinterpret_cast<apl::sg::EditTextConfig *>(handle);
            return coreEditTextConfig->secureInput();
        }

        JNIEXPORT jboolean JNICALL
        Java_com_amazon_apl_android_scenegraph_edittext_EditTextConfig_nIsSelectOnFocus(JNIEnv *env, jclass clazz,
                                                                                        jlong handle) {
            auto coreEditTextConfig = reinterpret_cast<apl::sg::EditTextConfig *>(handle);
            return coreEditTextConfig->selectOnFocus();
        }

        JNIEXPORT jlong JNICALL
        Java_com_amazon_apl_android_scenegraph_edittext_EditTextConfig_nGetTextProperties(JNIEnv *env, jclass clazz,
                                                                                          jlong handle) {
            auto coreEditTextConfig = reinterpret_cast<apl::sg::EditTextConfig *>(handle);
            auto textProperties = coreEditTextConfig->textProperties();
            return reinterpret_cast<jlong>(textProperties.get());
        }
#ifdef __cplusplus
        }
#endif
    } //namespace jni
} //namespace apl
#endif