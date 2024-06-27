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

    public static List<String> executeTopCommand() throws IOException {
        List<String> allLines = new ArrayList<>();

        String appId = "-p" + android.os.Process.myPid();
        ProcessBuilder builder = new ProcessBuilder("top", appId, "-oPID,%CPU", "-b", "-n1");
        Process process = builder.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                allLines.add(line);
            }
            return allLines;
        } catch (IOException e) {
            Log.e(TAG, "Unable to read line from stream.", e);
            throw(e);
        } finally {
            process.destroy();
        }
    }
}
