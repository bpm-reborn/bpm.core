package bpm.client.render.markdown


sealed class MarkdownNode {
    class Document(val children: MutableList<MarkdownNode>) : MarkdownNode()
    class Paragraph(val children: MutableList<MarkdownNode>) : MarkdownNode()
    class Heading(val level: Int, val children: MutableList<MarkdownNode>) : MarkdownNode()
    class Text(val content: String) : MarkdownNode()
    class Bold(val children: MutableList<MarkdownNode>) : MarkdownNode()
    class Italic(val children: MutableList<MarkdownNode>) : MarkdownNode()
    class Link(val url: String, val children: MutableList<MarkdownNode>) : MarkdownNode()
    class Image(val url: String, val alt: String) : MarkdownNode()
    class List(val ordered: Boolean, val items: MutableList<ListItem>) : MarkdownNode()
    class ListItem(val children: MutableList<MarkdownNode>) : MarkdownNode()
    class CodeBlock(val language: String, val content: String) : MarkdownNode()
    class InlineCode(val content: String) : MarkdownNode()
    object NewLine : MarkdownNode()
    object HorizontalRule : MarkdownNode()

    // Custom extensions
    data class CustomColor(val color: String, val children: MutableList<MarkdownNode>) : MarkdownNode()

    override fun toString(): String = buildString { printAST(this, "") }

    fun printAST(builder: StringBuilder, indent: String) {
        when (this) {
            is Document -> printNodeWithChildren(builder, indent, "Document")
            is Paragraph -> printNodeWithChildren(builder, indent, "Paragraph")
            is Heading -> printNodeWithChildren(builder, indent, "Heading (level $level)")
            is Text -> builder.appendLine("$indent├─ Text: \"$content\"")
            is Bold -> printNodeWithChildren(builder, indent, "Bold")
            is Italic -> printNodeWithChildren(builder, indent, "Italic")
            is Link -> printNodeWithChildren(builder, indent, "Link (url: $url)")
            is Image -> builder.appendLine("$indent├─ Image (url: $url, alt: \"$alt\")")
            is List -> printListNode(builder, indent)
            is ListItem -> printNodeWithChildren(builder, indent, "ListItem")
            is CodeBlock -> printCodeBlockNode(builder, indent)
            is InlineCode -> builder.appendLine("$indent├─ InlineCode: \"$content\"")
            is HorizontalRule -> builder.appendLine("$indent├─ HorizontalRule")
            is CustomColor -> printNodeWithChildren(builder, indent, "CustomColor (color: $color)")
            is NewLine -> builder.appendLine("$indent├─ NewLine")
        }
    }

    private fun printNodeWithChildren(builder: StringBuilder, indent: String, nodeName: String) {
        builder.appendLine("$indent├─ $nodeName")
        val children = when (this) {
            is Document -> children
            is Paragraph -> children
            is Heading -> children
            is Bold -> children
            is Italic -> children
            is Link -> children
            is ListItem -> children
            is CustomColor -> children
            else -> emptyList()
        }
        children.forEachIndexed { index, child ->
            if (index == children.lastIndex) {
                child.printAST(builder, "$indent│  ")
            } else {
                child.printAST(builder, "$indent├─ ")
            }
        }
    }

    private fun List.printListNode(builder: StringBuilder, indent: String) {
        builder.appendLine("$indent├─ List (ordered: $ordered)")
        this.items.forEachIndexed { index, item ->
            if (index == items.lastIndex) {
                item.printAST(builder, "$indent   ")
            } else {
                item.printAST(builder, "$indent│  ")
            }
        }
    }

    private fun CodeBlock.printCodeBlockNode(builder: StringBuilder, indent: String) {
        builder.appendLine("$indent├─ CodeBlock (language: $language)")
        this.content.lines().forEachIndexed { index, line ->
            if (index == content.lines().lastIndex) {
                builder.appendLine("$indent   $line")
            } else {
                builder.appendLine("$indent│  $line")
            }
        }
    }
}