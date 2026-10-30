// Copyright 2026 BandLab Singapore Pte Ltd
// SPDX-License-Identifier: Apache-2.0
import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("java")
    alias(libs.plugins.changelog)
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.intellij.platform)
    alias(libs.plugins.kotlin)
    alias(libs.plugins.kover)
    alias(libs.plugins.maven.publish.base)
}

group = providers.gradleProperty("pluginGroup").get()

version = providers.gradleProperty("pluginVersion").get()

// Set the JVM language level used to build the project.
kotlin {
    jvmToolchain(21)
}

// Configure project's dependencies
repositories {
    maven("https://packages.jetbrains.team/maven/p/kpm/public/")
    mavenCentral()
    google()

    // IntelliJ Platform Gradle Plugin Repositories Extension - read more:
    // https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-repositories-extension.html
    intellijPlatform {
        defaultRepositories()
    }
}

// Dependencies are managed with Gradle version catalog - read more:
// https://docs.gradle.org/current/userguide/platforms.html#sub:version-catalog
dependencies {
    intellijPlatform {
        intellijIdea(providers.gradleProperty("targetPlatformVersion"))

        // Plugin Dependencies. Uses `platformBundledPlugins` property from the gradle.properties
        // file for bundled IntelliJ Platform plugins.
        bundledPlugins(providers.gradleProperty("platformBundledPlugins").map { it.split(',') })

        // Plugin Dependencies. Uses `platformPlugins` property from the gradle.properties file for
        // plugin from JetBrains Marketplace.
        plugins(providers.gradleProperty("platformPlugins").map { it.split(',') })

        testFramework(TestFrameworkType.Platform)

        @Suppress("UnstableApiUsage") composeUI()
    }

    // bandlab-localizer config model, vendored as a prebuilt jar (config 3.2.0 release artifact).
    // file() deps carry no transitive metadata, so config's runtime deps are declared explicitly.
    implementation(files("libs/config.jar"))
    implementation(libs.ktoml.file)
    implementation(libs.serialization.json)
    testImplementation(kotlin("stdlib"))
    testImplementation(libs.coroutines.test) {
        // To avoid conflicts with IntelliJ embedded coroutines
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core")
    }
    testImplementation(libs.junit)
    // Used in com.intellij.util.lang.UrlClassLoader
    // see: https://github.com/JetBrains/intellij-platform-gradle-plugin/issues/2127
    testImplementation(libs.opentest4j)
    testImplementation(libs.truth)
    testImplementation(libs.turbine) {
        // To avoid conflicts with IntelliJ embedded coroutines
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core")
    }
}

// Configure IntelliJ Platform Gradle Plugin - read more:
// https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-extension.html
intellijPlatform {
    pluginConfiguration {
        name = providers.gradleProperty("pluginName")
        version = providers.gradleProperty("pluginVersion")

        // Extract the <!-- Plugin description --> section from README.md and provide for the
        // plugin's manifest
        description =
            providers.fileContents(layout.projectDirectory.file("README.md")).asText.map {
                val start = "<!-- Plugin description -->"
                val end = "<!-- Plugin description end -->"

                with(it.lines()) {
                    if (!containsAll(listOf(start, end))) {
                        throw GradleException(
                            "Plugin description section not found in README.md:\n$start ... $end",
                        )
                    }
                    subList(indexOf(start) + 1, indexOf(end))
                        .joinToString("\n")
                        .let(::markdownToHTML)
                }
            }

        val changelog = project.changelog // local variable for configuration cache compatibility
        // Get the latest available change notes from the changelog file
        changeNotes =
            providers.gradleProperty("pluginVersion").map { pluginVersion ->
                with(changelog) {
                    renderItem(
                        (getOrNull(pluginVersion) ?: getUnreleased())
                            .withHeader(false)
                            .withEmptySections(false),
                        Changelog.OutputType.HTML,
                    )
                }
            }

        ideaVersion {
            sinceBuild = providers.gradleProperty("pluginSinceBuild")
        }
    }

    signing {
        certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
        privateKey = providers.environmentVariable("PRIVATE_KEY")
        password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
    }

    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
        // The pluginVersion is based on the SemVer (https://semver.org) and supports pre-release
        // labels, like 2.1.7-alpha.3
        // Specify pre-release label to publish the plugin in a custom Release Channel
        // automatically. Read more:
        // https://plugins.jetbrains.com/docs/intellij/deployment.html#specifying-a-release-channel
        channels =
            providers.gradleProperty("pluginVersion").map {
                listOf(it.substringAfter('-', "").substringBefore('.').ifEmpty { "default" })
            }
    }

    pluginVerification {
        // Our plugin id is "com.bandlab.intellij.plugin", lately jetbrains added a check that
        // plugin id and name don't contain the word intellij and treats it as error.
        freeArgs =
            listOf(
                "-mute",
                "TemplateWordInPluginId,TemplateWordInPluginName,ExperimentalApiUsage",
            )

        ides {
            recommended()
        }
    }
}

// Configure Gradle Changelog Plugin - read more:
// https://github.com/JetBrains/gradle-changelog-plugin
changelog {
    groups.empty()
    repositoryUrl = providers.gradleProperty("pluginRepositoryUrl")
}

// Configure Gradle Kover Plugin - read more: https://github.com/Kotlin/kotlinx-kover#configuration
kover {
    reports.total.xml.onCheck = true
}

