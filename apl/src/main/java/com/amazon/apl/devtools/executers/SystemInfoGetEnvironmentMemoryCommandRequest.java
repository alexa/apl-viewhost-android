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
         SystemMemoryInfo memoryInfo = getSystemMemoryInfo();
         return new SystemInfoGetEnvironmentMemoryCommandResponse(getId(), memoryInfo.totalMemory, memoryInfo.availableMemory);
     }

     private SystemMemoryInfo getSystemMemoryInfo() {
         ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
         ActivityManager activityManager = (ActivityManager) mTargetCatalog.getAppContext().getSystemService(Context.ACTIVITY_SERVICE);
         if (activityManager != null) {
             activityManager.getMemoryInfo(memoryInfo);
             if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                 return new SystemMemoryInfo(memoryInfo.availMem, memoryInfo.totalMem);
             }
         }
         return new SystemMemoryInfo(-1, -1);
     }

     private static class SystemMemoryInfo {
         private final long availableMemory;
         private final long totalMemory;

         private SystemMemoryInfo(long availableMemory, long totalMemory) {
             this.availableMemory = availableMemory;
             this.totalMemory = totalMemory;
         }
     }

 }
