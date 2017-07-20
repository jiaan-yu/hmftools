package com.hartwig.hmftools.bamrecovery;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import com.google.common.collect.Lists;
import com.twitter.elephantbird.util.StreamSearcher;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class BamFile {

    private static final Logger LOGGER = LogManager.getLogger(BamFile.class);

    private static final String HEADER_BINARY =
            "00011111100010110000100000000100000000000000000000000000000000000000000011111111000001100000000001000010010000110000001000000000";
    static final byte[] HEADER = new BigInteger(HEADER_BINARY, 2).toByteArray();
    static final int BLOCK_SIZE_LENGTH = 2;
    static final int BLOCK_FOOTER_LENGTH = 8;

    @NotNull
    private final File file;

    BamFile(@NotNull final String fileName) throws IOException {
        this.file = new File(fileName);
    }

    long findNextOffset(@NotNull final InputStream archive, @NotNull AtomicLong currentStreamPosition) throws IOException {
        final StreamSearcher searcher = new StreamSearcher(HEADER);
        final long bytesReadForNextMatch = searcher.search(archive);
        if (bytesReadForNextMatch != -1) {
            final long offset = bytesReadForNextMatch + currentStreamPosition.get() - HEADER.length;
            currentStreamPosition.set(currentStreamPosition.get() + bytesReadForNextMatch);
            return offset;
        }
        currentStreamPosition.set(file.length());
        return -1;
    }

    private int extractBlockSize(@NotNull final InputStream archive, @NotNull AtomicLong currentStreamPosition) throws IOException {
        final long blockSize = readLittleEndianField(archive, BLOCK_SIZE_LENGTH);
        currentStreamPosition.set(currentStreamPosition.get() + BLOCK_SIZE_LENGTH);
        return (int) blockSize;
    }

    private static long readLittleEndianField(@NotNull final InputStream archive, final int bytes) throws IOException {
        if (bytes == 1) {
            return archive.read() & 0x00000000ffffffffL;
        } else {
            final ByteBuffer buffer = ByteBuffer.allocate(bytes).order(ByteOrder.LITTLE_ENDIAN);
            archive.read(buffer.array());
            if (bytes == 2) {
                return buffer.getShort() & 0x00000000ffffffffL;
            } else {
                return buffer.getInt() & 0x00000000ffffffffL;
            }
        }
    }

    List<Archive> findArchives() throws IOException {
        LOGGER.info("finding archives...");
        final List<Archive> archives = Lists.newArrayList();
        final byte[] bytesArr = new byte[HEADER.length];
        final RandomAccessFile randomFile = new RandomAccessFile(file, "r");
        final FileChannel channel = randomFile.getChannel();
        final InputStream archive = new FileInputStream(file);
        final AtomicLong currentStreamPosition = new AtomicLong(0);

        long previousHeaderOffset = findNextOffset(archive, currentStreamPosition);
        while (previousHeaderOffset != -1 || previousHeaderOffset != file.length()) {
            final int currentBlockSize = extractBlockSize(archive, currentStreamPosition) + 1;
            final long expectedNextHeaderOffset = currentStreamPosition.get() + currentBlockSize - HEADER.length - BLOCK_SIZE_LENGTH;
            if (expectedNextHeaderOffset == file.length()) {
                LOGGER.info("Reached end of file.");
                if (currentBlockSize != 28) {
                    LOGGER.warn("EOF maker block should have a size of 28, but was: " + currentBlockSize);
                }
                break;
            }
            final MappedByteBuffer mappedByteBuffer = channel.map(FileChannel.MapMode.READ_ONLY, expectedNextHeaderOffset, HEADER.length);
            mappedByteBuffer.get(bytesArr, 0, HEADER.length);
            if (equalsHeader(bytesArr)) {
                // header is in expected position
                long skipped = archive.skip(expectedNextHeaderOffset - currentStreamPosition.get() + HEADER.length);
                if (skipped < expectedNextHeaderOffset - currentStreamPosition.get()) {
                    LOGGER.warn("skipped less than expected: " + skipped + " instead of " + (expectedNextHeaderOffset
                            - currentStreamPosition.get()));
                }
                archives.add(ImmutableArchive.of(previousHeaderOffset, expectedNextHeaderOffset, currentBlockSize));
                previousHeaderOffset = expectedNextHeaderOffset;
                currentStreamPosition.set(expectedNextHeaderOffset + HEADER.length);
            } else {
                final long nextOffset = findNextOffset(archive, currentStreamPosition);
                LOGGER.info("Truncated archive: " + previousHeaderOffset + " -> " + nextOffset + ". Expected size: " + currentBlockSize
                        + ", actual size: " + (nextOffset - previousHeaderOffset));
                archives.add(ImmutableArchive.of(previousHeaderOffset, nextOffset, currentBlockSize));
                previousHeaderOffset = nextOffset;
            }
        }
        channel.close();
        LOGGER.info("done. found " + archives.size() + " archives.");
        return archives;
    }

    private boolean equalsHeader(final byte[] bytesArray) {
        for (int i = 0; i < bytesArray.length; i++) {
            if (bytesArray[i] != HEADER[i]) {
                return false;
            }
        }
        return true;
    }

    @NotNull
    public File file() {
        return file;
    }
}
