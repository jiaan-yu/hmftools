package com.hartwig.hmftools.svannotation;

import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.min;

import static com.hartwig.hmftools.common.io.FileWriterUtils.closeBufferedWriter;
import static com.hartwig.hmftools.common.io.FileWriterUtils.createBufferedWriter;
import static com.hartwig.hmftools.common.variant.structural.annotation.EnsemblGeneData.GENE_PHASING_REGION_5P_UTR;
import static com.hartwig.hmftools.common.variant.structural.annotation.EnsemblGeneData.GENE_PHASING_REGION_CODING_0;
import static com.hartwig.hmftools.common.variant.structural.annotation.EnsemblGeneData.GENE_PHASING_REGION_CODING_1;
import static com.hartwig.hmftools.common.variant.structural.annotation.EnsemblGeneData.GENE_PHASING_REGION_CODING_2;
import static com.hartwig.hmftools.common.variant.structural.annotation.EnsemblGeneData.GENE_PHASING_REGION_MAX;
import static com.hartwig.hmftools.common.variant.structural.annotation.EnsemblGeneData.phaseToRegion;
import static com.hartwig.hmftools.common.variant.structural.annotation.Transcript.TRANS_CODING_TYPE_CODING;

import static org.ensembl.database.homo_sapiens_core.tables.Exon.EXON;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hartwig.hmftools.common.variant.structural.StructuralVariant;
import com.hartwig.hmftools.common.variant.structural.annotation.EnsemblGeneData;
import com.hartwig.hmftools.common.variant.structural.annotation.GeneAnnotation;
import com.hartwig.hmftools.common.variant.structural.annotation.StructuralVariantAnnotation;
import com.hartwig.hmftools.common.variant.structural.annotation.Transcript;
import com.hartwig.hmftools.common.variant.structural.annotation.TranscriptExonData;
import com.hartwig.hmftools.common.variant.structural.annotation.TranscriptProteinData;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jooq.Record;

public class SvGeneTranscriptCollection
{
    private String mDataPath;

    private Map<String, List<TranscriptExonData>> mGeneTransExonDataMap;
    private Map<String, List<EnsemblGeneData>> mChromosomeGeneDataMap;
    private Map<String, List<EnsemblGeneData>> mChromosomeReverseGeneDataMap; // order by gene end not start
    private Map<Integer, List<TranscriptProteinData>> mEnsemblProteinDataMap;

    private BufferedWriter mBreakendWriter;

    // to get a wider range of candidate genes and filter by promotor distance later on
    public static int PRE_GENE_PROMOTOR_DISTANCE = 100000;

    private static final Logger LOGGER = LogManager.getLogger(SvGeneTranscriptCollection.class);

    public SvGeneTranscriptCollection()
    {
        mGeneTransExonDataMap = Maps.newHashMap();
        mChromosomeGeneDataMap = Maps.newHashMap();
        mChromosomeReverseGeneDataMap = Maps.newHashMap();
        mEnsemblProteinDataMap = Maps.newHashMap();
        mBreakendWriter = null;
    }

    public void setDataPath(final String dataPath)
    {
        mDataPath = dataPath;

        if(!mDataPath.endsWith(File.separator))
            mDataPath += File.separator;
    }

    public boolean hasCachedEnsemblData()
    {
        return !mChromosomeGeneDataMap.isEmpty() && !mGeneTransExonDataMap.isEmpty();
    }

    public final Map<String, List<TranscriptExonData>> getGeneExonDataMap() { return mGeneTransExonDataMap; }
    public final Map<String, List<EnsemblGeneData>> getChrGeneDataMap() { return mChromosomeGeneDataMap; }
    public Map<Integer, List<TranscriptProteinData>> getTranscriptProteinDataMap() { return mEnsemblProteinDataMap; }

    public final EnsemblGeneData getGeneData(final String geneName)
    {
        for(Map.Entry<String, List<EnsemblGeneData>> entry : mChromosomeGeneDataMap.entrySet())
        {
            for(final EnsemblGeneData geneData : entry.getValue())
            {
                if(geneData.GeneName.equals(geneName))
                    return geneData;
            }
        }

        return null;
    }

