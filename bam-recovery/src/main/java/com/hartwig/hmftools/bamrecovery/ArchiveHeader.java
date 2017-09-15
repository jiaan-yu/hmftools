package com.hartwig.hmftools.bamrecovery;

import static com.hartwig.hmftools.bamrecovery.BamFile.BLOCK_FOOTER_LENGTH;
import static com.hartwig.hmftools.bamrecovery.BamFile.BLOCK_SIZE_LENGTH;
import static com.hartwig.hmftools.bamrecovery.BamFile.HEADER;

import org.immutables.value.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Value.Immutable
@Value.Style(allParameters = true,
             passAnnotations = { NotNull.class, Nullable.class })
abstract class ArchiveHeader {

    abstract long startOffset();

    abstract long endOffset();

    abstract int size();

    abstract int crc();

    abstract int uncompressedSize();

    private long actualSize() {
        return endOffset() - startOffset();
    }

    int payloadSize() {
        return size() - HEADER.length - BLOCK_SIZE_LENGTH - BLOCK_FOOTER_LENGTH;
    }

    long payloadPosition() {
        return startOffset() + HEADER.length + BLOCK_SIZE_LENGTH;
    }

    boolean isTruncated() {
        return size() != actualSize();
    }

    @Override
    public String toString() {
        return "" + startOffset() + "-" + endOffset() + "(" + size() + ")";
    }

    private int payloadSize(final int size) {
        return size - HEADER.length - BLOCK_SIZE_LENGTH - BLOCK_FOOTER_LENGTH;
    }
}
