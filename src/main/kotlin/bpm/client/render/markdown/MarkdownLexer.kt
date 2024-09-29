package bpm.client.render.markdown

class MarkdownLexer(private val input: String) {
    enum class TokenType {
        HEADER, PARAGRAPH_START, PARAGRAPH_END, BOLD_START, BOLD_END, ITALIC_START, ITALIC_END,
        LINK_START, LINK_TEXT, LINK_URL, LINK_END, IMAGE_START, IMAGE_ALT, IMAGE_URL, IMAGE_END,
        LIST_ITEM, CODE_BLOCK_START, CODE_BLOCK_END, CODE_BLOCK_LANGUAGE, INLINE_CODE,
        HORIZONTAL_RULE, COLOR_START, COLOR_END, TEXT, NEWLINE, EOF
    }

    data class Token(val type: TokenType, val value: String, val line: Int, val column: Int) {

        override fun toString(): String = "$type: ${value.trim()} (line: $line, column: $column)"
    }

    private var position = 0
    private var line = 1
    private var column = 0
    private val emphasisStack = mutableListOf<Char>()
    private var inParagraph = false
    private var inListItem = false
    private var inLink = false
    private var inImage = false
    private var inCodeBlock = false

    fun nextToken(): Token {
        if (position >= input.length) {
            if (inParagraph) {
                inParagraph = false
                return Token(TokenType.PARAGRAPH_END, "", line, column)
            }
            return Token(TokenType.EOF, "", line, column)
        }

        val char = input[position]
        return when {
            inCodeBlock -> lexCodeBlockContent()
            char == '\n' -> lexNewline()
            char.isWhitespace() -> consumeWhitespace()
            char == '#' && (position == 0 || input[position - 1] == '\n') -> lexHeader()
            isHorizontalRule() -> lexHorizontalRule()
            (char == '*' || char == '_') && isEmphasisStart() -> lexEmphasisStart()
            (char == '*' || char == '_') && isEmphasisEnd() -> lexEmphasisEnd()
            char == '!' && peekNext() == '[' -> lexImageStart()
            char == '[' && !inLink && !inImage -> lexLinkStart()
            char == ']' && peekNext() == '(' && (inLink || inImage) -> lexMiddle()
            char == ')' && (inLink || inImage) -> lexEnd()
            isListItem() -> lexListItem()
            char == '`' -> lexCode()
            char == '{' && input.startsWith("{color:", position) -> lexColorStart()
            char == '{' && input.startsWith("{/color}", position) -> lexColorEnd()
            else -> lexText()
        }
    }


    private fun lexNewline(): Token {
        consume()
        line++
        column = 0
        if (position < input.length && input[position] == '\n') {
            if (inParagraph && !inListItem) {
                inParagraph = false
                return Token(TokenType.PARAGRAPH_END, "\n", line - 1, column)
            }
        }
        return Token(TokenType.NEWLINE, "\n", line - 1, column)
    }

    private fun consumeWhitespace(): Token {
        while (position < input.length && input[position].isWhitespace() && input[position] != '\n') {
            consume()
        }
        return nextToken()
    }

    private fun lexMiddle(): Token {
        consume() // Consume ']'
        consume() // Consume '('
        val start = position
        while (position < input.length && input[position] != ')') {
            consume()
        }
        return if (inImage) {
            Token(TokenType.IMAGE_URL, input.substring(start, position), line, column - (position - start))
        } else {
            Token(TokenType.LINK_URL, input.substring(start, position), line, column - (position - start))
        }
    }

    private fun lexEnd(): Token {
        consume()
        return if (inImage) {
            inImage = false
            Token(TokenType.IMAGE_END, ")", line, column - 1)
        } else {
            inLink = false
            Token(TokenType.LINK_END, ")", line, column - 1)
        }
    }

