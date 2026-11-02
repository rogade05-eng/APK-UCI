package org.uciasistant.core.clinicaldata

/** Version and governance metadata only; clinical content and decision logic are stored separately. */
data class ClinicalContentManifest(
    val id: String,
    val title: String,
    val kind: ClinicalContentKind,
    val population: ClinicalPopulation,
    val jurisdiction: String,
    val institutionId: String,
    val contentVersion: String,
    val sourceReferenceIds: List<String>,
    val licenseStatus: ContentLicenseStatus,
    val status: ClinicalContentStatus,
    val ownerId: String?,
    val reviewerId: String?,
    val approverId: String?,
    val approvedAtMillis: Long?,
    val effectiveAtMillis: Long?,
    val reviewDueAtMillis: Long?,
) {
    /** Returns blockers for activating this exact version in the requested population/site. */
    fun activationIssues(context: ContentActivationContext): List<ContentActivationIssue> = buildList {
        if (status != ClinicalContentStatus.ACTIVE) add(ContentActivationIssue.STATUS_NOT_ACTIVE)
        if (id.isBlank()) add(ContentActivationIssue.MISSING_ID)
        if (title.isBlank()) add(ContentActivationIssue.MISSING_TITLE)
        if (contentVersion.isBlank()) add(ContentActivationIssue.MISSING_VERSION)
        if (sourceReferenceIds.isEmpty() || sourceReferenceIds.any(String::isBlank) ||
            sourceReferenceIds.distinct().size != sourceReferenceIds.size
        ) {
            add(ContentActivationIssue.INVALID_SOURCE_REFERENCES)
        }
        if (licenseStatus != ContentLicenseStatus.CLEARED_FOR_DISTRIBUTION) {
            add(ContentActivationIssue.LICENSE_NOT_CLEARED)
        }
        if (population != context.population) add(ContentActivationIssue.POPULATION_MISMATCH)
        if (jurisdiction.isBlank()) {
            add(ContentActivationIssue.MISSING_JURISDICTION)
        } else if (jurisdiction != context.jurisdiction) {
            add(ContentActivationIssue.JURISDICTION_MISMATCH)
        }
        if (institutionId.isBlank()) {
            add(ContentActivationIssue.MISSING_INSTITUTION)
        } else if (institutionId != context.institutionId) {
            add(ContentActivationIssue.INSTITUTION_MISMATCH)
        }
        if (ownerId.isNullOrBlank()) add(ContentActivationIssue.MISSING_OWNER)
        if (reviewerId.isNullOrBlank()) add(ContentActivationIssue.MISSING_REVIEWER)
        if (approverId.isNullOrBlank()) add(ContentActivationIssue.MISSING_APPROVER)
        if (!ownerId.isNullOrBlank() && ownerId == reviewerId) {
            add(ContentActivationIssue.REVIEWER_NOT_INDEPENDENT)
        }

        val approvedAt = approvedAtMillis
        if (approvedAt == null) {
            add(ContentActivationIssue.MISSING_APPROVAL_TIME)
        } else if (approvedAt <= 0L || approvedAt > context.nowMillis) {
            add(ContentActivationIssue.INVALID_APPROVAL_TIME)
        }

        val effectiveAt = effectiveAtMillis
        if (effectiveAt == null) {
            add(ContentActivationIssue.MISSING_EFFECTIVE_TIME)
        } else {
            if (effectiveAt <= 0L) add(ContentActivationIssue.INVALID_EFFECTIVE_TIME)
            if (effectiveAt > context.nowMillis) add(ContentActivationIssue.NOT_YET_EFFECTIVE)
            if (approvedAt != null && effectiveAt < approvedAt) {
                add(ContentActivationIssue.EFFECTIVE_BEFORE_APPROVAL)
            }
        }

        val reviewDueAt = reviewDueAtMillis
        if (reviewDueAt == null) {
            add(ContentActivationIssue.MISSING_REVIEW_DUE_TIME)
        } else {
            if (reviewDueAt <= 0L) add(ContentActivationIssue.INVALID_REVIEW_DUE_TIME)
            if (reviewDueAt <= context.nowMillis) add(ContentActivationIssue.REVIEW_OVERDUE)
            if (effectiveAt != null && reviewDueAt <= effectiveAt) {
                add(ContentActivationIssue.REVIEW_DUE_NOT_AFTER_EFFECTIVE)
            }
        }
    }
}

data class ContentActivationContext(
    val population: ClinicalPopulation,
    val jurisdiction: String,
    val institutionId: String,
    val nowMillis: Long,
)

enum class ClinicalContentKind {
    PROTOCOL,
    SCALE,
    ALGORITHM,
    INDICATOR,
}

enum class ClinicalContentStatus {
    DRAFT,
    IN_REVIEW,
    APPROVED,
    ACTIVE,
    SUSPENDED,
    SUPERSEDED,
}

enum class ContentLicenseStatus {
    NOT_ASSESSED,
    REVIEW_REQUIRED,
    CLEARED_FOR_DISTRIBUTION,
    RESTRICTED,
}

enum class ContentActivationIssue {
    STATUS_NOT_ACTIVE,
    MISSING_ID,
    MISSING_TITLE,
    MISSING_VERSION,
    INVALID_SOURCE_REFERENCES,
    LICENSE_NOT_CLEARED,
    POPULATION_MISMATCH,
    MISSING_JURISDICTION,
    JURISDICTION_MISMATCH,
    MISSING_INSTITUTION,
    INSTITUTION_MISMATCH,
    MISSING_OWNER,
    MISSING_REVIEWER,
    MISSING_APPROVER,
    REVIEWER_NOT_INDEPENDENT,
    MISSING_APPROVAL_TIME,
    INVALID_APPROVAL_TIME,
    MISSING_EFFECTIVE_TIME,
    INVALID_EFFECTIVE_TIME,
    NOT_YET_EFFECTIVE,
    EFFECTIVE_BEFORE_APPROVAL,
    MISSING_REVIEW_DUE_TIME,
    INVALID_REVIEW_DUE_TIME,
    REVIEW_OVERDUE,
    REVIEW_DUE_NOT_AFTER_EFFECTIVE,
}
