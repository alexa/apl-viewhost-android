/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.alexaext;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

/**
 * Represents an extension session, i.e. a group of related activities.
 *
 * Session descriptors are immutable and hashable, so they are
 * suitable to use as keys in unordered maps or other hashing data structures.
 */
public class SessionDescriptor implements Parcelable {
    private final String mSessionId;

    public SessionDescriptor(String id) {
        mSessionId = id;
    }

    protected SessionDescriptor(Parcel in) {
        mSessionId = in.readString();
    }

    public static final Creator<SessionDescriptor> CREATOR = new Creator<SessionDescriptor>() {
        @Override
        public SessionDescriptor createFromParcel(Parcel in) {
            return new SessionDescriptor(in);
        }

        @Override
        public SessionDescriptor[] newArray(int size) {
            return new SessionDescriptor[size];
        }
    };

    public String getId() { return mSessionId; }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(mSessionId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SessionDescriptor that = (SessionDescriptor) o;
        return mSessionId.equals(that.mSessionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mSessionId);
    }
}
