# Distributed under the OSI-approved BSD 3-Clause License.  See accompanying
# file Copyright.txt or https://cmake.org/licensing for details.

cmake_minimum_required(VERSION 3.5)

file(MAKE_DIRECTORY
  "/Volumes/workplace/APLViewhostAndroid/src/APLViewhostAndroid/discovery/.cxx/cmake/debug/host/_deps/aplcore-build/yoga-prefix/src/yoga"
  "/Volumes/workplace/APLViewhostAndroid/src/APLViewhostAndroid/discovery/.cxx/cmake/debug/host/_deps/aplcore-build/yoga-prefix/src/yoga-build"
  "/Volumes/workplace/APLViewhostAndroid/src/APLViewhostAndroid/discovery/.cxx/cmake/debug/host/lib"
  "/Volumes/workplace/APLViewhostAndroid/src/APLViewhostAndroid/discovery/.cxx/cmake/debug/host/_deps/aplcore-build/yoga-prefix/tmp"
  "/Volumes/workplace/APLViewhostAndroid/src/APLViewhostAndroid/discovery/.cxx/cmake/debug/host/_deps/aplcore-build/yoga-prefix/src/yoga-stamp"
  "/Volumes/workplace/APLViewhostAndroid/src/APLViewhostAndroid/discovery/.cxx/cmake/debug/host/_deps/aplcore-build/yoga-prefix/src"
  "/Volumes/workplace/APLViewhostAndroid/src/APLViewhostAndroid/discovery/.cxx/cmake/debug/host/_deps/aplcore-build/yoga-prefix/src/yoga-stamp"
)

set(configSubDirs )
foreach(subDir IN LISTS configSubDirs)
    file(MAKE_DIRECTORY "/Volumes/workplace/APLViewhostAndroid/src/APLViewhostAndroid/discovery/.cxx/cmake/debug/host/_deps/aplcore-build/yoga-prefix/src/yoga-stamp/${subDir}")
endforeach()
if(cfgdir)
  file(MAKE_DIRECTORY "/Volumes/workplace/APLViewhostAndroid/src/APLViewhostAndroid/discovery/.cxx/cmake/debug/host/_deps/aplcore-build/yoga-prefix/src/yoga-stamp${cfgdir}") # cfgdir has leading slash
endif()
