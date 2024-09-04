package bpm.common.workspace.packets

import bpm.common.memory.Buffer
import bpm.common.packets.Packet
import bpm.common.serial.Serial
import bpm.common.workspace.Workspace

/**
 * Represents a packet for selecting a workspace.
 */
data class WorkspaceLoad(var workspace: Workspace? = null) : Packet {

    /**
     * Serializes the provided Buffer.
     *
     * @param buffer The Buffer to be serialized.
     */
    override fun serialize(buffer: Buffer) {
        Serial.write(buffer, workspace!!)
    }

    /**
     * Deserializes the given buffer.
     *
     * @param buffer The buffer to deserialize.
     */
    override fun deserialize(buffer: Buffer) {
        workspace = Serial.read(buffer) ?: error("Failed to deserialize workspace")
    }

}