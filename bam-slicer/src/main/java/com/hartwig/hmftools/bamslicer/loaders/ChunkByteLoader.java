package com.hartwig.hmftools.bamslicer.loaders;

import com.google.common.util.concurrent.ListenableFuture;

import org.jetbrains.annotations.NotNull;

public interface ChunkByteLoader {

    //MIVO: get bytes between start and end (inclusive)

    @NotNull
    ListenableFuture<byte[]> getBytes(final long start, final long end);

    void close();

    String getSource();
}
