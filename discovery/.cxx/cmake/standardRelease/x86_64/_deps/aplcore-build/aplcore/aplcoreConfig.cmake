
####### Expanded from @PACKAGE_INIT@ by configure_package_config_file() #######
####### Any changes to this file will be overwritten by the next CMake run ####
####### The input file was aplcoreConfig.cmake.in                            ########

get_filename_component(PACKAGE_PREFIX_DIR "${CMAKE_CURRENT_LIST_DIR}/../../../" ABSOLUTE)

macro(set_and_check _var _file)
  set(${_var} "${_file}")
  if(NOT EXISTS "${_file}")
    message(FATAL_ERROR "File or directory ${_file} referenced by variable ${_var} does not exist !")
  endif()
endmacro()

####################################################################################

set(YOGA_EXTERNAL_LIB /Volumes/workplace/APLViewhostAndroid/src/APLViewhostAndroid/discovery/.cxx/cmake/standardRelease/x86_64/lib/libyogacore.a)
set(USE_SYSTEM_RAPIDJSON OFF)

if(YOGA_EXTERNAL_LIB)
    set_and_check(aplcore_yoga_LIBRARY "${YOGA_EXTERNAL_LIB}")
else()
    # This file gets installed at ${APL_CORE_INSTALL_DIR}/lib/cmake/aplcore/aplcoreConfig.cmake, so go up 3 directories
    # to find the root
    get_filename_component(APL_CORE_INSTALL_DIR "${CMAKE_CURRENT_LIST_DIR}/../../../" ABSOLUTE)

    set_and_check(aplcore_yoga_LIBRARY "${APL_CORE_INSTALL_DIR}/lib/libyogacore.a")

endif()

set(ENABLE_ALEXAEXTENSIONS ON)

if(ENABLE_ALEXAEXTENSIONS)
    find_package(alexaext REQUIRED)
endif(ENABLE_ALEXAEXTENSIONS)

# For backwards-compatibility with the old build logic, try to locate RapidJSON on the system if the
# new CMake package is not found
if (NOT TARGET rapidjson-apl)
    if (USE_SYSTEM_RAPIDJSON)
        find_package(aplrapidjson QUIET)
        if (NOT aplrapidjson_FOUND)
            # Try to locate RapidJSON on the system
            find_package(RapidJSON QUIET)

            if (NOT RapidJSON_FOUND)
                # Try to find the headers directly on the system
                find_path(RAPIDJSON_INCLUDE_DIRS
                NAMES rapidjson/document.h
                REQUIRED)
            endif()

            add_library(rapidjson-apl INTERFACE IMPORTED)
            target_include_directories(rapidjson-apl INTERFACE ${RAPIDJSON_INCLUDE_DIRS})
        endif()
    else()
        find_package(aplrapidjson REQUIRED)
    endif()
endif()

include("${CMAKE_CURRENT_LIST_DIR}/aplcoreTargets.cmake")

target_link_libraries(apl::core INTERFACE "${aplcore_yoga_LIBRARY}")
