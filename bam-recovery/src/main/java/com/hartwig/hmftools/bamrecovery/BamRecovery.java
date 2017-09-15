package com.hartwig.hmftools.bamrecovery;

import java.io.IOException;
import java.util.List;

import com.google.common.annotations.VisibleForTesting;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import io.reactivex.Flowable;
import io.reactivex.schedulers.Schedulers;

public final class BamRecovery {
    private static final Logger LOGGER = LogManager.getLogger(BamRecovery.class);

    private static final String INPUT_FILE = "in";
    private static final String OUTPUT_DIR = "slices";

    public static void main(String[] args) throws ParseException, IOException, InterruptedException {
        final Options options = createOptions();
        final CommandLine cmd = createCommandLine(args, options);
        final String fileName = cmd.getOptionValue(INPUT_FILE);
        if (fileName == null) {
            final HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("Bam-Recovery", options);
        } else {
            final BamFile bamFile = new BamFile(fileName);
            final List<Archive> validArchives = getValidArchives(bamFile).toList().blockingGet();
        }
    }

    @NotNull
    private static Options createOptions() {
        final Options options = new Options();
        options.addOption(INPUT_FILE, true, "Path towards the input file.");
        return options;
    }

    @NotNull
    private static CommandLine createCommandLine(@NotNull String[] args, @NotNull Options options) throws ParseException {
        final CommandLineParser parser = new DefaultParser();
        return parser.parse(options, args);
    }

    @NotNull
    @VisibleForTesting
    static Flowable<Archive> getValidArchives(@NotNull final BamFile bamFile) {
        return bamFile.findArchives()
                .flatMap(archive -> Flowable.just(archive)
                        .subscribeOn(Schedulers.computation())
                        .filter(archive1 -> !archive1.isTruncated() && Unzipper.canUnzip(archive1)));
    }
}