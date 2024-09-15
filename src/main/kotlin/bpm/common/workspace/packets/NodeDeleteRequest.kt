package bpm.common.workspace.packets

import bpm.common.memory.Buffer
import bpm.common.memory.readSet
import bpm.common.memory.writeSet
import bpm.common.packets.Packet
import java.util.*

class NodeDeleteRequest(vararg uuid: UUID = arrayOf()) : Packet {

    private val _uuids: MutableSet<UUID> = uuid.toMutableSet()
    val uuids: Set<UUID> get() = _uuids

    /**
     * Serializes the provided Buffer.
     *
     * @param buffer The Buffer to be serialized.
     */
    override fun serialize(buffer: Buffer) {
        buffer.writeSet(_uuids)
    }


    /**
     * Deserializes the given buffer.
     *
     * @param buffer The buffer to deserialize.
     */
    override fun deserialize(buffer: Buffer) {
        _uuids.addAll(buffer.readSet())
    }
}