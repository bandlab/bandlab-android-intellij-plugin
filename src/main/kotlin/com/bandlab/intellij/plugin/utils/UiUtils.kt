package com.bandlab.intellij.plugin.utils

import com.intellij.openapi.observable.properties.ObservableProperty
import com.intellij.ui.dsl.builder.Row

fun Row.visibleIf(property: ObservableProperty<Boolean>): Row = apply {
    visible(property.get())
    property.afterChange {
        visible(it)
    }
}