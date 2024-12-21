package bpm.common.vm.compiliation

sealed class ASTNode { data class NodeDeclaration(
    val id: String, val children: MutableList<ASTNode> = mutableListOf()
) : ASTNode()

    data class EdgeDeclaration(val id: String) : ASTNode()
    data class Literal(val value: String) : ASTNode()
    data class NodeReference(val name: String) : ASTNode()
    data class ExecReference(val name: String) : ASTNode()
    data class VarReference(val name: String) : ASTNode()
    data class LambdaReference(val name: String) : ASTNode()
    data class JavaImport(val name: String) : ASTNode()
    data class SetupBlock(val content: String) : ASTNode()
    data class GenericExpression(val content: String) : ASTNode()
    data class OutputAssignment(val name: String, val value: String) : ASTNode()
}