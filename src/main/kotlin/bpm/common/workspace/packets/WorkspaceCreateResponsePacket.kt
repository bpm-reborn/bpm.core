package bpm.common.workspace.packets

import bpm.common.memory.Buffer
import bpm.common.packets.Packet
import java.util.*

/**
 * WorkspaceCreateResponsePacket represents a packet sent from the server to the client
 * containing the result of a workspace creation request.
 *
 * @property success Indicates whether the workspace creation was successful.
 * @property workspaceUid The UUID of the newly created workspace, if successful.
 */
class WorkspaceCreateResponsePacket(
    var success: Boolean = false,
    var workspaceUid: UUID = UUID.randomUUID()
) : Packet {

    /**
     * Serializes the packet data into the provided Buffer.
     *
     * @param buffer The Buffer to be serialized into.
     */
    override fun serialize(buffer: Buffer) {
        buffer.writeBoolean(success)
        buffer.writeUUID(workspaceUid)
    }

    /**
     * Deserializes the packet data from the given buffer.
     *
     * @param buffer The buffer to deserialize from.
     */
    override fun deserialize(buffer: Buffer) {
        success = buffer.readBoolean()
        workspaceUid = buffer.readUUID()
    }
}