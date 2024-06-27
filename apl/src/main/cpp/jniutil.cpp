/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

#include <jni.h>
#include <string>

#include "jniutil.h"
#include "apl/apl.h"

namespace apl {
    namespace jni {

#ifdef VERSION_NAME
        /**
        * Version string embedded for version information in .rodata .
        */
        const char* versionString = "APLJNI Library is " VERSION_NAME " version.";
#endif

        /**
         * Convert a Java string into a std::string in UTF-8 encoding
         * @param env The Java environment
         * @param value The string to convert
         * @return The std::string
         */
        std::string getStdString(JNIEnv *env, jstring value) {
            // Use the "getBytes" method to retrieve a UTF-8 string
            auto stringJbytes = reinterpret_cast<jbyteArray>(env->CallObjectMethod(value,
                                                                                   JAVA_LANG_STRING_GET_BYTES,
                                                                                   UTF8_STRING));

            // Copy the raw UTF8 byte array into a std::string
            auto length = static_cast<std::string::size_type>(env->GetArrayLength(stringJbytes));
            jbyte *bytes = env->GetByteArrayElements(stringJbytes, nullptr);
            auto result = std::string(reinterpret_cast<char *>(bytes), length);
            env->ReleaseByteArrayElements(stringJbytes, bytes, JNI_ABORT);
            env->DeleteLocalRef(stringJbytes);

            return result;
        }

