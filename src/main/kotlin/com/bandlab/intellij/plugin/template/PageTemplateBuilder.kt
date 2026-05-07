package com.bandlab.intellij.plugin.template

class PageTemplateBuilder(
    private val name: String,
    private val filePackage: String,
) {
    fun createPageWithContributesComponent(): String = """
        package $filePackage
        
        import android.content.Context
        import androidx.compose.runtime.Composable
        import com.bandlab.common.android.di.ContributesComponent
        import com.bandlab.common.android.pager.screen.di.HasPageServiceProvider
        import com.bandlab.common.android.pager.screen.di.graphCreator
        import com.bandlab.uikit.api.page.Page
        import dev.zacsweers.metro.Inject
        import dev.zacsweers.metro.createGraphFactory       

        @ContributesComponent(appDependencies = ${name}Page.ServiceProvider::class)
        @Inject
        class ${name}Page(context: Context) : Page<${name}ViewModel>, HasPageServiceProvider {

            override val graphCreator = graphCreator(context, createGraphFactory<${name}PageGraph.Factory>())

            @Composable
            override fun Content(viewModel: ${name}ViewModel) {
                
            }

            interface ServiceProvider {
                
            }
        }
        
    """.trimIndent()

    fun createViewModel(): String = """
        package $filePackage
        
        import dev.zacsweers.metro.Inject
        
        @Inject
        class ${name}ViewModel(
            
        ) {
            
        }
    """.trimIndent()

    fun createNavKey(): String = """
        package $filePackage
        
        import com.bandlab.models.navigation.GlobalPageNavKey
        import kotlinx.serialization.Serializable

        @Serializable
        data class ${name}NavKey(
            val id: String // TODO: Your params
        ) : GlobalPageNavKey()
    """.trimIndent()

    fun createNavEntry(): String = """
        package $filePackage
        
        import androidx.activity.ComponentActivity
        import com.bandlab.navigation.ui.GlobalPageNavEntry
        import com.bandlab.uikit.api.page.Page
        import dev.zacsweers.metro.AppScope
        import dev.zacsweers.metro.ContributesTo
        import dev.zacsweers.metro.Inject

        @ContributesTo(AppScope::class)
        class ${name}NavEntry @Inject constructor() : GlobalPageNavEntry<${name}NavKey> {
            override val keyInfo = GlobalPageNavEntry.KeyInfo(${name}NavKey::class, ${name}NavKey.serializer())
            override fun getPage(key: ${name}NavKey, activity: ComponentActivity): Page<*> = ${name}Page(activity)
        }
    """.trimIndent()
}
