 /*
  * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
  * SPDX-License-Identifier: Apache-2.0
  */
 package com.amazon.apl.devtools.executers;

 import android.app.ActivityManager;
 import android.content.Context;
 import android.os.Build;
 import android.util.Log;

 import com.amazon.apl.devtools.enums.CommandMethod;
 import com.amazon.apl.devtools.models.processor.SystemInfoGetEnvironmentMemoryCommandRequestModel;
 import com.amazon.apl.devtools.models.processor.SystemInfoGetEnvironmentMemoryCommandResponse;
 import com.amazon.apl.devtools.util.TargetCatalog;

 import org.json.JSONException;
 import org.json.JSONObject;

 public class SystemInfoGetEnvironmentMemoryCommandRequest extends SystemInfoGetEnvironmentMemoryCommandRequestModel {
     public static final String TAG = SystemInfoGetEnvironmentMemoryCommandRequest.class.getSimpleName();
     private final TargetCatalog mTargetCatalog;

     public SystemInfoGetEnvironmentMemoryCommandRequest(TargetCatalog targetCatalog, JSONObject obj) throws JSONException {
         super(obj);
         mTargetCatalog = targetCatalog;
     }

     @Override
     public SystemInfoGetEnvironmentMemoryCommandResponse execute() {
         Log.i(TAG, "Executing " + CommandMethod.SYSTEM_INFO_GET_ENVIRONMENT_MEMORY + " command");
         return new SystemInfoGetEnvironmentMemoryCommandResponse(getId(), getSystemTotalMemory(), getSystemAvailableMemory());
     }

     /**
      * Get the available memory on the system.
      *
      * @return The available memory in bytes, or -1 if unable to retrieve memory info or if the context is null.
      */
     private long getSystemAvailableMemory() {
         if (mTargetCatalog != null && mTargetCatalog.getAppContext() != null) {
             ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
             ActivityManager activityManager = (ActivityManager) mTargetCatalog.getAppContext().getSystemService(Context.ACTIVITY_SERVICE);
             if (activityManager != null) {
                 activityManager.getMemoryInfo(memoryInfo);
                 if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                     return memoryInfo.availMem;
                 }
             }
         }
         return -1;
     }


     /**
      * Get amount of physical memory on the system.
      *
      * @return The total memory in bytes, or -1 if unable to retrieve memory info or if the context is null.
      */
     private long getSystemTotalMemory() {
         if (mTargetCatalog != null && mTargetCatalog.getAppContext() != null) {
             ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
             ActivityManager activityManager = (ActivityManager) mTargetCatalog.getAppContext().getSystemService(Context.ACTIVITY_SERVICE);
             if (activityManager != null) {
                 activityManager.getMemoryInfo(memoryInfo);
                 if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                     return memoryInfo.totalMem;
                 }
             }
         }
         return -1;
     }

 }
