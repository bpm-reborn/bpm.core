package bpm.common.vm.transpiler

import bpm.Bpm
import bpm.common.network.Endpoint
import bpm.common.network.NetUtils
import bpm.common.network.Network
import bpm.common.network.listener
import bpm.common.property.Property
import bpm.common.property.cast
import bpm.common.type.NodeLibrary
import bpm.common.upstream.Schemas
import bpm.common.utils.className
import bpm.common.utils.sanitize
import bpm.common.workspace.Workspace
import bpm.common.workspace.graph.Edge
import bpm.common.workspace.graph.Function
import bpm.common.workspace.graph.Node
import java.util.UUID

class ASTGenerator(
    private val workspace: Workspace, library: NodeLibrary = listener<Schemas>(Endpoint.Side.SERVER).library
) {

    private val statementParser = ASTStatementParser(library)
    //Tracks the number of instances of a function
    private val functionInstanceCounter = mutableMapOf<UUID, Int>()
    private val functionReferenceInstances = mutableMapOf<UUID, Int>()

    /**
     * Generates a list of ASTNode.Node objects from the given graph.
     *
     * @param graph the input graph containing nodes to be transformed into ASTNode.Node objects
     * @return a list of generated ASTNode.Node objects
     */
    fun generate(): ASTNode.WorkspaceAST {
        val astNodes = mutableListOf<ASTNode.Node>()
        workspace.graph.nodes.forEach { node ->
            generateNode(astNodes, node)
        }

        // Get built-in class names
        val builtIns = Bpm.bootstrap.getBuiltIns().map { it.name to it.className }
        return ASTNode.WorkspaceAST(
            uid = workspace.uid.toString(),
            variables = generateVariables(),
            nodes = astNodes,
            builtIns = builtIns
        )
    }

    private fun generateVariables(): List<ASTNode.Variable> {
        return workspace.graph.variables.map { (name, value) ->
            ASTNode.Variable(name, value.toString())
        }
    }

    /**
     * Generates an ASTNode.Node representation for the given Node instance by creating its inputs, outputs, and statements.
     *
     * @param node The node for which an ASTNode.Node is to be generated.
     * @return An ASTNode.Node instance with fields populated using the input node's properties.
     */
    private fun generateNode(builder: MutableList<ASTNode.Node>, node: Node) {
        // First check if this is a function reference
        if (node.properties.contains("func_ref")) {
            val funcRef = node.properties["func_ref"].cast<Property.UUID>().get()
            val function = workspace.graph.getFunction(funcRef) ?: return
            generateFunctionInstance(builder, node, function)
            return  // Important: return after handling function reference
        }

        // Only proceed with normal node generation if not a function reference
        if (node.function == NetUtils.DefaultUUID) {
            val inputs = generateInputs(node)
            val outputs = generateOutputs(node)
            val statements = statementParser.parseStatements(node)

            builder.add(
                ASTNode.Node(
                    id = node.uid.toString(),
                    name = node.name,
                    type = node.type,
                    inputs = inputs,
                    outputs = outputs,
                    statements = statements
                )
            )
        }
    }


    /**
     * Generates an instance of a function representation for the given function call
     * and appends it to the list of ASTNode.Node.
     *
     * This generates the delegate function and the body nodes of the function, hooking them up to the delegate function.
     *
     * @param builder A mutable list of `ASTNode.Node` to which the generated function instance will be added.
     * @param functionCall The initiating node representing the function call for which an instance is to be generated.
     * @param targetFunction The referenced function to create an instance of.
     */
    private fun generateFunctionInstance(
        builder: MutableList<ASTNode.Node>,
        functionCall: Node,
        targetFunction: Function
    ) {
        // Get or create a new instance ID for this function reference
        val instanceId = functionReferenceInstances.getOrPut(functionCall.uid) {
            val nextId = functionInstanceCounter.getOrPut(targetFunction.uid) { 0 }
            functionInstanceCounter[targetFunction.uid] = nextId + 1
            nextId
        }

        // Create delegate node that handles input mapping
        val delegateNode = createDelegateNode(functionCall, targetFunction, instanceId)
        builder.add(delegateNode)

        // Generate instances of all body nodes
        targetFunction.nodes.forEach { nodeRef ->
            val node = workspace.graph.getNode(nodeRef.get()) ?: return@forEach
            generateFunctionBodyNodeInstances(builder, targetFunction, functionCall, node, instanceId)
        }
    }

    private fun createDelegateNode(
        functionCall: Node,
        targetFunction: Function,
        instanceId: Int
    ): ASTNode.Node {
        val delegateNodeId = "${functionCall.uid}_delegate_$instanceId"

        // Map inputs from function call to first nodes in function body
        val inputs = workspace.graph.getEdges(functionCall)
            .filter { it.direction == "input" }
            .map { edge ->
                val source = findSourceConnection(edge)?.let { (sourceNodeId, sourceEdgeName) ->
                    val sourceNode = workspace.graph.getNode(UUID.fromString(sourceNodeId))
                    Triple(sourceNodeId, sourceNode?.name ?: "", sourceEdgeName)
                }

                ASTNode.Input(
                    name = edge.name,
                    type = edge.type,
                    sourceNodeId = source?.first,
                    sourceNodeName = source?.second,
                    sourceEdgeName = source?.third,
                    defaultValue = getDefaultValue(edge)
                )
            }

        // Map outputs, will contain all calls to the internal body nodes
        val outputs = targetFunction.outputs.toHashSet().mapNotNull { workspace.graph.getEdge(it.get()) }
            .map {
                val targets = findTargetConnections(it).map { (targetNodeId, targetInputName) ->
                    val targetNode = workspace.graph.getNode(UUID.fromString(targetNodeId))
                    ASTNode.Output.Target(
                        nodeId = "${targetNodeId}_instance_$instanceId",
                        nodeName = targetNode?.name ?: "",
                        inputName = targetInputName
                    )
                }
                ASTNode.Output(name = it.name, type = it.type, targets = targets)
            }

//Locate all the connected body exec node body edges
        val bodyExecEdges = targetFunction.inputs.mapNotNull { workspace.graph.getEdge(it.get()) }
            .filter { it.type == "exec" }.map {
                val targets = findTargetConnections(it).map { (targetNodeId, targetInputName) ->
                    //The targetNodeId may be suffixed with _delegate_$instanceId, so we need to take the first part of the string to get the original node id
                    val originalNodeId = UUID.fromString(targetNodeId.substringBefore("_delegate_"))
                    val targetNode = workspace.graph.getNode(originalNodeId)
                    ASTNode.Output.Target(
                        nodeId = "${originalNodeId}_instance_$instanceId",
                        nodeName = targetNode?.name ?: "",
                        inputName = targetInputName
                    )
                }
                ASTNode.Output(name = it.name, type = it.type, targets = targets)
            }
        val allOutputs = outputs + bodyExecEdges

        val statements: MutableList<ASTNode.Statement> = bodyExecEdges.map { output ->
            ASTNode.Statement.ExecFlow(output.name)
        }.toMutableList()

        outputs.filter {
            if (bodyExecEdges.isNotEmpty()) it.type == "exec" else true
        }.forEach {
            statements.add(
                ASTNode.Statement.ExecFlow(it.name)
            )
        }

        //For all the non exec outputs, assign the value to the output
        outputs.filter { it.type != "exec" }.forEach { output ->
            val target = output.targets.firstOrNull()

            val source = "outputs['${target?.nodeName}_${target?.nodeId?.sanitize()}_${target?.inputName}']"
            statements.add(
                ASTNode.Statement.OutputAssignment(
                    outputName = output.name,
                    source
                )
            )
        }


        return ASTNode.Node(
            id = delegateNodeId,
            name = functionCall.name,
            type = "function_delegate",
            inputs = inputs,
            outputs = allOutputs,
            statements = statements
        )
    }

    /**
     * Generates instances of the nodes contained within a function body and appends them to the list of ASTNode.Node.
     *
     * The nodes within the function are linked to each other via the instance id. For edges owned by the function template,
     * it maps to the referenced call function's inputs instead.
     */
    private fun generateFunctionBodyNodeInstances(
        builder: MutableList<ASTNode.Node>,
        function: Function,
        functionCall: Node,
        node: Node,
        instanceId: Int
    ) {
        // Create instance-specific node ID
        val instanceNodeId = "${node.uid}_instance_$instanceId"

        // Map of function call inputs to their sources
        val functionCallInputSources = workspace.graph.getEdges(functionCall)
            .filter { it.direction == "input" }
            .mapNotNull { edge ->
                findSourceConnection(edge)?.let { source ->
                    edge.name to source
                }
            }
            .toMap()

        // Generate inputs with instance-specific connections
        val inputs = workspace.graph.getEdges(node)
            .filter { it.direction == "input" }
            .map { edge ->
                val source = findSourceConnection(edge)?.let { (sourceNodeId, sourceEdgeName) ->
                    // Check if source is a function or a node
                    val sourceFunction = workspace.graph.getFunction(UUID.fromString(sourceNodeId))
                    val sourceNode = workspace.graph.getNode(UUID.fromString(sourceNodeId))

                    when {
                        // If source is a function, route through function call's inputs
                        sourceFunction != null -> {
                            // Find which input on the function template this edge maps to
                            val templateInput = function.inputs
                                .mapNotNull { workspace.graph.getEdge(it.get()) }
                                .find { templateEdge ->
                                    // Find links that connect this template edge to our current edge
                                    workspace.graph.links.any { link ->
                                        link.from == templateEdge.uid && link.to == edge.uid
                                    }
                                }
                                ?.let { templateEdge ->
                                    // Use the template edge name to look up in function call inputs
                                    functionCallInputSources[templateEdge.name]
                                }

                            templateInput?.let { (actualSourceId, actualOutputName) ->
                                val actualSourceNode = workspace.graph.getNode(UUID.fromString(actualSourceId))
                                Triple(
                                    actualSourceId,
                                    actualSourceNode?.name ?: "",
                                    actualOutputName
                                )
                            }
                        }
                        // If source is a node in the same function, use instance-specific ID
                        sourceNode?.function == function.uid -> {
                            Triple(
                                "${sourceNodeId}_instance_$instanceId",
                                sourceNode.name,
                                sourceEdgeName
                            )
                        }
                        // Regular node outside the function
                        sourceNode != null -> {
                            Triple(
                                sourceNodeId,
                                sourceNode.name,
                                sourceEdgeName
                            )
                        }

                        else -> null
                    }
                }

                ASTNode.Input(
                    name = edge.name,
                    type = edge.type,
                    sourceNodeId = source?.first,
                    sourceNodeName = source?.second,
                    sourceEdgeName = source?.third,
                    defaultValue = getDefaultValue(edge)
                )
            }

        // Get all exec outputs for this node
        val nodeExecOutputs = workspace.graph.getEdges(node)
            .filter { it.direction == "output" && it.type == "exec" }

        // Check if any of our exec outputs are connected to function outputs
        val hasConnectedExecOutput = nodeExecOutputs.any { execOutput ->
            val functionOutputs = function.outputs.mapNotNull { workspace.graph.getEdge(it.get()) }
            workspace.graph.links.any { link ->
                link.to == execOutput.uid && functionOutputs.any { it.uid == link.from }
            }
        }

        // Only generate outputs if we have a connected exec output
        val outputs = if (hasConnectedExecOutput) {
            workspace.graph.getEdges(functionCall)
                .filter { it.direction == "output" }
                .map { edge ->
                    val targets = findTargetConnections(edge).map { (targetNodeId, targetInputName) ->
                        val targetNode = workspace.graph.getNode(UUID.fromString(targetNodeId))
                        val modifiedTargetId = if (targetNode?.function == function.uid) {
                            "${targetNodeId}_instance_$instanceId"
                        } else {
                            targetNodeId
                        }
                        ASTNode.Output.Target(
                            nodeId = modifiedTargetId,
                            nodeName = targetNode?.name ?: "",
                            inputName = targetInputName
                        )
                    }

                    ASTNode.Output(
                        name = edge.name,
                        type = edge.type,
                        targets = targets
                    )
                }
        } else {
            emptyList()
        }

        // Generate statements including exec flow if we have connected exec outputs
        val statements = statementParser.parseStatements(node) +
                if (hasConnectedExecOutput) {
                    outputs.filter { it.type == "exec" }
                        .flatMap { output ->
                            output.targets.map { target ->
                                ASTNode.Statement.NodeReference(target.nodeId)
                            }
                        }
                } else {
                    emptyList()
                }

        val instanceNode = ASTNode.Node(
            id = instanceNodeId,
            name = node.name,
            type = node.type,
            inputs = inputs,
            outputs = emptyList(),  // Outputs are always empty as they're handled by the delegate
            statements = statements
        )

        builder.add(instanceNode)
    }


    /**
     * Generates a list of input ports (`ASTNode.Input`) for a given node by analyzing
     * the edges in the graph that are directed as inputs.
     *
     * @param node The node for which the input ports are to be generated.
     * @return A list of `ASTNode.Input` representing the input ports of the specified node.
     */
    private fun generateInputs(node: Node): List<ASTNode.Input> =
        workspace.graph.getEdges(node).filter { it.direction == "input" }.map { edge ->
            val source = findSourceConnection(edge)
            val sourceNode = source?.let { (sourceNodeId, _) ->
                workspace.graph.getNode(UUID.fromString(sourceNodeId))
            }

            // Check if the source node is a function reference
            val isFunctionRef = sourceNode?.properties?.contains("func_ref") == true
            val delegateId = if (isFunctionRef) {
                val instanceId = functionReferenceInstances.getOrPut(sourceNode!!.uid) {
                    val funcRef = sourceNode.properties["func_ref"].cast<Property.UUID>().get()
                    val nextId = functionInstanceCounter.getOrPut(funcRef) { 0 }
                    functionInstanceCounter[funcRef] = nextId + 1
                    nextId
                }
                "${sourceNode.uid}_delegate_$instanceId"
            } else null

            ASTNode.Input(
                name = edge.name,
                type = edge.type,
                sourceNodeId = delegateId ?: source?.first,
                sourceNodeName = sourceNode?.name,
                sourceEdgeName = source?.second,
                defaultValue = getDefaultValue(edge)
            )
        }

    /**
     * Generates a list of `ASTNode.Output` objects for a given node by analyzing the edges in the graph
     * that are directed as outputs. Each output includes its name, type, and associated targets.
     *
     * @param node The node for which the output ports are to be generated.
     * @return A list of `ASTNode.Output` representing the output ports of the specified node.
     */
    private fun generateOutputs(node: Node): List<ASTNode.Output> {

        return workspace.graph.getEdges(node).filter { it.direction == "output" }.map { edge ->

            val targets = findTargetConnections(edge).map { (targetNodeId, targetInputName) ->

                // For normal nodes, look up by ID
                val targetNode = if (!targetNodeId.contains("_delegate_")) {
                    workspace.graph.getNode(UUID.fromString(targetNodeId))
                } else {
                    // For delegate nodes, extract the original node ID and look that up
                    val originalNodeId = targetNodeId.substringBefore("_delegate_")
                    workspace.graph.getNode(UUID.fromString(originalNodeId))
                }


                ASTNode.Output.Target(
                    nodeId = targetNodeId,
                    nodeName = targetNode?.name ?: "",
                    inputName = targetInputName
                )
            }
            ASTNode.Output(name = edge.name, type = edge.type, targets = targets)
        }
    }

    /**
     * Finds the source connection for a given edge in the graph, returning the ID of the source node
     * and the name of the output it is connected to.
     *
     * @param edge The edge for which the source connection needs to be identified.
     * @return A pair consisting of the source node's UID as a string and the source edge's output name,
     * or null if no source connection can be found.
     */
    private fun findSourceConnection(edge: Edge): Pair<String, String>? {
        val sourceLink = workspace.graph.links.find { it.to == edge.uid } ?: return null
        val sourceEdge = workspace.graph.getEdge(sourceLink.from) ?: return null
        val sourceUid = workspace.graph.getNode(sourceEdge.owner)?.uid
            ?: workspace.graph.getFunction(sourceEdge.owner)?.uid
            ?: return null
        return sourceUid.toString() to sourceEdge.name
    }

    /**
     * Finds and retrieves the target connections for a given edge in the graph.
     * Each target connection is represented as a pair containing the target node's unique identifier
     * and the name of the edge linked to the target.
     *
     * @param edge The edge for which target connections are to be identified.
     * @return A list of pairs where each pair contains the target node's UID as a string and the target edge's name.
     *         Returns an empty list if no target connections are found.
     */
    private fun findTargetConnections(edge: Edge): List<Pair<String, String>> {
        return workspace.graph.links.filter { it.from == edge.uid }.mapNotNull { link ->
            val targetEdge = workspace.graph.getEdge(link.to) ?: return@mapNotNull null
            val targetNode = workspace.graph.getNode(targetEdge.owner) ?: return@mapNotNull null

            if (targetNode.properties.contains("func_ref")) {
                val instanceId = functionReferenceInstances.getOrPut(targetNode.uid) {
                    val funcRef = targetNode.properties["func_ref"].cast<Property.UUID>().get()
                    val nextId = functionInstanceCounter.getOrPut(funcRef) { 0 }
                    functionInstanceCounter[funcRef] = nextId + 1
                    nextId
                }
                "${targetNode.uid}_delegate_$instanceId" to targetEdge.name
            } else {
                targetNode.uid.toString() to targetEdge.name
            }
        }
    }


    /**
     * Retrieves the default value of the given edge based on its type property.
     *
     * @param edge The edge from which the default value is to be retrieved.
     * @return The default value as a string, or null if no default value is found or the type is unsupported.
     */
    private fun getDefaultValue(edge: Edge): String {
        val value = edge.value
        if (value.isEmpty) return "nil"
        if (!value.contains("type")) return "nil"
        val type = value["type"].cast<Property.String>().get()
        var defaultValue = value["default"]
        if (defaultValue == Property.Null) defaultValue = Property.String("")
        return when (type) {
            "float" -> (defaultValue as? Property.Float)?.get()?.toString() ?: "0.0"
            "int" -> (defaultValue as? Property.Int)?.get()?.toString() ?: "0"
            "boolean" -> (defaultValue as? Property.Boolean)?.get()?.toString() ?: "false"
            "string" -> "\"${(defaultValue as? Property.String)?.get() ?: ""}\""
            "color" -> "\"${(defaultValue as? Property.String)?.get() ?: "#00000000"}\""
            else -> "nil"
        }
    }

}