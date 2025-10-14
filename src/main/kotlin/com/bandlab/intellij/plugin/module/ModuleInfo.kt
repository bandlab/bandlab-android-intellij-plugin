package com.bandlab.intellij.plugin.module

data class ModuleInfo(
    // Ex: /user/profile/edit-screen
    val path: String,
) {

    // Ex: :user:profile:edit-screen
    val reference: String
        get() = path.replace('/', ':')

    // Ex: project(":user:profile:edit-screen")
    val projectAccessorReference: String
        get() = "project(\"$reference\")"

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