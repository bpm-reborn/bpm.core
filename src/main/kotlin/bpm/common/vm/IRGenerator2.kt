package bpm.common.vm

import bpm.common.logging.KotlinLogging
import bpm.common.network.Endpoint
import bpm.common.network.NetUtils
import bpm.common.network.listener
import bpm.common.property.Property
import bpm.common.property.cast
import bpm.common.upstream.Schemas
import bpm.common.vm.compiliation.*
import bpm.common.workspace.Workspace
import bpm.common.workspace.graph.Edge
import bpm.common.workspace.graph.Function
import bpm.common.workspace.graph.Node
import java.util.*

/**
 * Enhanced IRGenerator with improved function instance handling.
 * Manages the generation of intermediate representation for both standalone nodes
 * and nodes within function instances.
 */
class IRGenerator2 {

    // Track function instances and their execution orders
    private val functionInstances = mutableMapOf<UUID, FunctionContext>()
    private val logger = KotlinLogging.logger{}
    /**
     * Represents the context of a function instance, including its nodes and execution order
     */
    private data class FunctionContext(
        val function: Function,
        val nodes: List<Node>,
        val executionOrder: List<Node>,
        val inputs: Map<String, Edge>,
        val outputs: Map<String, Edge>
    )

    fun generate(ast: AST, workspace: Workspace): IR {
        val ir = IR()
        initializeVariables(workspace, ir)

        // Generate IR for all nodes in dependency order
        val sortedNodes = computeExecutionOrder(workspace.graph.nodes.toList(), workspace)
        sortedNodes.forEach { node ->
            processNode(node, workspace, ir)
        }

        ast.edges.forEach { edge ->
            ir.edges.add(IREdge(edge.id))
        }

        logger.debug(IRDebugPrinter.formatIR(ir, workspace))
        return ir
    }

    private fun initializeVariables(workspace: Workspace, ir: IR) {
        workspace.graph.variables.forEach { (name, property) ->
            ir.variables[name] = when (property) {
                is Property.String -> IRValue.String(multiLineString(property.get()))
                is Property.Int -> IRValue.Int(property.get())
                is Property.Float -> IRValue.Float(property.get())
                is Property.Boolean -> IRValue.Boolean(property.get())
                else -> IRValue.Null
            }
        }
    }

    private fun initializeFunctionContexts(workspace: Workspace) {
        workspace.graph.functions.forEach { function ->
            val nodes = function.nodes.mapNotNull { workspace.getNode(it.get()) }
            val executionOrder = computeExecutionOrder(nodes, workspace)
            val inputs = collectFunctionEdges(function, "input", workspace)
            val outputs = collectFunctionEdges(function, "output", workspace)

            functionInstances[function.uid] = FunctionContext(
                function = function,
                nodes = nodes,
                executionOrder = executionOrder,
                inputs = inputs,
                outputs = outputs
            )
        }
    }

    private fun collectFunctionEdges(
        function: Function,
        direction: String,
        workspace: Workspace
    ): Map<String, Edge> {
        return workspace.graph.getEdges(function.uid)
            .filter { it.direction == direction }
            .associate { it.name to it }
    }

    private fun computeExecutionOrder(nodes: List<Node>, workspace: Workspace): List<Node> {
        val visited = mutableSetOf<UUID>()
        val order = mutableListOf<Node>()

        // Helper function for topological sort
        fun visit(node: Node) {
            if (node.uid in visited) return
            visited.add(node.uid)

            // Process input dependencies first
            workspace.graph.getEdges(node)
                .filter { it.direction == "input" }
                .forEach { edge ->
                    val sourceLink = workspace.graph.links.find { it.to == edge.uid }
                    if (sourceLink != null) {
                        val sourceEdge = workspace.graph.getEdge(sourceLink.from)
                        if (sourceEdge != null) {
                            val sourceNode = workspace.graph.getNode(sourceEdge.owner)
                            if (sourceNode != null && sourceNode.uid !in visited) {
                                visit(sourceNode)
                            }
                        }
                    }
                }

            order.add(node)
        }

        // Start with nodes that have exec inputs
        nodes.filter { node -> hasExecInput(node, workspace) }.forEach { visit(it) }

        // Process remaining nodes
        nodes.forEach { visit(it) }

        return order
    }

