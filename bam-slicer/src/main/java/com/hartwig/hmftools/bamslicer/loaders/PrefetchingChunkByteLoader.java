package com.hartwig.hmftools.bamslicer.loaders;

import java.util.concurrent.ConcurrentSkipListMap;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.Weigher;
import com.google.common.util.concurrent.ListenableFuture;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class PrefetchingChunkByteLoader implements ChunkByteLoader {
    private static final Logger LOGGER = LogManager.getLogger(PrefetchingChunkByteLoader.class);
    @NotNull
    private final LoadingCache<Pair<Long, Long>, ListenableFuture<byte[]>> cache;
    @NotNull
    private final ConcurrentSkipListMap<Long, Pair<Long, Long>> chunksLookahead;
    @NotNull
    private final ChunkByteLoader loader;

    private final double fillSize;

    public PrefetchingChunkByteLoader(@NotNull final ChunkByteLoader loader, final long maxCacheSize,
            @NotNull final ConcurrentSkipListMap<Long, Pair<Long, Long>> chunksLookahead) {
        this.fillSize = maxCacheSize * .5;
        this.chunksLookahead = chunksLookahead;
        this.loader = loader;
        this.cache = CacheBuilder.newBuilder()
                .concurrencyLevel(1)
                .maximumWeight(maxCacheSize)
                .weigher(cachedChunkWeigher())
                .removalListener(removalNotification -> {
                    LOGGER.info("Evicting {}", removalNotification.getKey().getLeft());
                    removalNotification.getValue().cancel(true);
                })
                .build(new PrefetchingCacheLoader(loader));
    }

    private void prefetch(final long position) {
        final long chunkOffset = chunksLookahead.floorEntry(position).getKey();
        LOGGER.info("refilling cache starting at offset: {}", chunkOffset);
        double runningSize = 0;
        for (final Pair<Long, Long> range : chunksLookahead.tailMap(chunkOffset, false).values()) {
            runningSize += rangeSize(range);
            if (runningSize >= fillSize) {
                break;
            }
            if (cache.getIfPresent(range) == null) {
                cache.refresh(range);
            }
        }
        LOGGER.info("done refilling cache starting at offset: {}", chunkOffset);
    }

    @NotNull
    private static Weigher<Pair<Long, Long>, ListenableFuture<byte[]>> cachedChunkWeigher() {
        return (range, bytesFuture) -> (int) rangeSize(range);
    }

    private static long rangeSize(@NotNull final Pair<Long, Long> range) {
        return range.getRight() - range.getLeft() + 1;
    }

    @NotNull
    @Override
    public ListenableFuture<byte[]> getBytes(final long start, final long end) {
        final Pair<Long, Long> range = Pair.of(start, end);
        if (cache.getIfPresent(range) == null) {
            LOGGER.info("Cache miss at range: {} - {}", start, end);
            cache.refresh(range);
        }
        prefetch(start);
        return cache.getUnchecked(range);
    }

    @Override
    public void close() {
        cache.invalidateAll();
        loader.close();
    }

    @Override
    public String getSource() {
        return loader.getSource();
    }
}
