package com.bandlab.intellij.plugin.template

class ComposableTemplateBuilder(
    private val name: String,
    private val filePackage: String,
) {
    fun buildFile(): String = """
        package $filePackage

        import androidx.compose.runtime.Composable
        import androidx.compose.runtime.Immutable
        import androidx.compose.ui.Modifier
        import com.bandlab.uikit.compose.ComposePreviewApi
        import com.bandlab.uikit.compose.PreviewDayNight

        @Immutable
        data class ${name}State(
            // TODO: Params you need for your Composable state
        ) {

            companion object {

                @ComposePreviewApi
                fun preview(): ${name}State = ${name}State(
                    // TODO: Default values for the state preview
                )
            }
        }

        @Composable
        internal fun $name(
            state: ${name}State,
            modifier: Modifier = Modifier,
        ) {
            
        }

        @PreviewDayNight
        @Composable
        private fun ${name}_Preview() {
            ${name}(
                state = ${name}State.preview(),
            )
        }
        
    """.trimIndent()

}