    private fun processNode(
        node: Node,
        workspace: Workspace,
        ir: IR
    ): IRFunction {
        // Handle function reference nodes specially
        if (node.type == "Functions") {
            return processFunctionReferenceNode(node, workspace, ir)
        }

        val inputEdges = workspace.graph.getEdges(node)
            .filter { it.direction == "input" }

        val irFunction = IRFunction(
            id = node.uid.toString(),
            originalName = node.name,
            nodeType = node.type,
            inputEdges = inputEdges.map { it.name },
            inputConnections = buildInputConnections(workspace, inputEdges),
            outputEdges = buildOutputConnections(workspace, node)
        )

        // Add node implementation
        generateNodeImplementation(node, workspace, irFunction)

        ir.functions.add(irFunction)
        return irFunction
    }

    private fun processFunctionReferenceNode(
        node: Node,
        workspace: Workspace,
        ir: IR
    ): IRFunction {
        val functionRef = node.properties["func_ref"]?.cast<Property.UUID>()?.get()
            ?: return createEmptyFunction(node.uid.toString())

        val function = workspace.graph.getFunction(functionRef)
            ?: return createEmptyFunction(node.uid.toString())

        // Process inputs/outputs for the function reference node
        val inputEdges = workspace.graph.getEdges(node)
            .filter { it.direction == "input" }

        val irFunction = IRFunction(
            id = node.uid.toString(),
            originalName = node.name,
            nodeType = "Functions",
            inputEdges = inputEdges.map { it.name },
            inputConnections = buildInputConnections(workspace, inputEdges),
            outputEdges = buildOutputConnections(workspace, node)
        )

        // Process function instance nodes
        function.nodes.mapNotNull { workspace.getNode(it.get()) }
            .forEach { instanceNode ->
                generateFunctionNodeImplementation(instanceNode, node, workspace, irFunction)
            }

        ir.functions.add(irFunction)
        return irFunction
    }


    private fun generateFunctionNodeImplementation(
        instanceNode: Node,
        functionNode: Node,
        workspace: Workspace,
        irFunction: IRFunction
    ) {
        // Forward function inputs to instance nodes
        workspace.graph.getEdges(instanceNode)
            .filter { it.direction == "input" }
            .forEach { edge ->
                val functionInputLink = workspace.graph.links.find { it.to == edge.uid }
                if (functionInputLink != null) {
                    val sourceEdge = workspace.graph.getEdge(functionInputLink.from)
                    if (sourceEdge?.owner == functionNode.uid) {
                        irFunction.body.add(
                            IRStatement.GenericExpression(
                            "${LuaTranspiler.sanitizeName(edge.name)} = ${LuaTranspiler.sanitizeName(sourceEdge.name)}"
                        ))
                    }
                }
            }

        // Generate node implementation
        generateNodeImplementation(instanceNode, workspace, irFunction)
    }


    private fun buildExecutionDependencies(
        node: Node,
        workspace: Workspace,
        ir: IR
    ) {
        // Track execution flow
        workspace.graph.getEdges(node)
            .filter { it.direction == "output" && it.type == "exec" }
            .forEach { execEdge ->
                workspace.graph.links
                    .filter { it.from == execEdge.uid }
                    .forEach { link ->
                        val targetEdge = workspace.graph.getEdge(link.to)
                        if (targetEdge != null) {
                            val targetNode = workspace.graph.getNode(targetEdge.owner)
                            if (targetNode != null) {
                                // If target is a function node, we need to forward execution to its implementation
                                if (targetNode.type == "Functions") {
                                    val functionRef = targetNode.properties["func_ref"]?.cast<Property.UUID>()?.get()
                                    val function = functionRef?.let { workspace.graph.getFunction(it) }
                                    if (function != null) {
                                        // Call the function's implementation nodes
                                        function.nodes.mapNotNull { workspace.getNode(it.get()) }
                                            .forEach { implementationNode ->
                                                ir.functions.find { it.id == node.uid.toString() }?.let { sourceFunction ->
                                                    sourceFunction.body.add(
                                                        IRStatement.NodeReference(implementationNode.uid.toString())
                                                    )
                                                }
                                            }
                                    }
                                }
                            }
                        }
                    }
            }
    }

    private fun processFunctionImplementation(
        function: Function,
        node: Node,
        workspace: Workspace,
        irFunction: IRFunction
    ) {
        // Add function context information
        irFunction.setupBlocks.add("-- Function Implementation: ${function.name}")

        // Process the function's nodes in order
        val nodes = function.nodes.mapNotNull { workspace.getNode(it.get()) }
        val executionOrder = computeExecutionOrder(nodes, workspace)

        executionOrder.forEach { functionNode ->
            val nodeTemplate = listener<Schemas>(Endpoint.Side.SERVER).library["${functionNode.type}/${functionNode.name}"]
            if (nodeTemplate != null) {
                // Process each node's implementation within the function context
                generateNodeImplementation(functionNode, workspace, irFunction)
            }
        }
    }

