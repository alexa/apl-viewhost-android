/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

#include <jni.h>

#include <locale>
#include <codecvt>
#include <vector>
#include <string>
#include <memory>
#include "jniutil.h"
#include "jnicomplexproperty.h"

namespace apl {
    namespace jni {

#ifdef __cplusplus
        extern "C" {
#endif

#pragma clang diagnostic push
#pragma ide diagnostic ignored "OCUnusedGlobalDeclarationInspection"
        const bool DEBUG_JNI = true;
#pragma clang diagnostic pop

#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wunused-parameter"

        /**
         * Create a class and method cache for calls to View Host.
         */
        jboolean
        complexproperty_OnLoad(JavaVM *vm, void *reserved) {

            LOG(apl::LogLevel::kDebug) << "Loading View Host ComplexProperty JNI environment.";

            JNIEnv *env;
            if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
                return JNI_FALSE;
            }

            JAVA_LANG_STRING = reinterpret_cast<jclass>(env->NewGlobalRef(env->FindClass("java/lang/String")));

            return JNI_TRUE;
        }

        /**
         * Release the class and method cache.
         */
        void
        complexproperty_OnUnload(JavaVM *vm, void *reserved) {
            LOG(apl::LogLevel::kDebug) << "Unloading View Host Component JNI environment.";
            apl::LoggerFactory::instance().reset();

            JNIEnv *env;
            if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
                // environment failure, can't proceed.
                return;
            }
        }

        JNIEXPORT jdouble JNICALL
        Java_com_amazon_apl_android_primitive_Dimension_nGetValue(JNIEnv *env, jclass clazz,
                                                                  jlong handle, jint propertyId) {
            auto lookup = getLookup<PropertyLookup>(handle);
            auto value = lookup->getObject(static_cast<int>(propertyId), handle);
            auto d = value.asDimension(*lookup->getContext(handle));
            return static_cast<jdouble>(d.getValue());
        }

        JNIEXPORT jfloatArray JNICALL
        Java_com_amazon_apl_android_primitive_Rect_nGetRect(JNIEnv *env, jclass clazz,
                                                              jlong handle, jint propertyId) {
            auto lookup = getLookup<PropertyLookup>(handle);
            auto value = lookup->getObject(static_cast<int>(propertyId), handle);
            const auto& rect = value.get<Rect>();
            float buffer[4] = { rect.getLeft(),
                                rect.getTop(),
                                rect.getWidth(),
                                rect.getHeight()};

            jfloatArray jrect = env->NewFloatArray(4);
            env->SetFloatArrayRegion(jrect, 0, 4, buffer);

            return jrect;
        }

        JNIEXPORT jfloat JNICALL
        Java_com_amazon_apl_android_primitive_Radii_nGetTopLeft(JNIEnv *env, jclass clazz,
                                                                jlong handle, jint propertyId) {
            auto lookup = getLookup<PropertyLookup>(handle);
            auto value = lookup->getObject(static_cast<int>(propertyId), handle);
            auto radii = value.get<Radii>();
            return static_cast<jfloat>(radii.topLeft());
        }

        JNIEXPORT jfloat JNICALL
        Java_com_amazon_apl_android_primitive_Radii_nGetTopRight(JNIEnv *env, jclass clazz,
                                                                 jlong handle, jint propertyId) {
            auto lookup = getLookup<PropertyLookup>(handle);
            auto value = lookup->getObject(static_cast<int>(propertyId), handle);
            auto radii = value.get<Radii>();
            return static_cast<jfloat>(radii.topRight());
        }

        JNIEXPORT jfloat JNICALL
        Java_com_amazon_apl_android_primitive_Radii_nGetBottomRight(JNIEnv *env, jclass clazz,
                                                                    jlong handle, jint propertyId) {
            auto lookup = getLookup<PropertyLookup>(handle);
            auto value = lookup->getObject(static_cast<int>(propertyId), handle);
            auto radii = value.get<Radii>();
            return static_cast<jfloat>(radii.bottomRight());
        }

