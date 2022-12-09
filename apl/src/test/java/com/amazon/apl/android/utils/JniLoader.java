package com.amazon.apl.android.utils;

import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

public class JniLoader {
    // Robolectric uses a custom ClassLoader for each configuration it tests, however, Java only allows
    // a unique binary to be loaded into a single ClassLoader, the others will throw errors when trying
    // to load the library. Imported native functions are also only available to the ClassLoader that
    // imported them.
    //
    // The solution for this is to make a copy of the library and load the copy in instead.
    public static void loadLibraries() {
        try {
            loadLibrary("apl-jni");
            loadLibrary("common-jni");
            loadLibrary("discovery-jni");
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static void loadLibrary(String libraryName) throws IOException {
        File originalLibrary = findLibraryPath(libraryName);

        String mappedLibraryName = System.mapLibraryName(libraryName);

        // Since we can't load the original library multiple times, load a copy instead.
        File tmpLibraryFile = java.nio.file.Files.createTempFile("", mappedLibraryName).toFile();
        tmpLibraryFile.deleteOnExit();

        Files.copy(originalLibrary, tmpLibraryFile);

        System.load(tmpLibraryFile.getAbsolutePath());
    }

    private static File findLibraryPath(String libraryName) {
        // The gradle file populates "java.library.path" with the directories where the core libraries are.
        String javaLibPath = System.getProperty("java.library.path");

        // Different platforms use different names for the libraries, this is usually handled by
        // System.loadLibrary but due to the ClassLoader problem, we need to load them by file name.
        String mappedLibraryName = System.mapLibraryName(libraryName);

        for (String path : javaLibPath.split(":")) {
            File file = Paths.get(path, mappedLibraryName).toFile();

            if (file.exists()) {
                return file;
            }
        }

        throw new RuntimeException(libraryName + " not found.");
    }
}
