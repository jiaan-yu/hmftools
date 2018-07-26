package com.hartwig.hmftools.bamslicer.clustering;

import java.util.Comparator;

import org.apache.commons.math3.ml.clustering.Clusterable;
import org.immutables.value.Value;
import org.jetbrains.annotations.NotNull;

import htsjdk.samtools.Chunk;
import htsjdk.samtools.util.BlockCompressedFilePointerUtil;

@Value.Immutable
@Value.Style(allParameters = true,
             passAnnotations = { NotNull.class })
public abstract class ClusterableChunk implements Clusterable, Comparable<ClusterableChunk> {

    @NotNull
    abstract Chunk chunk();

    @Value.Derived
    long startOffset() {
        return BlockCompressedFilePointerUtil.getBlockAddress(chunk().getChunkStart());
    }

    @Value.Derived
    long endOffset() {
        return BlockCompressedFilePointerUtil.getBlockAddress(chunk().getChunkEnd());
    }

    @Override
    public double[] getPoint() {
        return new double[] { startOffset(), endOffset() };
    }

    @Override
    public int compareTo(@NotNull final ClusterableChunk o) {
        final Comparator<ClusterableChunk> comparator =
                Comparator.<ClusterableChunk, Long>comparing(clusterableChunk -> clusterableChunk.chunk().getChunkStart()).thenComparing(
                        clusterableChunk -> clusterableChunk.chunk().getChunkEnd());
        return comparator.compare(this, o);
    }
}
