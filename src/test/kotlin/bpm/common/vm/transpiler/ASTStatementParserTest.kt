package bpm.common.vm.transpiler

import bpm.common.property.Property
import bpm.common.type.NodeLibrary
import bpm.common.type.NodeType
import bpm.common.type.NodeTypeMeta
import bpm.common.workspace.graph.Node
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ASTStatementParserTest {

    private lateinit var library: NodeLibrary
    private lateinit var parser: ASTStatementParser
    private lateinit var node: Node
    private val testNodeId = UUID.randomUUID()

    @BeforeEach
    fun setup() {
        library = mockk(relaxed = true)
        parser = ASTStatementParser(library)
        node = mockk {
            every { uid } returns testNodeId
            every { name } returns "TestNode"
            every { type } returns "Test"
            every { properties } returns Property.Object()
        }
    }

    @Test
    fun `test parsing literal statement`() {
        val source = "console.log('Hello World')"
        mockNodeTemplate(source)

        val statements = parser.parseStatements(node)

        assertEquals(1, statements.size)
        assertTrue(statements[0] is ASTNode.Statement.Literal)
        assertEquals(source, (statements[0] as ASTNode.Statement.Literal).value)
    }

    @Test
    fun `test parsing node reference statement`() {
        val nodeId = UUID.randomUUID()
        val source = "${'$'}{NODE.$nodeId}"
        mockNodeTemplate(source)

        val statements = parser.parseStatements(node)

        assertEquals(1, statements.size)
        assertTrue(statements[0] is ASTNode.Statement.NodeReference)
        assertEquals(nodeId.toString(), (statements[0] as ASTNode.Statement.NodeReference).nodeId)
    }

    @Test
    fun `test parsing execution flow statement`() {
        val targetId = UUID.randomUUID()
        val source = "${'$'}{EXEC.$targetId}"
        mockNodeTemplate(source)

        val statements = parser.parseStatements(node)

        assertEquals(1, statements.size)
        assertTrue(statements[0] is ASTNode.Statement.ExecFlow)
        assertEquals(targetId.toString(), (statements[0] as ASTNode.Statement.ExecFlow).ourEdgeId)
    }

    @Test
    fun `test parsing variable reference statement`() {
        val source = "${'$'}{VARS.testVariable}"
        mockNodeTemplate(source)

        val statements = parser.parseStatements(node)

        assertEquals(1, statements.size)
        assertTrue(statements[0] is ASTNode.Statement.VariableReference)
        assertEquals("testVariable", (statements[0] as ASTNode.Statement.VariableReference).name)
    }

    @Test
    fun `test parsing setup block statement`() {
        val source = "${'$'}{SETUP.local x = 42}"
        mockNodeTemplate(source)

        val statements = parser.parseStatements(node)

        assertEquals(1, statements.size)
        assertTrue(statements[0] is ASTNode.Statement.SetupBlock)
        assertEquals("local x = 42", (statements[0] as ASTNode.Statement.SetupBlock).content)
    }

    @Test
    fun `test parsing output assignment statement`() {
        val source = "${'$'}{OUTPUT.result = 42}"
        mockNodeTemplate(source)

        val statements = parser.parseStatements(node)

        assertEquals(1, statements.size)
        assertTrue(statements[0] is ASTNode.Statement.OutputAssignment)
        val assignment = statements[0] as ASTNode.Statement.OutputAssignment
        assertEquals("result", assignment.outputName)
        assertEquals("42", assignment.value)
    }

    @Test
    fun `test parsing multiple statements`() {
        val source = """
            console.log('Start')
            ${'$'}{NODE.${UUID.randomUUID()}}
            console.log('Middle')
            ${'$'}{EXEC.${UUID.randomUUID()}}
            console.log('End')
        """.trimIndent()
        mockNodeTemplate(source)

        val statements = parser.parseStatements(node)

        assertEquals(5, statements.size)
        assertTrue(statements[0] is ASTNode.Statement.Literal)
        assertTrue(statements[1] is ASTNode.Statement.NodeReference)
        assertTrue(statements[2] is ASTNode.Statement.Literal)
        assertTrue(statements[3] is ASTNode.Statement.ExecFlow)
        assertTrue(statements[4] is ASTNode.Statement.Literal)
    }

    @Test
    fun `test parsing mixed content within expressions`() {
        val source = """
            ${'$'}{SETUP.local count = 0}
            if count < 10 then
                ${'$'}{OUTPUT.value = count}
                ${'$'}{EXEC.${UUID.randomUUID()}}
            end
        """.trimIndent()
        mockNodeTemplate(source)

        val statements = parser.parseStatements(node)

        assertTrue(statements.isNotEmpty())
        assertTrue(statements[0] is ASTNode.Statement.SetupBlock)
        assertTrue(statements[1] is ASTNode.Statement.Literal)
        assertTrue(statements[2] is ASTNode.Statement.OutputAssignment)
        assertTrue(statements[3] is ASTNode.Statement.ExecFlow)
        assertTrue(statements[4] is ASTNode.Statement.Literal)
    }

    @Test
    fun `test parsing with source override`() {
        val overrideSource = "${'$'}{VARS.overrideVar}"
        every { node.properties.contains("override") } returns true
        every { node["override"] } returns Property.String(overrideSource)

        val statements = parser.parseStatements(node)

        assertEquals(1, statements.size)
        assertTrue(statements[0] is ASTNode.Statement.VariableReference)
        assertEquals("overrideVar", (statements[0] as ASTNode.Statement.VariableReference).name)
    }

    @Test
    fun `test parsing invalid expression content`() {
        val source = "${'$'}{INVALID.content}"
        mockNodeTemplate(source)

        val statements = parser.parseStatements(node)

        assertTrue(statements.isEmpty())
    }

    @Test
    fun `test parsing incomplete output assignment`() {
        val source = "${'$'}{OUTPUT.result}"  // Missing assignment
        mockNodeTemplate(source)

        val statements = parser.parseStatements(node)

        assertTrue(statements.isEmpty())
    }

    @Test
    fun `test parsing with multiple expressions in single line`() {
        val nodeId = UUID.randomUUID()
        val source = "Start ${'$'}{NODE.$nodeId} Middle ${'$'}{VARS.testVar} End"
        mockNodeTemplate(source)

        val statements = parser.parseStatements(node)

        assertEquals(5, statements.size)
        assertTrue(statements[0] is ASTNode.Statement.Literal)
        assertTrue(statements[1] is ASTNode.Statement.NodeReference)
        assertTrue(statements[2] is ASTNode.Statement.Literal)
        assertTrue(statements[3] is ASTNode.Statement.VariableReference)
        assertTrue(statements[4] is ASTNode.Statement.Literal)
    }

    @Test
    fun `test parsing empty source content`() {
        mockNodeTemplate("")

        val statements = parser.parseStatements(node)

        assertTrue(statements.isEmpty())
    }

    @Test
    fun `test parsing whitespace handling`() {
        val source = """
            
            ${'$'}{NODE.${UUID.randomUUID()}}
            
            ${'$'}{VARS.testVar}
            
        """.trimIndent()
        mockNodeTemplate(source)

        val statements = parser.parseStatements(node)

        assertEquals(2, statements.size)
        assertTrue(statements[0] is ASTNode.Statement.NodeReference)
        assertTrue(statements[1] is ASTNode.Statement.VariableReference)
    }

    private fun mockNodeTemplate(source: String) {
        val nodeType = NodeType(
            NodeTypeMeta("TestNode", "Test"),
            Property.Object().apply {
                this["source"] = Property.String(source)
            }
        )
        every { library["Test/TestNode"] } returns nodeType
        every { node.properties.contains("override") } returns false
    }
}