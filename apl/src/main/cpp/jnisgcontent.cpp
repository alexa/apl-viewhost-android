/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

#include <jni.h>

#include "apl/apl.h"
#include <codecvt>
#include <jnisgcontent.h>
#include <jniedittext.h>
#include <jnitextlayout.h>
#include <jnimediaplayer.h>
#include <jniapllayer.h>
#include "jniutil.h"

#ifdef SCENEGRAPH
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
        sgcontent_OnLoad(JavaVM *vm, void *reserved) {

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
        sgcontent_OnUnload(JavaVM *vm, void *reserved) {
            LOG(apl::LogLevel::kDebug) << "Unloading View Host Component JNI environment.";
            apl::LoggerFactory::instance().reset();

            JNIEnv *env;
            if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
                // environment failure, can't proceed.
                return;
            }
        }

        JNIEXPORT jobject JNICALL
        Java_com_amazon_apl_android_sgcontent_Node_nGetNodeObject(JNIEnv *env, jclass clazz,
                                                                  jlong nativeHandle) {
            auto node = reinterpret_cast<apl::sg::Node *>(nativeHandle);
            auto *aplLayer = node->getUserData<APLLayer>();
            if (aplLayer) {
                return aplLayer->getInstance();
            }
            return nullptr;
        }

        JNIEXPORT void JNICALL
        Java_com_amazon_apl_android_sgcontent_Node_nSetNodeObject(JNIEnv *env, jobject nodeObject,
                                                                  jlong nativeHandle) {
            auto node = reinterpret_cast<apl::sg::Node *>(nativeHandle);

            APLLayer *aplLayer = new APLLayer();
            auto layerRef = env->NewGlobalRef(nodeObject);
            aplLayer->setInstance(layerRef);
            node->setUserDataReleaseCallback([&](void *ptr) {
                APLLayer *layer = static_cast<APLLayer*>(ptr);
                delete layer;
            });
            node->setUserData(aplLayer);
        }

        JNIEXPORT jstring JNICALL
        Java_com_amazon_apl_android_sgcontent_Node_nGetType(JNIEnv *env,
                                                                          jclass clazz,
                                                                          jlong address) {
            apl::sg::Node * node = reinterpret_cast<apl::sg::Node *>(address);

            char* nodeType = "Unknown";
            switch (node->type()) {
                case sg::Node::Type::kDraw:
                    nodeType = "Draw";
                    break;
                case sg::Node::Type::kClip:
                    nodeType = "Clip";
                    break;
                case sg::Node::Type::kTransform:
                    nodeType = "Transform";
                    break;
                case sg::Node::Type::kOpacity:
                    nodeType = "Opacity";
                    break;
                case sg::Node::Type::kShadow:
                    nodeType = "Shadow";
                    break;
                case sg::Node::Type::kEditText:
                    nodeType = "EditText";
                    break;
                case sg::Node::Type::kText:
                    nodeType = "Text";
                    break;
                case sg::Node::Type::kImage:
                    nodeType = "Image";
                    break;
                case sg::Node::Type::kVideo:
                    nodeType = "Video";
                    break;
            }

            return env->NewStringUTF(nodeType);
        }

        JNIEXPORT jboolean JNICALL
        Java_com_amazon_apl_android_sgcontent_Node_nIsVisible(JNIEnv *env,
                                                            jclass clazz,
                                                            jlong address) {
            apl::sg::Node* node = reinterpret_cast<apl::sg::Node *>(address);
            return node->visible();
        }

        JNIEXPORT jlongArray JNICALL
        Java_com_amazon_apl_android_sgcontent_Node_nGetChildren(JNIEnv *env,
                                                                          jclass clazz,
                                                                          jlong address) {
            apl::sg::Node * node = reinterpret_cast<apl::sg::Node *>(address);
            jlong children[node->childCount()];
            int i = 0;
            auto child = node->child();
            while(child != nullptr && i < node->childCount()) {
                jlong tmp = reinterpret_cast<jlong>(child.get());
                children[i] = tmp;
                i++;
                child = child->next();
            }

            jlongArray result = env->NewLongArray(node->childCount());
            env->SetLongArrayRegion(result, 0, node->childCount(), children);
            return result;
        }

        JNIEXPORT jlong JNICALL
        Java_com_amazon_apl_android_sgcontent_Node_nNext(JNIEnv *env,
                                                          jclass clazz,
                                                          jlong address) {
            apl::sg::Node * node = reinterpret_cast<apl::sg::Node *>(address);
            auto nextNode = node->next().get();
            if (nextNode != nullptr) {
                return reinterpret_cast<jlong>(nextNode);
            } else {
                return 0;
            }
        }

        JNIEXPORT jlong JNICALL
        Java_com_amazon_apl_android_sgcontent_Node_nGetOp(JNIEnv *env,
                                                                      jclass clazz,
                                                                      jlong address) {
            apl::sg::DrawNode * node = reinterpret_cast<apl::sg::DrawNode *>(address);
            return reinterpret_cast<jlong>(node->getOp().get());
        }

        JNIEXPORT jfloat JNICALL
        Java_com_amazon_apl_android_sgcontent_Node_nGetOpacity(JNIEnv *env,
                                                               jclass clazz,
                                                               jlong address) {
            apl::sg::OpacityNode * node = reinterpret_cast<apl::sg::OpacityNode *>(address);
            return node->getOpacity();
        }

        JNIEXPORT jfloatArray JNICALL
        Java_com_amazon_apl_android_sgcontent_Node_nGetTransform(JNIEnv *env,
                                                                           jclass clazz,
                                                                           jlong coreLayerHandle) {
            auto node = reinterpret_cast<apl::sg::TransformNode *>(coreLayerHandle);
            auto transform = node->getTransform();
            float transformArray[6] = {0};
            auto array = transform.get();
            int i = 0;
            for (const auto& record : array) {
                transformArray[i] = record;
                i++;
            }
            float buffer[6] = {transformArray[0], transformArray[1],
                               transformArray[2], transformArray[3],
                               transformArray[4], transformArray[5],};
            jfloatArray jTransform = env->NewFloatArray(6);
            env->SetFloatArrayRegion(jTransform, 0, 6, buffer);
            return jTransform;
        }

        JNIEXPORT jobject JNICALL
        Java_com_amazon_apl_android_sgcontent_Node_nGetTextLayout(JNIEnv *env,
                                                                       jclass clazz,
                                                                       jlong coreNodeHandle) {
            auto node = reinterpret_cast<apl::sg::Node *>(coreNodeHandle);
            const auto *textNode = apl::sg::TextNode::cast(node);
            const auto *textLayout = textNode->getTextLayout().get();
            if (textLayout) {
                auto *aplTextLayout = static_cast<const APLTextLayout *>(textLayout);
                return aplTextLayout->getTextLayout();
            }
            return nullptr;
        }

        JNIEXPORT jobject JNICALL
        Java_com_amazon_apl_android_sgcontent_EditTextNode_nGetEditText(JNIEnv *env,
                                                                        jclass clazz,
                                                                        jlong coreNodeHandle) {
            auto node = reinterpret_cast<apl::sg::Node *>(coreNodeHandle);
            const auto *editTextNode = apl::sg::EditTextNode::cast(node);
            const auto *editText = editTextNode->getEditText().get();
            if (editText) {
                auto *aplEditText = static_cast<const APLEditText *>(editText);
                return aplEditText->getInstance();
            }
            return nullptr;
        }

        JNIEXPORT jobject JNICALL
        Java_com_amazon_apl_android_sgcontent_VideoNode_nGetMediaPlayer(JNIEnv *env,
                                                                               jclass clazz,
                                                                               jlong coreNodeHandle) {
            auto node = reinterpret_cast<apl::sg::Node *>(coreNodeHandle);
            const auto *videoNode = apl::sg::VideoNode::cast(node);
            const auto *mediaPlayer = videoNode->getMediaPlayer().get();
            if (mediaPlayer) {
                auto *aplMediaPlayer = static_cast<const AndroidMediaPlayer *>(mediaPlayer);
                return aplMediaPlayer->getInstance();
            }
            return nullptr;
        }

        JNIEXPORT jint JNICALL
        Java_com_amazon_apl_android_sgcontent_VideoNode_nGetVideoScale(JNIEnv *env,
                                                                       jclass clazz,
                                                                       jlong coreNodeHandle) {
            auto node = reinterpret_cast<apl::sg::Node *>(coreNodeHandle);
            const auto *videoNode = apl::sg::VideoNode::cast(node);
            auto videoScale = videoNode->getScale();
            return static_cast<jint>(videoScale);
        }

        JNIEXPORT jlong JNICALL
        Java_com_amazon_apl_android_sgcontent_EditTextNode_nGetTextConfig(JNIEnv *env,
                                                                          jclass clazz,
                                                                          jlong coreNodeHandle) {
            auto node = reinterpret_cast<apl::sg::Node *>(coreNodeHandle);
            const auto *editTextNode = apl::sg::EditTextNode::cast(node);
            return reinterpret_cast<jlong>(editTextNode->getEditTextConfig().get());
        }

        JNIEXPORT jstring JNICALL
        Java_com_amazon_apl_android_sgcontent_EditTextNode_nGetText(JNIEnv *env,
                                                                    jclass clazz,
                                                                    jlong coreNodeHandle) {
            auto node = reinterpret_cast<apl::sg::Node *>(coreNodeHandle);
            const auto *editTextNode = apl::sg::EditTextNode::cast(node);
            std::wstring_convert<std::codecvt_utf8_utf16<char16_t>, char16_t> converter;
            std::u16string u16str = converter.from_bytes(editTextNode->getText());
            return env->NewString((const jchar*)u16str.data(), u16str.length());
        }

        JNIEXPORT jfloatArray JNICALL
        Java_com_amazon_apl_android_sgcontent_EditTextNode_nGetSize(JNIEnv *env,
                                                                    jclass clazz,
                                                                    jlong coreNodeHandle) {
            auto node = reinterpret_cast<apl::sg::Node *>(coreNodeHandle);
            const auto *editTextNode = apl::sg::EditTextNode::cast(node);
            auto editTextBox = editTextNode->getEditTextBox();
            auto size = editTextBox->getSize();
            float buffer[2] = { size.getWidth(),
                                size.getHeight() };

            jfloatArray sizeArray = env->NewFloatArray(2);
            env->SetFloatArrayRegion(sizeArray, 0, 2, buffer);
            return sizeArray;
        }

        JNIEXPORT jlong JNICALL
        Java_com_amazon_apl_android_sgcontent_Node_nGetPath(JNIEnv *env,
                                                          jclass clazz,
                                                          jlong address) {
            apl::sg::DrawNode * node = reinterpret_cast<apl::sg::DrawNode *>(address);
            return reinterpret_cast<jlong>(node->getPath().get());
        }

        JNIEXPORT jlong JNICALL
        Java_com_amazon_apl_android_sgcontent_Node_nGetClipPath(JNIEnv *env,
                                                            jclass clazz,
                                                            jlong address) {
            apl::sg::ClipNode * node = reinterpret_cast<apl::sg::ClipNode *>(address);
            return reinterpret_cast<jlong>(node->getPath().get());
        }

        JNIEXPORT jint JNICALL
        Java_com_amazon_apl_android_sgcontent_Node_nShadowGetColor(JNIEnv *env,
                                                            jclass clazz,
                                                            jlong address) {
            apl::sg::ShadowNode * node = reinterpret_cast<apl::sg::ShadowNode *>(address);
            return node->getShadow()->getColor().get();
        }

        JNIEXPORT jfloatArray JNICALL
        Java_com_amazon_apl_android_sgcontent_Node_nShadowGetOffset(JNIEnv *env,
                                                                   jclass clazz,
                                                                   jlong address) {
            apl::sg::ShadowNode * node = reinterpret_cast<apl::sg::ShadowNode *>(address);
            auto point =  node->getShadow()->getOffset();
            float pointArray[] = {point.getX(), point.getY()};
            jfloatArray result = env->NewFloatArray(2);
            env->SetFloatArrayRegion(result,0,2, pointArray);
            return result;
        }

        JNIEXPORT jfloat JNICALL
        Java_com_amazon_apl_android_sgcontent_Node_nShadowGetRadius(JNIEnv *env,
                                                                    jclass clazz,
                                                                    jlong address) {
            apl::sg::ShadowNode * node = reinterpret_cast<apl::sg::ShadowNode *>(address);
            return node->getShadow()->getRadius();
        }

        JNIEXPORT jlong JNICALL
        Java_com_amazon_apl_android_sgcontent_Node_nImageGetFilter(JNIEnv *env,
                                                                    jclass clazz,
                                                                    jlong address) {
            apl::sg::ImageNode * node = reinterpret_cast<apl::sg::ImageNode *>(address);
            return reinterpret_cast<jlong>(node->getImage().get());
        }

        jfloatArray getRect(JNIEnv *env, Rect rect) {
            float rectArray[] = {rect.getLeft(), rect.getTop(), rect.getWidth(), rect.getHeight()};
            jfloatArray result = env->NewFloatArray(4);
            env->SetFloatArrayRegion(result,0,4, rectArray);
            return result;
        }

        JNIEXPORT jfloatArray JNICALL
        Java_com_amazon_apl_android_sgcontent_Node_nImageGetSourceRect(JNIEnv *env,
                                                                   jclass clazz,
                                                                   jlong address) {
            apl::sg::ImageNode * node = reinterpret_cast<apl::sg::ImageNode *>(address);
            return getRect(env, node->getSource());
        }

        JNIEXPORT jfloatArray JNICALL
        Java_com_amazon_apl_android_sgcontent_Node_nImageGetTargetRect(JNIEnv *env,
                                                                       jclass clazz,
                                                                       jlong address) {
            apl::sg::ImageNode * node = reinterpret_cast<apl::sg::ImageNode *>(address);
            return getRect(env, node->getTarget());
        }

        JNIEXPORT jstring JNICALL
        Java_com_amazon_apl_android_sgcontent_PathOp_nGetType(JNIEnv *env,
                                                            jclass clazz,
                                                            jlong address) {
            apl::sg::PathOp * pathOp = reinterpret_cast<apl::sg::PathOp *>(address);
            char* pathType = "Unknown";
            switch (pathOp->type) {
                case sg::PathOp::kFill:
                    pathType = "Fill";
                    break;
                case sg::PathOp::kStroke:
                    pathType = "Stroke";
                    break;
            }
            return env->NewStringUTF(pathType);
        }

        JNIEXPORT jstring JNICALL
        Java_com_amazon_apl_android_sgcontent_Path_nGetType(JNIEnv *env,
                                                              jclass clazz,
                                                              jlong address) {
            apl::sg::Path * path = reinterpret_cast<apl::sg::Path *>(address);
            char* pathType = "Unknown";
            switch (path->type()) {
                case sg::Path::Type::kGeneral:
                    pathType = "General";
                    break;
                case sg::Path::Type::kFrame:
                    pathType = "Frame";
                    break;
                case sg::Path::Type::kRect:
                    pathType = "Rect";
                    break;
                case sg::Path::Type::kRoundedRect:
                    pathType = "RRect";
                    break;
            }
            return env->NewStringUTF(pathType);
        }

        JNIEXPORT jstring JNICALL
        Java_com_amazon_apl_android_sgcontent_Path_nGetValue(JNIEnv *env,
                                                            jclass clazz,
                                                            jlong address) {
            apl::sg::GeneralPath * path = reinterpret_cast<apl::sg::GeneralPath *>(address);
            return env->NewStringUTF(path->getValue().c_str());
        }

        JNIEXPORT jfloatArray JNICALL
        Java_com_amazon_apl_android_sgcontent_Path_nGetPoints(JNIEnv *env,
                                                             jclass clazz,
                                                             jlong address) {
            apl::sg::GeneralPath * path = reinterpret_cast<apl::sg::GeneralPath *>(address);
            auto points = path->getPoints();
            jfloat p[points.size()];
            int i = 0;
            for (auto itr = points.begin(); itr < points.end(); itr++) {
                p[i] = *itr;
                i++;
            }
            jfloatArray result = env->NewFloatArray(points.size());
            env->SetFloatArrayRegion(result, 0, points.size(), p);
            return result;
        }

        JNIEXPORT jfloatArray JNICALL
        Java_com_amazon_apl_android_sgcontent_Path_nRectPathGetRect(JNIEnv *env,
                                                             jclass clazz,
                                                             jlong address) {
            apl::sg::RectPath * path = reinterpret_cast<apl::sg::RectPath *>(address);
            auto rect = path->getRect();
            jfloat r[4] = {rect.getLeft(), rect.getTop(), rect.getRight(), rect.getBottom()};
            jfloatArray result = env->NewFloatArray(4);
            env->SetFloatArrayRegion(result, 0, 4, r);
            return result;
        }

        JNIEXPORT jfloatArray JNICALL
        Java_com_amazon_apl_android_sgcontent_Path_nFramePathGetRRect(JNIEnv *env,
                                                                    jclass clazz,
                                                                    jlong address) {
            apl::sg::FramePath * path = reinterpret_cast<apl::sg::FramePath *>(address);
            auto rrect = path->getRoundedRect();
            auto rect = rrect.rect();
            auto radii = rrect.radii().get();
            jfloat r[8] = {rect.getLeft(), rect.getTop(), rect.getRight(), rect.getBottom(), radii[0], radii[1], radii[3], radii[2]};
            jfloatArray result = env->NewFloatArray(8);
            env->SetFloatArrayRegion(result, 0, 8, r);
            return result;
        }

        JNIEXPORT jfloatArray JNICALL
        Java_com_amazon_apl_android_sgcontent_Path_nFramePathGetInset(JNIEnv *env,
                                                                      jclass clazz,
                                                                      jlong address) {
            apl::sg::FramePath * path = reinterpret_cast<apl::sg::FramePath *>(address);
            auto rrect = path->getRoundedRect();
            rrect = rrect.inset(path->getInset());
            auto rect = rrect.rect();
            auto radii = rrect.radii().get();
            //top-left, top-right, bottom-left, bottom-right.
            jfloat r[8] = {rect.getLeft(), rect.getTop(), rect.getRight(), rect.getBottom(), radii[0], radii[1], radii[3], radii[2]};
            jfloatArray result = env->NewFloatArray(8);
            env->SetFloatArrayRegion(result, 0, 8, r);
            return result;
        }

        JNIEXPORT jfloatArray JNICALL
        Java_com_amazon_apl_android_sgcontent_Path_nRRectPathGetRRect(JNIEnv *env,
                                                                      jclass clazz,
                                                                      jlong address) {
            apl::sg::RoundedRectPath * path = reinterpret_cast<apl::sg::RoundedRectPath *>(address);
            auto rrect = path->getRoundedRect();
            auto rect = rrect.rect();
            auto radii = rrect.radii().get();
            jfloat r[8] = {rect.getLeft(), rect.getTop(), rect.getRight(), rect.getBottom(), radii[0], radii[1], radii[3], radii[2]};
            jfloatArray result = env->NewFloatArray(8);
            env->SetFloatArrayRegion(result, 0, 8, r);
            return result;
        }

        JNIEXPORT jlong JNICALL
        Java_com_amazon_apl_android_sgcontent_PathOp_nGetNextSibbling(JNIEnv *env,
                                                              jclass clazz,
                                                              jlong address) {
            apl::sg::PathOp * pathOp = reinterpret_cast<apl::sg::PathOp *>(address);
            return reinterpret_cast<jlong>(pathOp->nextSibling.get());
        }

        JNIEXPORT jfloat JNICALL
        Java_com_amazon_apl_android_sgcontent_PathOp_nGetStokeWidth(JNIEnv *env,
                                                                      jclass clazz,
                                                                      jlong address) {
            apl::sg::StrokePathOp * pathOp = reinterpret_cast<apl::sg::StrokePathOp *>(address);
            return static_cast<jfloat>(pathOp->strokeWidth);
        }

        JNIEXPORT jfloat JNICALL
        Java_com_amazon_apl_android_sgcontent_PathOp_nGetMiterLimit(JNIEnv *env,
                                                                    jclass clazz,
                                                                    jlong address) {
            apl::sg::StrokePathOp * pathOp = reinterpret_cast<apl::sg::StrokePathOp *>(address);
            return static_cast<jfloat>(pathOp->miterLimit);
        }

        JNIEXPORT jfloat JNICALL
        Java_com_amazon_apl_android_sgcontent_PathOp_nGetPathLength(JNIEnv *env,
                                                                    jclass clazz,
                                                                    jlong address) {
            apl::sg::StrokePathOp * pathOp = reinterpret_cast<apl::sg::StrokePathOp *>(address);
            return static_cast<jfloat>(pathOp->pathLength);
        }

        JNIEXPORT jfloat JNICALL
        Java_com_amazon_apl_android_sgcontent_PathOp_nGetStrokeDashOffset(JNIEnv *env,
                                                                    jclass clazz,
                                                                    jlong address) {
            apl::sg::StrokePathOp * pathOp = reinterpret_cast<apl::sg::StrokePathOp *>(address);
            return static_cast<jfloat>(pathOp->dashOffset);
        }

        JNIEXPORT jfloatArray JNICALL
        Java_com_amazon_apl_android_sgcontent_PathOp_nGetStrokeDashArray(JNIEnv *env,
                                                                          jclass clazz,
                                                                          jlong address) {
            apl::sg::StrokePathOp * pathOp = reinterpret_cast<apl::sg::StrokePathOp *>(address);

            std::vector<float> offsetsToCopy = pathOp->dashes;
            jfloat offsets[offsetsToCopy.size()];
            int i = 0;
            for (auto iter = offsetsToCopy.begin(); iter != offsetsToCopy.end(); iter++) {
               offsets[i] = offsetsToCopy[i];
               i++;
            }

            jfloatArray result = env->NewFloatArray(offsetsToCopy.size());
            env->SetFloatArrayRegion(result, 0, offsetsToCopy.size(), offsets);
            return result;
        }

        JNIEXPORT jint JNICALL
        Java_com_amazon_apl_android_sgcontent_PathOp_nGetLineCap(JNIEnv *env,
                                                                          jclass clazz,
                                                                          jlong address) {
            apl::sg::StrokePathOp * pathOp = reinterpret_cast<apl::sg::StrokePathOp *>(address);
            return static_cast<jint>(pathOp->lineCap);
        }

        JNIEXPORT jint JNICALL
        Java_com_amazon_apl_android_sgcontent_PathOp_nGetLineJoin(JNIEnv *env,
                                                                 jclass clazz,
                                                                 jlong address) {
            apl::sg::StrokePathOp * pathOp = reinterpret_cast<apl::sg::StrokePathOp *>(address);
            return static_cast<jint>(pathOp->lineJoin);
        }

        JNIEXPORT jint JNICALL
        Java_com_amazon_apl_android_sgcontent_PathOp_nGetFillType(JNIEnv *env,
                                                                  jclass clazz,
                                                                  jlong address) {
            apl::sg::FillPathOp * pathOp = reinterpret_cast<apl::sg::FillPathOp *>(address);
            return static_cast<jint>(pathOp->fillType);
        }

        JNIEXPORT jlong JNICALL
        Java_com_amazon_apl_android_sgcontent_PathOp_nGetPaint(JNIEnv *env,
                                                                      jclass clazz,
                                                                      jlong address) {
            apl::sg::PathOp * pathOp = reinterpret_cast<apl::sg::PathOp *>(address);
            return reinterpret_cast<jlong>(pathOp->paint.get());
        }

#pragma clang diagnostic pop

#ifdef __cplusplus
}
#endif

} //namespace jni
} //namespace apl
#endif