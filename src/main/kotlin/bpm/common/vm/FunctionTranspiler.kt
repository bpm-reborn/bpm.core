package bpm.common.vm

import bpm.common.workspace.Workspace
import bpm.common.workspace.graph.Function
import bpm.common.workspace.graph.Node
import java.util.*

object FunctionTranspiler {

//    /**
//     * Transforms the function node references in the workspace into their respective implementations.
//     * Keeps the workspace immutable. The returned workspace is a new instance that is used for
//     * node graph transpilation.
//     */
//    fun generateFunctionNodeReferenceImplementations(workspace: Workspace): Workspace {
//        val newGraph = workspace.graph.copy()
//        val newWorkspace = Workspace(
//            newGraph,
//            workspace.nodeLibrary,
//            workspace.workspaceName,
//            workspace.users,
//            workspace.description,
//            UUID.randomUUID(),
//            workspace.settings
//        )
//    }
//
//    //Represents a function node reference, which is a function (group of nodes with virtual inputs and outputs) and the nodes that reference it.
//    data class FunctionNodeReference(
//        val function: Function,
//        val functionReferences: List<Node>
//    )
}