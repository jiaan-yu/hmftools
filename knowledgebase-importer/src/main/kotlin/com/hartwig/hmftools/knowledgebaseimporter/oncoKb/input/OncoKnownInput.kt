package com.hartwig.hmftools.knowledgebaseimporter.oncoKb.input

import com.hartwig.hmftools.extensions.csv.CsvData
import com.hartwig.hmftools.knowledgebaseimporter.knowledgebases.CorrectedInput

data class OncoKnownInput(private val Isoform: String?, @get:JvmName("getGene_") private val Gene: String, val Alteration: String,
                          val `Mutation Effect`: String, val Oncogenicity: String) : CsvData, CorrectedInput<OncoKnownInput>,
        OncoKbInput {

    val reference = "$Gene $Alteration"
    override val transcript = Isoform
    override val gene = Gene
    override val variant = Alteration

    override fun correct(): OncoKnownInput {
        return when {
            Alteration.contains("IGH-NKX2") && Gene == "NKX2-1" -> copy(Alteration = Alteration.replace("IGH-NKX2", "IGH-NKX2-1"))
            Alteration.contains("ROS1-CD74")                    -> copy(Alteration = Alteration.replace("ROS1-CD74", "CD74-ROS1"))
            Alteration.contains("EP300-MLL")                    -> copy(Alteration = Alteration.replace("EP300-MLL", "MLL-EP300"))
            Alteration.contains("EP300-MOZ")                    -> copy(Alteration = Alteration.replace("EP300-MOZ", "MOZ-EP300"))
            Alteration.contains("RET-CCDC6")                    -> copy(Alteration = Alteration.replace("RET-CCDC6", "CCDC6-RET"))
            Alteration == "p61BRAF-V600E"                       -> copy(Alteration = "V600E/V600K")
            else                                                -> this
        }
    }
}
