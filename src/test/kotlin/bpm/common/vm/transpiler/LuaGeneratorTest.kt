//package bpm.common.vm.transpiler
//
//import bpm.common.property.Property
//import bpm.common.type.NodeLibrary
//import bpm.common.type.NodeType
//import bpm.common.type.NodeTypeMeta
//import bpm.common.utils.sanitize
//import bpm.common.workspace.Workspace
//import bpm.common.workspace.graph.Edge
//import bpm.common.workspace.graph.Graph
//import bpm.common.workspace.graph.Node
//import io.mockk.MockKMatcherScope
//import io.mockk.every
//import io.mockk.mockk
//import net.neoforged.fml.loading.FMLPaths
//import org.junit.jupiter.api.BeforeEach
//import org.junit.jupiter.api.Test
//import java.nio.file.Path
//import java.util.*
//import kotlin.test.assertContains
//import kotlin.test.assertEquals
//import kotlin.test.assertTrue
//import kotlin.test.junit5.JUnit5Asserter.fail
//
//class LuaGeneratorTest {
//
//    @Test
//    fun `test generated execution flow`() {
//        val result = LuaGenerator.generate(workspaceFor {
//            val node1 = Node(Property.Object {
//                "uid" to UUID.fromString("11111111-1111-1111-1111-111111111111")
//                "name" to "Node1"
//                "type" to "Node"
//                "override" to """
//                    ${'$'}{OUTPUT.Edge4 = Edge2}
//                    ${'$'}{EXEC.Edge1}
//            """.trimIndent()
//            })
//
//            val edge1 = Edge(Property.Object {
//                "name" to "Edge1"
//                "direction" to "output"
//                "type" to "exec"
//                "description" to ""
//                "uid" to UUID.fromString("33333333-3333-3333-3333-333333333333")
//                "value" to Property.Object()
//            })
//
//            val edge2 = Edge(Property.Object {
//                "name" to "Edge2"
//                "direction" to "input"
//                "type" to "string"
//                "description" to ""
//                "uid" to UUID.fromString("55555555-5555-5555-5555-555555555555")
//                "value" to Property.Object {
//                    "default" to "test"
//                    "type" to "string"
//                }
//            })
//
//            val edge4 = Edge(Property.Object {
//                "name" to "Edge4"
//                "direction" to "output"
//                "type" to "string"
//                "description" to ""
//                "uid" to UUID.fromString("77777777-7777-7777-7777-777777777777")
//            })
//            addNode(node1)
//            addEdge(node1, edge1)
//            addEdge(node1, edge2)
//            addEdge(node1, edge4)
//            val node2 = Node(Property.Object {
//                "uid" to UUID.fromString("22222222-2222-2222-2222-222222222222")
//                "name" to "Node2"
//                "type" to "Node"
//                "override" to """
//                    print(Edge5)
//                """.trimIndent()
//            })
//            val edge3 = Edge(Property.Object {
//                "name" to "Edge3"
//                "direction" to "input"
//                "type" to "exec"
//                "description" to ""
//                "uid" to UUID.fromString("44444444-4444-4444-4444-444444444444")
//                "value" to Property.Object()
//            })
//
//            val edge5 = Edge(Property.Object {
//                "name" to "Edge5"
//                "direction" to "input"
//                "type" to "string"
//                "description" to ""
//                "uid" to UUID.fromString("66666666-6666-6666-6666-666666666666")
//                "value" to Property.Object {
//                    "default" to "default_test"
//                    "type" to "string"
//                }
//            })
//
//            addNode(node2)
//            addEdge(node2, edge3)
//            addEdge(node2, edge5)
//            addLink(edge1.uid, edge3.uid)
//            //Link the edge 4 to edge 5
//            addLink(edge4.uid, edge5.uid)
//        })
//        assertTrue { result.contains("local _Uid = \"11111111-1111-1111-1111-111111111111\"") }
//        assertTrue { result.contains("local variables = {}") }
//        assertTrue { result.contains("local outputs = {}") }
//        assertTrue { result.contains("local Node1_11111111_1111_1111_1111_111111111111") }
//        assertTrue { result.contains("local Node2_22222222_2222_2222_2222_222222222222") }
//
//        assertContains(
//            result, """
//            Node1_11111111_1111_1111_1111_111111111111 = function()
//              local Edge2 = "test"
//              outputs['Node1_11111111_1111_1111_1111_111111111111_Edge4'] = Edge2
//              Node2_22222222_2222_2222_2222_222222222222()
//            end
//        """.trimIndent()
//        )
//
//        assertContains(result, """
//            Node2_22222222_2222_2222_2222_222222222222 = function()
//              local Edge5 = outputs['Node1_11111111_1111_1111_1111_111111111111_Edge4']
//              print(Edge5)
//            end
//        """.trimIndent())
//    }
//
//    private val workspaceId = UUID.fromString("11111111-1111-1111-1111-111111111111")
//
//    private fun workspaceFor(graph: Graph.() -> Unit): Workspace {
//        val nodeLibrary = NodeLibrary()
//        return Workspace(
//            Graph().apply(graph),
//            nodeLibrary = nodeLibrary,
//            workspaceName = "Test Workspace",
//            users = mutableMapOf(),
//            uid = workspaceId
//        )
//    }
//
//}