    private fun generateScopedNodeImplementation(
        node: Node,
        workspace: Workspace,
        irFunction: IRFunction,
        scopedVars: Map<String, String>
    ) {
        // Get the node template from the schema library
        val nodeTemplate = listener<Schemas>(Endpoint.Side.SERVER).library["${node.type}/${node.name}"]

        if (nodeTemplate != null) {
            // Determine if we should use override or template source
            val sourceTemplate = if (node.properties.contains("override")) {
                node["override"].cast<Property.String>().get()
            } else {
                nodeTemplate["source"].cast<Property.String>().get()
            }

            // Process the template with proper variable scoping
            val processedTemplate = processTemplateWithScope(sourceTemplate, node, workspace, scopedVars)

            // Add processed template to IR function body
            irFunction.body.add(IRStatement.Literal(processedTemplate))
        }
    }

    private fun processTemplateWithScope(
        template: String,
        node: Node,
        workspace: Workspace,
        scopedVars: Map<String, String>
    ): String {
        var processed = template

        // Replace scoped variable references
        scopedVars.forEach { (originalName, scopedName) ->
            processed = processed.replace("\${NODE.$originalName}", scopedName)
        }

        // Process function instance node references
        if (node.function != NetUtils.DefaultUUID) {
            val functionContext = functionInstances[node.function]
            functionContext?.nodes?.forEach { functionNode ->
                // Create properly scoped reference
                val scopedNodeName = "local ${LuaTranspiler.sanitizeName("${functionNode.name}_${functionNode.uid}")}"
                processed = processed.replace("\${NODE.${functionNode.name}}", scopedNodeName)
            }
        }

        // Process workspace variable references
        workspace.graph.variables.forEach { (name, _) ->
            processed = processed.replace("\${VARS.$name}", "variables['$name']")
        }

        return processed
    }

    private fun generateScopedOutputHandling(
        node: Node,
        workspace: Workspace,
        irFunction: IRFunction,
        scopedVars: Map<String, String>
    ) {
        workspace.graph.getEdges(node)
            .filter { it.direction == "output" }
            .forEach { edge ->
                when (edge.type) {
                    "exec" -> {
                        // Handle execution flow outputs
                        workspace.graph.links
                            .filter { it.from == edge.uid }
                            .forEach { link ->
                                val targetEdge = workspace.graph.getEdge(link.to)
                                val targetNode = targetEdge?.let { workspace.graph.getNode(it.owner) }
                                if (targetNode != null) {
                                    // Add execution reference with proper scoping
                                    irFunction.body.add(
                                        IRStatement.ExecReference(targetNode.uid.toString())
                                    )
                                }
                            }
                    }
                    else -> {
                        // Handle data flow outputs
                        val scopedName = scopedVars[edge.name] ?: LuaTranspiler.sanitizeName(edge.name)
                        irFunction.body.add(
                            IRStatement.OutputAssignment(
                                edge.name,
                                "local $scopedName"
                            )
                        )
                    }
                }
            }
    }

    private fun processStandaloneNode(
        node: ASTNode.NodeDeclaration,
        actualNode: Node,
        workspace: Workspace,
        irFunction: IRFunction
    ) {
        // Process AST children for standalone nodes
        node.children.forEach { child ->
            when (child) {
                is ASTNode.Literal -> {
                    irFunction.body.add(IRStatement.Literal(child.value))
                }
                is ASTNode.NodeReference -> {
                    // Only add node reference if it's not an input
                    if (child.name !in irFunction.inputEdges) {
                        irFunction.body.add(IRStatement.NodeReference(child.name))
                    }
                }
                is ASTNode.ExecReference -> {
                    irFunction.body.add(IRStatement.ExecReference(child.name))
                }
                is ASTNode.VarReference -> {
                    irFunction.body.add(IRStatement.VarReference(child.name))
                }
                is ASTNode.SetupBlock -> {
                    irFunction.setupBlocks.add(child.content.removePrefix("{").removeSuffix("}"))
                }
                is ASTNode.OutputAssignment -> {
                    irFunction.body.add(
                        IRStatement.OutputAssignment(child.name, child.value)
                    )
                }
                is ASTNode.GenericExpression -> {
                    irFunction.body.add(IRStatement.GenericExpression(child.content))
                }
                else -> {} // Handle other cases as needed
            }
        }

        // Add any node-specific setup requirements
        addNodeSetup(actualNode, workspace, irFunction)
    }

    private fun collectInputEdges(node: Node, workspace: Workspace): List<Edge> {
        return workspace.graph.getEdges(node)
            .filter { edge ->
                // Include non-exec inputs and exec inputs that have connections
                edge.direction == "input" && (
                        edge.type != "exec" ||
                                workspace.graph.links.any { it.to == edge.uid }
                        )
            }
    }

