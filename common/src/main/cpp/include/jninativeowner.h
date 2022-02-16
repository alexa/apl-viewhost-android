/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

#ifndef ANDROID_JNINATIVEOWNER_H
#define ANDROID_JNINATIVEOWNER_H

#include <jni.h>
#include <utility>

namespace apl {
    namespace jni {
        class Lookup {};

        /**
         * Owner object for JNI use with Shared Pointers.  The shared pointer is wrapped by this owner
         * object, and Java peer receives a handle to the owner, rather than the target object.
         * This keeps the shared pointer reference count intact so that the object
         * contained in the shared pointer isn't freed when the shared pointer goes out of scope.
         */
        template<class T = void>
        class NativeOwner {

        public:
            /**
             * Initializes the NativeOwner and binds with the specified object.
             * @param obj The core object.
             */
            explicit NativeOwner(const std::shared_ptr<T> &obj) {
                objPtr = obj;
                lookup = nullptr;
            }

            virtual ~NativeOwner() noexcept = default;

            /**
             * Set the native object.
             * @param obj
             */
            void set(const std::shared_ptr<T> &obj) {
                objPtr = obj;
            }

            /**
             * @return the handle to this object.
             */
            jlong instance() const {
                return reinterpret_cast<jlong>(this);
            }

            /**
             * @return The bounded object.
             */
            std::shared_ptr<T> getBoundObject() const {
                return objPtr;
            }

            /**
             * @return pointer use count for tis object.  A single instance of an object is likely
             * to have a value of 2, pointer used by core to create, and the pointer in this NativeOwner.
             */
            int getPointerCount() {
                return objPtr.use_count();
            }

            /**
             * Unbinds from the object.
             */
            static void unbind(jlong handle) {
                auto nativeOwner = NativeOwner<>::getNativeOwner(handle);
                nativeOwner->objPtr = nullptr;
                nativeOwner->lookup = nullptr;
                delete nativeOwner;
            }


            /**
              * Gets the NativeOwner reference from the specified handle.
              * @param handle The handle of the NativeOwner reference.
              * @return The NativeOwner reference.
              */
            static NativeOwner<T> *getNativeOwner(jlong handle) {
                return reinterpret_cast<NativeOwner<T> *>(handle);
            }

            std::shared_ptr<Lookup> lookup;

        private:
            std::shared_ptr<T> objPtr;
        };


        /**
        * Gets the native peer to a view host Metrics object;
        */
        template<class T>
        inline std::shared_ptr<T>
        get(jlong handle) {
            auto owner = NativeOwner<T>::getNativeOwner(handle);
            if(!owner) {
                return nullptr;
            }
            return owner->getBoundObject();
        }

        /**
        * Gets the native peer to a view host Metrics object;
        */
        template<class T>
        void
        set(jlong handle, const std::shared_ptr<T> &obj) {
            NativeOwner<T>::getNativeOwner(handle)->set(obj);
        }


        /**
         * Creates a NativeOwner for an object and returns the handle.
         */
        template<class T>
        inline jlong createHandle(const std::shared_ptr<T> &ptr) {
            auto owner = new NativeOwner<T>(ptr);
            return owner->instance();
        }


        /**
        * Creates a NativeOwner for an object and returns the handle.
        */
        template<class T, class L>
        inline jlong createHandle(const std::shared_ptr<T> &ptr) {
            auto owner = new NativeOwner<T>(ptr);
            owner->lookup = L::getInstance();
            return owner->instance();
        }


        template<class L>
        inline std::shared_ptr<L> getLookup(jlong handle) {
            auto nativeOwner = NativeOwner<>::getNativeOwner(handle);
            return std::static_pointer_cast<L>(nativeOwner->lookup);
        }
    } //namespace jni
} //namespace apl

#endif //ANDROID_JNINATIVEOWNER_H