package com.hartwig.hmftools.bamrecovery;

import static com.hartwig.hmftools.bamrecovery.BamFile.BLOCK_FOOTER_LENGTH;
import static com.hartwig.hmftools.bamrecovery.BamFile.BLOCK_SIZE_LENGTH;
import static com.hartwig.hmftools.bamrecovery.BamFile.HEADER;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.zip.CRC32;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

class Unzipper {
    private static final Logger LOGGER = LogManager.getLogger(BamFile.class);
    private static int MAX_ARCHIVE_SIZE = 65536;

    static boolean tryUnzip(@NotNull final File file, @NotNull final Archive archive) throws IOException {
        final RandomAccessFile randomFile = new RandomAccessFile(file, "r");
        final FileChannel channel = randomFile.getChannel();
        final MappedByteBuffer mappedByteBuffer = channel.map(FileChannel.MapMode.READ_ONLY, archive.startOffset(), archive.size());
        mappedByteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        channel.close();
        randomFile.close();
        mappedByteBuffer.position(archive.size() - BLOCK_FOOTER_LENGTH);
        final int compressedSize = archive.size() - HEADER.length - BLOCK_SIZE_LENGTH - BLOCK_FOOTER_LENGTH;
        final long expectedCrc = mappedByteBuffer.getInt();
        final int uncompressedSize = mappedByteBuffer.getInt();
        final byte[] uncompressedData = new byte[MAX_ARCHIVE_SIZE];
        final Inflater inflater = new Inflater(true);
        final byte[] bytes = new byte[archive.size()];
        mappedByteBuffer.rewind();
        mappedByteBuffer.get(bytes);
        inflater.setInput(bytes, HEADER.length + BLOCK_SIZE_LENGTH, compressedSize);
        try {
            final int inflatedBytes = inflater.inflate(uncompressedData);
            if (inflatedBytes != uncompressedSize) {
                LOGGER.warn("Archive: " + archive.startOffset() + " inflated amount " + inflatedBytes + " does not match expected amount "
                        + uncompressedSize);
                return false;
            }
            final CRC32 crc32 = new CRC32();
            crc32.update(uncompressedData, 0, inflatedBytes);
            if (expectedCrc != crc32.getValue()) {
                LOGGER.warn("Archive: " + archive.startOffset() + "-" + archive.endOffset() + "(" + archive.size() + ")"
                        + " failed crc32 check");
                return false;
            }
        } catch (DataFormatException e) {
            LOGGER.warn("Archive: " + archive.startOffset() + " failed decompression with: " + e.getMessage());
            return false;
        }
        return true;
    }
}
