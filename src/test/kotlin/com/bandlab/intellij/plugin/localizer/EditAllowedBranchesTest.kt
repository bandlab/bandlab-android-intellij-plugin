package com.bandlab.intellij.plugin.localizer

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class EditAllowedBranchesTest {

    private fun fresh() = EditAllowedBranches()

    @Test
    fun newInstanceAllowsNothing() {
        val service = fresh()
        assertThat(service.isAllowed("main")).isFalse()
        assertThat(service.isAllowed("feature/foo")).isFalse()
    }

    @Test
    fun allowedBranchIsRecognized() {
        val service = fresh()
        service.allow("feature/foo")
        assertThat(service.isAllowed("feature/foo")).isTrue()
    }

    @Test
    fun nonAllowedBranchIsNotRecognized() {
        val service = fresh()
        service.allow("feature/foo")
        assertThat(service.isAllowed("feature/bar")).isFalse()
    }

    @Test
    fun multipleAllowedBranches() {
        val service = fresh()
        service.allow("main")
        service.allow("feature/a")
        service.allow("feature/b")
        assertThat(service.isAllowed("main")).isTrue()
        assertThat(service.isAllowed("feature/a")).isTrue()
        assertThat(service.isAllowed("feature/b")).isTrue()
        assertThat(service.isAllowed("feature/c")).isFalse()
    }

    @Test
    fun allowIsDeduplicated() {
        val service = fresh()
        service.allow("main")
        service.allow("main")
        assertThat(service.getState().branches).containsExactly("main")
    }

    @Test
    fun stateSurvivesRoundTrip() {
        val original = fresh()
        original.allow("feature/x")
        original.allow("fix/y")

        // Simulate serialization round-trip: extract state, load into a new instance.
        val restored = fresh()
        restored.loadState(original.getState())

        assertThat(restored.isAllowed("feature/x")).isTrue()
        assertThat(restored.isAllowed("fix/y")).isTrue()
        assertThat(restored.isAllowed("other")).isFalse()
    }
}
