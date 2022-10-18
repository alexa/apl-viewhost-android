/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.alexaext;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

/**
 * Represents an activity that requests and uses functionality defined by a given extension.
 * For example, a rendering task for an APL document is a common type of activity that requests
 * APL extensions. Each activity belongs to a single extension session.
 *
 * Activity descriptors are immutable and hashable, so they are suitable to use as keys in
 * unordered maps or other hashing data structures.
 */
public class ActivityDescriptor implements Parcelable {
    private final String mURI;
    private final SessionDescriptor mSession;
    private final String mActivityId;

    public ActivityDescriptor(String uri, SessionDescriptor session, String id) {
        mURI = uri;
        mSession = session;
        mActivityId = id;
    }

    protected ActivityDescriptor(Parcel in) {
        mURI = in.readString();
        mSession = in.readParcelable(SessionDescriptor.class.getClassLoader());
        mActivityId = in.readString();
    }

    public static final Creator<ActivityDescriptor> CREATOR = new Creator<ActivityDescriptor>() {
        @Override
        public ActivityDescriptor createFromParcel(Parcel in) {
            return new ActivityDescriptor(in);
        }

        @Override
        public ActivityDescriptor[] newArray(int size) {
            return new ActivityDescriptor[size];
        }
    };

    public String getURI() {
        return mURI;
    }

    public SessionDescriptor getSession() {
        return mSession;
    }

    public String getActivityId() {
        return mActivityId;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(mURI);
        parcel.writeParcelable(mSession, i);
        parcel.writeString(mActivityId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ActivityDescriptor that = (ActivityDescriptor) o;
        return mURI.equals(that.mURI) && mSession.equals(that.mSession) && mActivityId.equals(that.mActivityId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mURI, mSession, mActivityId);
    }
}
