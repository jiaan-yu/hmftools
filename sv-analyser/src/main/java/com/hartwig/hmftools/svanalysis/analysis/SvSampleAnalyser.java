package com.hartwig.hmftools.svanalysis.analysis;

import com.google.common.collect.Lists;
import com.hartwig.hmftools.common.numeric.PerformanceCounter;
import com.hartwig.hmftools.common.variant.structural.EnrichedStructuralVariant;
import com.hartwig.hmftools.common.variant.structural.StructuralVariantType;
import com.hartwig.hmftools.svanalysis.annotators.ExternalSVAnnotator;
import com.hartwig.hmftools.svanalysis.annotators.FragileSiteAnnotator;
import com.hartwig.hmftools.svanalysis.annotators.GeneAnnotator;
import com.hartwig.hmftools.svanalysis.annotators.LineElementAnnotator;
import com.hartwig.hmftools.svanalysis.annotators.SvPONAnnotator;
import com.hartwig.hmftools.svanalysis.types.SvChain;
import com.hartwig.hmftools.svanalysis.types.SvClusterData;
import com.hartwig.hmftools.svanalysis.types.SvLinkedPair;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;

public class SvSampleAnalyser {

    private final SvClusteringConfig mConfig;
    private final SvUtilities mClusteringUtils;
    private final ClusterAnalyser mAnalyser;

    // data per run (ie sample)
    private String mSampleId;
    private List<SvClusterData> mAllVariants; // the original list to analyse
    private int mNextClusterId;

    private List<SvCluster> mClusters;

    BufferedWriter mFileWriter;
    SvPONAnnotator mSvPONAnnotator;
    FragileSiteAnnotator mFragileSiteAnnotator;
    LineElementAnnotator mLineElementAnnotator;
    ExternalSVAnnotator mExternalAnnotator;
    SvClusteringMethods mClusteringMethods;
    GeneAnnotator mGeneAnnotator;

    PerformanceCounter mPerfCounter;
    PerformanceCounter mPC1;
    PerformanceCounter mPC2;
    PerformanceCounter mPC3;
    PerformanceCounter mPC4;
    PerformanceCounter mPC5;
    PerformanceCounter mPC6;

    private static final Logger LOGGER = LogManager.getLogger(SvSampleAnalyser.class);

    public SvSampleAnalyser(final SvClusteringConfig config)
    {
        mConfig = config;
        mClusteringUtils = new SvUtilities(mConfig.getClusterBaseDistance());
        mFileWriter = null;
        mAnalyser = new ClusterAnalyser(config, mClusteringUtils);

        mClusteringMethods = new SvClusteringMethods(mClusteringUtils);

        mSvPONAnnotator = new SvPONAnnotator();
        mSvPONAnnotator.loadPonFile(mConfig.getSvPONFile());

        mFragileSiteAnnotator = new FragileSiteAnnotator();
        mFragileSiteAnnotator.loadFragileSitesFile(mConfig.getFragileSiteFile());

        mLineElementAnnotator = new LineElementAnnotator();
        mLineElementAnnotator.loadLineElementsFile(mConfig.getLineElementFile());

        mExternalAnnotator = new ExternalSVAnnotator();
        mExternalAnnotator.loadFile(mConfig.getExternalAnnotationsFile());

        mGeneAnnotator = new GeneAnnotator();
        mGeneAnnotator.loadGeneDriverFile(mConfig.getGeneDataFile());

        mPerfCounter = new PerformanceCounter("Total");
        mPC1 = new PerformanceCounter("Annotate&Filter");
        mPC3 = new PerformanceCounter("BaseDist");
        mPC4 = new PerformanceCounter("Analyse");
        mPC5 = new PerformanceCounter("Nearest");
        mPC2 = new PerformanceCounter("GeneData");
        mPC6 = new PerformanceCounter("CSV");

        clearState();
    }

    private void clearState()
    {
        mSampleId = "";
        mNextClusterId = 0;
        mAllVariants = Lists.newArrayList();
        mClusters = Lists.newArrayList();
    }

    public void loadFromEnrichedSVs(final String sampleId, final List<EnrichedStructuralVariant> variants)
    {
        if (variants.isEmpty())
            return;

        clearState();

        mSampleId = sampleId;

        for (final EnrichedStructuralVariant enrichedSV : variants)
        {
            mAllVariants.add(SvClusterData.from(enrichedSV));
        }
    }

    public void loadFromDatabase(final String sampleId, final List<SvClusterData> variants)
    {
        if (variants.isEmpty())
            return;

        clearState();

        mSampleId = sampleId;
        mAllVariants = Lists.newArrayList(variants);

        LOGGER.debug("loaded {} SVs", mAllVariants.size());
    }

