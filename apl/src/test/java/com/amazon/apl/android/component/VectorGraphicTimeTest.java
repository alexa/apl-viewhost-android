/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.component;

import com.amazon.apl.android.APLOptions;
import com.amazon.apl.android.APLTestContext;
import com.amazon.apl.android.Component;
import com.amazon.apl.android.RootContext;
import com.amazon.apl.android.VectorGraphic;
import com.amazon.apl.android.robolectric.ViewhostRobolectricTest;
import com.amazon.apl.android.utils.TestClock;
import com.amazon.apl.enums.ComponentType;

import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import static com.amazon.apl.enums.PropertyKey.kPropertyGraphic;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;

public class VectorGraphicTimeTest extends ViewhostRobolectricTest {

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
    }

    private final static String TIME_TEST =
            "{" +
                    "  \"type\": \"APL\"," +
                    "  \"version\": \"1.3\"," +
                    "  \"graphics\": {" +
                    "    \"clock\": {" +
                    "      \"description\": \"Live analog clock\"," +
                    "      \"type\": \"AVG\"," +
                    "      \"version\": \"1.0\"," +
                    "      \"height\": 100," +
                    "      \"width\": 100," +
                    "      \"item\": {" +
                    "        \"type\": \"group\"," +
                    "        \"rotation\": \"${Time.seconds(localTime)*6}\"," +
                    "        \"pivotX\": 50," +
                    "        \"pivotY\": 50," +
                    "        \"items\": {" +
                    "          \"type\": \"path\"," +
                    "          \"pathData\": \"M50,0 l0,50\"," +
                    "          \"stroke\": \"red\"" +
                    "        }" +
                    "      }" +
                    "    }" +
                    "  }," +
                    "  \"mainTemplate\": {" +
                    "    \"items\": {" +
                    "      \"type\": \"VectorGraphic\"," +
                    "      \"source\": \"clock\"," +
                    "      \"width\": \"100%\"," +
                    "      \"height\": \"100%\"," +
                    "      \"scale\": \"best-fit\"," +
                    "      \"align\": \"center\"" +
                    "    }" +
                    "  }" +
                    "}";

    /**
     * A popular use of a vector graphic is to create a clock.  This clock example uses
     * the "localTime" global property to move the second hand directly.
     */
    @Test
    public void graphicTest_time() {

        RootContext rootCtx = new APLTestContext()
                .setDocument(TIME_TEST)
                .setAplOptions(APLOptions.builder().aplClockProvider(TestClock::new).build())
                .buildRootContext();

        Component box = rootCtx.getTopComponent();
        assertNotNull(box);
        assertEquals(ComponentType.kComponentTypeVectorGraphic, box.getComponentType());
        VectorGraphic vectorGraphic = (VectorGraphic) box;

        // Advance core time, but do not process results
        rootCtx.testUtil_updateFrameTime(3000);

        // Verify dirty root, component, and properties
        assertTrue(rootCtx.isDirty());
        assertTrue(vectorGraphic.checkDirty());
        assertTrue(vectorGraphic.checkDirtyProperty(kPropertyGraphic));

        // Execute normal frame processing for the same time
        rootCtx.onTick(3000);

        // Verify updates are applied
        assertFalse(rootCtx.isDirty());
        assertFalse(vectorGraphic.checkDirty());
        assertFalse(vectorGraphic.checkDirtyProperty(kPropertyGraphic));
    }

    private static final String PARAMETERIZED_TIME =
            "{" +
                    "  \"type\": \"APL\"," +
                    "  \"version\": \"1.3\"," +
                    "  \"graphics\": {" +
                    "    \"clock\": {" +
                    "      \"type\": \"AVG\"," +
                    "      \"version\": \"1.0\"," +
                    "      \"height\": 100," +
                    "      \"width\": 100," +
                    "      \"parameters\": [" +
                    "        \"time\"" +
                    "      ]," +
                    "      \"item\": {" +
                    "        \"type\": \"group\"," +
                    "        \"rotation\": \"${Time.seconds(time)*6}\"," +
                    "        \"pivotX\": 50," +
                    "        \"pivotY\": 50," +
                    "        \"items\": {" +
                    "          \"type\": \"path\"," +
                    "          \"pathData\": \"M50,0 l0,50\"," +
                    "          \"stroke\": \"red\"" +
                    "        }" +
                    "      }" +
                    "    }" +
                    "  }," +
                    "  \"mainTemplate\": {" +
                    "    \"items\": {" +
                    "      \"type\": \"VectorGraphic\"," +
                    "      \"source\": \"clock\"," +
                    "      \"width\": \"100%\"," +
                    "      \"height\": \"100%\"," +
                    "      \"scale\": \"best-fit\"," +
                    "      \"align\": \"center\"," +
                    "      \"time\": \"${localTime + 30000}\"" +
                    "    }" +
                    "  }" +
                    "}";

    /**
     * This clock test passes the time as a parameter in from the mainTemplate
     */
    @Test
    public void graphicTest_parameterizedTime() {
        RootContext rootCtx = new APLTestContext()
                .setDocument(PARAMETERIZED_TIME)
                .setAplOptions(APLOptions.builder().aplClockProvider(TestClock::new).build())
                .buildRootContext();

        Component box = rootCtx.getTopComponent();
        assertNotNull(box);
        assertEquals(ComponentType.kComponentTypeVectorGraphic, box.getComponentType());
        VectorGraphic vectorGraphic = (VectorGraphic) box;

        // Advance core time, but do not process results
        rootCtx.testUtil_updateFrameTime(3000);

        // Verify dirty root, component, and properties
        assertTrue(rootCtx.isDirty());
        assertTrue(vectorGraphic.checkDirty());
        assertTrue(vectorGraphic.checkDirtyProperty(kPropertyGraphic));

        // Execute normal frame processing for the same time
        rootCtx.onTick(3000);

        // Verify updates are applied
        assertFalse(rootCtx.isDirty());
        assertFalse(vectorGraphic.checkDirty());
        assertFalse(vectorGraphic.checkDirtyProperty(kPropertyGraphic));
    }

    private static final String FULL_CLOCK =
            "{" +
                    "  \"type\": \"APL\"," +
                    "  \"version\": \"1.2\"," +
                    "  \"graphics\": {" +
                    "    \"clock\": {" +
                    "      \"type\": \"AVG\"," +
                    "      \"version\": \"1.0\"," +
                    "      \"parameters\": [" +
                    "        \"time\"" +
                    "      ]," +
                    "      \"width\": 100," +
                    "      \"height\": 100," +
                    "      \"items\": [" +
                    "        {" +
                    "          \"type\": \"group\"," +
                    "          \"description\": \"MinuteHand\"," +
                    "          \"rotation\": \"${Time.minutes(time) * 6}\"," +
                    "          \"pivotX\": 50," +
                    "          \"pivotY\": 50," +
                    "          \"items\": {" +
                    "            \"type\": \"path\"," +
                    "            \"pathData\": \"M48.5,7 L51.5,7 L51.5,50 L48.5,50 L48.5,7 Z\"," +
                    "            \"fill\": \"orange\"" +
                    "          }" +
                    "        }," +
                    "        {" +
                    "          \"type\": \"group\"," +
                    "          \"description\": \"HourHand\"," +
                    "          \"rotation\": \"${Time.hours(time) * 30}\"," +
                    "          \"pivotX\": 50," +
                    "          \"pivotY\": 50," +
                    "          \"items\": {" +
                    "            \"type\": \"path\"," +
                    "            \"pathData\": \"M48.5,17 L51.5,17 L51.5,50 L48.5,50 L48.5,17 Z\"," +
                    "            \"fill\": \"black\"" +
                    "          }" +
                    "        }," +
                    "        {" +
                    "          \"type\": \"group\"," +
                    "          \"description\": \"SecondHand\"," +
                    "          \"rotation\": \"${Time.seconds(time) * 6}\"," +
                    "          \"pivotX\": 50," +
                    "          \"pivotY\": 50," +
                    "          \"items\": {" +
                    "            \"type\": \"path\"," +
                    "            \"pathData\": \"M49.5,15 L50.5,15 L50.5,60 L49.5,60 L49.5,15 Z\"," +
                    "            \"fill\": \"red\"" +
                    "          }" +
                    "        }," +
                    "        {" +
                    "          \"type\": \"path\"," +
                    "          \"description\": \"Cap\"," +
                    "          \"pathData\": \"M50,53 C51.656854,53 53,51.6568542 53,50 C53,48.3431458 51.656854,47 50,47 C48.343146,47 47,48.3431458 47,50 C47,51.6568542 48.343146,53 50,53 Z\"," +
                    "          \"fill\": \"#d8d8d8ff\"," +
                    "          \"stroke\": \"#e6e6e6ff\"," +
                    "          \"strokeWidth\": 1" +
                    "        }" +
                    "      ]" +
                    "    }" +
                    "  }," +
                    "  \"mainTemplate\": {" +
                    "    \"parameters\": [" +
                    "      \"payload\"" +
                    "    ]," +
                    "    \"items\": {" +
                    "      \"type\": \"VectorGraphic\"," +
                    "      \"source\": \"clock\"," +
                    "      \"width\": \"100%\"," +
                    "      \"height\": \"100%\"," +
                    "      \"scale\": \"best-fit\"," +
                    "      \"align\": \"center\"," +
                    "      \"time\": \"${localTime + 1000 * (payload.seconds + 60 * payload.minutes + 3600 * payload.hours)}\"" +
                    "    }" +
                    "  }" +
                    "}";

    private final static String FULL_CLOCK_PAYLOAD =
            "{\"hours\": 1, \"minutes\": 20, \"seconds\": 30})\"";

    /**
     * Sanity check a clock with a second, minute, and hour hand.  We pass in a payload that specifies the
     * exact hours, minutes, and seconds we wish to set
     */
    @Test
    public void graphicTest_fullClock() {
        RootContext rootCtx = new APLTestContext()
                .setDocument(FULL_CLOCK)
                .setAplOptions(APLOptions.builder().aplClockProvider(TestClock::new).build())
                .setDocumentPayload("payload", FULL_CLOCK_PAYLOAD)
                .buildRootContext();

        Component box = rootCtx.getTopComponent();
        assertNotNull(box);
        assertEquals(ComponentType.kComponentTypeVectorGraphic, box.getComponentType());
        VectorGraphic vectorGraphic = (VectorGraphic) box;

        // Now advance local time by one hour, one minute, and one second
        for (int i = 1; i < 10; i++) {

            rootCtx.testUtil_updateFrameTime((3600 + 60 + 1) * 1000 * i);

            // verify root is dirty
            assertTrue("loop=" + i, rootCtx.isDirty());

            // verify component is dirty
            assertTrue("loop=" + i, vectorGraphic.checkDirty());
            assertTrue("loop=" + i, vectorGraphic.checkDirtyProperty(kPropertyGraphic));

            // Execute normal frame processing for the same time
            rootCtx.onTick((3600 + 60 + 1) * 1000 * i);


            // verify root is clean
            assertFalse("loop=" + i, rootCtx.isDirty());

            // verify component is dirty
            assertFalse("loop=" + i, vectorGraphic.checkDirty());
            assertFalse("loop=" + i, vectorGraphic.checkDirtyProperty(kPropertyGraphic));
        }
    }
}
