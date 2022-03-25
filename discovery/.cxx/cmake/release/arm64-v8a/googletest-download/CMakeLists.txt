# Build GoogleTest code at configure time

cmake_minimum_required(VERSION 3.5)

project(googletest-unpack NONE)

include(ExternalProject)
ExternalProject_Add(googletest
      URL /Users/pranavsu/Documents/viewhost/apl-viewhost-android/discovery/../../apl-core-library/thirdparty/googletest-release-1.8.1.tar.gz
      URL_MD5 2e6fbeb6a91310a16efe181886c59596
      SOURCE_DIR        "/Users/pranavsu/Documents/viewhost/apl-viewhost-android/discovery/.cxx/cmake/release/arm64-v8a/googletest-src"
      BINARY_DIR        "/Users/pranavsu/Documents/viewhost/apl-viewhost-android/discovery/.cxx/cmake/release/arm64-v8a/googletest-build"
      CONFIGURE_COMMAND ""
      BUILD_COMMAND     ""
      INSTALL_COMMAND   ""
      TEST_COMMAND      ""
)
