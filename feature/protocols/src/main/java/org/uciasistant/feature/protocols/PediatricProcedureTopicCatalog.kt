package org.uciasistant.feature.protocols

import org.uciasistant.core.clinicaldata.ClinicalPopulation

/** Normalized procedure-topic labels from verified publisher catalogues, not instructions. */
internal data class PediatricProcedureTopic(
    val id: String,
    val title: String,
    val population: ClinicalPopulation,
    val sourceReferenceIds: List<String>,
)

internal object PediatricProcedureTopicCatalog {
    private const val URGENCIAS_SOURCE_ID = "urgencias-pediatricas-2024"
    private const val WHO_ETAT_SOURCE_ID = "who-etat-2016"

    val topics = listOf(
        PediatricProcedureTopic(
            id = "peripheral-venous-access",
            title = "Acceso venoso periférico",
            population = ClinicalPopulation.PEDIATRIC,
            sourceReferenceIds = listOf(URGENCIAS_SOURCE_ID),
        ),
        PediatricProcedureTopic(
            id = "intraosseous-access",
            title = "Punción intraósea",
            population = ClinicalPopulation.PEDIATRIC,
            sourceReferenceIds = listOf(URGENCIAS_SOURCE_ID),
        ),
        PediatricProcedureTopic(
            id = "lumbar-puncture",
            title = "Punción lumbar",
            population = ClinicalPopulation.PEDIATRIC,
            sourceReferenceIds = listOf(URGENCIAS_SOURCE_ID),
        ),
        PediatricProcedureTopic(
            id = "rapid-sequence-intubation",
            title = "Intubación de secuencia rápida",
            population = ClinicalPopulation.PEDIATRIC,
            sourceReferenceIds = listOf(URGENCIAS_SOURCE_ID),
        ),
        PediatricProcedureTopic(
            id = "pediatric-transfer-stabilization",
            title = "Organización del traslado y estabilización prehospitalaria del paciente pediátrico",
            population = ClinicalPopulation.PEDIATRIC,
            sourceReferenceIds = listOf(URGENCIAS_SOURCE_ID),
        ),
        PediatricProcedureTopic(
            id = "bag-mask-ventilation",
            title = "Ventilación con bolsa y mascarilla",
            population = ClinicalPopulation.PEDIATRIC,
            sourceReferenceIds = listOf(URGENCIAS_SOURCE_ID),
        ),
        PediatricProcedureTopic(
            id = "triage",
            title = "Triage",
            population = ClinicalPopulation.PEDIATRIC,
            sourceReferenceIds = listOf(URGENCIAS_SOURCE_ID, WHO_ETAT_SOURCE_ID),
        ),
        PediatricProcedureTopic(
            id = "critical-intrahospital-transport",
            title = "Transporte intrahospitalario del paciente crítico",
            population = ClinicalPopulation.PEDIATRIC,
            sourceReferenceIds = listOf(URGENCIAS_SOURCE_ID),
        ),
    )
}
