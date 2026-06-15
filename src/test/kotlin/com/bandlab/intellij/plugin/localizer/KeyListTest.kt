package com.bandlab.intellij.plugin.localizer

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class KeyListTest {

    @Test
    fun splitsOnCommasSpacesAndNewlines() {
        assertThat(parseKeyList("a, b\n c\td,e"))
            .containsExactly("a", "b", "c", "d", "e").inOrder()
    }

    @Test
    fun trimsAndDropsBlanks() {
        assertThat(parseKeyList("  , , a ,\n\n  b , "))
            .containsExactly("a", "b").inOrder()
    }

    @Test
    fun dedupesPreservingFirstOccurrenceOrder() {
        assertThat(parseKeyList("a b a c b"))
            .containsExactly("a", "b", "c").inOrder()
    }

    @Test
    fun blankInputIsEmpty() {
        assertThat(parseKeyList("   \n \t ")).isEmpty()
    }
}
