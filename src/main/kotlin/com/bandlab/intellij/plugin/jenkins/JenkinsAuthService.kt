package com.bandlab.intellij.plugin.jenkins

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

/**
 * Holds the Jenkins credentials for the (single, fixed) UI-test job.
 *
 * Jenkins' Google SSO only guards the web UI — the REST API authenticates with a personal **API
 * token** over HTTP Basic, independent of the security realm. So the plugin never runs an OAuth
 * flow: the user generates an API token once (in their browser, already logged in via Google) and
 * pastes it into the Connect dialog.
 *
 * The Jenkins instance and the job are constants ([BASE_URL] / [JOB_PATH]) — there is exactly one
 * UI-test job, so the user never types them. Only the auth **username** is persisted (as a plain IDE
 * setting); the **token lives in [PasswordSafe]** (the IDE's encrypted store), never in plain XML.
 *
 * App-level: the Jenkins instance is the same across projects, so one set of credentials is shared.
 */
@Service(Service.Level.APP)
@State(name = "BandLabJenkinsSettings", storages = [Storage("bandlab-jenkins.xml")])
class JenkinsAuthService : PersistentStateComponent<JenkinsAuthService.State> {

    /** The only non-secret value we persist: the Jenkins login used for Basic auth. */
    data class State(var username: String = "")

    private var state = State()

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state = state
    }

    var username: String
        get() = state.username
        set(value) { state.username = value.trim() }

    /** The stored API token, or null when none has been saved. */
    fun token(): String? =
        PasswordSafe.instance[credentialAttributes()]?.getPasswordAsString()?.takeIf { it.isNotEmpty() }

    /** True when a token is present — the gate for triggering a build without prompting to connect. */
    fun hasToken(): Boolean = token() != null

    /** Persists [token] in PasswordSafe under the current username. */
    fun saveToken(token: String) {
        PasswordSafe.instance[credentialAttributes()] = Credentials(state.username, token)
    }

    /** Forgets the stored token (e.g. after a 401 / on disconnect). */
    fun clearToken() {
        PasswordSafe.instance[credentialAttributes()] = null
    }

    /** A ready-to-use client config when authenticated (token + username present), else null. */
    fun config(): JenkinsClient.Config? {
        val token = token() ?: return null
        if (state.username.isBlank()) return null
        return JenkinsClient.Config(
            baseUrl = BASE_URL,
            jobName = JOB_PATH,
            username = state.username,
            apiToken = token,
        )
    }

    private fun credentialAttributes(): CredentialAttributes =
        CredentialAttributes(generateServiceName(SUBSYSTEM, state.username))

    companion object {
        /** The one Jenkins instance the plugin talks to. */
        const val BASE_URL = "https://android-ci.bandlab.io/"

        /** The single UI-test job (used as `/job/<JOB_PATH>/buildWithParameters`). */
        const val JOB_PATH = "run_ui_tests"

        private const val SUBSYSTEM = "BandLab Jenkins"
    }
}
