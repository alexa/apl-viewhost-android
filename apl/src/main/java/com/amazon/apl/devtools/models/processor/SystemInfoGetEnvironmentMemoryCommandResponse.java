 /*
  * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
  * SPDX-License-Identifier: Apache-2.0
  */
 package com.amazon.apl.devtools.models.processor;

 import com.amazon.apl.devtools.models.common.Response;

 import org.json.JSONException;
 import org.json.JSONObject;

 public class SystemInfoGetEnvironmentMemoryCommandResponse extends Response {
     final long totalMemory;
     final long availableMemory;

     public SystemInfoGetEnvironmentMemoryCommandResponse(int id, long totalMemory, long availableMemory) {
         super(id);
         this.totalMemory = totalMemory;
         this.availableMemory = availableMemory;
     }

     @Override
     public JSONObject toJSONObject() throws JSONException {
         JSONObject result = new JSONObject();

         result.put("total", totalMemory);
         result.put("available", availableMemory);

         return super.toJSONObject().put("result", result);
     }
 }
