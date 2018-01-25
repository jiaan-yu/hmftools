package com.hartwig.hmftools.svannotation.annotations;

import com.hartwig.hmftools.common.variant.structural.StructuralVariant;

import org.jetbrains.annotations.NotNull;

public class Transcript {

    private final GeneAnnotation parent;
    private final String transcriptId;
    private final int exonUpstream;
    private final int exonUpstreamPhase;
    private final int exonDownstream;
    private final int exonDownstreamPhase;
    private final int exonMax;
    private final boolean canonical;

    public Transcript(final GeneAnnotation parent, @NotNull final String transcriptId, final int exonUpstream,
            final int exonUpstreamPhase, final int exonDownstream, final int exonDownstreamPhase, final int exonMax,
            final boolean canonical) {
        this.parent = parent;
        this.transcriptId = transcriptId;
        this.exonUpstream = exonUpstream;
        this.exonUpstreamPhase = exonUpstreamPhase;
        this.exonDownstream = exonDownstream;
        this.exonDownstreamPhase = exonDownstreamPhase;
        this.exonMax = exonMax;
        this.canonical = canonical;
    }

    @NotNull
    public String transcriptId() {
        return transcriptId;
    }

    public boolean isExonic() {
        return exonUpstream > 0 && exonUpstream == exonDownstream;
    }

    public boolean isPromoter() {
        return exonUpstream == 0 && exonDownstream == 1;
    }

    public boolean isIntronic() {
        return exonUpstream > 0 && (exonDownstream - exonUpstream) == 1;
    }

    public boolean isCanonical() {
        return canonical;
    }

    public GeneAnnotation parent() {
        return parent;
    }

    public String geneName() {
        return parent.geneName();
    }

    public StructuralVariant variant() {
        return parent.variant();
    }

    public int exonUpstream() {
        return exonUpstream;
    }

    public int exonUpstreamPhase() {
        return exonUpstreamPhase;
    }

    public int exonDownstream() {
        return exonDownstream;
    }

    public int exonDownstreamPhase() {
        return exonDownstreamPhase;
    }

    public int exonMax() {
        return exonMax;
    }
}
