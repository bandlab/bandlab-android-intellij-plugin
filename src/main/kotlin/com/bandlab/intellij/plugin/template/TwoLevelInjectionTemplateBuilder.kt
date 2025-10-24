package com.bandlab.intellij.plugin.template

class TwoLevelInjectionTemplateBuilder(
    private val name: String,
    private val filePackage: String,
) {
    fun buildInterface(): String = """
        package $filePackage
        
        import dev.zacsweers.metro.Inject
        import kotlinx.coroutines.CoroutineScope

        //TODO: Put this in your abstraction module, either :api or :ui
        interface $name {

            //TODO: things you want to expose

            @Inject
            class Factory(
                private val coroutineScope: CoroutineScope,
                private val appLevelFactory: AppLevelFactory,
            ) {
                fun create(
                    //TODO: Params you need for this dependency
                ): $name = appLevelFactory.create(
                    coroutineScope = coroutineScope
                )
            }

            interface AppLevelFactory {
                fun create(
                    coroutineScope: CoroutineScope,
                ): $name
            }
        }
        
    """.trimIndent()

    fun buildImpl(): String = """
        package $filePackage
        
        import dev.zacsweers.metro.AppScope
        import dev.zacsweers.metro.Assisted
        import dev.zacsweers.metro.AssistedFactory
        import dev.zacsweers.metro.AssistedInject
        import dev.zacsweers.metro.ContributesBinding
        import kotlinx.coroutines.CoroutineScope

        //TODO: Put this in the :impl, and remember to wire your impl to :app
        @AssistedInject
        class ${name}Impl(
            @Assisted private val coroutineScope: CoroutineScope,
        ) : $name {

            //TODO: Your business logic

            @ContributesBinding(AppScope::class)
            @AssistedFactory
            interface Factory : ${name}.AppLevelFactory {
                override fun create(
                    coroutineScope: CoroutineScope,
                ): ${name}Impl
            }
        }
        
    """.trimIndent()
}