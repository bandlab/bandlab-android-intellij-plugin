package com.bandlab.intellij.plugin.localizer

import com.bandlab.localizer.config.LocalizerConfig
import com.bandlab.localizer.config.LocalizerConfigLoader
import com.bandlab.localizer.config.toFileGroups
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.getLastModifiedTime

/**
 * Resolves the project's `bandlab-localizer-config.toml` manifest. The Localizer actions are
 * available whenever this resolves — they read the manifest and shell out to the CLI, so they work
 * even when Gradle sync fails (the plugin never depends on the Android/Gradle model).
 *
 * Convention: the manifest lives at `<projectRoot>/localizer/<DEFAULT_CONFIG_FILENAME>`. The parsed
 * result is cached and recomputed when the manifest's modification time changes (action `update()`
 * calls hit this frequently).
 */
@Service(Service.Level.PROJECT)
class LocalizerConfigService(private val project: Project) {

    /**
     * One `[[file]]` base file. [addKeysToFile] is the value to pass to the CLI's `--add-keys-to-file`
     * (the manifest's resolved, base_path-folded path); [baseFile] is its absolute location.
     */
    data class Target(val addKeysToFile: String, val baseFile: Path)

    private var cache: Pair<Long, LocalizerConfig?>? = null

    /** True when a localizer manifest is present and parses — the gate for the Localizer actions. */
    fun isConfigured(): Boolean = config() != null

    /** Every target base file, in manifest order (the first is the CLI's default). */
    fun targets(): List<Target> = groups().map { it.toTarget() }

    /** The target owning [file] when it's a manifest base or translation string file, else null. */
    fun targetFor(file: VirtualFile): Target? {
        val path = file.toNioPathOrNull() ?: return null
        return groups().firstOrNull { path == it.baseFile || path in it.translations.values }?.toTarget()
    }

    /** True when [file] is a manifest base or translation string file (gates the context actions). */
    fun isManagedStringFile(file: VirtualFile): Boolean = targetFor(file) != null

    /** Every base + translation file the manifest manages — the set to refresh after a CLI run. */
    fun managedFilePaths(): List<Path> = groups().flatMap { listOf(it.baseFile) + it.translations.values }

    private fun groups() = config()?.let { config ->
        projectRoot()?.let { root -> config.toFileGroups(root) }
    }.orEmpty()

    private fun com.bandlab.localizer.config.FileGroup.toTarget(): Target {
        val root = projectRoot()!!
        return Target(addKeysToFile = root.relativize(baseFile).joinToString("/"), baseFile = baseFile)
    }

    private fun projectRoot(): Path? = project.basePath?.let(Path::of)

    private fun config(): LocalizerConfig? {
        val manifest = projectRoot()?.resolve("localizer")?.resolve(LocalizerConfigLoader.DEFAULT_CONFIG_FILENAME)
        if (manifest == null || !manifest.exists()) {
            cache = null
            return null
        }
        val stamp = manifest.getLastModifiedTime().toMillis()
        cache?.let { (cachedStamp, cfg) -> if (cachedStamp == stamp) return cfg }
        return runCatching { LocalizerConfigLoader.load(manifest) }.getOrNull()
            .also { cache = stamp to it }
    }
}

private fun VirtualFile.toNioPathOrNull(): Path? = runCatching { toNioPath() }.getOrNull()
