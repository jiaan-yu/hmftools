package com.hartwig.hmftools.common.purple.region;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.google.common.annotations.VisibleForTesting;
import com.hartwig.hmftools.common.chromosome.HumanChromosome;
import com.hartwig.hmftools.common.numeric.Doubles;
import com.hartwig.hmftools.common.purple.BAFUtils;
import com.hartwig.hmftools.common.purple.PurityAdjuster;
import com.hartwig.hmftools.common.purple.gender.Gender;

import org.jetbrains.annotations.NotNull;

public class FittedRegionFactoryV2 implements FittedRegionFactory {

    private final Gender gender;
    private final double ploidyPenaltyFactor;
    private final PloidyDeviation ploidyDeviation;
    private final BAFUtils bafUtils;

    public FittedRegionFactoryV2(final Gender gender, final int averageReadDepth, double ploidyPenaltyFactor, double ploidyPenaltyStandardDeviation) {
        this.gender = gender;
        this.ploidyPenaltyFactor = ploidyPenaltyFactor;
        ploidyDeviation = new PloidyDeviation(ploidyPenaltyStandardDeviation);
        bafUtils = new BAFUtils(averageReadDepth);
    }

    @Override
    @NotNull
    public List<FittedRegion> fitRegion(final double purity, final double normFactor,
            @NotNull final Collection<ObservedRegion> observedRegions) {

        final Predicate<ObservedRegion> valid = observedRegion -> gender == Gender.MALE || !observedRegion.chromosome().equals("Y");
        return observedRegions.stream().filter(valid).map(x -> fitRegion(purity, normFactor, x)).collect(Collectors.toList());
    }

    @Override
    @NotNull
    public FittedRegion fitRegion(final double purity, final double normFactor, final @NotNull ObservedRegion observedRegion) {
        final PurityAdjuster purityAdjuster = new PurityAdjuster(gender, purity, normFactor);

        double observedTumorRatio = observedRegion.observedTumorRatio();
        double impliedCopyNumber = purityAdjuster.purityAdjustedCopyNumber(observedRegion.chromosome(), observedTumorRatio);
        double observedBAF = observedRegion.observedBAF();
        double impliedBAF = impliedBaf(purityAdjuster, observedRegion.chromosome(), impliedCopyNumber, observedBAF);

        double refNormalisedCopyNumber = purityAdjuster.purityAdjustedCopyNumber(observedTumorRatio, observedRegion.observedNormalRatio());

        double majorAllelePloidy = impliedBAF * impliedCopyNumber;
        double minorAllelePloidy = impliedCopyNumber - majorAllelePloidy;

        double majorAllelePloidyDeviation = ploidyDeviation.majorAlleleDeivation(purity, normFactor, majorAllelePloidy);
        double minorAllelePloidyDeviation = ploidyDeviation.minorAlleleDeviation(purity, normFactor, minorAllelePloidy);

        final double ploidyPenalty = PloidyPenalty.penaltyv2(ploidyPenaltyFactor, majorAllelePloidy, minorAllelePloidy);
        final double totalDeviation = (minorAllelePloidyDeviation + majorAllelePloidyDeviation) * observedBAF;

        ImmutableFittedRegion.Builder builder = ImmutableFittedRegion.builder()
                .from(observedRegion)
                .segmentBAF(0)
                .segmentTumorCopyNumber(0)
                .tumorCopyNumber(impliedCopyNumber)
                .tumorBAF(impliedBAF)
                .refNormalisedCopyNumber(Doubles.replaceNaNWithZero(refNormalisedCopyNumber))
                .modelBAF(0)
                .modelPloidy((int) Math.round(majorAllelePloidy))
                .modelTumorRatio(0)
                .bafDeviation(majorAllelePloidyDeviation)
                .cnvDeviation(minorAllelePloidyDeviation)
                .deviation(totalDeviation)
                .ploidyPenalty(ploidyPenalty);

        return builder.build();
    }

    public double impliedBaf(final PurityAdjuster purityAdjuster, final String chromosome, final double copyNumber,
            final double observedBAF) {
        boolean isHomologous = HumanChromosome.fromString(chromosome).isDiploid(gender);

        if (!isHomologous || Doubles.lessOrEqual(copyNumber, 1)) {
            return 1;
        }

        return Doubles.lessOrEqual(observedBAF, bafUtils.ambiguousBAF())
                ? bafToMinimiseDeviation(purityAdjuster, chromosome, copyNumber)
                : purityAdjuster.purityAdjustedBAFSimple(chromosome, copyNumber, observedBAF);

    }

    @VisibleForTesting
    double bafToMinimiseDeviation(final PurityAdjuster purityAdjuster, final String chromosome, double impliedCopyNumber) {

        final double minBAF = Math.max(0, Math.min(1, purityAdjuster.purityAdjustedBAFSimple(chromosome, impliedCopyNumber, 0.5)));
        final double maxBAF =
                Math.max(0, Math.min(1, purityAdjuster.purityAdjustedBAFSimple(chromosome, impliedCopyNumber, bafUtils.ambiguousBAF())));

        // Major Ploidy
        final double minBAFMajorAllelePloidy = minBAF * impliedCopyNumber;
        final double maxBAFMajorAllelePloidy = maxBAF * impliedCopyNumber;

        // Major Ploidy crosses whole number?
        final double minBAFMajorAllelePloidyCeil = Math.ceil(minBAFMajorAllelePloidy);
        if (!Doubles.equal(Math.signum(minBAFMajorAllelePloidyCeil - minBAFMajorAllelePloidy),
                Math.signum(minBAFMajorAllelePloidyCeil - maxBAFMajorAllelePloidy))) {
            return minBAFMajorAllelePloidyCeil / impliedCopyNumber;
        }

        // Minor Ploidy
        final double minBAFMinorAllelePloidy = impliedCopyNumber - minBAFMajorAllelePloidy;
        final double maxBAFMinorAllelePloidy = impliedCopyNumber - maxBAFMajorAllelePloidy;

        // Minor Ploidy crosses whole number?
        final double maxBAFMinorAllelePloidyCeil = Math.ceil(maxBAFMinorAllelePloidy);
        if (!Doubles.equal(Math.signum(maxBAFMinorAllelePloidyCeil - minBAFMinorAllelePloidy),
                Math.signum(maxBAFMinorAllelePloidyCeil - maxBAFMinorAllelePloidy))) {
            return 1 - maxBAFMinorAllelePloidyCeil / impliedCopyNumber;
        }

        double purity = purityAdjuster.purity();
        double normFactor = purityAdjuster.normFactor();

        // Minimise
        final double minBAFTotalDeviation =
                ploidyDeviation.majorAlleleDeivation(purity, normFactor, minBAFMajorAllelePloidy) + ploidyDeviation.minorAlleleDeviation(
                        purity,
                        normFactor,
                        minBAFMinorAllelePloidy);
        final double maxBAFTotalDeviation =
                ploidyDeviation.majorAlleleDeivation(purity, normFactor, maxBAFMajorAllelePloidy) + ploidyDeviation.minorAlleleDeviation(
                        purity,
                        normFactor,
                        maxBAFMinorAllelePloidy);
        return Doubles.lessThan(minBAFTotalDeviation, maxBAFTotalDeviation) ? 0.5 : bafUtils.ambiguousBAF();
    }

}