        JNIEXPORT jfloat JNICALL
        Java_com_amazon_apl_android_primitive_Radii_nGetBottomLeft(JNIEnv *env, jclass clazz,
                                                                   jlong handle, jint propertyId) {
            auto lookup = getLookup<PropertyLookup>(handle);
            auto value = lookup->getObject(static_cast<int>(propertyId), handle);
            auto radii = value.get<Radii>();
            return static_cast<jfloat>(radii.bottomLeft());
        }

        JNIEXPORT jstring JNICALL
        Java_com_amazon_apl_android_primitive_StyledText_nGetText(JNIEnv *env, jclass clazz,
                                                                  jlong handle, jint propertyId) {

            auto value = getLookup<PropertyLookup>(handle)->getObject(static_cast<int>(propertyId), handle);
            const auto &styledText = value.get<StyledText>();

            // for emoji characters, rapidjson stores in 4-byte format
            // eg. U+1F33D -> \xF0\x9F\x8C\xBD
            // but NewStringUTF expects modified UTF-8 and \xF0 is invalid
            // so converts the rapidjson value to UTF-16
            // and uses NewString to create Java string instead
            // not using NewStringUTF
            std::wstring_convert<std::codecvt_utf8_utf16<char16_t>, char16_t> converter;
            std::u16string u16str = converter.from_bytes(styledText.getText().data());
            return env->NewString((const jchar*)u16str.data(), u16str.length());
        }

        JNIEXPORT jint JNICALL
        Java_com_amazon_apl_android_primitive_StyledText_nGetSpanCount(JNIEnv *env, jclass clazz,
                                                                       jlong handle,
                                                                       jint propertyId) {
            auto value = getLookup<PropertyLookup>(handle)->getObject(static_cast<int>(propertyId), handle);
            const auto &styledText = value.get<StyledText>();
            auto spans = styledText.getSpans();
            return static_cast<jint>(spans.size());
        }

        JNIEXPORT jint JNICALL
        Java_com_amazon_apl_android_primitive_StyledText_nGetSpanTypeAt(JNIEnv *env, jclass clazz,
                                                                        jlong handle,
                                                                        jint propertyId,
                                                                        jint index) {
            auto value = getLookup<PropertyLookup>(handle)->getObject(static_cast<int>(propertyId), handle);
            const auto &styledText = value.get<StyledText>();
            auto spans = styledText.getSpans();
            auto span = spans.at(static_cast<unsigned int>(index));
            return static_cast<jint>(span.type);
        }

        JNIEXPORT jint JNICALL
        Java_com_amazon_apl_android_primitive_StyledText_nGetSpanStartAt(JNIEnv *env, jclass clazz,
                                                                         jlong handle,
                                                                         jint propertyId,
                                                                         jint index) {
            auto value = getLookup<PropertyLookup>(handle)->getObject(static_cast<int>(propertyId), handle);
            const auto &styledText = value.get<StyledText>();
            auto spans = styledText.getSpans();
            auto span = spans.at(static_cast<unsigned int>(index));
            return static_cast<jint>(span.start);
        }

        JNIEXPORT jint JNICALL
        Java_com_amazon_apl_android_primitive_StyledText_nGetSpanEndAt(JNIEnv *env, jclass clazz,
                                                                       jlong handle,
                                                                       jint propertyId,
                                                                       jint index) {
            auto value = getLookup<PropertyLookup>(handle)->getObject(static_cast<int>(propertyId), handle);
            const auto &styledText = value.get<StyledText>();
            auto spans = styledText.getSpans();
            auto span = spans.at(static_cast<unsigned int>(index));
            return static_cast<jint>(span.end);
        }

        JNIEXPORT jlong JNICALL
        Java_com_amazon_apl_android_primitive_StyledText_nCreateStyledTextIterator(
                                                                       JNIEnv *env, jclass clazz,
                                                                       jlong handle,
                                                                       jint propertyId) {
            auto value = getLookup<PropertyLookup>(handle)->getObject(static_cast<int>(propertyId), handle);
            const auto &styledText = value.get<StyledText>();
            auto spans = styledText.getSpans();
            return (jlong) new StyledText::Iterator(styledText);
        }

        JNIEXPORT jint JNICALL
        Java_com_amazon_apl_android_primitive_StyledText_nStyledTextIteratorNext(JNIEnv *env, jclass clazz, jlong nativePtr) {
            auto it = (StyledText::Iterator *) nativePtr;
            return static_cast<jint>(it->next());
        }

