/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.document;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static com.amazon.apl.android.espresso.APLViewActions.finish;
import static com.amazon.apl.android.espresso.APLViewActions.waitFor;
import static com.amazon.apl.android.espresso.APLViewAssertions.isFinished;

@RunWith(AndroidJUnit4.class)
public class LoadDocumentTest extends AbstractDocViewTest {

    private static final String DOC_STRING_FUNCTIONS =
            "\"type\": \"Container\",\n" +
                    "      \"height\": \"100vh\",\n" +
                    "      \"width\": \"100vw\",\n" +
                    "      \"item\": {\n" +
                    "        \"type\": \"Container\",\n" +
                    "        \"items\": [\n" +
                    "          {\n" +
                    "            \"type\": \"Text\",\n" +
                    "            \"id\":   \"ddrtedr1\",\n " +
                    "            \"color\": \"#FF0000\",\n " +
                    "            \"text\": \"${String.toLowerCase('HELLo WoRld TESTÄÖÜß!\uD83E\uDD86')}\"\n" +
                    "          }\n" +

                    "        ]\n" +
                    "      }";

    private static final String DOC_MEM_PRESSURE =
            "\"type\": \"GridSequence\",        \"width\": \"100vw\",        \"height\": \"100vh\",        \"childWidth\":  \"50%\",        \"childHeight\": \"50%\",        \"scrollDirection\": \"vertical\",        \"snap\": \"start\",        \"data\": \"${payload.filterList}\",        \"items\": [          {            \"type\": \"Container\",            \"direction\": \"column\",            \"items\": [              {                \"type\": \"Text\",                \"text\": \"${data.text}\"              },              {                \"type\": \"Image\",                \"height\": \"200dp\",                \"width\": \"200dp\",                \"source\": \"${data.sources}\",                \"filters\": \"${data.filters}\"              }            ]          }        ]";

