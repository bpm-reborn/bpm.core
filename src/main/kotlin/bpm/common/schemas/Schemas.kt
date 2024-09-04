package bpm.common.schemas

import bpm.common.logging.KotlinLogging
import bpm.common.network.Endpoint
import bpm.common.network.Listener
import bpm.common.packets.Packet
import bpm.common.type.NodeLibrary
import bpm.common.workspace.packets.NodeLibraryReloadRequest
import bpm.common.workspace.packets.NodeLibraryRequest
import bpm.common.workspace.packets.NodeLibraryResponse
import java.nio.file.Path
import java.util.*

class Schemas(private val path: Path, private val side: Endpoint.Side) : Listener {

    val library: NodeLibrary = NodeLibrary()
    private val logger = KotlinLogging.logger { }

    override fun onInstall() {
        if (side == Endpoint.Side.CLIENT) return
        //This is still ran on the client, the client has two instances of schemas (one for server and one for client as singleplayer still internally uses a server)




        //Load the node library, todo: move this to a config file
        library.readFrom(path)
        val types = library.collect()
        logger.info { "Loaded ${types.size} types" }
    }

    override fun onConnect(uuid: UUID) {
        if (side == Endpoint.Side.SERVER) return
        //Request the node library from the server
        client.send(NodeLibraryRequest())
    }

    override fun onPacket(packet: Packet, from: UUID) {
        if (packet is NodeLibraryRequest) {
            //Send the node library to the client
            val response = NodeLibraryResponse(library.collectToPropertyList())
            //Send the response to the client
            server.send(response, from)
            logger.debug { "Sent node library to client $from with ${library.count()} types" }
        }
        if (packet is NodeLibraryResponse) {
            if (side == Endpoint.Side.CLIENT) {
                library.clear()
                library.loadFrom(packet.nodeSchemas)
                logger.debug { "Received node library from server with ${library.count()} types" }
            }
        }
        if (packet is NodeLibraryReloadRequest) {
            library.clear()
            library.readFrom(path)
            val types = library.collect()
            logger.info { "Loaded ${types.size} types" }
            //send to all in workspace
            server.sendToAll(NodeLibraryResponse(library.collectToPropertyList()))
        }
    }

}