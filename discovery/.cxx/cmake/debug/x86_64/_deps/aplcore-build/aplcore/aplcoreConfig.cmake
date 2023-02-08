
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

set(YOGA_EXTERNAL_LIB /Volumes/workplace/APLViewhostAndroid/src/APLViewhostAndroid/discovery/.cxx/cmake/debug/x86_64/lib/libyogacore.a)

if(YOGA_EXTERNAL_LIB)
    set_and_check(aplcore_yoga_LIBRARY "${YOGA_EXTERNAL_LIB}")
else()
    # This file gets installed at ${APL_CORE_INSTALL_DIR}/lib/cmake/aplcore/aplcoreConfig.cmake, so go up 3 directories
    # to find the root
    get_filename_component(APL_CORE_INSTALL_DIR "${CMAKE_CURRENT_LIST_DIR}/../../../" ABSOLUTE)

    set_and_check(aplcore_yoga_LIBRARY "${APL_CORE_INSTALL_DIR}/lib/libyogacore.a")

endif()

set(ENABLE_ALEXAEXTENSIONS ON)
set(USE_INTERNAL_ALEXAEXT ON)

if(ENABLE_ALEXAEXTENSIONS)
    if(NOT USE_INTERNAL_ALEXAEXT)
        find_package(alexaext REQUIRED)
    endif()
endif(ENABLE_ALEXAEXTENSIONS)

include("${CMAKE_CURRENT_LIST_DIR}/aplcoreTargets.cmake")

set_target_properties(apl::core
    PROPERTIES
        INTERFACE_LINK_LIBRARIES "${aplcore_yoga_LIBRARY}"
)
