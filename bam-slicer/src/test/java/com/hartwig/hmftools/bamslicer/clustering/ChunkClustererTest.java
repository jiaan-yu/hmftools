package com.hartwig.hmftools.bamslicer.clustering;

import static com.hartwig.hmftools.bamslicer.clustering.ChunkClusterer.addIsolatedChunks;
import static com.hartwig.hmftools.bamslicer.clustering.ChunkClusterer.clusterChunks;

import static org.junit.Assert.assertEquals;

import java.util.List;

import com.google.common.collect.Lists;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import htsjdk.samtools.Chunk;

public class ChunkClustererTest {
    @Test
    public void clusters2ChunksWithinMergeThreshold() {
        final List<Chunk> chunks = Lists.newArrayList(chunk(0, 10), chunk(19, 29));
        final List<Chunk> clustered = clusterChunks(chunks, 10, 2);
        assertEquals(1, clustered.size());
    }

    @Test
    public void clusters2ChunksDistanceEqualToMergeThreshold() {
        final List<Chunk> chunks = Lists.newArrayList(chunk(0, 10), chunk(20, 29));
        final List<Chunk> clustered = clusterChunks(chunks, 10, 2);
        assertEquals(1, clustered.size());
    }

    @Test
    public void doesNotCluster2ChunksFartherThanMergeThreshold() {
        final List<Chunk> chunks = Lists.newArrayList(chunk(0, 10), chunk(21, 29));
        final List<Chunk> clustered = clusterChunks(chunks, 10, 2);
        assertEquals(2, clustered.size());
    }

    @Test
    public void creates2DisjointClusters() {
        final List<Chunk> chunks =
                Lists.newArrayList(chunk(0, 10), chunk(15, 20), chunk(21, 70), chunk(100, 117), chunk(120, 130), chunk(133, 135));
        final List<Chunk> clustered = clusterChunks(chunks, 10, 3);
        assertEquals(2, clustered.size());
        assertEquals(0, clustered.get(0).getChunkStart());
        assertEquals(70 << 16, clustered.get(0).getChunkEnd());
        assertEquals(100 << 16, clustered.get(1).getChunkStart());
        assertEquals(135 << 16, clustered.get(1).getChunkEnd());
    }

    @Test
    public void worksOnRealExample() {
        final List<Chunk> chunks = Lists.newArrayList(chunk(1107056378, 1107135059),
                chunk(1116186292, 1116251828),
                chunk(1125467486, 1125533022),
                chunk(1133742781, 1135022438),
                chunk(1144306714, 1144383459));
        final List<Chunk> clustered = clusterChunks(chunks, 10000000, 3);
        assertEquals(1, clustered.size());
    }

    @Test
    public void doesNotAddClusteredChunks() {
        final List<Chunk> chunks = Lists.newArrayList(chunk(0, 10), chunk(15, 20), chunk(25, 30));
        final List<Chunk> clusters = Lists.newArrayList(chunk(0, 30));
        assertEquals(clusters, addIsolatedChunks(chunks, clusters));
    }

    @Test
    public void addsIsolatedChunkBeforeCluster() {
        final List<Chunk> chunks = Lists.newArrayList(chunk(0, 10));
        final List<Chunk> clusters = Lists.newArrayList(chunk(15, 30));
        final List<Chunk> expected = Lists.newArrayList(chunk(0, 10), chunk(15, 30));
        assertEquals(expected, addIsolatedChunks(chunks, clusters));
    }

    @Test
    public void addsIsolatedChunkAfterCluster() {
        final List<Chunk> chunks = Lists.newArrayList(chunk(31, 35));
        final List<Chunk> clusters = Lists.newArrayList(chunk(15, 30));
        final List<Chunk> expected = Lists.newArrayList(chunk(15, 30), chunk(31, 35));
        assertEquals(expected, addIsolatedChunks(chunks, clusters));
    }

    @NotNull
    private static Chunk chunk(final long start, final long end) {
        return new Chunk(start << 16, end << 16);
    }
}
