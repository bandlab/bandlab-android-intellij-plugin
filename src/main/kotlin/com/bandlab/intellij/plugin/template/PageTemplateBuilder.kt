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
        class ${name}Page @Inject constructor(context: Context) : Page<${name}ViewModel>, HasPageServiceProvider {

            override val graphCreator = graphCreator(context, createGraphFactory<${name}PageGraph.Factory>())

            @Composable
            override fun Content(vm: ${name}ViewModel) {
                
            }

            interface ServiceProvider {
                
            }
        }
        
    """.trimIndent()


    fun createPageWithContributesInjector(): String = """
        package $filePackage
        
        import androidx.compose.runtime.Composable
        import com.bandlab.common.android.di.ContributesInjector
        import com.bandlab.common.di.FeatureScope
        import com.bandlab.uikit.api.page.Page
        import dev.zacsweers.metro.Inject
        
        @ContributesInjector(FeatureScope::class)
        class ${name}Page @Inject constructor() : Page<${name}ViewModel> {
        
            @Composable
            override fun Content(vm: ${name}ViewModel) {
                
            }
        }
        
    """.trimIndent()

    fun createViewModel(): String = """
        package $filePackage
        
        import dev.zacsweers.metro.Inject
        
        class ${name}ViewModel @Inject constructor(
            
        ) {
            
        }
    """.trimIndent()
}