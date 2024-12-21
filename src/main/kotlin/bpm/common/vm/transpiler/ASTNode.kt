package bpm.common.vm.transpiler

// The main AST node interface
sealed interface ASTNode {

    data class WorkspaceAST(
        val uid: String,
        val nodes: List<Node>,
        val builtIns: List<String> // List of built-in class names
    )

    // A node in the workspace
    data class Node(
        val id: String,
        val name: String,
        val type: String,
        val statements: List<Statement> = emptyList(),
        val inputs: List<Input> = emptyList(),
        val outputs: List<Output> = emptyList()
    ) : ASTNode

    // Input port on a node
    data class Input(
        val name: String,
        val type: String,
        val sourceNodeId: String? = null,
        val sourceEdgeName: String? = null,
        val defaultValue: String? = null
    )

    // Output port on a node
    data class Output(
        val name: String,
        val type: String,
        val targets: List<Target> = emptyList()
    ) {

        data class Target(
            val nodeId: String,
            val inputName: String
        )
    }

    // Different types of statements that can appear in a node
    sealed interface Statement {

        data class Literal(val value: String) : Statement
        data class NodeReference(val nodeId: String) : Statement
        data class ExecFlow(val ourEdgeId: String) : Statement
        data class VariableReference(val name: String) : Statement
        data class SetupBlock(val content: String) : Statement
        data class OutputAssignment(
            val outputName: String,
            val value: String
        ) : Statement
    }
}