    private static final String DATA_MEM_PRESSURE =
            "{  \"filterList\": [    {      \"text\": \"No Filters\",      \"sources\": [        \"https://images.pexels.com/photos/132464/pexels-photo-132464.jpeg?auto=compress&cs=tinysrgb&dpr=1&w=500\"      ],      \"filters\": null    },    {      \"text\": \"Blend Normal\",      \"sources\": [        \"https://images.pexels.com/photos/439284/pexels-photo-439284.jpeg?auto=compress&cs=tinysrgb&dpr=2&h=750&w=1260\",        \"https://images.pexels.com/photos/132464/pexels-photo-132464.jpeg?auto=compress&cs=tinysrgb&dpr=1&w=500\"      ],      \"filters\": [        {          \"type\": \"Blend\"        }      ]    },    {      \"text\": \"Blend Multiply\",      \"sources\": [        \"https://images.pexels.com/photos/132464/pexels-photo-132464.jpeg?auto=compress&cs=tinysrgb&dpr=1&w=500\",        \"https://images.pexels.com/photos/439284/pexels-photo-439284.jpeg?auto=compress&cs=tinysrgb&dpr=2&h=750&w=1260\"      ],      \"filters\": [        {          \"type\": \"Blend\",          \"mode\": \"multiply\",          \"source\": -2,          \"destination\": -1        }      ]    },    {      \"text\": \"Blend Screen\",      \"sources\": [        \"https://images.pexels.com/photos/132464/pexels-photo-132464.jpeg?auto=compress&cs=tinysrgb&dpr=1&w=500\",        \"https://images.pexels.com/photos/439284/pexels-photo-439284.jpeg?auto=compress&cs=tinysrgb&dpr=2&h=750&w=1260\"      ],      \"filters\": [        {          \"type\": \"Blend\",          \"mode\": \"screen\",          \"source\": 0,          \"destination\": 1        }      ]    },    {      \"text\": \"Blend Overlay\",      \"sources\": [        \"https://images.pexels.com/photos/132464/pexels-photo-132464.jpeg?auto=compress&cs=tinysrgb&dpr=1&w=500\",        \"https://images.pexels.com/photos/439284/pexels-photo-439284.jpeg?auto=compress&cs=tinysrgb&dpr=2&h=750&w=1260\"      ],      \"filters\": [        {          \"type\": \"Blend\",          \"mode\": \"overlay\",          \"source\": -2,          \"destination\": -1        }      ]    },    {      \"text\": \"Blend Darken\",      \"sources\": [        \"https://images.pexels.com/photos/132464/pexels-photo-132464.jpeg?auto=compress&cs=tinysrgb&dpr=1&w=500\",        \"https://images.pexels.com/photos/439284/pexels-photo-439284.jpeg?auto=compress&cs=tinysrgb&dpr=2&h=750&w=1260\"      ],      \"filters\": [        {          \"type\": \"Blend\",          \"mode\": \"darken\",          \"source\": 0,          \"destination\": 1        }      ]    },    {      \"text\": \"Blend Lighten\",      \"sources\": [        \"https://images.pexels.com/photos/132464/pexels-photo-132464.jpeg?auto=compress&cs=tinysrgb&dpr=1&w=500\",        \"https://images.pexels.com/photos/439284/pexels-photo-439284.jpeg?auto=compress&cs=tinysrgb&dpr=2&h=750&w=1260\"      ],      \"filters\": [        {          \"type\": \"Blend\",          \"mode\": \"lighten\",          \"source\": 0,          \"destination\": 1        }      ]    },    {      \"text\": \"Blend Color-Dodge\",      \"sources\": [        \"https://images.pexels.com/photos/132464/pexels-photo-132464.jpeg?auto=compress&cs=tinysrgb&dpr=1&w=500\",        \"https://images.pexels.com/photos/439284/pexels-photo-439284.jpeg?auto=compress&cs=tinysrgb&dpr=2&h=750&w=1260\"      ],      \"filters\": [        {          \"type\": \"Blend\",          \"mode\": \"color-dodge\",          \"source\": 0,          \"destination\": 1        }      ]    },    {      \"text\": \"Blend Color-Burn\",      \"sources\": [        \"https://images.pexels.com/photos/132464/pexels-photo-132464.jpeg?auto=compress&cs=tinysrgb&dpr=1&w=500\",        \"https://images.pexels.com/photos/439284/pexels-photo-439284.jpeg?auto=compress&cs=tinysrgb&dpr=2&h=750&w=1260\"      ],      \"filters\": [        {          \"type\": \"Blend\",          \"mode\": \"color-burn\",          \"source\": 0,          \"destination\": 1        }      ]    },    {      \"text\": \"Blend Hard-Light\",      \"sources\": [        \"https://images.pexels.com/photos/132464/pexels-photo-132464.jpeg?auto=compress&cs=tinysrgb&dpr=1&w=500\",        \"https://images.pexels.com/photos/439284/pexels-photo-439284.jpeg?auto=compress&cs=tinysrgb&dpr=2&h=750&w=1260\"      ],      \"filters\": [        {          \"type\": \"Blend\",          \"mode\": \"hard-light\",          \"source\": 0,          \"destination\": 1        }      ]    },    {      \"text\": \"Blend Soft-Light\",      \"sources\": [        \"https://images.pexels.com/photos/132464/pexels-photo-132464.jpeg?auto=compress&cs=tinysrgb&dpr=1&w=500\",        \"https://images.pexels.com/photos/439284/pexels-photo-439284.jpeg?auto=compress&cs=tinysrgb&dpr=2&h=750&w=1260\"      ],      \"filters\": [        {          \"type\": \"Blend\",          \"mode\": \"soft-light\",          \"source\": 0,          \"destination\": 1        }      ]    },    {      \"text\": \"Blend Difference\",      \"sources\": [        \"https://images.pexels.com/photos/132464/pexels-photo-132464.jpeg?auto=compress&cs=tinysrgb&dpr=1&w=500\",        \"https://images.pexels.com/photos/439284/pexels-photo-439284.jpeg?auto=compress&cs=tinysrgb&dpr=2&h=750&w=1260\"      ],      \"filters\": [        {          \"type\": \"Blend\",          \"mode\": \"difference\",          \"source\": 0,          \"destination\": 1        }      ]    },    {      \"text\": \"Blend Exclusion\",      \"sources\": [        \"https://images.pexels.com/photos/132464/pexels-photo-132464.jpeg?auto=compress&cs=tinysrgb&dpr=1&w=500\",        \"https://images.pexels.com/photos/439284/pexels-photo-439284.jpeg?auto=compress&cs=tinysrgb&dpr=2&h=750&w=1260\"      ],      \"filters\": [        {          \"type\": \"Blend\",          \"mode\": \"exclusion\",          \"source\": 0,          \"destination\": 1        }      ]    },    {      \"text\": \"Blend Hue\",      \"sources\": [        \"https://images.pexels.com/photos/132464/pexels-photo-132464.jpeg?auto=compress&cs=tinysrgb&dpr=1&w=500\",        \"https://images.pexels.com/photos/439284/pexels-photo-439284.jpeg?auto=compress&cs=tinysrgb&dpr=2&h=750&w=1260\"      ],      \"filters\": [        {          \"type\": \"Blend\",          \"mode\": \"hue\",          \"source\": 0,          \"destination\": 1        }      ]    },    {      \"text\": \"Blend Saturation\",      \"sources\": [        \"https://images.pexels.com/photos/132464/pexels-photo-132464.jpeg?auto=compress&cs=tinysrgb&dpr=1&w=500\",        \"https://images.pexels.com/photos/439284/pexels-photo-439284.jpeg?auto=compress&cs=tinysrgb&dpr=2&h=750&w=1260\"      ],      \"filters\": [        {          \"type\": \"Blend\",          \"mode\": \"saturation\",          \"source\": 0,          \"destination\": 1        }      ]    },    {      \"text\": \"Blend Color\",      \"sources\": [        \"https://images.pexels.com/photos/132464/pexels-photo-132464.jpeg?auto=compress&cs=tinysrgb&dpr=1&w=500\",        \"https://images.pexels.com/photos/439284/pexels-photo-439284.jpeg?auto=compress&cs=tinysrgb&dpr=2&h=750&w=1260\"      ],      \"filters\": [        {          \"type\": \"Blend\",          \"mode\": \"color\",          \"source\": 0,          \"destination\": 1        }      ]    }  ]}";

