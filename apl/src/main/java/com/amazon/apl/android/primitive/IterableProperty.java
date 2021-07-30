/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.primitive;

import androidx.annotation.NonNull;

import java.util.Iterator;
import java.util.List;

public interface IterableProperty<T> extends Iterable<T> {
    List<T> list();

    @NonNull
    @Override
    default Iterator<T> iterator() {
        return list().iterator();
    }

    default int size() {
        return list().size();
    }

    default T at(int index) {
        return list().get(index);
    }

    default void add(T value) {
        list().add(value);
    }

    default void add(int index, T value) {
        list().add(index, value);
    }

    static <K extends IterableProperty<T>, T> K create(ArrayGetter<K, T> getter) {
        K iterableProperty = getter.builder();
        for (int i = 0; i < getter.size(); i++) {
            iterableProperty.list().add(i, getter.get(i));
        }
        return iterableProperty;
    }
}
