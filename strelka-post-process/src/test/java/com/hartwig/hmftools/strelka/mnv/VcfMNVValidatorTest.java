package com.hartwig.hmftools.strelka.mnv;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.Streams;
import com.google.common.io.Resources;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFFileReader;

public class VcfMNVValidatorTest {
    private static final File POTENTIAL_VCF_FILE = new File(Resources.getResource("potential_mnvs.vcf").getPath());
    private static final File CORRECTED_VCF_FILE = new File(Resources.getResource("corrected_mnvs.vcf").getPath());

    private static final VCFFileReader POTENTIAL_VCF_READER = new VCFFileReader(POTENTIAL_VCF_FILE, false);
    private static final VCFFileReader CORRECTED_VCF_READER = new VCFFileReader(CORRECTED_VCF_FILE, false);
    private static final List<VariantContext> POTENTIAL_VARIANTS = Streams.stream(POTENTIAL_VCF_READER).collect(Collectors.toList());
    private static final List<VariantContext> CORRECTED_VARIANTS = Streams.stream(CORRECTED_VCF_READER).collect(Collectors.toList());

    private static final VariantContext VAR_1_A_ATC = POTENTIAL_VARIANTS.get(0);
    private static final VariantContext VAR_2_A_ATC = POTENTIAL_VARIANTS.get(1);
    private static final VariantContext VAR_3_TCC_T = POTENTIAL_VARIANTS.get(2);
    private static final VariantContext VAR_4_C_T = POTENTIAL_VARIANTS.get(3);
    private static final VariantContext VAR_5_A_T = POTENTIAL_VARIANTS.get(4);
    private static final VariantContext VAR_5_G_GTTTGAC = POTENTIAL_VARIANTS.get(5);
    private static final VariantContext VAR_6_A_T = POTENTIAL_VARIANTS.get(6);
    private static final VariantContext VAR_7_CT_C = POTENTIAL_VARIANTS.get(7);
    private static final VariantContext VAR_9_T_A = POTENTIAL_VARIANTS.get(8);
    private static final VariantContext MNV_1_AA_ATCATC = CORRECTED_VARIANTS.get(0);
    private static final VariantContext MNV_1_AGTCC_ATCGT = CORRECTED_VARIANTS.get(1);
    private static final VariantContext MNV_2_ATCC_ATCT = CORRECTED_VARIANTS.get(2);
    private static final VariantContext MNV_2_ATCCC_ATCTT = CORRECTED_VARIANTS.get(3);
    private static final VariantContext MNV_2_AGC_ATCGT = CORRECTED_VARIANTS.get(4);
    private static final VariantContext MNV_3_TCCC_TT = CORRECTED_VARIANTS.get(5);
    private static final VariantContext MNV_3_TCCA_TT = CORRECTED_VARIANTS.get(6);
    private static final VariantContext MNV_4_CA_TT = CORRECTED_VARIANTS.get(7);
    private static final VariantContext MNV_4_CGA_TGT = CORRECTED_VARIANTS.get(8);
    private static final VariantContext MNV_4_CAA_TTT = CORRECTED_VARIANTS.get(9);
    private static final VariantContext MNV_7_CTT_CA = CORRECTED_VARIANTS.get(10);

    static {
        assertFileVariants();
    }

    // MIVO: potential: 2 (A -> ATC), 3 (TCC -> T)                              corrected: 2 (ATCC -> ATCT)
    @Test
    public void detectsInsAndDelContainedInMNV() {
        final VariantContext mnv = MNV_2_ATCC_ATCT;
        assertTrue(VcfMNVValidator.variantContainedInMnv(VAR_2_A_ATC, mnv, 0, 0));
        assertTrue(VcfMNVValidator.variantContainedInMnv(VAR_3_TCC_T, mnv, 1, 3));
        assertTrue(potentialVariantsEqualMnv(mnv, VAR_2_A_ATC, VAR_3_TCC_T));
        //        assertFalse(potentialVariantsEqualMnv(mnv, VAR_1_A_ATC, VAR_3_TCC_T));
    }

