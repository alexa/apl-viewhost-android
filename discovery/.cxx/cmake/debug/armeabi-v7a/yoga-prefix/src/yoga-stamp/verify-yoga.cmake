# Distributed under the OSI-approved BSD 3-Clause License.  See accompanying
# file Copyright.txt or https://cmake.org/licensing for details.

cmake_minimum_required(VERSION 3.5)

if("/Users/pranavsu/Documents/viewhost/apl-viewhost-android/discovery/../../apl-core-library/thirdparty/yoga-1.16.0.tar.gz" STREQUAL "")
  message(FATAL_ERROR "LOCAL can't be empty")
endif()

if(NOT EXISTS "/Users/pranavsu/Documents/viewhost/apl-viewhost-android/discovery/../../apl-core-library/thirdparty/yoga-1.16.0.tar.gz")
  message(FATAL_ERROR "File not found: /Users/pranavsu/Documents/viewhost/apl-viewhost-android/discovery/../../apl-core-library/thirdparty/yoga-1.16.0.tar.gz")
endif()

if("MD5" STREQUAL "")
  message(WARNING "File will not be verified since no URL_HASH specified")
  return()
endif()

if("c9e88076ec371513fb23a0a5370ec2fd" STREQUAL "")
  message(FATAL_ERROR "EXPECT_VALUE can't be empty")
endif()

message(STATUS "verifying file...
     file='/Users/pranavsu/Documents/viewhost/apl-viewhost-android/discovery/../../apl-core-library/thirdparty/yoga-1.16.0.tar.gz'")

file("MD5" "/Users/pranavsu/Documents/viewhost/apl-viewhost-android/discovery/../../apl-core-library/thirdparty/yoga-1.16.0.tar.gz" actual_value)

if(NOT "${actual_value}" STREQUAL "c9e88076ec371513fb23a0a5370ec2fd")
  message(FATAL_ERROR "error: MD5 hash of
  /Users/pranavsu/Documents/viewhost/apl-viewhost-android/discovery/../../apl-core-library/thirdparty/yoga-1.16.0.tar.gz
does not match expected value
  expected: 'c9e88076ec371513fb23a0a5370ec2fd'
    actual: '${actual_value}'
")
endif()

message(STATUS "verifying file... done")
