package com.hartwig.hmftools.bamrecovery;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;

import com.twitter.elephantbird.util.StreamSearcher;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;

class BamFile {

    private static final Logger LOGGER = LogManager.getLogger(BamFile.class);

    private static final String HEADER_BINARY =
            "00011111100010110000100000000100000000000000000000000000000000000000000011111111000001100000000001000010010000110000001000000000";
    static final byte[] HEADER = new BigInteger(HEADER_BINARY, 2).toByteArray();
    static final int BLOCK_SIZE_LENGTH = 2;
    static final int BLOCK_FOOTER_LENGTH = 8;
    private static final int FOOTER_FIELD_LENGTH = 4;

    @NotNull
    private final File file;

    BamFile(@NotNull final String fileName) throws IOException {
        this.file = new File(fileName);
    }

    long findNextOffset(@NotNull final FileChannel fileChannel) throws IOException {
        final StreamSearcher searcher = new StreamSearcher(HEADER);
        final long currentPosition = fileChannel.position();
        final InputStream fileInputStream = Channels.newInputStream(fileChannel);
        final long bytesReadForNextMatch = searcher.search(fileInputStream);
        if (bytesReadForNextMatch != -1) {
            return bytesReadForNextMatch + currentPosition - HEADER.length;
        }
        return -1;
    }

    private int extractBlockSize(@NotNull final FileChannel fileChannel) throws IOException {
        return extractBuffer(fileChannel, BLOCK_SIZE_LENGTH).getShort();
    }

    private int extractFooterField(@NotNull final FileChannel fileChannel) throws IOException {
        return extractBuffer(fileChannel, FOOTER_FIELD_LENGTH).getInt();
    }

    private ByteBuffer extractBuffer(@NotNull final FileChannel fileChannel, final int length) throws IOException {
        final ByteBuffer byteBuffer = ByteBuffer.allocate(length).order(ByteOrder.LITTLE_ENDIAN);
        fileChannel.read(byteBuffer);
        byteBuffer.rewind();
        return byteBuffer;
    }

    private byte[] extractHeader(@NotNull final FileChannel fileChannel) throws IOException {
        return extractBuffer(fileChannel, HEADER.length).array();
    }

    private Archive extractArchive(@NotNull final FileChannel fileChannel, @NotNull final ArchiveHeader header) throws IOException {
        final ByteBuffer blockSizeBuffer = ByteBuffer.allocate(header.payloadSize());
        fileChannel.position(header.payloadPosition());
        fileChannel.read(blockSizeBuffer);
        final byte[] payload = blockSizeBuffer.array();
        return ImmutableArchive.of(header, payload);
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

    Observable<Archive> findArchives() {
        return Observable.create((final ObservableEmitter<Archive> observer) -> {
            try {
                final RandomAccessFile randomFile = new RandomAccessFile(file, "r");
                final FileChannel fileChannel = randomFile.getChannel();
                long previousHeaderOffset = findNextOffset(fileChannel);
                while ((previousHeaderOffset != -1 || previousHeaderOffset != file.length()) && !observer.isDisposed()) {
                    //                    LOGGER.info("reading archive at " + previousHeaderOffset);
                    final int currentBlockSize = extractBlockSize(fileChannel) + 1;
                    long nextHeaderOffset = previousHeaderOffset + currentBlockSize;

                    if (nextHeaderOffset == file.length()) {
                        LOGGER.info("Reached end of file.");
                        if (currentBlockSize != 28) {
                            LOGGER.warn("EOF maker block should have a size of 28, but was: " + currentBlockSize);
                        }
                        break;
                    }
                    fileChannel.position(nextHeaderOffset);
                    if (equalsHeader(extractHeader(fileChannel))) {
                        // header is in expected position
                        fileChannel.position(nextHeaderOffset - BLOCK_FOOTER_LENGTH);
                        final int expectedCrc = extractFooterField(fileChannel);
                        final int uncompressedSize = extractFooterField(fileChannel);
                        final ArchiveHeader header =
                                ImmutableArchiveHeader.of(previousHeaderOffset, nextHeaderOffset, currentBlockSize, expectedCrc,
                                        uncompressedSize);

                        final Archive archive = extractArchive(fileChannel, header);
                        if (!observer.isDisposed()) {
                            observer.onNext(archive);
                        }
                        fileChannel.position(nextHeaderOffset + HEADER.length);
                    } else {
                        nextHeaderOffset = findNextOffset(fileChannel);
                        final ArchiveHeader header =
                                ImmutableArchiveHeader.of(previousHeaderOffset, nextHeaderOffset, currentBlockSize, -1, -1);
                        LOGGER.info("Truncated archive: " + previousHeaderOffset + " -> " + nextHeaderOffset + ". Expected size: "
                                + currentBlockSize + ", actual size: " + (nextHeaderOffset - previousHeaderOffset));
                        if (!observer.isDisposed()) {
                            observer.onNext(ImmutableArchive.of(header, new byte[0]));
                        }
                    }
                    previousHeaderOffset = nextHeaderOffset;
                }
                fileChannel.close();
                if (!observer.isDisposed()) {
                    observer.onComplete();
                }
            } catch (final Exception e) {
                observer.onError(e);
            }
        });
    }

    private boolean equalsHeader(final byte[] bytesArray) {
        for (int i = 0; i < bytesArray.length; i++) {
            if (bytesArray[i] != HEADER[i]) {
                return false;
            }
        }
        return true;
    }
}
