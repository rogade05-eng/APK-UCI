package org.uciasistant.feature.handoff

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
}
