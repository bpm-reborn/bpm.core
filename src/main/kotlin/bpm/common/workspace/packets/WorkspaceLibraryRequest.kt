package bpm.common.workspace.packets

import bpm.common.memory.Buffer
import bpm.common.packets.Packet

data class WorkspaceLibraryRequest(val directory: String = "/") : Packet {

    /**
     * Serializes the provided Buffer.
     *
     * @param buffer The Buffer to be serialized.
     */
    override fun serialize(buffer: Buffer) {
        buffer.writeString(directory)
    }
    /**
     * Deserializes the given buffer.
     *
     * @param buffer The buffer to deserialize.
     */
    override fun deserialize(buffer: Buffer) {
        buffer.readString()
    }

}