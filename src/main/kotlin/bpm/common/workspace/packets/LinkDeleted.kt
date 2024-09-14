package bpm.common.workspace.packets

import bpm.common.memory.Buffer
import bpm.common.memory.readSet
import bpm.common.memory.writeSet
import bpm.common.network.NetUtils
import bpm.common.packets.Packet
import java.util.UUID

data class LinkDeleted(val uuids: MutableSet<UUID> = mutableSetOf()) : Packet {

    /**
     * Serializes the provided Buffer.
     *
     * @param buffer The Buffer to be serialized.
     */
    override fun serialize(buffer: Buffer) {
        buffer.writeSet(uuids)
    }


    /**
     * Deserializes the given buffer.
     *
     * @param buffer The buffer to deserialize.
     */
    override fun deserialize(buffer: Buffer) {
        uuids.addAll(buffer.readSet())
    }
}