package bpm.common.vm.transpiler

import bpm.common.property.Property
import bpm.common.type.NodeLibrary
import bpm.common.type.NodeType
import bpm.common.type.NodeTypeMeta
import bpm.common.workspace.Workspace
import bpm.common.workspace.graph.Edge
import bpm.common.workspace.graph.Graph
import bpm.common.workspace.graph.Link
import bpm.common.workspace.graph.Node
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.assertEquals

class ASTGeneratorTest {

    private lateinit var workspace: Workspace
    private lateinit var graph: Graph
    private lateinit var library: NodeLibrary
    private val testNodeId = UUID.randomUUID()
    private val sourceNodeId = UUID.randomUUID()
    private val targetNodeId = UUID.randomUUID()

    @BeforeEach
    fun setup() {
        graph = mockk(relaxed = true)
        library = mockk<NodeLibrary>()

        // Fix: Use a single matcher for the complete path
        every { library[any()] } returns NodeType(
            NodeTypeMeta("TestNode", "Test"),
            Property.Object().apply {
                this["source"] = Property.String("default source")
            }
        )

        workspace = spyk(Workspace(
            graph = graph,
            nodeLibrary = library,
            workspaceName = "test",
            users = mutableMapOf(),
            description = "Test Workspace",
            uid = UUID.fromString("00000000-0000-0000-0000-000000000000")
        ))
    }

    @Test
    fun `test generate node with inputs and outputs`() {
        // Create a test node
        val node = mockk<Node> {
            every { uid } returns testNodeId
            every { name } returns "TestNode"
            every { type } returns "Test"
            every { properties } returns Property.Object()
        }

        // Create input edge
        val inputEdge = mockk<Edge> {
            every { uid } returns UUID.randomUUID()
            every { name } returns "input"
            every { direction } returns "input"
            every { type } returns "number"
            every { value } returns Property.Object().apply {
                this["type"] = Property.String("number")
            }.apply {
                this["type"] = Property.String("float")
                this["default"] = Property.Float(42.0f)
            }
        }

        // Create output edge
        val outputEdge = mockk<Edge> {
            every { uid } returns UUID.randomUUID()
            every { name } returns "output"
            every { direction } returns "output"
            every { type } returns "number"
            every { value } returns Property.Object()
        }

        // Mock graph behavior
        every { graph.nodes } returns listOf(node)
        every { graph.getEdges(node) } returns listOf(inputEdge, outputEdge)
        every { graph.links } returns emptyList()

        // Create generator and generate AST
        val generator = ASTGenerator(workspace, library)
        val result = generator.generate()

        // Verify results
        assertEquals(1, result.size)
        val astNode = result[0]
        assertEquals(testNodeId.toString(), astNode.id)
        assertEquals("TestNode", astNode.name)
        assertEquals("Test", astNode.type)

        // Verify input
        assertEquals(1, astNode.inputs.size)
        assertEquals("input", astNode.inputs[0].name)
        assertEquals("number", astNode.inputs[0].type)
        assertEquals("42.0", astNode.inputs[0].defaultValue)

        // Verify output
        assertEquals(1, astNode.outputs.size)
        assertEquals("output", astNode.outputs[0].name)
        assertEquals("number", astNode.outputs[0].type)
    }

