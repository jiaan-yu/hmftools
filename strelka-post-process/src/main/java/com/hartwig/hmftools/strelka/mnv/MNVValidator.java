package com.hartwig.hmftools.strelka.mnv;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import htsjdk.variant.variantcontext.VariantContext;

public interface MNVValidator {
    @NotNull
    List<VariantContext> mergeVariants(@NotNull final PotentialMNVRegion potentialMnvRegion, @NotNull final MNVMerger merger);
}