    public static int EXON_RANK_MIN = 0;
    public static int EXON_RANK_MAX = 1;

    public int[] getExonData(final String geneName, long exonPosition)
    {
        int[] exonData = new int[EXON_RANK_MAX+1];

        final EnsemblGeneData geneData = getGeneData(geneName);

        if(geneData == null)
            return exonData;

        final List<TranscriptExonData> transExonDataList = getTransExonData(geneData.GeneId);

        if(transExonDataList == null)
            return exonData;

        int minRank = -1;
        int maxRank = -1;
        for(final TranscriptExonData transExonData : transExonDataList)
        {
            int exonRank = transExonData.ExonRank;
            // final String transcriptStableId = transcriptData.get(TRANSCRIPT.STABLE_ID);
            // final UInteger transcriptId = transcriptData.get(TRANSCRIPT.TRANSCRIPT_ID);

            minRank = minRank == -1 ? exonRank : min(exonRank, minRank);
            maxRank = max(exonRank, maxRank);
        }

        exonData[EXON_RANK_MIN] = minRank;
        exonData[EXON_RANK_MAX] = maxRank;
        return exonData;

    }

    public List<TranscriptExonData> getTransExonData(final String geneId)
    {
        return mGeneTransExonDataMap.get(geneId);
    }

    private static int SPECIFIC_VAR_ID = -1;
    // private static int SPECIFIC_VAR_ID = 4558066;

    public List<GeneAnnotation> findGeneAnnotationsBySv(int svId, boolean isStart, final String chromosome, long position,
            int upstreamDistance)
    {
        List<GeneAnnotation> geneAnnotations = Lists.newArrayList();

        if(svId == SPECIFIC_VAR_ID)
        {
            LOGGER.debug("specific SV({})", svId);
        }

        final List<EnsemblGeneData> geneRegions = mChromosomeGeneDataMap.get(chromosome);
        final List<EnsemblGeneData> geneRegionsReversed = mChromosomeReverseGeneDataMap.get(chromosome);

        if(geneRegions == null)
            return geneAnnotations;

        final List<EnsemblGeneData> matchedGenes = findGeneRegions(position, geneRegions, upstreamDistance);

        // now look up relevant transcript and exon information
        for(final EnsemblGeneData geneData : matchedGenes)
        {
            final List<TranscriptExonData> transExonDataList = mGeneTransExonDataMap.get(geneData.GeneId);

            if (transExonDataList == null || transExonDataList.isEmpty())
                continue;

            GeneAnnotation currentGene = new GeneAnnotation(svId, isStart, geneData.GeneName, geneData.GeneId,
                    geneData.Strand, geneData.Synonyms, geneData.EntrezIds, geneData.KaryotypeBand);

            // collect up all the relevant exons for each unique transcript to analyse as a collection

            int teIndex = 0;
            List<TranscriptExonData> transcriptExons = nextTranscriptExons(transExonDataList, teIndex);

            while(!transcriptExons.isEmpty())
            {
                Transcript transcript = extractTranscriptExonData(transcriptExons, position, currentGene);

                if(transcript != null)
                {
                    currentGene.addTranscript(transcript);

                    // annotate with preceding gene info if the up distance isn't set
                    if(transcript.exonDistanceUp() == -1)
                    {
                        EnsemblGeneData precedingGene = findPrecedingGene(geneData, geneData.Strand == 1 ? geneRegionsReversed : geneRegions);
                        if(precedingGene != null)
                        {
                            currentGene.setPrecedingGeneId(precedingGene.GeneId);

                            long preDistance = geneData.Strand == 1 ? position - precedingGene.GeneEnd : precedingGene.GeneStart - position;
                            transcript.setExonDistances((int)preDistance, transcript.exonDistanceDown());
                        }
                    }
                }

                teIndex += transcriptExons.size();
                transcriptExons = nextTranscriptExons(transExonDataList, teIndex);
            }

            geneAnnotations.add(currentGene);
        }

        return geneAnnotations;
    }

