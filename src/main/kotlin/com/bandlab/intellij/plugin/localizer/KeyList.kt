package com.bandlab.intellij.plugin.localizer

private val SEPARATORS = Regex("[,\\s]+")

/**
 * Splits a pasted list of keys on commas and/or whitespace (including newlines), trims, drops
 * blanks, and de-duplicates while preserving order. Lets a user paste keys in whatever shape is
 * convenient — `a, b`, `a b c`, or one per line.
 */
fun parseKeyList(raw: String): List<String> =
    raw.split(SEPARATORS).filter { it.isNotBlank() }.distinct()
