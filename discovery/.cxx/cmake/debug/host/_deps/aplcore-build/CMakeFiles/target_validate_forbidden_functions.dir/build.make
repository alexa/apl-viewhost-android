# CMAKE generated file: DO NOT EDIT!
# Generated by "Unix Makefiles" Generator, CMake Version 3.24

# Delete rule output on recipe failure.
.DELETE_ON_ERROR:

#=============================================================================
# Special targets provided by cmake.

# Disable implicit rules so canonical targets will work.
.SUFFIXES:

# Disable VCS-based implicit rules.
% : %,v

# Disable VCS-based implicit rules.
% : RCS/%

# Disable VCS-based implicit rules.
% : RCS/%,v

# Disable VCS-based implicit rules.
% : SCCS/s.%

# Disable VCS-based implicit rules.
% : s.%

.SUFFIXES: .hpux_make_needs_suffix_list

# Produce verbose output by default.
VERBOSE = 1

# Command-line flag to silence nested $(MAKE).
$(VERBOSE)MAKESILENT = -s

#Suppress display of executed commands.
$(VERBOSE).SILENT:

# A target that is always out of date.
cmake_force:
.PHONY : cmake_force

#=============================================================================
# Set environment variables for the build.

# The shell in which to execute make rules.
SHELL = /bin/sh

# The CMake executable.
CMAKE_COMMAND = /usr/local/Cellar/cmake/3.24.2/bin/cmake

# The command to remove a file.
RM = /usr/local/Cellar/cmake/3.24.2/bin/cmake -E rm -f

# Escaping for special characters.
EQUALS = =

# The top-level source directory on which CMake was run.
CMAKE_SOURCE_DIR = /Volumes/workplace/APLViewhostAndroid/src/APLViewhostAndroid/discovery

# The top-level build directory on which CMake was run.
CMAKE_BINARY_DIR = /Volumes/workplace/APLViewhostAndroid/src/APLViewhostAndroid/discovery/.cxx/cmake/debug/host

# Utility rule file for target_validate_forbidden_functions.

# Include any custom commands dependencies for this target.
include _deps/aplcore-build/CMakeFiles/target_validate_forbidden_functions.dir/compiler_depend.make

# Include the progress variables for this target.
include _deps/aplcore-build/CMakeFiles/target_validate_forbidden_functions.dir/progress.make

_deps/aplcore-build/CMakeFiles/target_validate_forbidden_functions: _deps/aplcore-build/forbidden_function_validation

_deps/aplcore-build/forbidden_function_validation:
	@$(CMAKE_COMMAND) -E cmake_echo_color --switch=$(COLOR) --blue --bold --progress-dir=/Volumes/workplace/APLViewhostAndroid/src/APLViewhostAndroid/discovery/.cxx/cmake/debug/host/CMakeFiles --progress-num=$(CMAKE_PROGRESS_1) "Generating forbidden_function_validation"
	cd /Volumes/workplace/APLViewhostAndroid/src/APLCoreEngine && bash /Volumes/workplace/APLViewhostAndroid/src/APLCoreEngine/bin/find-forbidden-functions

target_validate_forbidden_functions: _deps/aplcore-build/CMakeFiles/target_validate_forbidden_functions
target_validate_forbidden_functions: _deps/aplcore-build/forbidden_function_validation
target_validate_forbidden_functions: _deps/aplcore-build/CMakeFiles/target_validate_forbidden_functions.dir/build.make
.PHONY : target_validate_forbidden_functions

# Rule to build all files generated by this target.
_deps/aplcore-build/CMakeFiles/target_validate_forbidden_functions.dir/build: target_validate_forbidden_functions
.PHONY : _deps/aplcore-build/CMakeFiles/target_validate_forbidden_functions.dir/build

_deps/aplcore-build/CMakeFiles/target_validate_forbidden_functions.dir/clean:
	cd /Volumes/workplace/APLViewhostAndroid/src/APLViewhostAndroid/discovery/.cxx/cmake/debug/host/_deps/aplcore-build && $(CMAKE_COMMAND) -P CMakeFiles/target_validate_forbidden_functions.dir/cmake_clean.cmake
.PHONY : _deps/aplcore-build/CMakeFiles/target_validate_forbidden_functions.dir/clean

_deps/aplcore-build/CMakeFiles/target_validate_forbidden_functions.dir/depend:
	cd /Volumes/workplace/APLViewhostAndroid/src/APLViewhostAndroid/discovery/.cxx/cmake/debug/host && $(CMAKE_COMMAND) -E cmake_depends "Unix Makefiles" /Volumes/workplace/APLViewhostAndroid/src/APLViewhostAndroid/discovery /Volumes/workplace/APLViewhostAndroid/src/APLCoreEngine /Volumes/workplace/APLViewhostAndroid/src/APLViewhostAndroid/discovery/.cxx/cmake/debug/host /Volumes/workplace/APLViewhostAndroid/src/APLViewhostAndroid/discovery/.cxx/cmake/debug/host/_deps/aplcore-build /Volumes/workplace/APLViewhostAndroid/src/APLViewhostAndroid/discovery/.cxx/cmake/debug/host/_deps/aplcore-build/CMakeFiles/target_validate_forbidden_functions.dir/DependInfo.cmake --color=$(COLOR)
.PHONY : _deps/aplcore-build/CMakeFiles/target_validate_forbidden_functions.dir/depend
