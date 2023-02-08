#----------------------------------------------------------------
# Generated CMake target import file.
#----------------------------------------------------------------

# Commands may need to know the format version.
set(CMAKE_IMPORT_FILE_VERSION 1)

# Import target "apl::core" for configuration ""
set_property(TARGET apl::core APPEND PROPERTY IMPORTED_CONFIGURATIONS NOCONFIG)
set_target_properties(apl::core PROPERTIES
  IMPORTED_LINK_INTERFACE_LANGUAGES_NOCONFIG "CXX"
  IMPORTED_LOCATION_NOCONFIG "${_IMPORT_PREFIX}/lib/libapl.a"
  )

list(APPEND _cmake_import_check_targets apl::core )
list(APPEND _cmake_import_check_files_for_apl::core "${_IMPORT_PREFIX}/lib/libapl.a" )

# Commands beyond this point should not need to know the version.
set(CMAKE_IMPORT_FILE_VERSION)
