package com.hartwig.hmftools.bamrecovery;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.List;

import com.google.common.io.Resources;

import org.junit.Test;

public class BamRecoveryTest {
    private static final String EOF_BAM = Resources.getResource("eof.bam").getPath();
    private static final String TRUNCATED_BAM = Resources.getResource("truncated.bam").getPath();
    private static final String GOOD_BAM = Resources.getResource("good.bam").getPath();
    private static final String GOOD_EOF_BAM = Resources.getResource("goodEof.bam").getPath();
    private static final String BIT_FLIP_BAM = Resources.getResource("bitFlip.bam").getPath();
    private static final String MULTIPLE_ARCHIVES_BAM = Resources.getResource("multipleArchives.bam").getPath();

    @Test
    public void canReadEofBam() throws IOException {
        final List<Archive> validArchives = BamRecovery.getValidArchives(new BamFile(EOF_BAM)).toList().blockingGet();
        assertEquals(0, validArchives.size());
    }

    @Test
    public void skipsTruncatedBamWithoutEof() throws IOException {
        final List<Archive> validArchives = BamRecovery.getValidArchives(new BamFile(TRUNCATED_BAM)).toList().blockingGet();
        assertEquals(0, validArchives.size());
    }

    @Test
    public void readsGoodBamWithoutEof() throws IOException {
        final List<Archive> validArchives = BamRecovery.getValidArchives(new BamFile(GOOD_BAM)).toList().blockingGet();
        assertEquals(1, validArchives.size());
    }

    @Test
    public void readsGoodBamWithEof() throws IOException {
        final List<Archive> validArchives = BamRecovery.getValidArchives(new BamFile(GOOD_EOF_BAM)).toList().blockingGet();
        assertEquals(1, validArchives.size());
    }

    @Test
    public void reads3Good1TruncatedArchivesWithEof() throws IOException {
        final List<Archive> validArchives = BamRecovery.getValidArchives(new BamFile(MULTIPLE_ARCHIVES_BAM)).toList().blockingGet();
        assertEquals(3, validArchives.size());
    }

    @Test
    public void skipsBitFlippedBamWithoutEof() throws IOException {
        final List<Archive> validArchives = BamRecovery.getValidArchives(new BamFile(BIT_FLIP_BAM)).toList().blockingGet();
        assertEquals(0, validArchives.size());
    }
}