    public static List<TranscriptExonData> nextTranscriptExons(final List<TranscriptExonData> transExonDataList, int currentIndex)
    {
        List<TranscriptExonData> transcriptExons = Lists.newArrayList();

        if(currentIndex >= transExonDataList.size())
            return transcriptExons;

        int transId = transExonDataList.get(currentIndex).TransId;

        int j = currentIndex;
        for(; j < transExonDataList.size(); ++j)
        {
            if(transExonDataList.get(j).TransId != transId)
                break;

            transcriptExons.add(transExonDataList.get(j));
        }

        return transcriptExons;
    }

    private List<EnsemblGeneData> findGeneRegions(long position, List<EnsemblGeneData> geneDataList, int upstreamDistance)
    {
        List<EnsemblGeneData> matchedGenes = Lists.newArrayList();

        for(final EnsemblGeneData geneData : geneDataList)
        {
            long geneStartRange = geneData.Strand == 1 ? geneData.GeneStart - upstreamDistance : geneData.GeneStart;
            long geneEndRange = geneData.Strand == 1 ? geneData.GeneEnd : geneData.GeneEnd + upstreamDistance;

            if(position >= geneStartRange && position <= geneEndRange)
            {
                matchedGenes.add(geneData);
            }
        }

        return matchedGenes;
    }

    private EnsemblGeneData findPrecedingGene(final EnsemblGeneData geneData, List<EnsemblGeneData> geneDataList)
    {
        // find the first upstream non-overlapping gene
        if(geneData.Strand == 1)
        {
            for(int i = geneData.getReverseListIndex() - 1; i >= 0; --i)
            {
                final EnsemblGeneData gene = geneDataList.get(i);

                if(gene.Strand != geneData.Strand)
                    continue;

                if(gene.GeneEnd > geneData.GeneStart)
                    continue;

                return gene;
            }
        }
        else
        {
            for(int i = geneData.getListIndex() + 1; i < geneDataList.size(); ++i)
            {
                final EnsemblGeneData gene = geneDataList.get(i);

                if(gene.Strand != geneData.Strand)
                    continue;

                if(gene.GeneStart < geneData.GeneEnd)
                    continue;

                return gene;
            }
        }

        return null;
    }

