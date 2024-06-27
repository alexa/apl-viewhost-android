/**
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

#ifndef APLVIEWHOSTANDROID_JNIEDITTEXTBOX_H
#define APLVIEWHOSTANDROID_JNIEDITTEXTBOX_H
#ifdef __cplusplus
extern "C" {
#endif

namespace apl {
    namespace jni {
        class APLEditTextBox : public sg::EditTextBox {
        public:
            APLEditTextBox(apl::Size size, float baseline) :
                          mBaseline(baseline),
                          mSize(size) {}

            ~APLEditTextBox() {}

            Size getSize() const override {
                return mSize;
            }

            float getBaseline() const override {
                return mBaseline;
            }

        private:
            apl::Size mSize;
            float mBaseline;
        };
    } // namespace jni
} // namespace apl

#ifdef __cplusplus
}
#endif
#endif //APLVIEWHOSTANDROID_JNIEDITTEXTBOX_H