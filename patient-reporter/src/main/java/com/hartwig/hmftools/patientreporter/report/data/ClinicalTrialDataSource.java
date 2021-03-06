package com.hartwig.hmftools.patientreporter.report.data;

import static net.sf.dynamicreports.report.builder.DynamicReports.field;

import java.util.List;
import java.util.stream.Collectors;

import com.hartwig.hmftools.common.actionability.ClinicalTrial;

import org.apache.logging.log4j.util.Strings;
import org.jetbrains.annotations.NotNull;

import net.sf.dynamicreports.report.base.expression.AbstractSimpleExpression;
import net.sf.dynamicreports.report.builder.FieldBuilder;
import net.sf.dynamicreports.report.datasource.DRDataSource;
import net.sf.dynamicreports.report.definition.ReportParameters;
import net.sf.jasperreports.engine.JRDataSource;

public class ClinicalTrialDataSource {

    public static final FieldBuilder<?> EVENT_FIELD = field("event", String.class);
    public static final FieldBuilder<?> SCOPE_FIELD = field("scope", String.class);
    public static final FieldBuilder<?> TRIAL_FIELD = field("acronym", String.class);
    public static final FieldBuilder<?> SOURCE_FIELD = field("source", String.class);
    public static final FieldBuilder<?> CCMO_FIELD = field("ccmo", String.class);
    private static final FieldBuilder<?> REFERENCE_FIELD = field("reference", String.class);

    private ClinicalTrialDataSource() {
    }

    @NotNull
    public static FieldBuilder<?>[] clinicalTrialFields() {
        return new FieldBuilder<?>[] { EVENT_FIELD, SCOPE_FIELD, TRIAL_FIELD, SOURCE_FIELD, CCMO_FIELD, REFERENCE_FIELD };
    }

    @NotNull
    public static JRDataSource fromClinicalTrials(@NotNull List<ClinicalTrial> trials) {
        final DRDataSource evidenceItemDataSource = new DRDataSource(EVENT_FIELD.getName(),
                SCOPE_FIELD.getName(),
                TRIAL_FIELD.getName(),
                SOURCE_FIELD.getName(),
                CCMO_FIELD.getName(),
                REFERENCE_FIELD.getName());

        for (ClinicalTrial trial : sort(trials)) {
            assert trial.source().isTrialSource();

            evidenceItemDataSource.add(trial.event(),
                    trial.scope().readableString(),
                    trial.acronym(),
                    trial.source().sourceName(),
                    CCMOId(trial.reference()),
                    trial.reference());
        }
        return evidenceItemDataSource;
    }

    @NotNull
    private static String CCMOId(@NotNull String reference) {
        // Expected format "EXT1 (CCMO)"
        String referenceWithoutParenthesis = reference.replace(")", "");
        String[] splitExtAndCCMO = referenceWithoutParenthesis.split("\\(");
        return splitExtAndCCMO[1];
    }

    @NotNull
    private static List<ClinicalTrial> sort(@NotNull List<ClinicalTrial> trials) {
        return trials.stream().sorted((item1, item2) -> {
            if (item1.event().equals(item2.event())) {
                return item1.acronym().compareTo(item2.acronym());
            } else {
                return item1.event().compareTo(item2.event());
            }
        }).collect(Collectors.toList());
    }

    @NotNull
    public static AbstractSimpleExpression<String> sourceHyperlink() {
        return new AbstractSimpleExpression<String>() {
            @Override
            public String evaluate(@NotNull final ReportParameters data) {
                String source = data.getValue(SOURCE_FIELD.getName()).toString();
                String reference = data.getValue(REFERENCE_FIELD.getName()).toString();
                String ext = EXTId(reference);
                switch (source.toLowerCase()) {
                    case "iclusion":
                        return "https://iclusion.org/hmf/" + ext;
                    default:
                        return Strings.EMPTY;
                }
            }
        };
    }

    @NotNull
    private static String EXTId(@NotNull String reference) {
        // Expected format "EXT1 (CCMO)"
        String[] splitExtAndCCMO = reference.split("\\(");
        String ext = splitExtAndCCMO[0];
        return ext.substring(3).trim();
    }
}
