// Copyright 2026 BandLab Singapore Pte Ltd
// SPDX-License-Identifier: Apache-2.0
package com.bandlab.intellij.plugin.jenkins

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.*

/**
 * Minimal Jenkins client that triggers a parameterized build.
 *
 * It POSTs to `<baseUrl>/job/<job>/buildWithParameters` with the parameters form-encoded in the
 * body, authenticating via HTTP Basic (`username` + Jenkins API token). No external HTTP dependency
 * — the plugin ships no HTTP client, so we use the JDK's [HttpURLConnection].
 *
 * No CSRF crumb is requested: Jenkins exempts API-token-authenticated requests from crumb checks
 * (same behavior the jenkins-rest library relies on for its `apiToken` auth path).
 *
 * This is intentionally synchronous and side-effect free beyond the network call; callers should
 * run it off the EDT (e.g. inside a background task) and handle the thrown exception.
 */
object JenkinsClient {

    /** Connection details + credentials for the Jenkins instance. */
    data class Config(
        val baseUrl: String,
        val username: String,
        val apiToken: String,
    )

    /**
     * Triggers the fixed UI-test job ([JenkinsAuthService.UI_TESTS_PATH]) described by [config],
     * passing [parameters] (e.g. `targets`, `branch`, `testApi`) as build parameters. Completes
     * normally on success; throws [IOException] on a non-2xx status or a network error — callers
     * run this off the EDT and handle the exception.
     */
    @Throws(IOException::class)
    fun trigger(config: Config, parameters: Map<String, String>) {
        val base = config.baseUrl.trimEnd('/')
        val url =
            URI(
                    "$base/job/${encodePathSegment(JenkinsAuthService.UI_TESTS_PATH)}/buildWithParameters"
                )
                .toURL()
        val body = parameters.entries.joinToString("&") { (k, v) -> "${encode(k)}=${encode(v)}" }

        val connection =
            (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 15_000
                readTimeout = 15_000
                setRequestProperty("Authorization", basicAuth(config.username, config.apiToken))
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            }

        try {
            connection.outputStream.use { it.write(body.toByteArray(StandardCharsets.UTF_8)) }
            val status = connection.responseCode
            if (status !in 200..299) {
                val error =
                    connection.errorStream?.readBytes()?.toString(StandardCharsets.UTF_8).orEmpty()
                throw IOException("Jenkins returned HTTP $status :$error")
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun basicAuth(user: String, token: String): String {
        val encoded =
            Base64.getEncoder().encodeToString("$user:$token".toByteArray(StandardCharsets.UTF_8))
        return "Basic $encoded"
    }

    private fun encode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8)

    private fun encodePathSegment(value: String): String = encode(value).replace("+", "%20")
}
