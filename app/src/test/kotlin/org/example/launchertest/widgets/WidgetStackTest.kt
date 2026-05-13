package org.example.launchertest.widgets

import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetStackTest {
    @Test
    fun encodesAndDecodesWidgetStacks() {
        val stacks = listOf(
            WidgetStack(listOf(1, 2)),
            WidgetStack(listOf(3)),
        )

        assertEquals("1,2;3", encodeWidgetStacks(stacks))
        assertEquals(stacks, decodeWidgetStacks("1,2;3"))
    }

    @Test
    fun dropsEmptyAndDuplicateWidgetIds() {
        val stacks = listOf(
            WidgetStack(listOf(1, 1, 2)),
            WidgetStack(emptyList()),
        )

        assertEquals("1,2", encodeWidgetStacks(stacks))
        assertEquals(listOf(WidgetStack(listOf(1, 2))), decodeWidgetStacks("1,1,2;"))
    }

    @Test
    fun migratesFlatWidgetIdsToSingleWidgetStacks() {
        assertEquals(
            listOf(
                WidgetStack(listOf(4)),
                WidgetStack(listOf(5)),
            ),
            widgetStacksFromWidgetIds(listOf(4, 5, 4)),
        )
    }
}
