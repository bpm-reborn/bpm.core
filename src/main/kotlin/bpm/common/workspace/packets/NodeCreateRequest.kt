package bpm.common.workspace.packets

import bpm.common.memory.Buffer
import bpm.common.packets.Packet
import org.joml.Vector2f

data class NodeCreateRequest(var nodeType: String = "Node", var position: Vector2f = Vector2f()) : Packet {

    /**
     * Serializes the provided Buffer.
     *
     * @param buffer The Buffer to be serialized.
     */
    override fun serialize(buffer: Buffer) {
        buffer.writeString(nodeType)
        buffer.writeFloat(position.x)
        buffer.writeFloat(position.y)
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
    }
}