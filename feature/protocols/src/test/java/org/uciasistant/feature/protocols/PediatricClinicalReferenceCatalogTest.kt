package org.uciasistant.feature.protocols

import org.uciasistant.core.clinicaldata.ClinicalPopulation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PediatricClinicalReferenceCatalogTest {
    @Test
    fun catalogContainsIdentifiableReferences() {
        val references = PediatricClinicalReferenceCatalog.references

        assertEquals(9, references.size)
        assertEquals(references.size, references.map(ClinicalReference::id).toSet().size)
        references.forEach { reference ->
            assertTrue(reference.id.isNotBlank())
            assertTrue(reference.domain.isNotBlank())
            assertEquals(ClinicalPopulation.PEDIATRIC, reference.population)
            assertTrue(reference.title.isNotBlank())
            assertTrue(reference.citation.isNotBlank())
            assertTrue(reference.identifierLabel in setOf("DOI", "ISBN"))
            assertTrue(reference.identifier.isNotBlank())
            if (reference.identifierLabel == "DOI") {
                assertTrue(reference.identifier.startsWith("10."))
            } else {
                assertTrue(reference.identifier.startsWith("978"))
            }
        }
    }

    @Test
    fun procedureTopicsTraceToPopulationScopedCandidateReferences() {
        val references = PediatricClinicalReferenceCatalog.references.associateBy(ClinicalReference::id)
        val topics = PediatricProcedureTopicCatalog.topics

        assertEquals(8, topics.size)
        assertEquals(topics.size, topics.map(PediatricProcedureTopic::id).toSet().size)
        topics.forEach { topic ->
            assertTrue(topic.title.isNotBlank())
            assertEquals(ClinicalPopulation.PEDIATRIC, topic.population)
            assertTrue(topic.sourceReferenceIds.isNotEmpty())
            topic.sourceReferenceIds.forEach { sourceId ->
                val source = references[sourceId]
                assertEquals("ISBN", source?.identifierLabel)
                assertTrue(source?.identifier?.startsWith("978") == true)
            }
        }

        val triage = topics.single { it.id == "triage" }
        assertTrue(triage.sourceReferenceIds.contains("urgencias-pediatricas-2024"))
        assertTrue(triage.sourceReferenceIds.contains("who-etat-2016"))
    }

    @Test
    fun whoEtatReferenceExposesItsReuseRestriction() {
        val reference = PediatricClinicalReferenceCatalog.references.single { it.id == "who-etat-2016" }

        assertEquals("ISBN", reference.identifierLabel)
        assertEquals("9789241510219", reference.identifier)
        assertTrue(reference.licenseNote.orEmpty().contains("CC BY-NC-SA 3.0 IGO"))
        assertTrue(reference.licenseNote.orEmpty().contains("no comercial"))
        assertTrue(reference.licenseNote.orEmpty().contains("uso comercial requiere permiso"))
        assertEquals(
            ReferenceRightsReviewStatus.LICENSE_IDENTIFIED_COMPATIBILITY_PENDING,
            reference.rightsReviewStatus,
        )
        assertTrue(
            PediatricClinicalReferenceCatalog.references
                .filterNot { it.id == reference.id }
                .all { it.rightsReviewStatus == ReferenceRightsReviewStatus.NOT_ASSESSED },
        )
    }
}
