package bpm.common.workspace.packets

import bpm.common.memory.Buffer
import bpm.common.packets.Packet

class VariableDeleteRequest(var name: String = "") : Packet {

    override fun serialize(buffer: Buffer) {
        buffer.writeString(name)
    }

    override fun deserialize(buffer: Buffer) {
        name = buffer.readString()
    }

}
