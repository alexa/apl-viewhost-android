 /*
  * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
  * SPDX-License-Identifier: Apache-2.0
  */

 package com.amazon.apl.android.utils;

 /**
  * Class to provide information about a metric.
  */
 public class MetricInfo {
     private final String mName;
     private final double mValue;

     public MetricInfo(String  name, double value) {
         mName = name;
         mValue = value;
     }

     public String getName() {
         return mName;
     }

     public double getValue() {
         return mValue;
     }
 }
