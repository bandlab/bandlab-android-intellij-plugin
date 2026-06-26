package com.bandlab.intellij.plugin.jenkins

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.components.Service

/**
 * Holds the Jenkins credentials for the (single, fixed) UI-test job.
 *
 * Jenkins' Google SSO only guards the web UI — the REST API authenticates with a personal **API
 * token** over HTTP Basic, independent of the security realm. So the plugin never runs an OAuth flow:
 * the user generates an API token once (in their browser, already logged in via Google) and pastes it
 * into the Connect dialog.
 *
 * The instance and job are constants ([BASE_URL] / [UI_TESTS_PATH]). The username and token are stored
 * together as a single [Credentials] entry in [PasswordSafe] (the IDE's encrypted store) — nothing is
 * persisted in plain settings, so there is no mutable state component to maintain.
 *
 * App-level: the Jenkins instance is the same across projects, so one set of credentials is shared.
 */
@Service(Service.Level.APP)
class JenkinsAuthService {

    /** The saved Jenkins username, or null when not connected yet. */
    fun username(): String? = credentials()?.userName?.takeIf { it.isNotBlank() }

    /** The saved API token, or null when not connected yet. */
    fun token(): String? = credentials()?.getPasswordAsString()?.takeIf { it.isNotBlank() }

    /** True when a token is present — the gate for triggering a build without prompting to connect. */
    fun hasToken(): Boolean = token() != null

    /** Stores [username] + [token] in PasswordSafe (overwriting any previous entry). */
    fun save(username: String, token: String) {
        PasswordSafe.instance[CREDENTIAL_ATTRIBUTES] = Credentials(username, token)
    }

    /** Forgets the stored credentials (e.g. after a 401 / on disconnect). */
    fun clear() {
        PasswordSafe.instance[CREDENTIAL_ATTRIBUTES] = null
    }

    /** A ready-to-use client config when connected (username + token present), else null. */
    fun config(): JenkinsClient.Config? {
        val credentials = credentials() ?: return null
        val username = credentials.userName?.takeIf { it.isNotBlank() } ?: return null
        val token = credentials.getPasswordAsString()?.takeIf { it.isNotBlank() } ?: return null
        return JenkinsClient.Config(baseUrl = BASE_URL, username = username, apiToken = token)
    }

    private fun credentials(): Credentials? = PasswordSafe.instance[CREDENTIAL_ATTRIBUTES]

    companion object {
        const val BASE_URL = "https://android-ci.bandlab.io/"

        const val UI_TESTS_PATH = "run_ui_tests"

        const val JOBS_URL = "${BASE_URL}job/$UI_TESTS_PATH/"

        private val CREDENTIAL_ATTRIBUTES = CredentialAttributes(
            serviceName = generateServiceName(
                subsystem = "BandLab Jenkins",
                key = "credentials"
            )
        )
    }
}
