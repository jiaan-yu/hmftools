package com.hartwig.hmftools.svannotation.analysis;

import java.util.List;

import com.google.common.collect.Lists;
import com.hartwig.hmftools.common.variant.structural.annotation.EnsemblGeneData;
import com.hartwig.hmftools.common.variant.structural.annotation.GeneAnnotation;
import com.hartwig.hmftools.common.variant.structural.annotation.Transcript;
import com.hartwig.hmftools.common.variant.structural.annotation.TranscriptExonData;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;

public class SvAnnotatorTestUtils
{
    public static void initLogger()
    {
        Configurator.setRootLevel(Level.DEBUG);
    }

    public static GeneAnnotation createGeneAnnotation(int svId, boolean isStart, final String geneName, String stableId, int strand,
            final String chromosome, long position, Byte orientation)
    {
        List<String> synonyms = Lists.newArrayList();
        List<Integer> entrezIds = Lists.newArrayList();
        String karyotypeBand = "";

        GeneAnnotation gene = new GeneAnnotation(svId, isStart, geneName, stableId, strand, synonyms, entrezIds, karyotypeBand);
        gene.setPositionalData(chromosome, position, orientation);

        return gene;
    }

    /*
    public static Transcript createTranscript(final GeneAnnotation parent, @NotNull final String transcriptId,
            final int exonUpstream, final int exonUpstreamPhase, final int exonDownstream, final int exonDownstreamPhase,
            final long codingBases, final long totalCodingBases,
            final int exonMax, final boolean canonical, final long transcriptStart, final long transcriptEnd,
            @Nullable final Long codingStart, @Nullable final Long codingEnd)
    {
        Transcript transcript = new Transcript()
        return transcript;
    }
    */


    // Ensembl data types
    public static EnsemblGeneData createEnsemblGeneData(String geneId, String geneName, String chromosome, int strand, long geneStart, long geneEnd)
    {
        return new EnsemblGeneData(geneId, geneName, chromosome, (byte)strand, geneStart, geneEnd, "", "", "");
    }




}
