package bpm.client.render.markdown

class MarkdownParser(private val tokens: List<MarkdownLexer.Token>) {
    private var index = 0
    private val currentToken: MarkdownLexer.Token
        get() = tokens.getOrNull(index) ?: tokens.last()


//    fun parse(): MarkdownNode.Document {
//        val document = MarkdownNode.Document(mutableListOf())
//        var lastWasNewline = false
//        var forcedLineBreak = false
//
//        while (currentToken.type != MarkdownLexer.TokenType.EOF) {
//            when (currentToken.type) {
//                MarkdownLexer.TokenType.NEWLINE -> {
//                    if (lastWasNewline && !forcedLineBreak) {
//                        consume(MarkdownLexer.TokenType.NEWLINE)
//                    } else {
//                        document.children.add(MarkdownNode.NewLine)
//                        lastWasNewline = true
//                        consume(MarkdownLexer.TokenType.NEWLINE)
//                    }
//                    forcedLineBreak = false
//                }
//
//                MarkdownLexer.TokenType.HORIZONTAL_RULE -> {
//                    document.children.add(MarkdownNode.HorizontalRule)
//                    consume(MarkdownLexer.TokenType.HORIZONTAL_RULE)
//                    lastWasNewline = false
//                }
//
//                MarkdownLexer.TokenType.TEXT -> {
//                    if (currentToken.value.endsWith("\\")) {
//                        forcedLineBreak = true
//                        val textContent = currentToken.value.substring(0, currentToken.value.length - 1)
//                        document.children.add(MarkdownNode.Text(textContent))
//                        consume(MarkdownLexer.TokenType.TEXT)
//                    } else {
//                        document.children.add(parseBlock())
//                        lastWasNewline = false
//                    }
//                }
//
//                else -> {
//                    document.children.add(parseBlock())
//                    lastWasNewline = false
//                }
//            }
//        }
//
//        return document
//    }


    private fun log(message: String) {
        println("Parser: $message (Current token: ${currentToken.type} at index $index)")
    }

    fun parse(): MarkdownNode.Document {
        val document = MarkdownNode.Document(mutableListOf())
        while (currentToken.type != MarkdownLexer.TokenType.EOF) {
            try {
                document.children.add(parseBlock())
            } catch (e: Exception) {
                log("Error parsing block: ${e.message}")
                // Skip the problematic token and continue
                index++
            }
        }
        return document
    }



    private fun parseBlock(): MarkdownNode {
        log("Parsing block")
        return when (currentToken.type) {
            MarkdownLexer.TokenType.HEADER -> parseHeader()
            MarkdownLexer.TokenType.PARAGRAPH_START -> parseParagraph()
            MarkdownLexer.TokenType.LIST_ITEM -> parseList()
            MarkdownLexer.TokenType.CODE_BLOCK_START, MarkdownLexer.TokenType.CODE_BLOCK_LANGUAGE -> parseCodeBlock()
            MarkdownLexer.TokenType.HORIZONTAL_RULE -> parseHorizontalRule()
            MarkdownLexer.TokenType.NEWLINE -> {
                consume(MarkdownLexer.TokenType.NEWLINE)
                MarkdownNode.NewLine
            }
            else -> parseParagraph()
        }
    }



    private fun parseHeader(): MarkdownNode.Heading {
        val level = currentToken.value.count { it == '#' }
        consume(MarkdownLexer.TokenType.HEADER)
        val content = parseInline()
        return MarkdownNode.Heading(level, content)
    }