    private fun lexHeader(): Token {
        val start = position
        while (position < input.length && input[position] == '#') {
            consume()
        }
        // Include the space after '#' symbols in the header token
        if (position < input.length && input[position].isWhitespace()) {
            consume()
        }
        endParagraphIfNeeded()
        return Token(TokenType.HEADER, input.substring(start, position), line, column - (position - start))
    }

    private fun isEmphasisStart(): Boolean {
        val nextChar = peekNext()
        return nextChar != null && !nextChar.isWhitespace() &&
                (emphasisStack.isEmpty() || emphasisStack.last() != input[position])
    }

    private fun isEmphasisEnd(): Boolean {
        return emphasisStack.isNotEmpty() && emphasisStack.last() == input[position] &&
                (position == input.length - 1 || !input[position + 1].isLetterOrDigit())
    }

    private fun lexEmphasisStart(): Token {
        val char = input[position]
        emphasisStack.add(char)
        consume()
        return if (position < input.length && input[position] == char) {
            consume()
            emphasisStack.add(char)
            Token(TokenType.BOLD_START, char.toString().repeat(2), line, column - 2)
        } else {
            Token(TokenType.ITALIC_START, char.toString(), line, column - 1)
        }
    }

    private fun lexEmphasisEnd(): Token {
        val char = input[position]
        consume()
        return if (emphasisStack.size > 1 && emphasisStack[emphasisStack.size - 2] == char) {
            consume()
            emphasisStack.removeAt(emphasisStack.size - 1)
            emphasisStack.removeAt(emphasisStack.size - 1)
            Token(TokenType.BOLD_END, char.toString().repeat(2), line, column - 2)
        } else {
            emphasisStack.removeAt(emphasisStack.size - 1)
            Token(TokenType.ITALIC_END, char.toString(), line, column - 1)
        }
    }

    private fun lexLinkStart(): Token {
        inLink = true
        consume()
        return Token(TokenType.LINK_START, "[", line, column - 1)
    }

    private fun lexLinkMiddle(): Token {
        consume() // Consume ']'
        consume() // Consume '('
        val start = position
        while (position < input.length && input[position] != ')') {
            consume()
        }
        return Token(TokenType.LINK_URL, input.substring(start, position), line, column - (position - start))
    }

    private fun lexLinkEnd(): Token {
        inLink = false
        consume()
        return Token(TokenType.LINK_END, ")", line, column - 1)
    }

    private fun lexImageStart(): Token {
        consume() // Consume '!'
        consume() // Consume '['
        inImage = true
        return Token(TokenType.IMAGE_START, "![", line, column - 2)
    }

    private fun isHorizontalRule(): Boolean {
        if (position == 0 || input[position - 1] == '\n') {
            val char = input[position]
            if (char == '-' || char == '*' || char == '_') {
                var count = 0
                var i = position
                while (i < input.length && input[i] != '\n') {
                    if (input[i] == char) count++
                    else if (!input[i].isWhitespace()) return false
                    i++
                }
                return count >= 3
            }
        }
        return false
    }


    private fun lexHorizontalRule(): Token {
        val start = position
        while (position < input.length && input[position] != '\n') {
            consume()
        }
        endParagraphIfNeeded()
        return Token(
            TokenType.HORIZONTAL_RULE,
            input.substring(start, position).trim(),
            line,
            column - (position - start)
        )
    }


    private fun isListItem(): Boolean {
        if (position == 0 || input[position - 1] == '\n') {
            var i = position
            // Skip leading spaces
            while (i < input.length && input[i].isWhitespace() && input[i] != '\n') i++
            if (i < input.length) {
                if (input[i] == '-' || input[i] == '*') {
                    return i + 1 < input.length && input[i + 1].isWhitespace()
                }
                if (input[i].isDigit()) {
                    while (i < input.length && input[i].isDigit()) i++
                    return i + 1 < input.length && input[i] == '.' && input[i + 1].isWhitespace()
                }
            }
        }
        return false
    }

