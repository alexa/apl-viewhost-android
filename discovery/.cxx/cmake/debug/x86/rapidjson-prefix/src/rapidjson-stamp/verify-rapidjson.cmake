# Distributed under the OSI-approved BSD 3-Clause License.  See accompanying
# file Copyright.txt or https://cmake.org/licensing for details.

cmake_minimum_required(VERSION 3.5)

if("/Users/pranavsu/Documents/apl-viewhost-android/discovery/../../apl-core-library/thirdparty/rapidjson-v1.1.0.tar.gz" STREQUAL "")
  message(FATAL_ERROR "LOCAL can't be empty")
endif()

if(NOT EXISTS "/Users/pranavsu/Documents/apl-viewhost-android/discovery/../../apl-core-library/thirdparty/rapidjson-v1.1.0.tar.gz")
  message(FATAL_ERROR "File not found: /Users/pranavsu/Documents/apl-viewhost-android/discovery/../../apl-core-library/thirdparty/rapidjson-v1.1.0.tar.gz")
endif()

if("MD5" STREQUAL "")
  message(WARNING "File will not be verified since no URL_HASH specified")
  return()
endif()

if("badd12c511e081fec6c89c43a7027bce" STREQUAL "")
  message(FATAL_ERROR "EXPECT_VALUE can't be empty")
endif()

message(STATUS "verifying file...
     file='/Users/pranavsu/Documents/apl-viewhost-android/discovery/../../apl-core-library/thirdparty/rapidjson-v1.1.0.tar.gz'")

file("MD5" "/Users/pranavsu/Documents/apl-viewhost-android/discovery/../../apl-core-library/thirdparty/rapidjson-v1.1.0.tar.gz" actual_value)

if(NOT "${actual_value}" STREQUAL "badd12c511e081fec6c89c43a7027bce")
  message(FATAL_ERROR "error: MD5 hash of
  /Users/pranavsu/Documents/apl-viewhost-android/discovery/../../apl-core-library/thirdparty/rapidjson-v1.1.0.tar.gz
does not match expected value
  expected: 'badd12c511e081fec6c89c43a7027bce'
    actual: '${actual_value}'
")
endif()

message(STATUS "verifying file... done")
