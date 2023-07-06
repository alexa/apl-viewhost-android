# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.
include(FetchContent OPTIONAL RESULT_VARIABLE HAS_FETCH_CONTENT)

cmake_minimum_required(VERSION 3.18.1)
set(CMAKE_POLICY_DEFAULT_CMP0077 NEW)
set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)
project (apl-jni VERSION 1.0.0 LANGUAGES C CXX)

# set APL Core location
if (NOT APL_CORE_DIR)
    message(FATAL_ERROR "Please specify the location of APL Core")
endif ()
set(APL_PROJECT_DIR ${APL_CORE_DIR})

# Tell core to compile alexa extensions.
set(ENABLE_ALEXAEXTENSIONS ON)
set(BUILD_ALEXAEXTENSIONS ON)
set(ENABLE_PIC ON)

FetchContent_Declare(
        aplcore
        SOURCE_DIR ${APL_CORE_DIR}
)
FetchContent_MakeAvailable(aplcore)

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.
add_library(
        # Sets the name of the library.
        apl-jni
        # Sets the library as a shared library (libxxxx.so).
        SHARED
        # Provides a relative path to source file(s).
        src/main/cpp/jniapl.cpp
        src/main/cpp/jniaction.cpp
        src/main/cpp/jniaudioplayer.cpp
        src/main/cpp/jniaudioplayerfactory.cpp
        src/main/cpp/jnicontent.cpp
        src/main/cpp/jnidocumentcontext.cpp
        src/main/cpp/jnidocumentmanager.cpp
        src/main/cpp/jnidocumentsession.cpp
        src/main/cpp/jniembeddeddocumentrequest.cpp
        src/main/cpp/jniextensionclient.cpp
        src/main/cpp/jniextensioncommand.cpp
        src/main/cpp/jniextensioncomponent.cpp
        src/main/cpp/jniextensioneventhandler.cpp
        src/main/cpp/jniextensionfilter.cpp
        src/main/cpp/jniextensionmediator.cpp
        src/main/cpp/jnimediaplayer.cpp
        src/main/cpp/jnimediaplayerfactory.cpp
        src/main/cpp/jnirootcontext.cpp
        src/main/cpp/jnirootconfig.cpp
        src/main/cpp/jnisession.cpp
        src/main/cpp/jnicomponent.cpp
        src/main/cpp/jnicomplexproperty.cpp
        src/main/cpp/jnievent.cpp
        src/main/cpp/jnilivedata.cpp
        src/main/cpp/jniutil.cpp
        src/main/cpp/jniscaling.cpp
        src/main/cpp/jnimetricstransform.cpp
        src/main/cpp/jnigraphic.cpp
        src/main/cpp/jnitextmeasurecallback.cpp
        src/main/cpp/jnishadowblur.cpp
        src/main/cpp/jninoisefilter.cpp
        src/main/cpp/jnitext.cpp
)

# Specifies a path to native header files.
include_directories(src/main/cpp/include)

if (NOT ANDROID)
    # Ensure jni.h is found
    find_package(JNI REQUIRED)
    include_directories(${JAVA_INCLUDE_PATH})
    include_directories(${JAVA_INCLUDE_PATH2})
