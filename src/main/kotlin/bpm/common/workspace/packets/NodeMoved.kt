package bpm.common.workspace.packets

import bpm.common.memory.Buffer
import bpm.common.network.NetUtils
import bpm.common.packets.Packet
import java.util.UUID

data class NodeMoved(var uid: UUID = NetUtils.DefaultUUID, var x: Float = 0f, var y: Float = 0f) : Packet {

    /**
     * Serializes the provided Buffer.
     *
     * @param buffer The Buffer to be serialized.
     */
    override fun serialize(buffer: Buffer) {
        buffer.writeUUID(uid)
        buffer.writeFloat(x)
        buffer.writeFloat(y)
    }


    /**
     * Deserializes the given buffer.
     *
     * @param buffer The buffer to deserialize.
     */
    override fun deserialize(buffer: Buffer) {
        uid = buffer.readUUID()
        x = buffer.readFloat()
        y = buffer.readFloat()
    }
}