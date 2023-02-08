# Distributed under the OSI-approved BSD 3-Clause License.  See accompanying
# file Copyright.txt or https://cmake.org/licensing for details.

cmake_minimum_required(VERSION 3.18.1-g262b901)

# We name the project and the target for the ExternalProject_Add() call
# to something that will highlight to the user what we are working on if
# something goes wrong and an error message is produced.

project(pegtl-populate NONE)

include(ExternalProject)
ExternalProject_Add(pegtl-populate
                     "UPDATE_DISCONNECTED" "False" "URL" "/Volumes/workplace/APLViewhostAndroid/src/APLViewhostAndroid/discovery/../../APLCoreEngine/thirdparty/pegtl-2.8.3.tar.gz" "URL_MD5" "28b3c455d9ec392dd4230402383a8c6f" "PATCH_COMMAND" "patch" "-p1" "<" "/Volumes/workplace/APLViewhostAndroid/src/APLCoreEngine/patches/pegtl.patch"
                    SOURCE_DIR          "/Volumes/workplace/APLViewhostAndroid/src/APLViewhostAndroid/discovery/.cxx/cmake/standardDebug/arm64-v8a/_deps/pegtl-src"
                    BINARY_DIR          "/Volumes/workplace/APLViewhostAndroid/src/APLViewhostAndroid/discovery/.cxx/cmake/standardDebug/arm64-v8a/_deps/pegtl-build"
                    CONFIGURE_COMMAND   ""
                    BUILD_COMMAND       ""
                    INSTALL_COMMAND     ""
                    TEST_COMMAND        ""
                    USES_TERMINAL_DOWNLOAD  YES
                    USES_TERMINAL_UPDATE    YES
)


