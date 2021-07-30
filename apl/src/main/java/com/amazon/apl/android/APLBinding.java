/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android;


import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import android.util.Log;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Tracks the lifecycle of a BoundObject and releases the native peer when the object is out of scope.
 */
public class APLBinding extends PhantomReference<BoundObject> {

    // This implementation taken from Google I/O https://www.youtube.com/watch?v=7_caITSjk1k
    @NonNull
    private static final ReferenceQueue<BoundObject> sRefQueue = new ReferenceQueue<>();
    // Maintains a map of native handles to APLBindings. Since more than one Java object may be
    // bound to the same native handle, we only want to unbind the handle if there are no
    // more references to it.
    private static final Map<Long, Set<APLBinding>> sReferenceMap = new ConcurrentHashMap<>();

    // The native object handle
    private final long mNativeHandle;

    // Debug identifier
    private String mDebugId;


    /**
     * Constructs a binding from object to native peer.
     *
     * @param referent The bound object.nValidateUnbind(
     */
    private APLBinding(@NonNull BoundObject referent) {
        super(referent, sRefQueue);
        mNativeHandle = referent.getNativeHandle();
    }

    /**
     * Creates a binding from object to native peer.
     *
     * @param referent The bound object.
     */
    static synchronized void register(BoundObject referent) {
        if (!referent.isBound()) {
            throw new IllegalStateException("The object is not yet bound");
        }
        add(new APLBinding(referent));
    }

    /**
     * Releases all native objects that no longer have a bound object.  This method should
     * be called routinely.
     */
    public static synchronized void doDeletes() {
        APLBinding ref = (APLBinding) sRefQueue.poll();
        for (; ref != null; ref = (APLBinding) sRefQueue.poll()) {
            remove(ref);
        }
    }

    /**
     * Adds an element to the reference map.
     *
     * @param binding the binding to add.
     */
    private static void add(APLBinding binding) {
        Set<APLBinding> bindingSet = sReferenceMap.get(binding.mNativeHandle);
        if (bindingSet == null) {
            bindingSet = Collections.newSetFromMap(new ConcurrentHashMap<>());
            sReferenceMap.put(binding.mNativeHandle, bindingSet);
        }
        bindingSet.add(binding);
    }

    /**
     * Removes an element from the reference map.
     *
     * If there are no more references to the
     * native handle then the native object is freed.
     *
     * @param binding the binding to remove.
     */
    private static void remove(APLBinding binding) {
        Set<APLBinding> bindingSet = sReferenceMap.get(binding.mNativeHandle);
        bindingSet.remove(binding);
        if (bindingSet.isEmpty()) {
            sReferenceMap.remove(binding.mNativeHandle);
            nUnbind(binding.mNativeHandle);
        }
    }

    /**
     * TEST USE ONLY
     *
     * @return True if the handle is bound to a native object
     */
    @VisibleForTesting
    public static boolean testBound(long nativeHandle) {
        for (Set<APLBinding> bindingSet : sReferenceMap.values()) {
            for (APLBinding binding : bindingSet) {
                if (binding.mNativeHandle == nativeHandle)
                    return true;
            }
        }
        return false;
    }

    /**
     * TEST USE ONLY
     *
     * @return the bound object count
     */
    @VisibleForTesting
    public static int testBoundObjectCount() {
        return sReferenceMap.size();
    }

    /**
     * TEST USE ONLY
     *
     * @return the bound object count
     */
    @VisibleForTesting
    public static boolean testNativePeer(long handle) {
        return nTestNativePeer(handle);
    }

    /**
     * TEST USE ONLY
     *
     * @return The topmost pending delete.
     */
    @NonNull
    @VisibleForTesting
    public static APLBinding testPopPendingDelete() {
        APLBinding binding = (APLBinding) sRefQueue.poll();
        Set<APLBinding> bindingSet = sReferenceMap.get(binding.mNativeHandle);
        bindingSet.remove(binding);
        if (bindingSet.isEmpty()) {
            sReferenceMap.remove(binding.mNativeHandle);
        }
        return binding;
    }

    /**
     * TEST USE ONLY
     *
     * unbind the bound object.
     */
    @VisibleForTesting
    public static void testUnBind(APLBinding binding) {
        nUnbind(binding.getNativeHandle());
    }

    /**
     * TEST USE ONLY
     *
     * @return The count of references to this object in core.
     */
    @VisibleForTesting
    public static int testReferenceCount(long nativeHandle) {
        return nTestPointerCount(nativeHandle);
    }

    /**
     * Tests to see if the native peer exists.
     *
     * @param nativeHandle The handle to the native peer.
     * @return True if the Native peer can be cast.
     */
    private static native boolean nTestNativePeer(long nativeHandle);

    /**
     * Release the native object.
     *
     * @param nativeHandle Handle to the native peer.
     */
    private static native void nUnbind(long nativeHandle);

    /**
     * TEST USE ONLY
     *
     * @param nativeHandle Handle to the native peer.
     */
    private static native int nTestPointerCount(long nativeHandle);

    /**
     * @return The native handle.
     */
    public long getNativeHandle() {
        return mNativeHandle;
    }
}
