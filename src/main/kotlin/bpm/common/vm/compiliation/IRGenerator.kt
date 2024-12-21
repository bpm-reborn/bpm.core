package bpm.common.vm.compiliation

import bpm.common.logging.KotlinLogging
import bpm.common.property.Property
import bpm.common.vm.IRDebugPrinter
import bpm.common.workspace.Workspace
import bpm.common.workspace.graph.Edge
import bpm.common.workspace.graph.Node
import java.util.*

class IRGenerator {
    private val logger = KotlinLogging.logger {}

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

        // Resolve dependencies
        resolveDependencies(ir.functions, workspace)

        // Process edges
        ast.edges.forEach { edge ->
            ir.edges.add(IREdge(edge.id))
        }

        logger.debug(IRDebugPrinter.formatIR(ir, workspace))

        return ir
    }

    private fun resolveDependencies(functions: List<IRFunction>, workspace: Workspace) {
        functions.forEach { function ->
            function.body.forEach { statement ->
                when (statement) {
                    is IRStatement.NodeReference -> {
                        val referencedFunction = functions.find { it.id == statement.name }
                        if (referencedFunction != null && !function.dependencies.contains(referencedFunction)) {
                            function.dependencies.add(referencedFunction)
                        }
                    }

                    is IRStatement.ExecReference -> {
                        val referencedFunction = functions.find { it.id == statement.name }
                        if (referencedFunction != null && !function.dependencies.contains(referencedFunction)) {
                            function.dependencies.add(referencedFunction)
                        }
                    }

                    is IRStatement.OutputAssignment -> {
                        val outputEdge = workspace.graph.getEdges(workspace.graph.getNode(UUID.fromString(function.id))!!)
                            .find { it.name == statement.name && it.direction == "output" }
                        if (outputEdge != null) {
                            val targetNodes = getTargetNodes(workspace, outputEdge)
                            targetNodes.forEach { targetNode ->
                                val targetFunction = functions.find { it.id == targetNode.uid.toString() }
                                if (targetFunction != null && !function.dependencies.contains(targetFunction)) {
                                    function.dependencies.add(targetFunction)
                                }
                            }
                        }
                    }
                    // Ignore other statement types
                    else -> {}
                }
            }
        }
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
        val connectedLinks = workspace.graph.links.filter { it.from == edge.uid }
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

                is ASTNode.ExecReference -> function.body.add(
                    IRStatement.ExecReference(
                        child.name
                    )
                )
                is ASTNode.VarReference -> function.body.add(IRStatement.VarReference(child.name))
                is ASTNode.LambdaReference -> function.body.add(
                    IRStatement.LambdaReference(
                        child.name
                    )
                )
                is ASTNode.JavaImport -> function.body.add(IRStatement.JavaImport(child.name))
                is ASTNode.SetupBlock -> function.setupBlocks.add(child.content.removePrefix("{").removeSuffix("}"))
                is ASTNode.GenericExpression -> function.body.add(
                    IRStatement.GenericExpression(
                        child.content
                    )
                )
                is ASTNode.OutputAssignment -> function.body.add(
                    IRStatement.OutputAssignment(
                        child.name,
                        child.value
                    )
                )

                else -> {} // Ignore other node types
            }
        }

        return function
    }

    private fun getSourceNode(workspace: Workspace, edge: Edge): Node? {
        val connectedLink = workspace.graph.links.find { it.to == edge.uid }
        return if (connectedLink != null) {
            val sourceEdge = workspace.graph.getEdge(connectedLink.from)
            if (sourceEdge != null) {
                workspace.graph.getNode(sourceEdge.owner)
            } else null
        } else null
    }
}