package com.bandlab.intellij.plugin.resources

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.android.ide.common.vectordrawable.VdPreview
import com.intellij.util.ImageLoader
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtProperty
import java.awt.image.BufferedImage
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap
import javax.imageio.ImageIO
import javax.swing.Icon
import javax.swing.ImageIcon

private const val UIKIT_ICONS = "UikitIcons"
private const val GUTTER_ICON_SIZE = 16

private val LOG = logger<UikitIconLineMarkerProvider>()

class UikitIconLineMarkerProvider : LineMarkerProvider {

    // Maps drawable name → icon (Optional.empty means "not found, don't retry")
    private val iconCache = ConcurrentHashMap<String, Optional<Icon>>()

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        if (element !is LeafPsiElement || element.elementType != KtTokens.IDENTIFIER) return null

        val nameRef = element.parent as? KtNameReferenceExpression ?: return null
        val dotExpr = nameRef.parent as? KtDotQualifiedExpression ?: return null

        // Only match when nameRef is the selector (right side), not the receiver
        if (dotExpr.selectorExpression !== nameRef) return null

        val propertyName = nameRef.getReferencedName()
        val drawableName = when {
            dotExpr.receiverExpression.text == UIKIT_ICONS -> resolveDrawableName(nameRef, propertyName)
            else -> resolveViaExtensionDelegate(nameRef)
        } ?: return null

        val icon = iconCache.computeIfAbsent(drawableName) {
            Optional.ofNullable(findIcon(element.project, drawableName))
        }.orElse(null) ?: return null

        return LineMarkerInfo(
            /* element = */ element,
            /* range = */ element.textRange,
            /* icon = */ icon,
            /* tooltipProvider = */ com.intellij.util.Function { "$propertyName → $drawableName" },
            /* navHandler = */ null,
            /* alignment = */ GutterIconRenderer.Alignment.LEFT,
            /* accessibleNameProvider = */ { propertyName },
        )
    }

    private fun resolveDrawableName(nameRef: KtNameReferenceExpression, propertyName: String): String {
        val resolved = nameRef.reference?.resolve() as? KtProperty
        if (resolved != null) {
            extractFromGetter(resolved)?.let { return it }
        }
        return propertyName.toDrawableName()
    }

    // Handles extension properties that delegate to UikitIcons, e.g.:
    //   val ImageRes.Local.Companion.User get() = UikitIcons.User
    private fun resolveViaExtensionDelegate(nameRef: KtNameReferenceExpression): String? {
        val resolved = nameRef.reference?.resolve() as? KtProperty ?: return null
        val getterBody = resolved.getter?.bodyExpression as? KtDotQualifiedExpression ?: return null
        if (getterBody.receiverExpression.text != UIKIT_ICONS) return null
        val uikitPropName = getterBody.selectorExpression?.text ?: return null
        val uikitNameRef = getterBody.selectorExpression as? KtNameReferenceExpression
        return if (uikitNameRef != null) resolveDrawableName(uikitNameRef, uikitPropName)
               else uikitPropName.toDrawableName()
    }

    /**
     * Reads the getter body to extract the drawable resource name.
     * Handles: get() = ImageRes(resId = Icons.ic_something, ...)
     */
    private fun extractFromGetter(property: KtProperty): String? {
        val body = property.getter?.bodyExpression as? KtCallExpression ?: return null
        val resIdArg = body.valueArguments.find {
            it.getArgumentName()?.asName?.identifier == "resId"
        } ?: body.valueArguments.firstOrNull() ?: return null

        return when (val expr = resIdArg.getArgumentExpression()) {
            is KtDotQualifiedExpression -> expr.selectorExpression?.text
            is KtNameReferenceExpression -> expr.getReferencedName()
            else -> null
        }
    }

    private fun String.toDrawableName(): String =
        "ic_" + replace(Regex("([a-z])([A-Z])"), "$1_$2").lowercase()

    private fun findIcon(project: Project, drawableName: String): Icon? {
        for (ext in listOf("xml", "svg", "png", "jpg", "webp")) {
            val file = FilenameIndex.getVirtualFilesByName(
                "$drawableName.$ext",
                GlobalSearchScope.projectScope(project),
            ).firstOrNull { "/drawable" in it.path } ?: continue

            return try {
                loadIcon(file)
            } catch (e: Exception) {
                LOG.warn("Failed to load icon from ${file.path}", e)
                null
            }
        }
        return null
    }

    private fun loadIcon(file: VirtualFile): Icon? {
        val image = when (file.extension?.lowercase()) {
            "xml" -> {
                val xmlContent = file.inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
                VdPreview.getPreviewFromVectorXml(
                    VdPreview.TargetSize.createFromMaxDimension(GUTTER_ICON_SIZE),
                    xmlContent,
                    StringBuilder(),
                )
            }
            "svg" -> ImageLoader.loadFromUrl(file.toNioPath().toUri().toURL())
            "png", "jpg", "jpeg", "webp" -> file.inputStream.use { ImageIO.read(it) }
            else -> return null
        } ?: return null

        val scaled = BufferedImage(GUTTER_ICON_SIZE, GUTTER_ICON_SIZE, BufferedImage.TYPE_INT_ARGB)
        val g = scaled.createGraphics()
        try {
            g.drawImage(image, 0, 0, GUTTER_ICON_SIZE, GUTTER_ICON_SIZE, null)
        } finally {
            g.dispose()
        }
        return ImageIcon(scaled)
    }
}