    private Transcript extractTranscriptExonData(final List<TranscriptExonData> transcriptExons, long position, final GeneAnnotation geneAnnotation)
    {
        int exonMax = transcriptExons.size();

        final TranscriptExonData first = transcriptExons.get(0);

        boolean isForwardStrand = geneAnnotation.Strand == 1;

        int upExonRank = -1;
        int upExonPhase = -1;
        int downExonRank = -1;
        int downExonPhase = -1;
        long nextUpDistance = -1;
        long nextDownDistance = -1;
        boolean isCodingTypeOverride = false;

        // first check for a position outside the exon boundaries
        final TranscriptExonData firstExon = transcriptExons.get(0);
        final TranscriptExonData lastExon = transcriptExons.get(transcriptExons.size()-1);

        // for forward-strand transcripts the current exon is downstream, the previous is upstream
        // and the end-phase is taken from the upstream previous exon, the phase from the current downstream exon

        // for reverse-strand transcripts the current exon is upstream, the previous is downstream
        // and the end-phase is taken from the upstream (current) exon, the phase from the downstream (previous) exon

        if(position < firstExon.ExonStart)
        {
            if(isForwardStrand)
            {
                // proceed to the next exon assuming its splice acceptor is required
                final TranscriptExonData firstSpaExon = transcriptExons.size() > 1 ? transcriptExons.get(1) : firstExon;
                downExonRank = firstSpaExon.ExonRank;
                downExonPhase = firstSpaExon.ExonPhase;
                nextDownDistance = firstSpaExon.ExonStart - position;

                // correct the phasing if the next exon starts the coding region
                if(firstSpaExon.CodingStart != null && firstSpaExon.ExonStart == firstSpaExon.CodingStart)
                    downExonPhase = -1;

                isCodingTypeOverride = firstSpaExon.CodingStart != null && firstSpaExon.ExonStart > firstSpaExon.CodingStart;

                upExonRank = 0;
                upExonPhase = -1;
            }
            else
            {
                // falls after the last exon on forward strand or before the first on reverse strand makes this position downstream
                return null;
            }
        }
        else if(position > lastExon.ExonEnd)
        {
            if(!isForwardStrand)
            {
                final TranscriptExonData firstSpaExon = transcriptExons.size() > 1 ? transcriptExons.get(transcriptExons.size()-2) : lastExon;
                downExonRank = firstSpaExon.ExonRank;
                downExonPhase = firstSpaExon.ExonPhase;
                nextDownDistance = position - lastExon.ExonEnd;

                if(firstSpaExon.CodingEnd != null && firstSpaExon.ExonEnd == firstSpaExon.CodingEnd)
                    downExonPhase = -1;

                isCodingTypeOverride = firstSpaExon.CodingEnd != null && firstSpaExon.ExonEnd < firstSpaExon.CodingEnd;

                upExonRank = 0;
                upExonPhase = -1;
            }
            else
            {
                // falls after the last exon on forward strand or before the first on reverse strand makes this position downstream
                return null;
            }
        }
        else
        {
            for (int index = 0; index < transcriptExons.size(); ++index)
            {
                final TranscriptExonData exonData = transcriptExons.get(index);

                if (position >= exonData.ExonStart && position <= exonData.ExonEnd)
                {
                    // falls within an exon
                    upExonRank = downExonRank = exonData.ExonRank;
                    upExonPhase = exonData.ExonPhase;
                    downExonPhase = exonData.ExonPhaseEnd;
                    nextDownDistance = isForwardStrand ? exonData.ExonEnd - position : position - exonData.ExonStart;
                    nextUpDistance = isForwardStrand ? position - exonData.ExonStart : exonData.ExonEnd - position;
                    break;
                }
                else if(position < exonData.ExonStart)
                {
                    // position falls between this exon and the previous one
                    final TranscriptExonData prevExonData = transcriptExons.get(index-1);

                    if(isForwardStrand)
                    {
                        // the current exon is downstream, the prevous one is upstream
                        upExonRank = prevExonData.ExonRank;
                        upExonPhase = prevExonData.ExonPhaseEnd;
                        downExonRank = exonData.ExonRank;
                        downExonPhase = exonData.ExonPhase;
                        nextDownDistance = exonData.ExonStart - position;
                        nextUpDistance = position - prevExonData.ExonEnd;

                        if(exonData.CodingStart != null && exonData.ExonStart == exonData.CodingStart)
                            downExonPhase = -1;
                    }
                    else
                    {
                        // the previous exon in the list has the higher rank and is dowstream
                        upExonRank = exonData.ExonRank;
                        upExonPhase = exonData.ExonPhaseEnd;
                        downExonRank = prevExonData.ExonRank;
                        downExonPhase = prevExonData.ExonPhase;
                        nextUpDistance = exonData.ExonStart - position;
                        nextDownDistance = position - prevExonData.ExonEnd;

                        if(exonData.CodingEnd != null && prevExonData.ExonEnd == exonData.CodingEnd)
                            downExonPhase = -1;
                    }

                    break;
                }
            }
        }

        // now calculate coding bases for this transcript
        // for the given position, determine how many coding bases occur prior to the position
        // in the direction of the transcript

        boolean isCoding = first.CodingStart != null && first.CodingEnd != null;
        long codingStart = first.CodingStart != null ? first.CodingStart : 0;
        long codingEnd = first.CodingEnd != null ? first.CodingEnd : 0;
        boolean inCodingRegion = false;
        boolean codingRegionEnded = false;

        long codingBases = 0;
        long totalCodingBases = 0;

        if(isCoding)
        {
            for (TranscriptExonData exonData : transcriptExons)
            {
                long exonStart = exonData.ExonStart;
                long exonEnd = exonData.ExonEnd;

                if (!inCodingRegion)
                {
                    if (exonEnd >= codingStart)
                    {
                        // coding region begins in this exon
                        inCodingRegion = true;

                        totalCodingBases += exonEnd - codingStart + 1;

                        // check whether the position falls in this exon and if so before or after the coding start
                        if (position >= codingStart)
                        {
                            if (position < exonEnd)
                                codingBases += position - codingStart + 1;
                            else
                                codingBases += exonEnd - codingStart + 1;
                        }
                    }
                }
                else if (!codingRegionEnded)
                {
                    if (exonStart > codingEnd)
                    {
                        codingRegionEnded = true;
                    }
                    else if (exonEnd > codingEnd)
                    {
                        // coding region ends in this exon
                        codingRegionEnded = true;

                        totalCodingBases += codingEnd - exonStart + 1;

                        if (position >= exonStart)
                        {
                            if (position < codingEnd)
                                codingBases += position - exonStart + 1;
                            else
                                codingBases += codingEnd - exonStart + 1;
                        }
                    }
                    else
                    {
                        // take all of the exon's bases
                        totalCodingBases += exonEnd - exonStart + 1;

                        if (position >= exonStart)
                        {
                            if (position < exonEnd)
                                codingBases += position - exonStart + 1;
                            else
                                codingBases += exonEnd - exonStart + 1;
                        }
                    }
                }
            }

            if (!isForwardStrand)
            {
                codingBases = totalCodingBases - codingBases;
            }
        }

        Transcript transcript = new Transcript(geneAnnotation, first.TransId, first.TransName,
                upExonRank, upExonPhase, downExonRank, downExonPhase,
                (int)codingBases, (int)totalCodingBases,
                exonMax, first.IsCanonical, first.TransStart, first.TransEnd,
                first.CodingStart, first.CodingEnd);

        transcript.setBioType(first.BioType);
        transcript.setExonDistances((int)nextUpDistance, (int)nextDownDistance);

        if(isCodingTypeOverride)
            transcript.setCodingType(TRANS_CODING_TYPE_CODING);

        return transcript;
    }

