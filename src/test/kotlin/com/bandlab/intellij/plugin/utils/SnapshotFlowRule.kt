package com.bandlab.intellij.plugin.utils

import androidx.compose.runtime.snapshots.ObserverHandle
import androidx.compose.runtime.snapshots.Snapshot
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Snapshot flow doesn't work in unit test because there is composable rendered.
 * Include this flow to let snapshotFlow emits in unit test.
 */
class SnapshotFlowRule : TestWatcher() {

    private var observerHandle: ObserverHandle? = null

    override fun starting(description: Description?) {
        if (observerHandle == null) {
            observerHandle = Snapshot.registerGlobalWriteObserver {
                Snapshot.sendApplyNotifications()
            }
        }
    }

    override fun finished(description: Description?) {
        observerHandle?.dispose()
        observerHandle = null
    }
}