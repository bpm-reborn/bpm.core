package bpm.common.workspace.packets

import bpm.common.memory.Buffer
import bpm.common.network.NetUtils
import bpm.common.packets.Packet
import java.util.UUID

data class FunctionResized(var uid: UUID = NetUtils.DefaultUUID, var width: Float = 0.0f, var height: Float = 0.0f) :
    Packet {

    override fun serialize(buffer: Buffer) {
        buffer.writeUUID(uid)
        buffer.writeFloat(width)
        buffer.writeFloat(height)
    }

    override fun deserialize(buffer: Buffer) {
        uid = buffer.readUUID()
        width = buffer.readFloat()
        height = buffer.readFloat()
    }

}