    public String getExonDetailsForPosition(final GeneAnnotation gene, long posStart, long posEnd)
    {
        String exonDataStr = "";

        List<TranscriptExonData> transExonDataList = getTransExonData(gene.StableId);

        int teIndex = 0;
        List<TranscriptExonData> transcriptExons = nextTranscriptExons(transExonDataList, teIndex);

        while (!transcriptExons.isEmpty())
        {
            int exonCount = transcriptExons.size();
            for (int i = 0; i < exonCount; ++i)
            {
                final TranscriptExonData exonData = transcriptExons.get(i);

                if(abs(exonData.ExonStart - posStart) > 1 || abs(exonData.ExonEnd - posEnd) > 1)
                    continue;

                // found a match
                if (exonDataStr.isEmpty() || exonData.IsCanonical)
                {
                    exonDataStr = String.format("%s;%d;%d;%d",
                            exonData.TransName, exonData.ExonRank, exonCount, exonData.ExonEnd - exonData.ExonStart);

                    if (exonData.IsCanonical)
                        break;
                }
            }

            teIndex += transcriptExons.size();
            transcriptExons = nextTranscriptExons(transExonDataList, teIndex);
        }

        return exonDataStr;
    }

    public boolean loadEnsemblData()
    {
        if(!EnsemblDAO.loadTranscriptExonData(mDataPath, mGeneTransExonDataMap))
            return false;

        if(EnsemblDAO.loadEnsemblGeneData(mDataPath, mChromosomeGeneDataMap, mChromosomeReverseGeneDataMap))
            return false;

        if(!EnsemblDAO.loadTranscriptProteinData(mDataPath, mEnsemblProteinDataMap))
            return false;

        return true;
    }

