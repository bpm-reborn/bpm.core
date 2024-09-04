package bpm.common.workspace.packets

import bpm.common.memory.Buffer
import bpm.common.packets.Packet
import org.joml.Vector2f

enum class NodeType {
    GetVariable,
    SetVariable,
}

data class VariableNodeCreateRequest(
    var type: NodeType = NodeType.GetVariable,
    var position: Vector2f = Vector2f(),
    var variableName: String = ""
) : Packet {

    /**
     * Serializes the provided Buffer.
     *
     * @param buffer The Buffer to be serialized.
     */
    override fun serialize(buffer: Buffer) {
        buffer.writeEnum(type)
        buffer.writeFloat(position.x)
        buffer.writeFloat(position.y)
        buffer.writeString(variableName)
    }


    /**
     * Deserializes the given buffer.
     *
     * @param buffer The buffer to deserialize.
     */
    override fun deserialize(buffer: Buffer) {
        type = buffer.readEnum(NodeType::class.java)
        position.x = buffer.readFloat()
        position.y = buffer.readFloat()
        variableName = buffer.readString()
    }
}