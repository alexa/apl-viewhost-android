/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

#include <jni.h>
#include <include/apl/component/edittextcomponent.h>
#include "apl/apl.h"
#include "jniutil.h"

namespace apl {
    namespace jni {

#ifdef __cplusplus
        extern "C" {
#endif

        JNIEXPORT jboolean JNICALL
        Java_com_amazon_apl_android_EditText_nIsValidCharacter(JNIEnv *env,
                                                               jclass clazz,
                                                              jlong componentHandle,
                                                              jchar character) {
            auto c = get<EditTextComponent>(componentHandle);
            return static_cast<jboolean>(c->isCharacterValid(character));
        }

#ifdef __cplusplus
}



#endif

} //namespace jni
} //namespace apl
