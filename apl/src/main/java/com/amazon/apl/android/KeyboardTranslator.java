/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.collection.ArrayMap;
import android.util.Log;
import android.view.KeyEvent;

import com.amazon.apl.enums.KeyHandlerType;

import java.util.Map;

/**
 *  KeyEventHandler class maps Android OS Keys to APL Core Keys.
 *
 *  Android OS Keys: https://developer.android.com/reference/kotlin/android/view/KeyEvent
 *  APL Core Keys (based on web standards): https://developer.mozilla.org/en-US/docs/Web/API/Document/keydown_event
 */
public final class KeyboardTranslator {

    private final static String TAG = "KeyboardTranslator";
    private static final KeyboardTranslator INSTANCE = new KeyboardTranslator();

    private static final Map<Integer, APLKey> sAndroidKeyToAPLKeyMap = new ArrayMap<>(288);
    static {
        put(1, new APLKey("SoftLeft", "SoftLeft"));
        put(2, new APLKey("SoftRight", "SoftRight"));
        put(3, new APLKey("Home", "Home"));
        put(4, new APLKey("Back", "Back"));
        put(5, new APLKey("Call", "Call"));
        put(6, new APLKey("EndCall", "EndCall"));
        put(7, new APLKey("Digit0", "*"));
        put(8, new APLKey("Digit1", "*"));
        put(9, new APLKey("Digit2", "*"));
        put(10, new APLKey("Digit3", "*"));
        put(11, new APLKey("Digit4", "*"));
        put(12, new APLKey("Digit5", "*"));
        put(13, new APLKey("Digit6", "*"));
        put(14, new APLKey("Digit7", "*"));
        put(15, new APLKey("Digit8", "*"));
        put(16, new APLKey("Digit9", "*"));
        put(17, new APLKey("Star", "*"));
        put(18, new APLKey("Pound", "*"));
        put(19, new APLKey("ArrowUp", "ArrowUp"));
        put(20, new APLKey("ArrowDown", "ArrowDown"));
        put(21, new APLKey("ArrowLeft", "ArrowLeft"));
        put(22, new APLKey("ArrowRight", "ArrowRight"));
        put(23, new APLKey("Enter", "Enter"));
        put(24, new APLKey("VolumeUp", "VolumeUp"));
        put(25, new APLKey("VolumeDown", "VolumeDown"));
        put(26, new APLKey("Power", "Power"));
        put(27, new APLKey("Camera", "Camera"));
        put(28, new APLKey("Clear", "Clear"));
        put(29, new APLKey("KeyA", "*"));
        put(30, new APLKey("KeyB", "*"));
        put(31, new APLKey("KeyC", "*"));
        put(32, new APLKey("KeyD", "*"));
        put(33, new APLKey("KeyE", "*"));
        put(34, new APLKey("KeyF", "*"));
        put(35, new APLKey("KeyG", "*"));
        put(36, new APLKey("KeyH", "*"));
        put(37, new APLKey("KeyI", "*"));
        put(38, new APLKey("KeyJ", "*"));
        put(39, new APLKey("KeyK", "*"));
        put(40, new APLKey("KeyL", "*"));
        put(41, new APLKey("KeyM", "*"));
        put(42, new APLKey("KeyN", "*"));
        put(43, new APLKey("KeyO", "*"));
        put(44, new APLKey("KeyP", "*"));
        put(45, new APLKey("KeyQ", "*"));
        put(46, new APLKey("KeyR", "*"));
        put(47, new APLKey("KeyS", "*"));
        put(48, new APLKey("KeyT", "*"));
        put(49, new APLKey("KeyU", "*"));
        put(50, new APLKey("KeyV", "*"));
        put(51, new APLKey("KeyW", "*"));
        put(52, new APLKey("KeyX", "*"));
        put(53, new APLKey("KeyY", "*"));
        put(54, new APLKey("KeyZ", "*"));
        put(55, new APLKey("Comma", "*"));
        put(56, new APLKey("Period", "*"));
        put(57, new APLKey("Alt", "Alt"));
        put(58, new APLKey("Alt", "Alt"));
        put(59, new APLKey("Shift", "Shift"));
        put(60, new APLKey("Shift", "Shift"));
        put(61, new APLKey("Tab", "Tab"));
        put(62, new APLKey("Space", "Space"));
        put(63, new APLKey("Sym", "Sym"));
        put(64, new APLKey("Explorer", "Explorer"));
        put(65, new APLKey("Envelope", "Envelope"));
        put(66, new APLKey("Enter", "Enter"));
        put(67, new APLKey("Backspace", "Backspace"));
        put(68, new APLKey("Backquote", "*"));
        put(69, new APLKey("Minus", "*"));
        put(70, new APLKey("Equal", "*"));
        put(71, new APLKey("BracketLeft", "*"));
        put(72, new APLKey("BracketRight", "*"));
        put(73, new APLKey("Backslash", "*"));
        put(74, new APLKey("Semicolon", "*"));
        put(75, new APLKey("Quote", "*"));
        put(76, new APLKey("Slash", "*"));
        put(77, new APLKey("At", "*"));
        put(78, new APLKey("Num", "Num"));
        put(79, new APLKey("HeadsetHook", "HeadsetHook"));
        put(80, new APLKey("Focus", "Focus"));
        put(81, new APLKey("Plus", "*"));
        put(82, new APLKey("Menu", "Menu"));
        put(83, new APLKey("Notification", "Notification"));
        put(84, new APLKey("Search", "Search"));
        put(85, new APLKey("MediaPlayPause", "MediaPlayPause"));
        put(86, new APLKey("MediaStop", "MediaStop"));
        put(87, new APLKey("MediaNext", "MediaNext"));
        put(88, new APLKey("MediaPrevious", "MediaPrevious"));
        put(89, new APLKey("MediaRewind", "MediaRewind"));
        put(90, new APLKey("MediaFastForward", "MediaFastForward"));
        put(91, new APLKey("Mute", "Mute"));
        put(92, new APLKey("PageUp", "PageUp"));
        put(93, new APLKey("PageDown", "PageDown"));
        put(94, new APLKey("PictSymbols", "PictSymbols"));
        put(95, new APLKey("SwitchCharset", "SwitchCharset"));
        put(96, new APLKey("ButtonA", "ButtonA"));
        put(97, new APLKey("ButtonB", "ButtonB"));
        put(98, new APLKey("ButtonC", "ButtonC"));
        put(99, new APLKey("ButtonX", "ButtonX"));
        put(100, new APLKey("ButtonY", "ButtonY"));
        put(101, new APLKey("ButtonZ", "ButtonZ"));
        put(102, new APLKey("ButtonL1", "ButtonL1"));
        put(103, new APLKey("ButtonR1", "ButtonR1"));
        put(104, new APLKey("ButtonL2", "ButtonL2"));
        put(105, new APLKey("ButtonR2", "ButtonR2"));
        put(106, new APLKey("ButtonThumbL", "ButtonThumbL"));
        put(107, new APLKey("ButtonThumbR", "ButtonThumbR"));
        put(108, new APLKey("ButtonStart", "ButtonStart"));
        put(109, new APLKey("ButtonSelect", "ButtonSelect"));
        put(110, new APLKey("ButtonMode", "ButtonMode"));
        put(111, new APLKey("Escape", "Escape"));
        put(112, new APLKey("ForwardDel", "ForwardDel"));
        put(113, new APLKey("Ctrl", "Ctrl"));
        put(114, new APLKey("Ctrl", "Ctrl"));
        put(115, new APLKey("CapsLock", "CapsLock"));
        put(116, new APLKey("ScrollLock", "ScrollLock"));
        put(117, new APLKey("Meta", "Meta"));
        put(118, new APLKey("Meta", "Meta"));
        put(119, new APLKey("Function", "Function"));
        put(120, new APLKey("SysRq", "SysRq"));
        put(121, new APLKey("Break", "Break"));
        put(122, new APLKey("Home", "Home"));
        put(123, new APLKey("End", "End"));
        put(124, new APLKey("Insert", "Insert"));
        put(125, new APLKey("Forward", "Forward"));
        put(126, new APLKey("MediaPlay", "MediaPlay"));
        put(127, new APLKey("MediaPause", "MediaPause"));
        put(128, new APLKey("MediaClose", "MediaClose"));
        put(129, new APLKey("MediaEject", "MediaEject"));
        put(130, new APLKey("MediaRecord", "MediaRecord"));
        put(131, new APLKey("F1", "F1"));
        put(132, new APLKey("F2", "F2"));
        put(133, new APLKey("F3", "F3"));
        put(134, new APLKey("F4", "F4"));
        put(135, new APLKey("F5", "F5"));
        put(136, new APLKey("F6", "F6"));
        put(137, new APLKey("F7", "F7"));
        put(138, new APLKey("F8", "F8"));
        put(139, new APLKey("F9", "F9"));
        put(140, new APLKey("F10", "F10"));
        put(141, new APLKey("F11", "F11"));
        put(142, new APLKey("F12", "F12"));
        put(143, new APLKey("NumLock", "NumLock"));
        put(144, new APLKey("Numpad0", "*"));
        put(145, new APLKey("Numpad1", "*"));
        put(146, new APLKey("Numpad2", "*"));
        put(147, new APLKey("Numpad3", "*"));
        put(148, new APLKey("Numpad4", "*"));
        put(149, new APLKey("Numpad5", "*"));
        put(150, new APLKey("Numpad6", "*"));
        put(151, new APLKey("Numpad7", "*"));
        put(152, new APLKey("Numpad8", "*"));
        put(153, new APLKey("Numpad9", "*"));
        put(154, new APLKey("NumpadDivide", "*"));
        put(155, new APLKey("NumpadMultiply", "*"));
        put(156, new APLKey("NumpadSubstract", "*"));
        put(157, new APLKey("NumpadAdd", "*"));
        put(158, new APLKey("NumpadDot", "*"));
        put(159, new APLKey("NumpadComma", "*"));
        put(160, new APLKey("Enter", "Enter"));
        put(161, new APLKey("NumpadEquals", "*"));
        put(162, new APLKey("NumpadLeftParen", "*"));
        put(163, new APLKey("NumpadRightParen", "*"));
        put(164, new APLKey("VolumeMute", "VolumeMute"));
        put(165, new APLKey("Info", "Info"));
        put(166, new APLKey("ChannelUp", "ChannelUp"));
        put(167, new APLKey("ChannelDown", "ChannelDown"));
        put(168, new APLKey("ZoomIn", "ZoomIn"));
        put(169, new APLKey("ZoomOut", "ZoomOut"));
        put(170, new APLKey("TV", "TV"));
        put(171, new APLKey("Window", "Window"));
        put(172, new APLKey("Guide", "Guide"));
        put(173, new APLKey("DVR", "DVR"));
        put(174, new APLKey("Bookmark", "Bookmark"));
        put(175, new APLKey("Captions", "Captions"));
        put(176, new APLKey("Settings", "Settings"));
        put(177, new APLKey("TVPower", "TVPower"));
        put(178, new APLKey("TVInput", "TVInput"));
        put(179, new APLKey("STBPower", "STBPower"));
        put(180, new APLKey("STBInput", "STBInput"));
        put(181, new APLKey("AVRPower", "AVRPower"));
        put(182, new APLKey("AVTInput", "AVTInput"));
        put(183, new APLKey("ProgRed", "ProgRed"));
        put(184, new APLKey("ProgGreen", "ProgGreen"));
        put(185, new APLKey("ProgYellow", "ProgYellow"));
        put(186, new APLKey("ProgBlue", "ProgBlue"));
        put(187, new APLKey("AppSwitch", "AppSwitch"));
        put(188, new APLKey("Button1", "Button1"));
        put(189, new APLKey("Button2", "Button2"));
        put(190, new APLKey("Button3", "Button3"));
        put(191, new APLKey("Button4", "Button4"));
        put(192, new APLKey("Button5", "Button5"));
        put(193, new APLKey("Button6", "Button6"));
        put(194, new APLKey("Button7", "Button7"));
        put(195, new APLKey("Button8", "Button8"));
        put(196, new APLKey("Button9", "Button9"));
        put(197, new APLKey("Button10", "Button10"));
        put(198, new APLKey("Button11", "Button11"));
        put(199, new APLKey("Button12", "Button12"));
        put(200, new APLKey("Button13", "Button13"));
        put(201, new APLKey("Button14", "Button14"));
        put(202, new APLKey("Button15", "Button15"));
        put(203, new APLKey("Button16", "Button16"));
        put(204, new APLKey("LanguageSwitch", "LanguageSwitch"));
        put(205, new APLKey("MannerCode", "MannerCode"));
        put(206, new APLKey("3DMode", "3DMode"));
        put(207, new APLKey("Contacts", "Contacts"));
        put(208, new APLKey("Calendar", "Calendar"));
        put(209, new APLKey("Music", "Music"));
        put(210, new APLKey("Calculator", "Calculator"));
        put(211, new APLKey("ZenkakuHankuku", "ZenkakuHankuku"));
        put(212, new APLKey("Eisu", "Eisu"));
        put(213, new APLKey("Muhenkan", "Muhenkan"));
        put(214, new APLKey("Henkan", "Henkan"));
        put(215, new APLKey("KatakanaHiragana", "KatakanaHiragana"));
        put(216, new APLKey("Yen", "Yen"));
        put(217, new APLKey("Ro", "Ro"));
        put(218, new APLKey("Kana", "Kana"));
        put(219, new APLKey("Assist", "Assist"));
        put(220, new APLKey("BrightnessDown", "BrightnessDown"));
        put(221, new APLKey("BrightnessUp", "BrightnessUp"));
        put(222, new APLKey("MediaAudioTrack", "MediaAudioTrack"));
        put(223, new APLKey("Sleep", "Sleep"));
        put(224, new APLKey("WakeUp", "WakeUp"));
        put(225, new APLKey("Pairing", "Pairing"));
        put(226, new APLKey("MediaTopMenu", "MediaTopMenu"));
        put(227, new APLKey("11", "11"));
        put(228, new APLKey("12", "12"));
        put(229, new APLKey("LastChannel", "LastChannel"));
        put(230, new APLKey("TVDataService", "TVDataService"));
        put(231, new APLKey("VoiceAssist", "VoiceAssist"));
        put(232, new APLKey("TVRadioService", "TVRadioService"));
        put(233, new APLKey("TVTeletext", "TVTeletext"));
        put(234, new APLKey("TVNumberEntry", "TVNumberEntry"));
        put(235, new APLKey("TVTerrestrialAnalog", "TVTerrestrialAnalog"));
        put(236, new APLKey("TVTerrestrialDigital", "TVTerrestrialDigital"));
        put(237, new APLKey("TVSatellite", "TVSatellite"));
        put(238, new APLKey("TVSatelliteBS", "TVSatelliteBS"));
        put(239, new APLKey("TVSatelliteCS", "TVSatelliteCS"));
        put(240, new APLKey("TVSatelliteService", "TVSatelliteService"));
        put(241, new APLKey("TVNetwork", "TVNetwork"));
        put(242, new APLKey("TVAntennaCable", "TVAntennaCable"));
        put(243, new APLKey("TVInputHDMI1", "TVInputHDMI1"));
        put(244, new APLKey("TVInputHDMI2", "TVInputHDMI2"));
        put(245, new APLKey("TVInputHDMI3", "TVInputHDMI3"));
        put(246, new APLKey("TVInputHDMI4", "TVInputHDMI4"));
        put(247, new APLKey("TVInputComposite1", "TVInputComposite1"));
        put(248, new APLKey("TVInputComposite2", "TVInputComposite2"));
        put(249, new APLKey("TVInputComponent1", "TVInputComponent1"));
        put(250, new APLKey("TVInputComponent2", "TVInputComponent2"));
        put(251, new APLKey("TVInputVGA1", "TVInputVGA1"));
        put(252, new APLKey("TVAudioDescription", "TVAudioDescription"));
        put(253, new APLKey("TVAudioDescriptionMixUp", "TVAudioDescriptionMixUp"));
        put(254, new APLKey("TVAudioDescriptionMixDown", "TVAudioDescriptionMixDown"));
        put(255, new APLKey("TVZoomMode", "TVZoomMode"));
        put(256, new APLKey("TVContentsMenu", "TVContentsMenu"));
        put(257, new APLKey("TVMediaContextMenu", "TVMediaContextMenu"));
        put(258, new APLKey("TVTimerProgramming", "TVTimerProgramming"));
        put(259, new APLKey("Help", "Help"));
        put(260, new APLKey("NavigatePrevious", "NavigatePrevious"));
        put(261, new APLKey("NavigateNext", "NavigateNext"));
        put(262, new APLKey("NavigateIn", "NavigateIn"));
        put(263, new APLKey("NavigateOut", "NavigateOut"));
        put(264, new APLKey("StemPrimary", "StemPrimary"));
        put(265, new APLKey("Stem1", "Stem1"));
        put(266, new APLKey("Stem2", "Stem2"));
        put(267, new APLKey("Stem3", "Stem3"));
        put(268, new APLKey("ArrowUpLeft", "ArrowUpLeft"));
        put(269, new APLKey("ArrowDownLeft", "ArrowDownLeft"));
        put(270, new APLKey("ArrowUpRight", "ArrowUpRight"));
        put(271, new APLKey("ArrowDownRight", "ArrowDownRight"));
        put(272, new APLKey("MediaSkipForward", "MediaSkipForward"));
        put(273, new APLKey("MediaSkipBackward", "MediaSkipBackward"));
        put(274, new APLKey("MediaStepForward", "MediaStepForward"));
        put(275, new APLKey("MediaStepBackward", "MediaStepBackward"));
        put(276, new APLKey("SoftSleep", "SoftSleep"));
        put(277, new APLKey("Cut", "Cut"));
        put(278, new APLKey("Copy", "Copy"));
        put(279, new APLKey("Paste", "Paste"));
        put(280, new APLKey("ArrowUp", "ArrowUp"));
        put(281, new APLKey("ArrowDown", "ArrowDown"));
        put(282, new APLKey("ArrowLeft", "ArrowLeft"));
        put(283, new APLKey("ArrowRight", "ArrowRight"));
        put(284, new APLKey("AllApps", "AllApps"));
        put(285, new APLKey("Refresh", "Refresh"));
        put(286, new APLKey("ThumbsUp", "ThumbsUp"));
        put(287, new APLKey("ThumbsDown", "ThumbsDown"));
        put(288, new APLKey("ProfileSwitch", "ProfileSwitch"));
    }

