package com.what386.waterfall.widgets

data class WidgetStack(
    val widgetIds: List<Int>,
)

fun encodeWidgetStacks(stacks: List<WidgetStack>): String {
    return stacks
        .map { stack -> stack.widgetIds.distinct() }
        .filter { widgetIds -> widgetIds.isNotEmpty() }
        .joinToString(";") { widgetIds -> widgetIds.joinToString(",") }
}

fun decodeWidgetStacks(raw: String?): List<WidgetStack> {
    return raw
        ?.split(";")
        ?.map { rawStack ->
            WidgetStack(
                widgetIds = rawStack
                    .split(",")
                    .mapNotNull { rawId -> rawId.toIntOrNull() }
                    .distinct(),
            )
        }
        ?.filter { stack -> stack.widgetIds.isNotEmpty() }
        ?: emptyList()
}

fun widgetStacksFromWidgetIds(widgetIds: List<Int>): List<WidgetStack> {
    return widgetIds.distinct().map { widgetId -> WidgetStack(listOf(widgetId)) }
}