    private fun lexListItem(): Token {
        endParagraphIfNeeded()
        inListItem = true
        val start = position
        consume() // Consume '-' or '*'
        // Consume following whitespace
        //Remove the leading period from the list item
        if (position < input.length && input[position] == '.') {
            consume()
        }
        while (position < input.length && input[position].isWhitespace() && input[position] != '\n') {
            consume()
        }

        inParagraph = true
        return Token(TokenType.LIST_ITEM, input.substring(start, position), line, column - (position - start))
    }

    private fun lexCode(): Token {
        val start = position
        consume()
        if (position < input.length && input[position] == '`') {
            consume()
            if (position < input.length && input[position] == '`') {
                consume()
                inCodeBlock = true
                return lexCodeBlockLanguage()
            }
            return Token(TokenType.INLINE_CODE, "``", line, column - 2)
        }
        return Token(TokenType.INLINE_CODE, "`", line, column - 1)
    }

    private fun lexCodeBlockLanguage(): Token {
        val start = position
        while (position < input.length && input[position] != '\n') {
            consume()
        }
        val language = input.substring(start, position).trim()
        if (language.isNotEmpty()) {
            return Token(TokenType.CODE_BLOCK_LANGUAGE, language, line, column - language.length)
        }
        return Token(TokenType.CODE_BLOCK_START, "```", line, column - 3)
    }

    private fun lexCodeBlockContent(): Token {
        val start = position
        while (position < input.length) {
            if (input[position] == '`' && position + 2 < input.length &&
                input[position + 1] == '`' && input[position + 2] == '`'
            ) {
                if (start == position) {
                    consume()
                    consume()
                    consume()
                    inCodeBlock = false
                    return Token(TokenType.CODE_BLOCK_END, "```", line, column - 3)
                }
                break
            }
            if (input[position] == '\n') {
                line++
                column = 0
            } else {
                column++
            }
            position++
        }
        return if (start < position) {
            Token(TokenType.TEXT, input.substring(start, position), line, column - (position - start))
        } else {
            nextToken()
        }
    }

    private fun lexColorStart(): Token {
        val start = position
        while (position < input.length && input[position] != '}') {
            consume()
        }
        if (position < input.length) {
            consume() // Consume '}'
        }
        return Token(TokenType.COLOR_START, input.substring(start, position), line, column - (position - start))
    }

    private fun lexColorEnd(): Token {
        val start = position
        while (position < input.length && input[position] != '}') {
            consume()
        }
        if (position < input.length) {
            consume() // Consume '}'
        }
        return Token(TokenType.COLOR_END, input.substring(start, position), line, column - (position - start))
    }

    private fun lexText(): Token {
        if (!inParagraph && !inListItem) {
            inParagraph = true
            return Token(TokenType.PARAGRAPH_START, "", line, column)
        }
        val start = position
        while (position < input.length) {
            val char = input[position]
            if (char == '\n' || char == '[' || char == ']' || char == '`' || char == '{' ||
                (char == '!' && peekNext() == '[') ||
                ((char == '*' || char == '_') && (isEmphasisStart() || isEmphasisEnd())) ||
                (char == ')' && (inLink || inImage))
            ) {
                break
            }
            consume()
        }
        val text = input.substring(start, position)
        return if (inImage) {
            Token(TokenType.IMAGE_ALT, text, line, column - text.length)
        } else if (inLink) {
            Token(TokenType.LINK_TEXT, text, line, column - text.length)
        } else {
            Token(TokenType.TEXT, text, line, column - text.length)
        }
    }

    private fun endParagraphIfNeeded(): Token? {
        if (inParagraph) {
            inParagraph = false
            inListItem = false
            return Token(TokenType.PARAGRAPH_END, "", line, column)
        }
        return null
    }

    private fun consume() {
        position++
        column++
    }

    private fun peekNext(): Char? {
        return if (position + 1 < input.length) input[position + 1] else null
    }
}