    private static void put(int keyCode, APLKey aplKey) {
        sAndroidKeyToAPLKeyMap.put(keyCode, aplKey);
    }

    private KeyboardTranslator() { }

    /**
     * @return the KeyboardTranslator instance
     */
    public static final KeyboardTranslator getInstance() {
        return INSTANCE;
    }

    @Nullable
    public APLKeyboard translate(@Nullable final KeyEvent event) {
        if (event == null) {
            Log.w(TAG, " Event must be non-null. Discard APL Key Event.");
            return null;
        }

        final String key = getKey(event);
        final String code = getCode(event);

        if (key.equals("") || code.equals("")) {
            Log.w(TAG, "Key could not be found. Android key: " + event.getKeyCode() + ", code: " + event.getKeyCode());
            return null;
        }

        final KeyHandlerType type;

        switch (event.getAction()) {
            case KeyEvent.ACTION_UP:
                type = KeyHandlerType.kKeyUp;
                break;
            case KeyEvent.ACTION_DOWN:
            default:
                type = KeyHandlerType.kKeyDown;
                break;
        }

        return APLKeyboard.builder()
                .type(type)
                .key(key)
                .code(code)
                .repeat(event.getRepeatCount() > 0)
                .shift(event.isShiftPressed())
                .alt(event.isAltPressed())
                .ctrl(event.isCtrlPressed())
                .meta(event.isMetaPressed())
                .build();
    }

