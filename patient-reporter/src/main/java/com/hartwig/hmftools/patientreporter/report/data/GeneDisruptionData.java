package com.hartwig.hmftools.patientreporter.report.data;

import static com.hartwig.hmftools.patientreporter.util.PatientReportFormat.alleleFrequency;
import static com.hartwig.hmftools.patientreporter.util.PatientReportFormat.exonDescription;
import static com.hartwig.hmftools.patientreporter.util.PatientReportFormat.positionString;

import com.hartwig.hmftools.patientreporter.util.PatientReportFormat;
import com.hartwig.hmftools.svannotation.annotations.GeneAnnotation;
import com.hartwig.hmftools.svannotation.annotations.GeneDisruption;
import com.hartwig.hmftools.svannotation.annotations.Transcript;

import org.immutables.value.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Value.Immutable
@Value.Style(allParameters = true,
             passAnnotations = { NotNull.class, Nullable.class })
public abstract class GeneDisruptionData {

    public abstract String geneName();

    public abstract String location();

    public abstract String geneContext();

    public abstract String transcript();

    public abstract String partner();

    public abstract String type();

    public abstract String orientation();

    public abstract String vaf();

    @NotNull
    public static GeneDisruptionData from(@NotNull final GeneDisruption disruption) {
        final Transcript transcript = disruption.linkedAnnotation();
        final GeneAnnotation g = transcript.getGeneAnnotation();
        final int variantOrientation = g.isStart() ? g.getVariant().startOrientation() : g.getVariant().endOrientation();

        return ImmutableGeneDisruptionData.builder()
                .geneName(disruption.linkedAnnotation().getGeneName())
                .location(positionString(g))
                .geneContext(exonDescription(transcript, true)) // TODO: upstream ?
                .transcript(transcript.getTranscriptId())
                .partner(positionString(g.getVariant(), !g.isStart()))
                .type(g.getVariant().type().toString())
                .orientation(variantOrientation > 0 ? "5'" : "3'")
                .vaf(PatientReportFormat.formatNullablePercent(alleleFrequency(g)))
                .build();
    }
}