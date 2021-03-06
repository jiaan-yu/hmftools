package com.hartwig.hmftools.knowledgebaseimporter.transvar.matchers

import com.hartwig.hmftools.knowledgebaseimporter.knowledgebases.events.HgvsVariantType

// Rules based on:
// - https://github.com/zwdzwd/transvar/blob/v2.4.0.20180701/transvar/mutation.py
// - http://varnomen.hgvs.org/recommendations/protein/
object TransvarProteinMatcher : Matcher {
    private const val PREFIX = "(p\\.)?"
    private val AA = "(${AminoAcidSymbols.pattern})"
    private val AA_POSITION = "$AA(\\d+)"
    private val AA_RANGE = "${AA_POSITION}_$AA_POSITION"
    private val SUBSTITUTION = "$AA_POSITION$AA"
    private val DELETION_SUFFIX = "del($AA+)?"
    private val DELETION = "$AA_POSITION$DELETION_SUFFIX"
    private val DELETION_RANGE = "(\\d+)_(\\d+)$DELETION_SUFFIX"
    private val DELETION_AA_RANGE = "$AA_RANGE$DELETION_SUFFIX"
    private val DUPLICATION_SUFFIX = "dup($AA+)?"
    private val DUPLICATION = "$AA_POSITION$DUPLICATION_SUFFIX"
    private val DUPLICATION_RANGE = "$AA_RANGE$DUPLICATION_SUFFIX"
    private val INSERTION = "${AA_RANGE}ins($AA)+"
    private val DELINS = "${AA_POSITION}delins($AA)+"
    private val DELINS_RANGE = "${AA_RANGE}delins($AA)+"
    private const val FRAMESHIFT_PATTERN = "fs((\\*|Ter)(\\d+|\\?))?"
    private val FRAMESHIFT = "$AA_POSITION$FRAMESHIFT_PATTERN"
    private val FRAMESHIFT_WITH_ALT = "$AA_POSITION$AA$FRAMESHIFT_PATTERN"

    override fun matches(string: String): Boolean {
        val input = string.trim()
        return matchesSubstitution(input) || matchesDeletion(input) || matchesDuplication(input) || matchesInsertion(input) ||
                matchesDelIns(input) || matchesFrameshift(input)
    }

    private fun String.matchesPattern(pattern: String): Boolean {
        return "$PREFIX$pattern".toRegex(RegexOption.IGNORE_CASE).matches(this)
    }

    private fun matchesSubstitution(input: String) = input.matchesPattern(SUBSTITUTION)
    private fun matchesDeletion(input: String) = input.matchesPattern(DELETION) || input.matchesPattern(DELETION_AA_RANGE) ||
            input.matchesPattern(DELETION_RANGE)
    private fun matchesDuplication(input: String) = input.matchesPattern(DUPLICATION) || input.matchesPattern(DUPLICATION_RANGE)
    private fun matchesInsertion(input: String) = input.matchesPattern(INSERTION)
    private fun matchesDelIns(input: String) = input.matchesPattern(DELINS) || input.matchesPattern(DELINS_RANGE)
    private fun matchesFrameshift(input: String) = input.matchesPattern(FRAMESHIFT) || input.matchesPattern(FRAMESHIFT_WITH_ALT)

    fun type(input: String) = when {
        matchesSubstitution(input) -> HgvsVariantType.SUBSTITUTION
        matchesDeletion(input)     -> HgvsVariantType.DELETION
        matchesDuplication(input)  -> HgvsVariantType.DUPLICATION
        matchesInsertion(input)    -> HgvsVariantType.INSERTION
        matchesDelIns(input)       -> HgvsVariantType.DELINS
        matchesFrameshift(input)   -> HgvsVariantType.FRAMESHIFT
        else                       -> HgvsVariantType.OTHER
    }
}
