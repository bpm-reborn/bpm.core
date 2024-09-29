package bpm.client.render.markdown

object Markdown {

    data class CompilationUnit(val tokens: List<MarkdownLexer.Token>, val ast: MarkdownNode.Document) {

        override fun toString(): String = buildString {
            appendLine("Tokens:")
            tokens.forEachIndexed { index, token -> appendLine("$index. $token") }
            appendLine()
            appendLine("AST:")
            appendLine(ast)
        }
    }

    /**
     * Parses the given markdown input into a MarkdownNode.Document.
     */
    fun parseMarkdown(input: String): MarkdownNode.Document {
        val tokens = parseTokens(input)
        val parser = MarkdownParser(tokens)
        return parser.parse()
    }

    /**
     * Parses the given markdown input into a list of tokens.
     */
    fun parseTokens(input: String): List<MarkdownLexer.Token> {
        val lexer = MarkdownLexer(input)
        val tokens = mutableListOf<MarkdownLexer.Token>()
        do {
            val token = lexer.nextToken()
            tokens.add(token)
        } while (token.type != MarkdownLexer.TokenType.EOF)
        return tokens
    }

    fun parse(input: String): CompilationUnit {
        val tokens = parseTokens(input)
        val parser = MarkdownParser(tokens)
        val ast = parser.parse()
        return CompilationUnit(tokens, ast)
    }

}