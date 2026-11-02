package org.uciasistant.feature.protocols

/** Normalized procedure-topic labels from a verified publisher catalogue, not instructions. */
internal data class PediatricProcedureTopic(
    val id: String,
    val title: String,
    val sourceReferenceId: String,
)

internal object PediatricProcedureTopicCatalog {
    private const val URGENCIAS_SOURCE_ID = "urgencias-pediatricas-2024"

    val topics = listOf(
        PediatricProcedureTopic(
            id = "peripheral-venous-access",
            title = "Acceso venoso periférico",
            sourceReferenceId = URGENCIAS_SOURCE_ID,
        ),
        PediatricProcedureTopic(
            id = "intraosseous-access",
            title = "Punción intraósea",
            sourceReferenceId = URGENCIAS_SOURCE_ID,
        ),
        PediatricProcedureTopic(
            id = "lumbar-puncture",
            title = "Punción lumbar",
            sourceReferenceId = URGENCIAS_SOURCE_ID,
        ),
        PediatricProcedureTopic(
            id = "rapid-sequence-intubation",
            title = "Intubación de secuencia rápida",
            sourceReferenceId = URGENCIAS_SOURCE_ID,
        ),
        PediatricProcedureTopic(
            id = "pediatric-transfer-stabilization",
            title = "Organización del traslado y estabilización prehospitalaria del paciente pediátrico",
            sourceReferenceId = URGENCIAS_SOURCE_ID,
        ),
        PediatricProcedureTopic(
            id = "bag-mask-ventilation",
            title = "Ventilación con bolsa y mascarilla",
            sourceReferenceId = URGENCIAS_SOURCE_ID,
        ),
        PediatricProcedureTopic(
            id = "triage",
            title = "Triage",
            sourceReferenceId = URGENCIAS_SOURCE_ID,
        ),
        PediatricProcedureTopic(
            id = "critical-intrahospital-transport",
            title = "Transporte intrahospitalario del paciente crítico",
            sourceReferenceId = URGENCIAS_SOURCE_ID,
        ),
    )
}
