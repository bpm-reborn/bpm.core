package bpm.common.vm

import bpm.Bpm
import bpm.server.lua.LuaBuiltin
import bpm.common.logging.KotlinLogging
import bpm.common.network.Endpoint
import bpm.common.network.listener
import bpm.common.workspace.Workspace
import bpm.common.workspace.graph.Edge
import bpm.common.workspace.graph.Node
import bpm.common.property.Property
import bpm.common.property.cast
import bpm.common.schemas.Schemas
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object ComplexLuaTranspiler {

    private val logger = KotlinLogging.logger {}
    private val tokenizer = Tokenizer()
    private val parser = Parser()
    private val irGenerator = IRGenerator()
    private val nodeDependencies = ConcurrentHashMap<UUID, MutableSet<UUID>>()

    fun generateLuaScript(workspace: Workspace): String {
        nodeDependencies.clear()
        val nodes = topologicalSort(workspace, workspace.graph.nodes)
        val edges = workspace.graph.edges
        val tokens = tokenizer.tokenize(nodes, edges)
        val ast = parser.parse(tokens)
        val ir = irGenerator.generate(ast, workspace)
        return CodeGenerator(workspace).generate(ir)
    }

    private fun topologicalSort(workspace: Workspace, nodes: Collection<Node>): List<Node> {
        val sorted = mutableListOf<Node>()
        val visited = mutableSetOf<UUID>()

        fun visit(node: Node) {
            if (!visited.contains(node.uid)) {
                visited.add(node.uid)
                nodeDependencies[node.uid]?.forEach { dependencyUid ->
                    workspace.graph.getNode(dependencyUid)?.let { visit(it) }
                }
                sorted.add(node)
            }
        }

        nodes.forEach { if (!visited.contains(it.uid)) visit(it) }
        return sorted.reversed()
    }

    private class Tokenizer {

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
            val sourceTemplate = nodeTemplate["source"].cast<Property.String>().get()
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

    private class Parser {

        fun parse(tokens: List<Token>): AST {
            val ast = AST()
            val nodeStack = Stack<ASTNode.NodeDeclaration>()

            tokens.forEach { token ->
                when (token.type) {
                    TokenType.NODE_START -> {
                        val node = ASTNode.NodeDeclaration(token.value)
                        if (nodeStack.isNotEmpty()) {
                            nodeStack.peek().children.add(node)
                        } else {
                            ast.nodes.add(node)
                        }
                        nodeStack.push(node)
                    }

                    TokenType.NODE_END -> nodeStack.pop()
                    TokenType.EDGE -> ast.edges.add(ASTNode.EdgeDeclaration(token.value))
                    TokenType.EXPRESSION -> {
                        val expressionNode = parseExpression(token.value)
                        nodeStack.peek().children.add(expressionNode)
                    }

                    TokenType.LITERAL -> nodeStack.peek().children.add(ASTNode.Literal(token.value))
                }
            }

            return ast
        }


        private fun parseExpression(expression: String): ASTNode {
            return when {
                expression.startsWith("NODE.") -> ASTNode.NodeReference(expression.substringAfter("NODE."))
                expression.startsWith("EXEC.") -> ASTNode.ExecReference(expression.substringAfter("EXEC."))
                expression.startsWith("VARS.") -> ASTNode.VarReference(expression.substringAfter("VARS."))
                expression.startsWith("LAMBDA.") -> ASTNode.LambdaReference(expression.substringAfter("LAMBDA."))
                expression.startsWith("JAVA.") -> ASTNode.JavaImport(expression.substringAfter("JAVA."))
                expression.startsWith("SETUP.") -> ASTNode.SetupBlock(expression.substringAfter("SETUP."))
                expression.startsWith("OUTPUT.") -> {
                    val parts = expression.substringAfter("OUTPUT.").split("=", limit = 2)
                    if (parts.size == 2) {
                        ASTNode.OutputAssignment(parts[0].trim(), parts[1].trim())
                    } else {
                        ASTNode.GenericExpression(expression)
                    }
                }

                else -> ASTNode.GenericExpression(expression)
            }
        }
    }

    private class IRGenerator {

        fun generate(ast: AST, workspace: Workspace): IR {
            val ir = IR()

            // Process variables
            workspace.graph.variables.forEach { (name, property) ->
                ir.variables[name] = when (property) {
                    is Property.String -> IRValue.String(multiLineString(property.get()))
                    is Property.Int -> IRValue.Int(property.get())
                    is Property.Float -> IRValue.Float(property.get())
                    is Property.Boolean -> IRValue.Boolean(property.get())
                    else -> IRValue.Null
                }
            }

            // Process nodes
            ast.nodes.forEach { node ->
                val function = generateFunction(node, workspace)
                ir.functions.add(function)
            }

            // Process edges
            ast.edges.forEach { edge ->
                ir.edges.add(IREdge(edge.id))
            }

            return ir
        }


        private fun multiLineString(input: String): String {
            val lines = input.split("\n")
            return if (lines.size > 1) {
                val formattedLines = lines.mapIndexed { index, line ->
                    if (index == lines.lastIndex) {
                        "\"$line\""
                    } else {
                        "\"$line\\n\" .."
                    }
                }
                formattedLines.joinToString("\n    ")
            } else {
                "\"$input\""
            }
        }


        private fun getTargetNodes(workspace: Workspace, edge: Edge): List<Node> {
            val connectedLinks = workspace.graph.getLinks().filter { it.from == edge.uid }
            return connectedLinks.mapNotNull { link ->
                val targetEdge = workspace.graph.getEdge(link.to)
                targetEdge?.let { workspace.graph.getNode(it.owner) }
            }
        }

        private fun generateFunction(node: ASTNode.NodeDeclaration, workspace: Workspace): IRFunction {
            val actualNode = workspace.graph.getNode(UUID.fromString(node.id)) ?: return IRFunction(
                id = node.id,
                originalName = "UnknownNode",
                nodeType = "Unknown",
                inputEdges = emptyList(),
                inputConnections = emptyMap(),
                outputEdges = emptyMap()
            )

            val inputEdges = workspace.graph.getEdges(actualNode)
                .filter { it.direction == "input" && it.type != "exec" }.map { it.name }

            val inputConnections = workspace.graph.getEdges(actualNode)
                .filter { it.direction == "input" && it.type != "exec" }.mapNotNull { edge ->
                    val sourceNode = getSourceNode(workspace, edge)
                    if (sourceNode != null) {
                        edge.name to Pair(sourceNode.uid.toString(), sourceNode.name)
                    } else null
                }.toMap()

            val outputEdges = workspace.graph.getEdges(actualNode)
                .filter { it.direction == "output" && it.type == "exec" }.associate { edge ->
                    edge.name to getTargetNodes(workspace, edge).map { Pair(it.uid.toString(), it.name) }
                }

            val function = IRFunction(
                id = node.id,
                originalName = actualNode.name,
                nodeType = actualNode.type,
                inputEdges = inputEdges,
                inputConnections = inputConnections,
                outputEdges = outputEdges
            )

            node.children.forEach { child ->
                when (child) {
                    is ASTNode.Literal -> function.body.add(IRStatement.Literal(child.value))
                    is ASTNode.NodeReference -> {
                        if (child.name !in inputEdges) {
                            function.body.add(IRStatement.NodeReference(child.name))
                        }
                    }

                    is ASTNode.ExecReference -> function.body.add(IRStatement.ExecReference(child.name))
                    is ASTNode.VarReference -> function.body.add(IRStatement.VarReference(child.name))
                    is ASTNode.LambdaReference -> function.body.add(IRStatement.LambdaReference(child.name))
                    is ASTNode.JavaImport -> function.body.add(IRStatement.JavaImport(child.name))
                    is ASTNode.SetupBlock -> function.setupBlocks.add(child.content.removePrefix("{").removeSuffix("}"))
                    is ASTNode.GenericExpression -> function.body.add(IRStatement.GenericExpression(child.content))
                    is ASTNode.OutputAssignment -> function.body.add(
                        IRStatement.OutputAssignment(
                            child.name,
                            child.value
                        )
                    )

                    else -> {} // Ignore other node typesCodeGenerator
                }
            }

            return function
        }

        private fun getSourceNode(workspace: Workspace, edge: Edge): Node? {
            val connectedLink = workspace.graph.getLinks().find { it.to == edge.uid }
            return if (connectedLink != null) {
                val sourceEdge = workspace.graph.getEdge(connectedLink.from)
                if (sourceEdge != null) {
                    workspace.graph.getNode(sourceEdge.owner)
                } else null
            } else null
        }
    }

    private class CodeGenerator(private val workspace: Workspace) {

        private val indent = "  "
        private lateinit var currentNode: IRFunction
        private val nodeDependencies = mutableMapOf<String, MutableSet<String>>()
        private val executionOrder = mutableListOf<String>()

        fun generate(ir: IR): String {
            buildDependencies(ir)
            determineExecutionOrder(ir)

            val codeBuilder = StringBuilder()
            generateWorkspaceAccessor(workspace, codeBuilder)
            generateVariableInitializations(ir, codeBuilder)
            generateBuiltIns(codeBuilder)
            generateSetupFunction(ir, codeBuilder)
            generateGlobalOutputsTable(codeBuilder)
            generateFunctionDeclarations(ir, codeBuilder)
            generateFunctionImplementations(ir, codeBuilder)
            generateMainExecution(ir, codeBuilder)
            return codeBuilder.toString()
        }

        private fun buildDependencies(ir: IR) {
            ir.functions.forEach { function ->
                nodeDependencies[function.id] = mutableSetOf()
                function.inputConnections.forEach { (_, sourcePair) ->
                    nodeDependencies[function.id]!!.add(sourcePair.first)
                }
            }
        }

        private fun determineExecutionOrder(ir: IR) {
            val visited = mutableSetOf<String>()
            val tempVisited = mutableSetOf<String>()

            fun visit(id: String) {
                if (id in tempVisited) throw IllegalStateException("Cyclic dependency detected")
                if (id in visited) return

                tempVisited.add(id)
                nodeDependencies[id]?.forEach { visit(it) }
                tempVisited.remove(id)
                visited.add(id)
                executionOrder.add(id)
            }

            ir.functions.forEach { function ->
                if (function.id !in visited) visit(function.id)
            }
        }



        private fun generateGlobalOutputsTable(codeBuilder: StringBuilder) {
            codeBuilder.append("-- Global Outputs Table\n")
            codeBuilder.append("local globalOutputs = {}\n\n")
        }

        private fun generateBuiltIns(codeBuilder: StringBuilder) {
            val bootstrap = Bpm.bootstrap
            val builtIns = bootstrap.getBuiltIns()
            codeBuilder.append("-- Built-in Classes\n")
            builtIns.forEach { builtIn -> generateBuiltIn(builtIn, codeBuilder) }
        }

        private fun generateBuiltIn(builtIn: LuaBuiltin, codeBuilder: StringBuilder) {
            val classPath = builtIn.javaClass.name
            codeBuilder.append("local ${builtIn.name} = java.import('$classPath')\n")
        }

        private fun generateWorkspaceAccessor(workspace: Workspace, codeBuilder: StringBuilder) {
            codeBuilder.append("-- Workspace Accessor\n")
            codeBuilder.append("local _Uid =\"${workspace.uid}\"\n")
        }


        private fun generateVariableInitializations(ir: IR, codeBuilder: StringBuilder) {
            codeBuilder.append("-- Initialize Variables\n")
            codeBuilder.append("local variables = {\n")
            ir.variables.forEach { (name, value) ->
                codeBuilder.append("$indent$name = ${generateValue(value)},\n")
            }
            codeBuilder.append("}\n\n")
        }

        private fun generateSetupFunction(ir: IR, codeBuilder: StringBuilder) {
            codeBuilder.append("-- Setup\n")
            codeBuilder.append("local function setup()\n")
            ir.functions.flatMap { it.setupBlocks }.forEach { setupBlock ->
                setupBlock.lines().forEach { line ->
                    codeBuilder.append("$indent${line.trim()}\n")
                }
            }

            codeBuilder.append("end\n\n")
        }

        private fun generateFunctionDeclarations(ir: IR, codeBuilder: StringBuilder) {
            codeBuilder.append("-- Function Declarations\n")
            ir.functions.forEach { function ->
                val functionName = sanitizeName("${function.originalName}_${function.id}")
                codeBuilder.append("local $functionName\n")
            }
            codeBuilder.append("\n")
        }


        private fun generateFunctionImplementations(ir: IR, codeBuilder: StringBuilder) {
            codeBuilder.append("-- Function Implementations\n")
            executionOrder.forEach { id ->
                val function = ir.functions.find { it.id == id }
                if (function != null) {
                    generateFunction(function, codeBuilder)
                    codeBuilder.append("\n")
                }
            }
        }

        private fun generateFunction(function: IRFunction, codeBuilder: StringBuilder) {
            val functionName = sanitizeName("${function.originalName}_${function.id}")

            codeBuilder.append("-- Node: ${function.originalName} (${function.id})\n")
            codeBuilder.append("$functionName = function()\n")

            // Generate local variables for inputs
            function.inputEdges.forEach { inputName ->
                if (inputName in function.inputConnections) {
                    val (sourceNodeId, sourceNodeName) = function.inputConnections[inputName]!!
                    val sourceEdgeName = getSourceEdgeName(workspace, function.id, inputName)
                    val globalVarName = sanitizeName("${sourceNodeName}_${sourceNodeId}_${sanitizeName(sourceEdgeName)}")
                    codeBuilder.append("${indent}local $inputName = globalOutputs['$globalVarName']\n")
                } else {
                    val edge = workspace.graph.getEdges(workspace.graph.getNode(UUID.fromString(function.id))!!).find { it.name == inputName }
                    if (edge != null) {
                        val defaultValue = getDefaultValue(edge)
                        codeBuilder.append("${indent}local $inputName = $defaultValue\n")
                    }
                }
            }

            if (function.inputEdges.isNotEmpty()) {
                codeBuilder.append("\n")
            }

            // Generate function body
            function.body.forEach { statement ->
                generateStatement(statement, codeBuilder, functionName)
            }

            codeBuilder.append("end\n")
        }



        private fun getSourceEdgeName(workspace: Workspace, targetNodeId: String, targetEdgeName: String): String {
            val targetNode = workspace.graph.getNode(UUID.fromString(targetNodeId))
            val targetEdge = workspace.graph.getEdges(targetNode!!).find { it.name == targetEdgeName }
            val sourceLink = workspace.graph.getLinks().find { it.to == targetEdge!!.uid }
            val sourceEdge = workspace.graph.getEdge(sourceLink!!.from)
            return sourceEdge!!.name
        }


        private fun getTargetNodes(workspace: Workspace, edge: Edge): List<Node> {
            val connectedLinks = workspace.graph.getLinks().filter { it.from == edge.uid }
            return connectedLinks.mapNotNull { link ->
                val targetEdge = workspace.graph.getEdge(link.to)
                targetEdge?.let { workspace.graph.getNode(it.owner) }
            }
        }


        private fun generateNodeCall(node: Node): String {
            val functionName = sanitizeName("${node.name}_${node.uid}")
            return "$functionName()"
        }


        private fun generateStatement(statement: IRStatement, codeBuilder: StringBuilder, functionName: String) {
            when (statement) {
                is IRStatement.Literal -> codeBuilder.append("$indent${statement.value}\n")
                is IRStatement.NodeReference -> {
                    val inputCall = generateInputNodeCall(workspace.graph.getEdge(UUID.fromString(statement.name))!!)
                    codeBuilder.append("${indent}local ${statement.name} = $inputCall\n")
                }
                is IRStatement.ExecReference -> {

                }
                is IRStatement.VarReference -> codeBuilder.append("${indent}variables['${statement.name}']\n")
                is IRStatement.LambdaReference -> codeBuilder.append("${indent}${statement.name}\n")
                is IRStatement.JavaImport -> codeBuilder.append("${indent}${statement.name}\n")
                is IRStatement.GenericExpression -> codeBuilder.append("${indent}${statement.content}\n")
                is IRStatement.OutputAssignment -> {
                    val outputKey = "${functionName}_${sanitizeName(statement.name)}"
                    codeBuilder.append("${indent}globalOutputs['$outputKey'] = ${statement.value}\n")
                }
            }
        }


        private fun generateInputNodeCall(edge: Edge): String {
            val sourceNode = getSourceNode(workspace, edge)
            return if (sourceNode != null) {
                val sourceEdge = getSourceEdge(workspace, edge)
                if (sourceEdge != null && sourceEdge.type != "exec") {
                    val sourceFunctionName = sanitizeName("${sourceNode.name}_${sourceNode.uid}")
                    "globalOutputs['${sourceFunctionName}_${sanitizeName(sourceEdge.name)}']"
                } else {
                    getDefaultValue(edge)
                }
            } else {
                getDefaultValue(edge)
            }
        }

//        private fun generateInputNodeCall(edge: Edge): String {
//            val sourceNode = getSourceNode(workspace, edge)
//            return if (sourceNode != null) {
//                val sourceEdge = getSourceEdge(workspace, edge)
//                if (sourceEdge != null) {
//                    val sourceFunctionName = sanitizeName("${sourceNode.name}_${sourceNode.uid}")
//                    "${sourceFunctionName}_${sanitizeName(sourceEdge.name)}()"
//                } else {
//                    getDefaultValue(edge)
//                }
//            } else {
//                getDefaultValue(edge)
//            }
//        }

        private fun getSourceEdge(workspace: Workspace, targetEdge: Edge): Edge? {
            val connectedLink = workspace.graph.getLinks().find { it.to == targetEdge.uid }
            return connectedLink?.let { workspace.graph.getEdge(it.from) }
        }


        private fun getNodeLiteralValue(node: Node): String {
            val nodeTemplate = listener<Schemas>(Endpoint.Side.SERVER).library["${node.type}/${node.name}"]
            return nodeTemplate?.get("value")?.cast<Property.String>()?.get() ?: "nil"
        }

        private fun getDefaultValue(edge: Edge): String {
            val value = edge.value
            if (value.isEmpty) return "nil"

            val type = value["type"]?.cast<Property.String>()?.get() ?: "float"
            val defaultValue = value["default"] ?: Property.Float(0f)

            return when (type) {
                "float" -> (defaultValue as? Property.Float)?.get()?.toString() ?: "0.0"
                "int" -> (defaultValue as? Property.Int)?.get()?.toString() ?: "0"
                "boolean" -> (defaultValue as? Property.Boolean)?.get()?.toString() ?: "false"
                "string" -> "\"${(defaultValue as? Property.String)?.get() ?: ""}\""
                "color" -> "\"${(defaultValue as? Property.String)?.get() ?: "#00000000"}\""
                else -> "nil"
            }
        }


        private fun getSourceNode(workspace: Workspace, edge: Edge): Node? {
            val connectedLink = workspace.graph.getLinks().find { it.to == edge.uid }
            return if (connectedLink != null) {
                val sourceEdge = workspace.graph.getEdge(connectedLink.from)
                if (sourceEdge != null) {
                    workspace.graph.getNode(sourceEdge.owner)
                } else null
            } else null
        }
        private fun generateMainExecution(ir: IR, codeBuilder: StringBuilder) {
            codeBuilder.append("-- Main Execution\n")
            codeBuilder.append("setup()\n")
            codeBuilder.append("local function main()\n")
            executionOrder.forEach { id ->
                val function = ir.functions.find { it.id == id }
                if (function != null) {
                    val functionName = sanitizeName("${function.originalName}_${function.id}")
                    codeBuilder.append("${indent}$functionName()\n")
                }
            }
            codeBuilder.append("end\n\n")
            codeBuilder.append("return {\n")
            ir.functions.filter { it.nodeType == "Events" }.forEach { function ->
                val functionName = sanitizeName("${function.originalName}_${function.id}")
                codeBuilder.append("${indent}${function.originalName} = {main},\n")
            }
            codeBuilder.append("}\n")
        }

//        private fun generateMainExecution(ir: IR, codeBuilder: StringBuilder) {
//            codeBuilder.append("-- Main Execution\n")
//            codeBuilder.append("setup()\n")
//            codeBuilder.append("return {\n")
//            ir.functions.filter { it.nodeType == "Events" }.forEach { function ->
//                val functionName = sanitizeName("${function.originalName}_${function.id}")
//                codeBuilder.append("${indent}${function.originalName} = {\n")
//                codeBuilder.append("$indent$indent$functionName\n")
//                codeBuilder.append("$indent},\n")
//            }
//            codeBuilder.append("}\n")
//        }

        private fun generateValue(value: IRValue): String {
            return when (value) {
                //No need to wrap in braces because it's handled by the multiLineString function
                is IRValue.String -> value.value
                is IRValue.Int -> value.value.toString()
                is IRValue.Float -> value.value.toString()
                is IRValue.Boolean -> value.value.toString()
                is IRValue.Null -> "nil"
            }
        }

        private fun sanitizeName(name: String): String = name.replace(Regex("[^a-zA-Z0-9_]"), "_")
    }
    // Data classes and enums
    data class Token(val type: TokenType, val value: String)
    enum class TokenType { NODE_START, NODE_END, EDGE, EXPRESSION, LITERAL }
    sealed class IROutputTemplate {
        data class VariableAssignment(val variableName: String, val value: String) : IROutputTemplate()
        data class ExecCall(val execName: String) : IROutputTemplate()
    }

    class AST {

        val nodes = mutableListOf<ASTNode.NodeDeclaration>()
        val edges = mutableListOf<ASTNode.EdgeDeclaration>()
    }

    sealed class ASTNode { data class NodeDeclaration(
        val id: String, val children: MutableList<ASTNode> = mutableListOf()
    ) : ASTNode()

        data class EdgeDeclaration(val id: String) : ASTNode()
        data class Literal(val value: String) : ASTNode()
        data class NodeReference(val name: String) : ASTNode()
        data class ExecReference(val name: String) : ASTNode()
        data class VarReference(val name: String) : ASTNode()
        data class LambdaReference(val name: String) : ASTNode()
        data class JavaImport(val name: String) : ASTNode()
        data class SetupBlock(val content: String) : ASTNode()
        data class GenericExpression(val content: String) : ASTNode()
        data class OutputAssignment(val name: String, val value: String) : ASTNode()
    }

    class IR {

        val variables = mutableMapOf<String, IRValue>()
        val functions = mutableListOf<IRFunction>()
        val edges = mutableListOf<IREdge>()
    }

    data class IRFunction(
        val id: String,
        val originalName: String,
        val nodeType: String,
        val inputEdges: List<String>,
        val inputConnections: Map<String, Pair<String, String>>,
        val outputEdges: Map<String, List<Pair<String, String>>>,
        val body: MutableList<IRStatement> = mutableListOf(),
        val setupBlocks: MutableSet<String> = mutableSetOf(),
    )

    data class IREdge(val id: String)

    sealed class IRValue { data class String(val value: kotlin.String) : IRValue()
        data class Int(val value: kotlin.Int) : IRValue()
        data class Float(val value: kotlin.Float) : IRValue()
        data class Boolean(val value: kotlin.Boolean) : IRValue()
        object Null : IRValue()
    }

    sealed class IRStatement { data class Literal(val value: String) : IRStatement()
        data class NodeReference(val name: String) : IRStatement()
        data class ExecReference(val name: String) : IRStatement()
        data class VarReference(val name: String) : IRStatement()
        data class LambdaReference(val name: String) : IRStatement()
        data class JavaImport(val name: String) : IRStatement()
        data class GenericExpression(val content: String) : IRStatement()
        data class OutputAssignment(val name: String, val value: String) : IRStatement()
    }
    // Helper functions
    private fun sanitizeName(name: String): String = name.replace(Regex("[^a-zA-Z0-9_]"), "_")
}