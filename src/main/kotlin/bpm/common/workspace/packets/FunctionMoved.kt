package bpm.common.workspace.packets

import bpm.common.memory.Buffer
import bpm.common.network.NetUtils
import bpm.common.packets.Packet
import java.util.UUID

data class FunctionMoved(var uid: UUID = NetUtils.DefaultUUID, var x: Float = 0.0f, var y: Float = 0.0f) : Packet {

    override fun serialize(buffer: Buffer) {
        buffer.writeUUID(uid)
        buffer.writeFloat(x)
        buffer.writeFloat(y)
    }

    override fun deserialize(buffer: Buffer) {
        uid = buffer.readUUID()
        x = buffer.readFloat()
        y = buffer.readFloat()
    }
}