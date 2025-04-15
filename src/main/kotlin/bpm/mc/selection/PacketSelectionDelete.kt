package bpm.mc.selection

import bpm.common.memory.Buffer
import bpm.common.packets.Packet
import java.util.*

class PacketSelectionDelete(var uuid: UUID = UUID.randomUUID()) : Packet {

    override fun serialize(buffer: Buffer) {
        buffer.writeUUID(uuid)
    }

    override fun deserialize(buffer: Buffer) {
        uuid = buffer.readUUID()
    }
}