        JNIEXPORT jint JNICALL
        Java_com_amazon_apl_android_primitive_StyledText_nStyledTextIteratorGetSpanType(JNIEnv *env, jclass clazz, jlong nativePtr) {
            auto it = (StyledText::Iterator *) nativePtr;
            return static_cast<jint>(it->getSpanType());
        }

        JNIEXPORT jintArray JNICALL
        Java_com_amazon_apl_android_primitive_StyledText_nStyledTextIteratorGetSpanAttributesNames(JNIEnv *env, jclass clazz, jlong nativePtr) {
            auto it = (StyledText::Iterator *) nativePtr;
            auto spanAttributes = it->getSpanAttributes();

            int count = spanAttributes.size();
            jint names[count];
            for (int i = 0; i < count; i++) {
                names[i] = static_cast<int>(spanAttributes[i].name);
            }
            jintArray result = env->NewIntArray(count);
            env->SetIntArrayRegion(result, 0, count, names);

            return result;
        }

        JNIEXPORT jlong JNICALL
        Java_com_amazon_apl_android_primitive_StyledText_nStyledTextIteratorGetSpanAttributeGetColor(JNIEnv *env, jclass clazz, jlong nativePtr, jint attributeKey) {
            auto it = (StyledText::Iterator *) nativePtr;
            auto spanAttributes = it->getSpanAttributes();

            for (auto attr : spanAttributes) {
                if (attr.name == attributeKey) {
                    return static_cast<jlong>(attr.value.asColor().get());
                }
            }

            return 0;
        }

        JNIEXPORT jdouble JNICALL
        Java_com_amazon_apl_android_primitive_StyledText_nStyledTextIteratorGetSpanAttributeGetDimension(JNIEnv *env, jclass clazz, jlong nativePtr, jint attributeKey) {
            auto it = (StyledText::Iterator *) nativePtr;
            auto spanAttributes = it->getSpanAttributes();

            for (auto attr : spanAttributes) {
                if (attr.name == attributeKey) {
                    if (attr.value.isAbsoluteDimension()) {
                        auto dimension = attr.value;
                        return static_cast<jdouble>(dimension.getAbsoluteDimension());
                    }
                }
            }

            return 0;
        }

        JNIEXPORT jbyteArray JNICALL
        Java_com_amazon_apl_android_primitive_StyledText_nStyledTextIteratorGetString(JNIEnv *env, jclass clazz, jlong nativePtr) {
            auto it = (StyledText::Iterator *) nativePtr;
            auto len = it->getString().length();
            jbyteArray bytes = env->NewByteArray(len);
            env->SetByteArrayRegion(bytes, 0, len, (jbyte*) it->getString().c_str());
            return bytes;
        }

        JNIEXPORT void JNICALL
        Java_com_amazon_apl_android_primitive_StyledText_nDestroyStyledTextIterator(JNIEnv *env, jclass clazz, jlong nativePtr) {
            delete (StyledText::Iterator *) nativePtr;
        }

        JNIEXPORT jfloatArray JNICALL
        Java_com_amazon_apl_android_primitive_Gradient_nGetFloatArray(JNIEnv *env, jclass clazz,
                                                                      jlong handle,
                                                                      jint propertyId,
                                                                      jint graphicPropertyKey) {
            auto value = getLookup<PropertyLookup>(handle)->getObject(static_cast<int>(propertyId), handle);
            const auto &g = value.get<Gradient>();

            auto gradientArray = g.getProperty(static_cast<GradientProperty>(graphicPropertyKey)).getArray();
            int count = gradientArray.size();
            jfloat range[count];
            for (int i = 0; i < count; i++) {
                range[i] = static_cast<float>(gradientArray[i].asNumber());
            }
            jfloatArray result = env->NewFloatArray(count);
            env->SetFloatArrayRegion(result, 0, count, range);

            return result;
        }

