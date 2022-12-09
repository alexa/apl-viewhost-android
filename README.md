# Alexa Presentation Language (APL) ViewHost Android version 2022.2

APLViewHostAndroid is a view host implementation for the Android Platform. It consists of
a thin JNI layer that interacts with APL Core Engine for component inflation and command
handling, and a native Android layout that maps APL Components to Android Views and ViewGroups.

The APL Android Viewhost consists of two elements:

- APL JNI adapter: 'libaplcore-jni.so' allows in process communication between the
  core C++ and Java Android layers.

- APL Android Library: .aar library exposing functionality of the APL spec, for use by Android
  applications. This library embeds the Core and JNI libraries.  The aar build targets SDK 28 and
  supports a minimum SDK 22, it has 2 flavors:

  - apl-release.aar - The release library
  - apl-debug.aar - The debug library

The Android library is built with Gradle.  The Gradle CMake integration plugin
will build the APL Core Library and Core JNI dependencies.

### Prerequisites

Make sure you have installed:

- [Android SDK](https://developer.android.com/studio/intro/update) version 28 or higher
- [Android NDK](https://developer.android.com/ndk/guides/#download-ndk) version 22 or higher
- APL Core build dependencies (e.g. one of supported C++ compilers, CMake)
- Ninja (Needed for APLCoreEngine when building from APLViewhostAndroid)

Setup a directory with the APL Android and APL Core - https://github.com/alexa/apl-core-library

```bash
$ ls
apl-core-library
apl-viewhost-android
```
The APL Core code is required for building the APL Android project.  The Gradle build
assumes it is in a sibling folder to the `apl-viewhost-android` project.  If the APL Core
code is located elsewhere, Gradle commands must be augmented with `-PaplCoreDir=<path.to.core>`
or you can set the value in the `gradle.properties` file.

## Building the Android APL Library

Set the Android SDK root environment variable:

```bash
$ export ANDROID_SDK_ROOT=/Users/YOUR-LOGIN-HERE/Library/Android/sdk/
```

Build the Android APL library:
```bash
 $ ./gradlew build
```

This step will also build the APL Core Library as a dependency.

To see a full list of Gradle tasks:
```bash
 $ ./gradlew tasks
```

## Troubleshooting

### CMake Error
```
CMake Error at ...APLCoreEngine/thirdparty/thirdparty.cmake:120 (message):
    CMake step for googletest failed: 1
```
The `ninja` build tool needs to be available on the `PATH`.