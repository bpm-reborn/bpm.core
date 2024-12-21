package bpm.common.vm.compiliation

sealed class IRStatement { data class Literal(val value: String) : IRStatement()
    data class NodeReference(val name: String) : IRStatement()
    data class ExecReference(val name: String) : IRStatement()
    data class VarReference(val name: String) : IRStatement()
    data class LambdaReference(val name: String) : IRStatement()
    data class JavaImport(val name: String) : IRStatement()
    data class GenericExpression(val content: String) : IRStatement()
    data class OutputAssignment(val name: String, val value: String) : IRStatement()
}