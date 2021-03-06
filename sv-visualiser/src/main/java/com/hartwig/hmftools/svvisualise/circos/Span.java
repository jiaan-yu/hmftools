package com.hartwig.hmftools.svvisualise.circos;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import com.hartwig.hmftools.common.chromosome.HumanChromosome;
import com.hartwig.hmftools.common.position.GenomePosition;
import com.hartwig.hmftools.common.position.GenomePositions;
import com.hartwig.hmftools.common.region.GenomeRegion;
import com.hartwig.hmftools.common.region.GenomeRegionFactory;

import org.jetbrains.annotations.NotNull;

public class Span {

    @NotNull
    public static List<GenomeRegion> span(@NotNull final List<GenomePosition> positions) {
        final List<GenomeRegion> result = Lists.newArrayList();

        final List<String> chromosomes = positions.stream().map(GenomePosition::chromosome).distinct().collect(Collectors.toList());
        for (final String chromosome : chromosomes) {
            long min =
                    positions.stream().filter(x -> x.chromosome().equals(chromosome)).mapToLong(GenomePosition::position).min().orElse(0);
            long max =
                    positions.stream().filter(x -> x.chromosome().equals(chromosome)).mapToLong(GenomePosition::position).max().orElse(0);

            result.add(GenomeRegionFactory.create(chromosome, min, max));
        }

        Collections.sort(result);
        return result;
    }

    @NotNull
    public static List<GenomePosition> allPositions(@NotNull final List<? extends GenomeRegion> segments) {
        final List<GenomePosition> results = Lists.newArrayList();

        for (final GenomeRegion segment : segments) {
            if (HumanChromosome.contains(segment.chromosome())) {
                results.add(GenomePositions.create(segment.chromosome(), segment.start()));
                results.add(GenomePositions.create(segment.chromosome(), segment.end()));
            }
        }

        Collections.sort(results);
        return results;
    }

}
