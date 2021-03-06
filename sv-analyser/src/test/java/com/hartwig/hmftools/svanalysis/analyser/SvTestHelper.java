package com.hartwig.hmftools.svanalysis.analyser;

import static com.hartwig.hmftools.common.variant.structural.StructuralVariantType.BND;
import static com.hartwig.hmftools.common.variant.structural.StructuralVariantType.DEL;
import static com.hartwig.hmftools.common.variant.structural.StructuralVariantType.DUP;
import static com.hartwig.hmftools.common.variant.structural.StructuralVariantType.INS;
import static com.hartwig.hmftools.common.variant.structural.StructuralVariantType.INV;
import static com.hartwig.hmftools.common.variant.structural.StructuralVariantType.SGL;
import static com.hartwig.hmftools.svanalysis.analysis.SvClusteringMethods.DEFAULT_PROXIMITY_DISTANCE;
import static com.hartwig.hmftools.svanalysis.analysis.SvUtilities.CHROMOSOME_ARM_P;
import static com.hartwig.hmftools.svanalysis.analysis.SvUtilities.getChromosomalArm;

import java.util.List;

import com.google.common.collect.Lists;
import com.hartwig.hmftools.common.variant.structural.ImmutableStructuralVariantData;
import com.hartwig.hmftools.common.variant.structural.StructuralVariantData;
import com.hartwig.hmftools.common.variant.structural.StructuralVariantType;
import com.hartwig.hmftools.svanalysis.analysis.ClusterAnalyser;
import com.hartwig.hmftools.svanalysis.analysis.LinkFinder;
import com.hartwig.hmftools.svanalysis.analysis.SvClusteringMethods;
import com.hartwig.hmftools.svanalysis.analysis.SvaConfig;
import com.hartwig.hmftools.svanalysis.types.SvCluster;
import com.hartwig.hmftools.svanalysis.types.SvVarData;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;

public class SvTestHelper
{
    public String SampleId;
    public List<SvVarData> AllVariants;
    public SvaConfig Config;
    public SvClusteringMethods ClusteringMethods;
    public ClusterAnalyser Analyser;

    private int mNextVarId;

    public SvTestHelper()
    {
        Config = new SvaConfig(DEFAULT_PROXIMITY_DISTANCE);
        ClusteringMethods = new SvClusteringMethods(Config.ProximityDistance);
        Analyser = new ClusterAnalyser(Config, ClusteringMethods);

        Analyser.setRunValidationChecks(true);

        SampleId = "TEST";
        AllVariants = Lists.newArrayList();

        Analyser.setSampleData(SampleId, AllVariants);
        mNextVarId = 0;

        Configurator.setRootLevel(Level.DEBUG);
    }

    public final String nextVarId() { return String.format("%d", mNextVarId++); }
    public void logVerbose(boolean toggle)
    {
        Config.LogVerbose = toggle;
        Analyser.getChainFinder().setLogVerbose(toggle);
        Analyser.getLinkFinder().setLogVerbose(toggle);
    }

    public void preClusteringInit()
    {
        ClusteringMethods.populateChromosomeBreakendMap(AllVariants);
        ClusteringMethods.annotateNearestSvData();
        LinkFinder.findDeletionBridges(ClusteringMethods.getChrBreakendMap());
        ClusteringMethods.setSimpleVariantLengths(SampleId);
    }

    public void addClusterAndSVs(final SvCluster cluster)
    {
        Analyser.getClusters().add(cluster);
        AllVariants.addAll(cluster.getSVs());
    }

    public void clearClustersAndSVs()
    {
        // in case SVs are to be used again and re-clustered
        for(SvVarData var : AllVariants)
        {
            var.setCluster(null);
        }

        AllVariants.clear();
        ClusteringMethods.clearLOHBreakendData(SampleId);
        Analyser.getClusters().clear();
    }

    public void mergeOnProximity()
    {
        ClusteringMethods.clusterByProximity(AllVariants, Analyser.getClusters());
    }


    public final List<SvCluster> getClusters() { return Analyser.getClusters(); }



    public static SvVarData createSv(final String varId, final String chrStart, final String chrEnd,
            long posStart, long posEnd, int orientStart, int orientEnd, StructuralVariantType type, final String insertSeq)
    {
        return createTestSv(varId, chrStart, chrEnd, posStart, posEnd, orientStart, orientEnd, type,
                2, 2, 1, 1, 1, insertSeq);
    }

