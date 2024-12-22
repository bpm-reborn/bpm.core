package bpm.common.vm.transpiler


import bpm.common.network.Endpoint
import bpm.common.network.listener
import bpm.common.property.Property
import bpm.common.property.cast
import bpm.common.type.NodeLibrary
import bpm.common.type.NodeType
import bpm.common.upstream.Schemas
import bpm.common.workspace.graph.Node

/**
 * Parser responsible for generating AST statements from node source content.
 */
class ASTStatementParser(private val library: NodeLibrary) {

    companion object {

        private val EXPRESSION_REGEX = Regex("""\$\{(.*?)}""", RegexOption.DOT_MATCHES_ALL)
    }

    /**
     * Parses the source content of a node and generates a list of AST statements.
     *
     * @param node The node whose source content needs to be parsed
     * @return List of parsed statements
     */
    fun parseStatements(node: Node): List<ASTNode.Statement> {
        val sourceContent = getSourceContent(node, getNodeTemplate(node)) ?: return emptyList()
        return parseSourceContent(sourceContent)
    }

    /**
     * Gets the node template from the schema library.
     */
    private fun getNodeTemplate(node: Node): NodeType? = library["${node.type}/${node.name}"]

    /**
     * Gets the source content either from node override or template.
     */
    private fun getSourceContent(node: Node, template: NodeType? = null): String =
        if (node.properties.contains("override")) {
            node["override"].cast<Property.String>().get()
        } else {
            if (template == null || !template.contains("source")) {
                ""
            } else {
                val source = template["source"].cast<Property.String>().get()

                source
            }
        }

    /**
     * Parses the source content into a list of statements.
     */
    private fun parseSourceContent(source: String): List<ASTNode.Statement> {
        val statements = mutableListOf<ASTNode.Statement>()
        val chars = source.toCharArray()
        var i = 0
        var currentContent = StringBuilder()

        while (i < chars.size) {
            when {
                chars[i] == '$' && i + 1 < chars.size && chars[i + 1] == '{' -> {
                    // Handle any accumulated literal content
                    if (currentContent.isNotEmpty()) {
                        val content = currentContent.toString().trim()
                        if (content.isNotEmpty()) {
                            statements.add(ASTNode.Statement.Literal(content))
                        }
                        currentContent.clear()
                    }

                    // Parse expression
                    i += 2 // Skip ${
                    val expressionContent = parseUntilClosingBrace(chars, i)
                    i += expressionContent.length + 1 // +1 for closing }
                    parseExpression(expressionContent.trim())?.let { statements.add(it) }
                }
                else -> {
                    currentContent.append(chars[i])
                    i++
                }
            }
        }

        // Handle any remaining literal content
        if (currentContent.isNotEmpty()) {
            val content = currentContent.toString().trim()
            if (content.isNotEmpty()) {
                statements.add(ASTNode.Statement.Literal(content))
            }
        }

        return statements
    }

    /**
     * Parses content until finding the matching closing brace, handling nested braces.
     */
    private fun parseUntilClosingBrace(chars: CharArray, startIndex: Int): String {
        val content = StringBuilder()
        var braceCount = 1
        var i = startIndex

        while (i < chars.size) {
            when (chars[i]) {
                '{' -> braceCount++
                '}' -> {
                    braceCount--
                    if (braceCount == 0) {
                        return content.toString()
                    }
                }
            }
            content.append(chars[i])
            i++
        }

        // If we get here, there was no matching closing brace
        throw IllegalStateException("No matching closing brace found")
    }

    /**
     * Parses an individual expression into an AST statement.
     */
    private fun parseExpression(expression: String): ASTNode.Statement? {
        return when {
            expression.startsWith("NODE.") -> {
                val nodeId = expression.substringAfter("NODE.")
                ASTNode.Statement.NodeReference(nodeId)
            }
            expression.startsWith("EXEC.") -> {
                val targetId = expression.substringAfter("EXEC.")
                ASTNode.Statement.ExecFlow(targetId)
            }
            expression.startsWith("VARS.") -> {
                val varName = expression.substringAfter("VARS.")
                ASTNode.Statement.VariableReference(varName)
            }
            expression.startsWith("SETUP.") -> {
                val content = expression.substringAfter("SETUP.")
                ASTNode.Statement.SetupBlock(content)
            }
            expression.startsWith("OUTPUT.") -> {
                // Split only on first = to preserve any = in the value part
                val parts = expression.substringAfter("OUTPUT.").split("=", limit = 2)
                if (parts.size == 2) {
                    ASTNode.Statement.OutputAssignment(
                        outputName = parts[0].trim(),
                        value = parts[1].trim()
                    )
                } else null
            }
            else -> null
        }
    }
}