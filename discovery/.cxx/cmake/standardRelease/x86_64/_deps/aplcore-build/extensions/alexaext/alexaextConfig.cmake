
####### Expanded from @PACKAGE_INIT@ by configure_package_config_file() #######
####### Any changes to this file will be overwritten by the next CMake run ####
####### The input file was alexaextConfig.cmake.in                            ########

get_filename_component(PACKAGE_PREFIX_DIR "${CMAKE_CURRENT_LIST_DIR}/../../../" ABSOLUTE)

macro(set_and_check _var _file)
  set(${_var} "${_file}")
  if(NOT EXISTS "${_file}")
    message(FATAL_ERROR "File or directory ${_file} referenced by variable ${_var} does not exist !")
  endif()
endmacro()

####################################################################################

set(USE_SYSTEM_RAPIDJSON OFF)

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

include("${CMAKE_CURRENT_LIST_DIR}/alexaextTargets.cmake")