// Publish the plugin zip to Maven Central (Sonatype Central Portal) - read more:
// https://vanniktech.github.io/gradle-maven-publish-plugin/central/
// Credentials and the GPG key are Gradle properties: mavenCentralUsername, mavenCentralPassword,
// signingInMemoryKey and signingInMemoryKeyPassword. CI passes them as ORG_GRADLE_PROJECT_ env
// vars.
val pluginSourcesJar =
    tasks.register<Jar>("pluginSourcesJar") {
        description = "Assembles the sources jar published to Maven Central"
        group = "publishing"
        archiveClassifier = "sources"
        from(sourceSets.main.map { it.allSource })
    }

// Central requires a javadoc jar for non-pom packaging; an empty one is accepted.
val pluginJavadocJar =
    tasks.register<Jar>("pluginJavadocJar") {
        description = "Assembles the empty javadoc jar published to Maven Central"
        group = "publishing"
        archiveClassifier = "javadoc"
    }

publishing {
    publications {
        register<MavenPublication>("intellijPlugin") {
            // Publish the JetBrains-signed zip when the certificate is available (always on CI),
            // otherwise the unsigned one, so publishToMavenLocal works without it.
            if (providers.environmentVariable("CERTIFICATE_CHAIN").isPresent) {
                // signedArchiveFile can't be queried before buildPlugin runs, but signing reads
                // artifact files at configuration time, so point at the known output path.
                artifact(
                    layout.buildDirectory.file(
                        "distributions/${providers.gradleProperty("pluginName").get()}-$version-signed.zip",
                    ),
                ) {
                    classifier = null
                    extension = "zip"
                    builtBy(tasks.signPlugin)
                }
            } else {
                artifact(tasks.buildPlugin.flatMap { it.archiveFile }) { extension = "zip" }
            }
            artifact(pluginSourcesJar)
            artifact(pluginJavadocJar)
        }
    }
}

mavenPublishing {
    coordinates("com.bandlab", providers.gradleProperty("pluginName").get(), version.toString())

    pom {
        // Set explicitly; deriving it from the zip artifact resolves signPlugin's output too early.
        packaging = "zip"
        name = "BandLab Android IntelliJ Plugin"
        description = "IntelliJ plugin for BandLab Android project"
        inceptionYear = "2023"
        url = "https://github.com/bandlab/bandlab-android-intellij-plugin"
        licenses {
            license {
                name = "Apache License, Version 2.0"
                url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                distribution = "https://www.apache.org/licenses/LICENSE-2.0.txt"
            }
        }
        developers {
            developer {
                id = "bandlab"
                name = "BandLab Technologies"
                email = "support@bandlab.com"
                url = "https://github.com/bandlab"
                organization = "BandLab Technologies"
                organizationUrl = "https://github.com/bandlab"
            }
        }
        scm {
            url = "https://github.com/bandlab/bandlab-android-intellij-plugin"
            connection = "scm:git:git://github.com/bandlab/bandlab-android-intellij-plugin.git"
            developerConnection =
                "scm:git:ssh://git@github.com/bandlab/bandlab-android-intellij-plugin.git"
        }
    }

    // Releases publish automatically once Central validates the deployment.
    publishToMavenCentral(automaticRelease = true)

    // Central rejects unsigned releases; the publishing workflow fails early without a key.
    if (providers.gradleProperty("signingInMemoryKey").orNull?.isNotBlank() == true) {
        signAllPublications()
    }
}

tasks {
    // Launches a headless IDE to prebuild the settings search index; collides with a running IDE
    // ("only one instance of IDEA") and breaks local builds. Settings stay searchable without it.
    buildSearchableOptions {
        enabled = false
    }

    register("patchReadme") {
        description =
            "Updates the IntelliJ IDEA badge in README.md to match targetPlatformVersion in gradle.properties"
        group = "documentation"

        val targetVersion = providers.gradleProperty("targetPlatformVersion")
        val readmeFile = layout.projectDirectory.file("README.md").asFile

        inputs.property("targetVersion", targetVersion)
        inputs.file(readmeFile)
        outputs.file(readmeFile)

        doLast {
            val content = readmeFile.readText()
            val version = targetVersion.get()
            val updatedContent =
                content.replace(
                    Regex("""(badge/IntelliJ%20IDEA%20Target-)[A-Za-z0-9.\-]+(-[a-z]+\.svg)"""),
                ) { match ->
                    "${match.groupValues[1]}$version${match.groupValues[2]}"
                }
            if (content != updatedContent) {
                readmeFile.writeText(updatedContent)
            }
        }
    }

    named("prepareSandbox") {
        dependsOn("patchReadme")
    }

    test {
        // The bundled Vue.js plugin is pulled into the test sandbox transitively via the Android
        // plugin. During tests that mutate the project's VFS (e.g. adding files), the platform's
        // TypeScript compiler VFS listener eagerly instantiates all JS language services, and Vue's
        // LSP service crashes because its `lib/modules` packaging isn't recognized by
        // PluginManagerCore.getPluginDistDirByClass ("... should be lib directory"). The resulting
        // logged error is turned into a test failure by TestLoggerFactory. We don't rely on any
        // JS/Vue functionality, so suppress the plugin in tests to avoid the spurious failures.
        systemProperty("idea.suppressed.plugins.id", "org.jetbrains.plugins.vue")
    }

    publishPlugin {
        dependsOn(patchChangelog)
    }
}

intellijPlatformTesting {
    runIde {
        register("runIdeForUiTests") {
            task {
                jvmArgumentProviders += CommandLineArgumentProvider {
                    listOf(
                        "-Drobot-server.port=8082",
                        "-Dide.mac.message.dialogs.as.sheets=false",
                        "-Djb.privacy.policy.text=<!--999.999-->",
                        "-Djb.consents.confirmation.enabled=false",
                    )
                }
            }

            plugins {
                robotServerPlugin()
            }
        }
    }
}
