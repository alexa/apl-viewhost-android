
/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

#include <jni.h>

#include "jniaudioplayer.h"
#include "jniaudioplayerfactory.h"
#include "jnimediaplayer.h"
#include "jnimediaplayerfactory.h"
#include "jnirootcontext.h"
#include "jnirootconfig.h"
#include "jnicontent.h"
#include "jnicomplexproperty.h"
#include "jnievent.h"
#include "jniaction.h"
#include "jniutil.h"
#include "jnigraphic.h"
#include "jniscaling.h"
#include "jnitextmeasurecallback.h"
#include "jniextensionmediator.h"

#ifdef __cplusplus
extern "C" {
#endif

namespace apl {
    namespace jni {

        /**
         * Initialize and cache java class and method handles for callback to the rendering layer.
         * The VM calls JNI_OnLoad when the native library is loaded (for example, through System.loadLibrary).
         */
        JNIEXPORT jint
        JNI_OnLoad(JavaVM *vm, void *reserved) {
            JNIEnv *env;
            if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
                return -1;  //JNI_ERR
            }
            jboolean jniutilLoaded = jniutil_OnLoad(vm, reserved);
            jboolean driverLoaded = rootcontext_OnLoad(vm, reserved);
            jboolean contentLoaded = content_OnLoad(vm, reserved);
            jboolean rootconfigLoaded = rootconfig_OnLoad(vm, reserved);
            jboolean complexpropertyLoaded = complexproperty_OnLoad(vm, reserved);
            jboolean eventLoaded = event_OnLoad(vm, reserved);
            jboolean actionLoaded = action_OnLoad(vm, reserved);
            jboolean graphicLoaded = graphic_OnLoad(vm, reserved);
            jboolean jniscalingLoaded = jniscaling_OnLoad(vm, reserved);
            jboolean textmeasureLoaded = textmeasurecallback_OnLoad(vm, reserved);
            jboolean localExtensionMediatorLoaded = extensionmediator_OnLoad(vm, reserved);
            jboolean audioFactoryLoaded = audioplayerfactory_OnLoad(vm, reserved);
            jboolean audioPlayerLoaded = audioplayer_OnLoad(vm, reserved);
            jboolean mediaplayerLoaded = mediaplayer_OnLoad(vm, reserved);
            jboolean mediaplayerFactoryLoaded = mediaplayerfactory_OnLoad(vm, reserved);

            if (!driverLoaded || !contentLoaded || !rootconfigLoaded
                || !complexpropertyLoaded || !eventLoaded || !actionLoaded || !graphicLoaded
                || !jniutilLoaded || !jniscalingLoaded || !textmeasureLoaded
                || !localExtensionMediatorLoaded || !audioFactoryLoaded || !audioPlayerLoaded
                || !localExtensionMediatorLoaded || !mediaplayerLoaded || !mediaplayerFactoryLoaded)  {
                return JNI_ERR;
            }

            LOG(apl::LogLevel::DEBUG) << "Complete View Host JNI environment.";

            return JNI_VERSION_1_6;
        }

        /**
         * Release the class and method cache.
         * The VM calls JNI_OnUnload when the class loader containing the native library is garbage collected.
         */
        JNIEXPORT void
        JNI_OnUnload(JavaVM *vm, void *reserved) {
            rootcontext_OnUnload(vm, reserved);
            content_OnUnload(vm, reserved);
            rootconfig_OnUnload(vm, reserved);
            complexproperty_OnUnload(vm, reserved);
            event_OnUnload(vm, reserved);
            action_OnUnload(vm, reserved);
            graphic_OnUnload(vm, reserved);
            jniutil_OnUnload(vm, reserved);
            textmeasurecallback_OnUnload(vm, reserved);
            extensionmediator_OnUnload(vm, reserved);
            audioplayerfactory_OnUnload(vm, reserved);
            audioplayer_OnUnload(vm, reserved);
            mediaplayer_OnUnload(vm, reserved);
            mediaplayerfactory_OnUnload(vm, reserved);
        }

    } //namespace jni
} //namespace apl

#ifdef __cplusplus
}
#endif