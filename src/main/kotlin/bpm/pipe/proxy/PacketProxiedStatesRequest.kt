package bpm.pipe.proxy

import bpm.common.memory.Buffer
import bpm.common.packets.Packet
import java.util.UUID

/**
 * Used to request all proxies for the given workspace uuid
 */
class PacketProxiedStatesRequest(var workspaceUid: UUID = UUID.randomUUID()) : Packet {

    override fun serialize(buffer: Buffer) {
        buffer.writeUUID(workspaceUid)
    }

    override fun deserialize(buffer: Buffer) {
        workspaceUid = buffer.readUUID()
    }
}