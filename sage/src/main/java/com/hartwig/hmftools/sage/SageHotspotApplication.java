package com.hartwig.hmftools.sage;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.hartwig.hmftools.common.hotspot.HotspotEvidence;
import com.hartwig.hmftools.common.hotspot.HotspotEvidenceType;
import com.hartwig.hmftools.common.hotspot.HotspotEvidenceVCF;
import com.hartwig.hmftools.common.hotspot.ImmutableHotspotEvidence;
import com.hartwig.hmftools.common.hotspot.ImmutableVariantHotspotImpl;
import com.hartwig.hmftools.common.hotspot.InframeIndelHotspots;
import com.hartwig.hmftools.common.hotspot.SAMConsumer;
import com.hartwig.hmftools.common.hotspot.VariantHotspot;
import com.hartwig.hmftools.common.hotspot.VariantHotspotEvidence;
import com.hartwig.hmftools.common.hotspot.VariantHotspotEvidenceFactory;
import com.hartwig.hmftools.common.hotspot.VariantHotspotFile;
import com.hartwig.hmftools.common.region.BEDFileLoader;
import com.hartwig.hmftools.common.region.GenomeRegion;
import com.hartwig.hmftools.common.region.GenomeRegionBuilder;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;
import htsjdk.samtools.reference.IndexedFastaSequenceFile;

public class SageHotspotApplication implements AutoCloseable {

    private static final Logger LOGGER = LogManager.getLogger(SageHotspotApplication.class);

    public static void main(String[] args) throws IOException {
        final Options options = SageHotspotApplicationConfig.createOptions();
        try (final SageHotspotApplication application = new SageHotspotApplication(options, args)) {
            application.run();
        } catch (ParseException e) {
            LOGGER.warn(e);
            final HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("SageHotspotApplication", options);
            System.exit(1);
        }
    }

    private final SamReader tumorReader;
    private final SamReader referenceReader;
    private final SageHotspotApplicationConfig config;

    private SageHotspotApplication(final Options options, final String... args) throws ParseException {

        final CommandLine cmd = createCommandLine(args, options);
        config = SageHotspotApplicationConfig.createConfig(cmd);
        tumorReader = SamReaderFactory.makeDefault().open(new File(config.tumorBamPath()));
        referenceReader = SamReaderFactory.makeDefault().open(new File(config.referenceBamPath()));
    }

