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
        import com.bandlab.android.common.activity.componentCreator
        import com.bandlab.common.android.di.ContributesComponent
        import com.bandlab.common.android.di.HasServiceProvider
        import com.bandlab.navigation.android.activityIntent
        import com.bandlab.navigation.android.getObject
        import com.bandlab.navigation.android.putObject
        import com.bandlab.uikit.compose.activity.WindowInsetsType
        import com.bandlab.uikit.compose.activity.setContent
        import dev.zacsweers.metro.Inject
        import dev.zacsweers.metro.createGraphFactory
        import kotlinx.serialization.Serializable
        
        @ContributesComponent(appDependencies = ${name}Activity.ServiceProvider::class)
        class ${name}Activity : CommonActivity<Params>(), HasServiceProvider {
            
            @Inject override lateinit var dependencies: CommonActivityDependencies
            @Inject private lateinit var viewModel: ${name}ViewModel
            
            private val graph by graphCreator(createGraphFactory<${name}ActivityGraph.Factory>())
            
            override fun parseRequiredParams(bundle: Bundle): Params = bundle.getObject(Params.serializer())
            
            override fun onCreate() {
                setContent(windowInsets = WindowInsetsType.Scrolling) {
                    
                }
            }
            
            override fun <T> resolve(): T = HasServiceProvider.resolveFrom(graph)
            
            interface ServiceProvider {
                
            }
            
            @Serializable
            data class Params(TODO("add param or remove it"))
            
            companion object {
                fun buildIntent(context: Context): Intent {
                    return activityIntent<${name}Activity>(context) {
                        putObject(Params(), Params.serializer())
                    }
                }
            }
        }
        
    """.trimIndent()

    fun createViewModel(): String = """
        package $filePackage
        
        import dev.zacsweers.metro.Inject
        
        @Inject
        internal class ${name}ViewModel(
            
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