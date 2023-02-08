# Distributed under the OSI-approved BSD 3-Clause License.  See accompanying
# file Copyright.txt or https://cmake.org/licensing for details.

cmake_minimum_required(VERSION 3.5)

file(MAKE_DIRECTORY
  "/Volumes/workplace/APLViewhostAndroid/src/APLViewhostAndroid/discovery/../../APLCoreEngine"
  "/Volumes/workplace/APLViewhostAndroid/src/APLViewhostAndroid/discovery/.cxx/cmake/debug/host/_deps/aplcore-build"
  "/Volumes/workplace/APLViewhostAndroid/src/APLViewhostAndroid/discovery/.cxx/cmake/debug/host/_deps/aplcore-subbuild/aplcore-populate-prefix"
  "/Volumes/workplace/APLViewhostAndroid/src/APLViewhostAndroid/discovery/.cxx/cmake/debug/host/_deps/aplcore-subbuild/aplcore-populate-prefix/tmp"
  "/Volumes/workplace/APLViewhostAndroid/src/APLViewhostAndroid/discovery/.cxx/cmake/debug/host/_deps/aplcore-subbuild/aplcore-populate-prefix/src/aplcore-populate-stamp"
  "/Volumes/workplace/APLViewhostAndroid/src/APLViewhostAndroid/discovery/.cxx/cmake/debug/host/_deps/aplcore-subbuild/aplcore-populate-prefix/src"
  "/Volumes/workplace/APLViewhostAndroid/src/APLViewhostAndroid/discovery/.cxx/cmake/debug/host/_deps/aplcore-subbuild/aplcore-populate-prefix/src/aplcore-populate-stamp"
)

set(configSubDirs )
foreach(subDir IN LISTS configSubDirs)
    file(MAKE_DIRECTORY "/Volumes/workplace/APLViewhostAndroid/src/APLViewhostAndroid/discovery/.cxx/cmake/debug/host/_deps/aplcore-subbuild/aplcore-populate-prefix/src/aplcore-populate-stamp/${subDir}")
endforeach()
if(cfgdir)
  file(MAKE_DIRECTORY "/Volumes/workplace/APLViewhostAndroid/src/APLViewhostAndroid/discovery/.cxx/cmake/debug/host/_deps/aplcore-subbuild/aplcore-populate-prefix/src/aplcore-populate-stamp${cfgdir}") # cfgdir has leading slash
endif()
