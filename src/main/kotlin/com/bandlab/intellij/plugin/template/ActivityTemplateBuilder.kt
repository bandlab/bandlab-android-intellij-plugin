package com.bandlab.intellij.plugin.template

class ActivityTemplateBuilder(
    private val name: String,
    private val filePackage: String,
) {
    fun createActivity(): String = """
        package $filePackage
        
        import android.content.Context
        import android.content.Intent
        import android.os.Bundle
        import com.bandlab.android.common.activity.CommonActivity
        import com.bandlab.android.common.activity.CommonActivityDependencies
        import com.bandlab.android.common.activity.graphCreator
        import com.bandlab.android.common.ui.WindowInsetsType
        import com.bandlab.android.common.ui.setContent
        import com.bandlab.common.android.di.ContributesComponent
        import com.bandlab.common.android.di.HasServiceProvider
        import com.bandlab.common.android.pager.screen.dialog.toPageContainerState
        import com.bandlab.navigation.android.activityIntent
        import com.bandlab.navigation.android.getObject
        import com.bandlab.navigation.android.putObject
        import com.bandlab.uikit.compose.page.container.PageContainer
        import dev.zacsweers.metro.Inject
        import dev.zacsweers.metro.createGraphFactory

        @ContributesComponent(appDependencies = ${name}Activity.ServiceProvider::class)
        class ${name}Activity : CommonActivity<${name}Page.Params>(), HasServiceProvider {

            @Inject override lateinit var dependencies: CommonActivityDependencies
            @Inject private lateinit var page: ${name}Page

            private val graph by graphCreator(createGraphFactory<${name}ActivityGraph.Factory>())

            override fun parseRequiredParams(bundle: Bundle): ${name}Page.Params =
                bundle.getObject(${name}Page.Params.serializer())

            override fun onCreate() {
                val state = page.toPageContainerState(param = params)
                setContent(windowInsets = WindowInsetsType.Scrolling) {
                    PageContainer(state = state)
                }
            }

            override fun <T> resolve(): T = HasServiceProvider.resolveFrom(graph)

            interface ServiceProvider

            companion object {
                fun buildIntent(
                    context: Context,
                    //TODO: Your params
                ): Intent = activityIntent<${name}Activity>(context) {
                    putObject(
                        obj = ${name}Page.Params(),
                        serializer = ${name}Page.Params.serializer()
                    )
                }
            }
        }
        
    """.trimIndent()

    fun createPage(): String = """
        package $filePackage
        
        import androidx.activity.ComponentActivity
        import androidx.compose.runtime.Composable
        import androidx.savedstate.SavedState
        import com.bandlab.common.android.di.ContributesComponent
        import com.bandlab.common.android.pager.screen.ParamPage
        import com.bandlab.common.android.pager.screen.di.HasPageServiceProvider
        import com.bandlab.common.android.pager.screen.di.graphCreator
        import com.bandlab.navigation.android.getObjectOrNull
        import dev.zacsweers.metro.Inject
        import dev.zacsweers.metro.createGraphFactory
        import kotlinx.serialization.Serializable

        @ContributesComponent(appDependencies = ${name}Page.ServiceProvider::class)
        @Inject
        class ${name}Page(activity: ComponentActivity) : ParamPage<${name}ViewModel, ${name}Page.Params>, HasPageServiceProvider {

            override val graphCreator = graphCreator(activity, createGraphFactory<${name}PageGraph.Factory>())

            override fun parseParam(savedState: SavedState): Params? =
                savedState.getObjectOrNull(Params.serializer())

            @Composable
            override fun Content(viewModel: ${name}ViewModel) {
                
            }

            @Serializable
            data class Params(
                //TODO: Your params
                val foo: String,
            )

            interface ServiceProvider {
                
            }
        }

    """.trimIndent()

    fun createViewModel(): String = """
        package $filePackage
        
        import dev.zacsweers.metro.Inject
        import kotlinx.coroutines.flow.Flow

        @Inject
        class ${name}ViewModel(
            //TODO: Keep one of these two:
            // - Use paramFlow for the updates from activity's onNewIntent (ex: singleTop), the flow emits initial param, too.
            // - Use initialParam if you want only the initial param when the screen is opened.
            paramFlow: Flow<${name}Page.Params>,
            initialParam: ${name}Page.Params,
        ) {
            
        }

    """.trimIndent()

    fun createManifest(): String = """
        <?xml version="1.0" encoding="utf-8"?>
        <manifest xmlns:android="http://schemas.android.com/apk/res/android">
            
            <application>
                <activity
                    android:name=".${name}Activity"
                    android:configChanges="colorMode|density|fontScale|keyboard|keyboardHidden|layoutDirection|locale|mcc|mnc|navigation|orientation|screenLayout|screenSize|smallestScreenSize|touchscreen|uiMode|fontWeightAdjustment" />
            </application>
        </manifest>
    """.trimIndent()
}