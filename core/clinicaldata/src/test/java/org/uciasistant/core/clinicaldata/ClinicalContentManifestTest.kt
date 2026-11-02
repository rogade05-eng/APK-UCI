package org.uciasistant.core.clinicaldata

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClinicalContentManifestTest {
    private val context = ContentActivationContext(
        population = ClinicalPopulation.PEDIATRIC,
        jurisdiction = "CU",
        institutionId = "test-hospital",
        nowMillis = 30_000L,
    )

    private val activeManifest = ClinicalContentManifest(
        id = "test-protocol-001",
        title = "Synthetic protocol manifest",
        kind = ClinicalContentKind.PROTOCOL,
        population = ClinicalPopulation.PEDIATRIC,
        jurisdiction = "CU",
        institutionId = "test-hospital",
        contentVersion = "test-1",
        sourceReferenceIds = listOf("synthetic-source-001"),
        licenseStatus = ContentLicenseStatus.CLEARED_FOR_DISTRIBUTION,
        status = ClinicalContentStatus.ACTIVE,
        ownerId = "content-owner",
        reviewerId = "independent-reviewer",
        approverId = "content-approver",
        approvedAtMillis = 10_000L,
        effectiveAtMillis = 15_000L,
        reviewDueAtMillis = 40_000L,
    )

    @Test
    fun completeManifestMatchesPopulationAndSite() {
        assertTrue(activeManifest.activationIssues(context).isEmpty())
    }

    @Test
    fun draftContentAndUnclearedRightsCannotActivate() {
        val issues = activeManifest.copy(
            status = ClinicalContentStatus.DRAFT,
            licenseStatus = ContentLicenseStatus.REVIEW_REQUIRED,
        ).activationIssues(context)

        assertTrue(issues.contains(ContentActivationIssue.STATUS_NOT_ACTIVE))
        assertTrue(issues.contains(ContentActivationIssue.LICENSE_NOT_CLEARED))
    }

    @Test
    fun populationAndInstitutionMustMatchExactly() {
        val issues = activeManifest.activationIssues(
            context.copy(
                population = ClinicalPopulation.NEONATAL,
                institutionId = "other-hospital",
            ),
        )

        assertEquals(
            listOf(
                ContentActivationIssue.POPULATION_MISMATCH,
                ContentActivationIssue.INSTITUTION_MISMATCH,
            ),
            issues,
        )
    }

    @Test
    fun overdueReviewAndNonIndependentReviewerBlockActivation() {
        val issues = activeManifest.copy(
            reviewerId = "content-owner",
            reviewDueAtMillis = context.nowMillis,
        ).activationIssues(context)

        assertTrue(issues.contains(ContentActivationIssue.REVIEWER_NOT_INDEPENDENT))
        assertTrue(issues.contains(ContentActivationIssue.REVIEW_OVERDUE))
    }
}
