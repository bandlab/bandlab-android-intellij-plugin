package com.bandlab.intellij.plugin.strings

import com.bandlab.intellij.plugin.localizer.LocalizerConfigService
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.icons.AllIcons
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Iconable
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import javax.swing.Icon
import org.jetbrains.kotlin.analysis.api.fir.diagnostics.KaFirDiagnostic
import org.jetbrains.kotlin.idea.codeinsight.api.applicators.fixes.KotlinQuickFixRegistrar
import org.jetbrains.kotlin.idea.codeinsight.api.applicators.fixes.KotlinQuickFixesList
import org.jetbrains.kotlin.idea.codeinsight.api.applicators.fixes.KtQuickFixesListBuilder
import org.jetbrains.kotlin.idea.codeinsight.api.classic.quickfixes.PsiElementSuitabilityCheckers
import org.jetbrains.kotlin.idea.codeinsight.api.classic.quickfixes.quickFixesPsiBasedFactory
import org.jetbrains.kotlin.psi.KtNameReferenceExpression

/**
 * Contributes "Localizer: Add string" to the **red** error-fix section on an *unresolved*
 * `R.string.X` / `R.plurals.X` reference in Kotlin code (incl. import-aliased / typealiased forms) —
 * i.e. when the key doesn't exist yet. By hooking the K2 `UNRESOLVED_REFERENCE` diagnostic the fix
 * sits beside Android's "Create string value resource", rather than buried in the yellow intention
 * list ([AddStringFromReferenceIntention], which stays as the always-on fallback until this path is
 * confirmed in the IDE). Pulls the key from Tolgee via `update-strings --add-keys`.
 *
 * Registered through the Kotlin plugin EP `org.jetbrains.kotlin.codeinsight.quickfix.registrar`.
 */
class AddStringUnresolvedQuickFixRegistrar : KotlinQuickFixRegistrar() {

    override val list: KotlinQuickFixesList = KtQuickFixesListBuilder.registerPsiQuickFix {
        registerPsiQuickFixes(
            KaFirDiagnostic.UnresolvedReference::class,
            quickFixesPsiBasedFactory(PsiElementSuitabilityCheckers.ALWAYS_SUITABLE) { psi ->
                addStringFixesFor(psi)
            },
        )
    }
}

private fun addStringFixesFor(psi: PsiElement): List<IntentionAction> {
    if (!psi.project.service<LocalizerConfigService>().isConfigured()) return emptyList()
    val ref = psi as? KtNameReferenceExpression
        ?: PsiTreeUtil.getParentOfType(psi, KtNameReferenceExpression::class.java, false)
        ?: return emptyList()
    val key = resStringKey(ref) ?: return emptyList()
    return listOf(AddStringQuickFix(key))
}

private class AddStringQuickFix(private val key: String) : IntentionAction, Iconable {

    override fun getIcon(flags: Int): Icon = AllIcons.General.Add

    override fun getText(): String = "Localizer: Add string \"$key\""

    override fun getFamilyName(): String = "Localizer: Add string"

    override fun startInWriteAction(): Boolean = false

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean = true

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        LocalizerOps.addKey(project, key)
    }
}
