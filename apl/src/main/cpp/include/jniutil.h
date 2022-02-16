/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

#ifndef ANDROID_JNIUTIL_H
#define ANDROID_JNIUTIL_H

#include "alexaext/alexaext.h"
#include "apl/apl.h"
#include "jninativeowner.h"

#include <jni.h>
#include <jnimetricstransform.h>
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

        static jclass BOUND_OBJECT;
        static jmethodID BOUND_OBJECT_GET_NATIVE_HANDLE;

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
        class PropertyLookup : public Lookup {
        public:
            virtual apl::Object getObject(int propertyId, jlong handle) = 0;

            virtual std::shared_ptr<Context> getContext(jlong handle) = 0;
        };

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
    } //namespace jni
} //namespace apl

#endif //ANDROID_JNIUTIL_H

