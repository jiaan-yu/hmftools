package com.hartwig.hmftools.patientreporter.report.components;

import java.awt.Color;
import java.text.DecimalFormat;

import com.hartwig.hmftools.patientreporter.report.util.PatientReportFormat;

import org.jetbrains.annotations.NotNull;

import net.sf.dynamicreports.report.builder.component.ComponentBuilder;

public final class MutationalBurdenSection {

    private static final int BUFFER = 3;
    private static final double START = 1E-2;
    private static final double END = 120;
    private static final Color COLOR_BEGIN = new Color(253, 235, 208);
    private static final Color COLOR_END = new Color(248, 196, 113);

    @NotNull
    public static ComponentBuilder<?, ?> build(double tumorMutationalBurdenIndicator, boolean hasReliablePurityFit) {
        final int graphValue = computeGraphValue(tumorMutationalBurdenIndicator);

        final GradientBar gradient = !hasReliablePurityFit
                ? ImmutableGradientBar.of(COLOR_BEGIN, COLOR_END, "Low", "High")
                : ImmutableGradientBar.of(COLOR_BEGIN, COLOR_END, "Low", "High", graphValue);
        final SliderSection sliderSection = ImmutableSliderSection.of("Tumor Mutational Burden",
                interpret(tumorMutationalBurdenIndicator, hasReliablePurityFit),
                description(),
                gradient);

        return sliderSection.build();
    }

    @NotNull
    private static String interpret(double tumorMutationalBurdenIndicator, boolean hasReliablePurityFit) {
        String formattedTMB = new DecimalFormat("#.####").format(tumorMutationalBurdenIndicator);
        String interpretedTMB;

        if (hasReliablePurityFit) {
            interpretedTMB = formattedTMB + " variants per Mb.";
        } else {
            interpretedTMB = formattedTMB;
        }
        return PatientReportFormat.correctValueForFitReliability(interpretedTMB, hasReliablePurityFit);
    }

    private static int computeGraphValue(double value) {
        final double scaledStart = scale(START);
        final double scaledEnd = scale(END);
        final double scaledIntervalLength = scaledEnd - scaledStart;
        final double scaleValue = Math.min(scaledEnd, Math.max(scaledStart, scale(value)));

        final double graphIntervalLength = 100 - 2 * BUFFER;
        return (int) Math.round((scaleValue - scaledStart) * graphIntervalLength / scaledIntervalLength + BUFFER);
    }

    private static double scale(double value) {
        return Math.log10(value);
    }

    @NotNull
    private static String description() {
        return "The tumor mutational burden score represents the number of all somatic variants "
                + "across the whole genome of the tumor per Mb. ";
    }
}
