package bpm.common.vm.compiliation

import bpm.common.logging.KotlinLogging
import bpm.common.network.Endpoint
import bpm.common.network.listener
import bpm.common.property.Property
import bpm.common.property.cast
import bpm.common.upstream.Schemas
import bpm.common.workspace.graph.Edge
import bpm.common.workspace.graph.Node

class Tokenizer {
    private val logger = KotlinLogging.logger {}

    private lateinit var input: String
    private var position: Int = 0

    fun tokenize(nodes: Collection<Node>, edges: Collection<Edge>): List<Token> {
        val tokens = mutableListOf<Token>()

        nodes.forEach { node ->
            tokens.add(Token(TokenType.NODE_START, node.uid.toString()))
            tokens.addAll(tokenizeNodeSource(node))
            tokens.add(Token(TokenType.NODE_END, node.uid.toString()))
        }

        edges.forEach { edge ->
            tokens.add(Token(TokenType.EDGE, edge.uid.toString()))
        }

        return tokens
    }

    private fun tokenizeNodeSource(node: Node): List<Token> {
        val nodeTemplate = listener<Schemas>(Endpoint.Side.SERVER).library["${node.type}/${node.name}"]
        if (nodeTemplate == null) {
            logger.error { "Node template not found for ${node.type}/${node.name}" }
            return emptyList()
        }

        val hasOverride = node.properties.contains("override")
        val hasSource = nodeTemplate.properties.contains("source")
        if (!hasSource && !hasOverride) {
            logger.error { "Node template does not contain source for ${node.type}/${node.name}" }
            return emptyList()
        }
        val sourceTemplate = if (hasOverride) node["override"].cast<Property.String>()
            .get() else nodeTemplate["source"].cast<Property.String>().get()
        input = sourceTemplate
        position = 0
        val tokens = mutableListOf<Token>()

        while (position < input.length) {
            when {
                input.startsWith("\${", position) -> tokens.add(tokenizeExpression())
                else -> tokens.add(tokenizeLiteral())
            }
        }
        return tokens
    }

    private fun tokenizeExpression(): Token {
        position += 2 // Skip "${" prefix
        val start = position
        var braceCount = 1

        while (position < input.length && braceCount > 0) {
            when (input[position]) {
                '{' -> braceCount++
                '}' -> braceCount--
            }
            position++
        }

        if (braceCount > 0) throw IllegalStateException("Unmatched brace in expression")

        val content = input.substring(start, position - 1)
        return Token(TokenType.EXPRESSION, content)
    }

    private fun tokenizeLiteral(): Token {
        val start = position
        while (position < input.length && !input.startsWith(
                "\${", position
            )
        ) {
            position++
        }
        return Token(TokenType.LITERAL, input.substring(start, position))
    }
}