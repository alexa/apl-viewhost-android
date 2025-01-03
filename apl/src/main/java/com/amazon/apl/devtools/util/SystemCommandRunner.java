/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.util;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class SystemCommandRunner {
    private static final String TAG = SystemCommandRunner.class.getSimpleName();

    public static List<String> executeCommand(String... commandline) throws IOException {
        List<String> allLines = new ArrayList<>();

        ProcessBuilder builder = new ProcessBuilder(commandline).redirectErrorStream(true);
        Process process = builder.start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {

            String line;
            while ((line = reader.readLine()) != null) {
                allLines.add(line);
            }
        } catch (IOException e) {
            Log.e(TAG, "Unable to read line from stream.", e);
            throw(e);
        }
        finally {
            process.destroy();
        }
        return allLines;
    }
}
