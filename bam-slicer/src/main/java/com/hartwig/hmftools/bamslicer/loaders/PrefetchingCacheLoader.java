package com.hartwig.hmftools.bamslicer.loaders;

import com.google.common.cache.CacheLoader;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

public class PrefetchingCacheLoader extends CacheLoader<Pair<Long, Long>, ListenableFuture<byte[]>> {
    @NotNull
    private final ChunkByteLoader loader;

    PrefetchingCacheLoader(@NotNull final ChunkByteLoader loader) {
        this.loader = loader;
    }

    @Override
    @NotNull
    public ListenableFuture<byte[]> load(@NotNull final Pair<Long, Long> range) {
        return loader.getBytes(range.getLeft(), range.getRight());
    }

    @Override
    @NotNull
    public ListenableFuture<ListenableFuture<byte[]>> reload(@NotNull final Pair<Long, Long> range,
            @NotNull final ListenableFuture<byte[]> oldBytes) {
        return Futures.immediateFuture(oldBytes);
    }
}