    public void analyse()
    {
        if(mAllVariants.isEmpty())
            return;

        mPerfCounter.start();

        mPC1.start();

        annotateAndFilterVariants();

        mPC1.stop();

        mPC3.start();

        LOGGER.debug("sample({}) clustering {} variants", mSampleId, mAllVariants.size());

        clusterByBaseDistance();

        mPC3.stop();
        mPC4.start();

        for(SvCluster cluster : mClusters)
        {
            mAnalyser.setClusterStats(cluster);

            cluster.setUniqueBreakends();

            mAnalyser.findLinkedPairs(mSampleId, cluster);
            mAnalyser.findSvChains(mSampleId, cluster);
            mAnalyser.resolveTransitiveSVs(mSampleId, cluster);

            // mClusteringMethods.findFootprints(mSampleId, cluster);
        }

        mPC4.stop();
        mPC5.start();

        // mClusteringMethods.setClosestSVData(mAllVariants, mClusters);
        // mClusteringMethods.setClosestLinkedSVData(mAllVariants);

        mClusteringMethods.setChromosomalArmStats(mAllVariants);

        mPC5.stop();

        mPC2.start();

        if(mGeneAnnotator.hasData()) {
            for (SvClusterData var : mAllVariants) {

                mGeneAnnotator.addGeneData(mSampleId, var);
            }

            mGeneAnnotator.reportGeneMatchData(mSampleId);
        }

        mPC2.stop();

        mPC6.start();

        if(mConfig.getOutputCsvPath() != "")
            writeClusterDataOutput();

        mPC6.stop();
        mPerfCounter.stop();
    }

    private void annotateAndFilterVariants()
    {
        int currentIndex = 0;

        while(currentIndex < mAllVariants.size()) {

            SvClusterData var = mAllVariants.get(currentIndex);

//            // for now exclude Inserts
//            if (var.type() == StructuralVariantType.INS) {
//                mAllVariants.remove(currentIndex);
//                continue;
//            }

            if(mExternalAnnotator.hasData())
            {
                mExternalAnnotator.setSVData(mSampleId, var);
            }
            else {

                mSvPONAnnotator.setPonOccurenceCount(var);
                var.setFragileSites(mFragileSiteAnnotator.isFragileSite(var, true), mFragileSiteAnnotator.isFragileSite(var, false));
                var.setLineElements(mLineElementAnnotator.isLineElement(var, true), mLineElementAnnotator.isLineElement(var, false));
            }

            // exclude PON
            if (var.getPonCount() >= 2) {
                LOGGER.info("filtering sv({}) with PonCount({})", var.id(), var.getPonCount());
                mAllVariants.remove(currentIndex);
                continue;
            }

            String startArm = mClusteringUtils.getChromosomalArm(var.chromosome(true), var.position(true));
            String endArm = mClusteringUtils.getChromosomalArm(var.chromosome(false), var.position(false));
            var.setChromosomalArms(startArm, endArm);

            ++currentIndex;
        }
    }

    public void clusterByBaseDistance()
    {
        List<SvClusterData> unassignedVariants = Lists.newArrayList(mAllVariants);

        // assign each variant once to a cluster using proximity as a test
        int currentIndex = 0;

        while(currentIndex < unassignedVariants.size()) {
            SvClusterData currentVar = unassignedVariants.get(currentIndex);

            // make a new cluster
            SvCluster newCluster = new SvCluster(getNextClusterId(), mClusteringUtils);

            // first remove the current SV from consideration
            newCluster.addVariant(currentVar);
            unassignedVariants.remove(currentIndex); // index will remain the same and so point to the next item

            // and then search for all other linked ones
            mClusteringMethods.findLinkedSVsByDistance(newCluster, unassignedVariants);

            mClusters.add(newCluster);
        }
    }

    private int getNextClusterId() { return mNextClusterId++; }

