package org.uciasistant.feature.handoff

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HandoffDraftTest {
    @Test
    fun emptyDraftHasNoContent() {
        assertFalse(HandoffDraft().hasContent)
    }

    @Test
    fun nonBlankFieldMarksDraftAsContainingText() {
        assertTrue(HandoffDraft(activeProblems = "Texto de ejemplo").hasContent)
    }

    @Test
    fun whitespaceAloneDoesNotMarkDraftAsContainingText() {
        assertFalse(HandoffDraft(episodeReference = "  \n  ").hasContent)
    }

    @Test
    fun fixedStorageFieldOrderRoundTripsAllValues() {
        val draft = HandoffDraft(
            episodeReference = "CASO-FICTICIO-01",
            originInstitutionService = "Origen",
            destinationService = "Destino",
            transferType = "Traslado",
            transferReason = "Motivo de prueba",
            localPriority = "Sin clasificación",
            localCriteriaReference = "REF-TEST-v1",
            referralDateTime = "2026-11-02 10:15",
            departureDateTime = "2026-11-02 10:30",
            arrivalDateTime = "2026-11-02 11:00",
            handoverDateTime = "2026-11-02 11:10",
            sendingProfessional = "Profesional A",
            receivingProfessional = "Profesional B",
            activeProblems = "Texto con acentos: niño",
            allergies = "No especificado",
            interventionsInProgress = "Intervención de prueba",
            devices = "Dispositivo de prueba",
            pendingResults = "Resultado pendiente",
            outstandingTasks = "Tarea pendiente",
        )

        assertEquals(draft, HandoffDraft.fromStorageFields(draft.fieldsInStorageOrder()))
    }
}
