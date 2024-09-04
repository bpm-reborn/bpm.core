package bpm.common.workspace.packets

import bpm.common.memory.Buffer
import bpm.common.network.NetUtils
import bpm.common.packets.Packet
import java.util.UUID

/**
 * Represents a packet for selecting a workspace.
 *
 * @property workspaceUid The uuid of the selected workspace.
 */
data class WorkspaceSelected(var workspaceUid: UUID = NetUtils.DefaultUUID) : Packet {

    /**
     * Serializes the provided Buffer.
     *
     * @param buffer The Buffer to be serialized.
     */
    override fun serialize(buffer: Buffer) {
        buffer.writeUUID(workspaceUid)
    }

    /**
     * Deserializes the given buffer.
     *
     * @param buffer The buffer to deserialize.
     */
    override fun deserialize(buffer: Buffer) {
        workspaceUid = buffer.readUUID()
    }

    override fun toString(): String {
        return "WorkspaceSelected('$workspaceUid')"
    }
}