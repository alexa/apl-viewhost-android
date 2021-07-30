/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.focus;

import android.view.KeyEvent;

import org.junit.Test;

/**
 * Tests focus for a GridSequence.
 */
public class GridSequenceFocusTest extends ComponentFocusTest {
    @Override
    String componentProps() {
        return "\"type\": \"Container\",\n" +
                "      \"width\": \"100vw\",\n" +
                "      \"height\": \"100vh\",\n" +
                "      \"items\": [\n" +
                "        {\n" +
                "          \"type\": \"GridSequence\",\n" +
                "          \"width\": \"100vw\",\n" +
                "          \"height\": \"100vh\",\n" +
                "          \"id\": \"comp\",\n" +
                "          \"scrollDirection\": \"vertical\",\n" +
                "          \"handleKeyUp\": [{ " +
                "            \"when\": \"${event.keyboard.code == 'KeyS'}\"," +
                "            \"commands\": [{ " +
                "              \"type\": \"SendEvent\"," +
                "              \"arguments\": \"parent\"" +
                "            }]" +
                "          }]," +
                "          \"childWidths\": [\n" +
                "            \"auto\",\n" +
                "            \"auto\",\n" +
                "            \"auto\"\n" +
                "          ],\n" +
                "          \"childHeight\": \"5vh\",\n" +
                "          \"snap\": \"start\",\n" +
                "          \"data\": " + dataFor6Items() +",\n" +
                "          \"items\": [\n" +
                "            {\n" +
                "              \"type\": \"TouchWrapper\",\n" +
                "              \"id\": \"grid_${index}\",\n" +
                "              \"onPress\": {\n" +
                "                \"type\": \"SendEvent\",\n" +
                "                \"arguments\": [\n" +
                "                  \"args ${index}\"\n" +
                "                ]\n" +
                "              },\n" +
                "                \"handleKeyDown\": [{ " +
                "                  \"when\": \"${event.keyboard.code == 'KeyW'}\"," +
                "                  \"commands\": [{ " +
                "                    \"type\": \"SendEvent\"," +
                "                    \"arguments\": \"${data.text}\"" +
                "                  }]" +
                "                }]," +
                "              \"item\": {\n" +
                "                \"type\": \"Text\",\n" +
                "                \"style\": \"textStylePressable\",\n" +
                "                \"inheritParentState\": true,\n" +
                "                \"fontSize\": 24,\n" +
                "                \"text\": \"${index + 1}. ${data.text}\"\n" +
                "              }\n" +
                "            }\n" +
                "          ]\n" +
                "        }\n" +
                "      ]";
    }

    @Test
    public void test_initialFocus() {
        // Initial focus state all text black
        checkFocusedComponent(-1);
    }

    @Test
    public void test_initialFocus_down() {
        pressKey(KeyEvent.KEYCODE_DPAD_DOWN);

        // Top left
        checkFocusedComponent(0);
    }

    @Test
    public void test_initialFocus_up() {
        pressKey(KeyEvent.KEYCODE_DPAD_UP);

        // Top left
        checkFocusedComponent(0);
    }

    @Test
    public void test_focusNavigation() {
        pressKey(KeyEvent.KEYCODE_DPAD_DOWN);

        checkFocusedComponent(0);

        pressKey(KeyEvent.KEYCODE_DPAD_DOWN);

        checkFocusedComponent(3);

        pressKey(KeyEvent.KEYCODE_DPAD_DOWN);

        checkFocusedComponent(3);

        pressKey(KeyEvent.KEYCODE_DPAD_RIGHT);

        checkFocusedComponent(4);

        pressKey(KeyEvent.KEYCODE_DPAD_UP);

        checkFocusedComponent(1);

        pressKey(KeyEvent.KEYCODE_DPAD_RIGHT);

        checkFocusedComponent(2);

        pressKey(KeyEvent.KEYCODE_DPAD_DOWN);

        checkFocusedComponent(5);
    }

    @Test
    public void test_volumeKey_doesNotSetFocus() {
        pressKey(KeyEvent.KEYCODE_VOLUME_DOWN);

        checkFocusedComponent(-1);

        pressKey(KeyEvent.KEYCODE_VOLUME_UP);

        checkFocusedComponent(-1);
    }
}
