// Copyright 2026 BandLab Singapore Pte Ltd
// SPDX-License-Identifier: Apache-2.0
pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

// Rebuilds libs/compiler-plugins, bandlab-localizer's compiler plugin per supported IDE, from a
// bandlab-localizer checkout (a private repository, so CI uses the committed jars):
// ./gradlew updateLocalizerCompilerPlugins -PlocalizerSources=<path to bandlab-localizer>
if (providers.gradleProperty("localizerSources").isPresent) {
    listOf("intellij-idea", "android-studio-stable", "android-studio-next").forEach { ide ->
        include(":localizer-compiler-plugin-$ide")
        project(":localizer-compiler-plugin-$ide").apply {
            projectDir = file("localizer-compiler-plugin/$ide").apply { mkdirs() }
            buildFileName = "../ide.gradle.kts"
        }
    }
}
