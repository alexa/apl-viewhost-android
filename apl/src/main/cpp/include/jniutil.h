/*
 * Copyright 2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

#ifndef ANDROID_JNIUTIL_H
#define ANDROID_JNIUTIL_H


#include <jni.h>
#include <jnimetricstransform.h>
#include "apl/apl.h"
#include <utility>

#ifdef __cplusplus
extern "C" {
#endif

/**
         *  Initialize and cache java class and method handles for callback to the rendering layer.
         */
jboolean
jniutil_OnLoad(JavaVM *vm, void *reserved);

/**
 * Release the class and method cache.
 */
void
jniutil_OnUnload(JavaVM *vm, void *reserved);

namespace apl {
    namespace jni {
        static jclass JAVA_LANG_BOOLEAN;
        static jclass JAVA_LANG_CLASS;
        static jclass JAVA_LANG_DOUBLE;
        static jclass JAVA_LANG_INTEGER;
        static jclass JAVA_LANG_LONG;
        static jclass JAVA_LANG_NUMBER;
        static jclass JAVA_LANG_OBJECT;
        static jclass JAVA_LANG_OBJECT_ARRAY;
        static jclass JAVA_LANG_STRING;
        static jclass JAVA_UTIL_HASHMAP;
        static jclass JAVA_UTIL_ITERATOR;
        static jclass JAVA_UTIL_LIST;
        static jclass JAVA_UTIL_MAP;
        static jclass JAVA_UTIL_MAP_ENTRY;
        static jclass JAVA_UTIL_SET;
        static jclass APL_JSON_DATA;

        static jclass JAVA_LANG_BOOLEAN_TYPE;
        static jclass JAVA_LANG_SHORT_TYPE;
        static jclass JAVA_LANG_INT_TYPE;
        static jclass JAVA_LANG_LONG_TYPE;
        static jclass JAVA_LANG_FLOAT_TYPE;
        static jclass JAVA_LANG_DOUBLE_TYPE;

        static jmethodID JAVA_LANG_BOOLEAN_CONSTRUCTOR;
        static jmethodID JAVA_LANG_BOOLEAN_VALUE;
        static jmethodID JAVA_LANG_CLASS_IS_ARRAY;
        static jmethodID JAVA_LANG_CLASS_GET_COMPONENT_TYPE;
        static jmethodID JAVA_LANG_DOUBLE_CONSTRUCTOR;
        static jmethodID JAVA_LANG_INTEGER_CONSTRUCTOR;
        static jmethodID JAVA_LANG_LONG_CONSTRUCTOR;
        static jmethodID JAVA_LANG_NUMBER_DOUBLE_VALUE;
        static jmethodID JAVA_LANG_OBJECT_TO_STRING;
        static jmethodID JAVA_LANG_STRING_GET_BYTES;
        static jmethodID JAVA_UTIL_HASHMAP_CONSTRUCTOR;
        static jmethodID JAVA_UTIL_HASHMAP_PUT;
        static jmethodID JAVA_UTIL_ITERATOR_HAS_NEXT;
        static jmethodID JAVA_UTIL_ITERATOR_NEXT;
        static jmethodID JAVA_UTIL_LIST_GET;
        static jmethodID JAVA_UTIL_LIST_SIZE;
        static jmethodID JAVA_UTIL_MAP_ENTRY_GET_KEY;
        static jmethodID JAVA_UTIL_MAP_ENTRY_GET_VALUE;
        static jmethodID JAVA_UTIL_MAP_ENTRY_SET;
        static jmethodID JAVA_UTIL_SET_ITERATOR;

        static jfieldID APL_JSON_DATA_MBYTES;

        static jlong JAVA_INTEGER_MAX_VALUE = 2147483647;
        static jlong JAVA_INTEGER_MIN_VALUE = -2147483648;

        static jstring UTF8_STRING;
    }
}

jobject getJObject(JNIEnv *env, const apl::Object& obj);

#ifdef __cplusplus
}
#endif

namespace apl {
    namespace jni {

