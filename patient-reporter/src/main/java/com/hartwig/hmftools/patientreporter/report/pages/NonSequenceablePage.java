package com.hartwig.hmftools.patientreporter.report.pages;

import static com.hartwig.hmftools.patientreporter.report.Commons.SECTION_VERTICAL_GAP;
import static com.hartwig.hmftools.patientreporter.report.Commons.dataTableStyle;
import static com.hartwig.hmftools.patientreporter.report.Commons.fontStyle;
import static com.hartwig.hmftools.patientreporter.report.Commons.formattedDate;
import static com.hartwig.hmftools.patientreporter.report.Commons.tableHeaderStyle;

import static net.sf.dynamicreports.report.builder.DynamicReports.cmp;

import com.hartwig.hmftools.patientreporter.NotSequencedPatientReport;
import com.hartwig.hmftools.patientreporter.SampleReport;
import com.hartwig.hmftools.patientreporter.algo.NotSequenceableReason;
import com.hartwig.hmftools.patientreporter.algo.NotSequenceableStudy;
import com.hartwig.hmftools.patientreporter.report.components.MainPageTopSection;

import org.immutables.value.Value;
import org.jetbrains.annotations.NotNull;

import net.sf.dynamicreports.report.builder.component.ComponentBuilder;

@Value.Immutable
@Value.Style(passAnnotations = NotNull.class,
             allParameters = true)
public abstract class NonSequenceablePage {

    @NotNull
    abstract SampleReport sampleReport();

    @NotNull
    abstract String user();

    @NotNull
    abstract NotSequenceableReason reason();

    @NotNull
    abstract NotSequenceableStudy study();

    @NotNull
    public static NonSequenceablePage of(@NotNull final NotSequencedPatientReport report) {
        return ImmutableNonSequenceablePage.of(report.sampleReport(), report.user(), report.reason(), report.study());
    }

    @NotNull
    public ComponentBuilder<?, ?> reportComponent() {
        return cmp.verticalList(MainPageTopSection.build("HMF Sequencing Report", sampleReport()), cmp.verticalGap(SECTION_VERTICAL_GAP),
                mainPageNotSequenceableSection());
    }

    @NotNull
    private ComponentBuilder<?, ?> mainPageNotSequenceableSection() {
        if (sampleReport().recipient() == null) {
            throw new IllegalStateException("No recipient address present for sample " + sampleReport().sampleId());
        }

        final String title;
        final String subTitle;
        final String message;

        switch (reason()) {
            case LOW_DNA_YIELD: {
                title = "Notification tumor sample on hold for sequencing";
                subTitle = "Insufficient amount of DNA";
                message = "The amount of isolated DNA was <300 ng, which is insufficient for sequencing. "
                        + "This sample is on hold for further processing awaiting optimization of protocols.";
                break;
            }
            case LOW_TUMOR_PERCENTAGE: {
                title = "Notification of inadequate tumor sample";
                subTitle = "Insufficient percentage of tumor cells";
                message = "For sequencing we require a minimum of 30% tumor cells.";
                break;
            }
            case POST_ISOLATION_FAIL: {
                title = "Notification of inadequate tumor sample";
                subTitle = "Analysis has failed post DNA isolation";
                message = "This sample could not be processed to completion successfully.";
                break;
            }
            default: {
                title = "TITLE";
                subTitle = "SUB_TITLE";
                message = "MESSAGE";
            }
        }

        return cmp.verticalList(cmp.text(title).setStyle(tableHeaderStyle().setFontSize(12)).setHeight(20),
                cmp.text(subTitle).setStyle(dataTableStyle().setFontSize(12)).setHeight(20), cmp.verticalGap(SECTION_VERTICAL_GAP),
                cmp.text(message).setStyle(fontStyle()), cmp.verticalGap(SECTION_VERTICAL_GAP), cmp.text(
                        "The received biopsies for the tumor sample for this patient were inadequate to obtain a reliable sequencing "
                                + "result. Therefore whole genome sequencing cannot be performed, "
                                + "unless additional fresh tumor material can be provided for a new assessment.").setStyle(fontStyle()),
                cmp.verticalGap(SECTION_VERTICAL_GAP), cmp.text(
                        "When possible, please resubmit using the same " + study().studyName() + "-number. "
                                + "In case additional tumor material cannot be provided, please be notified that the patient will not be "
                                + "evaluable for the " + study().studyCode() + " study.").setStyle(fontStyle()),
                cmp.verticalGap(SECTION_VERTICAL_GAP),
                cmp.text("The biopsies evaluated for this sample have arrived on " + formattedDate(sampleReport().tumorArrivalDate()))
                        .setStyle(fontStyle()), cmp.verticalGap(SECTION_VERTICAL_GAP),
                cmp.text("This report is generated and verified by: " + user() + " and is addressed at " + sampleReport().recipient())
                        .setStyle(fontStyle()), cmp.verticalGap(SECTION_VERTICAL_GAP),
                cmp.text("For questions, please contact us via info@hartwigmedicalfoundation.nl").setStyle(fontStyle()));
    }
}