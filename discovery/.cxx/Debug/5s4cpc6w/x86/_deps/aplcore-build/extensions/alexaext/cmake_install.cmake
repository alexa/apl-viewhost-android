# Install script for directory: /Volumes/workplace/APLViewhostAndroid/src/APLCoreEngine/extensions/alexaext

# Set the install prefix
if(NOT DEFINED CMAKE_INSTALL_PREFIX)
  set(CMAKE_INSTALL_PREFIX "/usr/local")
endif()
string(REGEX REPLACE "/$" "" CMAKE_INSTALL_PREFIX "${CMAKE_INSTALL_PREFIX}")

# Set the install configuration name.
if(NOT DEFINED CMAKE_INSTALL_CONFIG_NAME)
  if(BUILD_TYPE)
    string(REGEX REPLACE "^[^A-Za-z0-9_]+" ""
           CMAKE_INSTALL_CONFIG_NAME "${BUILD_TYPE}")
  else()
    set(CMAKE_INSTALL_CONFIG_NAME "Debug")
  endif()
  message(STATUS "Install configuration: \"${CMAKE_INSTALL_CONFIG_NAME}\"")
endif()

# Set the component getting installed.
if(NOT CMAKE_INSTALL_COMPONENT)
  if(COMPONENT)
    message(STATUS "Install component: \"${COMPONENT}\"")
    set(CMAKE_INSTALL_COMPONENT "${COMPONENT}")
  else()
    set(CMAKE_INSTALL_COMPONENT)
  endif()
endif()

# Install shared libraries without execute permission?
if(NOT DEFINED CMAKE_INSTALL_SO_NO_EXE)
  set(CMAKE_INSTALL_SO_NO_EXE "0")
endif()

# Is this installation the result of a crosscompile?
if(NOT DEFINED CMAKE_CROSSCOMPILING)
  set(CMAKE_CROSSCOMPILING "TRUE")
endif()

# Set default install directory permissions.
if(NOT DEFINED CMAKE_OBJDUMP)
  set(CMAKE_OBJDUMP "/Users/pranavsu/Library/Android/sdk/ndk/23.0.7599858/toolchains/llvm/prebuilt/darwin-x86_64/bin/llvm-objdump")
endif()

if("x${CMAKE_INSTALL_COMPONENT}x" STREQUAL "xUnspecifiedx" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/lib" TYPE STATIC_LIBRARY FILES "/Volumes/workplace/APLViewhostAndroid/src/APLViewhostAndroid/discovery/.cxx/Debug/5s4cpc6w/x86/_deps/aplcore-build/extensions/alexaext/libalexaext.a")
endif()

if("x${CMAKE_INSTALL_COMPONENT}x" STREQUAL "xUnspecifiedx" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/include" TYPE DIRECTORY FILES "/Volumes/workplace/APLViewhostAndroid/src/APLCoreEngine/extensions/alexaext/include/alexaext" FILES_MATCHING REGEX "/[^/]*\\.h$")
endif()

if("x${CMAKE_INSTALL_COMPONENT}x" STREQUAL "xUnspecifiedx" OR NOT CMAKE_INSTALL_COMPONENT)
  if(EXISTS "$ENV{DESTDIR}${CMAKE_INSTALL_PREFIX}/lib/cmake/alexaext/alexaextTargets.cmake")
    file(DIFFERENT EXPORT_FILE_CHANGED FILES
         "$ENV{DESTDIR}${CMAKE_INSTALL_PREFIX}/lib/cmake/alexaext/alexaextTargets.cmake"
         "/Volumes/workplace/APLViewhostAndroid/src/APLViewhostAndroid/discovery/.cxx/Debug/5s4cpc6w/x86/_deps/aplcore-build/extensions/alexaext/CMakeFiles/Export/lib/cmake/alexaext/alexaextTargets.cmake")
    if(EXPORT_FILE_CHANGED)
      file(GLOB OLD_CONFIG_FILES "$ENV{DESTDIR}${CMAKE_INSTALL_PREFIX}/lib/cmake/alexaext/alexaextTargets-*.cmake")
      if(OLD_CONFIG_FILES)
        message(STATUS "Old export file \"$ENV{DESTDIR}${CMAKE_INSTALL_PREFIX}/lib/cmake/alexaext/alexaextTargets.cmake\" will be replaced.  Removing files [${OLD_CONFIG_FILES}].")
        file(REMOVE ${OLD_CONFIG_FILES})
      endif()
    endif()
  endif()
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/lib/cmake/alexaext" TYPE FILE FILES "/Volumes/workplace/APLViewhostAndroid/src/APLViewhostAndroid/discovery/.cxx/Debug/5s4cpc6w/x86/_deps/aplcore-build/extensions/alexaext/CMakeFiles/Export/lib/cmake/alexaext/alexaextTargets.cmake")
  if("${CMAKE_INSTALL_CONFIG_NAME}" MATCHES "^([Dd][Ee][Bb][Uu][Gg])$")
    file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/lib/cmake/alexaext" TYPE FILE FILES "/Volumes/workplace/APLViewhostAndroid/src/APLViewhostAndroid/discovery/.cxx/Debug/5s4cpc6w/x86/_deps/aplcore-build/extensions/alexaext/CMakeFiles/Export/lib/cmake/alexaext/alexaextTargets-debug.cmake")
  endif()
endif()

if("x${CMAKE_INSTALL_COMPONENT}x" STREQUAL "xUnspecifiedx" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/lib/cmake/alexaext" TYPE FILE FILES "/Volumes/workplace/APLViewhostAndroid/src/APLViewhostAndroid/discovery/.cxx/Debug/5s4cpc6w/x86/_deps/aplcore-build/extensions/alexaext/alexaextConfig.cmake")
endif()

