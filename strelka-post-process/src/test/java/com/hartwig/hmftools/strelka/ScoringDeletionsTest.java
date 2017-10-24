package com.hartwig.hmftools.strelka;

import static com.hartwig.hmftools.strelka.MNVDetectorTest.buildSamRecord;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import com.google.common.collect.Streams;
import com.google.common.io.Resources;
import com.hartwig.hmftools.strelka.scores.ImmutableReadScore;
import com.hartwig.hmftools.strelka.scores.ReadScore;
import com.hartwig.hmftools.strelka.scores.ReadType;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import htsjdk.samtools.SAMRecord;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFFileReader;

public class ScoringDeletionsTest {
    private static final File VCF_FILE = new File(Resources.getResource("mnvs.vcf").getPath());
    private static final VCFFileReader VCF_FILE_READER = new VCFFileReader(VCF_FILE, false);
    private static final List<VariantContext> VARIANTS = Streams.stream(VCF_FILE_READER).collect(Collectors.toList());
    private static final VariantContext DELETION = VARIANTS.get(1);

    @Test
    public void doesNotDetectDELinRef() {
        final SAMRecord reference = buildSamRecord(1, "11M", "GATCCCCGATC", false);
        final ReadScore score = Scoring.getReadScore(reference, DELETION);
        assertEquals(ReadType.REF, score.type());
        assertTrue(score.score() > 0);
    }

    @Test
    public void detectsDELinTumor() {
        final SAMRecord tumor = buildSamRecord(1, "3M2D6M", "GATCCGATC", false);
        final ReadScore score = Scoring.getReadScore(tumor, DELETION);
        assertEquals(ReadType.ALT, score.type());
        assertTrue(score.score() > 0);
    }

    @Test
    public void detectsDELinTumorWithINSandDEL() {
        final SAMRecord tumor = buildSamRecord(1, "2M2I1M2D4M", "GATCTCCGA", false);
        final ReadScore score = Scoring.getReadScore(tumor, DELETION);
        assertEquals(ReadType.ALT, score.type());
        assertTrue(score.score() > 0);
    }

    @Test
    public void detectsDELinTumorWithDELAfter() {
        final SAMRecord tumor = buildSamRecord(1, "3M2D2M3D1M", "GATCCC", false);
        final ReadScore score = Scoring.getReadScore(tumor, DELETION);
        assertEquals(ReadType.ALT, score.type());
        assertTrue(score.score() > 0);
    }

    @Test
    public void detectsDELinTumorWithDELPre() {
        final SAMRecord tumor = buildSamRecord(1, "1M1D1M2D6M", "GTCCGATC", false);
        final ReadScore score = Scoring.getReadScore(tumor, DELETION);
        assertEquals(ReadType.ALT, score.type());
        assertTrue(score.score() > 0);
    }

    @Test
    public void detectsDELinTumorWithINSAfter() {
        final SAMRecord tumor = buildSamRecord(1, "3M2D2M2I4M", "GATCCAAGATC", false);
        final ReadScore score = Scoring.getReadScore(tumor, DELETION);
        assertEquals(ReadType.ALT, score.type());
        assertTrue(score.score() > 0);
    }

    @Test
    public void detectsDELinTumorWithINSPre() {
        final SAMRecord tumor = buildSamRecord(1, "2M2I1M2D6M", "GAAATCCGATC", false);
        final ReadScore score = Scoring.getReadScore(tumor, DELETION);
        assertEquals(ReadType.ALT, score.type());
        assertTrue(score.score() > 0);
    }

    @Test
    public void detectsDELatEndOfRead() {
        final SAMRecord tumor = buildSamRecord(1, "3M2D1M", "GATC", false);
        final ReadScore score = Scoring.getReadScore(tumor, DELETION);
        assertEquals(ReadType.ALT, score.type());
        assertTrue(score.score() > 0);
    }

    @Test
    public void detectsDELatEndOfReadWithoutMatchAfter() {
        final SAMRecord tumor = buildSamRecord(1, "3M2D", "GAT", false);
        final ReadScore score = Scoring.getReadScore(tumor, DELETION);
        assertEquals(ReadType.ALT, score.type());
        assertTrue(score.score() > 0);
    }

    @Test
    public void computesScoreForDELinRef() {
        //MIVO: ref with qualities 25, 15, 35 --> average = 25
        final SAMRecord ref = buildSamRecord(3, "3M", "TCC", ":0D", false);
        assertEquals(ImmutableReadScore.of(ReadType.REF, 25), Scoring.getReadScore(ref, DELETION));
    }

    @Test
    public void computesScoreForDELinTumor() {
        //MIVO: take quality of first base after deletion if available
        final SAMRecord alt = buildSamRecord(3, "1M2D1M", "TT", "PA", false);
        assertEquals(ImmutableReadScore.of(ReadType.ALT, 32), Scoring.getReadScore(alt, DELETION));
    }

