package com.hartwig.hmftools.breakpointinspector;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import htsjdk.samtools.*;
import htsjdk.variant.variantcontext.Genotype;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFFileReader;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import static com.hartwig.hmftools.breakpointinspector.Util.*;
import static com.hartwig.hmftools.breakpointinspector.Stats.*;

import com.google.common.collect.Lists;

import org.jetbrains.annotations.NotNull;

public class BreakPointInspectorApplication {

    private static final String REF_PATH = "ref";
    private static final String REF_EVIDENCE_PATH = "ref_evidence";
    private static final String TUMOR_PATH = "tumor";
    private static final String TUMOR_EVIDENCE_PATH = "tumor_evidence";
    private static final String BREAK_POINT1 = "bp1";
    private static final String BREAK_POINT2 = "bp2";
    private static final String PROXIMITY = "proximity";
    private static final String SV_LEN = "svlen";
    private static final String SV_TYPE = "svtype";
    private static final String VCF = "vcf";

    @NotNull
    private static Options createOptions() {
        final Options options = new Options();
        options.addOption(REF_PATH, true, "the Reference BAM (indexed)");
        options.addOption(REF_EVIDENCE_PATH, true, "the Reference evidence BAM to output");
        options.addOption(TUMOR_PATH, true, "the Tumor BAM (indexed)");
        options.addOption(TUMOR_EVIDENCE_PATH, true, "the Tumor evidence BAM to output");
        options.addOption(BREAK_POINT1, true, "position of first break point in chrX:123456 format");
        options.addOption(BREAK_POINT2, true, "position of second break point in chrX:123456 format (optional)");
        options.addOption(PROXIMITY, true, "base distance around breakpoint");
        options.addOption(SV_LEN, true, "length of the SV to inspect (>0)");
        options.addOption(SV_TYPE, true, "one of BND, INV, DEL, DUP");
        options.addOption(VCF, true, "VCF file to batch inspect (can be compressed)");
        return options;
    }

    @NotNull
    private static CommandLine createCommandLine(@NotNull final Options options, @NotNull final String... args)
            throws ParseException {
        final CommandLineParser parser = new DefaultParser();
        return parser.parse(options, args);
    }

    private static void printHelpAndExit(final Options options) {
        final HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("Break-Point-Inspector", "Inspect structural variants", options, "", true);
        System.exit(1);
    }

    private static List<String> parseMantaPRSR(final Genotype genotype) {
        String pr = (String) genotype.getExtendedAttribute("PR", "0,0");
        String sr = (String) genotype.getExtendedAttribute("SR", "0,0");
        return Stream.concat(Arrays.stream(pr.split(",")), Arrays.stream(sr.split(","))).collect(Collectors.toList());
    }