        std::string getStdString(JNIEnv *env, jstring value);
        apl::Object getAPLObject(JNIEnv *env, jobject object);

// Developer Tips:
//
// javac -h : generate a jni header file from a java class containing native methods.
// javap -s : display to console class and method signatures
// Android NDK JNI tips https://developer.android.com/training/articles/perf-jni

// Java Type   Native Type   Description     Signature
// boolean    jboolean     unsigned 8 bits    Z
// byte       jbyte        signed 8 bits      B
// char       jchar        unsigned 16 bits   C
// short      jshort       signed 16 bits     S
// int        jint         signed 32 bits     I
// long       jlong        signed 64 bits     J
// float      jfloat       32 bits            F
// double     jdouble      64 bits            D
// void       void         N/A


        /**
         * Simple class that abstracts the lookup of a property apl::Object.  Allowing the property
         * to be fetched from any collection or instance. The properties to be bound to an owner
         * that is  native managed object (for example Component, Event) rather than
         * be a managed object themselves.
         */
        class PropertyLookup {
        public:
            virtual apl::Object getObject(int propertyId, jlong handle) = 0;

            virtual std::shared_ptr<Context> getContext(jlong handle) = 0;
        };


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

            std::shared_ptr<PropertyLookup> lookup;
            
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
                LOG(LogLevel::ERROR) << "Could not find owner for handle " << handle;
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


        inline std::shared_ptr<PropertyLookup> getLookup(jlong handle) {
            auto nativeOwner = NativeOwner<>::getNativeOwner(handle);
            return nativeOwner->lookup;
        }

        /**
         * Property lookup for Component properties.
         */
        class EventCommandPropertyLookup : public PropertyLookup {

        public:

            apl::Object getObject(int propertyId, jlong handle) override {
                auto evt = get<Event>(handle);
                auto pk = static_cast<apl::EventProperty>(propertyId);
                return evt->getValue(pk);
            }

            std::shared_ptr<Context> getContext(jlong handle) override {
                auto evt = get<Event>(handle);
                return evt->getComponent()->getContext();
            }

            static std::shared_ptr<EventCommandPropertyLookup> getInstance() {
                if (EventCommandPropertyLookup::instance == nullptr) {
                    EventCommandPropertyLookup::instance = std::make_shared<EventCommandPropertyLookup>();
                }
                return instance;
            }

            explicit EventCommandPropertyLookup() {

            }
        private:

            static std::shared_ptr<EventCommandPropertyLookup> instance;
        };

        /**
        * Property lookup for Component properties.
        */
        class ComponentPropertyLookup : public PropertyLookup {

        public:


            apl::Object getObject(int propertyId, jlong handle) override {
                auto component = get<Component>(handle);
                auto prop = static_cast<apl::PropertyKey>(propertyId);
                return component->getCalculated().get(prop);
            }

            std::shared_ptr<Context> getContext(jlong handle) override {
                auto component = get<Component>(handle);
                return component->getContext();
            }


            static std::shared_ptr<ComponentPropertyLookup> getInstance() {
                if (ComponentPropertyLookup::instance == nullptr) {
                    ComponentPropertyLookup::instance = std::make_shared<ComponentPropertyLookup>();
                }
                return instance;
            }

            explicit ComponentPropertyLookup() {

            }
        private:

            static std::shared_ptr<ComponentPropertyLookup> instance;
        };

        class GraphicPropertyLookup : public PropertyLookup {
        public:
            apl::Object getObject(int propertyId, jlong handle) override {
                auto element = get<GraphicElement>(handle);
                auto prop = static_cast<apl::GraphicPropertyKey>(propertyId);
                return element->getValue(prop);
            }

            std::shared_ptr<Context> getContext(jlong handle) override {
                return nullptr;
            }

            static std::shared_ptr<GraphicPropertyLookup> getInstance() {
                if (GraphicPropertyLookup::instance == nullptr) {
                    GraphicPropertyLookup::instance = std::make_shared<GraphicPropertyLookup>();
                }
                return instance;
            }

            explicit GraphicPropertyLookup() {}

        private:
            static std::shared_ptr<GraphicPropertyLookup> instance;
        };


//
#endif //ANDROID_JNIUTIL_H

    } //namespace jni
} //namespace apl

