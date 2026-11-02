package org.uciasistant.feature.protocols

/** Bibliographic metadata only; these entries are not approved or executable protocols. */
internal data class ClinicalReference(
    val id: String,
    val domain: String,
    val title: String,
    val citation: String,
    val identifierLabel: String,
    val identifier: String,
    val licenseNote: String? = null,
)

internal object PediatricClinicalReferenceCatalog {
    val references: List<ClinicalReference> = listOf(
        ClinicalReference(
            id = "ssc-pediatric-2026",
            domain = "Sepsis pediátrica",
            title = "Surviving Sepsis Campaign International Guidelines for the Management of Sepsis and Septic Shock in Children 2026",
            citation = "Pediatric Critical Care Medicine, 2026",
            identifierLabel = "DOI",
            identifier = "10.1097/PCC.0000000000003927",
        ),
        ClinicalReference(
            id = "who-etat-2016",
            domain = "Urgencias pediátricas y triaje ETAT",
            title = "Paediatric emergency triage, assessment and treatment: care of critically-ill children",
            citation = "World Health Organization, Geneva, 2016; 74 p.",
            identifierLabel = "ISBN",
            identifier = "9789241510219",
            licenseNote = "CC BY-NC-SA 3.0 IGO; atribución, uso no comercial y compartir adaptaciones bajo términos compatibles; uso comercial requiere permiso.",
        ),
        ClinicalReference(
            id = "palicc-2-2023",
            domain = "SDRA pediátrico",
            title = "Executive Summary of the Second International Guidelines for the Diagnosis and Management of Pediatric Acute Respiratory Distress Syndrome (PALICC-2)",
            citation = "Pediatric Critical Care Medicine, 2023; 24(2):143–168",
            identifierLabel = "DOI",
            identifier = "10.1097/PCC.0000000000003147",
        ),
        ClinicalReference(
            id = "phoenix-consensus-2024",
            domain = "Definición de sepsis pediátrica",
            title = "International Consensus Criteria for Pediatric Sepsis and Septic Shock",
            citation = "JAMA, 2024",
            identifierLabel = "DOI",
            identifier = "10.1001/jama.2024.0179",
        ),
        ClinicalReference(
            id = "phoenix-validation-2024",
            domain = "Disfunción orgánica pediátrica",
            title = "Development and Validation of the Phoenix Criteria for Pediatric Sepsis and Septic Shock",
            citation = "JAMA, 2024",
            identifierLabel = "DOI",
            identifier = "10.1001/jama.2024.0196",
        ),
        ClinicalReference(
            id = "ispad-dka-2022",
            domain = "Cetoacidosis diabética pediátrica",
            title = "ISPAD Clinical Practice Consensus Guidelines 2022: Diabetic ketoacidosis and hyperglycemic hyperosmolar state",
            citation = "Pediatric Diabetes, 2022; 23(7):835–856",
            identifierLabel = "DOI",
            identifier = "10.1111/pedi.13406",
        ),
        ClinicalReference(
            id = "pandem-pediatric-2022",
            domain = "Dolor, agitación y delirium en UCI pediátrica",
            title = "2022 Society of Critical Care Medicine Clinical Practice Guidelines on Prevention and Management of Pain, Agitation, Neuromuscular Blockade, and Delirium in Critically Ill Pediatric Patients with Consideration of the ICU Environment and Early Mobility",
            citation = "Pediatric Critical Care Medicine, 2022; 23(2):e74–e110",
            identifierLabel = "DOI",
            identifier = "10.1097/PCC.0000000000002873",
        ),
        ClinicalReference(
            id = "pelod-2-2013",
            domain = "Disfunción multiorgánica pediátrica",
            title = "PELOD-2: An Update of the PEdiatric Logistic Organ Dysfunction Score",
            citation = "Critical Care Medicine, 2013; 41(7):1761–1773",
            identifierLabel = "DOI",
            identifier = "10.1097/CCM.0b013e31828a2bbd",
        ),
        ClinicalReference(
            id = "urgencias-pediatricas-2024",
            domain = "Urgencias y procedimientos pediátricos",
            title = "Urgencias Pediátricas: Guía de actuación, 3.ª edición",
            citation = "Benito Fernández J, Mintegi Raso S, eds. Editorial Médica Panamericana, 2024",
            identifierLabel = "ISBN",
            identifier = "9788411062725",
        ),
    )
}
