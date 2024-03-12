# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.
include(FetchContent OPTIONAL RESULT_VARIABLE HAS_FETCH_CONTENT)

cmake_minimum_required(VERSION 3.18.1)
set(CMAKE_POLICY_DEFAULT_CMP0077 NEW)
set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)
set(ENABLE_PIC ON)
option(REMOVE_PRIVATE_EXPORTS "Hide Private Symbols from build" OFF)
option(INCLUDE_ALEXAEXT "Link Alexa Extension JNI" OFF)

if (DEFINED SCENE_GRAPH)
    set(ENABLE_SCENEGRAPH ${SCENE_GRAPH})
endif()

project (apl-jni VERSION 1.0.0 LANGUAGES C CXX)

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

target_compile_definitions(apl-jni PRIVATE VERSION_NAME="${VERSION_NAME}")

if (NOT ANDROID)
    # Ensure jni.h is found
    find_package(JNI REQUIRED)
    include_directories(${JAVA_INCLUDE_PATH})
    include_directories(${JAVA_INCLUDE_PATH2})

    add_library(aplcore STATIC IMPORTED)

    if (ENABLE_SCENEGRAPH)
        list(APPEND APL_INCLUDE_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../coreengine/src/main/cpp/aplsgconfig/include")
    else()
        list(APPEND APL_INCLUDE_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../coreengine/src/main/cpp/aplconfig/include")
    endif()
    list(APPEND APL_INCLUDE_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../coreengine/src/main/cpp/apl/include")

    set_target_properties(aplcore
            PROPERTIES
            IMPORTED_LOCATION
            "${CMAKE_CURRENT_SOURCE_DIR}/../coreengine/.cxx/cmake/debug/host/_deps/aplcore-build/aplcore/libapl.a"
            INTERFACE_INCLUDE_DIRECTORIES "${APL_INCLUDE_DIR}"
            )

    add_library(alexaext STATIC IMPORTED)
    set_target_properties(alexaext
            PROPERTIES
            IMPORTED_LOCATION
            "${CMAKE_CURRENT_SOURCE_DIR}/../coreengine/.cxx/cmake/debug/host/_deps/aplcore-build/extensions/alexaext/libalexaext.a"
            INTERFACE_INCLUDE_DIRECTORIES "${CMAKE_CURRENT_SOURCE_DIR}/../coreengine/src/main/cpp/alexaext/include"
            )

    add_library(rapidjson INTERFACE)
    target_include_directories(rapidjson INTERFACE
            # When we're building against RapidJSON, just use the include directory we discovered above
            $<BUILD_INTERFACE:${CMAKE_CURRENT_SOURCE_DIR}/../coreengine/src/main/cpp/rapidjson/include>
            )
    if(INCLUDE_ALEXAEXT)
        add_library(alexaextjni STATIC IMPORTED)
        set_target_properties(alexaextjni
                PROPERTIES
                IMPORTED_LOCATION
                "${CMAKE_CURRENT_SOURCE_DIR}/../alexaextjni/.cxx/cmake/debug/host/libalexaextjni.a"
                INTERFACE_INCLUDE_DIRECTORIES "${CMAKE_CURRENT_SOURCE_DIR}/../alexaextjni/src/main/cpp/include"
                )
        target_link_libraries(apl-jni alexaextjni aplcore rapidjson alexaext)
    else()
        target_link_libraries(apl-jni aplcore rapidjson alexaext)
    endif()

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
    # Remove private exports if demanded.
    if(REMOVE_PRIVATE_EXPORTS)
        # Use a version script (aka export map) to both reduce symbol visibility as
        # well as improve symbol relocation performance.  Without an export map,
        # all C++ symbols are global, even those not accessible using JNI.
        # For more info: https://www.akkadia.org/drepper/dsohowto.pdf
        set_target_properties(
                apl-jni PROPERTIES
                LINK_FLAGS "-Wl,--version-script=${CMAKE_CURRENT_SOURCE_DIR}/apl.map"
        )
    endif()

    if(INCLUDE_ALEXAEXT)
        target_compile_definitions(apl-jni PRIVATE INCLUDE_ALEXAEXT="${VERSION_NAME}")
        find_package(alexaextjni REQUIRED CONFIG)
        target_link_libraries(apl-jni alexaextjni::alexaextjni)
    endif()

    find_package(coreengine REQUIRED CONFIG)
    target_link_libraries(apl-jni coreengine::apl coreengine::alexaext coreengine::rapidjson)
endif()



# Common lib includes
include_directories(../common/src/main/cpp/include)

# ccache
find_program(CCACHE_FOUND ccache)
if(CCACHE_FOUND)
    set_property(GLOBAL PROPERTY RULE_LAUNCH_COMPILE ccache)
    set_property(GLOBAL PROPERTY RULE_LAUNCH_LINK ccache)
endif(CCACHE_FOUND)
