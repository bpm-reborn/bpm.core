package bpm.common.workspace.packets

import bpm.common.memory.Buffer
import bpm.common.packets.Packet

class NodeLibraryRequest(val nodes: String = "/") : Packet {

    override fun serialize(buffer: Buffer) {
        buffer.writeString(nodes)
    }

    override fun deserialize(buffer: Buffer) {
        buffer.readString()
    }
}