package com.hartwig.hmftools.bachelor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import com.google.common.collect.Maps;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BachelorApplication {

    private static final Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger(BachelorApplication.class);

    private static final String CONFIG_DIRECTORY = "configDirectory";

    private static Options createOptions() {
        final Options options = new Options();
        options.addOption(Option.builder(CONFIG_DIRECTORY).required().hasArg().desc("folder where ").build());
        return options;
    }

    private static CommandLine createCommandLine(@NotNull final Options options, @NotNull final String... args) throws ParseException {
        final CommandLineParser parser = new DefaultParser();
        return parser.parse(options, args);
    }

    private static void printHelpAndExit(final Options options) {
        final HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("Bachelor", "Determines your eligibility!", options, "", true);
        System.exit(1);
    }

    @Nullable
    private static Programs processXML(final Path path) {
        try {
            final JAXBContext context = JAXBContext.newInstance(com.hartwig.hmftools.bachelor.Programs.class);
            final Unmarshaller unmarshaller = context.createUnmarshaller();
            return (Programs) unmarshaller.unmarshal(path.toFile());
        } catch (final JAXBException e) {
            LOGGER.error("Failed to process: {}", path);
            return null;
        }
    }

    public static void main(final String... args) {
        final Options options = createOptions();
        try {
            final CommandLine cmd = createCommandLine(options, args);
            final Path configPath = Paths.get(cmd.getOptionValue(CONFIG_DIRECTORY));

            final List<Path> xmls = Files.walk(configPath).filter(p -> p.endsWith(".xml")).collect(Collectors.toList());
            final List<Programs> programs =
                    xmls.stream().map(BachelorApplication::processXML).filter(Objects::nonNull).collect(Collectors.toList());

            final Map<String, Programs.Program> programsMap = Maps.newHashMap();
            programs.forEach(ps -> ps.getProgram().forEach(p -> programsMap.put(p.name, p)));

        } catch (final Exception e) {
            printHelpAndExit(options);
        }
    }
}
