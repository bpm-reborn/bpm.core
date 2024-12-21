package bpm.common.vm

import bpm.common.network.Endpoint
import bpm.common.network.NetUtils
import bpm.common.network.listener
import bpm.common.property.PropertyMap
import bpm.common.upstream.Schemas
import bpm.common.vm.compiliation.IR
import bpm.common.vm.compiliation.IRFunction
import bpm.common.vm.compiliation.IRStatement
import bpm.common.vm.compiliation.IRValue
import bpm.common.workspace.Workspace
import bpm.common.workspace.graph.Node
import java.util.*

/**
 * Handles pretty printing of the IR structure for debugging purposes.
 * Provides a hierarchical view of nodes, functions, variables, and their relationships.
 */
object IRDebugPrinter {
    private const val INDENT = "  "

    /**
     * Creates a formatted string representation of the entire IR.
     */
    fun formatIR(ir: IR, workspace: Workspace? = null): String {
        val sb = StringBuilder()

        // Header with workspace info if available
        sb.appendLine("=".repeat(80))
        sb.appendLine("IR Structure Debug View")
        if (workspace != null) {
            sb.appendLine("Workspace: ${workspace.workspaceName} (${workspace.uid})")
            sb.appendLine("Total Nodes: ${workspace.graph.nodes.size}")
            sb.appendLine("Total Edges: ${workspace.graph.edges.size}")
            sb.appendLine("Total Links: ${workspace.graph.links.size}")
        }
        sb.appendLine("=".repeat(80))
        sb.appendLine()

        // Detailed workspace analysis
        if (workspace != null) {
            sb.appendLine("Workspace Analysis:")
            sb.appendLine("-".repeat(40))
            formatWorkspaceAnalysis(workspace, sb)
            sb.appendLine()
        }

        // Variables section with type information
        sb.appendLine("Variables:")
        sb.appendLine("-".repeat(40))
        if (ir.variables.isEmpty()) {
            sb.appendLine("${INDENT}No variables defined")
        } else {
            ir.variables.forEach { (name, value) ->
                sb.appendLine("$INDENT$name = ${formatValue(value)} (${value::class.simpleName})")
            }
        }
        sb.appendLine()

        // Functions section with detailed analysis
        sb.appendLine("Functions:")
        sb.appendLine("-".repeat(40))
        if (ir.functions.isEmpty()) {
            sb.appendLine("${INDENT}No functions generated")
            if (workspace != null) {
                sb.appendLine("\nFunction Generation Analysis:")
                workspace.graph.nodes.forEach { node ->
                    sb.appendLine("${INDENT}Node ${node.name} (${node.uid}):")
                    sb.appendLine("${INDENT}${INDENT}Type: ${node.type}")
                    sb.appendLine("${INDENT}${INDENT}Function Instance: ${node.function}")
                    formatNodeAnalysis(node, workspace, sb)
                }
            }
        } else {
            ir.functions.forEach { function ->
                formatFunction(function, workspace, sb)
                sb.appendLine()
            }
        }

        // Edges section with connection info
        sb.appendLine("Edges:")
        sb.appendLine("-".repeat(40))
        if (ir.edges.isEmpty()) {
            sb.appendLine("${INDENT}No edges defined")
        } else {
            ir.edges.forEach { edge ->
                sb.appendLine("$INDENT${edge.id}")
                if (workspace != null) {
                    val edgeData = workspace.getEdge(UUID.fromString(edge.id))
                    if (edgeData != null) {
                        sb.appendLine("${INDENT}${INDENT}Name: ${edgeData.name}")
                        sb.appendLine("${INDENT}${INDENT}Type: ${edgeData.type}")
                        sb.appendLine("${INDENT}${INDENT}Direction: ${edgeData.direction}")
                        sb.appendLine("${INDENT}${INDENT}Owner: ${edgeData.owner}")
                    }
                }
            }
        }

        // Template resolution status
        if (workspace != null) {
            sb.appendLine("\nTemplate Resolution Status:")
            sb.appendLine("-".repeat(40))
            workspace.graph.nodes.forEach { node ->
                sb.appendLine("${INDENT}Node: ${node.name} (${node.type}/${node.name})")
                val schemas = listener<Schemas>(Endpoint.Side.SERVER)
                val template = schemas.library["${node.type}/${node.name}"]
                if (template != null) {
                    sb.appendLine("${INDENT}${INDENT}✓ Template found")
                    sb.appendLine("${INDENT}${INDENT}Source: ${template.properties.contains("source")}")
                    sb.appendLine("${INDENT}${INDENT}Override: ${node.properties.contains("override")}")
                } else {
                    sb.appendLine("${INDENT}${INDENT}✗ Template not found")
                }
            }
        }

        // Dependency graph
        sb.appendLine("\nDependency Graph:")
        sb.appendLine("-".repeat(40))
        formatDependencyGraph(ir.functions, sb)

        return sb.toString()
    }


