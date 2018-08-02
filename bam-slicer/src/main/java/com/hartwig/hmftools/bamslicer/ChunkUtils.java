package com.hartwig.hmftools.bamslicer;

import static htsjdk.samtools.util.BlockCompressedFilePointerUtil.MAX_BLOCK_ADDRESS;

import java.util.List;
import java.util.Optional;

import com.google.common.collect.Lists;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import htsjdk.samtools.BAMFileSpan;
import htsjdk.samtools.Chunk;
import htsjdk.samtools.QueryInterval;
import htsjdk.samtools.util.BlockCompressedFilePointerUtil;
import htsjdk.samtools.util.BlockCompressedStreamConstants;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
final class ChunkUtils {
    private static final Logger LOGGER = LogManager.getLogger(ChunkUtils.class);

    private static final Chunk HEADER_CHUNK = new Chunk(0, (long) (BlockCompressedStreamConstants.MAX_COMPRESSED_BLOCK_SIZE - 1) << 16);

    @NotNull
    static List<Chunk> sliceChunks(@NotNull final Optional<Pair<QueryInterval[], BAMFileSpan>> queryIntervalsAndSpan,
            @NotNull final Optional<Chunk> unmappedChunk) {
        final List<Chunk> chunks = Lists.newArrayList();
        chunks.add(HEADER_CHUNK);
        queryIntervalsAndSpan.ifPresent(pair -> {
            final List<Chunk> splitChunks = expandChunks(pair.getValue().getChunks());
            chunks.addAll(splitChunks);
            LOGGER.info("Generated {} query intervals which map to {} bam chunks", pair.getKey().length, chunks.size());
        });
        unmappedChunk.ifPresent(chunks::add);
        return Chunk.optimizeChunkList(chunks, 0);
    }

    @NotNull
    private static List<Chunk> expandChunks(@NotNull final List<Chunk> chunks) {
        final List<Chunk> result = Lists.newArrayList();
        for (final Chunk chunk : chunks) {
            final long chunkEndBlockAddress = BlockCompressedFilePointerUtil.getBlockAddress(chunk.getChunkEnd());
            final long extendedEndBlockAddress = chunkEndBlockAddress + BlockCompressedStreamConstants.MAX_COMPRESSED_BLOCK_SIZE - 1;
            final long newChunkEnd = extendedEndBlockAddress > MAX_BLOCK_ADDRESS ? MAX_BLOCK_ADDRESS : extendedEndBlockAddress;
            final long chunkEndVirtualPointer = newChunkEnd << 16;
            result.add(new Chunk(chunk.getChunkStart(), chunkEndVirtualPointer));
        }
        return result;
    }

    private ChunkUtils() {
    }

}
