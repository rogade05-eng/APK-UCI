package org.uciasistant.feature.protocols

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PediatricClinicalReferenceCatalogTest {
    @Test
    fun catalogContainsIdentifiableReferences() {
        val references = PediatricClinicalReferenceCatalog.references

        assertTrue(references.isNotEmpty())
        assertEquals(references.size, references.map(ClinicalReference::id).toSet().size)
        references.forEach { reference ->
            assertTrue(reference.id.isNotBlank())
            assertTrue(reference.domain.isNotBlank())
            assertTrue(reference.title.isNotBlank())
            assertTrue(reference.citation.isNotBlank())
            assertTrue(reference.doi.startsWith("10."))
        }
    }
}
