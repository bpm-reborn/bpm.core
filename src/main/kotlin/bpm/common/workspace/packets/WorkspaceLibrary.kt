package bpm.common.workspace.packets

import bpm.common.memory.Buffer
import bpm.common.packets.Packet
import java.util.UUID

/**
 * Represents a list of workspaces.
 *
 * This class implements the Packet interface and can be used to serialize and deserialize workspaces to and from a buffer.
 *
 * @property workspaces A mutable list of workspaces.
 * @constructor Creates a WorkspaceList instance with an optional list of workspaces.
 */
class WorkspaceLibrary(val workspaces: MutableMap<UUID, Pair<String, String>> = mutableMapOf()) : Packet {

    /**
     * Serializes the provided Buffer.
     *
     * @param buffer The Buffer to be serialized.
     */
    override fun serialize(buffer: Buffer) {
        val size = workspaces.size
        buffer.writeInt(size)
        workspaces.forEach { (uuid, value) ->
            buffer.writeUUID(uuid)
            buffer.writeString(value.first)
            buffer.writeString(value.second)
        }
    }

    /**
     * Deserializes the given buffer.
     *
     * @param buffer The buffer to deserialize.
     */
    override fun deserialize(buffer: Buffer) {
        val size = buffer.readInt()
        repeat(size) {
            val uuid = buffer.readUUID()
            val name = buffer.readString()
            val description = buffer.readString()
            workspaces[uuid] = Pair(name, description)
        }
    }

}