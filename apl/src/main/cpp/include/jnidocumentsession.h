/**
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

#ifndef ANDROID_JNIDOCUMENTSESSION_H
#define ANDROID_JNIDOCUMENTSESSION_H

#include "apl/apl.h"
#include "documentsession.h"

namespace apl {
namespace jni {

class AndroidDocumentSession : public aplviewhost::DocumentSession {
public:
    static aplviewhost::DocumentSessionPtr create();

    AndroidDocumentSession();

    std::string getId() const override;

    bool hasEnded() const override;

    void end() override;

    /**
     * @return The underlying extension session for this instance
     */
    apl::ExtensionSessionPtr getExtensionSession() const { return mExtensionSession; }

private:
    mutable std::mutex mMutex;
    apl::ExtensionSessionPtr mExtensionSession;
};

} // namespace jni
} // namespace apl

#endif //ANDROID_JNIDOCUMENTSESSION_H
