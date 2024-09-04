package bpm.common.workspace.packets

import bpm.common.memory.Buffer
import bpm.common.network.NetUtils
import bpm.common.packets.Packet
import java.util.UUID

data class NodeDeleted(var uuid: UUID = NetUtils.DefaultUUID) : Packet {

    /**
     * Serializes the provided Buffer.
     *
     * @param buffer The Buffer to be serialized.
     */
    override fun serialize(buffer: Buffer) {
        buffer.writeUUID(uuid)
    }


    /**
     * Deserializes the given buffer.
     *
     * @param buffer The buffer to deserialize.
     */
    override fun deserialize(buffer: Buffer) {
        uuid = buffer.readUUID() ?: error("Failed to read properties from buffer!")
    }
}