package com.hartwig.hmftools.bachelor;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import nl.hartwigmedicalfoundation.bachelor.GeneIdentifier;
import nl.hartwigmedicalfoundation.bachelor.Program;
import nl.hartwigmedicalfoundation.bachelor.ProgramBlacklist;
import nl.hartwigmedicalfoundation.bachelor.ProgramWhitelist;
import nl.hartwigmedicalfoundation.bachelor.SnpEffect;

import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFFileReader;

public class BachelorEligibility {

    private static class ExtractedVariantInfo {
        final String GeneName = "BRCA2";
        final SnpEffect Effect = SnpEffect.MISSENSE;
        final List<String> dbSNP;
        final String Variant = "";

        private ExtractedVariantInfo(final VariantContext ctx) {
            dbSNP = Lists.newArrayList(ctx.getID().split(",")).stream().filter(s -> s.startsWith("rs")).collect(Collectors.toList());
            // TODO: extract details properly
        }

        static ExtractedVariantInfo from(final VariantContext ctx) {
            return new ExtractedVariantInfo(ctx);
        }
    }

    private final Map<String, Predicate<ExtractedVariantInfo>> predicates = Maps.newHashMap();

    private BachelorEligibility() {
    }

    public static BachelorEligibility fromMap(final Map<String, Program> input) {
        final BachelorEligibility result = new BachelorEligibility();

        for (final Map.Entry<String, Program> e : input.entrySet()) {
            final Program p = e.getValue();

            final boolean allGene = p.getPanel().getAllGenes() != null;
            final List<GeneIdentifier> panel = p.getPanel().getGene();
            final List<SnpEffect> effects = p.getPanel().getSnpEffect();

            // load ensembl mappings
            final Map<String, String> geneToEnsemblMap =
                    p.getPanel().getGene().stream().collect(Collectors.toMap(GeneIdentifier::getName, GeneIdentifier::getEnsembl));

            final List<ProgramBlacklist.Exclusion> blacklist =
                    p.getBlacklist() != null ? p.getBlacklist().getExclusion() : Lists.newArrayList();

            final Predicate<ExtractedVariantInfo> inBlacklist = v -> blacklist.stream().anyMatch(b -> {
                final boolean geneMatches = b.getGene().getName().equals(v.GeneName);
                if (b.getVariant() != null) {
                    return geneMatches && b.getVariant().equals(v.Variant);
                } else if (b.getMinCodon() != null) {
                    // TODO: check minCodon
                    return geneMatches;
                }
                return false;
            });

            final List<String> dbSNP = Lists.newArrayList();
            if (p.getWhitelist() != null) {
                for (final Object o : p.getWhitelist().getVariantOrDbSNP()) {
                    if (o instanceof ProgramWhitelist.Variant) {
                        // TODO:
                    } else if (o instanceof String) {
                        dbSNP.add((String) o);
                    }
                }
            }
            final Predicate<ExtractedVariantInfo> inWhitelist = v -> v.dbSNP.stream().anyMatch(dbSNP::contains);

            final Predicate<ExtractedVariantInfo> predicate =
                    v -> (allGene || panel.stream().anyMatch(i -> i.getName().equals(v.GeneName))) && effects.contains(v.Effect)
                            ? !inBlacklist.test(v)
                            : inWhitelist.test(v);
            result.predicates.put(e.getKey(), predicate);
        }

        return result;
    }

    public Map<String, Integer> processVCF(final VCFFileReader reader) {
        final Map<String, Integer> counts = Maps.newHashMap();
        for (final VariantContext variant : reader) {
            final List<String> matchingPrograms = predicates.entrySet()
                    .stream()
                    .filter(program -> program.getValue().test(ExtractedVariantInfo.from(variant)))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
            matchingPrograms.forEach(s -> counts.compute(s, (k, v) -> v == null ? 1 : v + 1));
        }
        return counts;
    }

}
