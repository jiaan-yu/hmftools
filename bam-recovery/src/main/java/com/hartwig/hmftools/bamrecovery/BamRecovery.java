package com.hartwig.hmftools.bamrecovery;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.zip.GZIPInputStream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class BamRecovery {
    private static final Logger LOGGER = LogManager.getLogger(BamRecovery.class);

    private static long bytesRead = 0;
    private static final String HEADER_BINARY =
            "00011111100010110000100000000100000000000000000000000000000000000000000011111111000001100000000001000010010000110000001000000000";
    private static final byte[] HEADER = new BigInteger(HEADER_BINARY, 2).toByteArray();
    private static final String HEADER_HEX = "\\x1f\\x8b\\x08\\x04\\x00\\x00\\x00\\x00\\x00\\xff\\x06\\x00\\x42\\x43\\x02\\x00";

    private static final String INPUT_FILE = "in";
    private static final String COUNT_SUBARCHIVES = "count";
    private static final String FIND_NEXT_ARCHIVE = "find";
    private static final String SKIP_LENGTH = "skip";
    private static final String SET_SIZE = "size";

    public static void main(String[] args) throws IOException, ParseException {
        final Options options = createOptions();
        final CommandLine cmd = createCommandLine(args, options);
        final String fileName = cmd.getOptionValue(INPUT_FILE);
        final InputStream stream = new FileInputStream(fileName);
        if (cmd.hasOption(SKIP_LENGTH)) {
            final long skipLength = Long.parseLong(cmd.getOptionValue(SKIP_LENGTH));
            stream.skip(skipLength);
            bytesRead = skipLength;
        }
        if (cmd.hasOption(COUNT_SUBARCHIVES)) {
            countSubArchives(stream);
        } else if (cmd.hasOption(FIND_NEXT_ARCHIVE)) {
            final long offset = findNextSubArchive(stream);
            LOGGER.info("offset of next sub-archive: " + offset);
        } else if (cmd.hasOption(SET_SIZE)) {
            final short size = Short.parseShort(cmd.getOptionValue(SET_SIZE));
            LOGGER.info("setting size to :" + size);
            final InputStream newHeader = setSize(stream, size);
            writeToTest(stream, newHeader);
        }

    }

    private static void countSubArchives(@NotNull final InputStream stream) throws IOException {
        int count = 0;
        LOGGER.info("block: " + count + "; offset: " + bytesRead);
        InputStream subStream = extractSubArchive(stream);
        while (subStream != null) {
            count++;
            LOGGER.info("block: " + count + "; offset: " + bytesRead);
            //            LOGGER.info("chunk: " + count);
            final GZIPInputStream zipStream = new GZIPInputStream(subStream);
            subStream = extractSubArchive(stream);
        }
        LOGGER.info("found " + count + " gzip archives");
    }

    private static void extractHeader(@NotNull final InputStream archive) throws IOException {
        final long id1 = readLittleEndianField(archive, 1);
        LOGGER.info(id1);
        final long id2 = readLittleEndianField(archive, 1);
        LOGGER.info(id2);
        final long compressionMethod = readLittleEndianField(archive, 1);
        LOGGER.info(compressionMethod);
        final long flags = readLittleEndianField(archive, 1);
        LOGGER.info(flags);
        final long modificationTime = readLittleEndianField(archive, 4);
        final long extraFlags = readLittleEndianField(archive, 1);
        final long osFlag = readLittleEndianField(archive, 1);
        final long extraLength = readLittleEndianField(archive, 2);
        LOGGER.info(extraLength);
        archive.skip(4);
        final long blockSize = readLittleEndianField(archive, 2);
        LOGGER.info(blockSize);
        archive.skip(blockSize - extraLength - 19);
        final long crc = readLittleEndianField(archive, 4);
        LOGGER.info(crc);
        final long inputSize = readLittleEndianField(archive, 4);
        LOGGER.info(inputSize);
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

    @Nullable
    private static InputStream extractSubArchive(@NotNull final InputStream archive) throws IOException {
        final ByteBuffer headerBuffer = ByteBuffer.allocate(16);
        if (archive.read(headerBuffer.array()) <= 0) {
            return null;
        }
        final int id1 = headerBuffer.get(0);
        final int id2 = headerBuffer.get(1);
        final int compressionMethod = headerBuffer.get(2);
        final int flags = headerBuffer.get(3);
        LOGGER.info("\theader: [" + id1 + " " + id2 + " " + compressionMethod + " " + flags + "]:" + bufferToBinary(headerBuffer));
        final ByteBuffer blockSizeBuffer = ByteBuffer.allocate(2);
        archive.read(blockSizeBuffer.array());
        final ByteBuffer blockSizeBufferClone = blockSizeBuffer.duplicate().order(ByteOrder.LITTLE_ENDIAN);
        final int blockSize = (int) (blockSizeBufferClone.getShort() & 0x00000000ffffffffL);
        LOGGER.info("\tblock size: " + blockSize + "; buffer: " + bufferToBinary(blockSizeBuffer));
        final ByteBuffer remainingBytes = ByteBuffer.allocate(blockSize - 6 - 19 + 8);
        archive.read(remainingBytes.array());
        final ByteBuffer archiveBuffer = ByteBuffer.allocate(blockSize + 1);
        archiveBuffer.put(headerBuffer);
        archiveBuffer.put(blockSizeBuffer);
        archiveBuffer.put(remainingBytes);
        bytesRead += blockSize + 1;
        return new ByteArrayInputStream(archiveBuffer.array());
    }

    @Nullable
    private static InputStream setSize(@NotNull final InputStream archive, final short size) throws IOException {
        // assumes one archive
        final ByteBuffer headerBuffer = ByteBuffer.allocate(16);
        if (archive.read(headerBuffer.array()) <= 0) {
            return null;
        }
        final int id1 = headerBuffer.get(0);
        final int id2 = headerBuffer.get(1);
        final int compressionMethod = headerBuffer.get(2);
        final int flags = headerBuffer.get(3);
        LOGGER.info("\theader: [" + id1 + " " + id2 + " " + compressionMethod + " " + flags + "]:" + bufferToBinary(headerBuffer));
        final ByteBuffer blockSizeBuffer = ByteBuffer.allocate(2);
        archive.read(blockSizeBuffer.array());
        final ByteBuffer blockSizeBufferClone = blockSizeBuffer.duplicate().order(ByteOrder.LITTLE_ENDIAN);
        final int blockSize = (int) (blockSizeBufferClone.getShort() & 0x00000000ffffffffL);
        LOGGER.info("\tblock size: " + blockSize + "; buffer: " + bufferToBinary(blockSizeBuffer));
        final ByteBuffer newHeaderBuffer = ByteBuffer.allocate(18);
        newHeaderBuffer.put(headerBuffer);
        final ByteBuffer newBlockSizeBuffer = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN);
        newBlockSizeBuffer.mark();
        newBlockSizeBuffer.putShort(size);
        newBlockSizeBuffer.reset();
        newHeaderBuffer.put(newBlockSizeBuffer);
        bytesRead += blockSize + 1;
        return new ByteArrayInputStream(newHeaderBuffer.array());
    }

    // 0b1111101 11000101
    // 0b111100 01011110
    private static long extractBlockSize(@NotNull final InputStream archive) throws IOException {
        //        archive.mark(100);
        archive.skip(16);
        final long blockSize = readLittleEndianField(archive, 2);
        LOGGER.info("blockSize: " + blockSize);
        //        archive.reset();
        archive.skip(blockSize - 6 - 19 + 8);
        return blockSize;
    }

    private static void writeToTest(@NotNull final InputStream archive, @NotNull final InputStream newHeader) throws IOException {
        final byte[] buffer = new byte[8192];
        final FileOutputStream tempFile = new FileOutputStream("test.gz");
        int len;
        while ((len = newHeader.read(buffer)) > 0) {
            tempFile.write(buffer, 0, len);
        }
        while ((len = archive.read(buffer)) > 0) {
            tempFile.write(buffer, 0, len);
        }
        tempFile.close();
    }

    private static String bufferToBinary(@NotNull final ByteBuffer buf) {
        String result = "";
        for (byte b : buf.array()) {
            result += String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0');
        }
        return result;
    }

    private static String bufferToXhex(@NotNull final ByteBuffer buf) {
        String result = "";
        for (byte b : buf.array()) {
            result += String.format("\\x%02x", b);
        }
        return result;
    }

    private static long findNextSubArchive(@NotNull final InputStream archive) throws IOException {
        final int bufferSize = 1048576;
        final ByteBuffer headerBuffer = ByteBuffer.allocate(bufferSize);
        while (archive.read(headerBuffer.array()) > 0) {
            final int index = KMPMatch.indexOf(headerBuffer.array(), HEADER);
            if (index != -1) {
                bytesRead += index;
                return bytesRead;
            }
            LOGGER.warn("Finding iterates; might return wrong results.");
            bytesRead += bufferSize;
        }
        return -1;
    }

    @NotNull
    private static Options createOptions() {
        final Options options = new Options();
        options.addOption(INPUT_FILE, true, "Path towards the input file.");
        options.addOption(COUNT_SUBARCHIVES, false, "Count gzip archives in bam file");
        options.addOption(FIND_NEXT_ARCHIVE, false, "Find offset of next gzip header");
        options.addOption(SKIP_LENGTH, true, "Number of bytes to skip.");
        options.addOption(SET_SIZE, true, "Number of bytes to skip.");
        return options;
    }

    @NotNull
    private static CommandLine createCommandLine(@NotNull String[] args, @NotNull Options options) throws ParseException {
        final CommandLineParser parser = new DefaultParser();
        return parser.parse(options, args);
    }

}