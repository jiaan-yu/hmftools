package com.hartwig.hmftools.bamrecovery;

import org.immutables.value.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Value.Immutable
@Value.Style(allParameters = true,
             passAnnotations = { NotNull.class, Nullable.class })
abstract class Archive {

    abstract long startOffset();

    abstract long endOffset();

    abstract int size();

    long actualSize() {
        return endOffset() - startOffset();
    }

    @Override
    public String toString() {
        return "" + startOffset() + "-" + endOffset() + "(" + size() + ")";
    }
}
