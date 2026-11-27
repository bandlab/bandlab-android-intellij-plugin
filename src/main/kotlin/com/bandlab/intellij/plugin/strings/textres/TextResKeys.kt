// Copyright 2026 BandLab Singapore Pte Ltd
// SPDX-License-Identifier: Apache-2.0
package com.bandlab.intellij.plugin.strings.textres

import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNameReferenceExpression

/** `Strings.<key>` / `Plurals.<key>` on an object the localizer compiler plugin declares. */
internal data class TextResKeyReference(
    val table: TextResTable,
    val isPlural: Boolean,
    val key: String,
)

internal fun textResKeyAt(psiFile: PsiFile, offset: Int): TextResKeyReference? {
    val element =
        psiFile.findElementAt(offset)
            ?: (if (offset > 0) psiFile.findElementAt(offset - 1) else null)
            ?: return null
    val ref =
        PsiTreeUtil.getParentOfType(element, KtNameReferenceExpression::class.java, false)
            ?: return null
    return textResKeyReference(ref)
}

/**
 * The key [ref] names when it is a member of a table object: `Strings.save`,
 * `Strings.welcome(name)`, `Plurals.n_followers(count)` (callable references are not covered). The
 * member need not resolve, so a key that is not in strings.xml yet counts; the receiver must name
 * the table's object, resolved through the compiler plugin when it is loaded.
 */
internal fun textResKeyReference(ref: KtNameReferenceExpression): TextResKeyReference? {
    val selector = (ref.parent as? KtCallExpression)?.takeIf { it.calleeExpression === ref } ?: ref
    val qualified = selector.parent as? KtDotQualifiedExpression ?: return null
    if (qualified.selectorExpression !== selector) return null
    val receiver = qualified.receiverExpression
    val receiverName = receiverShortName(receiver) ?: return null
    val tables =
        textResTables(ref)
            .filter { receiverName == it.stringsObject || receiverName == it.pluralsObject }
            .ifEmpty {
                return null
            }
    // The Analysis API refuses the EDT (e.g. go to declaration called there); imports name the
    // object just as well in the forms bandlab-android uses
    val onEdt = ApplicationManager.getApplication().isDispatchThread
    val classId =
        (if (onEdt) null else resolvedClassId(receiver)) ?: importedClassId(receiver, receiverName)
    val key = ref.getReferencedName()
    return tables.firstNotNullOfOrNull { table ->
        when (classId) {
            table.stringsClassId -> TextResKeyReference(table, isPlural = false, key)
            table.pluralsClassId -> TextResKeyReference(table, isPlural = true, key)
            else -> null
        }
    }
}

private fun receiverShortName(receiver: KtExpression): String? =
    when (receiver) {
        is KtNameReferenceExpression -> receiver.getReferencedName()
        is KtDotQualifiedExpression ->
            (receiver.selectorExpression as? KtNameReferenceExpression)?.getReferencedName()
        else -> null
    }

private fun resolvedClassId(receiver: KtExpression): ClassId? {
    val name =
        when (receiver) {
            is KtNameReferenceExpression -> receiver
            is KtDotQualifiedExpression -> receiver.selectorExpression as? KtNameReferenceExpression
            else -> null
        } ?: return null
    return analyze(name) { (name.mainReference.resolveToSymbol() as? KaClassSymbol)?.classId }
}

/**
 * Without the compiler plugin loaded (no sync yet, or the IDE runs another Kotlin than the bundled
 * builds), `Strings` doesn't resolve; the import, a fully qualified receiver or the file's own
 * package still name the object.
 */
private fun importedClassId(receiver: KtExpression, name: String): ClassId? {
    if (receiver is KtDotQualifiedExpression) {
        return ClassId.topLevel(FqName(receiver.text.filterNot(Char::isWhitespace)))
    }
    val file = receiver.containingFile as? KtFile ?: return null
    val imported =
        file.importDirectives.firstOrNull {
            (it.aliasName ?: it.importedFqName?.shortName()?.asString()) == name
        }
    val fqName = imported?.importedFqName ?: file.packageFqName.child(Name.identifier(name))
    return ClassId.topLevel(fqName)
}
