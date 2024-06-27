# Alexa Presentation Language (APL) ViewHost Android version 2024.2

The APL Android target contains 3 modules

- APL jni adapter: 'libaplcore-jni.so' allows in process communication between the 
  core C++ and Java Android layers.

- APL Android Library: .aar library exposing functionality of the APL spec, for use by Android 
  applications. This library embeds the core and jni libraries.  The aar build targets SDK 34 and 
  supports a minimum SDK 22, it has 5 flavors:
    
    - apl-core-release.aar - The release  library
    - apl-demo-release.aar - The release library with sample APL layout assets included.
    - aplMinSized-release.aar - The release library with size optimizations to minimize download size.
    - apl-core-debug.aar - The debug library
    - apl-demo-debug.aar -  The debug library with sample APL layout assets included.

The Android library is built with gradle. The Gradle cmake integration plugin
will build the APL core library and core jni dependencies.

The library ships with a discovery module which adds the extension discovery functionality. The
discovery library has following flavors and should be only used with its compatible flavor of  
apl library, when needed:

   - discovery-standard-debug.aar should be used with apl-core-debug.aar
   - discovery-standard-release.aar should be used with apl-core-release.aar
   - discovery-standardMinsized-release.aar should be used with apl-core-release.aar
   - discovery-serviceV2-debug.aar should be used with apl-core-debug.aar
   - discovery-serviceV2ls --release.aar should be used with apl-core-release.aar

### Prerequisites

- [Install NDK](https://developer.android.com/ndk/guides/#download-ndk) version 23.0.7599858 
- [Install Android SDK](https://developer.android.com/studio/intro/update) version 34 or higher
- Install Ninja (Needed for APLCoreEngine when building from APLViewhostAndroid)
- Java 17 (https://adoptium.net/ is recommended)
- Setup a directory with the APL Android and APL Core - https://github.com/alexa/apl-core-library
```bash
$ ls
APLCoreEngine        
APLViewhostAndroid
```
> The APL Core code is required for building the APL Android project.  The Gradle build
> assumes it is in a sibling folder to the APLAndroidViewhost project.  If the APL Core
> code is located elsewhere gradle commands must be augmented with `-PaplCoreDir=<path.to.core>`
> or set the value in the `gradle.properties` file.

## Building the Android APL Library


To build the Android APL library:
```bash
 $ ./gradlew build
```
To see a full list of gradle tasks:
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

### Gradle Error: Lombok

When building _outside_ of Android Studio using `gradlew`, you may get a fatal error from Lombok:
```
> java.lang.IllegalAccessError: class lombok.javac.apt.LombokProcessor (in unnamed module @0x1b65ad4c) cannot access class com.sun.tools.javac.processing.JavacProcessingEnvironment (in module jdk.compiler) because module jdk.compiler does not export com.sun.tools.javac.processing to unnamed module @0x1b65ad4c
```

This may occur because the Lombok library version must be compatible with the
Java compiler.  Android Studio uses an internal verson of Java, so the Lombok
library supplied with APLViewhostAndroid is compatible with that Java.
Building from the command line gives you whichever version of Java is in your
path, which you can see with `echo $JAVA_HOME`.  If your version of Java is
too new (or too old), the view host will not compile.

We recommend the Temurin 17 JDK.
