package com.hartwig.hmftools.bamrecovery;

import org.immutables.value.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Value.Immutable
@Value.Style(allParameters = true,
             passAnnotations = { NotNull.class, Nullable.class })
abstract class Archive {

    abstract ArchiveHeader header();

    abstract byte[] payload();

    boolean isTruncated() {
        return header().isTruncated();
    }

    @Override
    public String toString() {
        return header().toString();
    }
}
