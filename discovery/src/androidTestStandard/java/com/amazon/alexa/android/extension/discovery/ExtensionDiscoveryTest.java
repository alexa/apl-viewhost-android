/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.alexa.android.extension.discovery;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import androidx.test.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.amazon.alexa.android.extension.discovery.test.TestService;
import com.amazon.common.test.LeakRulesBaseClass;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@SuppressWarnings({"CheckStyle"})
@RunWith(AndroidJUnit4.class)
public class ExtensionDiscoveryTest extends LeakRulesBaseClass {

    private ExtensionDiscovery mDiscover = null;

    private Context mAppContext;
    private static final String TEST_PKG = "com.amazon.alexa.android.extension.discovery.test";

    // Allocate common test resources
    @Before
    public void doBefore() {
        mAppContext = InstrumentationRegistry.getTargetContext();
        mDiscover = ExtensionDiscovery.getInstance(mAppContext);
        assertNotNull(mDiscover);
    }

    // Free common test resources
    @After
    public void doAfter() {
        mDiscover.clearCache();
        mAppContext = null;
        mDiscover = null;
    }

    /**
     * Sanity test that test code didn't move to release.
     */
    @Test
    public void testAppContext() {

        // The application declared in the manifest is testOnly='true'
        assertEquals("The application for this Android module should only be exposed for testing.",
                "com.amazon.alexa.android.extension.discovery.test",
                mAppContext.getPackageName()
        );
    }


    /**
     * Singleton test.
     */
    @Test
    public void testClass_singleton() {
        int hash = mDiscover.hashCode();
        assertEquals(hash, ExtensionDiscovery.getInstance(mAppContext).hashCode());
    }

    /**
     * Discovery cache can be pre-initialized for performance.
     */
    @Test
    public void testClass_init() {
        // test before initialize call
        mDiscover.clearCache();
        assertEquals(0, mDiscover.getComponentCount());

        // test after initialize call
        assertTrue(0 < ExtensionDiscovery.getInstance(mAppContext).getComponentCount());
    }

    /**
     * Component that does not exist returns null.
     */
    @Test
    public void testGetComponentName_doesNotExist() {
        ComponentName componentName = mDiscover.getComponentName("alexatest:dne:10");
        assertNull(componentName);
    }


    /**
     * URI identifier in meta-data.
     * <meta-data
     * android:name="com.amazon.alexa.extensions.EXTENSION"
     * android:value="alexatest:simple:10" />
     */
    @Test
    public void testGetComponentName_Exists() {
        ComponentName componentName = mDiscover.getComponentName("alexatest:simple:10");
        assertNotNull(componentName);

        assertEquals(TEST_PKG, componentName.getPackageName());
        assertEquals(TestService.Simple.class.getName(), componentName.getClassName());
    }

    @Test
    public void testHasExtension_foesNotExist_NOT_PRESENT() {
        final ExtensionDiscovery.ExtensionPresence presence = mDiscover.hasExtension("alexatest:dne:10");
        assertEquals(ExtensionDiscovery.ExtensionPresence.NOT_PRESENT, presence);
    }

    @Test
    public void testHasExtension_forPresentExtension_PRESNET() {
        final ExtensionDiscovery.ExtensionPresence presence = mDiscover.hasExtension("alexatest:simple:10");
        assertEquals(ExtensionDiscovery.ExtensionPresence.PRESENT, presence);
    }

    @Test
    public void testHasExtension_forPresentDeferred_DEFERRED() {
        final ExtensionDiscovery.ExtensionPresence presence = mDiscover.hasExtension("alexatest:deferred:10");
        assertEquals(ExtensionDiscovery.ExtensionPresence.DEFERRED, presence);
    }


