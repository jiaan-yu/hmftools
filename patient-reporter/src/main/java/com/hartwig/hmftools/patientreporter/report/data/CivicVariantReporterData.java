package com.hartwig.hmftools.patientreporter.report.data;

import static java.util.Comparator.comparing;

import static net.sf.dynamicreports.report.builder.DynamicReports.field;

import java.util.List;
import java.util.stream.Collectors;

import com.hartwig.hmftools.apiclients.civic.data.CivicVariant;
import com.hartwig.hmftools.apiclients.civic.data.CivicVariantType;

import org.apache.logging.log4j.util.Strings;
import org.immutables.value.Value;
import org.jetbrains.annotations.NotNull;

import net.sf.dynamicreports.report.builder.FieldBuilder;

@Value.Immutable
@Value.Style(allParameters = true)
public abstract class CivicVariantReporterData {
    public static final FieldBuilder<?> VARIANT = field("variant", String.class);

    public abstract String getVariant();

    public abstract List<EvidenceItemReporterData> getEvidenceItems();

    public static CivicVariantReporterData of(@NotNull final CivicVariant civicVariant) {
        final String variant = civicVariant.name() + " " + civicVariant.coordinates() + " (" + Strings.join(
                civicVariant.variantTypes().stream().map(CivicVariantType::name).collect(Collectors.toList()), ',') + ")";
        final List<EvidenceItemReporterData> evidenceItems =
                civicVariant.evidenceItemsWithDrugs().stream().map(EvidenceItemReporterData::of).collect(Collectors.toList());
        evidenceItems.sort(comparing(EvidenceItemReporterData::getTumorType).thenComparing(EvidenceItemReporterData::getLevel)
                .thenComparing(EvidenceItemReporterData::getDirection));
        return ImmutableCivicVariantReporterData.of(variant, evidenceItems);
    }
}