        JNIEXPORT jlongArray JNICALL
        Java_com_amazon_apl_android_primitive_Gradient_nGetColorArray(JNIEnv *env, jclass clazz,
                                                                      jlong handle,
                                                                      jint propertyId,
                                                                      jint graphicPropertyKey) {
            auto value = getLookup<PropertyLookup>(handle)->getObject(static_cast<int>(propertyId), handle);
            const auto &g = value.get<Gradient>();

            auto gradientArray = g.getProperty(static_cast<GradientProperty>(graphicPropertyKey)).getArray();
            int count = gradientArray.size();
            jlong range[count];
            for (int i = 0; i < count; i++) {
                range[i] = static_cast<long>(gradientArray[i].asColor().get());
            }
            jlongArray result = env->NewLongArray(count);
            env->SetLongArrayRegion(result, 0, count, range);

            return result;
        }

        JNIEXPORT jfloat JNICALL
        Java_com_amazon_apl_android_primitive_Gradient_nGetFloat(JNIEnv *env, jclass clazz, jlong handle,
                                                                 jint propertyId, jint graphicPropertyKey) {

            auto value = getLookup<PropertyLookup>(handle)->getObject(static_cast<int>(propertyId), handle);
            const auto &g = value.get<Gradient>();
            return static_cast<jfloat>(g.getProperty(static_cast<GradientProperty>(graphicPropertyKey)).asNumber());
        }

        JNIEXPORT jint JNICALL
        Java_com_amazon_apl_android_primitive_Gradient_nGetInt(JNIEnv *env, jclass clazz, jlong handle,
                                                                 jint propertyId, jint graphicPropertyKey) {

            auto value = getLookup<PropertyLookup>(handle)->getObject(static_cast<int>(propertyId), handle);
            const auto &g = value.get<Gradient>();
            return static_cast<jint>(g.getProperty(static_cast<GradientProperty>(graphicPropertyKey)).asInt());
        }

        JNIEXPORT jint JNICALL
        Java_com_amazon_apl_android_primitive_Filters_nGetFilterTypeAt(JNIEnv *env, jclass clazz,
                                                                       jlong handle, jint propertyId, jint index) {

            auto value = getLookup<PropertyLookup>(handle)->getObject(static_cast<int>(propertyId), handle);
            const auto& array = value.getArray();
            auto &f = array[index].get<Filter>();
            return static_cast<jint>(f.getType());
        }

        JNIEXPORT jint JNICALL
        Java_com_amazon_apl_android_primitive_Filters_nGetNoiseKindAt(JNIEnv *env, jclass clazz,
                                                                      jlong handle, jint propertyId, jint index) {

            auto value = getLookup<PropertyLookup>(handle)->getObject(static_cast<int>(propertyId), handle);
            const auto& array = value.getArray();
            auto &f = array[index].get<Filter>();
            auto val = f.getValue(kFilterPropertyKind).getInteger();
            return static_cast<jint>(val);
        }

        JNIEXPORT jboolean JNICALL
        Java_com_amazon_apl_android_primitive_Filters_nGetNoiseUseColorAt(JNIEnv *env, jclass clazz,
                                                                          jlong handle, jint propertyId, jint index) {
            auto value = getLookup<PropertyLookup>(handle)->getObject(static_cast<int>(propertyId), handle);
            const auto& array = value.getArray();
            auto &f = array[index].get<Filter>();
            auto val = f.getValue(kFilterPropertyUseColor).getBoolean();
            return static_cast<jboolean>(val);
        }

        JNIEXPORT jfloat JNICALL
        Java_com_amazon_apl_android_primitive_Filters_nGetNoiseSigmaAt(JNIEnv *env, jclass clazz,
                                                                       jlong handle, jint propertyId, jint index) {

            auto value = getLookup<PropertyLookup>(handle)->getObject(static_cast<int>(propertyId), handle);
            const auto& array = value.getArray();
            auto &f = array[index].get<Filter>();
            auto val = f.getValue(kFilterPropertySigma).getDouble();
            return static_cast<jfloat>(val);
        }

        JNIEXPORT jlong JNICALL
        Java_com_amazon_apl_android_primitive_Filters_nGetColorAt(JNIEnv *env, jclass clazz,
                                                                   jlong handle, jint propertyId, jint index) {

            auto value = getLookup<PropertyLookup>(handle)->getObject(static_cast<int>(propertyId), handle);
            const auto& array = value.getArray();
            auto &f = array[index].get<Filter>();
            apl::Color color = f.getValue(kFilterPropertyColor).asColor();
            return static_cast<jlong>(color.get());
        }

