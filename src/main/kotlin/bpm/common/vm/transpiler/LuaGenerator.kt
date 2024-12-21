package bpm.common.vm.transpiler

import bpm.Bpm
import bpm.common.utils.sanitize
import bpm.common.workspace.Workspace

object LuaGenerator {

    private const val INDENT = "  "
    private var currentIndentLevel = 0

    fun generate(workspace: Workspace): String = generateSource(workspace, ASTGenerator(workspace).generate())

    /**
     * Generates Lua code from a list of AST nodes.
     */
    private fun generateSource(workspace: Workspace, nodes: List<ASTNode.Node>): String = buildString {
        currentIndentLevel = 0

        appendLineIndented("-- Generated Lua Code")
        appendLine()

        // Generate workspace accessor
        appendLineIndented("-- Workspace Accessor")
        appendLineIndented("local _Uid = \"${workspace.uid}\"")
        appendLine()

        //Generate built-in classes
        val builtIns = Bpm.bootstrap.getBuiltIns()
        appendLineIndented("-- Built-in Classes")
        builtIns.forEach { builtIn ->
            val classPath = builtIn.javaClass.name
            appendLineIndented("local ${builtIn.name} = java.import('$classPath')")
        }
        appendLine()

        // Generate global variables table
        appendLineIndented("-- Global Variables")
        appendLineIndented("local variables = {}")
        appendLine()

        // Generate global outputs table for sharing data between nodes
        appendLineIndented("-- Global Outputs")
        appendLineIndented("local outputs = {}")
        appendLine()

        appendLineIndented("-- Function forward Declarations")
        // Generate function declarations first to allow mutual recursion
        nodes.forEach { node ->
            appendLineIndented("local ${getFunctionName(node)}")
        }
        appendLine()

        // Generate function implementations
        nodes.forEach { node ->
            generateFunction(workspace, node, this)
            appendLine()
        }

        // Generate event handlers
        appendLineIndented("-- Event Handlers")
        appendLineIndented("return {")
        indented {
            nodes.filter { it.type == "Events" }.forEach { node ->
                appendLineIndented("${node.name} = {${getFunctionName(node)}},")
            }
        }
        appendLineIndented("}")
    }

    private fun generateFunction(workspace: Workspace, node: ASTNode.Node, builder: StringBuilder) {
        builder.apply {
            appendLineIndented("-- Node: ${node.name} (${node.id})")
            appendLineIndented("${getFunctionName(node)} = function()")
            var hasInputs = false
            indented {
                //If there's inputs, we add a comment
                if (node.inputs.filterNot { it.type === "exec" }.isNotEmpty()) {
                    appendLineIndented("-------- Inputs --------")
                    hasInputs = true
                }
                // Generate input assignments
                node.inputs.filterNot { it.type == "exec" }.forEach { input ->
                    if (input.sourceNodeId != null && input.sourceEdgeName != null) {
                        val sourceFuncName = getFunctionNameById(workspace, input.sourceNodeId)
                        //If there's a source node and edge, we need to call it's function first
                        appendLineIndented("$sourceFuncName()")
                        //Then we can make use of the output from the source node
                        appendLineIndented("local ${input.name} = outputs['${sourceFuncName}_${input.sourceEdgeName}']")
                    } else if (input.defaultValue != null) {
                        appendLineIndented("local ${input.name} = ${input.defaultValue}")
                    } else {
                        appendLineIndented("local ${input.name} = nil")
                    }
                }

                if (node.statements.isNotEmpty()) {
                    if (hasInputs) appendLine()
                    appendLineIndented("-------- Statements --------")
                }

                // Generate statements
                node.statements.forEach { statement ->
                    when (statement) {
                        is ASTNode.Statement.Literal -> {

                            //convert to lines, remove indent and empty lines, then add back the proper indent
                            statement.value.lines().map { it.trim() }.filter { it.isNotEmpty() }.forEach {
                                appendLineIndented(it)
                            }
                        }

                        is ASTNode.Statement.NodeReference -> {
                            appendLineIndented("${getFunctionNameById(workspace, statement.nodeId)}()")
                        }

                        is ASTNode.Statement.ExecFlow -> {
                            val ourEdgeId = statement.ourEdgeId

                            //gets the nodes outputs for the given edge
                            val ourEdges = node.outputs.find { it.name == ourEdgeId }?.targets ?: emptyList()
                            val targetNodes = ourEdges.map { getFunctionNameById(workspace, it.nodeId) }
                            //Call all the target nodes
                            for (targetNode in targetNodes) {
                                appendLineIndented("$targetNode()")
                            }

                        }

                        is ASTNode.Statement.VariableReference -> {
                            appendLineIndented("local ${statement.name} = variables['${statement.name}']")
                        }

                        is ASTNode.Statement.SetupBlock -> {
                            appendLineIndented(statement.content.trim())
                        }

                        is ASTNode.Statement.OutputAssignment -> {
                            val outputKey = "${getFunctionName(node)}_${statement.outputName}"
                            appendLineIndented("outputs['$outputKey'] = ${statement.value}")
                        }
                    }
                }
            }
            appendLineIndented("end")
        }
    }

    private fun getFunctionName(node: ASTNode.Node): String =
        "${node.name}_${node.id}".sanitize()

    private fun getFunctionNameById(workspace: Workspace, nodeId: String): String {
        val node = workspace.graph.getNode(java.util.UUID.fromString(nodeId))
            ?: throw IllegalStateException("Node not found: $nodeId")
        return "${node.name}_$nodeId".sanitize()
    }


    private fun indented(block: () -> Unit) {
        currentIndentLevel++
        block()
        currentIndentLevel--
    }

    private fun StringBuilder.appendLineIndented(str: String = "") {
        if (str.isNotEmpty()) {
            append(INDENT.repeat(currentIndentLevel))
        }
        appendLine(str)
    }
}