    /**
     * Alias identifier in meta-data
     * <meta-data
     * android:name="com.amazon.alexa.extensions.ALIAS"
     * android:value="simple2" />
     */
    @Test
    public void testDiscover_alias() {
        ComponentName componentName = mDiscover.getComponentName("simple2");
        assertNotNull(componentName);
        assertEquals(TEST_PKG, componentName.getPackageName());
        assertEquals(TestService.Simple2.class.getName(), componentName.getClassName());
    }


    /**
     * Discover from the meta-data resource a collection of extensions supported by a single service
     * <p>
     * <service android:name="com.amazon.alexa.android.extension.discovery.test.TestService$Simple2">
     * <intent-filter>
     * <action android:name="com.amazon.alexa.extensions.ACTION" />
     * </intent-filter>
     * <meta-data
     * android:name="com.amazon.alexa.extensions.EXTENSION"
     * android:resource="@array/extensionURI" />
     * </service>
     * <p>
     * values/arrays.xml
     * <resources>
     * <array name="extensionURI">
     * <item>alexatest:multi:10</item>
     * <item>alexatest:multi:20</item>
     * <item>alexatest:multi:30</item>
     * </array>
     * </resources>
     */
    @Test
    public void testGetComponentName_multi() {
        ComponentName componentName = mDiscover.getComponentName("alexatest:multi:10");
        assertNotNull(componentName);
        assertEquals(TEST_PKG, componentName.getPackageName());
        assertEquals(TestService.Multi.class.getName(), componentName.getClassName());

        componentName = mDiscover.getComponentName("alexatest:multi:20");
        assertNotNull(componentName);
        assertEquals(TEST_PKG, componentName.getPackageName());
        assertEquals(TestService.Multi.class.getName(), componentName.getClassName());

        componentName = mDiscover.getComponentName("alexatest:multi:20");
        assertNotNull(componentName);
        assertEquals(TEST_PKG, componentName.getPackageName());
        assertEquals(TestService.Multi.class.getName(), componentName.getClassName());
    }


    /**
     * Removed package cannot be discovered.
     */
    @Test
    public void testPackage_remove() {
        String uri = "alexatest:simple:10";

        // detect installed package
        ComponentName componentName = mDiscover.getComponentName(uri);
        assertNotNull(componentName);
        assertEquals(TEST_PKG, componentName.getPackageName());
        assertTrue(mDiscover.isDiscovered(uri));
        assertNotEquals(0, mDiscover.getComponentCount());

        // now remove
        ExtensionDiscovery.getInstance(mAppContext).removePackage(TEST_PKG);
        assertFalse(mDiscover.isDiscovered(uri));

        // assert no component name returned
        componentName = mDiscover.getComponentName(uri);
        assertNull(componentName);
    }


    /**
     * Added package is discovered
     */
    @Test
    public void testPackage_add() {
        String uri = "alexatest:simple:10";

        // before initialization
        int baseInfoCount;

        // trigger initialization
        mDiscover.getComponentName(uri);
        baseInfoCount = ExtensionDiscovery.getInstance(mAppContext).getComponentCount();
        assertNotEquals(baseInfoCount, 0);

        // remove
        mDiscover.removePackage(TEST_PKG);
        ComponentName componentName = mDiscover.getComponentName(uri);
        assertNull(componentName);

        // add
        PackageManager packageManager = mAppContext.getPackageManager();
        mDiscover.addPackage(packageManager, TEST_PKG);
        assertEquals(baseInfoCount, mDiscover.getComponentCount());
        componentName = mDiscover.getComponentName(uri);
        assertNotNull(componentName);
        assertTrue(mDiscover.isDiscovered(uri));
        assertNotEquals(0, mDiscover.getComponentCount());
    }