        JNIEXPORT jboolean JNICALL
        Java_com_amazon_apl_android_primitive_Filters_nHasPropertyAt(JNIEnv *env, jclass clazz, jlong handle,
                                                                     jint propertyId, jint filterPropertyId, jint index) {

            auto value = getLookup<PropertyLookup>(handle)->getObject(static_cast<int>(propertyId), handle);
            const auto& array = value.getArray();
            auto &f = array[index].get<Filter>();
            const auto &g = f.getValue(static_cast<FilterProperty>(filterPropertyId));
            return static_cast<jboolean>(!g.isNull());
        }

        JNIEXPORT jint JNICALL
        Java_com_amazon_apl_android_primitive_Filters_nGetGradientTypeAt(JNIEnv *env, jclass clazz,
                                                                         jlong handle, jint propertyId, jint index) {

            auto value = getLookup<PropertyLookup>(handle)->getObject(static_cast<int>(propertyId), handle);
            const auto& array = value.getArray();
            auto &f = array[index].get<Filter>();
            const auto &g = f.getValue(kFilterPropertyGradient).get<Gradient>();
            return static_cast<jint>(g.getProperty(kGradientPropertyType).asInt());
        }

        JNIEXPORT jfloat JNICALL
        Java_com_amazon_apl_android_primitive_Filters_nGetGradientFloatAt(JNIEnv *env, jclass clazz,
                                                                         jlong handle, jint propertyId, jint gradientPropertyId, jint index) {
            auto value = getLookup<PropertyLookup>(handle)->getObject(static_cast<int>(propertyId), handle);
            const auto& array = value.getArray();
            auto &f = array[index].get<Filter>();
            const auto &g = f.getValue(kFilterPropertyGradient).get<Gradient>();
            return static_cast<jfloat>(g.getProperty(static_cast<GradientProperty>(gradientPropertyId)).asNumber());
        }

        JNIEXPORT jfloatArray JNICALL
        Java_com_amazon_apl_android_primitive_Filters_nGetGradientInputRangeAt(JNIEnv *env, jclass clazz,
                                                                               jlong handle,
                                                                               jint propertyId,
                                                                               jint index) {

            auto value = getLookup<PropertyLookup>(handle)->getObject(static_cast<int>(propertyId), handle);
            const auto& array = value.getArray();
            auto &f = array[index].get<Filter>();
            const auto &g = f.getValue(kFilterPropertyGradient).get<Gradient>();

            apl::Object inputRangeObject = g.getProperty(kGradientPropertyInputRange);
            int inputCount = inputRangeObject.getArray().size();
            jfloat range[inputCount];
            for (int i = 0; i < inputCount; i++) {
                range[i] = static_cast<float>(inputRangeObject.getArray()[i].asNumber());
            }
            jfloatArray inputRange = env->NewFloatArray(inputCount);
            env->SetFloatArrayRegion(inputRange, 0, inputCount, range);

            return inputRange;
        }


        JNIEXPORT jlongArray JNICALL
        Java_com_amazon_apl_android_primitive_Filters_nGetGradientColorRangeAt(JNIEnv *env, jclass clazz,
                                                                               jlong handle,
                                                                               jint propertyId,
                                                                               jint index) {
            auto value = getLookup<PropertyLookup>(handle)->getObject(static_cast<int>(propertyId), handle);
            const auto& array = value.getArray();
            auto &f = array[index].get<Filter>();
            const auto &g = f.getValue(kFilterPropertyGradient).get<Gradient>();

            apl::Object colorRangeObject = g.getProperty(kGradientPropertyColorRange);
            int colorCount = colorRangeObject.getArray().size();
            jlong colors[colorCount];
            for (int i = 0; i < colorCount; i++) {
                apl::Color color = colorRangeObject.getArray()[i].asColor();
                colors[i] = static_cast<long>(color.get());
            }
            jlongArray colorRange = env->NewLongArray(colorCount);
            env->SetLongArrayRegion(colorRange, 0, colorCount, colors);

            return colorRange;
        }

