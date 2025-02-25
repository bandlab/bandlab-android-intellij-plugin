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
        import com.bandlab.common.android.di.HasServiceProvider
        import com.bandlab.common.android.pager.screen.di.componentCreator
        import com.bandlab.uikit.compose.pager.Page
        import javax.inject.Inject

        @ContributesComponent(appDependencies = ${name}Page.ServiceProvider::class)
        class ${name}Page @Inject constructor(context: Context) : Page<${name}ViewModel>, HasServiceProvider {

            private val component by componentCreator(context, Dagger${name}PageComponent.factory())

            @Composable
            override fun Content(vm: ${name}ViewModel) {
                
            }

            override fun <T> resolve(): T = HasServiceProvider.resolveFrom(component)

            interface ServiceProvider {
                
            }
        }
        
    """.trimIndent()


    fun createPageWithContributesInjector(): String = """
        package $filePackage
        
        import androidx.compose.runtime.Composable
        import com.bandlab.common.android.di.ContributesInjector
        import com.bandlab.common.di.FeatureGraph
        import com.bandlab.uikit.compose.pager.Page
        import javax.inject.Inject
        
        @ContributesInjector(FeatureGraph::class)
        class ${name}Page @Inject constructor() : Page<${name}ViewModel> {
        
            @Composable
            override fun Content(vm: ${name}ViewModel) {
                
            }
        }
        
    """.trimIndent()

    fun createViewModel(): String = """
        package $filePackage
        
        import javax.inject.Inject
        
        class ${name}ViewModel @Inject constructor(
            
        ) {
            
        }
    """.trimIndent()
}