    private fun parseParagraph(): MarkdownNode.Paragraph {
        val content = mutableListOf<MarkdownNode>()
        if (currentToken.type == MarkdownLexer.TokenType.PARAGRAPH_START) {
            consume(MarkdownLexer.TokenType.PARAGRAPH_START)
        }
        while (currentToken.type != MarkdownLexer.TokenType.PARAGRAPH_END &&
            currentToken.type != MarkdownLexer.TokenType.EOF &&
            currentToken.type != MarkdownLexer.TokenType.NEWLINE
        ) {
            content.addAll(parseInline())
        }
        if (currentToken.type == MarkdownLexer.TokenType.PARAGRAPH_END) {
            consume(MarkdownLexer.TokenType.PARAGRAPH_END)
        } else if (currentToken.type == MarkdownLexer.TokenType.NEWLINE) {
            consume(MarkdownLexer.TokenType.NEWLINE)
        }
        return MarkdownNode.Paragraph(content)
    }
    private fun parseList(): MarkdownNode.List {
        log("Parsing list")
        val items = mutableListOf<MarkdownNode.ListItem>()
        val ordered = currentToken.value.first().isDigit()

        while (currentToken.type == MarkdownLexer.TokenType.LIST_ITEM) {
            consume(MarkdownLexer.TokenType.LIST_ITEM)
            val itemContent = mutableListOf<MarkdownNode>()

            while (currentToken.type != MarkdownLexer.TokenType.NEWLINE &&
                currentToken.type != MarkdownLexer.TokenType.EOF &&
                currentToken.type != MarkdownLexer.TokenType.LIST_ITEM) {
                itemContent.addAll(parseInline())
            }

            items.add(MarkdownNode.ListItem(itemContent))

            if (currentToken.type == MarkdownLexer.TokenType.NEWLINE) {
                consume(MarkdownLexer.TokenType.NEWLINE)
            }
        }

        return MarkdownNode.List(ordered, items)
    }

    private fun parseCodeBlock(): MarkdownNode.CodeBlock {
        var language = ""
        if (currentToken.type == MarkdownLexer.TokenType.CODE_BLOCK_START) {
            consume(MarkdownLexer.TokenType.CODE_BLOCK_START)
        }
        if (currentToken.type == MarkdownLexer.TokenType.CODE_BLOCK_LANGUAGE) {
            language = currentToken.value
            consume(MarkdownLexer.TokenType.CODE_BLOCK_LANGUAGE)
        }
        val content = StringBuilder()
        while (currentToken.type != MarkdownLexer.TokenType.CODE_BLOCK_END &&
            currentToken.type != MarkdownLexer.TokenType.EOF
        ) {
            content.append(currentToken.value)
            if (currentToken.type == MarkdownLexer.TokenType.NEWLINE) {
                content.append("\n")
            }
            consume(currentToken.type)
        }
        if (currentToken.type == MarkdownLexer.TokenType.CODE_BLOCK_END) {
            consume(MarkdownLexer.TokenType.CODE_BLOCK_END)
        }
        return MarkdownNode.CodeBlock(language, content.toString().trim())
    }


    private fun parseHorizontalRule(): MarkdownNode.HorizontalRule {
        consume(MarkdownLexer.TokenType.HORIZONTAL_RULE)
        return MarkdownNode.HorizontalRule
    }


    private fun parseColorBlock(): MarkdownNode.CustomColor {
        val color = currentToken.value.removePrefix("{color:").removeSuffix("}")
        consume(MarkdownLexer.TokenType.COLOR_START)
        val content = parseUntil(MarkdownLexer.TokenType.COLOR_END)
        consume(MarkdownLexer.TokenType.COLOR_END)
        return MarkdownNode.CustomColor(color, content)
    }


    private fun parseUntil(target: MarkdownLexer.TokenType): MutableList<MarkdownNode> {
        val nodes = mutableListOf<MarkdownNode>()
        while (
            currentToken.type != MarkdownLexer.TokenType.PARAGRAPH_END &&
            currentToken.type != MarkdownLexer.TokenType.BOLD_END &&
            currentToken.type != MarkdownLexer.TokenType.ITALIC_END &&
            currentToken.type != MarkdownLexer.TokenType.LINK_URL &&
            currentToken.type != MarkdownLexer.TokenType.EOF &&
            currentToken.type != MarkdownLexer.TokenType.COLOR_END
        ) {
            when (currentToken.type) {
                MarkdownLexer.TokenType.TEXT -> nodes.add(parseText())
                MarkdownLexer.TokenType.BOLD_START -> nodes.add(parseBold())
                MarkdownLexer.TokenType.ITALIC_START -> nodes.add(parseItalic())
                MarkdownLexer.TokenType.LINK_START -> nodes.add(parseLink())
                MarkdownLexer.TokenType.IMAGE_START -> nodes.add(parseImage())
                MarkdownLexer.TokenType.INLINE_CODE -> nodes.add(parseInlineCode())
                MarkdownLexer.TokenType.COLOR_START -> nodes.add(parseColor())
                else -> consume(currentToken.type) // Skip unknown tokens
            }
        }
        return nodes
    }

