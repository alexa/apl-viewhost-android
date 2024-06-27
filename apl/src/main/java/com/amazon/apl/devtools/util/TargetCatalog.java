/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.util;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;

import com.amazon.apl.devtools.enums.TargetType;
import com.amazon.apl.devtools.models.Target;
import com.amazon.apl.devtools.models.ViewTypeTarget;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

public final class TargetCatalog {
    private final SortedMap<String, Target> mTargets = new TreeMap<>();
    private final Handler mHandler;
    private final HandlerThread mHandlerThread;

    private Context context;

    public TargetCatalog() {
        // Create a background thread and associate a Handler with it
        mHandlerThread = new HandlerThread("TargetCatalogThread");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
    }

    public void post(Runnable runnable) {
        mHandler.post(runnable);
    }

    public Handler getHandler() {
        return mHandler;
    }

    public Target add(Target target) {
        return mTargets.put(target.getTargetId(), target);
    }

    public Target remove(Target target) {
        return mTargets.remove(target.getTargetId());
    }

    public boolean has(String targetId) {
        return mTargets.containsKey(targetId);
    }

    public Target get(String targetId) {
        return mTargets.get(targetId);
    }

    /**
     * getAll returns an ordered collection of targets
     */
    public Collection<Target> getAll() {
        return mTargets.values();
    }

    public Context getAppContext() {
        return context;
    }

    public void setAppContext(Context context) {
        this.context = context;
    }

    /**
     * getViewTypeTargets returns an ordered list of view type targets
     */
    public List<ViewTypeTarget> getViewTypeTargets() {
        List<ViewTypeTarget> viewTypeTargets = new ArrayList<>();
        for (Target target : getAll()) {
            if (target.getType() == TargetType.VIEW) {
                viewTypeTargets.add((ViewTypeTarget) target);
            }
        }
        return viewTypeTargets;
    }

    public void cleanup() {
        mHandler.removeCallbacksAndMessages(null);
        mHandlerThread.quit();
    }
}
