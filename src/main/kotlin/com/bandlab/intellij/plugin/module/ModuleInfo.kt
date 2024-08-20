package com.bandlab.intellij.plugin.module

private val snakeRegex = "-[a-zA-Z]".toRegex()

data class ModuleInfo(
    // Ex: /user/profile/edit-screen
    val path: String,
) {

    // Ex: :user:profile:edit-screen
    val reference: String
        get() = path.replace('/', ':')

    // Ex: projects.user.profile.editScreen
    val projectAccessorReference: String
        get() = "projects" + snakeRegex.replace(path.replace('/', '.')) {
            it.value.replace("-", "").uppercase()
        }

    // Ex: com/bandlab/user/profile/edit/screen
    private val srcPackage: String
        get() = "com/bandlab" + path.replace('-', '/')

    // Ex: com.bandlab.user.profile.edit.screen
    val packageToImport: String
        get() = srcPackage.replace('/', '.')

    // Ex: /user/profile/edit-screen/src/main/kotlin/com/bandlab/user/profile/edit/screen
    val filesPath: String
        get() = "$path/src/main/kotlin/$srcPackage"

    // Ex: /user/profile/edit-screen/src/testFixtures/kotlin/com/bandlab/user/profile/edit/screen
    val testFixturesPath: String
        get() = "$path/src/testFixtures/kotlin/$srcPackage"
}