    private fun parseInline(): MutableList<MarkdownNode> {
        log("Parsing inline")
        val nodes = mutableListOf<MarkdownNode>()
        while (currentToken.type != MarkdownLexer.TokenType.NEWLINE &&
            currentToken.type != MarkdownLexer.TokenType.PARAGRAPH_END &&
            currentToken.type != MarkdownLexer.TokenType.EOF &&
            currentToken.type != MarkdownLexer.TokenType.LIST_ITEM
        ) {
            when (currentToken.type) {
                MarkdownLexer.TokenType.TEXT -> nodes.add(parseText())
                MarkdownLexer.TokenType.BOLD_START -> nodes.add(parseBold())
                MarkdownLexer.TokenType.ITALIC_START -> nodes.add(parseItalic())
                MarkdownLexer.TokenType.LINK_START -> nodes.add(parseLink())
                MarkdownLexer.TokenType.IMAGE_START -> nodes.add(parseImage())
                MarkdownLexer.TokenType.INLINE_CODE -> nodes.add(parseInlineCode())
                MarkdownLexer.TokenType.COLOR_START -> nodes.add(parseColorBlock())
                else -> {
                    log("Unexpected token in inline: ${currentToken.type}")
                    consume(currentToken.type)
                }
            }
        }
        return nodes
    }

    private fun parseText(): MarkdownNode.Text {
        val text = currentToken.value
        consume(MarkdownLexer.TokenType.TEXT)
        return MarkdownNode.Text(text)
    }

    private fun parseBold(): MarkdownNode.Bold {
        consume(MarkdownLexer.TokenType.BOLD_START)
        val content = parseInline()
        consume(MarkdownLexer.TokenType.BOLD_END)
        return MarkdownNode.Bold(content)
    }

    private fun parseItalic(): MarkdownNode.Italic {
        consume(MarkdownLexer.TokenType.ITALIC_START)
        val content = parseInline()
        consume(MarkdownLexer.TokenType.ITALIC_END)
        return MarkdownNode.Italic(content)
    }

    private fun parseLink(): MarkdownNode.Link {
        consume(MarkdownLexer.TokenType.LINK_START)
        val text = parseInline()
        val url = currentToken.value
        consume(MarkdownLexer.TokenType.LINK_URL)
        consume(MarkdownLexer.TokenType.LINK_END)
        return MarkdownNode.Link(url, text)
    }

    private fun parseImage(): MarkdownNode.Image {
        consume(MarkdownLexer.TokenType.IMAGE_START)
        maybeConsume(MarkdownLexer.TokenType.PARAGRAPH_START)
        val alt = currentToken.value
        consume(MarkdownLexer.TokenType.IMAGE_ALT)
        val url = currentToken.value
        MarkdownImages.loadImage(url)
        consume(MarkdownLexer.TokenType.IMAGE_URL)
        consume(MarkdownLexer.TokenType.IMAGE_END)

        //Eat the newline or paragraph end
        if (currentToken.type == MarkdownLexer.TokenType.NEWLINE) {
            consume(MarkdownLexer.TokenType.NEWLINE)
        }
        if (currentToken.type == MarkdownLexer.TokenType.PARAGRAPH_END) {
            consume(MarkdownLexer.TokenType.PARAGRAPH_END)
        }
        return MarkdownNode.Image(url, alt)
    }

    private fun parseInlineCode(): MarkdownNode.InlineCode {
        consume(MarkdownLexer.TokenType.INLINE_CODE)
        val content = currentToken.value
        consume(MarkdownLexer.TokenType.TEXT)
        consume(MarkdownLexer.TokenType.INLINE_CODE)
        return MarkdownNode.InlineCode(content)
    }

    private fun parseColor(): MarkdownNode.CustomColor {
        val color = currentToken.value.removePrefix("{color:").removeSuffix("}")
        consume(MarkdownLexer.TokenType.COLOR_START)
        val content = parseUntil(MarkdownLexer.TokenType.COLOR_END)
        consume(MarkdownLexer.TokenType.COLOR_END)
        return MarkdownNode.CustomColor(color, content)
    }

    private fun consume(expectedType: MarkdownLexer.TokenType? = null) {
        if (index < tokens.size - 1) {
            if (currentToken.type == expectedType || expectedType == null) {
                index++
            } else {
                log("Unexpected token: ${currentToken.type}, expected: $expectedType")
            }
        } else {
            log("Reached end of tokens")
        }
    }

    private fun maybeConsume(expectedType: MarkdownLexer.TokenType): Boolean {
        if (currentToken.type == expectedType) {
            index++
            return true
        }
        return false
    }
}