package bpm.common.workspace.packets

import bpm.common.memory.Buffer
import bpm.common.network.NetUtils
import bpm.common.packets.Packet
import bpm.common.serial.Serial
import java.util.*

class FunctionMinimized(
    var uid: UUID = NetUtils.DefaultUUID,
    var minimized: Boolean = false
) : Packet {

    override fun deserialize(buffer: Buffer) {
        uid = buffer.readUUID()
        minimized = buffer.readBoolean()
    }

    override fun serialize(buffer: Buffer) {
        buffer.writeUUID(uid)
        buffer.writeBoolean(minimized)
    }
}