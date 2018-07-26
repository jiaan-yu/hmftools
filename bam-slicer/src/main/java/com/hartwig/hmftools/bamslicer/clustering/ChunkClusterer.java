package com.hartwig.hmftools.bamslicer.clustering;

import java.util.List;
import java.util.stream.Collectors;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;

import org.apache.commons.math3.ml.clustering.Cluster;
import org.apache.commons.math3.ml.clustering.DBSCANClusterer;
import org.jetbrains.annotations.NotNull;

import htsjdk.samtools.Chunk;

public class ChunkClusterer {

    @VisibleForTesting
    @NotNull
    public static List<Chunk> clusterChunks(@NotNull final List<Chunk> chunks, final long mergeThreshold, final int minClusterSize) {
        final int minNeighbors = minClusterSize - 1;
        final DBSCANClusterer<ClusterableChunk> clusterer = new DBSCANClusterer<>(mergeThreshold, minNeighbors, new ChunkDistanceMeasure());
        final List<ClusterableChunk> clusterableChunks = chunks.stream().map(ImmutableClusterableChunk::of).collect(Collectors.toList());
        final List<Cluster<ClusterableChunk>> clusters = clusterer.cluster(clusterableChunks);
        final List<Chunk> clusteredChunks = clustersToChunks(clusters);
        return addIsolatedChunks(chunks, clusteredChunks);
    }

    @NotNull
    private static List<Chunk> clustersToChunks(@NotNull final List<Cluster<ClusterableChunk>> clusters) {
        return clusters.stream().map(cluster -> {
            final List<ClusterableChunk> sortedChunks = cluster.getPoints().stream().sorted().collect(Collectors.toList());
            final long chunkStart = sortedChunks.get(0).chunk().getChunkStart();
            final long chunkEnd = sortedChunks.get(sortedChunks.size() - 1).chunk().getChunkEnd();
            return new Chunk(chunkStart, chunkEnd);
        }).collect(Collectors.toList());
    }

    @VisibleForTesting
    @NotNull
    static List<Chunk> addIsolatedChunks(@NotNull final List<Chunk> chunks, @NotNull final List<Chunk> clusteredChunks) {
        final List<Chunk> results = Lists.newArrayList();
        int chunkIndex = 0;
        int clusterIndex = 0;
        while (chunkIndex < chunks.size() && clusterIndex < clusteredChunks.size()) {
            final Chunk chunk = chunks.get(chunkIndex);
            final Chunk cluster = clusteredChunks.get(clusterIndex);
            if (chunk.getChunkStart() < cluster.getChunkStart()) {
                results.add(chunk);
                chunkIndex++;
            } else if (chunkContainedInCluster(chunk, cluster)) {
                chunkIndex++;
            } else {
                results.add(cluster);
                clusterIndex++;
            }
        }
        while (chunkIndex < chunks.size()) {
            results.add(chunks.get(chunkIndex));
            chunkIndex++;
        }
        while (clusterIndex < clusteredChunks.size()) {
            results.add(clusteredChunks.get(clusterIndex));
            clusterIndex++;
        }
        return results;
    }

    private static boolean chunkContainedInCluster(@NotNull final Chunk chunk, @NotNull final Chunk cluster) {
        return chunk.getChunkStart() >= cluster.getChunkStart() && chunk.getChunkStart() <= cluster.getChunkEnd();
    }
}
