package com.bandlab.intellij.plugin.template

class NavKeyTemplateBuilder(
    private val name: String,
    private val filePackage: String,
) {

    fun createNavKey(): String = """
        package $filePackage
        
        import com.bandlab.models.navigation.GlobalPageNavKey
        import kotlinx.serialization.Serializable

        @Serializable
        data class ${name}Key(
            val id: String // TODO: Your params
        ) : GlobalPageNavKey
    """.trimIndent()

    fun createNavEntry(): String = """
        package $filePackage
        
        import androidx.activity.ComponentActivity
        import com.bandlab.navigation.ui.GlobalPageNavEntry
        import com.bandlab.uikit.api.page.Page
        import dev.zacsweers.metro.AppScope
        import dev.zacsweers.metro.ContributesIntoSet
        import dev.zacsweers.metro.Inject

        @ContributesIntoSet(scope = AppScope::class)
        internal object ${name}NavEntry : GlobalPageNavEntry<${name}Key> {
            override val keyInfo = GlobalPageNavEntry.KeyInfo(${name}Key::class, ${name}Key.serializer())
            override fun getPage(key: ${name}Key, activity: ComponentActivity): Page<*> = ${name}Page(activity)
        }
    """.trimIndent()
}