    public void writeBreakendData(final String sampleId, final List<StructuralVariantAnnotation> annotations)
    {
        if(mDataPath.isEmpty())
            return;

        LOGGER.debug("writing {} breakend data to file", annotations.size());

        try
        {
            if(mBreakendWriter == null)
            {
                mBreakendWriter = createBufferedWriter(mDataPath + "SV_BREAKENDS.csv", false);

                mBreakendWriter.write("SampleId,SvId,IsStart,Chromosome,Position,Orientation,Type");
                mBreakendWriter.write(",GeneName,GeneStableId,GeneStrand,TranscriptId,IsCanonical,BioType,TransStart,TransEnd");
                mBreakendWriter.write(",ExonRankUp,ExonPhaseUp,ExonRankDown,ExonPhaseDown,CodingBases,TotalCodingBases,Disruptive");
                mBreakendWriter.write(",ExonMax,CodingStart,CodingEnd,RegionType,CodingType,ExonDistanceUp,ExonDistanceDown");
                mBreakendWriter.newLine();
            }

            BufferedWriter writer = mBreakendWriter;

            for(final StructuralVariantAnnotation annotation : annotations)
            {
                if(annotation.annotations().isEmpty())
                    continue;

                for(final GeneAnnotation geneAnnotation : annotation.annotations())
                {
                    for(final Transcript transcript : geneAnnotation.transcripts())
                    {
                        final StructuralVariant var = annotation.variant();

                        boolean isStart = geneAnnotation.isStart();

                        writer.write(String.format("%s,%d,%s,%s,%d,%d,%s",
                                sampleId, var.primaryKey(), isStart, var.chromosome(isStart), var.position(isStart),
                                var.orientation(isStart), var.type()));

                        // Gene info: geneName, geneStableId, geneStrand, transcriptId
                        writer.write(
                                String.format(",%s,%s,%d,%s,%s,%s,%d,%d",
                                        geneAnnotation.GeneName, geneAnnotation.StableId, geneAnnotation.Strand,
                                        transcript.StableId, transcript.isCanonical(), transcript.bioType(),
                                        transcript.transcriptStart(), transcript.transcriptEnd()));

                        // Transcript info: exonUpstream, exonUpstreamPhase, exonDownstream, exonDownstreamPhase, exonStart, exonEnd, exonMax, canonical, codingStart, codingEnd
                        writer.write(
                                String.format(",%d,%d,%d,%d,%d,%d,%s",
                                        transcript.exonUpstream(), transcript.exonUpstreamPhase(),
                                        transcript.exonDownstream(), transcript.exonDownstreamPhase(),
                                        transcript.codingBases(), transcript.totalCodingBases(), transcript.isDisruptive()));

                        writer.write(
                                String.format(",%d,%d,%d,%s,%s,%d,%d",
                                        transcript.exonMax(), transcript.codingStart(), transcript.codingEnd(),
                                        transcript.regionType(), transcript.codingType(),
                                        transcript.exonDistanceUp(), transcript.exonDistanceDown()));

                        writer.newLine();
                    }
                }
            }
        }
        catch (final IOException e)
        {
            LOGGER.error("error writing breakend data: {}", e.toString());
        }
    }

    public void close()
    {
        closeBufferedWriter(mBreakendWriter);
    }

    public void writeGeneProbabilityData()
    {
        for(Map.Entry<String, List<EnsemblGeneData>> entry : mChromosomeGeneDataMap.entrySet())
        {
            for(final EnsemblGeneData geneData :entry.getValue())
            {
                final List<TranscriptExonData> transExonDataList = getTransExonData(geneData.GeneId);

                setGenePhasingCounts(geneData, transExonDataList);
            }
        }
    }

