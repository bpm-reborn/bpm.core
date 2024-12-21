package bpm.common.vm.transpiler

import bpm.common.network.NetUtils
import bpm.common.network.Network
import bpm.common.property.Property
import bpm.common.property.cast
import bpm.common.type.NodeLibrary
import bpm.common.workspace.Workspace
import bpm.common.workspace.graph.Edge
import bpm.common.workspace.graph.Function
import bpm.common.workspace.graph.Node
import java.util.UUID

class ASTGenerator(
    private val workspace: Workspace, library: NodeLibrary = workspace.nodeLibrary
) {

    private val statementParser = ASTStatementParser(library)
    //Tracks the number of instances of a function
    private val functionInstanceCounter = mutableMapOf<UUID, Int>()

    /**
     * Generates a list of ASTNode.Node objects from the given graph.
     *
     * @param graph the input graph containing nodes to be transformed into ASTNode.Node objects
     * @return a list of generated ASTNode.Node objects
     */
    fun generate(): List<ASTNode.Node> {
        val astNodes = mutableListOf<ASTNode.Node>()
        workspace.graph.nodes.forEach { node ->
            generateNode(astNodes, node)
        }
        return astNodes
    }

    /**
     * Generates an ASTNode.Node representation for the given Node instance by creating its inputs, outputs, and statements.
     *
     * @param node The node for which an ASTNode.Node is to be generated.
     * @return An ASTNode.Node instance with fields populated using the input node's properties.
     */
    private fun generateNode(builder: MutableList<ASTNode.Node>, node: Node) {
        val inputs = generateInputs(node)
        val outputs = generateOutputs(node)
        val statements = statementParser.parseStatements(node)

        //If this node is part of a function body, or is a function reference, we should not add it to the AST as it will be handled by the function instance
        if (node.function == NetUtils.DefaultUUID && !node.properties.contains("func_ref"))
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

        //If it's a function ref, we should use the function "template" and instantiate it
        if (node.properties.contains("func_ref")) {
            val funcRef = node.properties["func_ref"].cast<Property.UUID>().get()
            val function = workspace.graph.getFunction(funcRef) ?: return
            generateFunctionInstance(builder, node, function)
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
        //The initiator node
        functionCall: Node,
        //The referenced function to create an instance of
        targetFunction: Function
    ) {
        // This may need to get rewritten to properly handle the function instance, the "inputs" are actually the outputs of the function template.
        //They will be mapped to the outputs of the function instance
        val inputs = targetFunction.outputs.mapNotNull { workspace.graph.getEdge(it.get()) }.toHashSet().associateWith {
            findSourceConnection(it)?.let { (sourceNodeId, sourceEdgeName) ->
                ASTNode.Input(
                    name = it.name,
                    type = it.type,
                    sourceNodeId = sourceNodeId,
                    sourceEdgeName = sourceEdgeName,
                    defaultValue = getDefaultValue(it)
                )
            }
        }

        //Map the outputs to their targets, this maps the function templates "outputs" to the body nodes inputs
        val outputs = targetFunction.inputs.mapNotNull { workspace.graph.getEdge(it.get()) }.toHashSet().associateWith {
            findTargetConnections(it).map { (targetNodeId, targetInputName) ->
                ASTNode.Output.Target(targetNodeId, targetInputName)
            }
        }

        // The actual inputs that are for the function reference, this will be the inputs for the delegate function
        val delegateInputs = workspace.graph.getEdges(functionCall).filter { it.direction == "input" }

        // The actual outputs that are for the function reference, this will be the outputs for the delegate function
        val delegateOutputs = workspace.graph.getEdges(functionCall).filter { it.direction == "output" }

        //The instance ID of the function
        val instanceId = functionInstanceCounter.getOrPut(targetFunction.uid) { 0 }

        //Create all the body node instances for this function, they should link to the delegate function and each other,
        //making use of the proper instance
        for (i in 0 until targetFunction.nodes.size) {
            val node = workspace.graph.getNode(targetFunction.nodes[i].get()) ?: continue
            generateFunctionBodyNodeInstances(builder, targetFunction, node, instanceId)
        }

        //Increment the instance counter for this function
        functionInstanceCounter[targetFunction.uid] = instanceId + 1
    }


    /**
     * Generates instances of the nodes contained within a function body and appends them to the list of ASTNode.Node.
     *
     * The nodes within the function are linked to each other via the instance id
     */
    private fun generateFunctionBodyNodeInstances(
        builder: MutableList<ASTNode.Node>,
        function: Function,
        node: Node,
        instanceId: Int
    ) {

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
            // Find the edge that is connected to the input port. Inputs can only have one source.
            val source = findSourceConnection(edge)
            ASTNode.Input(
                name = edge.name,
                type = edge.type,
                sourceNodeId = source?.first,
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
    private fun generateOutputs(node: Node): List<ASTNode.Output> =
        workspace.graph.getEdges(node).filter { it.direction == "output" }.map { edge ->
            // Find the edge that is connected to the output port. Outputs can have multiple targets.
            val targets = findTargetConnections(edge)
            ASTNode.Output(name = edge.name,
                type = edge.type,
                targets = targets.map { (targetNodeId, targetInputName) ->
                    ASTNode.Output.Target(targetNodeId, targetInputName)
                })
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
        val sourceNode = workspace.graph.getNode(sourceEdge.owner) ?: return null
        return sourceNode.uid.toString() to sourceEdge.name
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
    private fun findTargetConnections(edge: Edge): List<Pair<String, String>> =
        workspace.graph.links.filter { it.from == edge.uid }.mapNotNull { link ->
            val targetEdge = workspace.graph.getEdge(link.to) ?: return@mapNotNull null
            val targetNode = workspace.graph.getNode(targetEdge.owner) ?: return@mapNotNull null
            targetNode.uid.toString() to targetEdge.name
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