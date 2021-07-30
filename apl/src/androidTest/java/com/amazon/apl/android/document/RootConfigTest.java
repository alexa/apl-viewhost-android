/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.document;

import com.amazon.apl.android.RootConfig;
import com.amazon.apl.enums.AnimationQuality;
import com.amazon.apl.enums.RootProperty;
import com.amazon.apl.enums.ScreenMode;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class RootConfigTest implements BoundObjectDefaultTest {

//    private static final String TAG = RootConfigTest.class.getSimpleName();

    static {
        System.loadLibrary("apl-jni");
    }


    @Before
    public void doBefore() {

    }


    @Override
    public long createBoundObjectHandle() {
        RootConfig rootConfig = RootConfig.create("Test", "1.0");
        long handle = rootConfig.getNativeHandle();
        return handle;
    }

    @Test
    @SmallTest
    public void testRootConfig_propertiesCase() {
        long now = System.currentTimeMillis();
        long offset = 0;

        RootConfig rootConfig = RootConfig.create("Test", "1.0")
                .disallowVideo(true)
                .allowOpenUrl(false)
                .animationQuality(AnimationQuality.kAnimationQualitySlow)
                .utcTime(now)
                .localTimeAdjustment(offset)
                .fontScale(2.0f)
                .screenMode(ScreenMode.kScreenModeHighContrast)
                .screenReader(true)
                .doublePressTimeout(1000)
                .longPressTimeout(2000)
                .minimumFlingVelocity(100)
                .pressedDuration(256)
                .tapOrScrollTimeout(300);

        assertEquals("Test", rootConfig.getAgentName());
        assertEquals("1.0", rootConfig.getAgentVersion());
        assertTrue(rootConfig.getDisallowVideo());
        assertFalse(rootConfig.getAllowOpenUrl());
        assertEquals(AnimationQuality.kAnimationQualitySlow, rootConfig.getAnimationQuality());
        assertEquals(now, rootConfig.getUTCTime());
        assertEquals(0, rootConfig.getLocalTimeAdjustment());
        assertEquals(2.0, rootConfig.getFontScale(), 0.01);
        assertEquals("high-contrast", rootConfig.getScreenMode());
        assertEquals(true, rootConfig.getScreenReader());
        assertEquals(1000, rootConfig.getDoublePressTimeout());
        assertEquals(2000, rootConfig.getLongPressTimeout());
        assertEquals(100, rootConfig.getMinimumFlingVelocity());
        assertEquals(256, rootConfig.getPressedDuration());
        assertEquals(300, rootConfig.getTapOrScrollTimeout());
    }


    @Test
    @SmallTest
    public void testRootConfig_setByProperty() {
        long now = System.currentTimeMillis();
        long offset = 0;

        RootConfig rootConfig = RootConfig.create("Test", "1.0")
                .set(RootProperty.kDisallowVideo, true)
                .set(RootProperty.kAllowOpenUrl, false)
                .set(RootProperty.kAnimationQuality, AnimationQuality.kAnimationQualitySlow)
                .set(RootProperty.kUTCTime, now)
                .set(RootProperty.kLocalTimeAdjustment, offset)
                .set(RootProperty.kFontScale, 2.0f)
                .set(RootProperty.kScreenMode, ScreenMode.kScreenModeHighContrast)
                .set(RootProperty.kScreenReader, true)
                .set(RootProperty.kDoublePressTimeout, 1000)
                .set(RootProperty.kLongPressTimeout, 2000)
                .set(RootProperty.kMinimumFlingVelocity, 100)
                .set(RootProperty.kPressedDuration, 256)
                .set(RootProperty.kTapOrScrollTimeout, 300);

        assertEquals("Test", rootConfig.getAgentName());
        assertEquals("1.0", rootConfig.getAgentVersion());
        assertTrue(rootConfig.getDisallowVideo());
        assertFalse(rootConfig.getAllowOpenUrl());
        assertEquals(AnimationQuality.kAnimationQualitySlow, rootConfig.getAnimationQuality());
        assertEquals(now, rootConfig.getUTCTime());
        assertEquals(0, rootConfig.getLocalTimeAdjustment());
        assertEquals(2.0, rootConfig.getFontScale(), 0.01);
        assertEquals("high-contrast", rootConfig.getScreenMode());
        assertEquals(true, rootConfig.getScreenReader());
        assertEquals(1000, rootConfig.getDoublePressTimeout());
        assertEquals(2000, rootConfig.getLongPressTimeout());
        assertEquals(100, rootConfig.getMinimumFlingVelocity());
        assertEquals(256, rootConfig.getPressedDuration());
        assertEquals(300, rootConfig.getTapOrScrollTimeout());
    }

    @Test
    @SmallTest
    public void testRootConfig_setByName() {
        long now = System.currentTimeMillis();
        long offset = 0;

        RootConfig rootConfig = RootConfig.create("Test", "1.0")
                .set("disallowVideo", true)
                .set("allowOpenUrl", false)
                .set("animationQuality", AnimationQuality.kAnimationQualitySlow)
                .set("utcTime", now)
                .set("localTimeAdjustment", offset)
                .set("fontScale", 2.0f)
                .set("screenMode", ScreenMode.kScreenModeHighContrast)
                .set("screenReader", true)
                .set("doublePressTimeout", 1000)
                .set("longPressTimeout", 2000)
                .set("fling.minimumVelocity", 100)
                .set("pressedDuration", 256)
                .set("tapOrScrollTimeout", 300);

        assertEquals("Test", rootConfig.getAgentName());
        assertEquals("1.0", rootConfig.getAgentVersion());
        assertTrue(rootConfig.getDisallowVideo());
        assertFalse(rootConfig.getAllowOpenUrl());
        assertEquals(AnimationQuality.kAnimationQualitySlow, rootConfig.getAnimationQuality());
        assertEquals(now, rootConfig.getUTCTime());
        assertEquals(0, rootConfig.getLocalTimeAdjustment());
        assertEquals(2.0, rootConfig.getFontScale(), 0.01);
        assertEquals("high-contrast", rootConfig.getScreenMode());
        assertEquals(true, rootConfig.getScreenReader());
        assertEquals(1000, rootConfig.getDoublePressTimeout());
        assertEquals(2000, rootConfig.getLongPressTimeout());
        assertEquals(100, rootConfig.getMinimumFlingVelocity());
        assertEquals(256, rootConfig.getPressedDuration());
        assertEquals(300, rootConfig.getTapOrScrollTimeout());
    }

    @Test
    @SmallTest
    public void testRootConfig_setByMap() {
        long now = System.currentTimeMillis();
        long offset = 0;

        Map<RootProperty, Object> propMap = new HashMap<>();
        propMap.put(RootProperty.kDisallowVideo, true);
        propMap.put(RootProperty.kAllowOpenUrl, false);
        propMap.put(RootProperty.kAnimationQuality, AnimationQuality.kAnimationQualitySlow);
        propMap.put(RootProperty.kUTCTime, now);
        propMap.put(RootProperty.kLocalTimeAdjustment, offset);
        propMap.put(RootProperty.kFontScale, 2.0f);
        propMap.put(RootProperty.kScreenMode, ScreenMode.kScreenModeHighContrast);
        propMap.put(RootProperty.kScreenReader, true);
        propMap.put(RootProperty.kDoublePressTimeout, 1000);
        propMap.put(RootProperty.kLongPressTimeout, 2000);
        propMap.put(RootProperty.kMinimumFlingVelocity, 100);
        propMap.put(RootProperty.kPressedDuration, 256);
        propMap.put(RootProperty.kTapOrScrollTimeout, 300);

        RootConfig rootConfig = RootConfig.create("Test", "1.0")
                .set(propMap);

        assertEquals("Test", rootConfig.getAgentName());
        assertEquals("1.0", rootConfig.getAgentVersion());
        assertTrue(rootConfig.getDisallowVideo());
        assertFalse(rootConfig.getAllowOpenUrl());
        assertEquals(AnimationQuality.kAnimationQualitySlow, rootConfig.getAnimationQuality());
        assertEquals(now, rootConfig.getUTCTime());
        assertEquals(0, rootConfig.getLocalTimeAdjustment());
        assertEquals(2.0, rootConfig.getFontScale(), 0.01);
        assertEquals("high-contrast", rootConfig.getScreenMode());
        assertEquals(true, rootConfig.getScreenReader());
        assertEquals(1000, rootConfig.getDoublePressTimeout());
        assertEquals(2000, rootConfig.getLongPressTimeout());
        assertEquals(100, rootConfig.getMinimumFlingVelocity());
        assertEquals(256, rootConfig.getPressedDuration());
        assertEquals(300, rootConfig.getTapOrScrollTimeout());
    }

    @Test
    @SmallTest
    public void testRootConfig_defaults() {
        long now = System.currentTimeMillis();
        Calendar cal = Calendar.getInstance();
        long offset = cal.get(Calendar.ZONE_OFFSET) + cal.get(Calendar.DST_OFFSET);

        RootConfig rootConfig = RootConfig.create();

        assertNotNull(rootConfig.getProperty(RootProperty.kAgentName));
        assertNotNull(rootConfig.getProperty(RootProperty.kAgentVersion));
        assertFalse((Boolean)rootConfig.getProperty(RootProperty.kDisallowVideo));
        assertFalse((Boolean)rootConfig.getProperty(RootProperty.kAllowOpenUrl));
        assertEquals(AnimationQuality.kAnimationQualityNormal, AnimationQuality.valueOf((Integer)rootConfig.getProperty(RootProperty.kAnimationQuality)));
        assertEquals(offset, ((Number)rootConfig.getProperty(RootProperty.kLocalTimeAdjustment)).longValue());
        assertTrue(Math.abs(rootConfig.getUTCTime() - now) < 100);
        assertEquals(1.0, ((Number)rootConfig.getProperty(RootProperty.kFontScale)).floatValue(), 0.01);
        assertEquals(ScreenMode.kScreenModeNormal, ScreenMode.valueOf((Integer)rootConfig.getProperty(RootProperty.kScreenMode)));
        assertEquals(false, rootConfig.getProperty(RootProperty.kScreenReader));
    }

    @Test
    @SmallTest
    public void testRootConfig_defaults_withContext() {
        long now = System.currentTimeMillis();
        Calendar cal = Calendar.getInstance();
        long offset = cal.get(Calendar.ZONE_OFFSET) + cal.get(Calendar.DST_OFFSET);

        RootConfig rootConfig = RootConfig.create(InstrumentationRegistry.getInstrumentation().getContext());

        assertNotNull(rootConfig.getAgentName());
        assertNotNull(rootConfig.getAgentVersion());
        assertFalse(rootConfig.getDisallowVideo());
        assertFalse(rootConfig.getAllowOpenUrl());
        assertEquals(AnimationQuality.kAnimationQualityNormal, rootConfig.getAnimationQuality());
        assertEquals(offset, rootConfig.getLocalTimeAdjustment());
        assertTrue(Math.abs(rootConfig.getUTCTime() - now) < 100);
        // may vary per device configuration
        assertEquals(1.0, rootConfig.getFontScale(), 0.01);
        assertEquals("normal", rootConfig.getScreenMode());
        assertEquals(false, rootConfig.getScreenReader());
    }

    @Test
    @SmallTest
    public void test_userGuideTest() {
        final String message = "this example is used in the User Guide. Should it fail, or need " +
                "to be refactored the User Guide must be updated with any code changes";

        // start User Guide example 1
        RootConfig rootConfig = RootConfig.create("MyAgent", "1.1");
        // end user guide example1

        assertEquals("MyAgent", rootConfig.getAgentName());
        assertEquals("1.1", rootConfig.getAgentVersion());

        long now = System.currentTimeMillis();

        // start User Guide example 2
        Calendar cal = Calendar.getInstance();
        long offset = cal.get(Calendar.ZONE_OFFSET) + cal.get(Calendar.DST_OFFSET);

        RootConfig rootConfig2 = RootConfig.create("Test", "1.0")
                .disallowVideo(true)
                .allowOpenUrl(true)
                .animationQuality(AnimationQuality.kAnimationQualitySlow)
                .utcTime(System.currentTimeMillis())
                .localTimeAdjustment(offset);
        // end user guide example 2

        assertNotNull(message, rootConfig.getAgentName());
        assertNotNull(message, rootConfig.getAgentVersion());
        assertTrue(message, rootConfig2.getDisallowVideo());
        assertTrue(message, rootConfig2.getAllowOpenUrl());
        assertEquals(message, AnimationQuality.kAnimationQualitySlow, rootConfig2.getAnimationQuality());
        assertEquals(message, offset, rootConfig2.getLocalTimeAdjustment());
        assertTrue(message, Math.abs(rootConfig2.getUTCTime() - now) < 100);


    }


}
