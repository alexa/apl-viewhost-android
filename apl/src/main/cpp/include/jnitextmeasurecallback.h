/**
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

#ifndef ANDROID_JNITEXTMEASURECALLBACK_H
#define ANDROID_JNITEXTMEASURECALLBACK_H

#include <utility>
#include <jni.h>

#include "apl/apl.h"

#include "jnimetricstransform.h"

#ifdef __cplusplus
extern "C" {
#endif

/**
*  Initialize and cache java class and method handles for callback to the rendering layer.
*/
jboolean
textmeasurecallback_OnLoad(JavaVM *vm, void * reserved ) ;

/**
* Release the class and method cache.
*/
void
textmeasurecallback_OnUnload(JavaVM *vm, void * reserved ) ;

namespace apl {
namespace jni {

    /**
     * Text Measurement Callback.  This object calls back to the view host via JNI.
     *
     * The sizing of text based Components is dependent on view host resources, such as font.  For
     * this reason APL Core defers size calculation of text based components to the view host.  This callback
     * is configured by {@link RootContext} and used any time core needs to (re)calculate a text component
     * size.
     *
     * The callback maintains a reference to the target component under measure through the measurement
     * process and releases the reference upon completion.  As core iterates over the required
     * measurements, the target component is updated.  The view host will access this component
     * during the measurement effort. To maintain proper binding with the native peer, this callback
     * should not be used across document.
     */
    class APLTextMeasurement : public apl::sg::TextMeasurement {
    public:
        APLTextMeasurement() {}

        virtual ~APLTextMeasurement();

        sg::TextLayoutPtr layout( const apl::sg::TextChunkPtr& chunk,
                                  const apl::sg::TextPropertiesPtr& textProperties,
                                  float width,
                                  MeasureMode widthMode,
                                  float height,
                                  MeasureMode heightMode ) override;

        sg::EditTextBoxPtr box( int size,
                                const apl::sg::TextPropertiesPtr& textProperties,
                                float width,
                                MeasureMode widthMode,
                                float height,
                                MeasureMode heightMode ) override;

        virtual void setInstance(jobject instance);

    private:
        jweak mInstance = nullptr;
    };

} // namespace jni
} // namespace apl

#ifdef __cplusplus
}
#endif
#endif //ANDROID_JNITEXTMEASURECALLBACK_H