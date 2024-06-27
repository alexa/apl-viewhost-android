/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.android.metrics;

/**
 * Defines the milestones for metrics recorded.
 */
public final class MetricsMilestoneConstants {
    //render document
    public static final String RENDER_START_MILESTONE = "APL.Viewhost.render.start";
    public static final String RENDER_END_MILESTONE = "APL.Viewhost.render.end";
    public static final String RENDER_FAILED_MILESTONE = "APL.Viewhost.render.failed";

    //extension registrations
    public static final String EXTENSION_REGISTRATION_START_MILESTONE = "APL.Viewhost.extension.register.start";
    public static final String EXTENSION_REGISTRATION_END_MILESTONE = "APL.Viewhost.extension.register.end";

    public static final String EXTENSION_REGISTRATION_FAILED_MILESTONE = "APL.Viewhost.extension.register.failed";

    //rootContext inflate and reinflate
    public static final String ROOTCONTEXT_INFLATE_START_MILESTONE = "APL.Viewhost.rootcontext.inflate.start";
    public static final String ROOTCONTEXT_INFLATE_END_MILESTONE = "APL.Viewhost.rootcontext.inflate.end";
    public static final String ROOTCONTEXT_INFLATE_FAILED_MILESTONE = "APL.Viewhost.rootcontext.inflate.failed";
    public static final String ROOTCONTEXT_REINFLATE_START_MILESTONE = "APL.Viewhost.rootcontext.reinflate.start";
    public static final String ROOTCONTEXT_REINFLATE_END_MILESTONE = "APL.Viewhost.rootcontext.reinflate.end";
    public static final String ROOTCONTEXT_REINFLATE_FAILED_MILESTONE = "APL.Viewhost.rootcontext.reinflate.failed";

    //apl layout layout and view layout
    public static final String APLLAYOUT_LAYOUT_START_MILESTONE = "APL.Viewhost.apllayout.layout.start";
    public static final String APLLAYOUT_LAYOUT_END_MILESTONE = "APL.Viewhost.apllayout.layout.end";
    public static final String APLLAYOUT_LAYOUT_FAILED_MILESTONE = "APL.Viewhost.apllayout.layout.failed";
    public static final String APLLAYOUT_VIEW_INFLATE_START_MILESTONE = "APL.Viewhost.apllayout.viewInflate.start";
    public static final String APLLAYOUT_VIEW_INFLATE_END_MILESTONE = "APL.Viewhost.apllayout.viewInflate.end";
    public static final String APLLAYOUT_VIEW_INFLATE_FAILED_MILESTONE = "APL.Viewhost.apllayout.viewInflate.failed";
    public static final String APLLAYOUT_RENDER_DOCUMENT_START_MILESTONE = "APL.Viewhost.apllayout.renderDocument.start";
    public static final String APLLAYOUT_RENDER_DOCUMENT_END_MILESTONE = "APL.Viewhost.apllayout.renderDocument.end";

    //content create metrics
    public static final String CONTENT_CREATE_START_MILESTONE = "APL.Viewhost.content.create.start";
    public static final String CONTENT_CREATE_END_MILESTONE = "APL.Viewhost.content.create.end";
    public static final String CONTENT_CREATE_FAILED_MILESTONE = "APL.Viewhost.content.create.failed";

}