        /**
         * Convert a Java Object into an APL Object.
         * @param env The Java environment
         * @param object The object to convert
         * @return An APL Object
         */
        apl::Object getAPLObject(JNIEnv *env, jobject object) {
            // Null
            if (object == NULL)
                return Object::NULL_OBJECT();

            // Boolean values
            if (env->IsInstanceOf(object, JAVA_LANG_BOOLEAN) == JNI_TRUE)
                return static_cast<bool>(env->CallBooleanMethod(object, JAVA_LANG_BOOLEAN_VALUE));

            // Numeric values.  This includes integers, floats, doubles, shorts, longs, etc.
            if (env->IsInstanceOf(object, JAVA_LANG_NUMBER) == JNI_TRUE)
                return static_cast<double>(env->CallDoubleMethod(object, JAVA_LANG_NUMBER_DOUBLE_VALUE));

            // String values are converted into std::string
            if (env->IsInstanceOf(object, JAVA_LANG_STRING) == JNI_TRUE)
                return getStdString(env, reinterpret_cast<jstring>(object));

            // An array of objects (e.g., Object[])
            if (env->IsInstanceOf(object, JAVA_LANG_OBJECT_ARRAY) == JNI_TRUE) {
                auto result = std::make_shared<ObjectArray>();
                auto size = env->GetArrayLength(reinterpret_cast<jarray>(object));
                for (jsize i = 0; i < size; i++) {
                    auto entry = env->GetObjectArrayElement(reinterpret_cast<jobjectArray>(object), i);
                    result->emplace_back(getAPLObject(env, entry));
                    env->DeleteLocalRef(entry);
                }
                return result;
            }

            // A Java List class (e.g., ArrayList<String>)
            if (env->IsInstanceOf(object, JAVA_UTIL_LIST) == JNI_TRUE) {
                auto result = std::make_shared<ObjectArray>();
                auto size = env->CallIntMethod(object, JAVA_UTIL_LIST_SIZE);
                for (jsize i = 0 ; i < size ; i++) {
                    auto entry = env->CallObjectMethod(object, JAVA_UTIL_LIST_GET, i);
                    result->emplace_back(getAPLObject(env, entry));
                    env->DeleteLocalRef(entry);
                }
                return result;
            }

            // Subclasses of maps are converted into shared ObjectMaps.  Because Java allows
            // any class to be the "KEY" of the map, we use the Object.toString() method to
            // convert the KEY into a suitable C++ string.
            if (env->IsInstanceOf(object, JAVA_UTIL_MAP) == JNI_TRUE) {
                auto result = std::make_shared<ObjectMap>();
                auto entrySet = env->CallObjectMethod(object, JAVA_UTIL_MAP_ENTRY_SET);
                auto iterator = env->CallObjectMethod(entrySet, JAVA_UTIL_SET_ITERATOR);

                while (env->CallBooleanMethod(iterator, JAVA_UTIL_ITERATOR_HAS_NEXT) == JNI_TRUE) {
                    auto pair = env->CallObjectMethod(iterator, JAVA_UTIL_ITERATOR_NEXT);
                    auto key = env->CallObjectMethod(pair, JAVA_UTIL_MAP_ENTRY_GET_KEY);
                    auto value = env->CallObjectMethod(pair, JAVA_UTIL_MAP_ENTRY_GET_VALUE);
                    auto keyString = env->CallObjectMethod(key, JAVA_LANG_OBJECT_TO_STRING);
                    result->emplace(getStdString(env, reinterpret_cast<jstring>(keyString)),
                                    getAPLObject(env, value));
                    env->DeleteLocalRef(keyString);
                    env->DeleteLocalRef(value);
                    env->DeleteLocalRef(key);
                    env->DeleteLocalRef(pair);
                }
                return result;
            }

            // APLJSONData is a custom class for wrapping raw JSON data objects
            if (env->IsInstanceOf(object, APL_JSON_DATA) == JNI_TRUE) {
                auto handle = env->CallLongMethod(object, BOUND_OBJECT_GET_NATIVE_HANDLE);
                auto jsonData = get<JsonData>(handle);
                return jsonData->get();
            }

            // Primitive arrays are a bit of a nuisance - we have to handle each type of primitive array separately
            jclass clazz = env->GetObjectClass(object);
            if (env->CallBooleanMethod(clazz, JAVA_LANG_CLASS_IS_ARRAY) == JNI_TRUE) {
                auto componentType = reinterpret_cast<jclass>(env->CallObjectMethod(clazz, JAVA_LANG_CLASS_GET_COMPONENT_TYPE));
                auto result = std::make_shared<ObjectArray>();
                auto size = env->GetArrayLength(reinterpret_cast<jarray>(object));

                if (env->IsSameObject(componentType, JAVA_LANG_INT_TYPE)) {
                    auto elements = env->GetIntArrayElements(reinterpret_cast<jintArray>(object), nullptr);
                    for (jint i = 0 ; i < size ; i++)
                        result->emplace_back(elements[i]);
                    env->ReleaseIntArrayElements(reinterpret_cast<jintArray>(object), elements, JNI_ABORT);
                }
                else if (env->IsSameObject(componentType, JAVA_LANG_SHORT_TYPE)) {
                    auto elements = env->GetShortArrayElements(reinterpret_cast<jshortArray>(object), nullptr);
                    for (jint i = 0 ; i < size ; i++)
                        result->emplace_back(elements[i]);
                    env->ReleaseShortArrayElements(reinterpret_cast<jshortArray>(object), elements, JNI_ABORT);
                }
                else if (env->IsSameObject(componentType, JAVA_LANG_LONG_TYPE)) {
                    auto elements = env->GetLongArrayElements(reinterpret_cast<jlongArray>(object), nullptr);
                    for (jint i = 0 ; i < size ; i++)
                        result->emplace_back(elements[i]);
                    env->ReleaseLongArrayElements(reinterpret_cast<jlongArray>(object), elements, JNI_ABORT);
                }
                else if (env->IsSameObject(componentType, JAVA_LANG_FLOAT_TYPE)) {
                    auto elements = env->GetFloatArrayElements(reinterpret_cast<jfloatArray>(object), nullptr);
                    for (jint i = 0 ; i < size ; i++)
                        result->emplace_back(elements[i]);
                    env->ReleaseFloatArrayElements(reinterpret_cast<jfloatArray>(object), elements, JNI_ABORT);
                }
                else if (env->IsSameObject(componentType, JAVA_LANG_DOUBLE_TYPE)) {
                    auto elements = env->GetDoubleArrayElements(reinterpret_cast<jdoubleArray>(object), nullptr);
                    for (jint i = 0 ; i < size ; i++)
                        result->emplace_back(elements[i]);
                    env->ReleaseDoubleArrayElements(reinterpret_cast<jdoubleArray>(object), elements, JNI_ABORT);
                }
                else if (env->IsSameObject(componentType, JAVA_LANG_BOOLEAN_TYPE)) {
                    auto elements = env->GetBooleanArrayElements(reinterpret_cast<jbooleanArray>(object), nullptr);
                    for (jint i = 0 ; i < size ; i++)
                        result->emplace_back(elements[i] ? Object::TRUE_OBJECT() : Object::FALSE_OBJECT());
                    env->ReleaseBooleanArrayElements(reinterpret_cast<jbooleanArray>(object), elements, JNI_ABORT);
                }
                return result;
            }

            return apl::Object::NULL_OBJECT();
        }

