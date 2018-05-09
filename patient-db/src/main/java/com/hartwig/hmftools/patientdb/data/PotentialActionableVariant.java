package com.hartwig.hmftools.patientdb.data;

import static com.hartwig.hmftools.patientdb.database.hmfpatients.Tables.BASELINE;
import static com.hartwig.hmftools.patientdb.database.hmfpatients.Tables.SOMATICVARIANT;

import org.immutables.value.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jooq.Record;

@Value.Immutable
@Value.Style(allParameters = true,
             passAnnotations = { NotNull.class, Nullable.class })
public abstract class PotentialActionableVariant {
    @NotNull
    public abstract String sampleId();

    @Nullable
    public abstract String primaryTumorLocation();

    @NotNull
    public abstract String gene();

    @NotNull
    public abstract String chromosome();

    @NotNull
    public abstract Integer position();

    @NotNull
    public abstract String ref();

    @NotNull
    public abstract String alt();

    @NotNull
    public static PotentialActionableVariant of(@NotNull final Record mysqlRecord) {
        return ImmutablePotentialActionableVariant.of(mysqlRecord.get(SOMATICVARIANT.SAMPLEID),
                mysqlRecord.get(BASELINE.PRIMARYTUMORLOCATION),
                mysqlRecord.get(SOMATICVARIANT.GENE),
                mysqlRecord.get(SOMATICVARIANT.CHROMOSOME),
                mysqlRecord.get(SOMATICVARIANT.POSITION),
                mysqlRecord.get(SOMATICVARIANT.REF),
                mysqlRecord.get(SOMATICVARIANT.ALT));
    }
}