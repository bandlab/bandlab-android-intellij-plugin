package com.bandlab.intellij.plugin.jenkins

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * "Connect to Jenkins" — collects the username and a personal API token and stores them via
 * [JenkinsAuthService] (token → PasswordSafe). The base URL and job are fixed constants, so they are
 * not asked for here.
 *
 * There is no Google login here on purpose: the REST API uses an API token, not the SSO session (see
 * [JenkinsAuthService]). The "Open token page" button takes the user to their Jenkins user page —
 * where they're already logged in via Google — to generate one.
 */
class JenkinsConnectDialog(private val project: Project) : DialogWrapper(project) {

    private val auth = service<JenkinsAuthService>()

    // The Jenkins login is always `<local-part>@bandlab.com`. The field edits only the local part;
    // the suffix is fixed and shown next to it.
    private val usernameField = JBTextField(defaultLocalPart())
    private val tokenField = JBPasswordField().apply { text = auth.token().orEmpty() }

    init {
        title = "Connect to Jenkins"
        setOKButtonText("Save")
        init()
    }

    override fun createCenterPanel(): JComponent {
        val openTokenPageButton = JButton("Open token page…").apply {
            addActionListener { BrowserUtil.browse(tokenPageUrl()) }
        }

        val usernameRow = JPanel(BorderLayout(4, 0)).apply {
            add(usernameField, BorderLayout.CENTER)
            add(JBLabel(USERNAME_SUFFIX), BorderLayout.EAST)
        }

        return FormBuilder.createFormBuilder()
            .addComponentToRightColumn(
                JBLabel(JenkinsAuthService.BASE_URL).apply {
                    foreground = JBUI.CurrentTheme.ContextHelp.FOREGROUND
                }
            )
            .addLabeledComponent("Username:", usernameRow)
            .addLabeledComponent("API token:", tokenField)
            .addComponentToRightColumn(
                JBLabel("Generate a token on your Jenkins user page, then paste it here.").apply {
                    foreground = JBUI.CurrentTheme.ContextHelp.FOREGROUND
                }
            )
            .addComponentToRightColumn(openTokenPageButton)
            .panel
            .apply { preferredSize = JBUI.size(480, 170) }
    }

    override fun doValidate(): ValidationInfo? {
        if (localPart().isBlank()) return ValidationInfo("Enter the username", usernameField)
        if (tokenField.password.isEmpty()) return ValidationInfo("Paste your API token", tokenField)
        return null
    }

    override fun doOKAction() {
        auth.save(username = fullUsername(), token = String(tokenField.password))
        super.doOKAction()
    }

    /** Jenkins user "Security" page is the modern home of API tokens; falls back to the user root. */
    private fun tokenPageUrl(): String {
        val base = JenkinsAuthService.BASE_URL.trimEnd('/')
        val fullUsername = fullUsername()
        return if (localPart().isBlank()) "$base/me/security/" else "$base/user/$fullUsername/security/"
    }

    /** Local part the user is editing (anything they type after a stray `@` is ignored). */
    private fun localPart(): String = usernameField.text.substringBefore('@').trim()

    /** The full Jenkins login: `<local-part>@bandlab.com`. */
    private fun fullUsername(): String = "${localPart()}$USERNAME_SUFFIX"

    /** Local part seeded from a saved username or git `user.email` (e.g. `artyom.tarassov`). */
    private fun defaultLocalPart(): String {
        val source = auth.username() ?: currentGitEmail(project).orEmpty()
        return source.substringBefore('@').trim()
    }

    private companion object {
        /** Every BandLab Jenkins login ends with this; the user only edits the local part. */
        const val USERNAME_SUFFIX = "@bandlab.com"
    }
}
