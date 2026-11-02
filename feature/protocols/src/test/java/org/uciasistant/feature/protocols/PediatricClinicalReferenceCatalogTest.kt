package org.uciasistant.feature.protocols

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
    fun procedureTopicsTraceToTheEditorialReference() {
        val references = PediatricClinicalReferenceCatalog.references.associateBy(ClinicalReference::id)
        val topics = PediatricProcedureTopicCatalog.topics

        assertTrue(topics.isNotEmpty())
        assertEquals(topics.size, topics.map(PediatricProcedureTopic::id).toSet().size)
        topics.forEach { topic ->
            assertTrue(topic.title.isNotBlank())
            val source = references[topic.sourceReferenceId]
            assertEquals("ISBN", source?.identifierLabel)
            assertTrue(source?.identifier?.startsWith("978") == true)
        }
    }

    @Test
    fun whoEtatReferenceExposesItsReuseRestriction() {
        val reference = PediatricClinicalReferenceCatalog.references.single { it.id == "who-etat-2016" }

        assertEquals("ISBN", reference.identifierLabel)
        assertEquals("9789241510219", reference.identifier)
        assertTrue(reference.licenseNote.orEmpty().contains("CC BY-NC-SA 3.0 IGO"))
        assertTrue(reference.licenseNote.orEmpty().contains("no comercial"))
        assertTrue(reference.licenseNote.orEmpty().contains("uso comercial requiere permiso"))
    }
}
