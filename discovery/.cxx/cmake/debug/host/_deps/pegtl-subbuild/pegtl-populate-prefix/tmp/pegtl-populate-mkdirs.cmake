# Distributed under the OSI-approved BSD 3-Clause License.  See accompanying
# file Copyright.txt or https://cmake.org/licensing for details.

cmake_minimum_required(VERSION 3.5)

file(MAKE_DIRECTORY
  "/Volumes/workplace/APLViewhostAndroid/src/APLViewhostAndroid/discovery/.cxx/cmake/debug/host/_deps/pegtl-src"
  "/Volumes/workplace/APLViewhostAndroid/src/APLViewhostAndroid/discovery/.cxx/cmake/debug/host/_deps/pegtl-build"
  "/Volumes/workplace/APLViewhostAndroid/src/APLViewhostAndroid/discovery/.cxx/cmake/debug/host/_deps/pegtl-subbuild/pegtl-populate-prefix"
  "/Volumes/workplace/APLViewhostAndroid/src/APLViewhostAndroid/discovery/.cxx/cmake/debug/host/_deps/pegtl-subbuild/pegtl-populate-prefix/tmp"
  "/Volumes/workplace/APLViewhostAndroid/src/APLViewhostAndroid/discovery/.cxx/cmake/debug/host/_deps/pegtl-subbuild/pegtl-populate-prefix/src/pegtl-populate-stamp"
  "/Volumes/workplace/APLViewhostAndroid/src/APLViewhostAndroid/discovery/.cxx/cmake/debug/host/_deps/pegtl-subbuild/pegtl-populate-prefix/src"
  "/Volumes/workplace/APLViewhostAndroid/src/APLViewhostAndroid/discovery/.cxx/cmake/debug/host/_deps/pegtl-subbuild/pegtl-populate-prefix/src/pegtl-populate-stamp"
)

set(configSubDirs )
foreach(subDir IN LISTS configSubDirs)
    file(MAKE_DIRECTORY "/Volumes/workplace/APLViewhostAndroid/src/APLViewhostAndroid/discovery/.cxx/cmake/debug/host/_deps/pegtl-subbuild/pegtl-populate-prefix/src/pegtl-populate-stamp/${subDir}")
endforeach()
if(cfgdir)
  file(MAKE_DIRECTORY "/Volumes/workplace/APLViewhostAndroid/src/APLViewhostAndroid/discovery/.cxx/cmake/debug/host/_deps/pegtl-subbuild/pegtl-populate-prefix/src/pegtl-populate-stamp${cfgdir}") # cfgdir has leading slash
endif()