        jclass getPrimitiveType(JNIEnv *env, const char *name) {
            auto clazz = reinterpret_cast<jclass>(env->FindClass(name));
            jfieldID type_field = env->GetStaticFieldID(clazz, "TYPE", "Ljava/lang/Class;");
            auto result = reinterpret_cast<jclass>(env->NewGlobalRef(env->GetStaticObjectField(clazz, type_field)));
            env->DeleteLocalRef(clazz);
            return result;
        }

        std::shared_ptr<EventCommandPropertyLookup> EventCommandPropertyLookup::instance = nullptr;
        std::shared_ptr<ComponentPropertyLookup> ComponentPropertyLookup::instance = nullptr;
        std::shared_ptr<GraphicPropertyLookup> GraphicPropertyLookup::instance = nullptr;

#ifdef __cplusplus
        extern "C" {
#endif

#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wunused-parameter"

        /**
         *  Initialize and cache java class and method handles for callback to the rendering layer.
         */
        jboolean
        jniutil_OnLoad(JavaVM *vm, void *reserved) {
            JNIEnv *env;
            if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
                return JNI_FALSE;
            }

            LOG(apl::LogLevel::kDebug) << "Loading View Host Utils JNI environment.";

            // For reading Integer and Boolean types
            JAVA_LANG_BOOLEAN = reinterpret_cast<jclass>(env->NewGlobalRef(env->FindClass("java/lang/Boolean")));
            JAVA_LANG_BOOLEAN_CONSTRUCTOR = env->GetMethodID(JAVA_LANG_BOOLEAN, "<init>", "(Z)V");
            JAVA_LANG_BOOLEAN_VALUE = env->GetMethodID(JAVA_LANG_BOOLEAN, "booleanValue", "()Z");

            JAVA_LANG_CLASS = reinterpret_cast<jclass>(env->NewGlobalRef(env->FindClass("java/lang/Class")));
            JAVA_LANG_CLASS_IS_ARRAY = env->GetMethodID(JAVA_LANG_CLASS, "isArray", "()Z");
            JAVA_LANG_CLASS_GET_COMPONENT_TYPE = env->GetMethodID(JAVA_LANG_CLASS, "getComponentType", "()Ljava/lang/Class;");

            JAVA_LANG_DOUBLE = reinterpret_cast<jclass>(env->NewGlobalRef(env->FindClass("java/lang/Double")));
            JAVA_LANG_DOUBLE_CONSTRUCTOR = env->GetMethodID(JAVA_LANG_DOUBLE, "<init>", "(D)V");

            JAVA_LANG_INTEGER = reinterpret_cast<jclass>(env->NewGlobalRef(env->FindClass("java/lang/Integer")));
            JAVA_LANG_INTEGER_CONSTRUCTOR = env->GetMethodID(JAVA_LANG_INTEGER, "<init>", "(I)V");

            JAVA_LANG_LONG = reinterpret_cast<jclass>(env->NewGlobalRef(env->FindClass("java/lang/Long")));
            JAVA_LANG_LONG_CONSTRUCTOR = env->GetMethodID(JAVA_LANG_LONG, "<init>", "(J)V");

            JAVA_LANG_NUMBER = reinterpret_cast<jclass>(env->NewGlobalRef(env->FindClass("java/lang/Number")));
            JAVA_LANG_NUMBER_DOUBLE_VALUE = env->GetMethodID(JAVA_LANG_NUMBER, "doubleValue", "()D");

            JAVA_LANG_OBJECT = reinterpret_cast<jclass>(env->NewGlobalRef(env->FindClass("java/lang/Object")));
            JAVA_LANG_OBJECT_TO_STRING = env->GetMethodID(JAVA_LANG_OBJECT, "toString", "()Ljava/lang/String;");

            JAVA_LANG_OBJECT_ARRAY = reinterpret_cast<jclass>(env->NewGlobalRef(env->FindClass("[Ljava/lang/Object;")));

            JAVA_LANG_STRING = reinterpret_cast<jclass>(env->NewGlobalRef(env->FindClass("java/lang/String")));
            JAVA_LANG_STRING_GET_BYTES = env->GetMethodID(JAVA_LANG_STRING, "getBytes", "(Ljava/lang/String;)[B");

            JAVA_UTIL_ITERATOR = reinterpret_cast<jclass>(env->NewGlobalRef(env->FindClass("java/util/Iterator")));
            JAVA_UTIL_ITERATOR_HAS_NEXT = env->GetMethodID(JAVA_UTIL_ITERATOR, "hasNext", "()Z");
            JAVA_UTIL_ITERATOR_NEXT = env->GetMethodID(JAVA_UTIL_ITERATOR, "next", "()Ljava/lang/Object;");

            JAVA_UTIL_LIST = reinterpret_cast<jclass>(env->NewGlobalRef(env->FindClass("java/util/List")));
            JAVA_UTIL_LIST_GET = env->GetMethodID(JAVA_UTIL_LIST, "get", "(I)Ljava/lang/Object;");
            JAVA_UTIL_LIST_SIZE = env->GetMethodID(JAVA_UTIL_LIST, "size", "()I");

            JAVA_UTIL_MAP = reinterpret_cast<jclass>(env->NewGlobalRef(env->FindClass("java/util/Map")));
            JAVA_UTIL_MAP_ENTRY = reinterpret_cast<jclass>(env->NewGlobalRef(env->FindClass("java/util/Map$Entry")));
            JAVA_UTIL_MAP_ENTRY_GET_KEY = env->GetMethodID(JAVA_UTIL_MAP_ENTRY, "getKey", "()Ljava/lang/Object;");
            JAVA_UTIL_MAP_ENTRY_GET_VALUE = env->GetMethodID(JAVA_UTIL_MAP_ENTRY, "getValue", "()Ljava/lang/Object;");
            JAVA_UTIL_MAP_ENTRY_SET = env->GetMethodID(JAVA_UTIL_MAP, "entrySet", "()Ljava/util/Set;");

            JAVA_UTIL_SET = reinterpret_cast<jclass>(env->NewGlobalRef(env->FindClass("java/util/Set")));
            JAVA_UTIL_SET_ITERATOR = env->GetMethodID(JAVA_UTIL_SET, "iterator", "()Ljava/util/Iterator;");

            JAVA_UTIL_HASHMAP = reinterpret_cast<jclass>(env->NewGlobalRef(env->FindClass("java/util/HashMap")));
            JAVA_UTIL_HASHMAP_CONSTRUCTOR = env->GetMethodID(JAVA_UTIL_HASHMAP, "<init>", "()V");
            JAVA_UTIL_HASHMAP_PUT = env->GetMethodID(JAVA_UTIL_HASHMAP, "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");

            APL_JSON_DATA = reinterpret_cast<jclass>(env->NewGlobalRef(env->FindClass("com/amazon/apl/android/APLJSONData")));
            BOUND_OBJECT = reinterpret_cast<jclass>(env->NewGlobalRef(env->FindClass("com/amazon/common/BoundObject")));
            BOUND_OBJECT_GET_NATIVE_HANDLE = env->GetMethodID(BOUND_OBJECT, "getNativeHandle", "()J");

            jfieldID type_field = env->GetStaticFieldID(JAVA_LANG_INTEGER, "TYPE", "Ljava/lang/Class;");
            JAVA_LANG_INT_TYPE = reinterpret_cast<jclass>(env->NewGlobalRef(env->GetStaticObjectField(JAVA_LANG_INTEGER, type_field)));

            type_field = env->GetStaticFieldID(JAVA_LANG_BOOLEAN, "TYPE", "Ljava/lang/Class;");
            JAVA_LANG_BOOLEAN_TYPE = reinterpret_cast<jclass>(env->NewGlobalRef(env->GetStaticObjectField(JAVA_LANG_BOOLEAN, type_field)));

            JAVA_LANG_SHORT_TYPE = getPrimitiveType(env, "java/lang/Short");
            JAVA_LANG_LONG_TYPE = getPrimitiveType(env, "java/lang/Long");
            JAVA_LANG_FLOAT_TYPE = getPrimitiveType(env, "java/lang/Float");
            JAVA_LANG_DOUBLE_TYPE = getPrimitiveType(env, "java/lang/Double");

            UTF8_STRING = reinterpret_cast<jstring>(env->NewGlobalRef(env->NewStringUTF("UTF-8")));

            if (
                    JAVA_LANG_BOOLEAN == nullptr ||
                    JAVA_LANG_INTEGER == nullptr ||
                    JAVA_LANG_LONG == nullptr ||
                    JAVA_LANG_BOOLEAN_CONSTRUCTOR == nullptr ||
                    JAVA_LANG_INTEGER_CONSTRUCTOR == nullptr ||
                    JAVA_LANG_LONG_CONSTRUCTOR == nullptr ||
                    JAVA_LANG_OBJECT == nullptr ||
                    JAVA_UTIL_HASHMAP == nullptr ||
                    JAVA_UTIL_HASHMAP_CONSTRUCTOR == nullptr ||
                    JAVA_UTIL_HASHMAP_PUT == nullptr
            ) {
                LOG(apl::LogLevel::kError)
                        << "Could not load classes for jniutils";
                return JNI_FALSE;
            }
            return JNI_TRUE;
        }