else()
    # Searches for a specified prebuilt library and stores the path as a
    # variable. Because CMake includes system libraries in the search path by
    # default, you only need to specify the name of the public NDK library
    # you want to add. CMake verifies that the library exists before
    # completing its build.
    find_library(
            # Sets the name of the path variable.
            log-lib
            # Specifies the name of the NDK library that you want CMake to locate.
            log)

    # Specifies libraries CMake should link to your target library. You
    # can link multiple libraries, such as libraries you define in this
    # build script, prebuilt third-party libraries, or system libraries.
    target_link_libraries(
            # Specifies the target library.
            apl-jni
            -ljnigraphics
            # Links the target library to the log library included in the NDK.
            ${log-lib})

    set(ENUMGEN_BIN "${CMAKE_BINARY_DIR}/tools/enumgen")

    add_custom_target(generate-android-enums ALL
            COMMAND cd ${APL_CORE_DIR} && ${ENUMGEN_BIN}
            -f  "AnimationQuality"
            -f  "AudioPlayerEventType"
            -f  "BlendMode"
            -f  "ComponentType"
            -f  "ContainerDirection"
            -f  "DimensionType"
            -f  "Display"
            -f  "DisplayState"
            -f  "EventAudioTrack"
            -f  "EventControlMediaCommand"
            -f  "EventDirection"
            -f  "EventHighlightMode"
            -f  "EventProperty"
            -f  "EventReason"
            -f  "EventScrollAlign"
            -f  "EventType"
            -f  "EventMediaType"
            -f  "FilterType"
            -f  "FilterProperty"
            -f  "FlexboxAlign"
            -f  "FlexboxJustifyContent"
            -f  "FocusDirection"
            -f  "FontStyle"
            -f  "GradientProperty"
            -f  "GradientSpreadMethod"
            -f  "GradientType"
            -f  "GradientUnits"
            -f  "GraphicTextAnchor"
            -f  "GraphicElementType"
            -f  "GraphicLayoutDirection"
            -f  "GraphicLineCap"
            -f  "GraphicLineJoin"
            -f  "GraphicPropertyKey"
            -f  "GraphicFilterType"
            -f  "GraphicFilterProperty"
            -f  "GraphicScale"
            -f  "GraphicScale"
            -f  "ImageAlign"
            -f  "ImageCount"
            -f  "ImageScale"
            -f  "KeyHandlerType"
            -f  "LayoutDirection"
            -f  "MediaPlayerEventType"
            -f  "Navigation"
            -f  "NoiseFilterKind"
            -f  "Position"
            -f  "PointerEventType"
            -f  "PointerType"
            -f  "PropertyKey"
            -f  "RootProperty"
            -f  "ScreenShape"
            -f  "ScrollDirection"
            -f  "SpanAttributeName"
            -f  "SpanType"
            -f  "Snap"
            -f  "SpeechMarkType"
            -f  "TextAlign"
            -f  "TextAlignVertical"
            -f  "TextTrackType"
            -f  "TokenType"
            -f  "TrackState"
            -f  "UpdateType"
            -f  "VectorGraphicAlign"
            -f  "VectorGraphicScale"
            -f  "VideoScale"
            -f  "ViewportMode"
            -f  "AudioTrack"
            -f  "KeyboardType"
            -f  "SubmitKeyType"
            -f  "ScreenMode"
            -f  "Role"
            -f  "ExtensionComponentResourceState"
            -l java -p com.amazon.apl.enums -o ${CMAKE_CURRENT_SOURCE_DIR}/src/main/java/com/amazon/apl/enums
            ${APL_CORE_DIR}/aplcore/include/action/*.h
            ${APL_CORE_DIR}/aplcore/include/animation/*.h
            ${APL_CORE_DIR}/aplcore/include/apl/audio/*.h
            ${APL_CORE_DIR}/aplcore/include/apl/command/*.h
            ${APL_CORE_DIR}/aplcore/include/apl/component/*.h
            ${APL_CORE_DIR}/aplcore/include/apl/content/*.h
            ${APL_CORE_DIR}/aplcore/include/apl/datagrammar/*.h
            ${APL_CORE_DIR}/aplcore/include/apl/document/*.h
            ${APL_CORE_DIR}/aplcore/include/apl/engine/*.h
            ${APL_CORE_DIR}/aplcore/include/apl/graphic/*.h
            ${APL_CORE_DIR}/aplcore/include/apl/media/*.h
            ${APL_CORE_DIR}/aplcore/include/apl/primitives/*.h
            ${APL_CORE_DIR}/aplcore/include/apl/time/*.h
            ${APL_CORE_DIR}/aplcore/include/apl/utils/*.h
            ${APL_CORE_DIR}/aplcore/include/apl/touch/*.h
            ${APL_CORE_DIR}/aplcore/include/apl/focus/*.h
            DEPENDS enumgen
            )

    add_dependencies(apl-jni generate-android-enums)
endif()


target_link_libraries(apl-jni apl alexaext)

# Common lib includes
include_directories(../common/src/main/cpp/include)

# ccache
find_program(CCACHE_FOUND ccache)
if(CCACHE_FOUND)
    set_property(GLOBAL PROPERTY RULE_LAUNCH_COMPILE ccache)
    set_property(GLOBAL PROPERTY RULE_LAUNCH_LINK ccache)
endif(CCACHE_FOUND)
