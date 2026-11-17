// Copyright 2026 BandLab Singapore Pte Ltd
// SPDX-License-Identifier: Apache-2.0
package com.bandlab.intellij.plugin.strings.textres

import com.google.common.truth.Truth.assertThat
import com.intellij.find.findUsages.PsiElement2UsageTargetAdapter
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiElement
import com.intellij.psi.xml.XmlTag
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.io.File
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import org.jetbrains.kotlin.idea.compiler.configuration.KotlinCommonCompilerArgumentsHolder

/**
 * The IDE side of bandlab-localizer's compiler plugin against the real IDE: the module's compiler
 * arguments name the plugin the way Gradle sync does, and `Strings.<key>` must resolve from
 * strings.xml.
 */
class TextResCompilerPluginIdeTest : BasePlatformTestCase() {

    private lateinit var stringsXml: File

    override fun setUp() {
        super.setUp()
        // The light project's basePath is shared across methods
        val root = File(requireNotNull(project.basePath), "textres").apply { deleteRecursively() }
        stringsXml = File(root, "strings/res/values/strings.xml")
        writeStrings(
            """<string name="save">Save</string><string name="welcome">Welcome, %s!</string>""",
        )
        // What Gradle sync exports: the user's compiler plugin jar and the table options. The jar
        // only declares the registrar, so the members resolve only if the bundled build replaced
        // it.
        val userJar = File(root, "gradle-cache/compiler-plugin-0.0.1-2.4.21.jar")
        userJar.parentFile.mkdirs()
        JarOutputStream(userJar.outputStream()).use {
            it.putNextEntry(
                JarEntry(
                    "META-INF/services/org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar",
                ),
            )
            it.write(
                "com.bandlab.localizer.text.compiler.TextResCompilerPluginRegistrar\n"
                    .toByteArray(),
            )
        }
        KotlinCommonCompilerArgumentsHolder.getInstance(project).update {
            pluginClasspaths = arrayOf(userJar.path)
            pluginOptions =
                arrayOf(
                    "plugin:$TEXT_RES_PLUGIN_ID:table=com.example.strings;Strings;Plurals",
                    "plugin:$TEXT_RES_PLUGIN_ID:tableFile=com.example.strings;${stringsXml.path}",
                )
        }
        myFixture.addFileToProject(
            "com/bandlab/localizer/text/TextRes.kt",
            "package com.bandlab.localizer.text\n\ninterface TextRes",
        )
        // The table module's runtime object. K2 resolves a reference to a generated member to its
        // package, which is an invalid PSI element when no class declares that package.
        myFixture.addFileToProject(
            "com/example/strings/StringsTable.kt",
            "package com.example.strings\n\nobject StringsTable",
        )
    }

    override fun tearDown() {
        try {
            KotlinCommonCompilerArgumentsHolder.getInstance(project).update {
                pluginClasspaths = emptyArray()
                pluginOptions = emptyArray()
            }
        } finally {
            super.tearDown()
        }
    }

    fun testKeysResolveThroughTheBundledCompilerPlugin() {
        myFixture.configureByText(
            "Usage.kt",
            """
            import com.example.strings.Strings

            val a = Strings.save
            val b = Strings.welcome(Strings.save)
            val c = Strings.nope
            """
                .trimIndent(),
        )
        assertThat(errors()).containsExactly("nope")
    }

    fun testCompletionOffersEveryKeyOfTheTable() {
        myFixture.configureByText(
            "Usage.kt",
            "import com.example.strings.Strings\n\nval a = Strings.<caret>",
        )
        myFixture.completeBasic()
        assertThat(myFixture.lookupElementStrings).containsAtLeast("save", "welcome")
    }

    fun testGoToDeclarationOpensTheStringsXmlEntry() {
        myFixture.configureByText(
            "Usage.kt",
            "import com.example.strings.Strings\n\nval a = Strings.sa<caret>ve",
        )
        val offset = myFixture.caretOffset
        // Off the EDT, like the platform's navigation, so the receiver goes through the Analysis
        // API
        val targets =
            ApplicationManager.getApplication().executeOnPooledThread<Array<PsiElement>?> {
                runReadAction {
                    TextResGotoDeclarationHandler()
                        .getGotoDeclarationTargets(
                            myFixture.file.findElementAt(offset),
                            offset,
                            myFixture.editor,
                        )
                }
            }
        val tag = PlatformTestUtil.waitForFuture(targets, 30_000)?.single() as XmlTag
        assertThat(tag.containingFile.virtualFile.path).isEqualTo(stringsXml.path)
        assertThat(tag.getAttributeValue("name")).isEqualTo("save")
    }

    fun testFindUsagesOfAKeyListsItsReferences() {
        myFixture.configureByText(
            "Usage.kt",
            """
            import com.example.strings.Strings

            val a = Strings.sa<caret>ve
            val b = Strings.welcome(Strings.save)
            val save = "save"
            """
                .trimIndent(),
        )
        val target =
            TextResUsageTargetProvider()
                .getTargets(myFixture.editor, myFixture.file)
                ?.single()
                .let { it as PsiElement2UsageTargetAdapter }
                .element
        assertThat((target as XmlTag).getAttributeValue("name")).isEqualTo("save")

        val usages = myFixture.findUsages(target).map { it.element?.text }

        assertThat(usages).containsExactly("save", "save")
    }

    fun testAddStringFixOffersAKeyMissingFromTheTable() {
        val manifest =
            File(requireNotNull(project.basePath), "localizer/bandlab-localizer-config.toml")
        manifest.parentFile.mkdirs()
        manifest.writeText(
            "api-key = \"k\"\nproject-id = \"1\"\n\n[[file]]\nlocale = \"en\"\npath = \"textres/strings/res/values/strings.xml\"\n",
        )
        LocalFileSystem.getInstance().refreshAndFindFileByIoFile(manifest)
        try {
            myFixture.configureByText(
                "Usage.kt",
                "import com.example.strings.Strings\n\nval a = Strings.no<caret>pe",
            )
            myFixture.doHighlighting()
            assertThat(myFixture.availableIntentions.map { it.text })
                .contains("Localizer: Add string \"nope\"")
        } finally {
            manifest.parentFile.deleteRecursively()
        }
    }

    fun testKeyAddedToStringsXmlResolvesWithoutSync() {
        myFixture.configureByText(
            "Usage.kt",
            "import com.example.strings.Strings\n\nval a = Strings.later",
        )
        assertThat(errors()).containsExactly("later")

        writeStrings("""<string name="save">Save</string><string name="later">Later</string>""")

        assertThat(errors()).isEmpty()
    }

    private fun errors(): List<String> =
        myFixture.doHighlighting(HighlightSeverity.ERROR).map { it.text }

    private fun writeStrings(entries: String) {
        stringsXml.parentFile.mkdirs()
        stringsXml.writeText("<resources>$entries</resources>\n")
        val file = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(stringsXml)!!
        WriteAction.runAndWait<Throwable> { file.refresh(false, false) }
    }
}
