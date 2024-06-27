/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

#ifndef APLVIEWHOSTANDROID_JNIMEDIAOBJECT_H
#define APLVIEWHOSTANDROID_JNIMEDIAOBJECT_H

#include <jni.h>
#include "apl/apl.h"
#include "jnimediamanager.h"

namespace apl {
    namespace jni {
    class jniMediaManager;

    class jniMediaObject : public apl::MediaObject, public std::enable_shared_from_this<jniMediaObject> {
        public:
            using CallbackID = int;
            static std::shared_ptr<jniMediaObject> create(std::shared_ptr<jniMediaManager> mediaManager, std::string url, apl::EventMediaType type);

            jniMediaObject(std::weak_ptr<jniMediaManager> mediaManager, std::string url, apl::EventMediaType type);
            ~jniMediaObject();

            std::string url() const override { return mURL; }
            State state() const override { return mState; }
            apl::EventMediaType type() const override { return mType; }
            apl::Size size() const override { return mSize; }
            int errorCode() const override { return mErrorCode; }
            std::string errorDescription() const override { return mErrorDescription; }
            const apl::HeaderArray& headers() const override { return mHeaders; }

            CallbackID addCallback(apl::MediaObjectCallback callback) override;
            void removeCallback(CallbackID callbackId) override;

            apl::GraphicContentPtr graphic() override { return mAVG; }

            void draw(const apl::Rect& dest, const apl::Rect& source, float opacity) const;

            void setJavaMediaObject(jobject javaMediaObject) {
                mJavaMediaObject = javaMediaObject;
            }

            jobject getJavaMediaObject() {
                return mJavaMediaObject;
            }

            void onLoad(int width, int height) {
                mSize = {(float)width, (float)height};
                mState = kReady;
                runCallbacks();
            }

            void onError(int errorCode, std::string errorDescription) {
                mErrorDescription = std::move(errorDescription);
                mState = kError;
                runCallbacks();
            }

    private:
            void runCallbacks();

            State mState = kPending;
            std::string mURL;
            apl::EventMediaType mType;
            apl::Size mSize;
            apl::HeaderArray mHeaders;
            std::unordered_map<CallbackID, apl::MediaObjectCallback> mCallbacks;
            jobject mJavaMediaObject = nullptr;
            std::weak_ptr<jniMediaManager> mMediaManager;
            int mErrorCode = 0;
            std::string mErrorDescription = "Unknown error";

            apl::GraphicContentPtr mAVG;
            CallbackID mCallbackId = 0;
        };

    } // namespace jni
} // namespace apl
#endif //APLVIEWHOSTANDROID_JNIMEDIAOBJECT_H
