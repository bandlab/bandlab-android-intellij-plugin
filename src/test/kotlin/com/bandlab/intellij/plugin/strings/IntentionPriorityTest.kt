package com.bandlab.intellij.plugin.strings

import com.google.common.truth.Truth.assertThat
import com.intellij.codeInsight.intention.PriorityAction
import org.junit.Test

/**
 * Both Localizer intentions are `PriorityAction.TOP` so they group together at the top of the ⌥⏎
 * list rather than being split apart by other TOP-priority actions (e.g. Android's "Open editor").
 * IntelliJ tie-breaks equal priority alphabetically, so the visible order is Delete then Update —
 * an accepted trade for keeping them adjacent. This guards both staying TOP.
 */
class IntentionPriorityTest {

    @Test
    fun updateIsTopPriority() {
        assertThat((UpdateStringIntention() as PriorityAction).priority)
            .isEqualTo(PriorityAction.Priority.TOP)
    }

    @Test
    fun deleteIsTopPriority() {
        assertThat((DeleteStringIntention() as PriorityAction).priority)
            .isEqualTo(PriorityAction.Priority.TOP)
    }
}
