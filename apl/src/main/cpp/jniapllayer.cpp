//
// Created by Das, Sourabh on 2023-01-12.
//

#include <jni.h>
#include <jnitextlayout.h>
#include "jniapllayer.h"
#include "jniutil.h"

#define ENV_CREATE() \
    JNIEnv *env; \
    if (JAVA_VM->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) { \
        LOG(apl::LogLevel::kError) << "Environment failure, cannot proceed"; \
        return; \
    } \
    jobject localRef = env->NewLocalRef(mInstance); \
    if (!localRef) { \
        return; \
}

#define ENV_CLEAR() \
    env->DeleteLocalRef(localRef)

namespace apl {
    namespace jni {
#ifdef __cplusplus
        extern "C" {
#endif

        static JavaVM* APLLAYER_VM_REFERENCE;
        static jclass APLLAYER_CLASS;
        static jmethodID APLLAYER_UPDATE_DIRTY_PROPERTIES;

        /**
         * Create a class and method cache for calls to View Host.
         */
        jboolean
        apllayer_OnLoad(JavaVM *vm, void *reserved) {

            LOG(apl::LogLevel::kDebug) << "Loading View Host Component JNI environment.";

            JNIEnv *env;
            if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
                LOG(apl::LogLevel::kError) << "Environment failure, cannot proceed";
                return JNI_FALSE;
            }

            APLLAYER_VM_REFERENCE = vm;
            APLLAYER_CLASS = reinterpret_cast<jclass>(env->NewGlobalRef(
                    env->FindClass("com/amazon/apl/android/scenegraph/APLLayer")));
            APLLAYER_UPDATE_DIRTY_PROPERTIES = env->GetMethodID(APLLAYER_CLASS, "updateDirtyProperties","(I)V");
            return JNI_TRUE;
        }

        /**
         * Release the class and method cache.
         */
        void
        apllayer_OnUnload(JavaVM *vm, void *reserved) {
            LOG(apl::LogLevel::kDebug) << "Unloading View Host Component JNI environment.";
            apl::LoggerFactory::instance().reset();

            JNIEnv *env;
            if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
                LOG(apl::LogLevel::kError) << "Environment failure, cannot proceed";
                return;
            }

            env->DeleteGlobalRef(APLLAYER_CLASS);
        }

        void APLLayer::updateDirtyProperties(int flags) {
            JNIEnv *env;
            if (APLLAYER_VM_REFERENCE->GetEnv(reinterpret_cast<void **>(&env),
                                              JNI_VERSION_1_6) != JNI_OK) {
                LOG(apl::LogLevel::kError) << "Environment failure, cannot proceed";
                return;
            }


            env->CallVoidMethod(mInstance, APLLAYER_UPDATE_DIRTY_PROPERTIES, flags);
        }

        void APLLayer::release() {
            JNIEnv *env;
            if (APLLAYER_VM_REFERENCE->GetEnv(reinterpret_cast<void **>(&env),
                                             JNI_VERSION_1_6) != JNI_OK) {
                LOG(apl::LogLevel::kError) << "Environment failure, cannot proceed";
                return;
            }

            env->DeleteGlobalRef(mInstance);
        }

        JNIEXPORT jobject JNICALL
        Java_com_amazon_apl_android_scenegraph_APLLayer_nGetAplLayer(JNIEnv *env, jclass clazz,
                                                                     jlong coreLayerHandle) {
            auto coreLayer = reinterpret_cast<apl::sg::Layer *>(coreLayerHandle);
            auto *aplLayer = coreLayer->getUserData<APLLayer>();
            if (aplLayer) {
                return aplLayer->getInstance();
            }
            return nullptr;
        }

        JNIEXPORT void JNICALL
        Java_com_amazon_apl_android_scenegraph_APLLayer_nSetUserData(JNIEnv *env, jobject thiz,
                                                                     jlong coreLayerHandle) {
            auto coreLayer = reinterpret_cast<apl::sg::Layer *>(coreLayerHandle);
            APLLayer *aplLayer = new APLLayer();
            auto layerRef = env->NewGlobalRef(thiz);
            aplLayer->setInstance(layerRef);
            coreLayer->setUserDataReleaseCallback([&](void *ptr) {
                APLLayer *layer = static_cast<APLLayer*>(ptr);
                // This destructs the C++ layer object which calls release cleaning up the Java global reference
                delete layer;
            });
            coreLayer->setUserData(aplLayer);
        }

