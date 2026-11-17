// Copyright 2026 BandLab Singapore Pte Ltd
// SPDX-License-Identifier: Apache-2.0
// bandlab-localizer's compiler plugin compiled against one IDE's own Kotlin compiler: public Kotlin
// releases of the same version are not binary compatible with it. One project per IDE, see
// settings.gradle.kts; the jar is named after the IDE's compiler version, which
// TextResCompilerPluginProvider matches at runtime.
import java.util.zip.ZipFile
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("org.jetbrains.intellij.platform.base")
    id("org.jetbrains.kotlin.jvm")
}

val ides =
    mapOf(
        // The IDE this plugin's tests run on
        "intellij-idea" to (IntelliJPlatformType.IntellijIdeaUltimate to "2025.3.3"),
        // Latest stable and next Android Studio
        "android-studio-stable" to (IntelliJPlatformType.AndroidStudio to "2026.2.2.9"),
        "android-studio-next" to (IntelliJPlatformType.AndroidStudio to "2026.2.3.4"),
    )
val (ideType, ideVersion) = ides.getValue(name.removePrefix("localizer-compiler-plugin-"))
val localizer = file(providers.gradleProperty("localizerSources").get())

repositories {
    mavenCentral()
    intellijPlatform { defaultRepositories() }
}

dependencies {
    intellijPlatform { create(ideType, ideVersion) }
    compileOnly(kotlin("stdlib"))
    compileOnly(
        files(
            configurations.named("intellijPlatformDependency").map { ide ->
                fileTree(ide.singleFile) { include("lib/*.jar", "plugins/Kotlin/lib/*.jar") }
            },
        ),
    )
}

sourceSets.main {
    kotlin.srcDirs(
        localizer.resolve("compiler-plugin/src/main/kotlin"),
        localizer.resolve("text-keys/src/main/kotlin"),
    )
    resources.srcDir(localizer.resolve("compiler-plugin/src/main/resources"))
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        // Same in every IDE project, so IDEs whose compilers link the same API give equal jars
        moduleName = "localizer-compiler-plugin"
        jvmTarget = JvmTarget.JVM_17
        optIn.addAll(
            "org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi",
            "org.jetbrains.kotlin.fir.extensions.ExperimentalTopLevelDeclarationsGenerationApi",
            "org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI",
        )
    }
}

val ideCompilerVersion =
    configurations.named("intellijPlatformDependency").map { ide ->
        ZipFile(ide.singleFile.resolve("plugins/Kotlin/lib/kotlinc.kotlin-compiler-common.jar")).use {
            it.getInputStream(it.getEntry("META-INF/compiler.version")).reader().readText().trim()
        }
    }
val localizerCommit =
    providers.exec { commandLine("git", "-C", localizer.path, "rev-parse", "--short", "HEAD") }
        .standardOutput
        .asText
        .map { it.trim() }

tasks.jar {
    archiveFileName = ideCompilerVersion.map { "localizer-compiler-plugin-$it.jar" }
    manifest { attributes("Localizer-Commit" to localizerCommit) }
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}