    private fun addNodeSetup(node: Node, workspace: Workspace, irFunction: IRFunction) {
        // Get the node template, handling the case where it might not exist
        val nodeTemplate = listener<Schemas>(Endpoint.Side.SERVER).library["${node.type}/${node.name}"]
        if (nodeTemplate == null) {
            logger.warn { "Node template not found for ${node.type}/${node.name}. Skipping setup." }
            return
        }

        // Safely handle imports - check if the property exists and is the right type
        nodeTemplate.properties["imports"]?.let { importsProperty ->
            if (importsProperty is Property.List) {
                importsProperty.get().forEach { import ->
                    if (import is Property.String) {
                        irFunction.setupBlocks.add("local ${import.get()}")
                    }
                }
            }
        }

        // Safely handle setup code - check if the property exists and is the right type
        nodeTemplate.properties["setup"]?.let { setupProperty ->
            if (setupProperty is Property.String) {
                irFunction.setupBlocks.add(setupProperty.get())
            }
        }
    }



    private fun processNodeInFunctionContext(
        node: Node,
        functionContext: FunctionContext,
        workspace: Workspace,
        irFunction: IRFunction
    ) {
        // Add function instance context information
        irFunction.setupBlocks.add("-- Function Instance: ${functionContext.function.name}")

        // Process function instance specific input/output mappings
        val scopedVars = processFunctionScopeVariables(node, functionContext, workspace)

        // Generate input handling with proper scope
        generateScopedInputSetup(node, workspace, irFunction, scopedVars)

        // Generate core logic with scope awareness
        generateScopedNodeImplementation(node, workspace, irFunction, scopedVars)

        // Generate output handling with scope awareness
        generateScopedOutputHandling(node, workspace, irFunction, scopedVars)
    }

