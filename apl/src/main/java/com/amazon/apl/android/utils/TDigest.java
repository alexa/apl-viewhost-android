/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.utils;

import com.google.common.annotations.VisibleForTesting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/*
 * This is a basic implementation of the algorithm outlined in "Computing Extremely Accurate Quantiles Using
 * t-Digests": https://arxiv.org/pdf/1902.04023
 *
 * The t-digest construction algorithm uses a variant of 1-dimensional k-means clustering to produce a very
 * compact data structure that allows accurate estimation of quantiles. This t-digest data structure can be
 * used to estimate quantiles, compute other rank statistics or even to estimate related measures like trimmed
 * means. The advantage of the t-digest over previous digests (eg. QDigest) is that the t-digest handles
 * data with full floating point resolution.
 */

public class TDigest {
    private final double compression;
    private final List<Centroid> centroids;
    private double count;

    public TDigest(double compression) {
        this.compression = compression;
        this.centroids = new ArrayList<>();
        this.count = 0;
    }

    public void add(double value) {
        add(value, 1);
    }

    @VisibleForTesting
    void add(double value, double weight) {
        if (centroids.isEmpty()) {
            centroids.add(new Centroid(value, weight));
            count = weight;
            return;
        }

        Centroid nearest;
        int index = Collections.binarySearch(centroids, new Centroid(value, 0));
        if (index < 0) {
            index = -index - 1;
        }

        if (index > 0 && index < centroids.size()) {
            Centroid left = centroids.get(index - 1);
            Centroid right = centroids.get(index);
            if (Math.abs(left.mean - value) < Math.abs(right.mean - value)) {
                nearest = left;
                index--;
            } else {
                nearest = right;
            }
        } else if (index == 0) {
            nearest = centroids.get(0);
        } else {
            nearest = centroids.get(centroids.size() - 1);
        }

        double qQuantile = (sumWeight(0, index) + (nearest.count / 2)) / (count + weight);
        double qSize = 4 * (count + weight) * qQuantile * (1 - qQuantile) / compression;

        if (nearest.count + weight <= qSize) {
            nearest.add(value, weight);
        } else {
            centroids.add(index, new Centroid(value, weight));
        }

        count += weight;
        if (centroids.size() > 10 * compression) {
            compress();
        }
    }

    public double getSum() {
        double sum = 0;
        for (int i = 0; i < centroids.size(); i++) {
            sum += centroids.get(i).count * centroids.get(i).mean;
        }
        return sum;
    }

    private double sumWeight(int start, int end) {
        double sum = 0;
        for (int i = start; i < end; i++) {
            sum += centroids.get(i).count;
        }
        return sum;
    }

    public double trimmedMean(double lowerQuantile, double upperQuantile) {
        // Return 0 with the intention that a 0 emitted value for this would mean something wrong
        if (centroids.isEmpty()) {
            return 0.0;
        }

        double lowerRank = lowerQuantile * count;
        double upperRank = upperQuantile * count;
        double sum = 0;
        double trimmedCount = 0;
        double cumulative = 0;

        for (Centroid c : centroids) {
            if (cumulative >= lowerRank && cumulative + c.count <= upperRank) {
                sum += c.mean * c.count;
                trimmedCount += c.count;
            } else if (cumulative < lowerRank && cumulative + c.count > lowerRank) {
                double partialCount = cumulative + c.count - lowerRank;
                sum += c.mean * partialCount;
                trimmedCount += partialCount;
            } else if (cumulative < upperRank && cumulative + c.count > upperRank) {
                double partialCount = upperRank - cumulative;
                sum += c.mean * partialCount;
                trimmedCount += partialCount;
            }
            cumulative += c.count;
            if (cumulative >= upperRank) {
                break;
            }
        }

        return trimmedCount > 0 ? sum / trimmedCount : 0.0;
    }
    /**
     * Re-examines a t-digest to determine whether some centroids are redundant.  If your data are
     * perversely ordered, this may be a good idea.  Even if not, this may save 20% or so in space.
     * <p>
     * The cost is roughly the same as adding as many data points as there are centroids.  This
     * is typically &lt; 10 * compression, but could be as high as 100 * compression.
     * <p>
     * This is a destructive operation that is not thread-safe.
     */
    private void compress() {
        List<Centroid> compressed = new ArrayList<>();
        double totalWeight = 0;
        Centroid current = null;

        for (Centroid c : centroids) {
            if (current == null) {
                current = new Centroid(c.mean, c.count);
            } else {
                double q = totalWeight / count;
                double k = 4 * count * q * (1 - q) / compression;

                if (current.count + c.count <= k) {
                    current.add(c.mean, c.count);
                } else {
                    compressed.add(current);
                    current = new Centroid(c.mean, c.count);
                }
            }
            totalWeight += c.count;
        }

        if (current != null) {
            compressed.add(current);
        }

        centroids.clear();
        centroids.addAll(compressed);
    }

    private static class Centroid implements Comparable<Centroid> {
        private double mean;
        private double count;

        public Centroid(double mean, double count) {
            this.mean = mean;
            this.count = count;
        }

        public void add(double value, double weight) {
            double sum = mean * count + value * weight;
            count += weight;
            mean = sum / count;
        }

        @Override
        public int compareTo(Centroid other) {
            return Double.compare(this.mean, other.mean);
        }
    }
}