    // MIVO: potential: 1 (A -> ATC), 3 (TCC -> T)                              corrected: 1 (AGTCC -> ATCGT)
    @Test
    public void detectsInsAndDelContainedInMNVWithGap() {
        final VariantContext mnv = MNV_1_AGTCC_ATCGT;
        assertTrue(VcfMNVValidator.variantContainedInMnv(VAR_1_A_ATC, mnv, 0, 0));
        assertTrue(VcfMNVValidator.variantContainedInMnv(VAR_3_TCC_T, mnv, 2, 4));
        assertTrue(potentialVariantsEqualMnv(mnv, VAR_1_A_ATC, VAR_3_TCC_T));
        assertFalse(potentialVariantsEqualMnv(mnv, VAR_2_A_ATC, VAR_3_TCC_T));
    }

    // MIVO: potential: 1 (A -> ATC), 2 (A -> ATC)                              corrected: 1 (AA -> ATCATC)
    @Test
    public void detectsInsAndInsContainedInMNV() {
        final VariantContext mnv = MNV_1_AA_ATCATC;
        assertTrue(VcfMNVValidator.variantContainedInMnv(VAR_1_A_ATC, mnv, 0, 0));
        assertTrue(VcfMNVValidator.variantContainedInMnv(VAR_2_A_ATC, mnv, 1, 3));
        assertTrue(potentialVariantsEqualMnv(mnv, VAR_1_A_ATC, VAR_2_A_ATC));
    }

    // MIVO: potential: 2 (A -> ATC), 4 (C -> T)                                corrected: 2 (AGC -> ATCGT)
    @Test
    public void detectsInsAndSNVContainedInMNVWithGap() {
        final VariantContext mnv = MNV_2_AGC_ATCGT;
        assertTrue(VcfMNVValidator.variantContainedInMnv(VAR_2_A_ATC, mnv, 0, 0));
        assertTrue(VcfMNVValidator.variantContainedInMnv(VAR_4_C_T, mnv, 2, 4));
        assertTrue(potentialVariantsEqualMnv(mnv, VAR_2_A_ATC, VAR_4_C_T));
        assertFalse(potentialVariantsEqualMnv(mnv, VAR_2_A_ATC, VAR_3_TCC_T, VAR_4_C_T));
    }

    // MIVO: potential: 3 (TCC -> T), 6 (A -> T)                                corrected: 3 (TCCA -> TT)
    @Test
    public void detectsDelAndSNVContainedInMNV() {
        final VariantContext mnv = MNV_3_TCCA_TT;
        assertTrue(VcfMNVValidator.variantContainedInMnv(VAR_3_TCC_T, mnv, 0, 0));
        assertTrue(VcfMNVValidator.variantContainedInMnv(VAR_6_A_T, mnv, 3, 1));
        assertTrue(potentialVariantsEqualMnv(mnv, VAR_3_TCC_T, VAR_6_A_T));
    }

    // MIVO: potential: 7 (CT -> C), 9 (T -> A)                                corrected: 7 (CTT -> CA)
    @Test
    public void detectsDelAndSNVContainedInMNV2() {
        final VariantContext mnv = MNV_7_CTT_CA;
        assertTrue(VcfMNVValidator.variantContainedInMnv(VAR_7_CT_C, mnv, 0, 0));
        assertTrue(VcfMNVValidator.variantContainedInMnv(VAR_9_T_A, mnv, 2, 1));
        assertTrue(potentialVariantsEqualMnv(mnv, VAR_7_CT_C, VAR_9_T_A));
    }

    // MIVO: potential: 4 (C -> T), 5 (A -> T)                                corrected: 4 (CA -> TT)
    @Test
    public void detectsSNVAndSNVContainedInMNV() {
        final VariantContext mnv = MNV_4_CA_TT;
        assertTrue(VcfMNVValidator.variantContainedInMnv(VAR_4_C_T, mnv, 0, 0));
        assertTrue(VcfMNVValidator.variantContainedInMnv(VAR_5_A_T, mnv, 1, 1));
        assertTrue(potentialVariantsEqualMnv(mnv, VAR_4_C_T, VAR_5_A_T));
    }