        JNIEXPORT jboolean JNICALL
        Java_com_amazon_apl_android_scenegraph_APLLayer_nIsFlagTransformChanged(JNIEnv *env,
                                                                                jclass clazz,
                                                                                jlong coreLayerHandle) {
            auto coreLayer = reinterpret_cast<apl::sg::Layer *>(coreLayerHandle);
            return coreLayer->isFlagSet(apl::sg::Layer::kFlagTransformChanged);
        }

        JNIEXPORT jboolean JNICALL
        Java_com_amazon_apl_android_scenegraph_APLLayer_nIsFlagChildrenChanged(JNIEnv *env,
                                                                                jclass clazz,
                                                                                jlong coreLayerHandle) {
            auto coreLayer = reinterpret_cast<apl::sg::Layer *>(coreLayerHandle);
            return coreLayer->isFlagSet(apl::sg::Layer::kFlagChildrenChanged);
        }

        JNIEXPORT jboolean JNICALL
        Java_com_amazon_apl_android_scenegraph_APLLayer_nIsFlagOpacityChanged(JNIEnv *env,
                                                                              jclass clazz,
                                                                              jlong coreLayerHandle) {
            auto coreLayer = reinterpret_cast<apl::sg::Layer *>(coreLayerHandle);
            return coreLayer->isFlagSet(apl::sg::Layer::kFlagOpacityChanged);
        }

        JNIEXPORT jlongArray JNICALL
        Java_com_amazon_apl_android_scenegraph_APLLayer_nGetChildren(JNIEnv *env, jobject thiz,
                                                                     jlong coreLayerHandle) {
            auto coreLayer = reinterpret_cast<apl::sg::Layer *>(coreLayerHandle);
            auto children = coreLayer->children();
            jlong childrenAddresses[children.size()];
            int i = 0;
            for (auto it = children.begin(); it != children.end(); it++) {
                childrenAddresses[i++] = reinterpret_cast<jlong>((*it).get());
            }
            jlongArray result = env->NewLongArray(children.size());
            env->SetLongArrayRegion(result, 0, children.size(), childrenAddresses);
            return result;
        }

        JNIEXPORT jfloatArray JNICALL
        Java_com_amazon_apl_android_scenegraph_APLLayer_nGetBounds(JNIEnv *env, jclass clazz,
                                                                jlong coreLayerHandle) {
            auto coreLayer = reinterpret_cast<apl::sg::Layer *>(coreLayerHandle);
            auto value = coreLayer->getBounds();
            float buffer[4] = { value.getLeft(),
                                value.getTop(),
                                value.getWidth(),
                                value.getHeight()};

            jfloatArray jrect = env->NewFloatArray(4);
            env->SetFloatArrayRegion(jrect, 0, 4, buffer);
            return jrect;
        }

        JNIEXPORT jfloatArray JNICALL
        Java_com_amazon_apl_android_scenegraph_APLLayer_nGetChildOffset(JNIEnv *env, jclass clazz,
                                                                        jlong coreLayerHandle) {
            auto coreLayer = reinterpret_cast<apl::sg::Layer *>(coreLayerHandle);
            auto value = coreLayer->getChildOffset();
            float buffer[2] = { value.getX(),
                                value.getY()};

            jfloatArray jrect = env->NewFloatArray(2);
            env->SetFloatArrayRegion(jrect, 0, 2, buffer);
            return jrect;
        }

