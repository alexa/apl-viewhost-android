# Build GoogleTest code at configure time

cmake_minimum_required(VERSION 3.5)

project(googletest-unpack NONE)

include(ExternalProject)
ExternalProject_Add(googletest
      URL /Volumes/workplace/APLViewhostAndroid/src/APLViewhostAndroid/discovery/../../APLCoreEngine/thirdparty/googletest-release-1.8.1.tar.gz
      URL_MD5 2e6fbeb6a91310a16efe181886c59596
      SOURCE_DIR        "/Volumes/workplace/APLViewhostAndroid/src/APLViewhostAndroid/discovery/.cxx/cmake/serviceV2Debug/armeabi-v7a/googletest-src"
      BINARY_DIR        "/Volumes/workplace/APLViewhostAndroid/src/APLViewhostAndroid/discovery/.cxx/cmake/serviceV2Debug/armeabi-v7a/googletest-build"
      CONFIGURE_COMMAND ""
      BUILD_COMMAND     ""
      INSTALL_COMMAND   ""
      TEST_COMMAND      ""
)
