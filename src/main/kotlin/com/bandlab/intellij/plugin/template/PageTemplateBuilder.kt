package com.bandlab.intellij.plugin.template

class PageTemplateBuilder(
    private val name: String,
    private val filePackage: String,
    includeNavKey: Boolean,
) {

    private val pageType = if (includeNavKey) {
        "ParamPage<${name}ViewModel, ${name}Key>"
    } else {
        "Page<${name}ViewModel>"
    }

    private val pageImport = if (includeNavKey) {
        "com.bandlab.common.android.pager.screen.ParamPage"
    } else {
        "com.bandlab.uikit.api.page.Page"
    }

    private val vmParam = if (includeNavKey) "key: ${name}Key" else ""

    fun createPage(): String = """
        package $filePackage
        
        import android.content.Context
        import androidx.compose.runtime.Composable
        import com.bandlab.common.android.pager.screen.di.HasPageServiceProvider
        import com.bandlab.common.android.pager.screen.di.graphCreator
        import com.bandlab.metro.station.MetroStation
        import $pageImport
        import dev.zacsweers.metro.Inject
        import dev.zacsweers.metro.createGraphFactory       

        @MetroStation(appDependencies = ${name}Page.ServiceProvider::class)
        @Inject
        class ${name}Page(context: Context) : $pageType {

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
            $vmParam
        ) {
            
        }
    """.trimIndent()
}