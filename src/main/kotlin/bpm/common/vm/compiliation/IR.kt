package bpm.common.vm.compiliation

class IR {

    val variables = mutableMapOf<String, IRValue>()
    val functions = mutableListOf<IRFunction>()
    val edges = mutableListOf<IREdge>()
}