    // for convenience
    public static SvVarData createDel(final String varId, final String chromosome, long posStart, long posEnd)
    {
        return createTestSv(varId, chromosome, chromosome, posStart, posEnd, 1, -1, DEL,
                2, 2, 1, 1, 1, "");
    }

    public static SvVarData createIns(final String varId, final String chromosome, long posStart, long posEnd)
    {
        return createTestSv(varId, chromosome, chromosome, posStart, posEnd, 1, -1, INS,
                2, 2, 1, 1, 1, "");
    }

    public static SvVarData createDup(final String varId, final String chromosome, long posStart, long posEnd)
    {
        return createTestSv(varId, chromosome, chromosome, posStart, posEnd, -1, 1, DUP,
                3, 3, 1, 1, 1, "");
    }

    public static SvVarData createInv(final String varId, final String chromosome, long posStart, long posEnd, int orientation)
    {
        return createTestSv(varId, chromosome, chromosome, posStart, posEnd, orientation, orientation, INV,
                orientation == 1 ? 3 : 2, orientation == 1 ? 2 : 3, 1, 1, 1, "");
    }

    public static SvVarData createSgl(final String varId, final String chromosome, long position, int orientation, boolean isNoneSegment)
    {
        SvVarData var = createTestSv(varId, chromosome, "0", position, -1, orientation, -1, SGL,
                2, 0, 1, 0, 1, "");

        return var;
    }

    public static SvVarData createBnd(final String varId, final String chrStart, long posStart, int orientStart, final String chrEnd, long posEnd, int orientEnd)
    {
        SvVarData var = createTestSv(varId, chrStart, chrEnd, posStart, posEnd, orientStart, orientEnd, BND,
                1, 1, 1, 1, 1, "");

        return var;
    }

    public static SvVarData createTestSv(final String varId, final String chrStart, final String chrEnd,
            long posStart, long posEnd, int orientStart, int orientEnd, StructuralVariantType type,
            double cnStart, double cnEnd, double cnChgStart, double cnChgEnd, double ploidy, final String insertSeq)
    {
        StructuralVariantData svData =
                ImmutableStructuralVariantData.builder()
                        .id(varId)
                        .startChromosome(chrStart)
                        .endChromosome(chrEnd)
                        .startPosition(posStart)
                        .endPosition(posEnd)
                        .startOrientation((byte)orientStart)
                        .endOrientation((byte)orientEnd)
                        .startAF(1.0)
                        .adjustedStartAF(1.0)
                        .adjustedStartCopyNumber(cnStart)
                        .adjustedStartCopyNumberChange(cnChgStart)
                        .endAF(1.0)
                        .adjustedEndAF(1.0)
                        .adjustedEndCopyNumber(cnEnd)
                        .adjustedEndCopyNumberChange(cnChgEnd)
                        .ploidy(ploidy)
                        .type(type)
                        .homology("")
                        .vcfId("")
                        .insertSequence(insertSeq)
                        .insertSequenceAlignments("")
                        .filter("PASS")
                        .imprecise(false)
                        .qualityScore(0.0)
                        .event("")
                        .startTumourVariantFragmentCount(0)
                        .startTumourReferenceFragmentCount(0)
                        .startNormalVariantFragmentCount(0)
                        .startNormalReferenceFragmentCount(0)
                        .endTumourVariantFragmentCount(0)
                        .endTumourReferenceFragmentCount(0)
                        .endNormalVariantFragmentCount(0)
                        .endNormalReferenceFragmentCount(0)
                        .startIntervalOffsetStart(0)
                        .startIntervalOffsetEnd(0)
                        .endIntervalOffsetStart(0)
                        .endIntervalOffsetEnd(0)
                        .inexactHomologyOffsetStart(0)
                        .inexactHomologyOffsetEnd(0)
                        .startLinkedBy("")
                        .endLinkedBy("")
                        .startRefContext("")
                        .endRefContext("")
                        .build();

        SvVarData var = new SvVarData(svData);

        String startArm = getChromosomalArm(var.chromosome(true), var.position(true));

        String endArm;
        if(!var.isNullBreakend())
            endArm = getChromosomalArm(var.chromosome(false), var.position(false));
        else
            endArm = CHROMOSOME_ARM_P;

        var.setChromosomalArms(startArm, endArm);

        return var;
    }


}
