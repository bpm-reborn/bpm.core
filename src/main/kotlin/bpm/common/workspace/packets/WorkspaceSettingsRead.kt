package bpm.common.workspace.packets

import bpm.common.memory.Buffer
import bpm.common.packets.Packet
import java.util.UUID

data class WorkspaceSettingsRead(var workspaceUid: UUID = UUID.randomUUID()) : Packet {

    override fun serialize(buffer: Buffer) {
        buffer.writeUUID(workspaceUid)
    }

    override fun deserialize(buffer: Buffer) {
        workspaceUid = buffer.readUUID()
    }
}