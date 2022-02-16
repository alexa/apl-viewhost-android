/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.alexa.android.extension.discovery;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Looper;
import android.os.UserHandle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import android.text.TextUtils;
import android.util.Log;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * L1 Client/Service Extension IPC.
 * This class provides the platform specific server discovery, binding, and connection management.
 */
public final class ExtensionDiscovery {

    private static final String TAG = "ExtensionDiscovery";
    private static final boolean DEBUG = false;

    // Meta-Data tags from package manager
    private static final String EXTENSION_BASE = "com.amazon.alexa.extensions.";
    private static final String EXTENSION_METADATA_APP = EXTENSION_BASE + "EXTENSION";
    private static final String EXTENSION_METADATA_ALIAS = EXTENSION_BASE + "ALIAS";
    private static final String EXTENSION_ACTION = EXTENSION_BASE + "ACTION";
    private static final String EXTENSION_DEFINITION = EXTENSION_BASE + "DEFINITION";

    // Singleton
    private static ExtensionDiscovery sInstance;
    // Cached packages for reuse
    private final Map<String, ComponentInfo> mComponents = new ConcurrentHashMap<>();

    /**
     * Defines extension presence.
     */
    public enum ExtensionPresence {
        PRESENT,
        DEFERRED,
        NOT_PRESENT
    }

    /* Private Constructor for singleton */
    private ExtensionDiscovery(@NonNull final Context context) {
        initialize(context.getApplicationContext());
    }

    /**
     * @return Single instance of the Extension Binding
     */
    static synchronized ExtensionDiscovery getInstance(@NonNull final Context context) {
        if (sInstance == null) {
            sInstance = new ExtensionDiscovery(context);
        }

        return sInstance;
    }

    /**
     * Create an intent for binding and discovery.
     *
     * @return A new Intent with the extension action.
     */
    static Intent createExtensionIntent() {
        return new Intent(EXTENSION_ACTION);
    }


    /**
     * Used for testing.
     *
     * @return the count of discovered extensions.
     */
    @VisibleForTesting
    synchronized int getComponentCount() {
        return mComponents.size();
    }


    /**
     * Used for testing.
     *
     * @return true if the extension has been previously discovered.
     */
    @VisibleForTesting
    synchronized boolean isDiscovered(String uri) {
        return mComponents.containsKey(uri);
    }

    /**
     * Used for testing. Not intended for production use.
     *
     * Reset the package list used in discovery.
     */
    @VisibleForTesting
    void clearCache() {
        mComponents.clear();
        sInstance = null;
    }

    /**
     * @param serviceURI extension service URI
     * @return {@link ComponentName} associated with the URI.
     */
    @Nullable
    synchronized ComponentName getComponentName(@NonNull final String serviceURI) {
        final ComponentInfo componentInfo = mComponents.get(serviceURI);
        if (componentInfo == null) {
            return null;
        }

        return componentInfo.mComponentName;
    }

    /**
     * @param uri extension service URI
     * @return {@link ExtensionPresence} telling how the component should be bound.
     */
    synchronized ExtensionPresence hasExtension(@NonNull final String uri) {
        final ComponentInfo componentInfo = mComponents.get(uri);

        if (componentInfo == null) {
            return ExtensionPresence.NOT_PRESENT;
        }

        if (TextUtils.isEmpty(componentInfo.mExtensionDefinition)) {
            return ExtensionPresence.PRESENT;
        }

        return ExtensionPresence.DEFERRED;
    }

    @Nullable
    synchronized String getExtensionDefinition(@NonNull final String uri) {
        final ComponentInfo componentInfo = mComponents.get(uri);

        if (componentInfo == null) {
            return null;
        }

        return componentInfo.mExtensionDefinition;
    }

    /**
     * Remove a previously discovered component as a result of package uninstall.
     *
     * @param packageName The package name.
     */
    private synchronized void removeComponent(@NonNull final String packageName) {
        final Iterator<ComponentInfo> each = mComponents.values().iterator();
        while (each.hasNext()) {
            ComponentInfo componentName = each.next();
            if (componentName.mComponentName.getPackageName().equals(packageName)) {
                each.remove();
            }
        }
    }

    /**
     * Find a package by package name, that implements an extension.
     *
     * @param packageManager The package manager.
     * @param packageName    The package name to search for.
     * @return a ResolveInfo list if the package is found, null otherwise.
     */
    @Nullable
    private List<ResolveInfo> findPackage(@NonNull final PackageManager packageManager, @NonNull final String packageName) {
        final Intent intent = createExtensionIntent();
        intent.setPackage(packageName);
        return packageManager.queryIntentServices(intent, PackageManager.GET_META_DATA);
    }


    /**
     * Adds a package to the extension cache if it supports Extensions.
     *
     * @param packageManager Thee package manager.
     * @param packageName    The package to add.
     */
    @VisibleForTesting
    synchronized void addPackage(@NonNull final PackageManager packageManager, @NonNull final String packageName) {
        final List<ResolveInfo> info = findPackage(packageManager, packageName);
        if (info != null) {
            registerComponentInfo(packageManager, info);
        }
    }

    /**
     * Remove a package from the extension cache.
     *
     * @param packageName The package name.
     */
    @VisibleForTesting
    synchronized void removePackage(@NonNull final String packageName) {
        removeComponent(packageName);
    }

