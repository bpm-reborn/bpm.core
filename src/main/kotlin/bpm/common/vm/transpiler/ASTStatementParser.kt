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

        private val EXPRESSION_REGEX = Regex("""\$\{(.*?)}""")
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
        var lastIndex = 0

        // Find all expressions in the source using regex
        EXPRESSION_REGEX.findAll(source).forEach { matchResult ->
            // Add any literal content before the expression
            if (matchResult.range.first > lastIndex) {
                val literalContent = source.substring(lastIndex, matchResult.range.first)
                if (literalContent.isNotBlank()) {
                    statements.add(ASTNode.Statement.Literal(literalContent.trim()))
                }
            }

            // Parse the expression content
            val expressionContent = matchResult.groupValues[1].trim()
            parseExpression(expressionContent)?.let { statements.add(it) }

            lastIndex = matchResult.range.last + 1
        }

        // Add any remaining literal content
        if (lastIndex < source.length) {
            val remaining = source.substring(lastIndex)
            if (remaining.isNotBlank()) {
                statements.add(ASTNode.Statement.Literal(remaining.trim()))
            }
        }

        return statements
    }

    /**
     * Parses an individual expression into an AST statement.
     */
    private fun parseExpression(expression: String): ASTNode.Statement? {
        return when {
            // Node reference (e.g., NODE.someNodeId)
            expression.startsWith("NODE.") -> {
                val nodeId = expression.substringAfter("NODE.")
                ASTNode.Statement.NodeReference(nodeId)
            }

            // Execution flow (e.g., EXEC.targetNodeId)
            expression.startsWith("EXEC.") -> {
                val targetId = expression.substringAfter("EXEC.")
                ASTNode.Statement.ExecFlow(targetId)
            }

            // Variable reference (e.g., VARS.someVariable)
            expression.startsWith("VARS.") -> {
                val varName = expression.substringAfter("VARS.")
                ASTNode.Statement.VariableReference(varName)
            }

            // Setup block (e.g., SETUP.{some code})
            expression.startsWith("SETUP.") -> {
                val content = expression.substringAfter("SETUP.")
                ASTNode.Statement.SetupBlock(content)
            }

            // Output assignment (e.g., OUTPUT.name = value)
            expression.startsWith("OUTPUT.") -> {
                val parts = expression.substringAfter("OUTPUT.").split("=", limit = 2)
                if (parts.size == 2) {
                    ASTNode.Statement.OutputAssignment(
                        outputName = parts[0].trim(),
                        value = parts[1].trim()
                    )
                } else null
            }

            else -> null // Unknown expression type
        }
    }
}