    private fun generateScopedInputSetup(
        node: Node,
        workspace: Workspace,
        irFunction: IRFunction,
        scopedVars: Map<String, String>
    ) {
        workspace.graph.getEdges(node)
            .filter { it.direction == "input" }
            .forEach { edge ->
                val varName = scopedVars[edge.name] ?: LuaTranspiler.sanitizeName(edge.name)
                val sourceConnection = workspace.graph.links.find { it.to == edge.uid }

                if (sourceConnection != null) {
                    val sourceEdge = workspace.graph.getEdge(sourceConnection.from)
                    val sourceNode = sourceEdge?.let { workspace.graph.getNode(it.owner) }
                    if (sourceNode != null) {
                        val statement = "local $varName = globalOutputs['${
                            LuaTranspiler.sanitizeName("${sourceNode.name}_${sourceNode.uid}")
                        }_${LuaTranspiler.sanitizeName(sourceEdge.name)}']"
                        irFunction.body.add(IRStatement.GenericExpression(statement))
                    }
                } else {
                    // Handle default values with proper scoping
                    val statement = "local $varName = ${getDefaultValue(edge)}"
                    irFunction.body.add(IRStatement.GenericExpression(statement))
                }
            }
    }

    private fun addDependencies(
        irFunction: IRFunction,
        workspace: Workspace,
        ir: IR
    ) {
        // Get the actual node
        val node = workspace.graph.getNode(UUID.fromString(irFunction.id)) ?: return

        // Collect all dependencies
        val dependencies = mutableSetOf<UUID>()

        // Add data flow dependencies
        workspace.graph.getEdges(node)
            .filter { it.direction == "input" }
            .forEach { edge ->
                val sourceLink = workspace.graph.links.find { it.to == edge.uid }
                if (sourceLink != null) {
                    val sourceEdge = workspace.graph.getEdge(sourceLink.from)
                    if (sourceEdge != null) {
                        dependencies.add(sourceEdge.owner)
                    }
                }
            }

        // Add execution flow dependencies
        workspace.graph.getEdges(node)
            .filter { it.direction == "input" && it.type == "exec" }
            .forEach { edge ->
                val sourceLink = workspace.graph.links.find { it.to == edge.uid }
                if (sourceLink != null) {
                    val sourceEdge = workspace.graph.getEdge(sourceLink.from)
                    if (sourceEdge != null) {
                        dependencies.add(sourceEdge.owner)
                    }
                }
            }

        // Add dependencies to IR function
        dependencies.forEach { depId ->
            ir.functions.find { it.id == depId.toString() }?.let {
                irFunction.dependencies.add(it)
            }
        }
    }


    private fun generateFunctionInstanceNode(
        node: Node,
        workspace: Workspace
    ): IRFunction {
        val functionContext = functionInstances[node.function]
            ?: return createEmptyFunction(node.uid.toString())

        val inputEdges = workspace.graph.getEdges(node)
            .filter { it.direction == "input" && it.type != "exec" }

        return IRFunction(
            id = node.uid.toString(),
            originalName = node.name,
            nodeType = node.type,
            inputEdges = inputEdges.map { it.name },
            inputConnections = buildInputConnections(workspace, inputEdges),
            outputEdges = buildOutputConnections(workspace, node)
        ).apply {
            // Add function instance context
            setupBlocks.add("-- Function Instance: ${functionContext.function.name}")

            // Generate input handling
            generateInputSetup(node, workspace, this)

            // Generate core node logic
            generateNodeImplementation(node, workspace, this)

            // Generate output handling
            generateOutputHandling(node, workspace, this)
        }
    }

    private fun generateInputSetup(node: Node, workspace: Workspace, function: IRFunction) {
        workspace.graph.getEdges(node)
            .filter { it.direction == "input" }
            .forEach { edge ->
                val sourceConnection = workspace.graph.links.find { it.to == edge.uid }
                if (sourceConnection != null) {
                    val sourceEdge = workspace.graph.getEdge(sourceConnection.from)
                    val sourceNode = sourceEdge?.let { workspace.graph.getNode(it.owner) }
                    if (sourceNode != null) {
                        val statement = "local ${LuaTranspiler.sanitizeName(edge.name)} = " +
                                "globalOutputs['${LuaTranspiler.sanitizeName("${sourceNode.name}_${sourceNode.uid}")}_" +
                                "${LuaTranspiler.sanitizeName(sourceEdge.name)}']"
                        function.body.add(IRStatement.GenericExpression(statement))
                    }
                } else {
                    // Use default value if no connection
                    val statement = "local ${LuaTranspiler.sanitizeName(edge.name)} = ${getDefaultValue(edge)}"
                    function.body.add(IRStatement.GenericExpression(statement))
                }
            }
    }

    private fun generateNodeImplementation(
        node: Node,
        workspace: Workspace,
        irFunction: IRFunction
    ) {
        // Generate input handling
        generateInputs(node, workspace, irFunction)

        // Generate node's core logic
        val nodeTemplate = listener<Schemas>(Endpoint.Side.SERVER).library["${node.type}/${node.name}"]
        if (nodeTemplate != null) {
            val sourceTemplate = if (node.properties.contains("override")) {
                node["override"].cast<Property.String>().get()
            } else {
                nodeTemplate["source"].cast<Property.String>().get()
            }
            irFunction.body.add(IRStatement.Literal(sourceTemplate))
        }

        // Generate output handling
        generateOutputs(node, workspace, irFunction)
    }

    private fun generateInputs(node: Node, workspace: Workspace, irFunction: IRFunction) {
        // First handle all non-exec inputs since they provide data needed for execution
        workspace.graph.getEdges(node)
            .filter { it.direction == "input" && it.type != "exec" }
            .forEach { edge ->
                // Check if this input has a connection from another node
                val sourceLink = workspace.graph.links.find { it.to == edge.uid }
                if (sourceLink != null) {
                    // There's a connection - get the data from the source node's output
                    val sourceEdge = workspace.graph.getEdge(sourceLink.from)
                    val sourceNode = sourceEdge?.let { workspace.graph.getNode(it.owner) }
                    if (sourceNode != null) {
                        // Create a reference to the source node's output in the global outputs table
                        irFunction.body.add(
                            IRStatement.GenericExpression(
                            "local ${LuaTranspiler.sanitizeName(edge.name)} = globalOutputs['" +
                                    "${LuaTranspiler.sanitizeName("${sourceNode.name}_${sourceNode.uid}")}_" +
                                    "${LuaTranspiler.sanitizeName(sourceEdge.name)}']"
                        ))
                    }
                } else {
                    // No connection - use the default value for this input
                    irFunction.body.add(
                        IRStatement.GenericExpression(
                        "local ${LuaTranspiler.sanitizeName(edge.name)} = ${getDefaultValue(edge)}"
                    ))
                }
            }

        // Then handle exec inputs since they determine execution flow
        workspace.graph.getEdges(node)
            .filter { it.direction == "input" && it.type == "exec" }
            .forEach { edge ->
                val sourceLink = workspace.graph.links.find { it.to == edge.uid }
                if (sourceLink != null) {
                    val sourceEdge = workspace.graph.getEdge(sourceLink.from)
                    if (sourceEdge != null) {
                        // For exec inputs, we just need to acknowledge them but don't need to store their value
                        irFunction.body.add(
                            IRStatement.GenericExpression(
                            "local ${LuaTranspiler.sanitizeName(edge.name)} = nil"
                        ))
                    }
                }
            }
    }

    private fun generateOutputs(node: Node, workspace: Workspace, irFunction: IRFunction) {
        workspace.graph.getEdges(node)
            .filter { it.direction == "output" }
            .forEach { edge ->
                when (edge.type) {
                    "exec" -> {
                        // For exec outputs, we need to call the next nodes in the execution chain
                        workspace.graph.links
                            .filter { it.from == edge.uid }
                            .forEach { link ->
                                val targetEdge = workspace.graph.getEdge(link.to)
                                val targetNode = targetEdge?.let { workspace.graph.getNode(it.owner) }
                                if (targetNode != null) {
                                    // Call the next node in the execution chain
                                    if (targetNode.type == "Functions") {
                                        // For function nodes, we need to handle the function instance
                                        val functionRef = targetNode.properties["func_ref"]?.cast<Property.UUID>()?.get()
                                        val function = functionRef?.let { workspace.graph.getFunction(it) }
                                        if (function != null) {
                                            // Call all nodes in the function instance
                                            function.nodes.mapNotNull { workspace.getNode(it.get()) }
                                                .forEach { instanceNode ->
                                                    irFunction.body.add(
                                                        IRStatement.NodeReference(
                                                        instanceNode.uid.toString()
                                                    ))
                                                }
                                        }
                                    } else {
                                        // For regular nodes, just call them directly
                                        irFunction.body.add(
                                            IRStatement.NodeReference(
                                            targetNode.uid.toString()
                                        ))
                                    }
                                }
                            }
                    }
                    else -> {
                        // For data outputs, we need to store the value in the global outputs table
                        irFunction.body.add(
                            IRStatement.OutputAssignment(
                            "${LuaTranspiler.sanitizeName("${node.name}_${node.uid}")}_${LuaTranspiler.sanitizeName(edge.name)}",
                            "local ${LuaTranspiler.sanitizeName(edge.name)}"
                        ))
                    }
                }
            }
    }

    private fun generateOutputHandling(node: Node, workspace: Workspace, function: IRFunction) {
        workspace.graph.getEdges(node)
            .filter { it.direction == "output" }
            .forEach { edge ->
                when (edge.type) {
                    "exec" -> {
                        workspace.graph.links
                            .filter { it.from == edge.uid }
                            .forEach { link ->
                                val targetEdge = workspace.graph.getEdge(link.to)
                                val targetNode = targetEdge?.let { workspace.graph.getNode(it.owner) }
                                if (targetNode != null) {
                                    function.body.add(
                                        IRStatement.ExecReference(targetNode.uid.toString())
                                    )
                                }
                            }
                    }

                    else -> {
                        function.body.add(
                            IRStatement.OutputAssignment(
                                edge.name,
                                "local ${LuaTranspiler.sanitizeName(edge.name)}"
                            )
                        )
                    }
                }
            }
    }

    private fun processTemplateInContext(template: String, node: Node, workspace: Workspace): String {
        var processed = template

        // Replace node references
        val functionContext = functionInstances[node.function]
        functionContext?.nodes?.forEach { functionNode ->
            processed = processed.replace(
                "\${NODE.${functionNode.name}}",
                "local ${LuaTranspiler.sanitizeName("${functionNode.name}_${functionNode.uid}")}"
            )
        }

        // Process variable references
        workspace.graph.variables.forEach { (name, _) ->
            processed = processed.replace("\${VARS.$name}", "variables['$name']")
        }

        return processed
    }

    private fun generateStandardNode(
        node: ASTNode.NodeDeclaration,
        actualNode: Node,
        workspace: Workspace
    ): IRFunction {
        val inputEdges = workspace.graph.getEdges(actualNode)
            .filter { it.direction == "input" && it.type != "exec" }

        return IRFunction(
            id = actualNode.uid.toString(),
            originalName = actualNode.name,
            nodeType = actualNode.type,
            inputEdges = inputEdges.map { it.name },
            inputConnections = buildInputConnections(workspace, inputEdges),
            outputEdges = buildOutputConnections(workspace, actualNode)
        ).apply {
            node.children.forEach { child -> processChild(child, this, inputEdges.map { it.name }) }
        }
    }

    private fun processChild(
        child: ASTNode,
        function: IRFunction,
        inputEdges: List<String>
    ) {
        when (child) {
            is ASTNode.Literal ->
                function.body.add(IRStatement.Literal(child.value))

            is ASTNode.NodeReference -> {
                if (child.name !in inputEdges) {
                    function.body.add(IRStatement.NodeReference(child.name))
                }
            }

            is ASTNode.ExecReference ->
                function.body.add(IRStatement.ExecReference(child.name))

            is ASTNode.VarReference ->
                function.body.add(IRStatement.VarReference(child.name))

            is ASTNode.LambdaReference ->
                function.body.add(IRStatement.LambdaReference(child.name))

            is ASTNode.JavaImport ->
                function.body.add(IRStatement.JavaImport(child.name))

            is ASTNode.SetupBlock ->
                function.setupBlocks.add(child.content.removePrefix("{").removeSuffix("}"))

            is ASTNode.GenericExpression ->
                function.body.add(IRStatement.GenericExpression(child.content))

            is ASTNode.OutputAssignment ->
                function.body.add(IRStatement.OutputAssignment(child.name, child.value))

            else -> {} // Ignore other node types
        }
    }

    // Helper methods remain mostly unchanged but are moved inside the class...
    private fun hasExecInput(node: Node, workspace: Workspace): Boolean {
        return workspace.graph.getEdges(node)
            .any { it.direction == "input" && it.type == "exec" }
    }

    private fun createEmptyFunction(id: String): IRFunction {
        return IRFunction(
            id = id,
            originalName = "UnknownNode",
            nodeType = "Unknown",
            inputEdges = emptyList(),
            inputConnections = emptyMap(),
            outputEdges = emptyMap()
        )
    }

    private fun buildInputConnections(
        workspace: Workspace,
        inputEdges: List<Edge>
    ): Map<String, Pair<String, String>> {
        return inputEdges.mapNotNull { edge ->
            val sourceLink = workspace.graph.links.find { it.to == edge.uid }
            if (sourceLink != null) {
                val sourceEdge = workspace.graph.getEdge(sourceLink.from)
                if (sourceEdge != null) {
                    val sourceNode = workspace.graph.getNode(sourceEdge.owner)
                    if (sourceNode != null) {
                        edge.name to Pair(sourceNode.uid.toString(), sourceNode.name)
                    } else null
                } else null
            } else null
        }.toMap()
    }

    private fun buildOutputConnections(
        workspace: Workspace,
        node: Node
    ): Map<String, List<Pair<String, String>>> {
        return workspace.graph.getEdges(node)
            .filter { it.direction == "output" }
            .associate { edge ->
                val targets = workspace.graph.links.filter { it.from == edge.uid }
                    .mapNotNull { link ->
                        val targetEdge = workspace.graph.getEdge(link.to)
                        val targetNode = targetEdge?.let { workspace.graph.getNode(it.owner) }
                        if (targetNode != null) {
                            Pair(targetNode.uid.toString(), targetNode.name)
                        } else null
                    }
                edge.name to targets
            }
    }

    private fun getDefaultValue(edge: Edge): String {
        val value = edge.value
        if (value.isEmpty) return "nil"

        // Extract type and default value from the edge
        val type = value["type"]?.cast<Property.String>()?.get() ?: "float"
        val defaultValue = value["default"] ?: Property.Float(0f)

        // Convert the value based on its type
        return when (type) {
            "float" -> (defaultValue as? Property.Float)?.get()?.toString() ?: "0.0"
            "int" -> (defaultValue as? Property.Int)?.get()?.toString() ?: "0"
            "boolean" -> (defaultValue as? Property.Boolean)?.get()?.toString() ?: "false"
            "string" -> "\"${(defaultValue as? Property.String)?.get() ?: ""}\""
            "color" -> "\"${(defaultValue as? Property.String)?.get() ?: "#00000000"}\""
            else -> "nil"
        }
    }

    private fun multiLineString(input: String): String {
        val lines = input.split("\n")
        return if (lines.size > 1) {
            // Format multi-line strings for Lua
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

    /**
     * Special handling for function instance scope variables
     */
    private fun processFunctionScopeVariables(
        node: Node,
        functionContext: FunctionContext,
        workspace: Workspace
    ): Map<String, String> {
        val scopedVars = mutableMapOf<String, String>()

        // Process input mappings
        functionContext.inputs.forEach { (name, edge) ->
            val connections = workspace.graph.links.filter { it.to == edge.uid }
            connections.forEach { link ->
                val sourceEdge = workspace.graph.getEdge(link.from)
                if (sourceEdge != null) {
                    val sourceNode = workspace.graph.getNode(sourceEdge.owner)
                    if (sourceNode != null) {
                        scopedVars[name] = "${LuaTranspiler.sanitizeName("${sourceNode.name}_${sourceNode.uid}")}_${
                            LuaTranspiler.sanitizeName(sourceEdge.name)
                        }"
                    }
                }
            }
        }

        // Process output mappings
        functionContext.outputs.forEach { (name, edge) ->
            val connections = workspace.graph.links.filter { it.from == edge.uid }
            connections.forEach { link ->
                val targetEdge = workspace.graph.getEdge(link.to)
                if (targetEdge != null) {
                    val targetNode = workspace.graph.getNode(targetEdge.owner)
                    if (targetNode != null) {
                        scopedVars["${name}_target"] = "${LuaTranspiler.sanitizeName("${targetNode.name}_${targetNode.uid}")}_${
                            LuaTranspiler.sanitizeName(targetEdge.name)
                        }"
                    }
                }
            }
        }

        return scopedVars
    }

    /**
     * Resolves dependencies between nodes in function instances
     */
    private fun resolveFunctionInstanceDependencies(
        node: Node,
        functionContext: FunctionContext,
        workspace: Workspace
    ): Set<Node> {
        val dependencies = mutableSetOf<Node>()

        // Collect input dependencies
        workspace.graph.getEdges(node)
            .filter { it.direction == "input" }
            .forEach { edge ->
                val sourceLink = workspace.graph.links.find { it.to == edge.uid }
                if (sourceLink != null) {
                    val sourceEdge = workspace.graph.getEdge(sourceLink.from)
                    if (sourceEdge != null) {
                        val sourceNode = workspace.graph.getNode(sourceEdge.owner)
                        if (sourceNode != null && sourceNode.function == functionContext.function.uid) {
                            dependencies.add(sourceNode)
                        }
                    }
                }
            }

        // Include execution dependencies
        workspace.graph.getEdges(node)
            .filter { it.direction == "input" && it.type == "exec" }
            .forEach { execEdge ->
                workspace.graph.links.filter { it.to == execEdge.uid }
                    .forEach { link ->
                        val sourceEdge = workspace.graph.getEdge(link.from)
                        if (sourceEdge != null) {
                            val sourceNode = workspace.graph.getNode(sourceEdge.owner)
                            if (sourceNode != null && sourceNode.function == functionContext.function.uid) {
                                dependencies.add(sourceNode)
                            }
                        }
                    }
            }

        return dependencies
    }

    /**
     * Validates that a node's connections are consistent within its function instance
     */
    private fun validateFunctionInstanceConnections(
        node: Node,
        functionContext: FunctionContext,
        workspace: Workspace
    ): Boolean {
        // Verify all connected nodes are part of the same function instance
        val connectedNodes = mutableSetOf<Node>()

        // Check input connections
        workspace.graph.getEdges(node)
            .filter { it.direction == "input" }
            .forEach { edge ->
                val sourceLink = workspace.graph.links.find { it.to == edge.uid }
                if (sourceLink != null) {
                    val sourceEdge = workspace.graph.getEdge(sourceLink.from)
                    if (sourceEdge != null) {
                        val sourceNode = workspace.graph.getNode(sourceEdge.owner)
                        if (sourceNode != null) {
                            connectedNodes.add(sourceNode)
                        }
                    }
                }
            }

        // Check output connections
        workspace.graph.getEdges(node)
            .filter { it.direction == "output" }
            .forEach { edge ->
                workspace.graph.links.filter { it.from == edge.uid }
                    .forEach { link ->
                        val targetEdge = workspace.graph.getEdge(link.to)
                        if (targetEdge != null) {
                            val targetNode = workspace.graph.getNode(targetEdge.owner)
                            if (targetNode != null) {
                                connectedNodes.add(targetNode)
                            }
                        }
                    }
            }

        // Verify all connected nodes are in the same function instance
        return connectedNodes.all { it.function == functionContext.function.uid }
    }

    companion object {

        /**
         * Creates a map of node dependencies within a function instance
         */
        fun createDependencyMap(
            nodes: List<Node>,
            workspace: Workspace
        ): Map<UUID, Set<UUID>> {
            val dependencies = mutableMapOf<UUID, MutableSet<UUID>>()

            nodes.forEach { node ->
                val nodeDeps = mutableSetOf<UUID>()

                // Add input dependencies
                workspace.graph.getEdges(node)
                    .filter { it.direction == "input" }
                    .forEach { edge ->
                        val sourceLink = workspace.graph.links.find { it.to == edge.uid }
                        if (sourceLink != null) {
                            val sourceEdge = workspace.graph.getEdge(sourceLink.from)
                            if (sourceEdge != null) {
                                val sourceNode = workspace.graph.getNode(sourceEdge.owner)
                                if (sourceNode != null) {
                                    nodeDeps.add(sourceNode.uid)
                                }
                            }
                        }
                    }

                dependencies[node.uid] = nodeDeps
            }

            return dependencies
        }
    }
}