/**
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *     http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

#ifndef VIEWHOST_DOCUMENTSESSION_H
#define VIEWHOST_DOCUMENTSESSION_H

#include <functional>
#include <memory>

namespace aplviewhost {

/**
 * Defines a grouping of one or more related documents to render. Typically, an APL runtime will map this to
 * AVS skill sessions.
 */
class DocumentSession {
protected:
    DocumentSession() = default;

public:
    virtual ~DocumentSession() = default;

    /**
     * Creates a new DocumentSession instance. The specific type of the instance returned is unspecified, but all
     * DocumentSession instances create from this factory method are guaranteed to be thread-safe.
     *
     * @return A new DocumentSession instance
     */
    static std::shared_ptr<DocumentSession> create();

    /**
     * @return The globally unique ID associated with this session.
     */
    virtual std::string getId() const = 0;

    /**
     * Determines whether this session has ended.
     *
     * @return @c true if end() was previously called for this session, @c false otherwise.
     */
    virtual bool hasEnded() const = 0;

    /**
     * Ends this session, triggering any previously-registered callbacks if the session was still active at the time
     * of the call. Subsequent calls to this method have no effect.
     *
     * There is no guarantee made that any APL documents rendered within this session will be
     * finished. Runtimes should explicitly finish any document they are tracking.
     */
    virtual void end() = 0;
};

using DocumentSessionPtr = std::shared_ptr<DocumentSession>;

} // namespace aplviewhost

#endif // VIEWHOST_DOCUMENTSESSION_H