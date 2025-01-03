package com.amazon.apl.android.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class TDigestTest {

    @Test
    public void testTDigestGetSum() {
        // Arrange
        TDigest tDigest = new TDigest(10);
        List<Integer> list = new ArrayList<>();
        // Act
        for (int i = 0; i < 100; i++) {
            tDigest.add(i);
            list.add(i);
        }
        // Assert
        assertEquals(list.stream().map(Double::new).reduce(0.0d, Double::sum), tDigest.getSum(), 0.0);
    }

    @Test
    public void testTDigestTrimmedMean() {
        // Arrange
        TDigest tDigest = new TDigest(10);
        // Act
        for (int i = 0; i < 100; i++) {
            tDigest.add(i);
        }
        // Assert
        assertEquals(47.5, tDigest.trimmedMean(0.0, 0.96), 0.0);
    }

    @Test
    public void testTDigestTrimmedMeanWithCentroidWeightIsZero() {
        // Arrange
        TDigest tDigest = new TDigest(10);
        // Act
        tDigest.add(1.0, 0.0);
        // Assert
        assertEquals(0.0, tDigest.trimmedMean(0.0, 0.96), 0.0);
    }

    @Test
    public void testTDigestTrimmedMeanWithEmptyCentroid() {
        // Arrange
        TDigest tDigest = new TDigest(10);

        // Assert
        assertEquals(0.0, tDigest.trimmedMean(0.0, 0.96), 0.0);
    }

    @Test
    public void testTDigestTrimmedMeanWithLowerAndUpperQuantile() {
        // Arrange
        TDigest tDigest = new TDigest(20);
        // Act
        for (int i = 0; i < 10000; i++) {
            tDigest.add(i);
        }
        // Assert
        // Due to T-Digest centroid compression, it won't be exactly average of the percentile but very close
        assertEquals(3500, tDigest.trimmedMean(0.19, 0.51), 1.0);
    }

    @Test
    public void testTDigestTrimmedMeanWithCompress() {
        // Arrange
        TDigest tDigest = new TDigest(20);
        // Act
        for (int i = 0; i < 10000; i++) {
            tDigest.add(i);
        }
        // Assert
        // Due to T-Digest centroid compression, it won't be exactly average of 50 percentile but very close
        assertEquals(2550, tDigest.trimmedMean(0.0, 0.51), 1.0);
    }
}
