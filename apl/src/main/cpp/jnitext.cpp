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
        Java_com_amazon_apl_android_Text_nCountCharactersInRange(JNIEnv *env, jclass clazz, jlong componentHandle, jint index, jint count) {
            auto c = get<Component>(componentHandle);
            std::string text = c->getCalculated(kPropertyText).get<StyledText>().getText();

            int utf8Characters = utf8StringLength((uint8_t *)text.data() + index, count);

            return static_cast<jint>(utf8Characters);
        }

#ifdef __cplusplus
}
#endif

} //namespace jni
} //namespace apl