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
CMAKE_SOURCE_DIR = /Volumes/workplace/APLViewhostAndroid/src/APLViewhostAndroid/common

# The top-level build directory on which CMake was run.
CMAKE_BINARY_DIR = /Volumes/workplace/APLViewhostAndroid/src/APLViewhostAndroid/common/.cxx/cmake/debug/host

# Include any dependencies generated for this target.
include CMakeFiles/common-jni.dir/depend.make
# Include any dependencies generated by the compiler for this target.
include CMakeFiles/common-jni.dir/compiler_depend.make

# Include the progress variables for this target.
include CMakeFiles/common-jni.dir/progress.make

# Include the compile flags for this target's objects.
include CMakeFiles/common-jni.dir/flags.make

CMakeFiles/common-jni.dir/src/main/cpp/jninativeowner.cpp.o: CMakeFiles/common-jni.dir/flags.make
CMakeFiles/common-jni.dir/src/main/cpp/jninativeowner.cpp.o: /Volumes/workplace/APLViewhostAndroid/src/APLViewhostAndroid/common/src/main/cpp/jninativeowner.cpp
CMakeFiles/common-jni.dir/src/main/cpp/jninativeowner.cpp.o: CMakeFiles/common-jni.dir/compiler_depend.ts
	@$(CMAKE_COMMAND) -E cmake_echo_color --switch=$(COLOR) --green --progress-dir=/Volumes/workplace/APLViewhostAndroid/src/APLViewhostAndroid/common/.cxx/cmake/debug/host/CMakeFiles --progress-num=$(CMAKE_PROGRESS_1) "Building CXX object CMakeFiles/common-jni.dir/src/main/cpp/jninativeowner.cpp.o"
	/Library/Developer/CommandLineTools/usr/bin/c++ $(CXX_DEFINES) $(CXX_INCLUDES) $(CXX_FLAGS) -MD -MT CMakeFiles/common-jni.dir/src/main/cpp/jninativeowner.cpp.o -MF CMakeFiles/common-jni.dir/src/main/cpp/jninativeowner.cpp.o.d -o CMakeFiles/common-jni.dir/src/main/cpp/jninativeowner.cpp.o -c /Volumes/workplace/APLViewhostAndroid/src/APLViewhostAndroid/common/src/main/cpp/jninativeowner.cpp

CMakeFiles/common-jni.dir/src/main/cpp/jninativeowner.cpp.i: cmake_force
	@$(CMAKE_COMMAND) -E cmake_echo_color --switch=$(COLOR) --green "Preprocessing CXX source to CMakeFiles/common-jni.dir/src/main/cpp/jninativeowner.cpp.i"
	/Library/Developer/CommandLineTools/usr/bin/c++ $(CXX_DEFINES) $(CXX_INCLUDES) $(CXX_FLAGS) -E /Volumes/workplace/APLViewhostAndroid/src/APLViewhostAndroid/common/src/main/cpp/jninativeowner.cpp > CMakeFiles/common-jni.dir/src/main/cpp/jninativeowner.cpp.i

CMakeFiles/common-jni.dir/src/main/cpp/jninativeowner.cpp.s: cmake_force
	@$(CMAKE_COMMAND) -E cmake_echo_color --switch=$(COLOR) --green "Compiling CXX source to assembly CMakeFiles/common-jni.dir/src/main/cpp/jninativeowner.cpp.s"
	/Library/Developer/CommandLineTools/usr/bin/c++ $(CXX_DEFINES) $(CXX_INCLUDES) $(CXX_FLAGS) -S /Volumes/workplace/APLViewhostAndroid/src/APLViewhostAndroid/common/src/main/cpp/jninativeowner.cpp -o CMakeFiles/common-jni.dir/src/main/cpp/jninativeowner.cpp.s

# Object files for target common-jni
common__jni_OBJECTS = \
"CMakeFiles/common-jni.dir/src/main/cpp/jninativeowner.cpp.o"

# External object files for target common-jni
common__jni_EXTERNAL_OBJECTS =

libcommon-jni.dylib: CMakeFiles/common-jni.dir/src/main/cpp/jninativeowner.cpp.o
libcommon-jni.dylib: CMakeFiles/common-jni.dir/build.make
libcommon-jni.dylib: CMakeFiles/common-jni.dir/link.txt
	@$(CMAKE_COMMAND) -E cmake_echo_color --switch=$(COLOR) --green --bold --progress-dir=/Volumes/workplace/APLViewhostAndroid/src/APLViewhostAndroid/common/.cxx/cmake/debug/host/CMakeFiles --progress-num=$(CMAKE_PROGRESS_2) "Linking CXX shared library libcommon-jni.dylib"
	$(CMAKE_COMMAND) -E cmake_link_script CMakeFiles/common-jni.dir/link.txt --verbose=$(VERBOSE)

# Rule to build all files generated by this target.
CMakeFiles/common-jni.dir/build: libcommon-jni.dylib
.PHONY : CMakeFiles/common-jni.dir/build

CMakeFiles/common-jni.dir/clean:
	$(CMAKE_COMMAND) -P CMakeFiles/common-jni.dir/cmake_clean.cmake
.PHONY : CMakeFiles/common-jni.dir/clean

CMakeFiles/common-jni.dir/depend:
	cd /Volumes/workplace/APLViewhostAndroid/src/APLViewhostAndroid/common/.cxx/cmake/debug/host && $(CMAKE_COMMAND) -E cmake_depends "Unix Makefiles" /Volumes/workplace/APLViewhostAndroid/src/APLViewhostAndroid/common /Volumes/workplace/APLViewhostAndroid/src/APLViewhostAndroid/common /Volumes/workplace/APLViewhostAndroid/src/APLViewhostAndroid/common/.cxx/cmake/debug/host /Volumes/workplace/APLViewhostAndroid/src/APLViewhostAndroid/common/.cxx/cmake/debug/host /Volumes/workplace/APLViewhostAndroid/src/APLViewhostAndroid/common/.cxx/cmake/debug/host/CMakeFiles/common-jni.dir/DependInfo.cmake --color=$(COLOR)
.PHONY : CMakeFiles/common-jni.dir/depend

