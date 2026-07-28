package com.what386.waterfall.widgets

data class WidgetStack(
    val widgetIds: List<Int>,
    val id: String = "legacy-${widgetIds.firstOrNull() ?: "empty"}",
)

fun encodeWidgetStacks(stacks: List<WidgetStack>): String =
    stacks
        .map { stack -> stack.copy(widgetIds = stack.widgetIds.distinct()) }
        .filter { stack -> stack.widgetIds.isNotEmpty() }
        .joinToString(";") { stack ->
            "${stack.id}:${stack.widgetIds.joinToString(",")}"
        }

fun decodeWidgetStacks(raw: String?): List<WidgetStack> =
    raw
        ?.split(";")
        ?.map { rawStack ->
            val (rawId, rawWidgetIds) =
                if (":" in rawStack) {
                    rawStack.substringBefore(":") to rawStack.substringAfter(":")
                } else {
                    null to rawStack
                }
            val widgetIds =
                rawWidgetIds
                    .split(",")
                    .mapNotNull { rawIdValue -> rawIdValue.toIntOrNull() }
                    .distinct()
            WidgetStack(
                widgetIds = widgetIds,
                id =
                    rawId?.takeIf { it.isNotBlank() }
                        ?: "legacy-${widgetIds.firstOrNull() ?: "empty"}",
            )
        }?.filter { stack -> stack.widgetIds.isNotEmpty() }
        ?: emptyList()

fun widgetStacksFromWidgetIds(widgetIds: List<Int>): List<WidgetStack> =
    widgetIds.distinct().map { widgetId -> WidgetStack(listOf(widgetId)) }