    private fun formatValue(value: IRValue): String {
        return when (value) {
            is IRValue.String -> "\"${value.value}\""
            is IRValue.Int -> value.value.toString()
            is IRValue.Float -> value.value.toString()
            is IRValue.Boolean -> value.value.toString()
            is IRValue.Null -> "nil"
        }
    }

    private fun formatFunction(
        function: IRFunction,
        workspace: Workspace?,
        sb: StringBuilder
    ) {
        // Function header with enhanced metadata
        sb.appendLine("$INDENT[Function] ${function.originalName} (${function.nodeType})")
        sb.appendLine("$INDENT${INDENT}ID: ${function.id}")

        // Add function instance context if available
        if (workspace != null) {
            val node = workspace.getNode(UUID.fromString(function.id))
            if (node != null) {
                // Show function instance relationship
                if (node.function != NetUtils.DefaultUUID) {
                    val parentFunction = workspace.graph.getFunction(node.function)
                    if (parentFunction != null) {
                        sb.appendLine("$INDENT${INDENT}Parent Function: ${parentFunction.name} (${parentFunction.uid})")
                    }
                }

                // Show node template information
                val template = listener<Schemas>(Endpoint.Side.SERVER).library["${node.type}/${node.name}"]
                if (template != null) {
                    sb.appendLine("$INDENT${INDENT}Template: ✓ Found")
                    sb.appendLine("$INDENT${INDENT}Has Source: ${template.properties.contains("source")}")
                    sb.appendLine("$INDENT${INDENT}Has Override: ${node.properties.contains("override")}")
                } else {
                    sb.appendLine("$INDENT${INDENT}Template: ✗ Not Found")
                }
            }
        }

        // Input edges with detailed information
        sb.appendLine("$INDENT${INDENT}Input Edges:")
        if (function.inputEdges.isEmpty()) {
            sb.appendLine("$INDENT$INDENT$INDENT(No input edges)")
        } else {
            function.inputEdges.forEach { edge ->
                sb.appendLine("$INDENT$INDENT$INDENT- $edge")
                // Show edge details if workspace is available
                if (workspace != null) {
                    val node = workspace.getNode(UUID.fromString(function.id))
                    if (node != null) {
                        val edgeObj = workspace.graph.getEdges(node)
                            .find { it.name == edge }
                        if (edgeObj != null) {
                            sb.appendLine("$INDENT$INDENT$INDENT  Type: ${edgeObj.type}")
                            sb.appendLine("$INDENT$INDENT$INDENT  Direction: ${edgeObj.direction}")
                            if (!edgeObj.value.isEmpty) {
                                sb.appendLine("$INDENT$INDENT$INDENT  Default Value: ${formatEdgeValue(edgeObj.value)}")
                            }
                        }
                    }
                }
            }
        }

        // Input connections with flow analysis
        sb.appendLine("$INDENT${INDENT}Input Connections:")
        if (function.inputConnections.isEmpty()) {
            sb.appendLine("$INDENT$INDENT$INDENT(No input connections)")
        } else {
            function.inputConnections.forEach { (edge, sourcePair) ->
                sb.appendLine("$INDENT$INDENT$INDENT$edge <- ${sourcePair.second} (${sourcePair.first})")
                // Show connection details if workspace is available
                if (workspace != null) {
                    val sourceNode = workspace.getNode(UUID.fromString(sourcePair.first))
                    if (sourceNode != null) {
                        // Show data flow type
                        val sourceEdge = workspace.graph.getEdges(sourceNode)
                            .find { it.name == edge }
                        if (sourceEdge != null) {
                            sb.appendLine("$INDENT$INDENT$INDENT  Flow Type: ${sourceEdge.type}")
                            // Show if this is part of a function instance
                            if (sourceNode.function != NetUtils.DefaultUUID) {
                                val functionInstance = workspace.graph.getFunction(sourceNode.function)
                                if (functionInstance != null) {
                                    sb.appendLine("$INDENT$INDENT$INDENT  In Function: ${functionInstance.name}")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Output edges with execution flow
        sb.appendLine("$INDENT${INDENT}Output Edges:")
        if (function.outputEdges.isEmpty()) {
            sb.appendLine("$INDENT$INDENT$INDENT(No output edges)")
        } else {
            function.outputEdges.forEach { (edge, targets) ->
                sb.appendLine("$INDENT$INDENT$INDENT$edge ->")
                targets.forEach { target ->
                    sb.appendLine("$INDENT$INDENT$INDENT$INDENT${target.second} (${target.first})")
                    // Show execution flow details
                    if (workspace != null) {
                        val targetNode = workspace.getNode(UUID.fromString(target.first))
                        if (targetNode != null) {
                            workspace.graph.getEdges(targetNode)
                                .filter { it.direction == "input" && it.type == "exec" }
                                .forEach { execEdge ->
                                    sb.appendLine("$INDENT$INDENT$INDENT$INDENT  Triggers: ${execEdge.name}")
                                }
                        }
                    }
                }
            }
        }

        // Setup blocks with documentation
        if (function.setupBlocks.isNotEmpty()) {
            sb.appendLine("$INDENT${INDENT}Setup Blocks:")
            function.setupBlocks.forEachIndexed { index, block ->
                sb.appendLine("$INDENT$INDENT$INDENT[$index] $block")
            }
        }

        // Function body with enhanced statement analysis
        sb.appendLine("$INDENT${INDENT}Body:")
        if (function.body.isEmpty()) {
            sb.appendLine("$INDENT$INDENT$INDENT(Empty body)")
        } else {
            function.body.forEachIndexed { index, statement ->
                formatStatement(statement, sb, 3)
                // Add context for node references
                if (statement is IRStatement.NodeReference && workspace != null) {
                    val referencedNode = workspace.getNode(UUID.fromString(statement.name))
                    if (referencedNode != null) {
                        sb.appendLine("$INDENT$INDENT$INDENT  Referenced Node: ${referencedNode.name} (${referencedNode.type})")
                    }
                }
            }
        }

        // Dependencies with relationship analysis
        sb.appendLine("$INDENT${INDENT}Dependencies:")
        if (function.dependencies.isEmpty()) {
            sb.appendLine("$INDENT$INDENT$INDENT(No dependencies)")
        } else {
            function.dependencies.forEach { dep ->
                sb.appendLine("$INDENT$INDENT$INDENT-> ${dep.originalName} (${dep.id})")
                if (workspace != null) {
                    // Show why this dependency exists
                    val depNode = workspace.getNode(UUID.fromString(dep.id))
                    if (depNode != null) {
                        val relationships = getNodeRelationships(
                            UUID.fromString(function.id),
                            depNode.uid,
                            workspace
                        )
                        relationships.forEach { relationship ->
                            sb.appendLine("$INDENT$INDENT$INDENT  $relationship")
                        }
                    }
                }
            }
        }
    }

    /**
     * Helper function to analyze relationships between nodes
     */
    private fun getNodeRelationships(
        sourceId: UUID,
        targetId: UUID,
        workspace: Workspace
    ): List<String> {
        val relationships = mutableListOf<String>()
        val sourceNode = workspace.getNode(sourceId) ?: return relationships
        val targetNode = workspace.getNode(targetId) ?: return relationships

        // Check data flow dependencies
        workspace.graph.getEdges(sourceNode)
            .filter { it.direction == "input" }
            .forEach { edge ->
                workspace.graph.links
                    .filter { it.to == edge.uid }
                    .forEach { link ->
                        val sourceEdge = workspace.getEdge(link.from)
                        if (sourceEdge?.owner == targetId) {
                            relationships.add("Uses data from ${targetNode.name}.${sourceEdge.name}")
                        }
                    }
            }

        // Check execution flow dependencies
        workspace.graph.getEdges(sourceNode)
            .filter { it.direction == "input" && it.type == "exec" }
            .forEach { edge ->
                workspace.graph.links
                    .filter { it.to == edge.uid }
                    .forEach { link ->
                        val sourceEdge = workspace.getEdge(link.from)
                        if (sourceEdge?.owner == targetId) {
                            relationships.add("Executes after ${targetNode.name}.${sourceEdge.name}")
                        }
                    }
            }

        return relationships
    }


    private fun formatEdgeValue(value: PropertyMap): String {
        return buildString {
            append("{")
            value.forEach { (key, prop) ->
                append("$key: ${prop.get()}, ")
            }
            if (length > 1) {
                setLength(length - 2)  // Remove trailing comma and space
            }
            append("}")
        }
    }


    private fun formatStatement(
        statement: IRStatement,
        sb: StringBuilder,
        indentLevel: Int
    ) {
        val indent = INDENT.repeat(indentLevel)
        when (statement) {
            is IRStatement.Literal -> {
                sb.appendLine("$indent[Literal]")
                // Split and indent multiple lines
                statement.value.lines().forEach { line ->
                    sb.appendLine("$indent$INDENT$line")
                }
            }
            is IRStatement.NodeReference ->
                sb.appendLine("$indent[NodeRef] ${statement.name}")
            is IRStatement.ExecReference ->
                sb.appendLine("$indent[ExecRef] ${statement.name}")
            is IRStatement.VarReference ->
                sb.appendLine("$indent[VarRef] ${statement.name}")
            is IRStatement.LambdaReference ->
                sb.appendLine("$indent[Lambda] ${statement.name}")
            is IRStatement.JavaImport ->
                sb.appendLine("$indent[Import] ${statement.name}")
            is IRStatement.GenericExpression ->
                sb.appendLine("$indent[Expr] ${statement.content}")
            is IRStatement.OutputAssignment ->
                sb.appendLine("$indent[Output] ${statement.name} = ${statement.value}")
        }
    }

    private fun formatDependencyGraph(functions: List<IRFunction>, sb: StringBuilder) {
        // Create adjacency map
        val adjacencyMap = mutableMapOf<String, Set<String>>()
        functions.forEach { function ->
            adjacencyMap[function.id] = function.dependencies.map { it.id }.toSet()
        }

        // Helper function to format a node in the graph
        fun formatNode(nodeId: String, visited: MutableSet<String>, depth: Int = 0) {
            val indent = INDENT.repeat(depth)
            val function = functions.find { it.id == nodeId }
            if (function != null) {
                val marker = if (visited.contains(nodeId)) "*" else ""
                sb.appendLine("$indent${function.originalName} ($nodeId)$marker")

                if (!visited.contains(nodeId)) {
                    visited.add(nodeId)
                    adjacencyMap[nodeId]?.forEach { dependencyId ->
                        formatNode(dependencyId, visited, depth + 1)
                    }
                }
            }
        }

        // Format each root node (nodes with no incoming edges)
        val allDependencies = adjacencyMap.values.flatten().toSet()
        val rootNodes = functions.filter { !allDependencies.contains(it.id) }

        rootNodes.forEach { root ->
            formatNode(root.id, mutableSetOf())
            sb.appendLine()
        }

    }

    private fun formatWorkspaceAnalysis(workspace: Workspace, sb: StringBuilder) {
        // Analyze function instances
        val functionInstances = workspace.graph.functions
        sb.appendLine("${INDENT}Function Instances: ${functionInstances.size}")
        functionInstances.forEach { function ->
            sb.appendLine("${INDENT}${INDENT}Function: ${function.name} (${function.uid})")
            sb.appendLine("${INDENT}${INDENT}Nodes: ${function.nodes.size}")
            function.nodes.forEach { nodeRef ->
                val node = workspace.getNode(nodeRef.get())
                if (node != null) {
                    sb.appendLine("${INDENT}${INDENT}${INDENT}- ${node.name} (${node.type})")
                }
            }
        }

        // Analyze node relationships
        val standaloneNodes = workspace.graph.nodes.filter { it.function == NetUtils.DefaultUUID }
        sb.appendLine("\n${INDENT}Standalone Nodes: ${standaloneNodes.size}")
        standaloneNodes.forEach { node ->
            sb.appendLine("${INDENT}${INDENT}- ${node.name} (${node.type})")
        }
    }

    private fun formatNodeAnalysis(node: Node, workspace: Workspace, sb: StringBuilder) {
        // Analyze edges
        val edges = workspace.graph.getEdges(node)
        sb.appendLine("${INDENT}${INDENT}Edges:")
        edges.forEach { edge ->
            sb.appendLine("${INDENT}${INDENT}${INDENT}- ${edge.name} (${edge.type}, ${edge.direction})")
            // Show connections
            workspace.graph.links.filter { it.from == edge.uid || it.to == edge.uid }.forEach { link ->
                val otherEnd = if (link.from == edge.uid) link.to else link.from
                val otherEdge = workspace.getEdge(otherEnd)
                val otherNode = otherEdge?.let { workspace.getNode(it.owner) }
                if (otherNode != null) {
                    sb.appendLine("${INDENT}${INDENT}${INDENT}  Connected to: ${otherNode.name}.${otherEdge.name}")
                }
            }
        }
    }

    /**
     * Additional debugging helper methods
     */
    object DebugHelpers {
        /**
         * Formats a specific function's context for debugging
         */
        fun formatFunctionContext(
            function: IRFunction,
            workspace: Workspace
        ): String {
            val sb = StringBuilder()

            sb.appendLine("Function Context Debug:")
            sb.appendLine("=".repeat(40))
            sb.appendLine("Name: ${function.originalName}")
            sb.appendLine("ID: ${function.id}")
            sb.appendLine("Type: ${function.nodeType}")

            // Input/Output analysis
            sb.appendLine("\nInput Analysis:")
            function.inputEdges.forEach { inputName ->
                val connection = function.inputConnections[inputName]
                if (connection != null) {
                    sb.appendLine("$INDENT$inputName <- ${connection.second} (${connection.first})")
                } else {
                    sb.appendLine("$INDENT$inputName : No connection")
                }
            }

            // Statement analysis
            sb.appendLine("\nStatement Analysis:")
            function.body.forEachIndexed { index, statement ->
                sb.appendLine("$INDENT[$index] ${analyzeStatement(statement)}")
            }

            return sb.toString()
        }

        private fun analyzeStatement(statement: IRStatement): String {
            return when (statement) {
                is IRStatement.Literal ->
                    "Literal (${statement.value.lines().count()} lines)"
                is IRStatement.NodeReference ->
                    "Node Reference to ${statement.name}"
                is IRStatement.ExecReference ->
                    "Execution Flow to ${statement.name}"
                is IRStatement.VarReference ->
                    "Variable Access: ${statement.name}"
                is IRStatement.LambdaReference ->
                    "Lambda Reference: ${statement.name}"
                is IRStatement.JavaImport ->
                    "Java Import: ${statement.name}"
                is IRStatement.GenericExpression ->
                    "Expression: ${statement.content}"
                is IRStatement.OutputAssignment ->
                    "Output Assignment: ${statement.name} = ${statement.value}"
            }
        }
    }
}