    // MIVO: potential: 4 (C -> T), 6 (A -> T)                                corrected: 4 (CGA -> TGT)
    @Test
    public void detectsSNVAndSNVContainedInMNVWithGap() {
        final VariantContext mnv = MNV_4_CGA_TGT;
        assertTrue(VcfMNVValidator.variantContainedInMnv(VAR_4_C_T, mnv, 0, 0));
        assertTrue(VcfMNVValidator.variantContainedInMnv(VAR_6_A_T, mnv, 2, 2));
        assertTrue(potentialVariantsEqualMnv(mnv, VAR_4_C_T, VAR_6_A_T));
        assertFalse(potentialVariantsEqualMnv(mnv, VAR_4_C_T, VAR_5_A_T, VAR_6_A_T));
        assertFalse(potentialVariantsEqualMnv(mnv, VAR_4_C_T, VAR_5_G_GTTTGAC, VAR_6_A_T));
    }

    // MIVO: potential: 4 (C -> T), 5 (A -> T), 6 (A -> T)                    corrected: 4 (CAA -> TTT)
    @Test
    public void detectsSNVAndSNVAndSNVContainedInMNV() {
        final VariantContext mnv = MNV_4_CAA_TTT;
        assertTrue(VcfMNVValidator.variantContainedInMnv(VAR_4_C_T, mnv, 0, 0));
        assertTrue(VcfMNVValidator.variantContainedInMnv(VAR_5_A_T, mnv, 1, 1));
        assertTrue(VcfMNVValidator.variantContainedInMnv(VAR_6_A_T, mnv, 2, 2));
        assertTrue(potentialVariantsEqualMnv(mnv, VAR_4_C_T, VAR_5_A_T, VAR_6_A_T));
        assertFalse(potentialVariantsEqualMnv(mnv, VAR_4_C_T, VAR_6_A_T));
    }

    private static boolean potentialVariantsEqualMnv(@NotNull final VariantContext mnv, @NotNull final VariantContext... variants) {
        PotentialMNV potentialMNV = PotentialMNV.fromVariant(variants[0]);
        for (int index = 1; index < variants.length; index++) {
            potentialMNV = PotentialMNV.addVariant(potentialMNV, variants[index]);
        }
        return mnv.getContig().equals(potentialMNV.chromosome()) && mnv.getStart() == potentialMNV.start()
                && mnv.getEnd() == potentialMNV.end() - 1 && VcfMNVValidator.potentialMnvMatchesCorrectedMnv(potentialMNV, mnv);
    }

    private static void assertFileVariants() {
        assertVariant(VAR_1_A_ATC, 1, "A", "ATC");
        assertVariant(VAR_2_A_ATC, 2, "A", "ATC");
        assertVariant(VAR_3_TCC_T, 3, "TCC", "T");
        assertVariant(VAR_4_C_T, 4, "C", "T");
        assertVariant(VAR_5_A_T, 5, "A", "T");
        assertVariant(VAR_5_G_GTTTGAC, 5, "G", "GTTTGAC");
        assertVariant(VAR_6_A_T, 6, "A", "T");
        assertVariant(VAR_7_CT_C, 7, "CT", "C");
        assertVariant(VAR_9_T_A, 9, "T", "A");
        assertVariant(MNV_1_AA_ATCATC, 1, "AA", "ATCATC");
        assertVariant(MNV_1_AGTCC_ATCGT, 1, "AGTCC", "ATCGT");
        assertVariant(MNV_2_ATCC_ATCT, 2, "ATCC", "ATCT");
        assertVariant(MNV_2_ATCCC_ATCTT, 2, "ATCCC", "ATCTT");
        assertVariant(MNV_2_AGC_ATCGT, 2, "AGC", "ATCGT");
        assertVariant(MNV_3_TCCC_TT, 3, "TCCC", "TT");
        assertVariant(MNV_3_TCCA_TT, 3, "TCCA", "TT");
        assertVariant(MNV_4_CA_TT, 4, "CA", "TT");
        assertVariant(MNV_4_CGA_TGT, 4, "CGA", "TGT");
        assertVariant(MNV_4_CAA_TTT, 4, "CAA", "TTT");
        assertVariant(MNV_7_CTT_CA, 7, "CTT", "CA");
    }

    private static void assertVariant(@NotNull final VariantContext variant, final int position, @NotNull final String ref,
            @NotNull final String alt) {
        assert variant.getStart() == position;
        assert variant.getReference().getBaseString().equals(ref);
        assert variant.getAlternateAllele(0).getBaseString().equals(alt);
    }
}
