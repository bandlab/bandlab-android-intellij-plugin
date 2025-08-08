package com.bandlab.intellij.plugin.module

enum class ModuleValidationError {
    ModuleNameEmpty,
    ModuleNameInvalidChar,
    ModuleNameShouldStartWithColon,
    ModuleNameEndsWithColon,

    ApiModuleExist,
    ImplModuleExist,
    ScreenModuleExist,
    UiModuleExist;

    val isNameError: Boolean
        get() = when (this) {
            ModuleNameEmpty, ModuleNameInvalidChar, ModuleNameShouldStartWithColon, ModuleNameEndsWithColon -> true
            ApiModuleExist, ImplModuleExist, ScreenModuleExist, UiModuleExist -> false
        }

    val errorMessage: String
        get() = when (this) {
            ModuleNameEmpty -> "Module name is empty"
            ModuleNameInvalidChar -> "Invalid char, only lowercase, '-' and ':' are allowed"
            ModuleNameShouldStartWithColon -> "Module name should start with ':'"
            ModuleNameEndsWithColon -> "Module name shouldn't end with ':'"
            ApiModuleExist -> ":api module already exist"
            ImplModuleExist -> ":impl module already exist"
            ScreenModuleExist -> ":screen module already exist"
            UiModuleExist -> ":ui module already exist"
        }

    companion object {
        fun Set<ModuleValidationError>.errorMessageOrNull(
            variant: BandLabModuleVariant,
            parentModule: CharSequence,
        ): String? = when (variant) {
            is BandLabModuleVariant.Api -> if (contains(ApiModuleExist)) "$parentModule${ApiModuleExist.errorMessage}" else null
            is BandLabModuleVariant.Impl -> if (contains(ImplModuleExist)) "$parentModule${ImplModuleExist.errorMessage}" else null
            is BandLabModuleVariant.Screen -> if (contains(ScreenModuleExist)) "$parentModule${ScreenModuleExist.errorMessage}" else null
            is BandLabModuleVariant.Ui -> if (contains(UiModuleExist)) "$parentModule${UiModuleExist.errorMessage}" else null
        }
    }
}