package bpm.common.utils

import kotlin.math.max

fun <T : Any> List<T>.toFormattedString(maxElementsPerLine: Int = this.size / 5): String {
    if (isEmpty()) return "[]"

    // First Pass: Find the longest string representation for each column
    val maxLengths = IntArray(minOf(maxElementsPerLine, size)) { 0 }
    for (i in indices) {
        val elementLength = this[i].toString().length
        maxLengths[i % maxElementsPerLine] = max(maxLengths[i % maxElementsPerLine], elementLength)
    }

    // Creating the horizontal separator line
    val separatorLine = StringBuilder().apply {
        append("    \u2554") // Top left corner
        maxLengths.forEachIndexed { index, length ->
            append("\u2550".repeat(length + 2)) // Horizontal line
            append(if (index < maxLengths.lastIndex) "\u2566" else "\u2557") // T connector or top right corner
        }
    }

    // Second Pass: Build the formatted string
    val builder = StringBuilder()
    builder.append("\n").append(separatorLine).append("\n    \u2551 ") // Left wall

    for (i in indices) {
        val element = this[i].toString()
        val column = i % maxElementsPerLine
        builder.append(element.padEnd(maxLengths[column], ' '))

        if (column < maxElementsPerLine - 1) {
            builder.append(" \u2551 ") // Vertical separator
        } else {
            builder.append(" \u2551\n") // Right wall
            if (i < size - 1) {
                builder.append("    \u2560") // Left T connector
                maxLengths.forEachIndexed { index, length ->
                    builder.append("\u2550".repeat(length + 2))
                    builder.append(if (index < maxLengths.lastIndex) "\u256C" else "\u2563") // Cross connector or right T connector
                }
                builder.append("\n    \u2551 ") // Left wall for the next line
            }
        }
    }

    // Bottom border logic
    val bottomSeparatorLine = StringBuilder()
    bottomSeparatorLine.append("    \u255A") // Bottom left corner

    val elementsInLastRow = if (size % maxElementsPerLine == 0) maxElementsPerLine else size % maxElementsPerLine

    for (index in maxLengths.indices) {
        val length = maxLengths[index]
        bottomSeparatorLine.append("\u2550".repeat(length + 2)) // Horizontal line

        if (index < maxLengths.lastIndex) {
            bottomSeparatorLine.append("\u2569") // Bottom T connector
        } else {
            bottomSeparatorLine.append("\u255D") // Bottom right corner
        }
    }

    // Add padding for missing elements in the last row
    if (elementsInLastRow < maxElementsPerLine) {
        for (index in elementsInLastRow until maxElementsPerLine - 1) {
            val length = maxLengths[index]
            builder.append(" ".repeat(length + 2)).append("\u2551 ")
        }
        builder.append(" ".repeat(maxLengths.last() + 1)).append("\u2551\n")
    }

    builder.append(bottomSeparatorLine)
    builder.append("\n")
    return builder.toString()
}