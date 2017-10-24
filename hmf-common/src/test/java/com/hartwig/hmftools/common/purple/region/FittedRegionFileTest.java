package com.hartwig.hmftools.common.purple.region;

import static org.junit.Assert.assertEquals;

import java.util.Random;

import com.hartwig.hmftools.common.purple.segment.StructuralVariantSupport;

import org.junit.Test;

public class FittedRegionFileTest {
    @Test
    public void testToFromString() {
        final FittedRegion expected = createRandom(new Random());
        final FittedRegion decoded = FittedRegionFile.fromString(FittedRegionFile.toString(expected));
        assertEquals(expected, decoded);
    }

    private static FittedRegion createRandom(final Random random) {
        return ImmutableFittedRegion.builder()
                .chromosome("X")
                .start(random.nextLong())
                .end(random.nextLong())
                .status(ObservedRegionStatus.GERMLINE)
                .modelPloidy(random.nextInt())
                .bafCount(random.nextInt())
                .observedBAF(random.nextDouble())
                .modelBAF(random.nextDouble())
                .bafDeviation(random.nextDouble())
                .observedTumorRatio(random.nextDouble())
                .observedNormalRatio(random.nextDouble())
                .modelTumorRatio(random.nextDouble())
                .cnvDeviation(random.nextDouble())
                .deviation(random.nextDouble())
                .tumorCopyNumber(random.nextDouble())
                .broadTumorCopyNumber(random.nextDouble())
                .broadBAF(random.nextDouble())
                .segmentTumorCopyNumber(random.nextDouble())
                .segmentBAF(random.nextDouble())
                .refNormalisedCopyNumber(random.nextDouble())
                .ratioSupport(random.nextBoolean())
                .structuralVariantSupport(StructuralVariantSupport.BND)
                .observedTumorRatioCount(random.nextInt())
                .build();
    }

}
