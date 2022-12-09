# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.
cmake_minimum_required(VERSION 3.10)
set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
project (apl-jni VERSION 1.0.0 LANGUAGES C CXX)

# set APL Core location
if (NOT APL_CORE_DIR)
    message(FATAL_ERROR "Please specify the location of APL Core")
endif ()
set(APL_PROJECT_DIR ${APL_CORE_DIR})

# third party
set(APL_PATCH_DIR "${APL_CORE_DIR}/patches")
set(APL_DEPENDENCIES_DIR "${APL_CORE_DIR}/thirdparty")
include(${APL_DEPENDENCIES_DIR}/thirdparty.cmake)

# tools
set(EXTENSIONS_BUILD_DIR ${CMAKE_CURRENT_BINARY_DIR}/build-extensions)
set(APL_CORE_BUILD_DIR ${CMAKE_CURRENT_BINARY_DIR}/build)
include(${APL_CORE_DIR}/options.cmake)
include(${APL_CORE_DIR}/tools.cmake)

include_directories(${APL_CORE_DIR}/aplcore)
include_directories(${APL_CORE_BUILD_DIR/aplcore/include})

# Extensions framework
include_directories(${APL_CORE_DIR}/extensions/alexaext/include)

# build folder
add_subdirectory(${APL_CORE_DIR}/extensions ${EXTENSIONS_BUILD_DIR})

target_compile_definitions(alexaext PUBLIC ALEXAEXTENSIONS)
add_dependencies(alexaext rapidjson-build)

# build folder
file(MAKE_DIRECTORY ${APL_CORE_DIR}/aplcore/build)
add_subdirectory(${APL_CORE_DIR}/aplcore ${APL_CORE_BUILD_DIR})

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
        src/main/cpp/jnidocumentsession.cpp
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
)

# Specifies a path to native header files.
include_directories(src/main/cpp/include)

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


target_link_libraries(apl-jni apl alexaext)

# APL Core include and dependencies
include_directories(${APL_CORE_DIR}/aplcore/include)
include_directories(${APL_CORE_DIR}/aplcore/include/apl)
include_directories(${APL_CORE_DIR}/aplcore/include/apl/util)
include_directories(${APL_CORE_DIR}/aplcore/include/apl/datasource)

# APL Core 3rd party dependencies
include_directories(${PEGTL_INCLUDE})
include_directories(${RAPIDJSON_INCLUDE})
include_directories(${YOGA_INCLUDE})

# Common lib includes
include_directories(../common/src/main/cpp/include)

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
        -f  "ObjectType"
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

# ccache
find_program(CCACHE_FOUND ccache)
if(CCACHE_FOUND)
    set_property(GLOBAL PROPERTY RULE_LAUNCH_COMPILE ccache)
    set_property(GLOBAL PROPERTY RULE_LAUNCH_LINK ccache)
endif(CCACHE_FOUND)