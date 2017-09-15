package com.hartwig.hmftools.bamrecovery;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;

import org.immutables.value.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Value.Immutable
@Value.Style(allParameters = true,
             passAnnotations = { NotNull.class, Nullable.class })
public abstract class FlowableState {

    public abstract FileChannel fileChannel();

    public abstract long currentHeaderOffset();

    static FlowableState initialState(@NotNull final File file) throws IOException {
        final RandomAccessFile randomFile = new RandomAccessFile(file, "r");
        final FileChannel fileChannel = randomFile.getChannel();
        return ImmutableFlowableState.of(fileChannel, BamFile.findNextOffset(fileChannel));
    }
}
