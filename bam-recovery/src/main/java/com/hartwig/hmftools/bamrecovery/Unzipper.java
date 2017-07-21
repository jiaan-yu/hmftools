package com.hartwig.hmftools.bamrecovery;

import java.util.zip.CRC32;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

class Unzipper {
    private static final Logger LOGGER = LogManager.getLogger(BamFile.class);
    private static int MAX_ARCHIVE_SIZE = 65536;

    static boolean canUnzip(@NotNull final Archive archive) throws InterruptedException {
        //        LOGGER.info("unzipping " + archive + " on " + Thread.currentThread().getName());
        final byte[] uncompressedData = new byte[MAX_ARCHIVE_SIZE];
        final Inflater inflater = new Inflater(true);
        inflater.setInput(archive.payload(), 0, archive.header().payloadSize());
        try {
            final int inflatedBytes = inflater.inflate(uncompressedData);
            if (inflatedBytes != archive.header().uncompressedSize()) {
                LOGGER.warn(
                        "Archive: " + archive + " inflated amount " + inflatedBytes + " does not match expected amount " + archive.header()
                                .uncompressedSize());
                return false;
            }
            final CRC32 crc32 = new CRC32();
            crc32.update(uncompressedData, 0, inflatedBytes);
            if (archive.header().crc() != (int) crc32.getValue()) {
                LOGGER.warn("Archive: " + archive + " failed crc32 check");
                return false;
            }
        } catch (DataFormatException e) {
            LOGGER.warn("Archive: " + archive + " failed decompression with: " + e.getMessage());
            return false;
        }
        //        LOGGER.info("done unzipping " + archive + " on " + Thread.currentThread().getName());
        return true;
    }
}
