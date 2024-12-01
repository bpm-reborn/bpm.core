package bpm.common.workspace.packets

import bpm.common.memory.Buffer
import bpm.common.network.NetUtils
import bpm.common.packets.Packet
import bpm.common.serial.Serial
import bpm.common.workspace.graph.Function
import org.joml.Vector4f
import java.rmi.server.UID
import java.util.UUID

data class FunctionColored(var uid: UUID = NetUtils.DefaultUUID, var newColor: Vector4f = Vector4f()) : Packet {

    override fun serialize(buffer: Buffer) {
        buffer.writeUUID(uid)
        buffer.writeFloat(newColor.x)
        buffer.writeFloat(newColor.y)
        buffer.writeFloat(newColor.z)
        buffer.writeFloat(newColor.w)
    }

    override fun deserialize(buffer: Buffer) {
        uid = buffer.readUUID()
        newColor.x = buffer.readFloat()
        newColor.y = buffer.readFloat()
        newColor.z = buffer.readFloat()
        newColor.w = buffer.readFloat()
    }
}