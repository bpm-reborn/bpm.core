package bpm.common.workspace.packets

import bpm.common.memory.Buffer
import bpm.common.network.NetUtils
import bpm.common.packets.Packet
import bpm.common.packets.internal.Time
import java.util.UUID

/**
 * Represents a workspace creation event.
 *
 * @property workspaceId The ID of the workspace.
 * @property owner The owner of the workspace.
 * @property name The name of the workspace.
 * @property description The description of the workspace.
 * @property createdAt The creation timestamp of the workspace.
 */
data class WorkspaceCreated(
    var workspaceId: String = NetUtils.DefaultUUID.toString(),
    var owner: UUID = NetUtils.DefaultUUID,
    var name: String = "Default Workspace",
    var description: String = "Default Workspace",
    var createdAt: Time = Time.now,
) : Packet {

    /**
     * Serializes the provided Buffer.
     *
     * @param buffer The Buffer to be serialized.
     */
    override fun serialize(buffer: Buffer) {
        buffer.writeString(workspaceId)
        buffer.writeUUID(owner)
        buffer.writeString(name)
        buffer.writeString(description)
        buffer.writeLong(createdAt.time)
    }
    /**
     * Deserializes the given buffer.
     *
     * @param buffer The buffer to deserialize.
     */
    override fun deserialize(buffer: Buffer) {
        workspaceId = buffer.readString()
        owner = buffer.readUUID()
        name = buffer.readString()
        description = buffer.readString()
        createdAt = Time(buffer.readLong())
    }

}