    public static void main(final String... args) throws ParseException, IOException {

        final Options options = createOptions();
        try {
            final CommandLine cmd = createCommandLine(options, args);

            // grab arguments
            final String refPath = cmd.getOptionValue(REF_PATH);
            final String refEvidencePath = cmd.getOptionValue(REF_EVIDENCE_PATH);
            final String tumorPath = cmd.getOptionValue(TUMOR_PATH);
            final String tumorEvidencePath = cmd.getOptionValue(TUMOR_EVIDENCE_PATH);
            final String vcfPath = cmd.getOptionValue(VCF);
            final int range = Integer.parseInt(cmd.getOptionValue(PROXIMITY, "500"));

            if (refPath == null || tumorPath == null || (vcfPath == null))
                printHelpAndExit(options);

            // load the files
            final File tumorBAM = new File(tumorPath);
            final SamReader tumorReader = SamReaderFactory.makeDefault().open(tumorBAM);
            final File refBAM = new File(refPath);
            final SamReader refReader = SamReaderFactory.makeDefault().open(refBAM);

            final File tumorEvidenceBAM;
            SAMFileWriter tumorWriter = null;
            if (tumorEvidencePath != null) {
                tumorEvidenceBAM = new File(tumorEvidencePath);
                tumorWriter = new SAMFileWriterFactory().makeBAMWriter(tumorReader.getFileHeader(), false,
                        tumorEvidenceBAM);
            }

            final File refEvidenceBAM;
            SAMFileWriter refWriter = null;
            if (refEvidencePath != null) {
                refEvidenceBAM = new File(refEvidencePath);
                refWriter = new SAMFileWriterFactory().makeBAMWriter(refReader.getFileHeader(), false, refEvidenceBAM);
            }

            final File vcfFile = new File(vcfPath);
            final VCFFileReader vcfReader = new VCFFileReader(vcfFile, false);

            // work out the reference sample
            final List<String> samples = vcfReader.getFileHeader().getGenotypeSamples();
            final Predicate<String> isRef = s -> s.endsWith("R") || s.endsWith("BL");
            final String refSampleName = samples.stream().filter(isRef).findFirst().orElse(null);
            final String tumorSampleName = samples.stream().filter(
                    s -> s.endsWith("T") || !isRef.test(s)).findFirst().orElse(null);
            if (refSampleName == null || tumorSampleName == null) {
                System.err.println("could not determine tumor and sample from VCF");
                System.exit(1);
                return;
            }

            // output the header
            final ArrayList<String> header = Lists.newArrayList("ID", "SVTYPE", "ORIENTATION", "MANTA_BP1",
                    "MANTA_BP2", "MANTA_SVLEN", "MANTA_REF_PR_NORMAL", "MANTA_REF_PR_SUPPORT", "MANTA_REF_SR_NORMAL",
                    "MANTA_REF_SR_SUPPORT", "MANTA_TUMOR_PR_NORMAL", "MANTA_TUMOR_PR_SUPPORT", "MANTA_TUMOR_SR_NORMAL",
                    "MANTA_TUMOR_SR_SUPPORT", "MANTA_HOMSEQ", "MANTA_INSSEQ");
            header.addAll(prefixList(Sample.GetHeader(), "REF_"));
            header.addAll(prefixList(Sample.GetHeader(), "TUMOR_"));
            header.add("BPI_BP1");
            header.add("BPI_BP2");
            header.add("FILTER");
            header.add("TUMOR_CLIP_INFO");
            System.out.println(String.join("\t", header));

            for (final VariantContext variant : vcfReader) {

                final String location = variant.getContig() + ":" + Integer.toString(variant.getStart());
                Location location1 = Location.parseLocationString(location,
                        tumorReader.getFileHeader().getSequenceDictionary());
                Location location2;

                HMFVariantType svType;
                switch (variant.getStructuralVariantType()) {
                    case INV:
                        if (variant.hasAttribute("INV3")) {
                            svType = HMFVariantType.INV3;
                        } else if (variant.hasAttribute("INV5")) {
                            svType = HMFVariantType.INV5;
                        } else {
                            System.err.println(variant.getID() + " : expected either INV3 or INV5 flag");
                            continue;
                        }
                        location2 = location1.add(Math.abs(variant.getAttributeAsInt("SVLEN", 0)));
                        break;
                    case DEL:
                        svType = HMFVariantType.DEL;
                        location2 = location1.add(Math.abs(variant.getAttributeAsInt("SVLEN", 0)));
                        break;
                    case DUP:
                        svType = HMFVariantType.DUP;
                        location2 = location1.add(Math.abs(variant.getAttributeAsInt("SVLEN", 0)));
                        break;
                    case BND:
                        final String call = variant.getAlternateAllele(0).getDisplayString();
                        final String[] leftSplit = call.split("\\]");
                        final String[] rightSplit = call.split("\\[");
                        if (leftSplit.length >= 2) {
                            location2 = Location.parseLocationString(leftSplit[1],
                                    tumorReader.getFileHeader().getSequenceDictionary());
                            if (leftSplit[0].length() > 0) {
                                svType = HMFVariantType.INV3;
                            } else {
                                svType = HMFVariantType.DUP;
                            }
                        } else if (rightSplit.length >= 2) {
                            location2 = Location.parseLocationString(rightSplit[1],
                                    tumorReader.getFileHeader().getSequenceDictionary());
                            if (rightSplit[0].length() > 0) {
                                svType = HMFVariantType.DEL;
                            } else {
                                svType = HMFVariantType.INV5;
                            }
                        } else {
                            System.err.println(variant.getID() + " : could not parse breakpoint");
                            continue;
                        }
                        break;
                    default:
                        System.err.println(
                                variant.getID() + " : UNEXPECTED SVTYPE=" + variant.getStructuralVariantType());
                        continue;
                }

                // uncertainty
                final List<Integer> ciPos = variant.getAttributeAsIntList("CIPOS", 0);
                Range uncertainty1 = ciPos.size() == 2 ? new Range(ciPos.get(0), ciPos.get(1)) : null;
                final List<Integer> ciEnd = variant.getAttributeAsIntList("CIEND", 0);
                Range uncertainty2 = ciEnd.size() == 2 ? new Range(ciEnd.get(0), ciEnd.get(1)) : null;

                // handle HOMSEQ in BND
                if (variant.hasAttribute("HOMSEQ") && !variant.hasAttribute("CIEND"))
                    uncertainty2 = new Range(-ciPos.get(1), ciPos.get(0));
                // TODO: double check this
                // TODO: anything for SVINSSEQ?

                final List<String> extraData = Lists.newArrayList(variant.getID(),
                        variant.getStructuralVariantType().toString(), HMFVariantType.getOrientation(svType),
                        location1.toString(), location2.toString(), variant.getAttributeAsString("SVLEN", "."));

                extraData.addAll(parseMantaPRSR(variant.getGenotype(refSampleName)));
                extraData.addAll(parseMantaPRSR(variant.getGenotype(tumorSampleName)));

                extraData.add(variant.getAttributeAsString("HOMSEQ", "."));
                extraData.add(variant.getAttributeAsString("SVINSSEQ", "."));

                final HMFVariantContext ctx = new HMFVariantContext(location1, location2, svType);
                ctx.Filter.addAll(variant.getFilters());
                ctx.Uncertainty1 = uncertainty1;
                ctx.Uncertainty2 = uncertainty2;

                // TODO: pass filter
                Analysis.processStructuralVariant(extraData, refReader, refWriter, tumorReader, tumorWriter, ctx,
                        range);
            }

            // close all the files
            refReader.close();
            tumorReader.close();
            if (refWriter != null)
                refWriter.close();
            if (tumorWriter != null)
                tumorWriter.close();

        } catch (ParseException e) {
            printHelpAndExit(options);
            System.exit(1);
        }
    }
}
