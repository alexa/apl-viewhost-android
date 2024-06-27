/**
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */
#ifndef ANDROID_JNITEXTLAYOUT_H
#define ANDROID_JNITEXTLAYOUT_H

#include "apl/apl.h"

#ifdef __cplusplus
extern "C" {
#endif
/**
 * Initialize and cache java class and method handles for callback to the rendering layer.
 */
jboolean textlayout_OnLoad(JavaVM *vm, void * reserved );

/**
* Release the class and method cache.
*/
void textlayout_OnUnload(JavaVM *vm, void * reserved );

namespace apl {
    namespace jni {
        class APLTextLayout : public sg::TextLayout {
        public:
            explicit APLTextLayout(const apl::sg::TextChunkPtr& textChunk,
                                   const apl::sg::TextPropertiesPtr& textProperties)
            : mTextChunk(textChunk),
              mTextProperties(textProperties) {}

            ~APLTextLayout() {
                release();
            }

            void release() const;

            bool empty() const override {
                return false;
            }

            Size getSize() const override;

            float getBaseline() const override;

            int getLineCount() const override;

            std::string toDebugString() const override {
                return "TextLayout";
            }

            unsigned int getByteLength() const override;

            Range getLineRangeFromByteRange(Range byteRange) const override;

            Rect getBoundingBoxForLines(Range lineRange) const override;

            std::string getLaidOutText() const override;

            bool isTruncated() const override;

            rapidjson::Value serialize(rapidjson::Document::AllocatorType &allocator) const override;

            void setTextLayout(jobject textLayout);

            jobject getTextLayout() const {
                return mTextLayout;
            }

        private:
            jobject mTextLayout;

            // Hold onto weak pointers strictly for debugging purposes
            std::weak_ptr<apl::sg::TextChunk> mTextChunk;
            std::weak_ptr<apl::sg::TextProperties> mTextProperties;
        };
    } // namespace jni
} // namespace apl

#ifdef __cplusplus
}
#endif
#endif //ANDROID_JNITEXTLAYOUT_H