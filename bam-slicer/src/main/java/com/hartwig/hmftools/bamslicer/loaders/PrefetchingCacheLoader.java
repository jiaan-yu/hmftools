package com.hartwig.hmftools.bamslicer.loaders;

import com.google.common.cache.CacheLoader;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class PrefetchingCacheLoader extends CacheLoader<Pair<Long, Long>, ListenableFuture<byte[]>> {
    private static final Logger LOGGER = LogManager.getLogger(PrefetchingCacheLoader.class);

    @NotNull
    private final ChunkByteLoader loader;

    PrefetchingCacheLoader(@NotNull final ChunkByteLoader loader) {
        this.loader = loader;
    }

    @Override
    @NotNull
    public ListenableFuture<byte[]> load(@NotNull final Pair<Long, Long> range) {
        LOGGER.info("Cache-loading bytes at: {} - {}", range.getLeft(), range.getRight());
        return loader.getBytes(range.getLeft(), range.getRight());
    }

    @Override
    @NotNull
    public ListenableFuture<ListenableFuture<byte[]>> reload(@NotNull final Pair<Long, Long> range,
            @NotNull final ListenableFuture<byte[]> oldBytes) {
        LOGGER.info("Cache re-loading bytes at: {} - {}", range.getLeft(), range.getRight());
        return Futures.immediateFuture(oldBytes);
    }
}
