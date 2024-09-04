package bpm.common.vm

import bpm.common.logging.KotlinLogging
import bpm.common.network.Endpoint
import bpm.common.network.listener
import bpm.common.property.*
import bpm.common.schemas.Schemas
import bpm.common.workspace.Workspace
import bpm.common.workspace.graph.Edge
import bpm.common.workspace.graph.Node
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object LuaTranspiler {

    private val logger = KotlinLogging.logger {}
    private val nodeDependencies = ConcurrentHashMap<UUID, MutableSet<UUID>>()
    private val generatedFunctions = mutableSetOf<UUID>()
    private val codeBuilder = StringBuilder()
    private val sanitizeRegex = Regex("[^a-zA-Z0-9_]")
    private lateinit var currentNode: Node

    fun generateLuaScript(workspace: Workspace): String {
        nodeDependencies.clear()
        generatedFunctions.clear()
        codeBuilder.clear()
        codeBuilder.append("-- Generated Lua Script\n\n")

        initializeVariables(workspace)
        generateSetup()
        analyzeDependencies(workspace)
        val sortedNodes = topologicalSort(workspace, workspace.graph.nodes)

        declareFunctions(sortedNodes)
        implementFunctions(workspace, sortedNodes)
        generateMainExecution(sortedNodes)

        return codeBuilder.toString()
    }

    private fun initializeVariables(workspace: Workspace) {
        codeBuilder.append("-- Initialize Variables\n")
        codeBuilder.append("local variables = {}\n")
        workspace.graph.variables.forEach { (name, property) ->
            val value = when (property) {
                is Property.String -> generateSmartString(property.get())
                is Property.Int -> property.get().toString()
                is Property.Float -> property.get().toString()
                is Property.Boolean -> property.get().toString()
                else -> "nil"
            }
            codeBuilder.append("variables['$name'] = $value\n")
        }
        codeBuilder.append("\n")
    }

    private fun generateSmartString(input: String): String {
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

    private fun generateSetup() {
        codeBuilder.append("-- Setup\n")
        codeBuilder.append("function setup()\n")
        codeBuilder.append("  math.randomseed(os.time())\n")
        codeBuilder.append("end\n\n")
    }

    private fun declareFunctions(sortedNodes: List<Node>) {
        codeBuilder.append("-- Function Declarations\n")
        sortedNodes.forEach { node ->
            val functionName = sanitizeName("${node.name}_${node.uid}")
            codeBuilder.append("local $functionName\n")
        }
        codeBuilder.append("\n")
    }

    private fun implementFunctions(workspace: Workspace, sortedNodes: List<Node>) {
        codeBuilder.append("-- Function Implementations\n")
        sortedNodes.forEach { node ->
            generateNodeFunction(workspace, node)
        }
    }

    private fun generateMainExecution(sortedNodes: List<Node>) {
        val events = sortedNodes.filter { it.type == "Events" }
        codeBuilder.append("setup()\n")
        codeBuilder.append("return {\n")

        val groupedEvents = events.groupBy { it.name }

        groupedEvents.forEach { (eventName, nodes) ->
            codeBuilder.append("  $eventName = {\n")
            nodes.forEachIndexed { index, node ->
                codeBuilder.append("    ${sanitizeName("${node.name}_${node.uid}")}")
                if (index != nodes.size - 1) {
                    codeBuilder.append(",\n")
                } else {
                    codeBuilder.append("\n")
                }
            }
            codeBuilder.append("  },\n")
        }

        codeBuilder.append("}\n")
    }

    private fun generateNodeFunction(workspace: Workspace, node: Node) {
        if (generatedFunctions.contains(node.uid)) return

        currentNode = node
        val nodeTemplate = listener<Schemas>(Endpoint.Side.SERVER).library["${node.type}/${node.name}"]
        if (nodeTemplate == null) {
            logger.error { "Node template not found for ${node.type}/${node.name}" }
            return
        }

        val sourceTemplate = nodeTemplate["source"]?.cast<Property.String>()?.get()
        if (sourceTemplate.isNullOrBlank()) {
            logger.error { "Source template is missing for node ${node.type}/${node.name}" }
            return
        }

        codeBuilder.append("-- Node: ${node.name} (${node.uid})\n")

        val functionName = sanitizeName("${node.name}_${node.uid}")
        val inputEdges = workspace.graph.getEdges(node).filter { it.direction == "input" && it.type != "exec" }
        val inputParams = inputEdges.joinToString(", ") { sanitizeName(it.name) }

        var replacedSource = sourceTemplate
            .replace("\${NODE.name}", sanitizeName(node.name))
            .replace("\${NODE.uid}", node.uid.toString())

        replacedSource = handleVariableTemplating(workspace, replacedSource)
        replacedSource = handleLambdaTemplating(workspace, replacedSource)

        inputEdges.forEach { edge ->
            replacedSource = replacedSource.replace("\${${edge.name}}", sanitizeName(edge.name))
        }

        val execOutEdges = workspace.graph.getEdges(node).filter { it.direction == "output" && it.type == "exec" }
        execOutEdges.forEach { edge ->
            val execCalls = getTargetNodes(workspace, edge).map { targetNode ->
                generateNodeCall(workspace, targetNode)
            }
            replacedSource = replacedSource.replace("\${EXEC.${edge.name}}", execCalls.joinToString("\n  "))
        }

        replacedSource = replacedSource.replaceFirst(Regex("local function .*?\\(.*?\\)\\s*"), "")
        replacedSource = replacedSource.trimEnd().removeSuffix("end")

        codeBuilder.append("$functionName = function($inputParams)\n")
        codeBuilder.append("  $replacedSource\n")

        codeBuilder.append("end\n\n")

        generatedFunctions.add(node.uid)
    }

    private fun generateNodeCall(workspace: Workspace, node: Node): String {
        val functionName = sanitizeName("${node.name}_${node.uid}")
        val inputEdges = workspace.graph.getEdges(node).filter { it.direction == "input" && it.type != "exec" }
        val inputParams = inputEdges.joinToString(", ") { edge ->
            val sourceNode = getSourceNode(workspace, edge)
            if (sourceNode != null) {
                generateNodeCall(workspace, sourceNode)
            } else {
                getDefaultValue(edge)
            }
        }
        return "$functionName($inputParams)"
    }

    private fun handleVariableTemplating(workspace: Workspace, source: String): String {
        val regex = Regex("\\$\\{VARS\\.(\\w+)}")
        return regex.replace(source) { matchResult ->
            val variableName = matchResult.groupValues[1]
            val variable = workspace.getVariable(variableName)
            if (variable != null) {
                "variables['$variableName']"
            } else {
                matchResult.value
            }
        }
    }

    private fun handleLambdaTemplating(workspace: Workspace, source: String): String {
        val regex = Regex("\\$\\{LAMBDA\\.(\\w+)}")
        return regex.replace(source) { matchResult ->
            val inputName = matchResult.groupValues[1]
            "function() return ${generateInputNodeCall(workspace, inputName)} end"
        }
    }

    private fun generateInputNodeCall(workspace: Workspace, inputName: String): String {
        val inputEdge = workspace.graph.getEdges(currentNode).find { it.name == inputName && it.direction == "input" }
        return if (inputEdge != null) {
            val sourceNode = getSourceNode(workspace, inputEdge)
            if (sourceNode != null) {
                generateNodeCall(workspace, sourceNode)
            } else {
                getDefaultValue(inputEdge)
            }
        } else {
            "nil"
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
            else -> "nil"
        }
    }

    private fun analyzeDependencies(workspace: Workspace) {
        nodeDependencies.clear()
        workspace.graph.nodes.forEach { node ->
            nodeDependencies[node.uid] = mutableSetOf()
            workspace.graph.getEdges(node).forEach { edge ->
                when (edge.direction) {
                    "input" -> {
                        val sourceNode = getSourceNode(workspace, edge)
                        if (sourceNode != null) {
                            nodeDependencies[node.uid]?.add(sourceNode.uid)
                        }
                    }

                    "output" -> {
                        if (edge.type == "exec") {
                            getTargetNodes(workspace, edge).forEach { targetNode ->
                                nodeDependencies[targetNode.uid]?.add(node.uid)
                            }
                        }
                    }
                }
            }
        }
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

    private fun getSourceNode(workspace: Workspace, edge: Edge): Node? {
        val connectedLink = workspace.graph.getLinks().find { it.to == edge.uid }
        return if (connectedLink != null) {
            val sourceEdge = workspace.graph.getEdge(connectedLink.from)
            if (sourceEdge != null) {
                workspace.graph.getNode(sourceEdge.owner)
            } else null
        } else null
    }

    private fun getTargetNodes(workspace: Workspace, edge: Edge): List<Node> {
        val connectedLinks = workspace.graph.getLinks().filter { it.from == edge.uid }
        return connectedLinks.mapNotNull { link ->
            val targetEdge = workspace.graph.getEdge(link.to)
            targetEdge?.let { workspace.graph.getNode(it.owner) }
        }
    }

    private fun sanitizeName(name: String): String {
        return name.replace(sanitizeRegex, "_")
    }
}