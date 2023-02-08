#----------------------------------------------------------------
# Generated CMake target import file for configuration "Debug".
#----------------------------------------------------------------

# Commands may need to know the format version.
set(CMAKE_IMPORT_FILE_VERSION 1)

# Import target "apl::core" for configuration "Debug"
set_property(TARGET apl::core APPEND PROPERTY IMPORTED_CONFIGURATIONS DEBUG)
set_target_properties(apl::core PROPERTIES
  IMPORTED_LINK_INTERFACE_LANGUAGES_DEBUG "CXX"
  IMPORTED_LOCATION_DEBUG "${_IMPORT_PREFIX}/lib/libapl.a"
  )

list(APPEND _IMPORT_CHECK_TARGETS apl::core )
list(APPEND _IMPORT_CHECK_FILES_FOR_apl::core "${_IMPORT_PREFIX}/lib/libapl.a" )

# Commands beyond this point should not need to know the version.
set(CMAKE_IMPORT_FILE_VERSION)
