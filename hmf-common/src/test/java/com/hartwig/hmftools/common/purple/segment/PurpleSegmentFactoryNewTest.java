package com.hartwig.hmftools.common.purple.segment;

import static com.hartwig.hmftools.common.purple.segment.PurpleSegmentStatus.CLUSTER;
import static com.hartwig.hmftools.common.purple.segment.PurpleSegmentStatus.NORMAL;
import static com.hartwig.hmftools.common.purple.segment.StructuralVariantSupport.BND;
import static com.hartwig.hmftools.common.purple.segment.StructuralVariantSupport.MULTIPLE;
import static com.hartwig.hmftools.common.purple.segment.StructuralVariantSupport.NONE;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;
import com.hartwig.hmftools.common.chromosome.ChromosomeLength;
import com.hartwig.hmftools.common.chromosome.ImmutableChromosomeLength;
import com.hartwig.hmftools.common.position.GenomePosition;
import com.hartwig.hmftools.common.variant.structural.StructuralVariantType;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

public class PurpleSegmentFactoryNewTest {
    private static final ChromosomeLength CHROM = ImmutableChromosomeLength.builder().chromosome("chromosome").position(10_000_000).build();

    @Test
    public void testEmpty() {
        final List<PurpleSegment> segments = PurpleSegmentFactoryNew.create(CHROM, Collections.emptyList(), Collections.emptyList());
        assertEquals(1, segments.size());
        assertPurpleSegment(segments.get(0), 1, CHROM.position(), true, NONE, NORMAL);
    }

    @Test
    public void testSingleSV() {
        final List<StructuralVariantCluster> clusters = Lists.newArrayList(cluster(18881, 17001, 20000).build());
        final List<PurpleSegment> segments = PurpleSegmentFactoryNew.create(CHROM, clusters, Collections.emptyList());
        assertEquals(2, segments.size());
        assertPurpleSegment(segments.get(0), 1, 18880, true, NONE, NORMAL);
        assertPurpleSegment(segments.get(1), 18881, CHROM.position(), false, BND, NORMAL);
    }

    @Test
    public void testSingleSVWithRatioSupportAtStart() {
        final List<StructuralVariantCluster> clusters = Lists.newArrayList(cluster(18881, 17001, 20000).build());
        final List<GenomePosition> ratios = Lists.newArrayList(ratio(17050));
        final List<PurpleSegment> segments = PurpleSegmentFactoryNew.create(CHROM, clusters, ratios);
        assertEquals(2, segments.size());
        assertPurpleSegment(segments.get(0), 1, 18880, true, NONE, NORMAL);
        assertPurpleSegment(segments.get(1), 18881, CHROM.position(), true, BND, NORMAL);
    }

    @Test
    public void testSingleSVWithRatioBeforeStart() {
        final List<StructuralVariantCluster> clusters = Lists.newArrayList(cluster(18881, 17001, 20000).build());
        final List<GenomePosition> ratios = Lists.newArrayList(ratio(16001));
        final List<PurpleSegment> segments = PurpleSegmentFactoryNew.create(CHROM, clusters, ratios);
        assertEquals(3, segments.size());
        assertPurpleSegment(segments.get(0), 1, 16000, true, NONE, NORMAL);
        assertPurpleSegment(segments.get(1), 16001, 18880, true, NONE, NORMAL);
        assertPurpleSegment(segments.get(2), 18881, CHROM.position(), false, BND, NORMAL);
    }

    @Test
    public void testSingleSVWithRatioSupportAtEnd() {
        final List<StructuralVariantCluster> clusters = Lists.newArrayList(cluster(18881, 17001, 20000).build());
        final List<GenomePosition> ratios = Lists.newArrayList(ratio(19050));
        final List<PurpleSegment> segments = PurpleSegmentFactoryNew.create(CHROM, clusters, ratios);
        assertEquals(2, segments.size());
        assertPurpleSegment(segments.get(0), 1, 18880, true, NONE, NORMAL);
        assertPurpleSegment(segments.get(1), 18881, CHROM.position(), true, BND, NORMAL);
    }

    @Test
    public void testSingleSVWithRatioAfter() {
        final List<StructuralVariantCluster> clusters = Lists.newArrayList(cluster(18881, 17001, 20000).build());
        final List<GenomePosition> ratios = Lists.newArrayList(ratio(20001));
        final List<PurpleSegment> segments = PurpleSegmentFactoryNew.create(CHROM, clusters, ratios);
        assertEquals(3, segments.size());
        assertPurpleSegment(segments.get(0), 1, 18880, true, NONE, NORMAL);
        assertPurpleSegment(segments.get(1), 18881, 20000, false, BND, NORMAL);
        assertPurpleSegment(segments.get(2), 20001, CHROM.position(), true, NONE, NORMAL);
    }

