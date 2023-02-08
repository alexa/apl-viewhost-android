# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.
include(FetchContent OPTIONAL RESULT_VARIABLE HAS_FETCH_CONTENT)

cmake_minimum_required(VERSION 3.18.1)
set(CMAKE_POLICY_DEFAULT_CMP0077 NEW)
set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)
project (discovery-jni VERSION 1.0.0 LANGUAGES C CXX)

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

add_library( # Sets the name of the library.
        discovery-jni

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        src/main/cpp/jnidiscovery.cpp
        src/main/cpp/jniextensionexecutor.cpp
        src/main/cpp/jniextensionproxy.cpp
        src/main/cpp/jniextensionregistrar.cpp
        src/main/cpp/jniextensionresource.cpp)

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
            discovery-jni

            # Links the target library to the log library
            # included in the NDK.
            ${log-lib})
endif()

# Specifies a path to native header files.
include_directories(src/main/cpp/include)

# Common lib includes
include_directories(../common/src/main/cpp/include)

target_link_libraries(discovery-jni alexaext)