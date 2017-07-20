package com.hartwig.hmftools.bamrecovery;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

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

public final class BamRecovery {
    private static final Logger LOGGER = LogManager.getLogger(BamRecovery.class);

    private static final String INPUT_FILE = "in";

    public static void main(String[] args) throws IOException, ParseException {
        final Options options = createOptions();
        final CommandLine cmd = createCommandLine(args, options);
        final String fileName = cmd.getOptionValue(INPUT_FILE);
        if (fileName == null) {
            final HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("Bam-Recovery", options);
        } else {
            final BamFile bamFile = new BamFile(fileName);
            final List<Archive> archives = bamFile.findArchives();
            final List<Archive> truncatedArchives =
                    archives.stream().filter(archive -> archive.size() != archive.actualSize()).collect(Collectors.toList());
            final List<Archive> corruptedArchives = Lists.newArrayList();
            for (final Archive archive : archives) {
                if (archive.actualSize() == archive.size() && !Unzipper.tryUnzip(bamFile.file(), archive)) {
                    corruptedArchives.add(archive);
                }
            }
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

    private static void writeOutput(@NotNull final List<Archive> corruptedArchives, @NotNull final String csvOutPath) throws IOException {
        final BufferedWriter writer = new BufferedWriter(new FileWriter(csvOutPath, false));
        for (final Archive archive : corruptedArchives) {
            writer.write(archive.startOffset() + "," + archive.endOffset() + "," + archive.size() + "," + archive.actualSize() + "\n");
        }
        writer.close();
        LOGGER.info("Written data to " + csvOutPath);
    }

}