    @Test
    public void testMultipleSVAtSamePosition() {
        final List<StructuralVariantCluster> clusters =
                Lists.newArrayList(cluster(18881, 17001, 20000).addVariants(variant(18881)).build());
        final List<PurpleSegment> segments = PurpleSegmentFactoryNew.create(CHROM, clusters, Collections.emptyList());
        assertEquals(2, segments.size());
        assertPurpleSegment(segments.get(0), 1, 18880, true, NONE, NORMAL);
        assertPurpleSegment(segments.get(1), 18881, CHROM.position(), false, MULTIPLE, NORMAL);
    }

    @Test
    public void testMultipleSVInSameCluster() {
        final List<StructuralVariantCluster> clusters =
                Lists.newArrayList(cluster(18881, 17001, 20000).addVariants(variant(19991)).build());
        final List<PurpleSegment> segments = PurpleSegmentFactoryNew.create(CHROM, clusters, Collections.emptyList());
        assertEquals(3, segments.size());
        assertPurpleSegment(segments.get(0), 1, 18880, true, NONE, NORMAL);
        assertPurpleSegment(segments.get(1), 18881, 19990, false, BND, CLUSTER);
        assertPurpleSegment(segments.get(2), 19991, CHROM.position(), false, BND, NORMAL);
    }

    @Test
    public void testMultipleSVInSameClusterWithRatioSupport() {
        final List<StructuralVariantCluster> clusters =
                Lists.newArrayList(cluster(18881, 17001, 21000).addVariants(variant(19991)).build());
        final List<GenomePosition> ratios = Lists.newArrayList(ratio(20001));
        final List<PurpleSegment> segments = PurpleSegmentFactoryNew.create(CHROM, clusters, ratios);
        assertEquals(3, segments.size());
        assertPurpleSegment(segments.get(0), 1, 18880, true, NONE, NORMAL);
        assertPurpleSegment(segments.get(1), 18881, 19990, true, BND, CLUSTER);
        assertPurpleSegment(segments.get(2), 19991, CHROM.position(), true, BND, NORMAL);
    }

    @Test
    public void testMultipleClustersWithRatios() {
        final List<StructuralVariantCluster> clusters =
                Lists.newArrayList(
                        cluster(18881, 17001, 20000).build(),
                        cluster(25551, 24001, 27000).build());
        final List<GenomePosition> ratios = Lists.newArrayList(ratio(19001));
        final List<PurpleSegment> segments = PurpleSegmentFactoryNew.create(CHROM, clusters, ratios);
        assertEquals(3, segments.size());
        assertPurpleSegment(segments.get(0), 1, 18880, true, NONE, NORMAL);
        assertPurpleSegment(segments.get(1), 18881, 25550, true, BND, NORMAL);
        assertPurpleSegment(segments.get(2), 25551, CHROM.position(), false, BND, NORMAL);
    }

    private static void assertPurpleSegment(@NotNull final PurpleSegment victim, long start, long end, boolean ratioSupport,
            @NotNull final StructuralVariantSupport variantSupport, @NotNull final PurpleSegmentStatus status) {
        assertEquals(start, victim.start());
        assertEquals(end, victim.end());
        assertEquals(ratioSupport, victim.ratioSupport());
        assertEquals(variantSupport, victim.structuralVariantSupport());
        assertEquals(status, victim.status());
    }

    private static ImmutableStructuralVariantCluster.Builder cluster(long position, long start, long end) {
        return ImmutableStructuralVariantCluster.builder()
                .chromosome(CHROM.chromosome())
                .start(start)
                .end(end)
                .addVariants(variant(position));
    }

    private static StructuralVariantPosition variant(long position) {
        return ImmutableStructuralVariantPosition.builder()
                .chromosome(CHROM.chromosome())
                .position(position)
                .id("ID")
                .type(StructuralVariantType.BND)
                .orientation((byte) 1)
                .build();
    }

    private static GenomePosition ratio(final long position) {
        return new GenomePosition() {
            @NotNull
            @Override
            public String chromosome() {
                return CHROM.chromosome();
            }

            @Override
            public long position() {
                return position;
            }
        };
    }

}