        /**
         * Release the class and method cache.
         */
        void
        jniutil_OnUnload(JavaVM *vm, void *reserved) {


        }

        /**
         * Converts an `apl::Object` into a Java Object. Because an apl::Object can be an array
         * of `apl::Object`s, all primitives are converted to the Java class primitive wrappers. For
         * example, `bool` is converted to a `Boolean` not a `boolean`. This allows us to create
         * Object[] and Map<String, Object>. In general, though, use primitive types whenever possible.
         * @param env The JNI Environment
         * @param obj The object to concert
         * @return A Java Object that represents the apl::Object
         */
        jobject getJObject(JNIEnv *env, const apl::Object& obj) {
            if (obj.isNull()) {
                return NULL;
            }


            if (obj.isBoolean()) {
                return env->NewObject(JAVA_LANG_BOOLEAN, JAVA_LANG_BOOLEAN_CONSTRUCTOR, obj.getBoolean());
            }
            else if (obj.isNumber()) {
                double integerPart;
                double value = obj.getDouble();
                if (std::modf(value, &integerPart) == 0.0) {
                    auto asLong = static_cast<jlong>(integerPart);
                    if (asLong > JAVA_INTEGER_MAX_VALUE ||
                        asLong < JAVA_INTEGER_MIN_VALUE) {
                        return env->NewObject(JAVA_LANG_LONG, JAVA_LANG_LONG_CONSTRUCTOR, asLong);
                    } else {
                        return env->NewObject(JAVA_LANG_INTEGER, JAVA_LANG_INTEGER_CONSTRUCTOR,
                                              static_cast<jint>(integerPart));
                    }
                }
                return env->NewObject(JAVA_LANG_DOUBLE, JAVA_LANG_DOUBLE_CONSTRUCTOR,
                        static_cast<jdouble>(value));
            }
            else if (obj.isString()) {
                return env->NewStringUTF(obj.asString().c_str());
            }
            else if (obj.isArray()) {
                auto& array = obj.getArray();
                jobjectArray objArray = env->NewObjectArray(array.size(), JAVA_LANG_OBJECT, nullptr);
                for(int i = 0; i < array.size(); i++) {
                    jobject value = getJObject(env, array[i]);
                    env->SetObjectArrayElement(objArray, i, value);
                    env->DeleteLocalRef(value);
                }
                return objArray;
            }
            else if (obj.isMap()) {
                auto& map = obj.getMap();
                jobject javaMap = env->NewObject(JAVA_UTIL_HASHMAP, JAVA_UTIL_HASHMAP_CONSTRUCTOR);
                for(auto& it : map) {
                    jstring key = env->NewStringUTF(it.first.c_str());
                    jobject jvalue = getJObject(env, it.second);
                    env->CallObjectMethod(javaMap, JAVA_UTIL_HASHMAP_PUT, key, jvalue);
                    env->DeleteLocalRef(key);
                    env->DeleteLocalRef(jvalue);
                }
                return javaMap;
            }

            LOG(LogLevel::kDebug) << "Type not supported";
            return nullptr;
        }

