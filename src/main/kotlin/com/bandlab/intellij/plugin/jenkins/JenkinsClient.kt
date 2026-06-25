package com.bandlab.intellij.plugin.jenkins

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Base64

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
 * This is intentionally synchronous and side-effect free beyond the network call; callers should run
 * it off the EDT (e.g. inside a background task) and surface [Result] to the user.
 */
object JenkinsClient {

    /** Connection details + credentials for one Jenkins instance/job. */
    data class Config(
        val baseUrl: String,
        val jobName: String,
        val username: String,
        val apiToken: String,
    )

    /** Outcome of a trigger attempt: the HTTP status and the queue location (if Jenkins returned one). */
    data class Response(val statusCode: Int, val queueLocation: String?)

    /**
     * Triggers the build described by [config], passing [parameters] (e.g. `targets`, `branch`,
     * `testApi`) as build parameters. Returns a [Result] wrapping the [Response] on success or the
     * thrown exception on failure (network error, non-2xx status, etc.).
     */
    fun trigger(config: Config, parameters: Map<String, String>): Result<Response> =
        runCatching {
            val base = config.baseUrl.trimEnd('/')
            val url = URI("$base/job/${encodePathSegment(config.jobName)}/buildWithParameters").toURL()
            val body = parameters.entries.joinToString("&") { (k, v) -> "${encode(k)}=${encode(v)}" }

            val connection = (url.openConnection() as HttpURLConnection).apply {
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
                    val error = connection.errorStream?.readBytes()?.toString(StandardCharsets.UTF_8).orEmpty()
                    throw IOException("Jenkins returned HTTP $status${if (error.isBlank()) "" else ": ${error.take(500)}"}")
                }
                Response(statusCode = status, queueLocation = connection.getHeaderField("Location"))
            } finally {
                connection.disconnect()
            }
        }

    /** The job's web page, used as a fallback link when the build URL can't be resolved yet. */
    fun jobUrl(config: Config): String =
        "${config.baseUrl.trimEnd('/')}/job/${encodePathSegment(config.jobName)}/"

    private fun basicAuth(user: String, token: String): String {
        val encoded = Base64.getEncoder().encodeToString("$user:$token".toByteArray(StandardCharsets.UTF_8))
        return "Basic $encoded"
    }

    private fun encode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8)

    private fun encodePathSegment(value: String): String = encode(value).replace("+", "%20")
}
