package com.hartwig.hmftools.bamslicer.clustering;

import static org.junit.Assert.assertEquals;

import com.hartwig.hmftools.bamslicer.clustering.ChunkDistanceMeasure;
import com.hartwig.hmftools.bamslicer.clustering.ClusterableChunk;

import org.apache.commons.math3.ml.distance.DistanceMeasure;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import htsjdk.samtools.Chunk;

public class ChunkDistanceMeasureTest {
    @NotNull
    private final DistanceMeasure distanceMeasure = new ChunkDistanceMeasure();

    @Test
    public void distanceBetweenOverlappingChunksIsZero() {
        final double[] first = clusterableChunk(5, 10).getPoint();
        final double[] second = clusterableChunk(7, 13).getPoint();
        final double[] mid = clusterableChunk(7, 8).getPoint();
        assertDistance(0, first, second);
        assertDistance(0, first, mid);
        assertDistance(0, first, first);
    }

    @Test
    public void computesDistanceBetweenDisjointIntervals() {
        final double[] first = clusterableChunk(5, 10).getPoint();
        final double[] second = clusterableChunk(12, 13).getPoint();
        assertDistance(2, first, second);
    }

    private void assertDistance(final double expected, final double[] one, final double[] other) {
        assertEquals(expected, distanceMeasure.compute(one, other), 0.0000001);
        assertEquals(expected, distanceMeasure.compute(other, one), 0.0000001);
    }

    @NotNull
    private static ClusterableChunk clusterableChunk(final long start, final long end) {
        return ImmutableClusterableChunk.of(new Chunk(start << 16, end << 16));
    }

}