        JNIEXPORT jobject JNICALL
        Java_com_amazon_apl_android_PropertyMap_nGet(JNIEnv *env, jclass clazz, jlong handle,
                                                     jint propertyId) {
            auto value = getLookup<PropertyLookup>(handle)->getObject(static_cast<int>(propertyId), handle);
            return getJObject(env, value);
        }

        JNIEXPORT jboolean JNICALL
        Java_com_amazon_apl_android_PropertyMap_nIsColor(JNIEnv *env, jclass clazz, jlong handle,
                                                         jint propertyId) {
            auto value = getLookup<PropertyLookup>(handle)->getObject(static_cast<int>(propertyId), handle);
            return value.is<Color>();
        }

        JNIEXPORT jboolean JNICALL
        Java_com_amazon_apl_android_PropertyMap_nIsGradient(JNIEnv *env, jclass clazz, jlong handle,
                                                         jint propertyId) {
            auto value = getLookup<PropertyLookup>(handle)->getObject(static_cast<int>(propertyId), handle);
            return value.is<Gradient>();
        }

        JNIEXPORT jboolean JNICALL
        Java_com_amazon_apl_android_PropertyMap_nIsGraphicPattern(JNIEnv *env, jclass clazz, jlong handle,
                                                         jint propertyId) {
            auto value = getLookup<PropertyLookup>(handle)->getObject(static_cast<int>(propertyId), handle);
            return value.is<GraphicPattern>();
        }

