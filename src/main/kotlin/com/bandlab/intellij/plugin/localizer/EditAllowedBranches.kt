package com.bandlab.intellij.plugin.localizer

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

/**
 * Persists the set of git branches on which the developer has explicitly chosen to allow direct
 * edits to localizer-managed string files. Stored in the workspace file so the preference is local
 * to this developer and not committed to VCS.
 *
 * When the current branch is in this set, [LocalizerEditWarningTypedHandler] silently permits edits
 * without prompting.
 */
@Service(Service.Level.PROJECT)
@State(
    name = "BandLabLocalizerEditAllowedBranches",
    storages = [Storage(StoragePathMacros.WORKSPACE_FILE)],
)
class EditAllowedBranches : PersistentStateComponent<EditAllowedBranches.State> {

    class State {
        // MutableList serializes reliably with the IntelliJ XML serializer; Set/LinkedHashSet can
        // lose elements on round-trip. The contains/add contract is preserved via list operations.
        var branches: MutableList<String> = mutableListOf()
    }

    private var state = State()

    override fun getState(): State = state

    override fun loadState(s: State) {
        state = s
    }

    /** True when edits on [branch] should be allowed without a warning dialog. */
    fun isAllowed(branch: String): Boolean = branch in state.branches

    /** Marks [branch] as allowed; subsequent keystrokes in managed files will pass through silently. */
    fun allow(branch: String) {
        if (branch !in state.branches) state.branches.add(branch)
    }

    companion object {
        fun getInstance(project: Project): EditAllowedBranches = project.service()
    }
}
