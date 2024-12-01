package bpm.common.workspace.packets

import bpm.common.memory.Buffer
import bpm.common.network.NetUtils
import bpm.common.packets.Packet
import bpm.common.serial.Serial
import bpm.common.workspace.graph.Function
import org.joml.Vector4f
import java.rmi.server.UID
import java.util.UUID

data class FunctionNamed(var uid: UUID = NetUtils.DefaultUUID, var newName: String = "") : Packet {

    override fun serialize(buffer: Buffer) {
        buffer.writeUUID(uid)
        buffer.writeString(newName)
    }

    override fun deserialize(buffer: Buffer) {
        uid = buffer.readUUID()
        newName = buffer.readString()
    }
}