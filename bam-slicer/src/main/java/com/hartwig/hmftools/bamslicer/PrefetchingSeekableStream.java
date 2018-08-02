package com.hartwig.hmftools.bamslicer;

import java.io.IOException;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutionException;

import com.hartwig.hmftools.bamslicer.loaders.ChunkByteLoader;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import htsjdk.samtools.seekablestream.SeekableStream;

public class PrefetchingSeekableStream extends SeekableStream {
    private static final Logger LOGGER = LogManager.getLogger(PrefetchingSeekableStream.class);
    private byte[] currentBytes = null;
    private long currentBytesOffset = -1;
    private long position = -1;
    private long contentLength;
    @NotNull
    private final ChunkByteLoader loader;
    @NotNull
    private final ConcurrentSkipListMap<Long, Pair<Long, Long>> chunksLookahead;

    PrefetchingSeekableStream(@NotNull final ChunkByteLoader loader, @NotNull ConcurrentSkipListMap<Long, Pair<Long, Long>> chunksLookahead,
            final long contentLength) throws IOException {
        this.contentLength = contentLength;
        this.chunksLookahead = chunksLookahead;
        this.loader = loader;
        seek(0);
    }

    @Override
    public long position() {
        return position;
    }

    @Override
    public long length() {
        return contentLength;
    }

    @Override
    public long skip(long n) throws IOException {
        long bytesToSkip = Math.min(n, contentLength - position);
        seek(position + bytesToSkip);
        return bytesToSkip;
    }

    @Override
    public boolean eof() {
        return contentLength > 0 && position >= contentLength;
    }

    @Override
    public void seek(final long position) throws IOException {
        if (currentBytes == null || position >= currentBytesOffset + currentBytes.length) {
            final Pair<Long, Long> range = nextChunkByteRange(position);
            if (position <= range.getRight()) {
                try {
                    currentBytes = loader.getBytes(range.getLeft(), range.getRight()).get();
                    currentBytesOffset = range.getKey();
                } catch (InterruptedException e) {
                    throw new IOException(
                            "Interrupted while seeking to " + position + ". Byte range: " + range.getLeft() + " - " + range.getRight());
                } catch (ExecutionException e) {
                    throw new IOException("Execution exception raised while seeking to " + position + ". Cause: " + e.getMessage());
                }
            }
        }
        this.position = position;
    }

    @Override
    public int read(byte[] buffer, int offset, int len) throws IOException {
        if (offset < 0 || len < 0 || (offset + len) > buffer.length) {
            LOGGER.error("Attempted to copy {} bytes at offset {} into buffer of size {}", len, offset, buffer.length);
            throw new IndexOutOfBoundsException();
        }
        if (len == 0) {
            return 0;
        }
        if (eof()) {
            return -1;
        }
        final int positionInChunk = (int) (position - currentBytesOffset);
        int readLen = len;
        if (positionInChunk + len > currentBytes.length) {
            readLen = currentBytes.length - positionInChunk;
        }
        System.arraycopy(currentBytes, positionInChunk, buffer, offset, readLen);
        seek(position + readLen);
        return readLen;
    }

    @Override
    public void close() {
        loader.close();
    }

    @Override
    public int read() throws IOException {
        byte[] tmp = new byte[1];
        if (read(tmp, 0, 1) == -1) {
            return -1;
        }
        return (int) tmp[0] & 0xFF;
    }

    @Override
    @NotNull
    public String getSource() {
        return loader.getSource();
    }

    @NotNull
    private Pair<Long, Long> nextChunkByteRange(final long position) {
        return chunksLookahead.floorEntry(position).getValue();
    }
}
