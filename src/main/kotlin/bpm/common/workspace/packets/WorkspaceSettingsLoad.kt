package bpm.common.workspace.packets

import bpm.common.memory.Buffer
import bpm.common.packets.Packet
import bpm.common.serial.Serial
import bpm.common.workspace.WorkspaceSettings
import java.util.UUID

data class WorkspaceSettingsLoad(
    var workspaceSettings: WorkspaceSettings = WorkspaceSettings()
) : Packet {

    override fun serialize(buffer: Buffer) {
        Serial.write(buffer, workspaceSettings)
    }

    override fun deserialize(buffer: Buffer) {
        workspaceSettings = Serial.read(buffer) ?: error("Failed to deserialize workspace settings")
    }
}