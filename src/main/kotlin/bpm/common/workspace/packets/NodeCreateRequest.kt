package bpm.common.workspace.packets

import bpm.common.memory.Buffer
import bpm.common.network.NetUtils
import bpm.common.packets.Packet
import org.joml.Vector2f
import java.util.UUID

data class NodeCreateRequest(
    var nodeType: String = "Node",
    var position: Vector2f = Vector2f(),
    var function: UUID = NetUtils.DefaultUUID
) : Packet {

    /**
     * Serializes the provided Buffer.
     *
     * @param buffer The Buffer to be serialized.
     */
    override fun serialize(buffer: Buffer) {
        buffer.writeString(nodeType)
        buffer.writeFloat(position.x)
        buffer.writeFloat(position.y)
        buffer.writeUUID(function)
    }


    /**
     * Deserializes the given buffer.
     *
     * @param buffer The buffer to deserialize.
     */
    override fun deserialize(buffer: Buffer) {
        nodeType = buffer.readString()
        position.x = buffer.readFloat()
        position.y = buffer.readFloat()
        function = buffer.readUUID()
    }
}