    public static void setGenePhasingCounts(final EnsemblGeneData geneData, final List<TranscriptExonData> transExonDataList)
    {
        long geneStart = geneData.GeneStart;
        long geneEnd = geneData.GeneEnd;
        int geneLength = (int) (geneEnd - geneStart + 1);

        boolean[][] geneBases = new boolean[geneLength][GENE_PHASING_REGION_MAX];

        boolean hasCodingExons = false;
        int teIndex = 0;
        List<TranscriptExonData> transcriptExons = nextTranscriptExons(transExonDataList, teIndex);

        while (!transcriptExons.isEmpty())
        {
            if (geneData.Strand == 1)
            {
                for (int i = 0; i < transcriptExons.size(); ++i)
                {
                    final TranscriptExonData exonData = transcriptExons.get(i);

                    if (exonData.CodingStart == null)
                        break;

                    hasCodingExons = true;

                    if (exonData.ExonStart > exonData.CodingEnd) // past end of coding region
                        break;

                    // first mark the entire pre-coding region as 5' UTR
                    if (i == 0)
                    {
                        for (long j = geneStart; j <= exonData.CodingStart; ++j)
                        {
                            int gbPos = (int) (j - geneStart);
                            geneBases[gbPos][GENE_PHASING_REGION_5P_UTR] = true;
                        }
                    }

                    if (exonData.ExonEnd < exonData.CodingStart) // already accounted for
                        continue;

                    // now handle the exon's phasing
                    long codingStart = max(exonData.ExonStart, exonData.CodingStart);
                    for (long j = codingStart; j <= exonData.ExonEnd; ++j)
                    {
                        int gbPos = (int) (j - geneStart);

                        if (j > exonData.CodingEnd)
                            break;

                        long adjustedPhase = exonData.ExonPhase + (j - codingStart);
                        int calcPhase = (int) (adjustedPhase % 3);
                        geneBases[gbPos][phaseToRegion(calcPhase)] = true;
                    }

                    // fill in the intronic phasing between coding exons
                    if (i < transcriptExons.size() - 1)
                    {
                        final TranscriptExonData nextExon = transcriptExons.get(i + 1);

                        if (nextExon.ExonStart <= nextExon.CodingEnd)
                        {
                            int regionType = phaseToRegion(exonData.ExonPhaseEnd);

                            for (long j = exonData.ExonEnd + 1; j < nextExon.ExonStart; ++j)
                            {
                                int gbPos = (int) (j - geneStart);
                                geneBases[gbPos][regionType] = true;
                            }
                        }
                    }
                }
            }
            else
            {
                // navigate through as per the exon rank
                for (int i = transcriptExons.size() - 1; i >= 0; --i)
                {
                    final TranscriptExonData exonData = transcriptExons.get(i);

                    if (exonData.CodingStart == null)
                        break;

                    hasCodingExons = true;

                    if (exonData.ExonEnd < exonData.CodingStart) // past end of coding region
                        break;

                    // first mark the entire pre-coding region as 5' UTR
                    if (i == transcriptExons.size() - 1)
                    {
                        for (long j = exonData.CodingEnd; j <= geneEnd; ++j)
                        {
                            int gbPos = (int) (geneEnd - j);
                            geneBases[gbPos][GENE_PHASING_REGION_5P_UTR] = true;
                        }
                    }

                    if (exonData.ExonStart > exonData.CodingEnd) // already accounted for
                        continue;

                    // allocate the exon's phasing - working backwwards this time
                    long codingEnd = min(exonData.ExonEnd, exonData.CodingEnd);
                    for (long j = codingEnd; j >= exonData.ExonStart; --j)
                    {
                        int gbPos = (int) (geneEnd - j);

                        if (j < exonData.CodingStart)
                            break;

                        long adjustedPhase = exonData.ExonPhase + (codingEnd - j);
                        int calcPhase = (int) (adjustedPhase % 3);
                        geneBases[gbPos][phaseToRegion(calcPhase)] = true;
                    }

                    // fill in the intronic phasing between coding exons
                    if (i > 0)
                    {
                        final TranscriptExonData nextExon = transcriptExons.get(i - 1);

                        if (nextExon.ExonEnd >= nextExon.CodingStart)
                        {
                            int regionType = phaseToRegion(exonData.ExonPhaseEnd);

                            for (long j = nextExon.ExonEnd + 1; j < exonData.ExonStart; ++j)
                            {
                                int gbPos = (int) (geneEnd - j);
                                geneBases[gbPos][regionType] = true;
                            }
                        }
                    }
                }
            }

            teIndex += transcriptExons.size();
            transcriptExons = nextTranscriptExons(transExonDataList, teIndex);
        }

        // now compute the number of bases for each phasing region
        int[] regionTotals = geneData.getRegionTotals();

        for(int i = 0; i < geneLength; ++i)
        {
            for(int j = 0; j < GENE_PHASING_REGION_MAX; ++j)
            {
                if(geneBases[i][j])
                    ++regionTotals[j];
            }
        }

        if(hasCodingExons)
        {
            LOGGER.debug("gene({}) length({}) region counts: pre-coding({}) phases(0={} 1={} 2={})",
                    geneData.GeneId, geneData.GeneName, geneLength, regionTotals[GENE_PHASING_REGION_5P_UTR],
                    regionTotals[GENE_PHASING_REGION_CODING_0], regionTotals[GENE_PHASING_REGION_CODING_1],
                    regionTotals[GENE_PHASING_REGION_CODING_2]);
        }
    }

}
