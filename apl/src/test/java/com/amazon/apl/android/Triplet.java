/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android;

public class Triplet<F, S, T> {
    private final F mFirst;
    private final S mSecond;
    private final T mThird;

    public Triplet(F first, S second, T third) {
        mFirst = first;
        mSecond = second;
        mThird = third;
    }

    public F getFirst() { return mFirst; }
    public S getSecond() { return mSecond; }
    public T getThird() { return mThird; }
}
