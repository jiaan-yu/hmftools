package com.hartwig.hmftools.bachelorpp.types;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.hartwig.hmftools.common.variant.EnrichedSomaticVariant;
import com.hartwig.hmftools.common.variant.SomaticVariant;
import com.hartwig.hmftools.common.variant.VariantConsequence;

import htsjdk.variant.variantcontext.VariantContext;

public class BachelorGermlineVariant implements Comparable<BachelorGermlineVariant>
{
    private String mSampleId;
    private String mSource;
    private String mProgram;
    private String mVariantId;
    private String mGene;
    private String mTranscriptId;
    private String mChromosome;
    private long mPosition;
    private String mRef;
    private String mAlts;
    private String mEffects;
    private List<String> mEffectsList;
    private String mAnnotations;
    private int mPhredScore;
    private boolean mIsHomozygous;
    private String mHgvsProtein;
    private String mHgvsCoding;
    private String mMatchType;
    private String mSignificance;
    private String mDiagnosis;
    private String mCodonInfo;

    private int mGermlineAltCount;
    private int mGermlineReadDepth;
    private int mTumorAltCount;
    private int mTumorReadDepth;
    private boolean mReadDataSet;

    private double mAdjustedVaf;

    private SomaticVariant mSomaticVariant;
    private VariantContext mVariantContext;
    private EnrichedSomaticVariant mEnrichedVariant;

    public static int PHRED_SCORE_CUTOFF = 150;

    public BachelorGermlineVariant(String sampleId, String source, String program, String varId,
            String gene, String transcriptId, String chromosome, long position,
            String ref, String alts, String effects, String annotations, String hgvsProtein,
            boolean isHomozygous, int phredScore, String hgvsCoding, String matchType, String codonInfo)
    {
        mSampleId = sampleId;
        mSource = source;
        mProgram = program;
        mVariantId = varId;
        mGene = gene;
        mTranscriptId = transcriptId;
        mChromosome = chromosome;
        mPosition = position;
        mRef = ref;
        mAlts = alts;
        mAnnotations = annotations;
        mPhredScore = phredScore;
        mIsHomozygous = isHomozygous;
        mHgvsProtein = hgvsProtein;
        mHgvsCoding = hgvsCoding;
        mMatchType = matchType;
        mCodonInfo = codonInfo;

        mRef = mRef.replaceAll("\\*", "");
        mAlts = mAlts.replaceAll("\\*", "");

        mEffects = effects;
        mEffectsList = Arrays.stream(effects.split("&")).collect(Collectors.toList());

        mGermlineAltCount = 0;
        mTumorAltCount = 0;
        mGermlineReadDepth = 0;
        mTumorReadDepth = 0;
        mReadDataSet = false;
        mAdjustedVaf = 0;

        mSignificance = "";
        mDiagnosis = "";

        mSomaticVariant = null;
        mVariantContext = null;
        mEnrichedVariant = null;
    }

    public int compareTo(final BachelorGermlineVariant other)
    {
        // sort based on Chromosome then Position
        if(other.chromosome().equals(mChromosome))
        {
            return mPosition < other.position() ? -1 : 1;
        }
        else
        {
            int chr = chromosomeToInt(mChromosome);
            int otherChr = chromosomeToInt(other.chromosome());

            if(chr > 0 && otherChr > 0)
                return chr < otherChr ? -1 : 1;
            else if(chr > 0)
                return -1;
            else if(otherChr > 0)
                return 1;
            else
                return mChromosome.compareTo(other.chromosome());
        }
    }

    private static int chromosomeToInt(final String chr)
    {
        try
        {
            return Integer.parseInt(chr);
        }
        catch(Exception e)
        {
            return 0;
        }
    }

    public final String variantId() { return mVariantId; };
    public final String sampleId() { return mSampleId; };
    public final String source() { return mSource; };
    public final String program() { return mProgram; };
    public final String gene() { return mGene; };
    public final String transcriptId() { return mTranscriptId; };
    public final String chromosome() { return mChromosome; };
    public long position() { return mPosition; };
    public final String ref() { return mRef; };
    public final String alts() { return mAlts; };
    public final String effects() { return mEffects; };
    public final List<String> effectsList() { return mEffectsList; }
    public final String annotations() { return mAnnotations; };
    public final String hgvsProtein() { return mHgvsProtein; };
    public final String hgvsCoding() { return mHgvsCoding; };
    public boolean isHomozygous() { return mIsHomozygous; }
    public String matchType() { return mMatchType; }
    public int getGermlineAltCount() { return mGermlineAltCount; }
    public int getGermlineRefCount() { return mGermlineReadDepth - mGermlineAltCount; }
    public int getGermlineReadDepth() { return mGermlineReadDepth; }
    public int getTumorAltCount() { return mTumorAltCount; }
    public int getTumorRefCount() { return mTumorReadDepth - mTumorAltCount; }
    public int getTumorReadDepth() { return mTumorReadDepth; }

    public void setTumorData(int altCount, int readDepth)
    {
        mTumorAltCount = altCount;
        mTumorReadDepth = readDepth;
    }

    public void setTumorAltCount(int count) { mTumorAltCount = count; }
    public final String getDiagnosis() { return mDiagnosis; }
    public final String getSignificance() { return mSignificance; }
    public final String codonInfo() { return mCodonInfo; }

    public void setDiagnosis(final String text) { mDiagnosis = text; }
    public void setSignificance(final String text) { mSignificance = text; }

    public void setReadData(int glCount, int glReadDepth, int tumorCount, int tumorReadDepth)
    {
        mGermlineAltCount = glCount;
        mGermlineReadDepth = glReadDepth;
        mTumorAltCount = tumorCount;
        mTumorReadDepth = tumorReadDepth;
        mReadDataSet = true;
    }

    public boolean hasEffect(final VariantConsequence consequence)
    {
        for(final String effect : mEffectsList)
        {
            if(consequence.isParentTypeOf(effect))
                return true;
        }

        return false;
    }

    public boolean isReadDataSet() { return mReadDataSet; }

    public void setAdjustedVaf(double vaf) { mAdjustedVaf = vaf; }
    public double getAdjustedVaf() { return mAdjustedVaf; }

    public boolean isBiallelic()
    {
        if(mEnrichedVariant == null)
            return false;

        double copyNumber = mEnrichedVariant.adjustedCopyNumber();
        double minorAllelePloidy = copyNumber - (copyNumber * mAdjustedVaf);
        return (minorAllelePloidy < 0.5);
    }

    public boolean isValid()
    {
        return mSomaticVariant != null && mEnrichedVariant != null && mVariantContext != null;
    }

    public int phredScore()
    {
        return mPhredScore;
    }
    public boolean isLowScore()
    {
        return mPhredScore < PHRED_SCORE_CUTOFF && mAdjustedVaf < 0;
    }

    public final SomaticVariant getSomaticVariant() { return mSomaticVariant; }
    public final EnrichedSomaticVariant getEnrichedVariant() { return mEnrichedVariant; }

    public void setSomaticVariant(final SomaticVariant var) { mSomaticVariant = var; }
    public void setVariantContext(final VariantContext var) { mVariantContext = var; }
    public void setEnrichedVariant(final EnrichedSomaticVariant var) { mEnrichedVariant = var; }
}
