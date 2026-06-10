package com.bandlab.intellij.plugin.strings

import com.google.common.truth.Truth.assertThat
import com.intellij.codeInsight.intention.HighPriorityAction
import com.intellij.codeInsight.intention.PriorityAction
import org.junit.Test

/**
 * Guards the ⌥⏎ ordering of the Localizer intentions. Update must sit above Delete; giving both the
 * same priority ties and tie-breaks alphabetically — putting "Delete" first (the regression this
 * catches). Update is `PriorityAction.TOP`, Delete is `HighPriorityAction` (HIGH); TOP sorts above
 * HIGH, so both still lead the list in the right order.
 */
class IntentionPriorityTest {

    @Test
    fun updateIsTopPriority() {
        assertThat((UpdateStringIntention() as PriorityAction).priority)
            .isEqualTo(PriorityAction.Priority.TOP)
    }

    @Test
    fun deleteIsHighPriorityNotTop() {
        // HighPriorityAction (not PriorityAction.TOP) keeps Delete just below Update. The bug had
        // Delete as PriorityAction.TOP and not HighPriorityAction, which this assertion fails on.
        assertThat(DeleteStringIntention()).isInstanceOf(HighPriorityAction::class.java)
    }
}