        JNIEXPORT jboolean JNICALL
        Java_com_amazon_apl_android_primitive_Filters_nGetBooleanAt(JNIEnv *env, jclass clazz,
                                                                    jlong handle,
                                                                    jint propertyId,
                                                                    jint filterPropertyKey,
                                                                    jint index) {
            auto value = getLookup<PropertyLookup>(handle)->getObject(static_cast<int>(propertyId), handle);
            const auto &array = value.getArray();
            auto &f = array[index].get<Filter>();
            auto val = f.getValue(static_cast<FilterProperty>(filterPropertyKey)).asBoolean();
            return static_cast<jboolean>(val);
        }

        JNIEXPORT jstring JNICALL
        Java_com_amazon_apl_android_primitive_Filters_nGetStringAt(JNIEnv *env, jclass clazz,
                                                                   jlong handle,
                                                                   jint propertyId,
                                                                   jint filterPropertyKey,
                                                                   jint index) {
            auto value = getLookup<PropertyLookup>(handle)->getObject(static_cast<int>(propertyId), handle);
            const auto &array = value.getArray();
            auto &f = array[index].get<Filter>();
            auto val = f.getValue(static_cast<FilterProperty>(filterPropertyKey)).asString();
            return env->NewStringUTF(val.c_str());
        }

        static jobject getJObjectPropertyAt(JNIEnv *env, jclass clazz,
                                            jlong handle,
                                            jint propertyId,
                                            jint filterPropertyKey,
                                            jint index) {
            auto value = getLookup<PropertyLookup>(handle)->getObject(static_cast<int>(propertyId), handle);
            const auto &array = value.getArray();
            auto &f = array[index].get<Filter>();
            auto val = f.getValue(static_cast<FilterProperty>(filterPropertyKey));
            return getJObject(env, val);
        }

        JNIEXPORT jobject JNICALL
        Java_com_amazon_apl_android_primitive_Filters_nGetMapAt(JNIEnv *env, jclass clazz,
                                                                jlong handle,
                                                                jint propertyId,
                                                                jint filterPropertyKey,
                                                                jint index) {
            return getJObjectPropertyAt(env, clazz, handle, propertyId, filterPropertyKey, index);
        }

        JNIEXPORT jobject JNICALL
        Java_com_amazon_apl_android_primitive_Filters_nGetObjectAt(JNIEnv *env, jclass clazz,
                                                                   jlong handle,
                                                                   jint propertyId,
                                                                   jint filterPropertyKey,
                                                                   jint index) {
            return getJObjectPropertyAt(env, clazz, handle, propertyId, filterPropertyKey, index);
        }


        JNIEXPORT jfloat JNICALL
        Java_com_amazon_apl_android_primitive_Filters_nGetFloatAt(JNIEnv *env, jclass clazz,
                                                                  jlong handle,
                                                                  jint propertyId,
                                                                  jint filterPropertyKey,
                                                                  jint index) {

            auto value = getLookup<PropertyLookup>(handle)->getObject(static_cast<int>(propertyId), handle);
            const auto &array = value.getArray();
            auto &f = array[index].get<Filter>();
            auto val = f.getValue(static_cast<FilterProperty>(filterPropertyKey)).asNumber();
            return static_cast<jfloat>(val);
        }

        JNIEXPORT jint JNICALL
        Java_com_amazon_apl_android_primitive_Filters_nGetIntAt(JNIEnv *env, jclass clazz, jlong handle,
                                                                  jint propertyId, jint filterPropertyKey,
                                                                  jint index) {

            auto value = getLookup<PropertyLookup>(handle)->getObject(static_cast<int>(propertyId), handle);
            const auto& array = value.getArray();
            auto &f = array[index].get<Filter>();
            auto val = f.getValue(static_cast<FilterProperty>(filterPropertyKey)).asInt();
            return static_cast<jint>(val);
        }

        JNIEXPORT jint JNICALL
        Java_com_amazon_apl_android_primitive_BoundMediaSources_nGetMediaSourceDurationAt(JNIEnv *env, jclass clazz,
                                                                                     jlong handle,
                                                                                     jint propertyId,
                                                                                     jint index) {
            auto value = getLookup<PropertyLookup>(handle)->getObject(static_cast<int>(propertyId), handle);
            const auto& array = value.getArray();
            auto source = array[index].get<MediaSource>();
            return static_cast<jint>(source.getDuration());
        }

