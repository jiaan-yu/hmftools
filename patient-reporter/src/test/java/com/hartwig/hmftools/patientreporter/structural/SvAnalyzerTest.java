package com.hartwig.hmftools.patientreporter.structural;

import static com.hartwig.hmftools.patientreporter.PatientReporterTestFactory.createTestCopyNumberBuilder;
import static com.hartwig.hmftools.patientreporter.PatientReporterTestUtil.testActionabilityAnalyzer;
import static com.hartwig.hmftools.patientreporter.PatientReporterTestUtil.testDrupActionabilityModel;
import static com.hartwig.hmftools.patientreporter.structural.SvAnalysisDatamodelTestFactory.createTestDisruptionBuilder;
import static com.hartwig.hmftools.patientreporter.structural.SvAnalysisDatamodelTestFactory.createTestFusionBuilder;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.List;

import com.google.common.collect.Lists;
import com.hartwig.hmftools.common.purple.gene.GeneCopyNumber;
import com.hartwig.hmftools.patientreporter.genepanel.GeneModel;
import com.hartwig.hmftools.patientreporter.genepanel.GeneModelFactory;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

public class SvAnalyzerTest {

    private static final String DISRUPTED_GENE = "TP53";

    @Test
    public void canAnalyzeFusionsDisruptions() throws IOException {
        SvAnalyzer testAnalyzer = buildTestAnalyzer();

        GeneModel geneModel = GeneModelFactory.create(testDrupActionabilityModel());
        List<GeneCopyNumber> geneCopyNumbers =
                Lists.newArrayList(createTestCopyNumberBuilder().minCopyNumber(2D).maxCopyNumber(2D).gene(DISRUPTED_GENE).build());
        SvAnalysis analysis = testAnalyzer.run(geneModel, geneCopyNumbers, testActionabilityAnalyzer(), null);

        assertEquals(1, analysis.reportableFusions().size());
        assertEquals(1, analysis.reportableDisruptions().size());
        assertEquals(0, analysis.evidenceItems().size());
    }

    @NotNull
    private static SvAnalyzer buildTestAnalyzer() {
        List<Fusion> testFusions = Lists.newArrayList(createTestFusionBuilder().geneUp("X").geneDown("Y").reportable(true).build(),
                createTestFusionBuilder().geneUp("A").geneDown("B").reportable(false).build());

        List<Disruption> testDisruptions =
                Lists.newArrayList(createTestDisruptionBuilder().gene(DISRUPTED_GENE).isDisruptive(true).canonical(true).build(),
                        createTestDisruptionBuilder().gene(DISRUPTED_GENE).isDisruptive(false).canonical(true).build(),
                        createTestDisruptionBuilder().gene(DISRUPTED_GENE).isDisruptive(true).canonical(false).build());

        return new SvAnalyzer(testFusions, testDisruptions);
    }
}
