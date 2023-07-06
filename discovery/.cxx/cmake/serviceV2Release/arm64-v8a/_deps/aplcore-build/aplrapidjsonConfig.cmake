
####### Expanded from @PACKAGE_INIT@ by configure_package_config_file() #######
####### Any changes to this file will be overwritten by the next CMake run ####
####### The input file was aplrapidjsonConfig.cmake.in                            ########

get_filename_component(PACKAGE_PREFIX_DIR "${CMAKE_CURRENT_LIST_DIR}/../../../" ABSOLUTE)

macro(set_and_check _var _file)
  set(${_var} "${_file}")
  if(NOT EXISTS "${_file}")
    message(FATAL_ERROR "File or directory ${_file} referenced by variable ${_var} does not exist !")
  endif()
endmacro()

####################################################################################

set(USE_RAPIDJSON_PACKAGE FALSE)

if (USE_RAPIDJSON_PACKAGE)
    if (NOT TARGET rapidjson-apl) # Guard against multiple inclusion
        # Short circuit the usual mechanism and instead proxy the installed package.
        # This way we don't accidentally capture the exact path of the system rapidjson library
        find_package(RapidJSON REQUIRED)

        add_library(rapidjson-apl INTERFACE IMPORTED)
        target_include_directories(rapidjson-apl INTERFACE ${RAPIDJSON_INCLUDE_DIRS})
    endif()
else()
    include("${CMAKE_CURRENT_LIST_DIR}/aplrapidjsonTargets.cmake")
endif()

