package com.hartwig.hmftools.bamrecovery;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

class FileWriter {
    private static final Logger LOGGER = LogManager.getLogger(FileWriter.class);

    @NotNull
    private final FileChannel source;

    @NotNull
    private final FileChannel destination;

    FileWriter(@NotNull final BamFile source, @NotNull final String fileName) throws IOException {
        this.source = FileChannel.open(source.getFile().toPath(), StandardOpenOption.READ);
        this.destination = FileChannel.open(Paths.get(fileName), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
    }

    void copyWithoutInvalidArchives(@NotNull final List<Archive> invalidArchives) throws IOException {
        long sourcePosition = 0;
        long destinationPosition = 0;
        for (final Archive invalidArchive : invalidArchives) {
            final long count = invalidArchive.header().startOffset() - sourcePosition;
            destination.transferFrom(source, destinationPosition, count);
            sourcePosition = invalidArchive.header().endOffset();
            destinationPosition += count;
            source.position(sourcePosition);
        }
        final long count = source.size() - sourcePosition;
        destination.transferFrom(source, destinationPosition, count);
    }
}
