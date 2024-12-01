package bpm.common.workspace.packets

import bpm.common.memory.Buffer
import bpm.common.network.NetUtils
import bpm.common.packets.Packet
import bpm.common.serial.Serial
import bpm.common.workspace.graph.Function
import java.util.UUID

data class FunctionDeleted(var uid: UUID = NetUtils.DefaultUUID) : Packet {

    override fun serialize(buffer: Buffer) {
        buffer.writeUUID(uid)
    }

    override fun deserialize(buffer: Buffer) {
        uid = buffer.readUUID()
    }

}