    /**
     * Updates the package info in the package cache.  This may result in the
     * package info being added, remove, or changed.
     *
     * @param packageManager The package manager.
     * @param packageName    The package name.
     */
    @VisibleForTesting
    synchronized void updatePackage(PackageManager packageManager, String packageName) {
        final List<ResolveInfo> info = findPackage(packageManager, packageName);
        if (info != null) {
            // remove then re-add to cache the latest data
            removePackage(packageName);
            registerComponentInfo(packageManager, info);
        }
    }

    /**
     * Initialize discovery for faster lookup.  Also, registers to get package updates using the
     * calling threads looper.
     *
     * @param context AndroidContext.
     */
    private synchronized void initialize(@NonNull final Context context) {
        final PackageManager packageManager = context.getPackageManager();
        if (packageManager == null) {
            return;
        }

        // Get the packages and meta-data that satisfy extension requests
        final Intent intent = createExtensionIntent();
        final List<ResolveInfo> info = packageManager.queryIntentServices(intent, PackageManager.GET_META_DATA);

        // Create ComponentInfo to URI mapping.
        registerComponentInfo(packageManager, info);

        // Start listening for changes in installed packages
        final LauncherApps launchService = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);
        final Looper looper = Looper.myLooper();
        if (null != looper) {
            launchService.registerCallback(new LauncherApps.Callback() {
                @Override
                public void onPackageAdded(String packageName, UserHandle user) {
                    addPackage(packageManager, packageName);
                }

                @Override
                public void onPackageRemoved(String packageName, UserHandle user) {
                    removePackage(packageName);
                }

                @Override
                public void onPackageChanged(String packageName, UserHandle user) {
                    updatePackage(packageManager, packageName);
                }


                @Override
                public void onPackagesAvailable(String[] packageNames, UserHandle user, boolean replacing) {
                    // applies to temp storage
                    // no-op package found on cache init
                }

                @Override
                public void onPackagesUnavailable(String[] packageNames, UserHandle user, boolean replacing) {
                    // applies to temp storage
                    // no-op package found on cache init, binding will fail if not available
                }
            });
        }
    }

    private void registerComponentInfo(@NonNull final PackageManager packageManager, @NonNull final List<ResolveInfo> info) {
        for (final ResolveInfo resolveInfo : info) {
            final ServiceInfo svcInfo = resolveInfo.serviceInfo;

            if (svcInfo == null) {
                return;
            }

            if (DEBUG)
                Log.v(TAG, "discover candidate: " + svcInfo.packageName + "." + svcInfo.name);

            final String[] uris = getUris(packageManager, svcInfo);

            if (uris == null || uris.length == 0) {
                return;
            }

            final String[] definitions = getDefinitions(packageManager, svcInfo);

            for (int i = 0; i < uris.length; i++) {
                String definition = null;

                if (definitions != null && definitions.length > i) {
                    definition = definitions[i];
                }

                mComponents.put(uris[i], new ComponentInfo(svcInfo, definition));
            }
        }
    }

    private String[] getUris(@NonNull final PackageManager packageManager, @NonNull final ServiceInfo svcInfo) {
        final String[] appUris = getStringArrayFromMetadataObject(packageManager, svcInfo,
                EXTENSION_METADATA_APP, svcInfo.packageName);

        if (appUris != null) {
            return appUris;
        }

        return getStringArrayFromMetadataObject(packageManager, svcInfo,
                EXTENSION_METADATA_ALIAS, svcInfo.packageName);
    }

    private String[] getDefinitions(@NonNull final PackageManager packageManager, @NonNull final ServiceInfo svcInfo) {
        return getStringArrayFromMetadataObject(packageManager, svcInfo, EXTENSION_DEFINITION, svcInfo.packageName);
    }

    @Nullable
    private String[] getStringArrayFromMetadataObject(@NonNull final PackageManager packageManager,
                                                      @NonNull final ServiceInfo svcInfo,
                                                      @NonNull final String key,
                                                      @NonNull final String packageName) {
        final Bundle metaData = svcInfo.metaData;

        if (metaData == null || metaData.isEmpty()) {
            return null;
        }

        final Object md = metaData.get(key);

        if (md == null) {
            return null;
        }

        // Direct string from "value" meta-data property
        if (md instanceof String) {
            return new String[]{(String) md};
        }

        // Indirect string from  "resource" meta-data property.
        if (md instanceof Integer) {
            try {
                final long start = System.currentTimeMillis();
                final Resources res = packageManager.getResourcesForApplication(packageName);
                return res.getStringArray((Integer) md);
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "The extension package resources are missing: "
                        + packageName, e);
            } catch (Resources.NotFoundException nfe) {
                Log.e(TAG, "The extension resource is missing: " + key, nfe);
            }
        }

        return null;
    }

    private static class ComponentInfo {
        private final ComponentName mComponentName;
        private final String mExtensionDefinition;

        ComponentInfo(@NonNull final ServiceInfo serviceInfo, @Nullable final String extensionDefinition) {
            mComponentName = new ComponentName(serviceInfo.packageName, serviceInfo.name);
            mExtensionDefinition = extensionDefinition;
        }
    }
}
