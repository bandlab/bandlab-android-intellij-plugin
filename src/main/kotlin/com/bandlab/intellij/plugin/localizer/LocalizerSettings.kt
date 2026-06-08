package com.bandlab.intellij.plugin.localizer

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service

/** Persisted plugin settings. Application-level so the preference is shared across projects. */
@Service(Service.Level.APP)
@State(name = "BandLabLocalizerSettings", storages = [Storage("bandlab-localizer.xml")])
class LocalizerSettings : PersistentStateComponent<LocalizerSettings.State> {

    class State {
        var warnOnEditingManagedFile: Boolean = true
    }

    private var state = State()

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state = state
    }

    var warnOnEditingManagedFile: Boolean
        get() = state.warnOnEditingManagedFile
        set(value) {
            state.warnOnEditingManagedFile = value
        }

    companion object {
        fun getInstance(): LocalizerSettings = service()
    }
}
