package bpm.common.packets.internal

import bpm.common.logging.KotlinLogging
import bpm.common.memory.Buffer
import bpm.common.network.NetUtils
import bpm.common.packets.Packet
import java.util.UUID

data class ConnectRequest(var uuid: UUID = NetUtils.DefaultUUID) : Packet {

    /**
     * Serializes the provided Buffer.
     *
     * @param buffer The Buffer to be serialized.
     */
    override fun serialize(buffer: Buffer) {
        if (NetUtils.isDefaultUUID(uuid)) logger.warn { "UUID is null before write" }
        buffer.writeUUID(uuid)
    }
    /**
     * Deserializes the given buffer.
     *
     * @param buffer The buffer to deserialize.
     */
    override fun deserialize(buffer: Buffer) {
        uuid = buffer.readUUID() //This could throw an exception if the uuid isn't set when serializing
        if (NetUtils.isDefaultUUID(uuid)) logger.warn { "UUID is null after read" }
    }

    companion object {

        private val logger = KotlinLogging.logger {}
    }
}