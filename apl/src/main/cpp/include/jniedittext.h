/**
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

#ifndef APLVIEWHOSTANDROID_JNIEDITTEXT_H
#define APLVIEWHOSTANDROID_JNIEDITTEXT_H
#ifdef __cplusplus
extern "C" {
#endif


/**
  *  Initialize and cache java class and method handles for callback to the rendering layer.
  */
jboolean edittext_OnLoad(JavaVM *vm, void *reserved __attribute__((__unused__)));

/**
 * Release the class and method cache.
 */
void edittext_OnUnload(JavaVM *vm, void *reserved __attribute__((__unused__)));

#ifdef SCENEGRAPH
namespace apl {
    namespace jni {

        class APLEditText : public sg::EditText {
        public:
            APLEditText(sg::EditTextSubmitCallback submitCallback,
                        sg::EditTextChangedCallback changedCallback,
                        sg::EditTextFocusCallback focusCallback);

            void release() override;
            void setFocus(bool hasFocus) override;

            // Call these on the core dispatch queue
            void doSubmit();
            void doChanged(const std::string& text);
            void doFocused(bool focused);
            void setInstance(jobject instance);
            jobject getInstance() const {
                return mInstance;
            }

        private:
            jobject mInstance;
        };

    } // namespace jni
} // namespace apl
#endif
#ifdef __cplusplus
}
#endif
#endif //APLVIEWHOSTANDROID_JNIEDITTEXT_H
