package bpm.common.packets.internal

import bpm.common.logging.KotlinLogging
import bpm.common.memory.Buffer
import bpm.common.network.NetUtils
import bpm.common.packets.Packet
import java.util.UUID

/**
 * Represents a packet that notifies a disconnect event. This packet is sent by the server to all clients when a client disconnects.
 * It is sent to the server by the client when it disconnects as well.
 *
 * @property uuid The UUID of the disconnected client.
 * @constructor Creates a DisconnectNotification with the specified UUID.
 */
data class DisconnectPacket(var uuid: UUID = NetUtils.DefaultUUID) : Packet {

    /**
     * Serializes the provided Buffer.
     *
     * @param buffer The Buffer to be serialized.
     */
    override fun serialize(buffer: Buffer) {
        if (NetUtils.isDefaultUUID(uuid))
            logger.warn { "UUID is null before write" }
        buffer.writeUUID(uuid)
    }

    /**
     * Deserializes the given buffer.
     *
     * @param buffer The buffer to deserialize.
     */
    override fun deserialize(buffer: Buffer) {
        uuid = buffer.readUUID() //This could throw an exception if the uuid isn't set when serializing
        if (NetUtils.isDefaultUUID(uuid))
            logger.warn { "UUID is null after read" }
    }

    companion object {
        private val logger = KotlinLogging.logger {}
    }

}