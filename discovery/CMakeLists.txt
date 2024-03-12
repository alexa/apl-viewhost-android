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
option(INCLUDE_ALEXAEXT "Link Alexa Extension JNI" ON)
project (discovery-jni VERSION 1.0.0 LANGUAGES C CXX)


# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.
if (INCLUDE_ALEXAEXT)
    add_library( # Sets the name of the library.
            discovery-jni

            # Sets the library as a shared library.
            SHARED

            # Provides a relative path to your source file(s).
            src/main/cpp/jnidiscovery.cpp)
endif()

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
    add_library(alexaextjni STATIC IMPORTED)
    set_target_properties(alexaextjni
            PROPERTIES
            IMPORTED_LOCATION
            "${CMAKE_CURRENT_SOURCE_DIR}/../alexaextjni/.cxx/cmake/debug/host/libalexaextjni.a"
            INTERFACE_INCLUDE_DIRECTORIES "${CMAKE_CURRENT_SOURCE_DIR}/../alexaextjni/src/main/cpp/include"
            )

    add_library(rapidjson INTERFACE)
    target_include_directories(rapidjson INTERFACE
            # When we're building against RapidJSON, just use the include directory we discovered above
            $<BUILD_INTERFACE:${CMAKE_CURRENT_SOURCE_DIR}/../coreengine/src/main/cpp/rapidjson/include>
            )

    target_link_libraries(discovery-jni alexaextjni rapidjson alexaext)
else()
    if (INCLUDE_ALEXAEXT)
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

            target_compile_definitions(discovery-jni PRIVATE INCLUDE_ALEXAEXT="${INCLUDE_ALEXAEXT}")
            find_package(alexaextjni REQUIRED CONFIG)
            target_link_libraries(discovery-jni alexaextjni::alexaextjni)

            find_package(coreengine REQUIRED CONFIG)
            target_link_libraries(discovery-jni coreengine::alexaext coreengine::rapidjson)

    endif()

endif()

# Specifies a path to native header files.
include_directories(src/main/cpp/include)

# Common lib includes
include_directories(../common/src/main/cpp/include)
include_directories(../alexaextjni/src/main/cpp/include)