    private static final String PAYLOAD_MEM_PRESSURE = "payload";

    private static final String DOC_MANY_COMPONENTS =
            "\"type\": \"ScrollView\",\n" +
            "      \"width\": \"100vw\",\n" +
            "      \"height\": \"100vh\",\n" +
            "      \"item\": {\n" +
            "        \"type\": \"Container\",\n" +
            "        \"data\": \"${Array.range(1, 1000)}\",\n" +
            "        \"item\": {\n" +
            "          \"type\": \"Text\",\n" +
            "          \"text\": \"${data}\"\n" +
            "        }\n" +
            "      }";

    /*
        tests that the JNI integration of Stringfunctions is resilient against mempressure.
        For some reason after loading a bigger doc, reloading the document with stringfunctions
        forces the rootcontext into a weird state, but only if some memory has been reused.
        Should not throw and exception.
     */
    @Test
    public void testRestoreDocument_docLoadingWithHighMemory() {
        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(inflate(DOC_STRING_FUNCTIONS, ""))
                .check(hasRootContext())
                .perform(finish(mAplController))
                .check(isFinished());

        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(inflate(DOC_MEM_PRESSURE, "", PAYLOAD_MEM_PRESSURE, DATA_MEM_PRESSURE, null))
                .check(hasRootContext());
        onView(isRoot()).perform(waitFor(2000));

        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(finish(mAplController))
                .check(isFinished());

        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(inflate(DOC_STRING_FUNCTIONS, ""))
                .check(hasRootContext());
    }

    @LargeTest
    @Test
    public void testInflateManyComponents() {
        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(inflate(DOC_MANY_COMPONENTS, ""))
                .check(hasRootContext());
    }
}
