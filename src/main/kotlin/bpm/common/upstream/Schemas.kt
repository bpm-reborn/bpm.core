package bpm.common.upstream

import bpm.common.logging.KotlinLogging
import bpm.common.network.Endpoint
import bpm.common.network.Listener
import bpm.common.packets.Packet
import bpm.common.property.Property
import bpm.common.property.configured
import bpm.common.type.NodeLibrary
import bpm.common.type.NodeType
import bpm.common.workspace.Workspace
import bpm.common.workspace.graph.Edge
import bpm.common.workspace.graph.Node
import bpm.common.workspace.packets.NodeLibraryReloadRequest
import bpm.common.workspace.packets.NodeLibraryRequest
import bpm.common.workspace.packets.NodeLibraryResponse
import org.joml.Vector2f
import java.nio.file.Path
import java.util.*

class Schemas(private val path: Path, private val side: Endpoint.Side) : Listener {

    val library: NodeLibrary = NodeLibrary()
    private val logger = KotlinLogging.logger { }
    //TODO: make this a configuration option
    private val gitSchemaLoader = GitLoader("https://github.com/bpm-reborn/bpm.nodes.git", "main", path)

    override fun onInstall() {
        if (side == Endpoint.Side.CLIENT) return

        // Clone or pull the repository and load the node library
        val schemasPath = gitSchemaLoader.cloneOrPull()
        library.readFrom(schemasPath)
        val types = library.collect()
        logger.info { "Loaded ${types.size} types from Git repository" }
    }

    override fun onConnect(uuid: UUID) {
        if (side == Endpoint.Side.SERVER) return
        //Request the node library from the server
        client.send(NodeLibraryRequest())
    }

    override fun onPacket(packet: Packet, from: UUID) {
        when (packet) {
            is NodeLibraryRequest -> {
                val response = NodeLibraryResponse(library.collectToPropertyList())
                server.send(response, from)
                logger.debug { "Sent node library to client $from with ${library.count()} types" }
            }

            is NodeLibraryResponse -> {
                if (side == Endpoint.Side.CLIENT) {
                    library.clear()
                    library.loadFrom(packet.nodeSchemas)
                    logger.debug { "Received node library from server with ${library.count()} types" }
                }
            }

            is NodeLibraryReloadRequest -> {
                library.clear()
                val schemasPath = gitSchemaLoader.cloneOrPull()
                library.readFrom(schemasPath)
                val types = library.collect()
                logger.info { "Reloaded ${types.size} types from Git repository" }
                server.sendToAll(NodeLibraryResponse(library.collectToPropertyList()))
            }
        }
    }


    //Simulated means that it's not added to graph at all, it's just a temporary node typically used for rendering
    fun createFromType(workspace: Workspace, nodeType: NodeType, position: Vector2f, simulated: Boolean = false): Node {
        val name = nodeType["name"] as? Property.String ?: Property.String(nodeType.meta.name)
        val theme = nodeType["theme"] as? Property.Object ?: Property.Object()
        val color = parseThemeColor(theme)
        val edges = nodeType["edges"] as? Property.Object ?: Property.Object()
        val width = theme["width"] as? Property.Float ?: theme["width"] as? Property.Int ?: Property.Float(100f)
        val height = theme["height"] as? Property.Float ?: theme["height"] as? Property.Int ?: Property.Float(50f)
        val iconInt = theme["icon"] as? Property.Int ?: Property.Int(0)
        val newNode = configured<Node> {
            "name" to name
            "type" to nodeType.meta.group
            "color" to color
            "x" to position.x
            "y" to position.y
            "uid" to UUID.randomUUID()
            "width" to width
            "height" to height
            "edges" to Property.Object()
            "icon" to iconInt
        }

        // Process edges
        for ((edgeName, edgeProperty) in edges) {
            if (edgeProperty !is Property.Object) continue

            val edgeDirection = edgeProperty["direction"] as? Property.String ?: Property.String("input")
            val edgeType = edgeProperty["type"] as? Property.String ?: Property.String("exec")
            val edgeDescription = edgeProperty["description"] as? Property.String ?: Property.String("")
            val value = edgeProperty["value"] as? Property.Object ?: Property.Object()
            val icon = edgeProperty["icon"] as? Property.Int ?: Property.Int(0)
            val edge = configured<Edge> {
                "name" to Property.String(edgeName)
                "direction" to edgeDirection
                "type" to edgeType
                "description" to edgeDescription
                "uid" to Property.UUID(UUID.randomUUID())
                "value" to value
                "icon" to icon
            }

            (newNode["edges"] as Property.Object)[edgeName] = edge.properties
            if (simulated) continue
            workspace.addEdge(newNode, edge)
        }
        if (simulated) return newNode
        workspace.addNode(newNode)
        return newNode
    }

    private fun parseColor(color: Property.String): Property.Vec4i {
        val color = color.get().removePrefix("#")
        val r = color.substring(0, 2).toInt(16)
        val g = color.substring(2, 4).toInt(16)
        val b = color.substring(4, 6).toInt(16)
        val a = if (color.length == 8) color.substring(6, 8).toInt(16) else 255
        return Property.Vec4i(r, g, b, a)
    }

    private fun parseThemeColor(theme: Property.Object): Property.Vec4i {
        val color = theme["color"]
        if (color is Property.String) {
            return parseColor(color)
        }
        if (color is Property.Vec4i) {
            return Property.Vec4i(color.get().x, color.get().y, color.get().z, color.get().w)
        }
        if (color is Property.Vec4f) {
            return Property.Vec4i(
                (color.get().x * 255).toInt(),
                (color.get().y * 255).toInt(), (color.get().z * 255).toInt(), (color.get().w * 255).toInt()
            )
        }
        return Property.Vec4i(0, 0, 0, 255)
    }


}