        JNIEXPORT jint JNICALL
        Java_com_amazon_apl_android_primitive_BoundMediaSources_nGetMediaSourceOffsetAt(JNIEnv *env, jclass clazz,
                                                                                   jlong handle,
                                                                                   jint propertyId,
                                                                                   jint index) {
            auto value = getLookup<PropertyLookup>(handle)->getObject(static_cast<int>(propertyId), handle);
            const auto& array = value.getArray();
            auto source = array[index].get<MediaSource>();
            return static_cast<jint>(source.getOffset());
        }

        JNIEXPORT jint JNICALL
        Java_com_amazon_apl_android_primitive_BoundMediaSources_nGetMediaSourceRepeatCountAt(JNIEnv *env, jclass clazz,
                                                                                        jlong handle,
                                                                                        jint propertyId,
                                                                                        jint index) {
            auto value = getLookup<PropertyLookup>(handle)->getObject(static_cast<int>(propertyId), handle);
            const auto& array = value.getArray();
            auto source = array[index].get<MediaSource>();
            return static_cast<jint>(source.getRepeatCount());
        }

        JNIEXPORT jstring JNICALL
        Java_com_amazon_apl_android_primitive_BoundMediaSources_nGetMediaSourceUrlAt(JNIEnv *env, jclass clazz,
                                                                                jlong handle,
                                                                                jint propertyId,
                                                                                jint index) {
            auto value = getLookup<PropertyLookup>(handle)->getObject(static_cast<int>(propertyId), handle);
            const auto& array = value.getArray();
            auto source = array[index].get<MediaSource>();
            return env->NewStringUTF(source.getUrl().c_str());
        }

        jobjectArray getStringArray(JNIEnv *env, std::vector<std::basic_string<char, std::char_traits<char>, std::allocator<char>>> array) {
            jobjectArray stringArray = env->NewObjectArray(array.size(), JAVA_LANG_STRING, nullptr);
            if (stringArray == NULL) {
                return NULL; /* out of memory error thrown */
            }
            for (int i = 0; i < array.size(); ++i) {
                jobject object = getJObject(env, array[i]);
                if (env->IsInstanceOf(object, JAVA_LANG_STRING)) {
                    env->SetObjectArrayElement(stringArray, i, object);
                }
                env->DeleteLocalRef(object);
            }
            return stringArray;
        }

        JNIEXPORT jobjectArray JNICALL
        Java_com_amazon_apl_android_primitive_BoundMediaSources_nGetMediaSourceHeadersAt(JNIEnv *env, jclass clazz,
                                                                                jlong handle,
                                                                                jint propertyId,
                                                                                jint index) {
            auto value = getLookup<PropertyLookup>(handle)->getObject(static_cast<int>(propertyId), handle);
            const auto& array = value.getArray();
            const auto& source = array[index].get<MediaSource>();
            const auto& headers = source.getHeaders();
            return getStringArray(env, headers);
        }

        JNIEXPORT jint JNICALL
        Java_com_amazon_apl_android_primitive_UrlRequestGetter_nSize(JNIEnv *env, jclass clazz,
                                                                     jlong handle,
                                                                     jint propertyId) {
            const auto& value = getLookup<PropertyLookup>(handle)->getObject(static_cast<int>(propertyId), handle);
            if (!value.isArray()) return 1;
            const auto& array = value.getArray();
            return static_cast<jint>(array.size());
        }

        JNIEXPORT jobjectArray JNICALL
        Java_com_amazon_apl_android_primitive_UrlRequestGetter_nGetUrlRequestHeadersAt(JNIEnv *env, jclass clazz,
                                                                                    jlong handle,
                                                                                    jint propertyId,
                                                                                    jint index) {
            const auto& value = getLookup<PropertyLookup>(handle)->getObject(static_cast<int>(propertyId), handle);
            if (value.isArray()) {
                const auto& array = value.getArray();
                const auto& source = URLRequest::asURLRequest(array[index]);
                const auto& headers = source.getHeaders();
                return getStringArray(env, headers);
            } else {
                const auto& source = URLRequest::asURLRequest(value);
                const auto& headers = source.getHeaders();
                return getStringArray(env, headers);
            }
        }

