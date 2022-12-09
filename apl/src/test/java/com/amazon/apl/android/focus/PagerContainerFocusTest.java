/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.focus;

import android.view.KeyEvent;

import org.junit.Test;

/**
 * Tests focus for a Pager with Touchables in a Container.
 */
public class PagerContainerFocusTest extends ComponentFocusTest {
    @Override
    String componentProps() {
        return "\"type\": \"Container\",\n" +
                "      \"width\": \"100vw\",\n" +
                "      \"height\": \"100vh\",\n" +
                "      \"items\": [\n" +
                "        {\n" +
                "          \"type\": \"Pager\",\n" +
                "          \"width\": \"100vw\",\n" +
                "          \"height\": \"100vh\",\n" +
                "          \"handleKeyUp\": [{ " +
                "            \"when\": \"${event.keyboard.code == 'KeyS'}\"," +
                "            \"commands\": [{ " +
                "              \"type\": \"SendEvent\"," +
                "              \"arguments\": \"parent\"" +
                "            }]" +
                "          }]," +
                "          \"items\": {\n" +
                "            \"type\": \"Container\",\n" +
                "            \"width\": \"100vw\",\n" +
                "            \"height\": \"100vh\",\n" +
                "            \"id\": \"comp\",\n" +
                "            \"data\": " + dataFor6Items() + ",\n" +
                "            \"items\": [\n" +
                "              {\n" +
                "                \"type\": \"TouchWrapper\",\n" +
                "                \"id\": \"grid_${index}\",\n" +
                "                \"onPress\": {\n" +
                "                  \"type\": \"SendEvent\",\n" +
                "                  \"arguments\": [\n" +
                "                    \"${data.text}\"\n" +
                "                  ]\n" +
                "                },\n" +
                "                \"handleKeyDown\": [{ " +
                "                  \"when\": \"${event.keyboard.code == 'KeyW'}\"," +
                "                  \"commands\": [{ " +
                "                    \"type\": \"SendEvent\"," +
                "                    \"arguments\": \"${data.text}\"" +
                "                  }]" +
                "                }]," +
                "                \"item\": {\n" +
                "                  \"type\": \"Text\",\n" +
                "                  \"style\": \"textStylePressable\",\n" +
                "                  \"inheritParentState\": true,\n" +
                "                  \"fontSize\": 24,\n" +
                "                  \"text\": \"${index + 1}. ${data.text}\"\n" +
                "                }\n" +
                "              }\n" +
                "            ]\n" +
                "          }\n" +
                "        }\n" +
                "      ]";
    }

    @Test
    public void test_initialFocus_down() {
        pressKey(KeyEvent.KEYCODE_DPAD_DOWN);

        // Top
        checkFocusedComponent(0);
    }

    @Test
    public void test_initialFocus_up() {
        pressKey(KeyEvent.KEYCODE_DPAD_UP);

        // Still top
        checkFocusedComponent(0);
    }

    @Test
    public void test_focusNavigation() {
        for (int i = 0; i < 6; i++) {
            pressKey(KeyEvent.KEYCODE_DPAD_DOWN);

            checkFocusedComponent(i);
        }

        for (int i = 4; i >= 0; i--) {
            pressKey(KeyEvent.KEYCODE_DPAD_UP);

            checkFocusedComponent(i);
        }
    }
}
