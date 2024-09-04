package bpm.common.workspace.packets

import bpm.common.memory.Buffer
import bpm.common.packets.Packet

/**
 * WorkspaceCreateRequestPacket represents a packet used to request the creation of a new workspace.
 *
 * @property name The name of the new workspace to be created.
 * @property description The description of the new workspace.
 */
class WorkspaceCreateRequestPacket(
    var name: String = "",
    var description: String = ""
) : Packet {

    /**
     * Serializes the packet data into the provided Buffer.
     *
     * @param buffer The Buffer to be serialized into.
     */
    override fun serialize(buffer: Buffer) {
        buffer.writeString(name)
        buffer.writeString(description)
    }

    /**
     * Deserializes the packet data from the given buffer.
     *
     * @param buffer The buffer to deserialize from.
     */
    override fun deserialize(buffer: Buffer) {
        name = buffer.readString()
        description = buffer.readString()
    }
}