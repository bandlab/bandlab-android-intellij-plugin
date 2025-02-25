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
        import com.bandlab.android.common.activity.CommonActivity2
        import com.bandlab.android.common.activity.CommonActivityDependencies
        import com.bandlab.android.common.activity.componentCreator
        import com.bandlab.common.android.di.ContributesComponent
        import com.bandlab.common.android.di.HasServiceProvider
        import com.bandlab.navigation.android.activityIntent
        import com.bandlab.uikit.compose.activity.WindowInsetsType
        import com.bandlab.uikit.compose.activity.setContent
        import javax.inject.Inject
        
        @ContributesComponent(appDependencies = ${name}Activity.ServiceProvider::class)
        class ${name}Activity : CommonActivity2<Unit>(), HasServiceProvider {
            
            @Inject override lateinit var dependencies: CommonActivityDependencies
            @Inject lateinit var viewModel: ${name}ViewModel
            
            private val component by componentCreator(Dagger${name}ActivityComponent.factory())
            
            override fun parseRequiredParams(bundle: Bundle) = Unit
            
            override fun onCreate() {
                setContent(windowInsets = WindowInsetsType.Scrolling) {
                    
                }
            }
            
            override fun <T> resolve(): T = HasServiceProvider.resolveFrom(component)
            
            interface ServiceProvider {
                
            }
            
            companion object {
                fun buildIntent(context: Context): Intent {
                    return activityIntent<${name}Activity>(context)
                }
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

    fun createManifest(): String = """
        <?xml version="1.0" encoding="utf-8"?>
        <manifest xmlns:android="http://schemas.android.com/apk/res/android">
            
            <application>
                <activity
                    android:name=".${name}Activity"
                    android:configChanges="colorMode|density|fontScale|keyboard|keyboardHidden|layoutDirection|mcc|mnc|navigation|orientation|screenLayout|screenSize|smallestScreenSize|touchscreen|fontWeightAdjustment" />
            </application>
        </manifest>
    """.trimIndent()
}