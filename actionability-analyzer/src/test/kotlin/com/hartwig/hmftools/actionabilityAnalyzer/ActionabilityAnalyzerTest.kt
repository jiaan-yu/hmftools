package com.hartwig.hmftools.actionabilityAnalyzer

import com.google.common.io.Resources
import com.hartwig.hmftools.patientdb.data.ImmutablePotentialActionableVariant
import io.kotlintest.matchers.shouldBe
import io.kotlintest.specs.StringSpec

class ActionabilityAnalyzerTest : StringSpec() {
    private val ON_LABEL = "On-label"

    private val actionableCNVs = Resources.getResource("actionableCNVs").path
    private val actionableFusionPairs = Resources.getResource("actionableFusionPairs").path
    private val actionablePromiscuousFive = Resources.getResource("actionablePromiscuousFive").path
    private val actionablePromiscuousThree = Resources.getResource("actionablePromiscuousThree").path
    private val actionableVariants = Resources.getResource("actionableVariants").path
    private val cancerTypesMapping = Resources.getResource("knowledgebaseCancerTypes").path
    private val actionabilityAnalyzer = ActionabilityAnalyzer(actionableVariants, actionableFusionPairs, actionablePromiscuousFive,
                                                              actionablePromiscuousThree, actionableCNVs, cancerTypesMapping)

    private val brafSNV = ImmutablePotentialActionableVariant.of("CPCT99110022", "Skin", "BRAF", "7", 140453136, "A", "T")

    init {
        "finds BRAF SNV actionability" {
            val actionability = actionabilityAnalyzer.actionabilityForVariant(brafSNV)
            val drugs = drugs(actionability)
            val sources = sources(actionability)
            val events = actionability.map { it.actionableTreatment.event }.toSet()
            actionability.size shouldBe 6
            actionability.filter { it.treatmentType == ON_LABEL }.size shouldBe 2
            events.size shouldBe 1
            events.first() shouldBe "${brafSNV.gene()} ${brafSNV.chromosome()}:${brafSNV.position()} ${brafSNV.ref()}->${brafSNV.alt()}"
            (drugs == setOf("Dabrafenib", "Vemurafenib")) shouldBe true
            (sources == setOf("civic", "cgi")) shouldBe true
        }

        "finding variant does not depend on gene name" {
            val variant = brafSNV.withGene("BRAF2")
            val actionability = actionabilityAnalyzer.actionabilityForVariant(variant)
            val drugs = drugs(actionability)
            val sources = sources(actionability)
            val events = actionability.map { it.actionableTreatment.event }.toSet()
            actionability.size shouldBe 6
            actionability.filter { it.treatmentType == ON_LABEL }.size shouldBe 2
            events.size shouldBe 1
            events.first() shouldBe "${variant.gene()} ${variant.chromosome()}:${variant.position()} ${variant.ref()}->${variant.alt()}"
            (drugs == setOf("Dabrafenib", "Vemurafenib")) shouldBe true
            (sources == setOf("civic", "cgi")) shouldBe true
        }

        "finds BRAF SNV actionability for different primary tumor" {
            val variant = brafSNV.withPrimaryTumorLocation("Lung")
            val actionability = actionabilityAnalyzer.actionabilityForVariant(variant)
            val drugs = drugs(actionability)
            val sources = sources(actionability)
            val events = actionability.map { it.actionableTreatment.event }.toSet()
            actionability.size shouldBe 6
            actionability.filter { it.treatmentType == ON_LABEL }.size shouldBe 3
            events.size shouldBe 1
            events.first() shouldBe "${variant.gene()} ${variant.chromosome()}:${variant.position()} ${variant.ref()}->${variant.alt()}"
            (drugs == setOf("Dabrafenib", "Vemurafenib")) shouldBe true
            (sources == setOf("oncoKb", "cgi")) shouldBe true
        }

        "does not find SNV with different chromosome" {
            actionabilityAnalyzer.actionabilityForVariant(brafSNV.withChromosome("8")).size shouldBe 0
        }

        "does not find SNV with different position" {
            actionabilityAnalyzer.actionabilityForVariant(brafSNV.withPosition(140453137)).size shouldBe 0
        }

        "does not find SNV with different ref" {
            actionabilityAnalyzer.actionabilityForVariant(brafSNV.withRef("C")).size shouldBe 0
        }

        "does not find SNV with different alt" {
            actionabilityAnalyzer.actionabilityForVariant(brafSNV.withAlt("G")).size shouldBe 0
        }
    }

    private fun drugs(actionability: Set<ActionabilityOutput>): Set<String> {
        return actionability.filter { it.treatmentType == ON_LABEL }.map { it.actionableTreatment.actionability.drug }.toSet()
    }

    private fun sources(actionability: Set<ActionabilityOutput>): Set<String> {
        return actionability.filter { it.treatmentType == ON_LABEL }.map { it.actionableTreatment.actionability.source }.toSet()
    }
}