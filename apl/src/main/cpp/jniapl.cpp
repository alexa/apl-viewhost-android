/**
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

#include <jni.h>

#include "jniaction.h"
#include "jniapltextproperties.h"
#include "jniaudioplayer.h"
#include "jniaudioplayerfactory.h"
#include "jnicomplexproperty.h"
#include "jnicontent.h"
#include "jnidocumentmanager.h"
#include "jniembeddeddocumentrequest.h"
#include "jnievent.h"
#include "jniextensionmediator.h"
#include "jnigraphic.h"
#include "jnimediaplayer.h"
#include "jnimediaplayerfactory.h"
#include "jnirootconfig.h"
#include "jnirootcontext.h"
#include "jniscaling.h"
#include "jnisession.h"
#include "jnitextlayout.h"
#include "jnitextmeasurecallback.h"
#include "jniutil.h"
#ifdef SCENEGRAPH
#include "jniaplview.h"
#include "jniapllayer.h"
#include "jniaplscenegraph.h"
#include "jnisgcontent.h"
#include "jnisglayer.h"
#include "jniedittext.h"
#include "jniedittextfactory.h"
#include "scenegraph/jnimediamanager.h"
#endif

#ifdef INCLUDE_ALEXAEXT
#include "jniextensionexecutor.h"
#include "jniextensionproxy.h"
#include "jniextensionregistrar.h"
#include "jniextensionresource.h"
#endif

#ifdef __ANDROID__
#include "loggingbridge.h"
#endif

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
#ifdef __ANDROID__
            apl::LoggerFactory::instance().initialize(std::make_shared<AndroidJniLogBridge>());
#endif
            JNIEnv *env;
            if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
                return -1;  //JNI_ERR
            }
            jboolean actionLoaded = action_OnLoad(vm, reserved);
            jboolean audioFactoryLoaded = audioplayerfactory_OnLoad(vm, reserved);
            jboolean audioPlayerLoaded = audioplayer_OnLoad(vm, reserved);
            jboolean complexpropertyLoaded = complexproperty_OnLoad(vm, reserved);
            jboolean contentLoaded = content_OnLoad(vm, reserved);
            jboolean documentmanagerLoaded = documentmanager_OnLoad(vm, reserved);
            jboolean driverLoaded = rootcontext_OnLoad(vm, reserved);
            jboolean eventLoaded = event_OnLoad(vm, reserved);
            jboolean graphicLoaded = graphic_OnLoad(vm, reserved);
            jboolean jniscalingLoaded = jniscaling_OnLoad(vm, reserved);
            jboolean jnisessionLoaded = jnisession_OnLoad(vm, reserved);
            jboolean jniutilLoaded = jniutil_OnLoad(vm, reserved);
            jboolean localExtensionMediatorLoaded = extensionmediator_OnLoad(vm, reserved);
            jboolean mediaplayerFactoryLoaded = mediaplayerfactory_OnLoad(vm, reserved);
            jboolean mediaplayerLoaded = mediaplayer_OnLoad(vm, reserved);
            jboolean rootconfigLoaded = rootconfig_OnLoad(vm, reserved);
            jboolean textlayoutLoaded = textlayout_OnLoad(vm, reserved);
            jboolean textmeasureLoaded = textmeasurecallback_OnLoad(vm, reserved);
            jboolean textpropertiesLoaded = apltextproperties_OnLoad(vm, reserved);

#ifdef SCENEGRAPH
            jboolean jniaplviewLoaded = aplview_OnLoad(vm, reserved);
            jboolean apllayerLoaded = apllayer_OnLoad(vm, reserved);
            jboolean aplscenegraphLoaded = aplscenegraph_OnLoad(vm, reserved);
            jboolean sgcontentLoaded = sgcontent_OnLoad(vm, reserved);
            jboolean sglayerLoaded = sglayer_OnLoad(vm, reserved);
            jboolean edittextLoaded = edittext_OnLoad(vm, reserved);
            jboolean edittextfactoryLoaded = edittextfactory_OnLoad(vm, reserved);
            jboolean mediaManagerLoaded = mediamanager_OnLoad(vm, reserved);
#endif

#ifdef INCLUDE_ALEXAEXT
            jboolean extensionExecutorLoaded = extensionexecutor_OnLoad(vm, reserved);
            jboolean extensionProxyLoaded = extensionproxy_OnLoad(vm, reserved);
            jboolean extensionProviderLoaded = extensionprovider_OnLoad(vm, reserved);
            jboolean extensionResourceProviderLoaded = extensionresource_OnLoad(vm, reserved);

            if (!extensionProxyLoaded || !extensionProviderLoaded || !extensionResourceProviderLoaded
                || !extensionExecutorLoaded) {
                    return JNI_ERR;
            }
#endif

            if (!driverLoaded || !contentLoaded || !rootconfigLoaded
                || !complexpropertyLoaded || !eventLoaded || !actionLoaded || !graphicLoaded
                || !textlayoutLoaded || !textpropertiesLoaded
                || !jniutilLoaded || !jniscalingLoaded || !textmeasureLoaded
                || !localExtensionMediatorLoaded || !audioFactoryLoaded || !audioPlayerLoaded
                || !localExtensionMediatorLoaded || !mediaplayerLoaded || !mediaplayerFactoryLoaded
                || !documentmanagerLoaded || !jnisessionLoaded
#ifdef SCENEGRAPH
                || !sgcontentLoaded || !sglayerLoaded || !apllayerLoaded || !aplscenegraphLoaded
                || !jniaplviewLoaded || !edittextLoaded || !edittextfactoryLoaded || !mediaManagerLoaded
#endif
                )  {
                return JNI_ERR;
            }

            LOG(apl::LogLevel::kDebug) << "Complete View Host JNI environment.";

            return JNI_VERSION_1_6;
        }

        /**
         * Release the class and method cache.
         * The VM calls JNI_OnUnload when the class loader containing the native library is garbage collected.
         */
        JNIEXPORT void
        JNI_OnUnload(JavaVM *vm, void *reserved) {
            action_OnUnload(vm, reserved);
            apltextproperties_OnUnload(vm, reserved);
            audioplayer_OnUnload(vm, reserved);
            audioplayerfactory_OnUnload(vm, reserved);
            complexproperty_OnUnload(vm, reserved);
            content_OnUnload(vm, reserved);
            documentmanager_OnUnload(vm, reserved);
            event_OnUnload(vm, reserved);
            extensionmediator_OnUnload(vm, reserved);
            graphic_OnUnload(vm, reserved);
            jnisession_OnUnload(vm, reserved);
            jniutil_OnUnload(vm, reserved);
            mediaplayer_OnUnload(vm, reserved);
            mediaplayerfactory_OnUnload(vm, reserved);
            rootconfig_OnUnload(vm, reserved);
            rootcontext_OnUnload(vm, reserved);
            textlayout_OnUnload(vm, reserved);
            textmeasurecallback_OnUnload(vm, reserved);

#ifdef SCENEGRAPH
            aplview_OnUnload(vm, reserved);
            apllayer_OnUnload(vm, reserved);
            aplscenegraph_OnUnload(vm, reserved);
            sglayer_OnUnload(vm, reserved);
            sgcontent_OnUnload(vm, reserved);
            edittext_OnUnload(vm, reserved);
            edittextfactory_OnUnload(vm, reserved);
            mediamanager_OnUnload(vm, reserved);
#endif

#ifdef INCLUDE_ALEXAEXT
            extensionexecutor_OnUnload(vm, reserved);
            extensionproxy_OnUnload(vm, reserved);
            extensionprovider_OnUnload(vm, reserved);
            extensionresource_OnUnload(vm, reserved);
#endif

        }

    } //namespace jni
} //namespace apl

#ifdef __cplusplus
}
#endif