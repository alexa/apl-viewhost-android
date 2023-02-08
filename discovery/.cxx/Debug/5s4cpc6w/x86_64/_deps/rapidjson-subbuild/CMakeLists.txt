# Distributed under the OSI-approved BSD 3-Clause License.  See accompanying
# file Copyright.txt or https://cmake.org/licensing for details.

cmake_minimum_required(VERSION 3.18.1-g262b901)

# We name the project and the target for the ExternalProject_Add() call
# to something that will highlight to the user what we are working on if
# something goes wrong and an error message is produced.

project(rapidjson-populate NONE)

include(ExternalProject)
ExternalProject_Add(rapidjson-populate
                     "UPDATE_DISCONNECTED" "False" "URL" "/Volumes/workplace/APLViewhostAndroid/src/APLViewhostAndroid/discovery/../../APLCoreEngine/thirdparty/rapidjson-v1.1.0.tar.gz" "URL_MD5" "badd12c511e081fec6c89c43a7027bce" "PATCH_COMMAND" "patch" "-p1" "<" "/Volumes/workplace/APLViewhostAndroid/src/APLCoreEngine/patches/rapidjson.patch"
                    SOURCE_DIR          "/Volumes/workplace/APLViewhostAndroid/src/APLViewhostAndroid/discovery/.cxx/Debug/5s4cpc6w/x86_64/_deps/rapidjson-src"
                    BINARY_DIR          "/Volumes/workplace/APLViewhostAndroid/src/APLViewhostAndroid/discovery/.cxx/Debug/5s4cpc6w/x86_64/_deps/rapidjson-build"
                    CONFIGURE_COMMAND   ""
                    BUILD_COMMAND       ""
                    INSTALL_COMMAND     ""
                    TEST_COMMAND        ""
                    USES_TERMINAL_DOWNLOAD  YES
                    USES_TERMINAL_UPDATE    YES
)