        JNIEXPORT jstring JNICALL
        Java_com_amazon_apl_android_primitive_UrlRequestGetter_nGetUrlRequestSourceAt(JNIEnv *env, jclass clazz,
                                                                                    jlong handle,
                                                                                    jint propertyId,
                                                                                    jint index) {
            const auto& value = getLookup<PropertyLookup>(handle)->getObject(static_cast<int>(propertyId), handle);
            if (value.isArray()) {
                const auto& array = value.getArray();
                const auto& urlRequest = URLRequest::asURLRequest(array[index]);
                return env->NewStringUTF(urlRequest.getUrl().c_str());
            } else {
                const auto& urlRequest = URLRequest::asURLRequest(value);
                return env->NewStringUTF(urlRequest.getUrl().c_str());
            }
        }

        JNIEXPORT jint JNICALL
        Java_com_amazon_apl_android_primitive_ArrayGetter_nSize(JNIEnv *env, jclass clazz,
                                                                     jlong handle,
                                                                     jint propertyId) {
            auto value = getLookup<PropertyLookup>(handle)->getObject(static_cast<int>(propertyId), handle);
            const auto& array = value.getArray();
            return static_cast<jint>(array.size());
        }

        JNIEXPORT jstring JNICALL
        Java_com_amazon_apl_android_primitive_AccessibilityActions_nGetAccessibilityActionNameAt(JNIEnv *env, jclass clazz,
                                                                                jlong handle,
                                                                                jint propertyId,
                                                                                jint index) {
            auto value = getLookup<PropertyLookup>(handle)->getObject(static_cast<int>(propertyId), handle);
            const auto& array = value.getArray();
            auto action = array[index].get<AccessibilityAction>();
            return env->NewStringUTF(action->getName().c_str());
        }

        JNIEXPORT jstring JNICALL
        Java_com_amazon_apl_android_primitive_AccessibilityActions_nGetAccessibilityActionLabelAt(JNIEnv *env, jclass clazz,
                                                                                                 jlong handle,
                                                                                                 jint propertyId,
                                                                                                 jint index) {
            auto value = getLookup<PropertyLookup>(handle)->getObject(static_cast<int>(propertyId), handle);
            const auto& array = value.getArray();
            auto action = array[index].get<AccessibilityAction>();
            return env->NewStringUTF(action->getLabel().c_str());
        }

        JNIEXPORT jint JNICALL
        Java_com_amazon_apl_android_primitive_GraphicFilters_nGetGraphicFilterTypeAt(JNIEnv *env, jclass clazz,
                                                                                     jlong handle, jint propertyId, jint index) {

            auto value = getLookup<PropertyLookup>(handle)->getObject(static_cast<int>(propertyId), handle);
            const auto &array = value.getArray();
            auto &f = array[index].get<GraphicFilter>();

            return static_cast<jint>(f.getType());
        }

        JNIEXPORT jlong JNICALL
        Java_com_amazon_apl_android_primitive_GraphicFilters_nGetColorAt(JNIEnv *env, jclass clazz,
                                                                         jlong handle, jint propertyId, jint index) {
            auto lookup = getLookup<PropertyLookup>(handle);
            auto value = lookup->getObject(static_cast<int>(propertyId), handle);
            const auto& array = value.getArray();
            auto &f = array[index].get<GraphicFilter>();
            auto val = f.getValue(kGraphicPropertyFilterColor).getColor();

            return static_cast<jlong>(val);
        }

        JNIEXPORT jfloat JNICALL
        Java_com_amazon_apl_android_primitive_GraphicFilters_nGetFloatAt(JNIEnv *env, jclass clazz,
                                                                          jlong handle, jint propertyId, jint graphicFilterPropertykey, jint index) {
            auto lookup = getLookup<PropertyLookup>(handle);
            auto value = lookup->getObject(static_cast<int>(propertyId), handle);
            const auto& array = value.getArray();
            auto &f = array[index].get<GraphicFilter>();
            auto val = f.getValue(static_cast<GraphicFilterProperty>(graphicFilterPropertykey)).asNumber();
            return static_cast<jfloat>(val);
        }

#pragma clang diagnostic pop

#ifdef __cplusplus
        }
#endif

    } //namespace jni
} //namespace apl