    private void writeClusterDataOutput()
    {
        try {

            BufferedWriter writer = null;

            if(mConfig.getUseCombinedOutputFile() && mFileWriter != null)
            {
                // check if can continue appending to an existing file
                writer = mFileWriter;
            }
            else
            {
                String outputFileName = mConfig.getOutputCsvPath();

                if(!outputFileName.endsWith("/"))
                    outputFileName += "/";

                if(mConfig.getUseCombinedOutputFile())
                    outputFileName += "CLUSTER.csv";
                else
                    outputFileName += mSampleId + ".csv";

                Path outputFile = Paths.get(outputFileName);

                boolean fileExists = Files.exists(outputFile);

                writer = Files.newBufferedWriter(outputFile, fileExists ? StandardOpenOption.APPEND : StandardOpenOption.CREATE_NEW);
                mFileWriter = writer;

                if(!fileExists) {
                    // SV info
                    writer.write("SampleId,ClusterId,ClusterCount,Id,Type,Ploidy,PONCount,PONRegionCount,");
                    writer.write("ChrStart,PosStart,OrientStart,ArmStart,AdjAFStart,AdjCNStart,AdjCNChgStart,");
                    writer.write("ChrEnd,PosEnd,OrientEnd,ArmEnd,AdjAFEnd,AdjCNEnd,AdjCNChgEnd,");
                    writer.write("Homology,InsertSeq,MantaPrecise,SomaticScore,");
                    writer.write("FSStart,FSEnd,LEStart,LEEnd,DupBEStart,DupBEEnd,");
                    writer.write("ArmCountStart,ArmExpStart,ArmCountEnd,ArmExpEnd,");

                    // cluster-level info
                    writer.write("ClusterDesc,Consistency,ArmCount,DupBECount,DupBESiteCount,");

                    // linked pair info
                    writer.write("LnkSvStart,LnkTypeStart,LnkLenStart,LnkSvEnd,LnkTypeEnd,LnkLenEnd,");

                    // chain info
                    writer.write("ChainId,ChainCount,ChainTICount,ChainDBCount,ChainIndex,");

                    // transitive info
                    writer.write("TransType,TransLen,TransSvLinks");

                    writer.newLine();
                }
            }

            for(final SvCluster cluster : mClusters)
            {
                int duplicateBECount = cluster.getDuplicateBECount();
                int duplicateBESiteCount = cluster.getDuplicateBESiteCount();

                for (final SvClusterData var : cluster.getSVs()) {
                    writer.write(
                            String.format("%s,%d,%d,%s,%s,%.2f,%d,%d,",
                                    mSampleId, cluster.getId(), cluster.getCount(), var.id(), var.type(),
                                    var.getSvData().ploidy(), var.getPonCount(), var.getPonRegionCount()));

                    writer.write(
                            String.format("%s,%d,%d,%s,%.2f,%.2f,%.2f,%s,%d,%d,%s,%.2f,%.2f,%.2f,",
                                    var.chromosome(true), var.position(true), var.orientation(true), var.getStartArm(),
                                    var.getSvData().adjustedStartAF(), var.getSvData().adjustedStartCopyNumber(), var.getSvData().adjustedStartCopyNumberChange(),
                                    var.chromosome(false), var.position(false), var.orientation(false), var.getEndArm(),
                                    var.getSvData().adjustedEndAF(), var.getSvData().adjustedEndCopyNumber(), var.getSvData().adjustedEndCopyNumberChange()));

                    writer.write(
                            String.format("%s,%s,%s,%d,",
                                    var.getSvData().insertSequence().isEmpty() && var.type() != StructuralVariantType.INS ? var.getSvData().homology() : "",
                                    var.type() == StructuralVariantType.INS ? var.getSvData().insertSequence() : "",
                                    var.getSvData().mantaPrecise(), var.getSvData().somaticScore()));

                    writer.write(
                            String.format("%s,%s,%s,%s,%s,%s,%s,",
                                    // var.getNearestSVLength(), var.getNearestSVLinkType(), var.getNearestTILength(), var.getNearestDBLength(),
                                    var.isStartFragileSite(), var.isEndFragileSite(),
                                    var.isStartLineElement(), var.isEndLineElement(),
                                    var.isDupBEStart(), var.isDupBEEnd(),
                                    mClusteringMethods.getChrArmData(var)));

                    writer.write(
                            String.format("%s,%d,%d,%d,%d,",
                                    cluster.getDesc(), cluster.getConsistencyCount(), cluster.getChromosomalArmCount(),
                                    duplicateBECount, duplicateBESiteCount));

                    // linked pair info
                    SvLinkedPair startLP = cluster.findLinkedPair(var, true);
                    if(startLP != null)
                        writer.write(String.format("%s,%s,%d,",
                                startLP.first().equals(var) ? startLP.second().id() : startLP.first().id(),
                                startLP.linkType(), startLP.length()));
                    else
                        writer.write("0,,-1,");

                    SvLinkedPair endLP = cluster.findLinkedPair(var, false);
                    if(endLP != null)
                        writer.write(String.format("%s,%s,%d,",
                                endLP.first().equals(var) ? endLP.second().id() : endLP.first().id(),
                                endLP.linkType(), endLP.length()));
                    else
                        writer.write("0,,-1,");

                    // chain info
                    SvChain chain = cluster.findChain(var);

                    if(chain != null)
                        writer.write(String.format("%d,%d,%d,%d,%d,",
                                chain.getId(), chain.getLinkCount(), chain.getTICount(), chain.getDBCount(), chain.getSvIndex(var)));
                    else
                        writer.write("0,0,0,0,0,");

                    writer.write(String.format("%s,%d,%s", var.getTransType(), var.getTransLength(), var.getTransSvLinks()));

                    writer.newLine();
                }
            }

        }
        catch (final IOException e) {
            LOGGER.error("error writing to outputFile");
        }
    }

    public void close()
    {
        if(mFileWriter == null)
            return;

        try
        {
            mFileWriter.close();
        }
        catch (final IOException e) {
        }

        // log perf stats
        mPerfCounter.logStats();
        mPC1.logStats();
        mPC3.logStats();
        mPC4.logStats();
        mPC5.logStats();
        mPC2.logStats();
        mPC6.logStats();
    }

    public List<SvCluster> getClusters() { return mClusters; }

    private final SvClusterData getSvData(final EnrichedStructuralVariant variant)
    {
        for(final SvClusterData svData : mAllVariants)
        {
            if(svData.id() == variant.id())
                return svData;
        }

        return null;
    }
}