        JNIEXPORT jboolean JNICALL
        Java_com_amazon_apl_android_PropertyMap_nHasProperty(JNIEnv *env, jclass clazz, jlong handle,
                                                           jint propertyId) {

            auto value = getLookup<PropertyLookup>(handle)->getObject(static_cast<int>(propertyId), handle);
            return static_cast<jboolean>(!value.isNull());
        }

        JNIEXPORT jint JNICALL
        Java_com_amazon_apl_android_PropertyMap_nGetInt(JNIEnv *env, jclass clazz, jlong handle,
                                                      jint propertyId) {

            auto value = getLookup<PropertyLookup>(handle)->getObject(static_cast<int>(propertyId), handle);
            return static_cast<jint>(value.asNumber());
        }


        JNIEXPORT jint JNICALL
        Java_com_amazon_apl_android_PropertyMap_nGetEnum(JNIEnv *env, jclass clazz, jlong handle,
                                                       jint propertyId) {

            auto value = getLookup<PropertyLookup>(handle)->getObject(static_cast<int>(propertyId), handle);
            // unlikely that enum doesn't have a default value, or possibly the propertyId was wrong
            if (value.isNull())
                return -1;
            return static_cast<jint>(value.asNumber());
        }

        JNIEXPORT jfloatArray JNICALL
        Java_com_amazon_apl_android_PropertyMap_nGetTransform(JNIEnv *env, jclass clazz, jlong handle,
                                                              jint propertyId) {
            auto lookup = getLookup<PropertyLookup>(handle);
            auto value = lookup->getObject(static_cast<int>(propertyId), handle);
            auto transform = value.get<Transform2D>().get();
            jfloatArray ret = env->NewFloatArray(6);
            env->SetFloatArrayRegion(ret, 0, 6, transform.data());
            return ret;
        }

        JNIEXPORT jboolean JNICALL
        Java_com_amazon_apl_android_PropertyMap_nHasTransform(JNIEnv *env, jclass clazz, jlong handle,
                                                              jint propertyId) {
            auto lookup = getLookup<PropertyLookup>(handle);
            auto value = lookup->getObject(static_cast<int>(propertyId), handle);
            auto transform = value.get<Transform2D>();
            return static_cast<jboolean>(!transform.isIdentity());
        }

        JNIEXPORT jfloat JNICALL
        Java_com_amazon_apl_android_PropertyMap_nGetFloat(JNIEnv *env, jclass clazz, jlong handle,
                                                        jint propertyId) {

            auto value = getLookup<PropertyLookup>(handle)->getObject(static_cast<int>(propertyId), handle);
            return static_cast<jfloat>(value.asNumber());
        }