    @Test
    fun `test node connection handling`() {
        // Create source and target nodes
        val sourceNode = mockk<Node> {
            every { uid } returns sourceNodeId
            every { name } returns "SourceNode"
            every { type } returns "Test"
            every { properties } returns Property.Object()
            every { function } returns UUID.randomUUID()
            every { edgeIds } returns mutableListOf()
        }

        val targetNode = mockk<Node> {
            every { uid } returns targetNodeId
            every { name } returns "TargetNode"
            every { type } returns "Test"
            every { properties } returns Property.Object()
            every { function } returns UUID.randomUUID()
            every { edgeIds } returns mutableListOf()
        }

        // Create connected edges with fixed UUIDs
        val sourceEdgeId = UUID.randomUUID()
        val targetEdgeId = UUID.randomUUID()

        val sourceEdge = mockk<Edge> {
            every { uid } returns sourceEdgeId
            every { name } returns "output"
            every { direction } returns "output"
            every { type } returns "number"
            every { owner } returns sourceNodeId
            every { value } returns Property.Object().apply {
                this["type"] = Property.String("number")
            }
        }

        val targetEdge = mockk<Edge> {
            every { uid } returns targetEdgeId
            every { name } returns "input"
            every { direction } returns "input"
            every { type } returns "number"
            every { owner } returns targetNodeId
            every { value } returns Property.Object().apply {
                this["type"] = Property.String("number")
            }
        }

        // Create link with the fixed edge IDs
        val link = mockk<Link> {
            every { uid } returns UUID.randomUUID()
            every { from } returns sourceEdgeId
            every { to } returns targetEdgeId
        }

        // Mock graph behavior to track edges properly
        every { sourceNode.edgeIds } returns mutableListOf(Property.UUID(sourceEdgeId))
        every { targetNode.edgeIds } returns mutableListOf(Property.UUID(targetEdgeId))

        // Mock graph APIs
        every { graph.nodes } returns listOf(sourceNode, targetNode)
        every { graph.getEdges(any<Node>()) } answers {
            val node = firstArg<Node>()
            println("Getting edges for node: ${node.uid}")
            node.edgeIds.map { edgeId ->
                when (edgeId.get()) {
                    sourceEdgeId -> sourceEdge
                    targetEdgeId -> targetEdge
                    else -> null
                }
            }.filterNotNull()
        }
        every { graph.links } returns listOf(link)
        every { graph.getEdge(any()) } answers {
            val uid = firstArg<UUID>()
            when (uid) {
                sourceEdgeId -> sourceEdge
                targetEdgeId -> targetEdge
                else -> null
            }
        }
        every { graph.getNode(any()) } answers {
            val uid = firstArg<UUID>()
            when (uid) {
                sourceNodeId -> sourceNode
                targetNodeId -> targetNode
                else -> null
            }
        }

        // Update source node's edge IDs
        every { sourceNode.edgeIds } returns mutableListOf(Property.UUID(sourceEdgeId))

        // Update target node's edge IDs
        every { targetNode.edgeIds } returns mutableListOf(Property.UUID(targetEdgeId))


        // Mock graph behavior
        every { graph.nodes } returns listOf(sourceNode, targetNode)
        every { graph.getEdges(any<Node>()) } answers {
            val node = firstArg<Node>()
            when (node.uid) {
                sourceNodeId -> listOf(sourceEdge)
                targetNodeId -> listOf(targetEdge)
                else -> emptyList()
            }
        }
        every { graph.links } returns listOf(link)
        every { graph.getEdge(any()) } answers {
            val uid = firstArg<UUID>()
            when (uid) {
                sourceEdge.uid -> sourceEdge
                targetEdge.uid -> targetEdge
                else -> null
            }
        }
        every { graph.getNode(any()) } answers {
            val uid = firstArg<UUID>()
            when (uid) {
                sourceNodeId -> sourceNode
                targetNodeId -> targetNode
                else -> null
            }
        }

        // Generate AST
        val generator = ASTGenerator(workspace)
        val result = generator.generate()

        // Verify results
        assertEquals(2, result.size)

        // Find source and target nodes in result
        val sourceAstNode = result.find { it.id == sourceNodeId.toString() }!!
        val targetAstNode = result.find { it.id == targetNodeId.toString() }!!

        // Verify source node output
        assertEquals(1, sourceAstNode.outputs.size)
        val output = sourceAstNode.outputs[0]
        assertEquals("output", output.name)
        assertEquals(1, output.targets.size)
        assertEquals(targetNodeId.toString(), output.targets[0].nodeId)
        assertEquals("input", output.targets[0].inputName)

        // Verify target node input
        assertEquals(1, targetAstNode.inputs.size)
        val input = targetAstNode.inputs[0]
        assertEquals("input", input.name)
        assertEquals(sourceNodeId.toString(), input.sourceNodeId)
        assertEquals("output", input.sourceEdgeName)
    }

    @Test
    fun `test default value handling for different types`() {
        val edgeId = UUID.randomUUID()
        val node = mockk<Node> {
            every { uid } returns testNodeId
            every { name } returns "TestNode"
            every { type } returns "Test"
            every { properties } returns Property.Object()
        }

        // Test each property type
        val testCases = listOf(
            Triple("float", Property.Float(42.5f), "42.5"),
            Triple("int", Property.Int(42), "42"),
            Triple("boolean", Property.Boolean(true), "true"),
            Triple("string", Property.String("hello"), "hello")
        )

        testCases.forEach { (type, defaultValue, expected) ->
            // Create edge with specific type and default value
            val edge = mockk<Edge> {
                every { uid } returns edgeId
                every { name } returns "input"
                every { direction } returns "input"
                every { this@mockk.type } returns type
                every { value } returns Property.Object().apply {
                    this["type"] = Property.String(type)
                    this["default"] = defaultValue
                }
            }

            // Mock graph behavior
            every { graph.nodes } returns listOf(node)
            every { graph.getEdges(node) } returns listOf(edge)
            every { graph.links } returns emptyList()

            // Generate AST
            val generator = ASTGenerator(workspace)
            val result = generator.generate()

            // Verify default value
            assertEquals(expected, result[0].inputs[0].defaultValue)
        }
    }
}