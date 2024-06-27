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
project (alexaextjni VERSION 1.0.0 LANGUAGES C CXX)


# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

add_library( # Sets the name of the library.
        alexaextjni
        # Sets the library as a static library.
        STATIC
        src/main/cpp/jniextensionexecutor.cpp
        src/main/cpp/jniextensionproxy.cpp
        src/main/cpp/jniextensionregistrar.cpp
        src/main/cpp/jniextensionresource.cpp
        src/main/cpp/jnimetricsextensionv2.cpp
        src/main/cpp/jnidestinationfactory.cpp
        src/main/cpp/jnidestination.cpp
        )

# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

if (NOT ANDROID)
    # Ensure jni.h is found
    find_package(JNI REQUIRED)
    include_directories(${JAVA_INCLUDE_PATH})
    include_directories(${JAVA_INCLUDE_PATH2})

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

    target_link_libraries(alexaextjni rapidjson alexaext)
else()
    find_library( # Sets the name of the path variable.
        log-lib

        # Specifies the name of the NDK library that
        # you want CMake to locate.
        log)

    # Specifies libraries CMake should link to your target library. You
    # can link multiple libraries, such as libraries you define in this
    # build script, prebuilt third-party libraries, or system libraries.

    target_link_libraries( # Specifies the target library.
            alexaextjni

            # Links the target library to the log library
            # included in the NDK.
            ${log-lib})

    find_package(coreengine REQUIRED CONFIG)
    target_link_libraries(alexaextjni coreengine::alexaext coreengine::rapidjson)
endif()

# Specifies a path to native header files.
include_directories(src/main/cpp/include)

# Common lib includes
include_directories(../common/src/main/cpp/include)