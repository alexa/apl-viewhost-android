/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

#include <jni.h>
#include <string>
#include "apl/apl.h"
#include "apl/primitives/unicode.h"

#include "rapidjson/document.h"
#include "rapidjson/writer.h"
#include "rapidjson/stringbuffer.h"
#include "jninativeowner.h"

namespace apl {
    namespace jni {

#ifdef __cplusplus
        extern "C" {
#endif

JNIEXPORT jint JNICALL
        Java_com_amazon_apl_android_utils_APLTextUtil_nCountCharactersInRange(JNIEnv *env,
                                                                              jclass clazz,
                                                                              jstring text,
                                                                              jint index,
                                                                              jint count) {
            const char* chars = env->GetStringUTFChars(text, nullptr);
            std::string result(chars);
            int utf8Characters = utf8StringLength((uint8_t *)result.data() + index, count);
            env->ReleaseStringUTFChars(text, chars);
            return static_cast<jint>(utf8Characters);
        }

#ifdef __cplusplus
}
#endif

} //namespace jni
} //namespace apl