package bpm.common.vm.transpiler

import bpm.common.utils.sanitize
import bpm.common.workspace.Workspace

object LuaGenerator {

    private const val INDENT = "  "
    private var currentIndentLevel = 0

    fun generate(workspace: Workspace): String = generateSource(ASTGenerator(workspace).generate())

    /**
     * Generates Lua code from a list of AST nodes.
     */
    private fun generateSource(workspaceAST: ASTNode.WorkspaceAST): String = buildString {
        currentIndentLevel = 0

        appendLineIndented("-- Generated Lua Code")
        appendLine()

//        appendLineIndented("-- Random seed")
//        appendLineIndented("math.randomseed(os.time())")

        // Generate workspace accessor
        appendLineIndented("-- Workspace Accessor")
        appendLineIndented("local _Uid = \"${workspaceAST.uid}\"")
        appendLine()

        // Generate built-in classes
        appendLineIndented("-- Built-in Classes")
        workspaceAST.builtIns.forEach { (builtInName, javaClass) ->
            appendLineIndented("local $builtInName = java.import('${javaClass}')")
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
        workspaceAST.nodes.forEach { node ->
            appendLineIndented("local ${getFunctionName(node)}")
        }
        appendLine()

        // Generate function implementations
        workspaceAST.nodes.forEach { node ->
            generateFunction(node, workspaceAST, this)
            appendLine()
        }

        // Generate event handlers
        appendLineIndented("-- Event Handlers")
        appendLineIndented("return {")
        indented {
            workspaceAST.nodes.filter { it.type == "Events" }.forEach { node ->
                appendLineIndented("${node.name} = {${getFunctionName(node)}},")
            }
        }
        appendLineIndented("}")
    }

    private fun generateFunction(node: ASTNode.Node, ast: ASTNode.WorkspaceAST, builder: StringBuilder) {
        builder.apply {
            appendLineIndented("-- Node: ${node.name} (${node.id})")
            appendLineIndented("${getFunctionName(node)} = function()")

            indented {
                if (node.inputs.filterNot { it.type == "exec" }.isNotEmpty()) {
                    appendLineIndented("-------- Inputs --------")
                }

                // Generate input assignments
                node.inputs.filterNot { it.type == "exec" }.forEach { input ->
                    if (input.sourceNodeId != null && input.sourceEdgeName != null) {
                        val sourceNode = ast.findNode(input.sourceNodeId)
                        if (sourceNode != null) {
                            appendLineIndented("${getFunctionName(sourceNode)}()")
                            appendLineIndented("local ${input.name} = outputs['${getFunctionName(sourceNode)}_${input.sourceEdgeName}']")
                        }
                    } else if (input.defaultValue != null) {
                        appendLineIndented("local ${input.name} = ${input.defaultValue}")
                    } else {
                        appendLineIndented("local ${input.name} = nil")
                    }
                }

                if (node.statements.isNotEmpty()) {
                    appendLineIndented("-------- Statements --------")
                }
                val uniqueExecutions = mutableSetOf<String>()

                // Generate statements
                node.statements.forEach { statement ->
                    when (statement) {
                        is ASTNode.Statement.Literal -> {
                            statement.value.lines()
                                .map { it.trim() }
                                .filter { it.isNotEmpty() }
                                .forEach { appendLineIndented(it) }
                        }

                        is ASTNode.Statement.NodeReference -> {
                            val targetNode = ast.findNode(statement.nodeId)
                            appendLineIndented("${targetNode?.let { getFunctionName(it) }}()")
                        }

                        is ASTNode.Statement.ExecFlow -> {
                            val outputEdge = node.outputs.find { it.name == statement.ourEdgeId }
                            outputEdge?.targets?.forEach { target ->
                                val targetFunc = "${target.nodeName!!.sanitize()}_${target.nodeId.sanitize()}"
                                if (targetFunc !in uniqueExecutions) {
                                    uniqueExecutions.add(targetFunc)
                                    appendLineIndented("$targetFunc()")
                                }
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