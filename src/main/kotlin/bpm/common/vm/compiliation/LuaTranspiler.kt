package bpm.common.vm.compiliation

import bpm.Bpm
import bpm.server.lua.LuaBuiltin
import bpm.common.logging.KotlinLogging
import bpm.common.workspace.Workspace
import bpm.common.workspace.graph.Edge
import bpm.common.workspace.graph.Node
import bpm.common.property.Property
import bpm.common.property.cast
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object LuaTranspiler {

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

    class CodeGenerator(private val workspace: Workspace) {

        private val indent = "  "
        private val nodeDependencies = mutableMapOf<String, MutableSet<String>>()
        private val executionOrder = mutableListOf<String>()
        private val nodeOutputs = mutableMapOf<String, Set<String>>()
        private val functionCalls = mutableMapOf<String, MutableSet<String>>()
        fun generate(ir: IR): String {
            val codeBuilder = StringBuilder()
            buildDependencies(ir)
            buildNodeOutputs(ir)
            determineExecutionOrder(ir)

            generateWorkspaceAccessor(workspace, codeBuilder)
            generateVariableInitializations(ir, codeBuilder)
            generateBuiltIns(codeBuilder)
            generateGlobalOutputsTable(codeBuilder)
            generateFunctionDeclarations(ir, codeBuilder)
            generateFunctionImplementations(ir, codeBuilder)
            generateEventHandlers(ir, codeBuilder)
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

        private fun buildNodeOutputs(ir: IR) {
            ir.functions.forEach { function ->
                nodeOutputs[function.id] = function.body
                    .filterIsInstance<IRStatement.OutputAssignment>()
                    .map { it.name }
                    .toSet()
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


        private fun generateFunctionDeclarations(ir: IR, codeBuilder: StringBuilder) {
            codeBuilder.append("-- Function Declarations\n")
            ir.functions.forEach { function ->
                val functionName = sanitizeName("${function.originalName}_${function.id}")
                codeBuilder.append("local $functionName\n")
            }
            codeBuilder.append("\n")
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


        private fun generateFunctionImplementations(ir: IR, codeBuilder: StringBuilder) {
            codeBuilder.append("-- Function Implementations\n")
            executionOrder.forEach { functionId ->
                val function = ir.functions.find { it.id == functionId }
                if (function != null) {
                    generateFunction(function, ir, codeBuilder)
                    codeBuilder.append("\n")
                }
            }
        }


        private fun generateFunction(function: IRFunction, ir: IR, codeBuilder: StringBuilder) {
            val functionName = sanitizeName("${function.originalName}_${function.id}")

            codeBuilder.append("-- Node: ${function.originalName} (${function.id})\n")
            codeBuilder.append("$functionName = function()\n")

            generateFunctionBody(function, ir, codeBuilder, "${indent}")

            codeBuilder.append("end\n")
        }

        private fun generateFunctionBody(function: IRFunction, ir: IR, codeBuilder: StringBuilder, indent: String) {

            if (function.nodeType == "Functions") {
                generateFunctionInstanceBody(function, workspace, codeBuilder, indent)
                return
            }

            // Generate calls to nodes without execution flow
            function.inputConnections.forEach { (_, sourcePair) ->
                val sourceFunction = ir.functions.find { it.id == sourcePair.first }
                if (sourceFunction != null && !hasExecInput(sourceFunction)) {
                    val sourceFunctionName = sanitizeName("${sourceFunction.originalName}_${sourceFunction.id}")
                    codeBuilder.append("$indent$sourceFunctionName()\n")
                }
            }

            // Generate inputs
            function.inputEdges.forEach { inputName ->
                if (inputName in function.inputConnections) {
                    val (sourceNodeId, sourceNodeName) = function.inputConnections[inputName]!!
                    val sourceEdgeName = getSourceEdgeName(workspace, function.id, inputName)
                    val sourceFunctionName = sanitizeName("${sourceNodeName}_$sourceNodeId")
                    val sourceOutputKey = "${sourceFunctionName}_${sanitizeName(sourceEdgeName)}"

                    codeBuilder.append("$indent$inputName = globalOutputs['$sourceOutputKey']\n")
                } else {
                    val edge = workspace.graph.getEdges(workspace.graph.getNode(UUID.fromString(function.id))!!)
                        .find { it.name == inputName }
                    if (edge != null) {
                        val defaultValue = getDefaultValue(edge)
                        codeBuilder.append("$indent$inputName = $defaultValue\n")
                    }
                }
            }

            // Generate function body
            function.body.forEach { statement ->
                generateStatement(statement, ir, codeBuilder, indent, function)
            }
        }

        private fun generateFunctionInstanceBody(
            function: IRFunction,
            workspace: Workspace,
            codeBuilder: StringBuilder,
            indent: String
        ) {
            val functionNode = workspace.graph.getNode(UUID.fromString(function.id)) ?: return
            val actualFunction = workspace.graph.getFunction(functionNode.function) ?: return

            // Handle input edges connected to this function instance
            workspace.graph.getEdges(functionNode)
                .filter { it.direction == "input" && it.type != "exec" }
                .forEach { inputEdge ->
                    // Find if this input is connected to something
                    val sourceLink = workspace.graph.links.find { it.to == inputEdge.uid }
                    if (sourceLink != null) {
                        val sourceEdge = workspace.graph.getEdge(sourceLink.from)
                        if (sourceEdge != null) {
                            val sourceNode = workspace.graph.getNode(sourceEdge.owner)
                            if (sourceNode != null) {
                                // Get value from the connected node's output
                                val sourceFunctionName = sanitizeName("${sourceNode.name}_${sourceNode.uid}")
                                val sourceOutputKey = "${sourceFunctionName}_${sanitizeName(sourceEdge.name)}"
                                codeBuilder.append("${indent}local ${sanitizeName(inputEdge.name)} = globalOutputs['$sourceOutputKey']\n")

                                // Call any value nodes connected to inputs
                                if (!hasExecInput(sourceNode, workspace)) {
                                    codeBuilder.append("${indent}$sourceFunctionName()\n")
                                }
                            }
                        }
                    } else {
                        // Use default value if not connected
                        val defaultValue = getDefaultValue(inputEdge)
                        codeBuilder.append("${indent}local ${sanitizeName(inputEdge.name)} = $defaultValue\n")
                    }
                }

            // Pass through values from inputs to outputs
            workspace.graph.getEdges(functionNode)
                .filter { it.direction == "output" && it.type != "exec" }
                .forEach { outputEdge ->
                    // Find corresponding input edge on the function implementation
                    val functionInputEdge = workspace.graph.getEdges(actualFunction.uid)
                        .find { it.direction == "input" && it.name == outputEdge.name }

                    if (functionInputEdge != null) {
                        val funcName = sanitizeName("${functionNode.name}_${functionNode.uid}")
                        val outputKey = "${funcName}_${sanitizeName(outputEdge.name)}"
                        val inputName = sanitizeName(functionInputEdge.name)
                        codeBuilder.append("${indent}globalOutputs['$outputKey'] = $inputName\n")
                    }
                }

            // Handle contained nodes in the function
            workspace.graph.getEdges(actualFunction.uid)
                .filter { it.direction == "input" && it.type == "exec" }
                .forEach { execInputEdge ->
                    // Handle exec flow by following connection links
                    val execLink = workspace.graph.links.find { it.to == execInputEdge.uid }
                    if (execLink != null) {
                        val sourceEdge = workspace.graph.getEdge(execLink.from)
                        if (sourceEdge != null) {
                            val sourceNode = workspace.graph.getNode(sourceEdge.owner)
                            if (sourceNode != null) {
                                // Call source node
                                val sourceFuncName = sanitizeName("${sourceNode.name}_${sourceNode.uid}")
                                codeBuilder.append("${indent}$sourceFuncName()\n")
                            }
                        }
                    }
                }

            // Handle execution outputs
            workspace.graph.getEdges(functionNode)
                .filter { it.direction == "output" && it.type == "exec" }
                .forEach { execEdge ->
                    val targetLinks = workspace.graph.links.filter { it.from == execEdge.uid }
                    targetLinks.forEach { link ->
                        val targetEdge = workspace.graph.getEdge(link.to)
                        if (targetEdge != null) {
                            val targetNode = workspace.graph.getNode(targetEdge.owner)
                            if (targetNode != null) {
                                val targetFuncName = sanitizeName("${targetNode.name}_${targetNode.uid}")
                                codeBuilder.append("${indent}$targetFuncName()\n")
                            }
                        }
                    }
                }
        }


        private fun hasExecInput(node: Node, workspace: Workspace): Boolean {
            return workspace.graph.getEdges(node)
                .any { it.direction == "input" && it.type == "exec" }
        }

        private fun hasExecInput(function: IRFunction): Boolean {
            return workspace.graph.getEdges(workspace.graph.getNode(UUID.fromString(function.id))!!)
                .any { it.direction == "input" && it.type == "exec" }
        }

        private fun generateStatement(
            statement: IRStatement,
            ir: IR,
            codeBuilder: StringBuilder,
            indent: String,
            currentFunction: IRFunction
        ) {
            when (statement) {
                is IRStatement.Literal -> codeBuilder.append("$indent${statement.value}\n")
                is IRStatement.NodeReference -> {
                    val referencedFunction = ir.functions.find { it.id == statement.name }
                    if (referencedFunction != null) {
                        val referencedFunctionName = sanitizeName("${referencedFunction.originalName}_${referencedFunction.id}")
                        codeBuilder.append("$indent$referencedFunctionName()\n")
                    }
                }

                is IRStatement.ExecReference -> {
                    val execEdge = currentFunction.outputEdges[statement.name]
                    execEdge?.forEach { (targetNodeId, targetNodeName) ->
                        val targetFunctionName = sanitizeName("${targetNodeName}_$targetNodeId")
                        codeBuilder.append("$indent$targetFunctionName()\n")
                    }
                }

                is IRStatement.GenericExpression -> codeBuilder.append("$indent${statement.content}\n")
                is IRStatement.OutputAssignment -> {
                    val outputKey = "${sanitizeName("${currentFunction.originalName}_${currentFunction.id}")}_${
                        sanitizeName(
                            statement.name
                        )
                    }"
                    codeBuilder.append("${indent}globalOutputs['$outputKey'] = ${statement.value}\n")
                }

                else -> {}
            }
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

        private fun getSourceEdgeName(workspace: Workspace, targetNodeId: String, targetEdgeName: String): String {
            val targetNode = workspace.graph.getNode(UUID.fromString(targetNodeId))
            val targetEdge = workspace.graph.getEdges(targetNode!!)
                .find { it.name == targetEdgeName && it.direction == "input" }

            if (targetEdge == null) {
                throw IllegalStateException("Target edge not found: $targetEdgeName for node $targetNodeId")
            }

            val sourceLink = workspace.graph.links.find { it.to == targetEdge.uid }
            if (sourceLink == null) {
                throw IllegalStateException("No source link found for edge: ${targetEdge.uid}")
            }

            val sourceEdge = workspace.graph.getEdge(sourceLink.from)
            if (sourceEdge == null) {
                throw IllegalStateException("Source edge not found for link: ${sourceLink.from}")
            }

            return sourceEdge.name
        }

        private fun generateEventHandlers(ir: IR, codeBuilder: StringBuilder) {
            codeBuilder.append("-- Event Handlers\n")
            codeBuilder.append("return {\n")
            ir.functions.filter { it.nodeType == "Events" }.forEach { function ->
                val functionName = sanitizeName("${function.originalName}_${function.id}")
                codeBuilder.append("$indent${function.originalName} = {$functionName},\n")
            }
            codeBuilder.append("}\n")
        }


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

    // Helper functions
     fun sanitizeName(name: String): String = name.replace(Regex("[^a-zA-Z0-9_]"), "_")
}