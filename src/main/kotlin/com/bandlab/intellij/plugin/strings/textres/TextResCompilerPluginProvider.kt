// Copyright 2026 BandLab Singapore Pte Ltd
// SPDX-License-Identifier: Apache-2.0
package com.bandlab.intellij.plugin.strings.textres

import com.intellij.ide.plugins.PluginManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import org.jetbrains.kotlin.idea.compiler.configuration.KotlinPluginLayout
import org.jetbrains.kotlin.idea.fir.extensions.CompilerPluginRegistrarUtils
import org.jetbrains.kotlin.idea.fir.extensions.KotlinBundledFirCompilerPluginProvider

/**
 * Replaces the localizer compiler plugin jar that Gradle sync puts into the Kotlin facet with a
 * build of the same plugin compiled against the IDE's own Kotlin compiler, shipped in this plugin's
 * `compiler-plugins/` directory. The IDE treats a substituted jar as bundled, so the plugin's FIR
 * extensions load without the `kotlin.k2.only.bundled.compiler.plugins.enabled` registry key and
 * without binary mismatches between the project's Kotlin and the IDE's.
 *
 * The bundled jar contains the plugin's `text-keys` dependency too; Gradle's separate `text-keys`
 * jar maps to the same file, which the IDE then loads once.
 */
class TextResCompilerPluginProvider : KotlinBundledFirCompilerPluginProvider {

    override fun provideBundledPluginJar(project: Project, userSuppliedPluginJar: Path): Path? {
        if (!isLocalizerJar(userSuppliedPluginJar)) return null
        return bundledJar
    }

    private fun isLocalizerJar(jar: Path): Boolean =
        jar.name.startsWith("text-keys-") && jar.any { it.name == "text-keys" } ||
            CompilerPluginRegistrarUtils.readRegistrarContent(jar)?.contains(REGISTRAR) == true

    private val bundledJar: Path? by lazy {
        val directory =
            PluginManager.getInstance()
                .findEnabledPlugin(PluginId.getId(PLUGIN_ID))
                ?.pluginPath
                ?.resolve("compiler-plugins")
                ?.takeIf { it.isDirectory() }
        val jars = directory?.listDirectoryEntries("$JAR_PREFIX*.jar").orEmpty()
        val ideKotlin = KotlinPluginLayout.ideCompilerVersion.rawVersion
        val picked =
            pickCompilerPluginBuild(
                ideKotlin,
                jars.map { it.name.removePrefix(JAR_PREFIX).removeSuffix(".jar") },
            )
        LOG.info(
            "Localizer compiler plugin for IDE Kotlin $ideKotlin: ${picked ?: "none of $jars"}",
        )
        picked?.let { directory?.resolve("$JAR_PREFIX$it.jar") }
    }

    private companion object {
        const val PLUGIN_ID = "com.bandlab.intellij.plugin"
        const val JAR_PREFIX = "localizer-compiler-plugin-"
        const val REGISTRAR = "com.bandlab.localizer.text.compiler.TextResCompilerPluginRegistrar"
        val LOG = logger<TextResCompilerPluginProvider>()
    }
}

/**
 * The bundled build to load into an IDE whose analyzer is Kotlin [ideKotlin], among [builds] (the
 * IDE compiler versions the plugin jars were compiled against). An exact match wins. Otherwise only
 * a build of the same IntelliJ branch (`2.3.20-ij253-NNN`) with a build number not above the IDE's:
 * a public release of the same version is not binary compatible (2.3.20 changed
 * `createMemberFunction`'s return type, `2.3.20-ij253-105` did not), and Android Studio reports
 * `2.x.255-dev-255` whatever dev compiler it bundles. Null when nothing fits: the IDE then falls
 * back to the user-supplied jar behind the registry key.
 */
internal fun pickCompilerPluginBuild(ideKotlin: String, builds: List<String>): String? {
    if (ideKotlin in builds) return ideKotlin
    val (branch, number) = ideBranchBuild(ideKotlin) ?: return null
    return builds
        .mapNotNull { build -> ideBranchBuild(build)?.let { build to it } }
        .filter { (_, it) -> it.first == branch && it.second <= number }
        .maxByOrNull { (_, it) -> it.second }
        ?.first
}

private fun ideBranchBuild(version: String): Pair<String, Int>? =
    Regex("""^(\d+\.\d+\.\d+-ij\d+)-(\d+)$""").find(version)?.groupValues?.let {
        it[1] to it[2].toInt()
    }