    @Test
    public void computesScoreForDELinTumorWithDelAtEnd() {
        //MIVO: take quality of base before deletion if base after deletion not present
        final SAMRecord shortAlt = buildSamRecord(3, "1M2D", "T", "P", false);
        assertEquals(ImmutableReadScore.of(ReadType.ALT, 47), Scoring.getReadScore(shortAlt, DELETION));
    }

    @Test
    public void computesScoreForDELinOther() {
        final SAMRecord otherSNV = buildSamRecord(3, "2M", "TG", "FF", false);
        assertEquals(ImmutableReadScore.of(ReadType.REF, 37), Scoring.getReadScore(otherSNV, DELETION));
    }

    @Test
    public void computesScoreForDELinReadWithDeletionOnVariantPos() {
        final SAMRecord deleted = buildSamRecord(2, "1M1D2M", "ACC", "FD", false);
        assertEquals(ImmutableReadScore.of(ReadType.MISSING, 0), Scoring.getReadScore(deleted, DELETION));
    }

    @Test
    public void computesScoreForDELinReadWithPartialDeletion() {
        //MIVO: read with partial deletion TC -> T instead of TCC -> T
        //MIVO: ref with qualities 32, 40 --> average = 36
        final SAMRecord otherDeletion = buildSamRecord(3, "1M1D1M", "TC", "AI", false);
        assertEquals(ImmutableReadScore.of(ReadType.REF, 36), Scoring.getReadScore(otherDeletion, DELETION));
    }

    @Test
    public void doesNotComputeScoreForShorterDELinTumor() {
        final SAMRecord alt = buildSamRecord(3, "1M1D1M", "TT", "PA", false);
        assertEquals(ImmutableReadScore.of(ReadType.REF, 39), Scoring.getReadScore(alt, DELETION));
    }

    @Test
    public void doesNotComputesScoreForDELinLongerDELinTumor() {
        final SAMRecord alt = buildSamRecord(3, "1M3D", "T", "P", false);
        final ReadScore score = Scoring.getReadScore(alt, DELETION);
        assertEquals(ReadType.REF, score.type());
        assertEquals(47, score.score());
    }

    @Test
    public void doesNotComputesScoreForDELinLongerDELinTumor2() {
        final SAMRecord alt = buildSamRecord(3, "1M3D1M", "TT", "PA", false);
        final ReadScore score = Scoring.getReadScore(alt, DELETION);
        assertEquals(ReadType.REF, score.type());
        assertEquals(39, score.score());
    }

    @Test
    public void containsDeletionMNVTest() {
        final VariantContext DELETION = VARIANTS.get(3);
        final VariantContext SNV = VARIANTS.get(4);
        final List<SAMRecord> records = deletionMNVRecords();
        final SAMRecord cleanRecord = records.get(0);
        final SAMRecord deletionMNVRecord = records.get(1);
        assertEquals(32, Scoring.getReadScore(cleanRecord, DELETION).score());
        assertEquals(ReadType.REF, Scoring.getReadScore(cleanRecord, DELETION).type());
        assertEquals(32, Scoring.getReadScore(cleanRecord, SNV).score());
        assertEquals(ReadType.REF, Scoring.getReadScore(cleanRecord, DELETION).type());
        assertEquals(32, Scoring.getReadScore(deletionMNVRecord, DELETION).score());
        assertEquals(ReadType.ALT, Scoring.getReadScore(deletionMNVRecord, DELETION).type());
        assertEquals(32, Scoring.getReadScore(deletionMNVRecord, SNV).score());
        assertEquals(ReadType.ALT, Scoring.getReadScore(deletionMNVRecord, DELETION).type());
    }

    @NotNull
    private List<SAMRecord> deletionMNVRecords() {
        final SAMRecord deletionMNVRecord = buildSamRecord(56654806, "69M1D82M",
                "TCTGTACTTCAGATTAGGAGGAAAAAAAAAAGAAATCAAGCCAGATGCCACAATGGACTAAAACAAGCTTCCGACTTTGCCAGTTGGTTTTGATTGTTTACAAAGAAAAAGCCAAACAAAGAAGGAGGTGGAATTTATTTCAGTAAACAGC",
                false);
        final SAMRecord noMNVRecord = buildSamRecord(56654804, "151M",
                "TGTCTGTACTTCAGATTAGGAGGAAAAAAAAAAGAAATCAAGCCAGATGCCACAATGGACTAAAACAAGCTCCCCGACTTTGCCAGTTGGTTTTGATTGTTTACAAAGAAAAAGCCAAACAAAGAAGGAGGTGGAATTTATTTCAGTAAAC",
                false);
        return Lists.newArrayList(noMNVRecord, deletionMNVRecord);
    }
}