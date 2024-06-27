/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.viewhost.internal;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class APLPropertiesTest {
    private APLProperties mProperties;

    @Before
    public void setup() {
        mProperties = new APLProperties();
    }

    @Test
    public void test_set_get() {
        mProperties.set(APLProperty.kFluidityIncidentUpsThreshold, 1.2d);
        assertEquals(mProperties.get(APLProperty.kFluidityIncidentUpsThreshold), 1.2d);
        mProperties.set(APLProperty.kFluidityIncidentUpsThreshold, 2.1d);
        assertEquals(mProperties.get(APLProperty.kFluidityIncidentUpsThreshold), 2.1d);
    }

    @Test
    public void test_getDouble_validDouble() {
        mProperties.set(APLProperty.kFluidityIncidentUpsThreshold, 1.5d);
        assertEquals(mProperties.getDouble(APLProperty.kFluidityIncidentUpsThreshold), new Double(1.5));
    }

    @Test
    public void test_getDouble_invalid() {
        mProperties.set(APLProperty.kFluidityIncidentUpsThreshold, "1.5d");
        assertEquals(mProperties.getDouble(APLProperty.kFluidityIncidentUpsThreshold), null);
    }

    @Test
    public void test_set_invalidProperty() {
        String key = "invalidProperty";
        // Verify that this key doesn't exist
        assertEquals(APLProperty.sPropertyToEnumMap.containsKey(key), false);

        // Insertion failed
        assertEquals(mProperties.set(key, 0), false);
    }

    @Test
    public void test_set_validProperty() {
        String key = APLProperty.sPropertyToEnumMap.inverse().get(APLProperty.kFluidityIncidentUpsThreshold);

        // Insertion succeeded
        assertEquals(mProperties.set(key, 10d), true);
        assertEquals(mProperties.getDouble(APLProperty.kFluidityIncidentUpsThreshold), new Double(10));
    }
}
