package bpm.common.vm.compiliation

data class IRFunction(
        val id: String,
        val originalName: String,
        val nodeType: String,
        val inputEdges: List<String>,
        val inputConnections: Map<String, Pair<String, String>>,
        val outputEdges: Map<String, List<Pair<String, String>>>,
        val body: MutableList<IRStatement> = mutableListOf(),
        val setupBlocks: MutableSet<String> = mutableSetOf(),
        val dependencies: MutableList<IRFunction> = mutableListOf()
)