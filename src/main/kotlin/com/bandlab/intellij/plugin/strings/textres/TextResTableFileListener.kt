// Copyright 2026 BandLab Singapore Pte Ltd
// SPDX-License-Identifier: Apache-2.0
package com.bandlab.intellij.plugin.strings.textres

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import java.nio.file.Path
import org.jetbrains.kotlin.analysis.api.platform.modification.publishGlobalSourceOutOfBlockModificationEvent

/**
 * The compiler plugin re-reads a table's strings.xml when the file changes on disk, but the IDE
 * caches the members it generated in each module's FIR session. An edit of the XML invalidates only
 * the module owning the file and its dependents, and the modules using `Strings` compile against
 * the table, not that module (in bandlab-android it is runtime-only or absent). So when a table
 * file is written (an edit saved, the localizer CLI behind "Add string", a VCS update), drop the
 * source sessions and re-highlight; the next analysis asks the plugin again and sees the new keys.
 *
 * One global event rather than one per module: every Kotlin module of bandlab-android compiles
 * against the common table, so a per-module event would cover nearly all of them anyway.
 */
class TextResTableFileListener(private val project: Project) : BulkFileListener {

    override fun after(events: List<VFileEvent>) {
        if (project.isDisposed) return
        val tableFiles =
            textResTableFiles(project).ifEmpty {
                return
            }
        if (events.none { Path.of(it.path) in tableFiles }) return
        // VFS events are delivered in a write action, which publishing requires
        project.publishGlobalSourceOutOfBlockModificationEvent()
        DaemonCodeAnalyzer.getInstance(project).restart("localizer table file changed")
    }
}
