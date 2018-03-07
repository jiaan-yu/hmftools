package com.hartwig.hmftools.portal.converter

import com.hartwig.hmftools.portal.converter.extensions.getOrNull
import org.apache.commons.csv.CSVRecord
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class SampleClinicalData(val cpctId: String?, val sampleId: String?, val gender: String, val ageAtEnrollment: String,
                              val cancerType: String, val specimenType: String, val specimenTypeOther: String) {
    companion object Factory {
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        operator fun invoke(record: CSVRecord): SampleClinicalData {
            val gender = determineGender(record.getOrNull(GENDER_FIELD))
            val ageAtEnrollment = determineAgeAtEnrollment(record.getOrNull(BIRTH_YEAR_FIELD), record.getOrNull(REGISTRATION_DATE_FIELD))
            val cancerType = determineCancerType(record.getOrNull(CANCER_TYPE_FIELD))
            val (specimenType, specimenTypeOther) = determineSpecimenType(record.getOrNull(BIOPSY_SITE_FIELD))
            return SampleClinicalData(record.getOrNull(CPCT_ID_FIELD),
                                      record.getOrNull(SAMPLE_ID_FIELD),
                                      gender,
                                      ageAtEnrollment,
                                      cancerType,
                                      specimenType,
                                      specimenTypeOther)
        }

        private fun determineGender(ecrfGender: String?): String {
            return when (ecrfGender?.trim()) {
                "male"   -> "1"
                "female" -> "2"
                else     -> DEFAULT_VALUE
            }
        }

        private fun determineCancerType(ecrfCancerType: String?): String {
            return if (ecrfCancerType == null || ecrfCancerType.isBlank()) "Missing" else ecrfCancerType
        }

        private fun determineSpecimenType(ecrfBiopsySite: String?): Pair<String, String> {
            return when (ecrfBiopsySite?.trim()?.toLowerCase()) {
                "", null -> Pair("Biopsy site: -", DEFAULT_VALUE)
                else     -> Pair("Biopsy site: " + ecrfBiopsySite, DEFAULT_VALUE)
            }
        }

        private fun determineAgeAtEnrollment(birthYearString: String?, registrationDateString: String?): String {
            return if (birthYearString == null || registrationDateString == null) {
                DEFAULT_VALUE
            } else {
                try {
                    val birthYear = birthYearString.toInt()
                    val registrationDate = LocalDate.parse(registrationDateString, DATE_FORMATTER)
                    (registrationDate.year - birthYear).toString()
                } catch (e: Exception) {
                    DEFAULT_VALUE
                }
            }
        }
    }
}
