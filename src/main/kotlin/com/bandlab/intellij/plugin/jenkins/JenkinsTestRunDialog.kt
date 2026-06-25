package com.bandlab.intellij.plugin.jenkins

import com.bandlab.intellij.plugin.localizer.currentGitBranch
import com.intellij.ide.BrowserUtil
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.CheckboxTree
import com.intellij.ui.CheckboxTreeBase.CheckPolicy
import com.intellij.ui.CheckboxTreeListener
import com.intellij.ui.CheckedTreeNode
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.tree.TreeUtil
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.datatransfer.StringSelection
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTree

/**
 * Dialog that lets the user pick tests from the open file — by whole class or by individual method —
 * via a checkbox tree, see the resulting Jenkins `targets` JSON live, copy it, or trigger a Jenkins
 * build directly.
 *
 * The tree mirrors the file's structure: each [TestClass] is a parent node, each [TestMethod] a leaf
 * under it. Checkbox propagation (check a class → checks its tests) is handled by [CheckboxTree].
 * Selection is collapsed to `class <fqName>` when all of a class's methods are checked (see
 * [JenkinsTargets]).
 */
class JenkinsTestRunDialog(
    private val project: Project,
    testClasses: List<TestClass>,
) : DialogWrapper(project) {

    // Accumulated selection across files/sessions. Pre-check this file's nodes from what's stored.
    private val store = project.service<JenkinsTargetsStore>()
    private val classFqns = testClasses.map { it.fqName }.toSet()

    private val rootNode = CheckedTreeNode("Tests").apply {
        val stored = store.all().toSet()
        isChecked = false
        testClasses.forEach { testClass ->
            val wholeClassSelected = "class ${testClass.fqName}" in stored
            val classNode = CheckedTreeNode(testClass).apply { isChecked = wholeClassSelected }
            testClass.methods.forEach { method ->
                val checked = wholeClassSelected || "class ${testClass.fqName}#${method.name}" in stored
                classNode.add(CheckedTreeNode(method).apply { isChecked = checked })
            }
            add(classNode)
        }
    }

    private val tree = CheckboxTree(
        TestTreeCellRenderer(),
        rootNode,
        checkPolicy = CheckPolicy.PROPAGATE_EVERYTHING_POLICY
    ).apply {
        isRootVisible = false
        showsRootHandles = true
        TreeUtil.expandAll(this)
    }

    private val jsonPreview = JBTextArea(16, 48).apply {
        isEditable = false
        lineWrap = false
        text = "[ ]"
    }

    // Build parameters. `user` defaults to the git config user (user.name), falling back to the job's
    // own default. `targets` comes from the tree above.
    private val userField = JBTextField(currentGitUser(project) ?: DEFAULT_USER)
    private val branchField = JBTextField(currentGitBranch(project).orEmpty())
    private val testApiCombo = ComboBox(arrayOf("prod", "stage"))

    // devices: "Default" omits the param (Jenkins applies the job's default); "Custom" sends the JSON.
    private val devicesModeCombo = ComboBox(arrayOf(DEVICES_DEFAULT, DEVICES_CUSTOM)).apply {
        addActionListener { onDevicesModeChanged() }
    }
    private val devicesField = JBTextField(EXAMPLE_DEVICES).apply { isEnabled = false }

    init {
        title = "Configure Jenkins Test Run"
        setOKButtonText("Send to Jenkins")
        tree.addCheckboxTreeListener(object : CheckboxTreeListener {
            override fun nodeStateChanged(node: CheckedTreeNode) {
                syncStore()
                refreshPreview()
                updateSendEnabled()
            }
        })
        refreshPreview()
        init()
        updateSendEnabled()
    }

    override fun createCenterPanel(): JComponent {
        val treePanel = JPanel(BorderLayout(0, 4)).apply {
            add(JBLabel("Select classes or individual tests to run:"), BorderLayout.NORTH)
            add(JBScrollPane(tree).apply { preferredSize = Dimension(560, 240) }, BorderLayout.CENTER)
        }

        val copyButton = JButton("Copy JSON")
        copyButton.addActionListener {
            copyJson()
            showCopiedBalloon(copyButton)
        }
        val clearButton = JButton("Clear").apply { addActionListener { clearAll() } }
        val previewPanel = JPanel(BorderLayout(0, 4)).apply {
            add(JBLabel("Targets (JSON)"), BorderLayout.NORTH)
            add(JBScrollPane(jsonPreview).apply { preferredSize = Dimension(560, 320) }, BorderLayout.CENTER)
            add(
                JPanel(BorderLayout()).apply {
                    add(JPanel().apply { add(clearButton); add(copyButton) }, BorderLayout.EAST)
                },
                BorderLayout.SOUTH,
            )
        }

        val devicesRow = JPanel(BorderLayout(8, 0)).apply {
            add(devicesModeCombo, BorderLayout.WEST)
            add(devicesField, BorderLayout.CENTER)
        }

        // FormBuilder keeps each label at its natural width and stretches the field to fill the row —
        // no half-width column gap, so "User:" sits right before its input.
        val paramsPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent("User:", userField)
            .addLabeledComponent("Branch:", branchField)
            .addLabeledComponent("TestApi:", testApiCombo)
            .addLabeledComponent("Devices:", devicesRow)
            .panel
            .apply { border = JBUI.Borders.emptyTop(8) }

        return JPanel(BorderLayout(0, 10)).apply {
            add(treePanel, BorderLayout.NORTH)
            add(previewPanel, BorderLayout.CENTER)
            add(paramsPanel, BorderLayout.SOUTH)
            preferredSize = Dimension(580, 680)
        }
    }

    override fun doValidate(): ValidationInfo? {
        if (store.isEmpty()) {
            return ValidationInfo("Select at least one test or class", tree)
        }
        if (userField.text.isBlank()) return ValidationInfo("Enter the user", userField)
        if (branchField.text.isBlank()) return ValidationInfo("Enter the branch", branchField)
        if (isCustomDevices() && devicesField.text.isBlank()) {
            return ValidationInfo("Enter the devices JSON", devicesField)
        }
        return null
    }

    override fun doOKAction() {
        val auth = service<JenkinsAuthService>()
        // No token yet → make the user connect first; if they cancel, keep this dialog open.
        if (!auth.hasToken()) {
            if (!JenkinsConnectDialog(project).showAndGet()) return
        }
        val config = auth.config() ?: run {
            Messages.showErrorDialog(project, "Jenkins connection is not fully configured.", "Jenkins Test Run")
            return
        }

        val parameters = buildMap {
            put("user", userField.text.trim())
            put("branch", branchField.text.trim())
            put("testApi", (testApiCombo.selectedItem as? String).orEmpty())
            put("targets", JenkinsTargets.toJson(store.all()))
            // "Default" → omit devices so Jenkins uses the job's own default value.
            if (isCustomDevices()) put("devices", devicesField.text.trim())
        }
        triggerInBackground(config, parameters)
        store.clear() // start fresh next time, now that this selection has been sent
        super.doOKAction()
    }

    private fun triggerInBackground(config: JenkinsClient.Config, parameters: Map<String, String>) {
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Triggering Jenkins build", true) {
            override fun run(indicator: ProgressIndicator) {
                JenkinsClient.trigger(config, parameters)
                    .onSuccess {
                        // Link to the job page — instant and always valid (the build number doesn't
                        // exist until the build leaves the queue). It's also where Jenkins' own
                        // "Build" button redirects; the just-triggered build sits at the top.
                        notifyTriggered(JenkinsClient.jobUrl(config))
                    }
                    .onFailure { error ->
                        val message = error.message.orEmpty()
                        // Bad/expired token → forget it so the next Send prompts to reconnect.
                        if ("HTTP 401" in message || "HTTP 403" in message) {
                            service<JenkinsAuthService>().clearToken()
                            notifyError("Jenkins rejected the token (it was cleared — reconnect on next send):\n$message")
                        } else {
                            notifyError("Failed to trigger Jenkins build:\n$message")
                        }
                    }
            }
        })
    }

    private fun notifyTriggered(buildUrl: String) {
        ApplicationManager.getApplication().invokeLater {
            NotificationGroupManager.getInstance()
                .getNotificationGroup("BandLab Jenkins")
                .createNotification("Jenkins test run triggered", buildUrl, NotificationType.INFORMATION)
                .addAction(NotificationAction.createSimple("Open in Jenkins") { BrowserUtil.browse(buildUrl) })
                .notify(project)
        }
    }

    private fun notifyError(message: String) {
        ApplicationManager.getApplication().invokeLater {
            Messages.showErrorDialog(project, message, "Jenkins Test Run")
        }
    }

    private fun isCustomDevices(): Boolean = devicesModeCombo.selectedItem == DEVICES_CUSTOM

    /** Enable the field only in Custom; refill the template if the field was left blank. */
    private fun onDevicesModeChanged() {
        devicesField.isEnabled = isCustomDevices()
        if (devicesField.text.isBlank()) devicesField.text = EXAMPLE_DEVICES
    }

    private fun copyJson() {
        CopyPasteManager.getInstance().setContents(StringSelection(JenkinsTargets.toJson(store.all())))
    }

    /** Small auto-fading "copied" balloon anchored above the Copy button (works inside the modal). */
    private fun showCopiedBalloon(anchor: JComponent) {
        JBPopupFactory.getInstance()
            .createHtmlTextBalloonBuilder("Targets copied to clipboard", MessageType.INFO, null)
            .setFadeoutTime(2_000)
            .createBalloon()
            .show(RelativePoint.getCenterOf(anchor), Balloon.Position.above)
    }

    /** Preview shows the full accumulated selection (this file + every other file). */
    private fun refreshPreview() {
        jsonPreview.text = JenkinsTargets.toJson(store.all())
    }

    /** Pushes this file's current checkbox selection into the store, leaving other files intact. */
    private fun syncStore() {
        store.replaceForClasses(classFqns, currentFileTargets())
    }

    /** Clears every accumulated target and unchecks the whole tree. */
    private fun clearAll() {
        store.clear()
        for (i in 0 until rootNode.childCount) {
            val classNode = rootNode.getChildAt(i) as? CheckedTreeNode ?: continue
            classNode.isChecked = false
            for (j in 0 until classNode.childCount) {
                (classNode.getChildAt(j) as? CheckedTreeNode)?.isChecked = false
            }
        }
        tree.repaint()
        refreshPreview()
        updateSendEnabled()
    }

    /** Send to Jenkins is only allowed when at least one target is selected. */
    private fun updateSendEnabled() {
        isOKActionEnabled = !store.isEmpty()
    }

    /** Reads the current file's checkbox state out of the tree and builds its target list. */
    private fun currentFileTargets(): List<String> {
        val selections = mutableListOf<JenkinsTargets.Selection>()
        for (i in 0 until rootNode.childCount) {
            val classNode = rootNode.getChildAt(i) as? CheckedTreeNode ?: continue
            val testClass = classNode.userObject as? TestClass ?: continue
            val selectedMethods = mutableListOf<TestMethod>()
            for (j in 0 until classNode.childCount) {
                val methodNode = classNode.getChildAt(j) as? CheckedTreeNode ?: continue
                val method = methodNode.userObject as? TestMethod ?: continue
                if (methodNode.isChecked) selectedMethods.add(method)
            }
            selections.add(JenkinsTargets.Selection(testClass, selectedMethods))
        }
        return JenkinsTargets.build(selections)
    }

    /** Renders [TestClass] / [TestMethod] user objects as plain labels in the checkbox tree. */
    private class TestTreeCellRenderer : CheckboxTree.CheckboxTreeCellRenderer() {
        override fun customizeRenderer(
            tree: JTree,
            value: Any,
            selected: Boolean,
            expanded: Boolean,
            leaf: Boolean,
            row: Int,
            hasFocus: Boolean
        ) {
            val node = value as? CheckedTreeNode ?: return
            val text = when (val obj = node.userObject) {
                is TestClass -> "${obj.simpleName}  (${obj.methods.size})"
                is TestMethod -> obj.name
                else -> obj?.toString().orEmpty()
            }
            textRenderer.append(text)
        }
    }

    private companion object {
        const val DEFAULT_USER = "bandlab"

        // devices mode options.
        const val DEVICES_DEFAULT = "Default"
        const val DEVICES_CUSTOM = "Custom"

        // A starting template for the Custom field — only sent when "Custom" is selected, so it is
        // not a hardcoded default (Default mode defers to the Jenkins job).
        const val EXAMPLE_DEVICES = """[ {"model": "MediumPhone.arm", "version": "36"} ]"""
    }
}