    /**
     * @return The string representation of the physical key on keyboard.
     *
     * Code values are strings and are defined in the W3C Candidate Recommendation for UI Events KeyboardEvent code values:
     * https://www.w3.org/TR/uievents-code/
     */
    @NonNull
    private String getCode(@NonNull final KeyEvent event) {
        final APLKey aplKey = sAndroidKeyToAPLKeyMap.get(event.getKeyCode());

        return aplKey != null ? aplKey.code : "";
    }

    /**
     * @return The string representation of key pressed on the keyboard, taking into account modifier keys.
     *
     * Key values are strings and are defined in the W3C Candidate Recommendation for UI Events KeyboardEvent key values:
     * https://www.w3.org/TR/uievents-key/
     */
    @NonNull
    private String getKey(@NonNull final KeyEvent event) {
        final APLKey aplKey = sAndroidKeyToAPLKeyMap.get(event.getKeyCode());

        if (aplKey == null) {
            return "";
        }

        // `*` means APL will use the char generated in Unicode format
        if ("*".equals(aplKey.keyName)) {
            char character = (char) event.getUnicodeChar();

            return String.valueOf(character);
        }
        return aplKey.keyName;
    }

    private static class APLKey {

        @NonNull final String code;
        @NonNull final String keyName;

        public APLKey(@NonNull final String code, @NonNull final String keyName) {
            this.code = code;
            this.keyName = keyName;
        }
    }
}