        JNIEXPORT jfloatArray JNICALL
        Java_com_amazon_apl_android_scenegraph_APLLayer_nGetLayerTransform(JNIEnv *env,
                                                                        jclass clazz,
                                                                        jlong coreLayerHandle) {
            auto coreLayer = reinterpret_cast<apl::sg::Layer *>(coreLayerHandle);
            auto transform = coreLayer->getTransform();
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

        JNIEXPORT jfloat JNICALL
        Java_com_amazon_apl_android_scenegraph_APLLayer_nGetLayerOpacity(JNIEnv *env,
                                                                      jclass clazz,
                                                                      jlong coreLayerHandle) {
            auto coreLayer = reinterpret_cast<apl::sg::Layer *>(coreLayerHandle);
            auto opacity = coreLayer->getOpacity();
            return static_cast<float>(opacity);
        }

        JNIEXPORT jlong JNICALL
                Java_com_amazon_apl_android_scenegraph_APLLayer_nGetChildClipPath(JNIEnv *env,
                                                                                  jclass clazz,
                                                                                  jlong coreLayerHandle) {
            auto coreLayer = reinterpret_cast<apl::sg::Layer *>(coreLayerHandle);
            return reinterpret_cast<jlong>(coreLayer->getChildClip().get());
        }


        JNIEXPORT jfloatArray JNICALL
        Java_com_amazon_apl_android_scenegraph_APLLayer_nGetLayerClipRect(JNIEnv *env,
                                                                       jclass clazz,
                                                                       jlong coreLayerHandle) {
            auto coreLayer = reinterpret_cast<apl::sg::Layer *>(coreLayerHandle);
            auto childClip = coreLayer->getChildClip();
            if (childClip) {
                if (childClip->type() == sg::Path::kRoundedRect) {
                    auto clipPath = std::static_pointer_cast<apl::sg::RoundedRectPath>(childClip);
                    auto bounds = clipPath->getRoundedRect().rect();
                    float boundsArray[4] = {bounds.getTop(), bounds.getLeft(),
                                            bounds.getRight(),
                                            bounds.getBottom()};
                    jfloatArray jClipBounds = env->NewFloatArray(4);
                    env->SetFloatArrayRegion(jClipBounds, 0, 4, boundsArray);
                    return jClipBounds;
                }
            } else {
                // No child clip path is specified, using bounds to clip children.
                auto layerBounds = coreLayer->getBounds();
                float layerBoundsArray[4] = {layerBounds.getLeft(), layerBounds.getTop(),
                                             layerBounds.getRight(), layerBounds.getBottom()};
                jfloatArray jLayerBounds = env->NewFloatArray(4);
                env->SetFloatArrayRegion(jLayerBounds, 0, 4, layerBoundsArray);
                return jLayerBounds;
            }
        }

        JNIEXPORT jfloatArray JNICALL
        Java_com_amazon_apl_android_scenegraph_APLLayer_nGetLayerClipRadii(JNIEnv *env,
                                                                        jclass clazz,
                                                                        jlong coreLayerHandle) {
            auto coreLayer = reinterpret_cast<apl::sg::Layer *>(coreLayerHandle);
            auto childClip = coreLayer->getChildClip();
            if (childClip) {
                if (childClip->type() == sg::Path::kRoundedRect) {
                    auto clipPath = std::static_pointer_cast<apl::sg::RoundedRectPath>(childClip);
                    auto radii = clipPath->getRoundedRect().radii();
                    float radiiArray[4] = {radii.topLeft(),
                                           radii.topRight(),
                                           radii.bottomRight(),
                                           radii.bottomLeft()};
                    jfloatArray jClipRadii = env->NewFloatArray(4);
                    env->SetFloatArrayRegion(jClipRadii, 0, 4, radiiArray);
                    return jClipRadii;
                }
            } else {
                // No child clip path is specified, using bounds to clip children.
                float layerClipArray[4] = {0, 0, 0, 0};
                jfloatArray jLayerClipRadii = env->NewFloatArray(4);
                env->SetFloatArrayRegion(jLayerClipRadii, 0, 4, layerClipArray);
                return jLayerClipRadii;
            }
        }

        JNIEXPORT jlong JNICALL
                Java_com_amazon_apl_android_scenegraph_APLLayer_nGetOutlinePath(JNIEnv *env,
                                                                                jclass clazz,
                                                                                jlong coreLayerHandle) {
            auto coreLayer = reinterpret_cast<apl::sg::Layer *>(coreLayerHandle);
            auto outline = coreLayer->getOutline();
            return reinterpret_cast<jlong>(outline.get());
        }

        JNIEXPORT jfloatArray JNICALL
        Java_com_amazon_apl_android_scenegraph_APLLayer_nGetOutlineRect(JNIEnv *env,
                                                                     jclass clazz,
                                                                     jlong coreLayerHandle) {
            auto coreLayer = reinterpret_cast<apl::sg::Layer *>(coreLayerHandle);
            auto outline = coreLayer->getOutline();
            if (outline) {
                if (outline->type() == sg::Path::kRoundedRect) {
                    auto outlineRect = std::static_pointer_cast<apl::sg::RoundedRectPath>(outline);
                    auto bounds = outlineRect->getRoundedRect().rect();
                    float boundsArray[4] = {bounds.getTop(), bounds.getLeft(),
                                            bounds.getRight(),
                                            bounds.getBottom()};
                    jfloatArray jOutline = env->NewFloatArray(4);
                    env->SetFloatArrayRegion(jOutline, 0, 4, boundsArray);
                    return jOutline;
                }
            } else {
                // No child clip path is specified, using bounds to clip children.
                auto layerBounds = coreLayer->getBounds();
                float layerBoundsArray[4] = {layerBounds.getLeft(), layerBounds.getTop(),
                                             layerBounds.getRight(), layerBounds.getBottom()};
                jfloatArray jLayerBounds = env->NewFloatArray(4);
                env->SetFloatArrayRegion(jLayerBounds, 0, 4, layerBoundsArray);
                return jLayerBounds;
            }
        }

        JNIEXPORT jlong JNICALL
        Java_com_amazon_apl_android_scenegraph_APLLayer_nGetShadow(JNIEnv *env,
                                                                        jclass clazz,
                                                                        jlong coreLayerHandle) {
            auto coreLayer = reinterpret_cast<apl::sg::Layer *>(coreLayerHandle);
            return reinterpret_cast<jlong>(coreLayer->getShadow().get());
        }

        JNIEXPORT jlong JNICALL
        Java_com_amazon_apl_android_scenegraph_APLLayer_nGetAccessibility(JNIEnv *env,
                                                                          jclass clazz,
                                                                          jlong coreLayerHandle) {
            auto coreLayer = reinterpret_cast<apl::sg::Layer *>(coreLayerHandle);
            return reinterpret_cast<jlong>(coreLayer->getAccessibility().get());
        }

        JNIEXPORT jboolean JNICALL
        Java_com_amazon_apl_android_scenegraph_APLLayer_nIsAccessibilityChecked(JNIEnv *env,
                                                                                jclass clazz,
                                                                                jlong coreLayerHandle) {
            auto coreLayer = reinterpret_cast<apl::sg::Layer *>(coreLayerHandle);
            return coreLayer->getInteraction() & apl::sg::Layer::kInteractionChecked;
        }

        JNIEXPORT jboolean JNICALL
        Java_com_amazon_apl_android_scenegraph_APLLayer_nIsAccessibilityDisabled(JNIEnv *env,
                                                                                 jclass clazz,
                                                                                 jlong coreLayerHandle) {
            auto coreLayer = reinterpret_cast<apl::sg::Layer *>(coreLayerHandle);
            return coreLayer->getInteraction() & apl::sg::Layer::kInteractionDisabled;
        }

        JNIEXPORT jboolean JNICALL
        Java_com_amazon_apl_android_scenegraph_APLLayer_nIsAccessibilityPressable(JNIEnv *env,
                                                                                  jclass clazz,
                                                                                  jlong coreLayerHandle) {
            auto coreLayer = reinterpret_cast<apl::sg::Layer *>(coreLayerHandle);
            return coreLayer->getInteraction() & apl::sg::Layer::kInteractionPressable;
        }

        JNIEXPORT jboolean JNICALL
        Java_com_amazon_apl_android_scenegraph_APLLayer_nIsHorizontallyScrollable(JNIEnv *env,
                                                                                  jclass clazz,
                                                                                  jlong coreLayerHandle) {
            auto coreLayer = reinterpret_cast<apl::sg::Layer *>(coreLayerHandle);
            return coreLayer->getInteraction() & apl::sg::Layer::kInteractionScrollHorizontal;
        }

        JNIEXPORT jboolean JNICALL
        Java_com_amazon_apl_android_scenegraph_APLLayer_nIsVerticallyScrollable(JNIEnv *env,
                                                                                jclass clazz,
                                                                                jlong coreLayerHandle) {
            auto coreLayer = reinterpret_cast<apl::sg::Layer *>(coreLayerHandle);
            return coreLayer->getInteraction() & apl::sg::Layer::kInteractionScrollVertical;
        }

        JNIEXPORT jfloatArray JNICALL
        Java_com_amazon_apl_android_scenegraph_APLLayer_nGetOutlineRadii(JNIEnv *env,
                                                                      jclass clazz,
                                                                      jlong coreLayerHandle) {
            auto coreLayer = reinterpret_cast<apl::sg::Layer *>(coreLayerHandle);
            auto outline = coreLayer->getOutline();
            if (outline) {
                if (outline->type() == sg::Path::kRoundedRect) {
                    auto outlineRect = std::static_pointer_cast<apl::sg::RoundedRectPath>(outline);
                    auto radii = outlineRect->getRoundedRect().radii();
                    float radiiArray[4] = {radii.topLeft(),
                                           radii.topRight(),
                                           radii.bottomRight(),
                                           radii.bottomLeft()};
                    jfloatArray jOutlineRadii = env->NewFloatArray(4);
                    env->SetFloatArrayRegion(jOutlineRadii, 0, 4, radiiArray);
                    return jOutlineRadii;
                }
            } else {
                // No child clip path is specified, using bounds to clip children.
                float layerClipArray[4] = {0, 0, 0, 0};
                jfloatArray jLayerClipRadii = env->NewFloatArray(4);
                env->SetFloatArrayRegion(jLayerClipRadii, 0, 4, layerClipArray);
                return jLayerClipRadii;
            }
        }

        JNIEXPORT bool JNICALL
        Java_com_amazon_apl_android_scenegraph_APLLayer_nIsRedrawFlagSet(JNIEnv *env,
                                                                      jclass clazz,
                                                                      jlong coreLayerHandle) {
            auto coreLayer = reinterpret_cast<apl::sg::Layer *>(coreLayerHandle);
            return coreLayer->isFlagSet(apl::sg::Layer::kFlagRedrawContent);
        }

        JNIEXPORT jstring JNICALL
        Java_com_amazon_apl_android_scenegraph_APLLayer_nGetLayerName(JNIEnv *env,
                                                                   jclass clazz,
                                                                   jlong coreLayerHandle) {
            auto coreLayer = reinterpret_cast<apl::sg::Layer *>(coreLayerHandle);
            return env->NewStringUTF(coreLayer->getName().c_str());
        }

        JNIEXPORT jlongArray JNICALL
        Java_com_amazon_apl_android_scenegraph_APLLayer_nGetContent(JNIEnv *env,
                                                                          jclass clazz,
                                                                          jlong coreLayerHandle) {
            auto coreLayer = reinterpret_cast<apl::sg::Layer *>(coreLayerHandle);
            auto content = coreLayer->content();
            int size = 0;
            while(content) {
                content = content->next();
                size++;
            }

            content = coreLayer->content();
            jlong children[size];
            int i = 0;
            while(content != nullptr) {
                jlong tmp = reinterpret_cast<jlong>(content.get());
                children[i] = tmp;
                i++;
                content = content->next();
            }

            jlongArray result = env->NewLongArray(size);
            env->SetLongArrayRegion(result, 0, size, children);
            return result;
        }

        // TODO: Node methods need to moved to their own JNI file
        JNIEXPORT jboolean JNICALL
        Java_com_amazon_apl_android_scenegraph_APLLayer_nIsNodeVisible(JNIEnv *env,
                                                                       jclass clazz,
                                                                       jlong coreNodeHandle) {
            auto node = reinterpret_cast<apl::sg::Node *>(coreNodeHandle);
            return node && node->visible();
        }

        JNIEXPORT jboolean JNICALL
        Java_com_amazon_apl_android_scenegraph_APLLayer_nIsTransformNode(JNIEnv *env,
                                                                         jclass clazz,
                                                                         jlong coreNodeHandle) {
            auto node = reinterpret_cast<apl::sg::Node *>(coreNodeHandle);
            return node->type() == sg::Node::kTransform;
        }

        JNIEXPORT jfloatArray JNICALL
        Java_com_amazon_apl_android_scenegraph_APLLayer_nGetContentTransform(JNIEnv *env,
                                                                             jclass clazz,
                                                                             jlong coreNodeHandle) {
            auto node = reinterpret_cast<apl::sg::Node *>(coreNodeHandle);
            const auto *transformNode = apl::sg::TransformNode::cast(node);
            float transformArray[6] = {0};
            auto array = transformNode->getTransform().get();
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

        JNIEXPORT jboolean JNICALL
        Java_com_amazon_apl_android_scenegraph_APLLayer_nIsDrawNode(JNIEnv *env,
                                                                         jclass clazz,
                                                                         jlong coreNodeHandle) {
            auto node = reinterpret_cast<apl::sg::Node *>(coreNodeHandle);
            return node->type() == sg::Node::kDraw;
        }

        JNIEXPORT jboolean JNICALL
        Java_com_amazon_apl_android_scenegraph_APLLayer_nIsPathRoundedRect(JNIEnv *env,
                                                                                jclass clazz,
                                                                                jlong coreNodeHandle) {
            auto node = reinterpret_cast<apl::sg::Node *>(coreNodeHandle);
            if (node->type() == sg::Node::kDraw) {
                auto drawNode = reinterpret_cast<apl::sg::DrawNode *>(node);
                auto path = drawNode->getPath();
                return path->type() == sg::Path::kRoundedRect;
            }
            return false;
        }

        JNIEXPORT jboolean JNICALL
        Java_com_amazon_apl_android_scenegraph_APLLayer_nIsPathFrameRect(JNIEnv *env,
                                                                              jclass clazz,
                                                                              jlong coreNodeHandle) {
            auto node = reinterpret_cast<apl::sg::Node *>(coreNodeHandle);
            if (node->type() == sg::Node::kDraw) {
                auto drawNode = reinterpret_cast<apl::sg::DrawNode *>(node);
                auto path = drawNode->getPath();
                return path->type() == sg::Path::kFrame;
            }
            return false;
        }

        JNIEXPORT int JNICALL
        Java_com_amazon_apl_android_scenegraph_APLLayer_nGetColor(JNIEnv *env,
                                                                       jclass clazz,
                                                                       jlong coreNodeHandle) {
            auto node = reinterpret_cast<apl::sg::Node *>(coreNodeHandle);
            auto drawNode = reinterpret_cast<apl::sg::DrawNode *>(node);
            
            auto paint = drawNode->getOp()->paint;
            if (paint->type() == sg::Paint::kColor) {
                auto colorPaint = apl::sg::ColorPaint::cast(paint);
                auto color = colorPaint->getColor();
                return color.get();
            }
            return 0;
        }

        JNIEXPORT jfloatArray JNICALL
        Java_com_amazon_apl_android_scenegraph_APLLayer_nGetInnerRectBounds(JNIEnv *env,
                                                                            jclass clazz,
                                                                            jlong coreNodeHandle) {
            auto node = reinterpret_cast<apl::sg::Node *>(coreNodeHandle);
            auto drawNode = reinterpret_cast<apl::sg::DrawNode *>(node);

            auto path = drawNode->getPath();
            auto frameRect = apl::sg::FramePath::cast(path);
            auto innerRect = frameRect->getRoundedRect().inset(frameRect->getInset());
            jfloat buffer[4] = { innerRect.rect().getLeft(),
                                 innerRect.rect().getTop(),
                                 innerRect.rect().getWidth(),
                                 innerRect.rect().getHeight()};
            jfloatArray jbounds = env->NewFloatArray(4);
            env->SetFloatArrayRegion(jbounds, 0, 4, buffer);
            return jbounds;
        }

        JNIEXPORT jfloatArray JNICALL
        Java_com_amazon_apl_android_scenegraph_APLLayer_nGetInnerRectRadii(JNIEnv *env,
                                                                           jclass clazz,
                                                                           jlong coreNodeHandle) {
            auto node = reinterpret_cast<apl::sg::Node *>(coreNodeHandle);
            auto drawNode = reinterpret_cast<apl::sg::DrawNode *>(node);

            auto path = drawNode->getPath();
            auto frameRect = apl::sg::FramePath::cast(path);
            auto innerRect = frameRect->getRoundedRect().inset(frameRect->getInset());
            jfloat buffer[4] = { innerRect.radii().topLeft(),
                                 innerRect.radii().topRight(),
                                 innerRect.radii().bottomRight(),
                                 innerRect.radii().bottomLeft()};
            jfloatArray jradii = env->NewFloatArray(4);
            env->SetFloatArrayRegion(jradii, 0, 4, buffer);
            return jradii;
        }

        JNIEXPORT jfloatArray JNICALL
        Java_com_amazon_apl_android_scenegraph_APLLayer_nGetRoundedRectBounds(JNIEnv *env,
                                                                                   jclass clazz,
                                                                                   jlong coreNodeHandle) {
            auto node = reinterpret_cast<apl::sg::Node *>(coreNodeHandle);
            auto drawNode = reinterpret_cast<apl::sg::DrawNode *>(node);
            
            auto path = drawNode->getPath();
            auto roundedRect = apl::sg::RoundedRectPath::cast(path);
            jfloat buffer[4] = { roundedRect->getRoundedRect().rect().getLeft(),
                                 roundedRect->getRoundedRect().rect().getTop(),
                                 roundedRect->getRoundedRect().rect().getWidth(),
                                 roundedRect->getRoundedRect().rect().getHeight()};
            jfloatArray jbounds = env->NewFloatArray(4);
            env->SetFloatArrayRegion(jbounds, 0, 4, buffer);
            return jbounds;
        }

        JNIEXPORT jfloatArray JNICALL
        Java_com_amazon_apl_android_scenegraph_APLLayer_nGetRoundedRectRadii(JNIEnv *env,
                                                                                  jclass clazz,
                                                                                  jlong coreNodeHandle) {
            auto node = reinterpret_cast<apl::sg::Node *>(coreNodeHandle);
            auto drawNode = reinterpret_cast<apl::sg::DrawNode *>(node);
            auto path = drawNode->getPath();
            auto roundedRect = apl::sg::RoundedRectPath::cast(path);
            jfloat buffer[4] = { roundedRect->getRoundedRect().radii().topLeft(),
                                 roundedRect->getRoundedRect().radii().topRight(),
                                 roundedRect->getRoundedRect().radii().bottomRight(),
                                 roundedRect->getRoundedRect().radii().bottomLeft()};
            jfloatArray jradii = env->NewFloatArray(4);
            env->SetFloatArrayRegion(jradii, 0, 4, buffer);
            return jradii;
        }

        JNIEXPORT jfloatArray JNICALL
        Java_com_amazon_apl_android_scenegraph_APLLayer_nGetFrameRectBounds(JNIEnv *env,
                                                                                 jclass clazz,
                                                                                 jlong coreNodeHandle) {
            auto node = reinterpret_cast<apl::sg::Node *>(coreNodeHandle);
            auto drawNode = reinterpret_cast<apl::sg::DrawNode *>(node);
            auto path = drawNode->getPath();
            auto frameRect = apl::sg::FramePath::cast(path);
            jfloat buffer[4] = { frameRect->getRoundedRect().rect().getLeft(),
                                 frameRect->getRoundedRect().rect().getTop(),
                                 frameRect->getRoundedRect().rect().getWidth(),
                                 frameRect->getRoundedRect().rect().getHeight()};
            jfloatArray jbounds = env->NewFloatArray(4);
            env->SetFloatArrayRegion(jbounds, 0, 4, buffer);
            return jbounds;
        }

        JNIEXPORT jfloatArray JNICALL
        Java_com_amazon_apl_android_scenegraph_APLLayer_nGetFrameRectRadii(JNIEnv *env,
                                                                                jclass clazz,
                                                                                jlong coreNodeHandle) {
            auto node = reinterpret_cast<apl::sg::Node *>(coreNodeHandle);
            auto drawNode = reinterpret_cast<apl::sg::DrawNode *>(node);
            auto path = drawNode->getPath();
            auto frameRect = apl::sg::FramePath::cast(path);
            jfloat buffer[4] = { frameRect->getRoundedRect().radii().topLeft(),
                                 frameRect->getRoundedRect().radii().topRight(),
                                 frameRect->getRoundedRect().radii().bottomRight(),
                                 frameRect->getRoundedRect().radii().bottomLeft()};
            jfloatArray jradii = env->NewFloatArray(4);
            env->SetFloatArrayRegion(jradii, 0, 4, buffer);
            return jradii;
        }

#ifdef __cplusplus
        }
#endif
    } //namespace jni
} //namespace apl
