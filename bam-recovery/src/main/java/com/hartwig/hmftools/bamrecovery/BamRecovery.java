package com.hartwig.hmftools.bamrecovery;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import com.google.common.collect.Lists;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;

public final class BamRecovery {
    private static final Logger LOGGER = LogManager.getLogger(BamRecovery.class);

    private static final String INPUT_FILE = "in";

    public static void main(String[] args) throws ParseException, IOException {
        final Options options = createOptions();
        final CommandLine cmd = createCommandLine(args, options);
        final String fileName = cmd.getOptionValue(INPUT_FILE);
        if (fileName == null) {
            final HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("Bam-Recovery", options);
        } else {
            final BamFile bamFile = new BamFile(fileName);
            LOGGER.info("reading archives...");

            final List<ArchiveHeader> truncatedArchives = Lists.newArrayList();
            final List<ArchiveHeader> corruptedArchives = Lists.newArrayList();

            bamFile.findArchives().doOnNext(archive -> {
                if (archive.isTruncated()) {
                    truncatedArchives.add(archive.header());
                }
            }).buffer(Runtime.getRuntime().availableProcessors()).flatMap(buffer -> {
                final Observable<Archive> buffered = Observable.fromIterable(buffer);
                final List<Archive> filtered = buffered.flatMap(archive2 -> Observable.just(archive2)
                        .subscribeOn(Schedulers.computation())
                        .filter(archive -> !archive.isTruncated() && !Unzipper.canUnzip(archive))).toList().blockingGet();
                return Observable.fromIterable(filtered);
            }).blockingSubscribe(archive -> corruptedArchives.add(archive.header()), LOGGER::error, () -> LOGGER.info("done."));

            writeOutput(truncatedArchives, fileName + ".truncated.csv");
            writeOutput(corruptedArchives, fileName + ".corrupted.csv");
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

    private static void writeOutput(@NotNull final List<ArchiveHeader> archives, @NotNull final String csvOutPath) throws IOException {
        final BufferedWriter writer = new BufferedWriter(new FileWriter(csvOutPath, false));
        for (final ArchiveHeader archive : archives) {
            writer.write(archive.startOffset() + "," + archive.endOffset() + "," + archive.size() + "," + archive.actualSize() + "\n");
        }
        writer.close();
        LOGGER.info("Written data to " + csvOutPath);
    }

}