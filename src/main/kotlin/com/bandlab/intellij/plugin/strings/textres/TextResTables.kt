// Copyright 2026 BandLab Singapore Pte Ltd
// SPDX-License-Identifier: Apache-2.0
package com.bandlab.intellij.plugin.strings.textres

import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootModificationTracker
import com.intellij.psi.PsiElement
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import java.nio.file.Path
import org.jetbrains.kotlin.idea.compiler.configuration.KotlinCommonCompilerArgumentsHolder
import org.jetbrains.kotlin.idea.compiler.configuration.KotlinCompilerSettingsTracker
import org.jetbrains.kotlin.idea.facet.KotlinFacet
import org.jetbrains.kotlin.idea.facet.KotlinFacetModificationTracker
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/**
 * Id of bandlab-localizer's compiler plugin (`com.bandlab.localizer.text`), see `-P plugin:<id>:`
 */
internal const val TEXT_RES_PLUGIN_ID = "com.bandlab.localizer.text"

/**
 * One `strings.xml` whose keys the compiler plugin declares as members of [stringsObject] and
 * [pluralsObject] in [packageName]. Read from the compile task's `-P` options that Gradle sync puts
 * into the module's Kotlin facet, so it names exactly the file the compiler plugin reads.
 */
internal data class TextResTable(
    val packageName: String,
    val stringsObject: String,
    val pluralsObject: String,
    val file: Path,
) {
    val stringsClassId: ClassId = classId(stringsObject)
    val pluralsClassId: ClassId = classId(pluralsObject)

    private fun classId(name: String) = ClassId(FqName(packageName), Name.identifier(name))
}

/**
 * Tables declared by `-P plugin:com.bandlab.localizer.text:table=<package>;<Strings>;<Plurals>` and
 * `...:tableFile=<package>;<path>`. A table without a file is dropped: the compiler plugin rejects
 * it too.
 */
internal fun parseTextResTables(pluginOptions: Iterable<String>): List<TextResTable> {
    val prefix = "plugin:$TEXT_RES_PLUGIN_ID:"
    val options =
        pluginOptions
            .filter { it.startsWith(prefix) }
            .map { it.removePrefix(prefix).substringBefore('=') to it.substringAfter('=') }
    val files =
        options
            .filter { (key, _) -> key == "tableFile" }
            .associate { (_, value) -> value.substringBefore(';') to value.substringAfter(';') }
    return options
        .filter { (key, _) -> key == "table" }
        .mapNotNull { (_, value) ->
            val parts = value.split(';').takeIf { it.size == 3 } ?: return@mapNotNull null
            val (packageName, strings, plurals) = parts
            val file = files[packageName] ?: return@mapNotNull null
            TextResTable(packageName, strings, plurals, Path.of(file))
        }
        .distinct()
}

/**
 * The tables of [module], from its Kotlin facet (what Gradle sync imports) or, without one, the
 * project-wide Kotlin compiler settings — the same arguments the IDE hands to the compiler plugin.
 */
internal fun textResTables(module: Module): List<TextResTable> =
    CachedValuesManager.getManager(module.project).getCachedValue(module) {
        val arguments =
            KotlinFacet.get(module)?.configuration?.settings?.mergedCompilerArguments
                ?: KotlinCommonCompilerArgumentsHolder.getInstance(module.project).settings
        CachedValueProvider.Result.create(
            parseTextResTables(arguments.pluginOptions.orEmpty().asIterable()),
            KotlinFacetModificationTracker.getInstance(module.project),
            KotlinCompilerSettingsTracker.getInstance(module.project),
        )
    }

internal fun textResTables(element: PsiElement): List<TextResTable> =
    ModuleUtilCore.findModuleForPsiElement(element)?.let(::textResTables).orEmpty()

/** Every table file any module of [project] compiles against. */
internal fun textResTableFiles(project: Project): Set<Path> =
    CachedValuesManager.getManager(project).getCachedValue(project) {
        CachedValueProvider.Result.create(
            ModuleManager.getInstance(project).modules.flatMap(::textResTables).mapTo(
                mutableSetOf(),
            ) {
                it.file
            },
            ProjectRootModificationTracker.getInstance(project),
            KotlinFacetModificationTracker.getInstance(project),
            KotlinCompilerSettingsTracker.getInstance(project),
        )
    }