    private void run() throws IOException {
        final String hotspotPath = config.knownHotspotPath();
        final String tumorBam = config.tumorBamPath();
        final String referenceBam = config.referenceBamPath();
        final String refGenome = config.refGenomePath();
        final String codingRegionBedFile = config.codingRegionBedPath();
        final String outputVCF = config.outputFile();
        final String referenceSample = config.normal();
        final String tumorSample = config.tumor();
        final int minMappingQuality = config.minMappingQuality();
        final int minBaseQuality = config.minBaseQuality();

        final IndexedFastaSequenceFile refSequence = new IndexedFastaSequenceFile(new File(refGenome));

        LOGGER.info("Loading coding regions from {}", codingRegionBedFile);
        final Collection<GenomeRegion> codingRegions = BEDFileLoader.fromBedFile(codingRegionBedFile).values();

        LOGGER.info("Loading known hotspots from {}", hotspotPath);
        final Set<VariantHotspot> knownHotspots = Sets.newHashSet(VariantHotspotFile.read(hotspotPath).values());

        LOGGER.info("Looking for potential inframe indel locations ");
        final Set<VariantHotspot> allHotspots = Sets.newHashSet();
        allHotspots.addAll(knownHotspots);
        allHotspots.addAll(new InframeIndelHotspots(minMappingQuality, codingRegions, refSequence).findInframeIndels(tumorReader));
        final List<GenomeRegion> allHotspotRegions = asRegions(config.typicalReadDepth(), allHotspots);
        final SAMConsumer hotspotRegionConsumer = new SAMConsumer(minMappingQuality, allHotspotRegions);

        LOGGER.info("Looking for evidence of hotspots in tumor bam {}", tumorBam);
        final VariantHotspotEvidenceFactory tumorEvidenceFactory = new VariantHotspotEvidenceFactory(minBaseQuality);
        final Map<VariantHotspot, VariantHotspotEvidence> tumorEvidence =
                asMap(tumorEvidenceFactory.evidence(hotspotRegionConsumer, refSequence, tumorReader, allHotspots));

        LOGGER.info("Looking for evidence of hotspots in reference bam {}", referenceBam);
        final VariantHotspotEvidenceFactory referenceEvidenceFactory = new VariantHotspotEvidenceFactory(minBaseQuality);
        final Map<VariantHotspot, VariantHotspotEvidence> referenceEvidence =
                asMap(referenceEvidenceFactory.evidence(hotspotRegionConsumer, refSequence, referenceReader, allHotspots));

        final List<HotspotEvidence> evidence = Lists.newArrayList();
        for (Map.Entry<VariantHotspot, VariantHotspotEvidence> entry : tumorEvidence.entrySet()) {
            final VariantHotspot variant = entry.getKey();
            final VariantHotspotEvidence tumor = entry.getValue();
            final VariantHotspotEvidence normal = referenceEvidence.get(variant);
            evidence.add(createEvidence(knownHotspots.contains(variant), tumor, normal));
        }

        LOGGER.info("Writing output to {}", outputVCF);
        Collections.sort(evidence);
        new HotspotEvidenceVCF(referenceSample,
                tumorSample,
                config.maxHetBinomialLikelihood(),
                config.minTumorReads(),
                config.minSnvVAF(),
                config.minIndelVAF(),
                config.minSnvQuality(),
                config.minIndelQuality()).write(outputVCF, evidence);

    }

    @NotNull
    private List<GenomeRegion> asRegions(int typicalReadDepth, @NotNull final Set<VariantHotspot> allHotspots) {

        final Map<String, GenomeRegionBuilder> builders = Maps.newHashMap();
        allHotspots.forEach(x -> builders.computeIfAbsent(x.chromosome(), key -> new GenomeRegionBuilder(key, typicalReadDepth))
                .addPosition(x.position()));

        final List<GenomeRegion> results = Lists.newArrayList();
        builders.values().forEach(x -> results.addAll(x.build()));

        Collections.sort(results);
        return results;
    }

    @NotNull
    private static Map<VariantHotspot, VariantHotspotEvidence> asMap(@NotNull final List<VariantHotspotEvidence> evidence) {
        return evidence.stream().collect(Collectors.toMap(x -> ImmutableVariantHotspotImpl.builder().from(x).build(), x -> x));
    }

    private static HotspotEvidence createEvidence(boolean known, @NotNull final VariantHotspotEvidence tumor,
            @Nullable final VariantHotspotEvidence normal) {
        return ImmutableHotspotEvidence.builder()
                .from(tumor)
                .ref(tumor.ref())
                .alt(tumor.alt())
                .qualityScore(tumor.altQuality())
                .tumorAltCount(tumor.altSupport())
                .tumorRefCount(tumor.refSupport())
                .tumorReads(tumor.readDepth())
                .normalAltCount(normal == null ? 0 : normal.altSupport())
                .normalRefCount(normal == null ? 0 : normal.refSupport())
                .normalReads(normal == null ? 0 : normal.readDepth())
                .normalIndelCount(normal == null ? 0 : normal.indelSupport())
                .type(known ? HotspotEvidenceType.KNOWN : HotspotEvidenceType.INFRAME)
                .build();
    }

    @NotNull
    private static CommandLine createCommandLine(@NotNull String[] args, @NotNull Options options) throws ParseException {
        final CommandLineParser parser = new DefaultParser();
        return parser.parse(options, args);
    }

    @Override
    public void close() throws IOException {
        tumorReader.close();
        referenceReader.close();
        LOGGER.info("Complete");
    }
}
