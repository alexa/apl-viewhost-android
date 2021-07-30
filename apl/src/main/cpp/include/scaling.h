/*
 *  Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

#ifndef ANDROID_SCALING_H
#define ANDROID_SCALING_H

#include "apl/apl.h"

namespace apl {
    namespace jni {

        class Scaling {
        public:
            Scaling(double biasConstant) : biasConstant(biasConstant) {}

            void addViewportSpecification(const ViewportSpecification &spec) {
                specifications.emplace_back(spec);
            }

            void addAllowMode(const ViewportMode mode) {
                allowModes.emplace(mode);
            }

            double biasConstant;
            std::vector <ViewportSpecification> specifications;
            std::set <ViewportMode> allowModes;
        };
    }
}

#endif //APLVIEWHOSTANDROID_SCALING_H
