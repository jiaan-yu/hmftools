package com.hartwig.hmftools.bamrecovery;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.util.Arrays;

import com.twitter.elephantbird.util.StreamSearcher;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import io.reactivex.Flowable;

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

    @NotNull
    public File getFile() {
        return file;
    }

    // MIVO: side-effect: advances the file channel position to end of header
    static long findNextOffset(@NotNull final FileChannel fileChannel) throws IOException {
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

    Flowable<Archive> findArchives() {
        return Flowable.generate(() -> FlowableState.initialState(file), (state, emitter) -> {
            if (state.currentHeaderOffset() == -1) {
                LOGGER.warn("Could not find next header in " + file.getName());
                emitter.onComplete();
                return state;
            } else if (state.currentHeaderOffset() == file.length()) {
                LOGGER.warn("File " + file.getName() + " does not end with the EOF marker block.");
                emitter.onComplete();
                return state;
            } else {
                final int currentBlockSize = extractBlockSize(state.fileChannel()) + 1;
                long nextHeaderOffset = state.currentHeaderOffset() + currentBlockSize;
                if (nextHeaderOffset == file.length() && currentBlockSize == 28) {
                    emitter.onComplete();
                    return state;
                }
                state.fileChannel().position(nextHeaderOffset);
                if (nextHeaderOffset == file.length() || Arrays.equals(extractHeader(state.fileChannel()), HEADER)) {
                    state.fileChannel().position(nextHeaderOffset - BLOCK_FOOTER_LENGTH);
                    final int expectedCrc = extractFooterField(state.fileChannel());
                    final int uncompressedSize = extractFooterField(state.fileChannel());
                    final ArchiveHeader header =
                            ImmutableArchiveHeader.of(state.currentHeaderOffset(), nextHeaderOffset, currentBlockSize, expectedCrc,
                                    uncompressedSize);
                    final Archive archive = extractArchive(state.fileChannel(), header);
                    emitter.onNext(archive);
                    state.fileChannel().position(nextHeaderOffset + HEADER.length);
                } else {
                    state.fileChannel().position(state.currentHeaderOffset() + 1);
                    nextHeaderOffset = findNextOffset(state.fileChannel());
                    if (nextHeaderOffset == -1) {
                        nextHeaderOffset = file.length();
                    }
                    final ArchiveHeader header =
                            ImmutableArchiveHeader.of(state.currentHeaderOffset(), nextHeaderOffset, currentBlockSize, -1, -1);
                    emitter.onNext(ImmutableArchive.of(header, new byte[0]));
                }
                return ImmutableFlowableState.of(state.fileChannel(), nextHeaderOffset);
            }
        }, flowableState -> flowableState.fileChannel().close());
    }
}
