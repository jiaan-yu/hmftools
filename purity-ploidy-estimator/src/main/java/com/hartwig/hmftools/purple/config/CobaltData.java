package com.hartwig.hmftools.purple.config;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import com.hartwig.hmftools.common.chromosome.Chromosome;
import com.hartwig.hmftools.common.chromosome.ChromosomeLength;
import com.hartwig.hmftools.common.chromosome.ChromosomeLengthFactory;
import com.hartwig.hmftools.common.chromosome.ChromosomeLengthFile;
import com.hartwig.hmftools.common.chromosome.HumanChromosome;
import com.hartwig.hmftools.common.cobalt.CobaltRatio;
import com.hartwig.hmftools.common.cobalt.CobaltRatioFile;
import com.hartwig.hmftools.common.pcf.PCFFile;
import com.hartwig.hmftools.common.pcf.PCFPosition;
import com.hartwig.hmftools.common.pcf.PCFSource;
import com.hartwig.hmftools.common.position.GenomePosition;
import com.hartwig.hmftools.common.position.GenomePositions;
import com.hartwig.hmftools.common.purple.gender.Gender;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.immutables.value.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Value.Immutable
@Value.Style(passAnnotations = { NotNull.class, Nullable.class })
public interface CobaltData {

    Logger LOGGER = LogManager.getLogger(CobaltData.class);
    String COBALT = "cobalt";

    @NotNull
    Gender gender();

    @NotNull
    ListMultimap<Chromosome, CobaltRatio> ratios();

    @NotNull
    Map<Chromosome, GenomePosition> chromosomeLengths();

    boolean chromosomeLengthsEstimated();

    Multimap<Chromosome, PCFPosition> tumorSegments();

    Multimap<Chromosome, PCFPosition> referenceSegments();

    static void addOptions(@NotNull Options options) {
        options.addOption(COBALT, true, "COBALT directory. Defaults to <run_dir>/cobalt");
    }

    @NotNull
    static CobaltData createCobaltData(@NotNull final CommandLine cmd, @NotNull final CommonConfig commonConfig)
            throws ParseException, IOException {
        final String cobaltDirectory =
                cmd.hasOption(COBALT) ? cmd.getOptionValue(COBALT) : commonConfig.runDirectory() + File.separator + "cobalt";
        final String cobaltFilename = CobaltRatioFile.generateFilename(cobaltDirectory, commonConfig.tumorSample());
        if (!new File(cobaltFilename).exists()) {
            throw new ParseException("Unable to open cobalt ratio file: " + cobaltFilename);
        }

        final String referenceSegmentFile = PCFFile.generateRatioFilename(cobaltDirectory, commonConfig.refSample());
        if (!new File(referenceSegmentFile).exists()) {
            throw new ParseException("Unable to open cobalt reference pcf file: " + referenceSegmentFile);
        }

        final String tumorSegmentFile = PCFFile.generateRatioFilename(cobaltDirectory, commonConfig.tumorSample());
        if (!new File(tumorSegmentFile).exists()) {
            throw new ParseException("Unable to open cobalt tumor pcf file: " + tumorSegmentFile);
        }

        LOGGER.info("Reading cobalt ratios from {}", cobaltFilename);
        final ListMultimap<Chromosome, CobaltRatio> ratios = CobaltRatioFile.read(cobaltFilename);
        final Gender gender = Gender.fromCobalt(ratios);

        final Map<Chromosome, GenomePosition> lengths;
        final boolean lengthsEstimated;
        final String chrLengthFile = ChromosomeLengthFile.generateFilename(cobaltDirectory, commonConfig.tumorSample());
        if (!new File(chrLengthFile).exists()) {
            lengthsEstimated = true;
            lengths = fromLengths(ChromosomeLengthFactory.create(commonConfig.windowSize(), ratios).values());
        } else {
            lengthsEstimated = false;
            lengths = fromLengths(ChromosomeLengthFile.read(chrLengthFile));
        }

        LOGGER.info("Reading cobalt reference segments from {}", referenceSegmentFile);
        final Multimap<Chromosome, PCFPosition> referenceSegments =
                PCFFile.readPositions(commonConfig.windowSize(), PCFSource.REFERENCE_RATIO, referenceSegmentFile);

        LOGGER.info("Reading cobalt tumor segments from {}", tumorSegmentFile);
        final Multimap<Chromosome, PCFPosition> tumorSegments =
                PCFFile.readPositions(commonConfig.windowSize(), PCFSource.TUMOR_RATIO, tumorSegmentFile);

        return ImmutableCobaltData.builder()
                .chromosomeLengths(lengths)
                .chromosomeLengthsEstimated(lengthsEstimated)
                .ratios(ratios)
                .gender(gender)
                .tumorSegments(tumorSegments)
                .referenceSegments(referenceSegments)
                .build();
    }

    @NotNull
    static Map<Chromosome, GenomePosition> fromLengths(@NotNull final Collection<ChromosomeLength> lengths) {
        return lengths.stream()
                .filter(x -> HumanChromosome.contains(x.chromosome()))
                .collect(Collectors.toMap(x -> HumanChromosome.fromString(x.chromosome()),
                        item -> GenomePositions.create(item.chromosome(), item.length())));
    }

}
