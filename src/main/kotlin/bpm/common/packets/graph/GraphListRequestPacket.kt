package bpm.common.packets.graph

import bpm.common.memory.Buffer
import bpm.common.packets.Packet

/**
 * GraphListRequestPacket represents a packet used to request a list of graphs from the server.
 *
 * @property group The group name for which to request the list of graphs. Default value is an empty string.
 * @property size The number of bytes that the packet occupies in memory.
 */
class GraphListRequestPacket(var group: String = "") : Packet {

    /**
     * Serializes the provided Buffer.
     *
     * @param buffer The Buffer to be serialized.
     */
    override fun serialize(buffer: Buffer) {
        buffer.writeString(group)
    }
    /**
     * Deserializes the given buffer.
     *
     * @param buffer The buffer to deserialize.
     */
    override fun deserialize(buffer: Buffer) {
        group = buffer.readString()
    }


}