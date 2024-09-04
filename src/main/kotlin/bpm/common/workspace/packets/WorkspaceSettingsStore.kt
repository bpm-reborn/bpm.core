package bpm.common.workspace.packets

import bpm.common.memory.Buffer
import bpm.common.packets.Packet
import bpm.common.serial.Serial
import bpm.common.workspace.WorkspaceSettings
import java.util.UUID

data class WorkspaceSettingsStore(
    var workspaceUid: UUID = UUID.randomUUID(),
    var workspaceSettings: WorkspaceSettings = WorkspaceSettings()
) : Packet {

    override fun serialize(buffer: Buffer) {
        buffer.writeUUID(workspaceUid)
        Serial.write(buffer, workspaceSettings)
    }

    override fun deserialize(buffer: Buffer) {
        workspaceUid = buffer.readUUID()
        workspaceSettings = Serial.read(buffer) ?: error("Failed to deserialize workspace settings")
    }
}