    /**
     * Updated package is added when previously absent.  This simulates a package update
     * that adds new extension support.
     */
    @Test
    public void testPackage_update() {
        String uri = "alexatest:simple:10";

        int baseInfoCount;

        // Execute method that triggers initialization
        mDiscover.getComponentName(uri);
        baseInfoCount = ExtensionDiscovery.getInstance(mAppContext).getComponentCount();
        assertNotEquals(baseInfoCount, 0);

        // remove package - as though it didn't support extension
        mDiscover.removePackage(TEST_PKG);
        ComponentName componentName = mDiscover.getComponentName(uri);
        assertNull(componentName);

        // update
        PackageManager packageManager = mAppContext.getPackageManager();
        mDiscover.updatePackage(packageManager, TEST_PKG);
        assertEquals(baseInfoCount, mDiscover.getComponentCount());
        componentName = mDiscover.getComponentName(uri);
        assertNotNull(componentName);
        assertTrue(mDiscover.isDiscovered(uri));
        assertNotEquals(0, mDiscover.getComponentCount());
    }

    @Test
    public void testPresenceSimple() {
        // Deferred
        final String uriDeferred = "alexatest:deferred:10";
        assertEquals(ExtensionDiscovery.ExtensionPresence.DEFERRED,
                mDiscover.hasExtension(uriDeferred));
        assertEquals(mAppContext.getString(R.string.definition), ExtensionDiscovery.getInstance(mAppContext).getExtensionDefinition(uriDeferred));

        // Present
        final String uriPresent = "alexatest:simple:10";
        assertEquals(ExtensionDiscovery.ExtensionPresence.PRESENT,
                mDiscover.hasExtension(uriPresent));
        assertNull(ExtensionDiscovery.getInstance(mAppContext).getExtensionDefinition(uriPresent));

        // Present
        final String uriNotPresent = "alexatest:dne:10";
        assertEquals(ExtensionDiscovery.ExtensionPresence.NOT_PRESENT,
                mDiscover.hasExtension(uriNotPresent));
        assertNull(ExtensionDiscovery.getInstance(mAppContext).getExtensionDefinition(uriNotPresent));
    }

    @Test
    public void testPresenceMulti() {
        final String uri1 = "alexatest:deferredmulti:10";
        final String uri2 = "alexatest:deferredmulti:20";
        final String uri3 = "alexatest:deferredmulti:30";

        assertEquals(ExtensionDiscovery.ExtensionPresence.DEFERRED,
                mDiscover.hasExtension(uri1));
        assertEquals("multidefinition1", ExtensionDiscovery.getInstance(mAppContext).getExtensionDefinition(uri1));

        assertEquals(ExtensionDiscovery.ExtensionPresence.DEFERRED,
                mDiscover.hasExtension(uri2));
        assertEquals("multidefinition2", ExtensionDiscovery.getInstance(mAppContext).getExtensionDefinition(uri2));

        assertEquals(ExtensionDiscovery.ExtensionPresence.DEFERRED,
                mDiscover.hasExtension(uri3));
        assertEquals("multidefinition3", ExtensionDiscovery.getInstance(mAppContext).getExtensionDefinition(uri3));
    }

    @Test
    public void testPresenceMultiMismatch() {
        final String uri1 = "alexatest:deferredmismatch:10";
        final String uri2 = "alexatest:deferredmismatch:20";
        final String uri3 = "alexatest:deferredmismatch:30";

        assertEquals(ExtensionDiscovery.ExtensionPresence.DEFERRED,
                mDiscover.hasExtension(uri1));
        assertEquals("mismatchdefinition1", ExtensionDiscovery.getInstance(mAppContext).getExtensionDefinition(uri1));

        assertEquals(ExtensionDiscovery.ExtensionPresence.DEFERRED,
                mDiscover.hasExtension(uri2));
        assertEquals("mismatchdefinition2", ExtensionDiscovery.getInstance(mAppContext).getExtensionDefinition(uri2));

        assertEquals(ExtensionDiscovery.ExtensionPresence.PRESENT,
                mDiscover.hasExtension(uri3));
        assertNull(ExtensionDiscovery.getInstance(mAppContext).getExtensionDefinition(uri3));
    }
}
