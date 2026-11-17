// Copyright 2026 BandLab Singapore Pte Ltd
// SPDX-License-Identifier: Apache-2.0
package com.bandlab.intellij.plugin.strings.textres

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag
import org.jetbrains.kotlin.psi.KtNameReferenceExpression

/**
 * Go to declaration on `Strings.save` opens `<string name="save">` in the table's strings.xml. The
 * compiler plugin's members have no source, so without this the IDE has nowhere to go.
 */
class TextResGotoDeclarationHandler : GotoDeclarationHandler {

    override fun getGotoDeclarationTargets(
        sourceElement: PsiElement?,
        offset: Int,
        editor: Editor?,
    ): Array<PsiElement>? {
        val ref =
            PsiTreeUtil.getParentOfType(sourceElement, KtNameReferenceExpression::class.java, false)
                ?: return null
        val key = textResKeyReference(ref) ?: return null
        return textResKeyTag(sourceElement!!, key)?.let { arrayOf(it) }
    }
}

/** The `<string>`/`<plurals>` element [key] refers to, or null when it is not in the file. */
internal fun textResKeyTag(context: PsiElement, key: TextResKeyReference): XmlTag? {
    val virtualFile =
        VirtualFileManager.getInstance().findFileByNioPath(key.table.file) ?: return null
    val xml =
        PsiManager.getInstance(context.project).findFile(virtualFile) as? XmlFile ?: return null
    val tagName = if (key.isPlural) "plurals" else "string"
    return xml.rootTag?.findSubTags(tagName)?.firstOrNull {
        it.getAttributeValue("name") == key.key
    }
}
