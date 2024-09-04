package bpm.common.workspace.packets

import bpm.common.memory.Buffer
import bpm.common.network.NetUtils
import bpm.common.packets.Packet
import java.util.*

data class WorkspaceCompileRequest(var workspaceId: UUID = NetUtils.DefaultUUID) : Packet {

    override fun serialize(buffer: Buffer) {
        buffer.writeUUID(workspaceId)
    }

    override fun deserialize(buffer: Buffer) {
        workspaceId = buffer.readUUID()
    }

}