        JNIEXPORT jboolean JNICALL
        Java_com_amazon_apl_android_PropertyMap_nGetBoolean(JNIEnv *env, jclass clazz, jlong handle,
                                                          jint propertyId) {
            auto value = getLookup<PropertyLookup>(handle)->getObject(static_cast<int>(propertyId), handle);
            return static_cast<jboolean>(value.asBoolean());
        }

        JNIEXPORT jstring JNICALL
        Java_com_amazon_apl_android_PropertyMap_nGetString(JNIEnv *env, jclass clazz, jlong handle,
                                                         jint propertyId) {

            auto value = getLookup<PropertyLookup>(handle)->getObject(static_cast<int>(propertyId), handle);
            if (value.isNull())
                return NULL;

            std::u16string u16 = converter.from_bytes(value.asString().c_str());
            return env->NewString(reinterpret_cast<const jchar *>(u16.c_str()), u16.length());
        }

        JNIEXPORT jlong JNICALL
        Java_com_amazon_apl_android_PropertyMap_nGetColor(JNIEnv *env, jclass clazz, jlong handle,
                                                        jint propertyId) {

            auto value = getLookup<PropertyLookup>(handle)->getObject(static_cast<int>(propertyId), handle);
            return static_cast<jlong>(value.asColor().get());
        }

        JNIEXPORT jfloatArray JNICALL
        Java_com_amazon_apl_android_PropertyMap_nGetFloatArray(JNIEnv *env, jclass clazz, jlong handle,
                                                            jint propertyId) {
            auto data =
                    getLookup<PropertyLookup>(handle)->getObject(static_cast<int>(propertyId), handle).getArray();
            int inputCount = data.size();
            jfloat range[inputCount];
            for (int i = 0; i < inputCount; i++) {
                range[i] = static_cast<float>(data[i].asNumber());
            }
            jfloatArray inputRange = env->NewFloatArray(inputCount);
            env->SetFloatArrayRegion(inputRange, 0, inputCount, range);
            return inputRange;
        }

        JNIEXPORT jintArray JNICALL
        Java_com_amazon_apl_android_PropertyMap_nGetIntArray(JNIEnv *env, jclass clazz, jlong handle,
                                                             jint propertyId) {
            auto data =
                    getLookup<PropertyLookup>(handle)->getObject(static_cast<int>(propertyId), handle).getArray();
            int inputCount = data.size();
            jint range[inputCount];
            for (int i = 0; i < inputCount; i++) {
                range[i] = static_cast<int>(data[i].asNumber());
            }
            jintArray inputRange = env->NewIntArray(inputCount);
            env->SetIntArrayRegion(inputRange, 0, inputCount, range);
            return inputRange;
        }

        JNIEXPORT jlong JNICALL
        Java_com_amazon_apl_android_APLJSONData_nCreate(JNIEnv *env, jclass clazz, jstring data_) {
            const char* data = env->GetStringUTFChars(data_, nullptr);
            auto jsonData = JsonData(data);
            env->ReleaseStringUTFChars(data_, data);
            return createHandle(std::make_shared<JsonData>(std::move(jsonData)));
        }

        JNIEXPORT jlong JNICALL
        Java_com_amazon_apl_android_APLJSONData_nCreateWithByteArray(JNIEnv *env, jclass clazz, jbyteArray byteArray) {
                auto elements = env->GetByteArrayElements(byteArray, nullptr);
                auto length = static_cast<std::string::size_type>(env->GetArrayLength(byteArray));

                rapidjson::Document doc;
                rapidjson::ParseResult ok = doc.Parse<
                        rapidjson::kParseValidateEncodingFlag | rapidjson::kParseStopWhenDoneFlag>(
                        reinterpret_cast<const char *>(elements), length);
                env->ReleaseByteArrayElements(byteArray, elements, JNI_ABORT);
                if (ok.IsError()) {
                    LOG(apl::LogLevel::kError)
                            << "Parsing error: " << rapidjson::GetParseError_En(ok.Code());
                }
                auto jsonData = JsonData(std::move(doc));
                return createHandle(std::make_shared<JsonData>(std::move(jsonData)));
        }


#pragma clang diagnostic pop

#ifdef __cplusplus
}
#endif

    } //namespace jni
} //namespace apl


