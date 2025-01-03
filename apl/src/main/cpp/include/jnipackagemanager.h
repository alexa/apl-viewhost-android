/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

#ifndef APLVIEWHOSTANDROID_JNIPACKAGEMANAGER_H
#define APLVIEWHOSTANDROID_JNIPACKAGEMANAGER_H

#include <jni.h>
#include "apl/apl.h"
#include "apl/content/packagemanager.h"

#ifdef __cplusplus
extern "C" {
#endif

namespace apl {
    namespace jni {

        class jniPackageManager
                : public apl::PackageManager, public std::enable_shared_from_this<jniPackageManager> {
        public:
            static apl::PackageManagerPtr create(jweak javapackageManagerInstance);

            jniPackageManager() = default;

            virtual ~jniPackageManager();

            using PackageRequestPtr = std::shared_ptr<PackageRequest>;

            void loadPackage(const PackageRequestPtr& packageRequest) override;

        private:

            jweak mJavaPackageManagerInstance;
        };

        /**
        *  Initialize and cache java class and method handles for callback to the rendering layer.
        */
        jboolean packagemanager_OnLoad(JavaVM *vm, void *reserved __attribute__((__unused__)));

        /**
         * Release the class and methpackageLoadCompleteod cache.
         */
        void packagemanager_OnUnload(JavaVM *vm, void *reserved __attribute__((__unused__)));

    } // namespace jni
} // namespace apl

#ifdef __cplusplus
}
#endif

#endif //APLVIEWHOSTANDROID_JNIPACKAGEMANAGER_H
