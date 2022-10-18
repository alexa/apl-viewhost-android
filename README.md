#README: APL Android

The APL Android target contains 3 modules

- APL jni adapter: 'libaplcore-jni.so' allows in process communication between the 
  core C++ and Java Android layers.

- APL Android Library: .aar library exposing functionality of the APL spec, for use by Android 
  applications. This library embeds the core and jni libraries.  The aar build targets SDK 28 and 
  supports a minimum SDK 22, it has 4 flavors:
    
    - apl-core-release.aar - The release  library
    - apl-demo-release.aar - The release library with sample APL layout assets included.
    - apl-core-debug.aar - The debug library
    - apl-demo-debug.aar -  The debug library with sample APL layout assets included.

- APL Android Sample App:  A simple sample application that uses the demo library sample assets.

The Android library and Sample application are built with gradle.  The Gradle cmake integration plugin
will build the the APL core library and core jni dependencies.


### Prerequisites

- [Install NDK](https://developer.android.com/ndk/guides/#download-ndk) version 20 
 - Higher versions of NDK are not currently compatible with the current gradle plugin.
- [Install Android SDK](https://developer.android.com/studio/intro/update) version 28 or higher
- Install Ninja (Needed for APLCoreEngine when building from APLViewhostAndroid)
- Setup a workspace with the APL Android, and (optionally) APL Core code
```bash
$ ls
APLCoreEngine        
APLViewhostAndroid
```
> The APL Core code is required for building the APL Android project.  The Gradle build
> assumes it is in a sibling folder to the APLAndroidViewhost project.  If the APL Core
> code is located elsewhere gradle commands must be augmented with `-PaplCoreDir=<path.to.core>`
> or set the value in the `gradle.properties` file.

## Android Studio 

The APL Android sample application is used to test and demo the APL Android functionality.  It has a fixed
build dependency on  apl-demo-debug.aar .  The sample app targets Android SDK 28 and can be run with 
an emulator for desktop display and projection.

To run the project in Android Studio:

- Launch the latest Android Studio (3.3.0+)
- Import the gradle project located at <workspace>/APLAndroidViewhost/
- You will see Android Studio Modules for "apl" and "app"
- Open file app/com/amazon/apl/examples/AplSampleActivity and select your preferred demo data, or add your own
- Run AplSampleActivity

> To debug the project configure Android Studio to find the debug symbols. 
> "Run"->"Edit Configuration"->"Debugger"->"Symbol Directories" add the APL
> Android target root directory  <workspace>/APLViewhostAndroid

## Building the APL Core for the Android APL Library

The APL Core library is a required dependency of the Android Viewhost and must be built using the CMAKE defines for Android.

To compile 'libaplcore.a' and 'libaplcore-jni.so' for armeabi-v7a devices, do the following, from the APL Core Library project:
```
 $ mkdir build
 $ cd build
 $ cmake -DANDROID_ABI="armeabi-v7a" -DANDROID_PLATFORM=android-28 -DCMAKE_TOOLCHAIN_FILE=$NDK_HOME/build/cmake/android.toolchain.cmake -DAPL_JNI=ON ..
 $ make -j4
```
For other architectures, the ANDROID_ABI flag should be set appropriately:  
  for example, -DANDROID_ABI="x86" for an x86 build

## Building the Android APL Library


To build the Android APL library and the Sample application:
```bash
 $ ./gradlew build
```
To install the `APL Sample App` connect your device or emulator:
```bash
 $ ./gradlew app:installDebug
```
To see a full list of gradle tasks:
```bash
 $ ./gradlew tasks
```

## Troubleshooting

### CMake Error
```
CMake Error at /Volumes/workplace/APLAndroid/src/APLCoreEngine/thirdparty/thirdparty.cmake:120 (message):
    CMake step for googletest failed: 1
```
The `ninja` build tool needs to be available on the `PATH`.

### Gradle Error
```
> Could not get unknown property 'externalNativeBuildCoreDebug' for project ':apl' of type org.gradle.api.Project.
```

The current android gradle version (3.3.0) requires a NDK install that includes a `platforms` folder. 
Starting with NDK 22.x, the `platforms` folder no longer exists. Additionally, 
the default ndk location for 3.3.0 is `$ANDROID_SDK_HOME/ndk-bundle`, but the current version of 
Android Studio (4.1.2) is no longer capable of installing the NDK to that location. 

To work around this error (on a mac), you should download a compatible ndk version (< 22.x) and then 
symbolic link it to the `ndk-bundle` folder:
```
ln -s /Users/$USER/Library/Android/sdk/ndk/20.0.5594570/ /Users/$USER/Library/Android/sdk/ndk-bundle
```