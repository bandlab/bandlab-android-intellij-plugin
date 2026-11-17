// Copyright 2026 BandLab Singapore Pte Ltd
// SPDX-License-Identifier: Apache-2.0
package com.bandlab.intellij.plugin.strings.textres

import com.android.resources.ResourceType
import com.android.tools.idea.res.psi.ResourceReferencePsiElement
import com.intellij.find.findUsages.PsiElement2UsageTargetAdapter
import com.intellij.openapi.application.QueryExecutorBase
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.search.PsiSearchHelper
import com.intellij.psi.search.UsageSearchContext
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.xml.XmlTag
import com.intellij.usages.UsageTarget
import com.intellij.usages.UsageTargetProvider
import com.intellij.util.Processor
import java.nio.file.Path
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNameReferenceExpression

/**
 * Find usages on `Strings.save` searches its strings.xml entry. The compiler plugin's members have
 * no declaration and K2 resolves the reference to the package, which is what Find Usages would
 * search otherwise.
 */
class TextResUsageTargetProvider : UsageTargetProvider {

    override fun getTargets(editor: Editor, file: PsiFile): Array<UsageTarget>? {
        if (file !is KtFile) return null
        val key = textResKeyAt(file, editor.caretModel.offset) ?: return null
        val tag = textResKeyTag(file, key) ?: return null
        return arrayOf(PsiElement2UsageTargetAdapter(tag, true))
    }
}

/**
 * The `Strings.<key>` / `Plurals.<key>` references of a strings.xml entry: the `<string>` or
 * `<plurals>` tag of a table file, or Android Studio's resource element for it. Found by word
 * search, since nothing indexes references to members without a declaration.
 */
class TextResKeyReferencesSearcher :
    QueryExecutorBase<PsiReference, ReferencesSearch.SearchParameters>(true) {

    override fun processQuery(
        parameters: ReferencesSearch.SearchParameters,
        consumer: Processor<in PsiReference>,
    ) {
        val target = parameters.elementToSearch
        val searched = searchedKey(target) ?: return
        if (textResTableFiles(target.project).isEmpty()) return
        PsiSearchHelper.getInstance(target.project)
            .processElementsWithWord(
                { element, _ ->
                    val ref = element as? KtNameReferenceExpression
                    val key = ref?.let(::textResKeyReference)
                    if (key == null || !searched.matches(key)) true
                    else consumer.process(TextResKeyUsage(ref, target))
                },
                parameters.effectiveSearchScope,
                searched.key,
                UsageSearchContext.IN_CODE,
                true,
            )
    }

    private fun searchedKey(target: PsiElement): SearchedKey? =
        when (target) {
            is XmlTag -> {
                val file = target.containingFile?.virtualFile?.toNioPath()
                val key = target.getAttributeValue("name")
                when {
                    file == null || key == null || file !in textResTableFiles(target.project) ->
                        null
                    target.name == "string" -> SearchedKey(key, isPlural = false, file)
                    target.name == "plurals" -> SearchedKey(key, isPlural = true, file)
                    else -> null
                }
            }
            is ResourceReferencePsiElement ->
                when (target.resourceReference.resourceType) {
                    ResourceType.STRING -> SearchedKey(target.resourceReference.name, false, null)
                    ResourceType.PLURALS -> SearchedKey(target.resourceReference.name, true, null)
                    else -> null
                }
            else -> null
        }

    /** [file] is null for Android Studio's resource element, which spans every strings.xml. */
    private data class SearchedKey(val key: String, val isPlural: Boolean, val file: Path?) {
        fun matches(reference: TextResKeyReference): Boolean =
            reference.key == key &&
                reference.isPlural == isPlural &&
                (file == null || reference.table.file == file)
    }
}

private class TextResKeyUsage(ref: KtNameReferenceExpression, private val target: PsiElement) :
    PsiReferenceBase<KtNameReferenceExpression>(ref, TextRange(0, ref.textLength), true) {

    override fun resolve(): PsiElement = target
}
